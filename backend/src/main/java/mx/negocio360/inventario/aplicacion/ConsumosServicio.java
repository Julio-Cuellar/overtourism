package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.inventario.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioConsumos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioMovimientos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.inventario.dominio.CausaTipo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.Consumo;
import mx.negocio360.inventario.dominio.Consumo.LineaConsumida;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.dominio.Saldo;
import mx.negocio360.inventario.dominio.TipoMovimiento;

/**
 * Consumo de insumos por una causa externa, sin confirmacion humana porque es consecuencia de una venta ya confirmada
 * (6.4, D-22). {@code registrarConsumo} y {@code revertirConsumo} son idempotentes por (negocio, causa, consumidor)
 * (K-01, K-03) y dejan una marca si la anulacion llega antes que el consumo (K-04).
 *
 * <p><b>Atomicidad (K-02).</b> Todo se valida y se calcula antes de escribir, asi que una linea que no sirve no deja
 * nada a medias. La escritura en si es atomica solo si el adaptador de persistencia envuelve la llamada en una
 * transaccion; los repositorios en memoria no la dan, y una carrera por el saldo de la segunda linea podria dejar la
 * primera escrita. Dentro de un proceso, las llamadas se serializan para que "revisar si ya existe y escribir" no se
 * cruce con otra.
 */
public final class ConsumosServicio extends Valuador {

    /** Quienes pueden consumir (6.4). */
    static final Set<String> CONSUMIDORES = Set.of("vinculo", "restaurantes");
    static final int MAX_LINEAS = 100;

    private final RepositorioConsumos consumos;
    private final Object cerrojo = new Object();

    /** Arma el servicio. {@code ids} debe ser la misma secuencia que usan los manejadores de movimiento. */
    public ConsumosServicio(RepositorioArticulos articulos, RepositorioSaldos saldos,
                            RepositorioMovimientos movimientos, RepositorioConsumos consumos,
                            PublicadorEventos eventos, Clock reloj, SecuenciaDeIds ids) {
        super(articulos, saldos, movimientos, eventos, reloj, ids);
        this.consumos = Objects.requireNonNull(consumos, "consumos");
    }

    /** Descuenta las lineas de la causa para el consumidor (K-01, K-02, K-04). */
    public ConsumoResultado registrar(ContextoNegocio contexto, String consumidor, UUID causaId, Instant ocurridoEn,
                                      List<LineaDeConsumo> lineas) {
        exigirConsumidor(consumidor);
        Objects.requireNonNull(causaId, "causaId");
        Objects.requireNonNull(lineas, "lineas");
        if (lineas.isEmpty()) {
            throw new InventarioException(CodigoError.CANTIDAD_INVALIDA);
        }
        if (lineas.size() > MAX_LINEAS) {
            throw new IllegalArgumentException("Se pueden consumir como maximo " + MAX_LINEAS + " lineas");
        }
        synchronized (cerrojo) {
            var previo = consumos.porLlave(contexto.negocioId(), causaId, consumidor);
            if (previo.isPresent()) {
                return repeticionDeRegistro(previo.get());
            }
            return descontar(contexto, consumidor, causaId, fechaDelHecho(ocurridoEn), lineas);
        }
    }

    /** Repone al costo original lo que se consumio, o deja la marca si el consumo no ha llegado (K-03, K-04). */
    public ConsumoResultado revertir(ContextoNegocio contexto, String consumidor, UUID causaId) {
        exigirConsumidor(consumidor);
        Objects.requireNonNull(causaId, "causaId");
        synchronized (cerrojo) {
            var previo = consumos.porLlave(contexto.negocioId(), causaId, consumidor);
            if (previo.isEmpty()) {
                consumos.insertar(Consumo.marcaDeAnulacion(contexto.negocioId(), causaId, consumidor,
                        ahora()));
                return ConsumoResultado.sinMovimientos(EstadoDeConsumo.MARCADO_ANULADO_PREVIO);
            }
            var consumo = previo.get();
            return switch (consumo.estado()) {
                case ANULADO_PREVIO -> ConsumoResultado.sinMovimientos(EstadoDeConsumo.MARCADO_ANULADO_PREVIO);
                case REVERTIDO -> new ConsumoResultado(EstadoDeConsumo.YA_REVERTIDO, consumo.movimientosReversa(),
                        consumo.costoTotal(), List.of());
                case REGISTRADO -> reponer(contexto, consumo);
            };
        }
    }

    private static void exigirConsumidor(String consumidor) {
        if (consumidor == null || !CONSUMIDORES.contains(consumidor)) {
            throw new InventarioException(CodigoError.CONSUMIDOR_INVALIDO);
        }
    }

    private static ConsumoResultado repeticionDeRegistro(Consumo guardado) {
        if (guardado.estado() == Consumo.Estado.ANULADO_PREVIO) {
            return ConsumoResultado.sinMovimientos(EstadoDeConsumo.OMITIDO_ANULADO_PREVIO);
        }
        var movimientos = guardado.lineas().stream().map(LineaConsumida::movimientoId).toList();
        return new ConsumoResultado(EstadoDeConsumo.YA_REGISTRADO, movimientos, guardado.costoTotal(), List.of());
    }

    private ConsumoResultado descontar(ContextoNegocio contexto, String consumidor, UUID causaId, Instant ocurridoEn,
                                       List<LineaDeConsumo> lineas) {
        var calculos = new ArrayList<Calculo>(lineas.size());
        var saldosDeTrabajo = new LinkedHashMap<UUID, Saldo>();
        var versiones = new LinkedHashMap<UUID, Long>();
        // Primero se calcula todo: una linea que no sirve falla antes de escribir nada (K-02).
        for (var linea : lineas) {
            var articulo = articuloActivo(contexto, linea.articuloId());
            var conversion = convertir(articulo, linea.cantidad(), linea.unidad());
            var antes = saldosDeTrabajo.get(articulo.id());
            if (antes == null) {
                antes = saldoDe(contexto, articulo);
                versiones.put(articulo.id(), antes.version());
            }
            var calculo = valuar(articulo, antes, TipoMovimiento.SALIDA, CausaTipo.VENTA, causaId, null, conversion,
                    conversion.cantidadBase(), null, null, ocurridoEn, contexto.actor(),
                    "Consumo por venta de " + descripcion(articulo, conversion));
            saldosDeTrabajo.put(articulo.id(), calculo.despues());
            calculos.add(calculo);
        }
        var costoTotal = costoDe(calculos);
        var consumidas = calculos.stream().map(c -> new LineaConsumida(c.articulo().id(), c.movimiento().id(),
                c.movimiento().cantidadBase().negate(), c.movimiento().costoUnitario())).toList();
        // El consumo se guarda al final: si el saldo falla por una carrera, no queda un registro que diga que ya se desconto.
        var alertas = escribir(calculos, versiones);
        consumos.insertar(Consumo.registrado(contexto.negocioId(), causaId, consumidor, consumidas, costoTotal,
                ahora()));
        var movimientos = calculos.stream().map(c -> c.movimiento().id()).toList();
        return new ConsumoResultado(EstadoDeConsumo.REGISTRADO, movimientos, costoTotal, alertas);
    }

    private ConsumoResultado reponer(ContextoNegocio contexto, Consumo consumo) {
        var calculos = new ArrayList<Calculo>(consumo.lineas().size());
        var saldosDeTrabajo = new LinkedHashMap<UUID, Saldo>();
        var versiones = new LinkedHashMap<UUID, Long>();
        var ahora = ahora();
        for (var linea : consumo.lineas()) {
            // Reponer en un articulo dado de baja lo dejaria inactivo con saldo (A-04): se rechaza y se puede reintentar.
            var articulo = articuloActivo(contexto, linea.articuloId());
            var conversion = articulo.aBase(linea.cantidadBase(), articulo.unidadBase().codigo());
            var antes = saldosDeTrabajo.get(articulo.id());
            if (antes == null) {
                antes = saldoDe(contexto, articulo);
                versiones.put(articulo.id(), antes.version());
            }
            var calculo = valuar(articulo, antes, TipoMovimiento.ENTRADA, CausaTipo.REVERSION, consumo.causaId(), null,
                    conversion, linea.cantidadBase(), linea.costoUnitario(), null, ahora, contexto.actor(),
                    "Reversion de consumo de " + descripcion(articulo, conversion));
            saldosDeTrabajo.put(articulo.id(), calculo.despues());
            calculos.add(calculo);
        }
        var movimientos = calculos.stream().map(c -> c.movimiento().id()).toList();
        var alertas = escribir(calculos, versiones);
        consumos.actualizar(consumo.revertido(movimientos));
        return new ConsumoResultado(EstadoDeConsumo.REVERTIDO, movimientos, costoDe(calculos), alertas);
    }

    /**
     * Escribe lo calculado: primero un saldo por articulo (si otra peticion lo movio, falla con CONCURRENCIA antes de
     * dejar movimientos de ese articulo), luego los movimientos y los eventos en el orden de las lineas.
     */
    private List<AlertaDeConsumo> escribir(List<Calculo> calculos, Map<UUID, Long> versiones) {
        var finales = new LinkedHashMap<UUID, Saldo>();
        calculos.forEach(c -> finales.put(c.articulo().id(), c.despues()));
        finales.forEach((articuloId, saldo) -> saldos.actualizar(saldo, versiones.get(articuloId)));
        var alertas = new ArrayList<AlertaDeConsumo>();
        for (var calculo : calculos) {
            movimientos.insertar(calculo.movimiento());
            alertas.addAll(publicar(calculo));
        }
        return alertas;
    }

    private static BigDecimal costoDe(List<Calculo> calculos) {
        return calculos.stream().map(c -> c.movimiento().costoTotal().abs()).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(Redondeo.ESCALA_DINERO);
    }
}
