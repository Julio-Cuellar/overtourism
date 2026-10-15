package mx.negocio360.ventas.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import mx.negocio360.compartido.comandos.ManejadorComando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.dinero.DecimalPlano;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.ventas.aplicacion.puertos.CatalogoDeVentas;
import mx.negocio360.ventas.aplicacion.puertos.CuponesDePasaporte;
import mx.negocio360.ventas.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.ventas.aplicacion.puertos.RepositorioVentas;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.EstadoVenta;
import mx.negocio360.ventas.dominio.LineaVenta;
import mx.negocio360.ventas.dominio.PagoVenta;
import mx.negocio360.ventas.dominio.Venta;
import mx.negocio360.ventas.dominio.VentasException;
import mx.negocio360.ventas.eventos.VentaRegistrada;

/**
 * VF-10 RegistrarVenta: VT-01 a VT-08, VT-12 y VT-13.
 *
 * <p>Preparar y confirmar calculan lo mismo con los precios y el cupon del momento. Confirmar compara su resumen con
 * el que se mostro (VT-05), canjea el cupon (VT-07), toma el folio (VT-06), guarda y publica {@code venta.registrada}
 * v1. El orden importa: el canje va antes del folio para que un canje fallido no gaste folio; en produccion todo es
 * una sola transaccion con el outbox, y un fallo despues del canje la revierte entera.
 */
public final class ManejadorRegistrarVenta implements ManejadorComando<RegistrarVentaComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "registrar_venta";
    /** Serie unica del MVP; series por caja estan por definir. */
    public static final String SERIE = "A";

    private final CatalogoDeVentas catalogo;
    private final CuponesDePasaporte cupones;
    private final RepositorioVentas ventas;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    /** Cifras de la venta tal como estan ahora. */
    private record Calculo(List<LineaVenta> lineas, BigDecimal subtotal, UUID cuponId, BigDecimal descuento,
                           BigDecimal total, List<PagoVenta> pagos, BigDecimal cambio, String resumen) {
    }

    /**
     * Arma el manejador.
     *
     * @param catalogo precios de Catalogo (VT-02)
     * @param cupones  cupones de Pasaporte (VT-07)
     * @param ventas   repositorio de ventas y folios
     * @param eventos  outbox
     * @param reloj    reloj de la plataforma
     */
    public ManejadorRegistrarVenta(CatalogoDeVentas catalogo, CuponesDePasaporte cupones, RepositorioVentas ventas,
                                   PublicadorEventos eventos, Clock reloj) {
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.cupones = Objects.requireNonNull(cupones, "cupones");
        this.ventas = Objects.requireNonNull(ventas, "ventas");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<RegistrarVentaComando> claseComando() {
        return RegistrarVentaComando.class;
    }

    /** El turno, para que Caja cuente las ventas preparadas sin confirmar (CJ-07). */
    @Override
    public String ambito(RegistrarVentaComando comando) {
        return comando.turnoId() == null ? null : comando.turnoId().toString();
    }

    /**
     * Valida y calcula la venta sin escribir nada.
     *
     * @throws VentasException TURNO_NO_ABIERTO, los de {@link CalculadoraVenta} o los del cupon
     */
    @Override
    public String preparar(ContextoNegocio contexto, RegistrarVentaComando comando) {
        return calcular(contexto, comando, false).resumen();
    }

    /**
     * Registra la venta.
     *
     * @throws VentasException COMANDO_CAMBIO si el precio o el cupon cambiaron (VT-05), o los de preparar y canje
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, RegistrarVentaComando comando,
                                      String resumenPreparado) {
        var calculo = calcular(contexto, comando, true);
        if (!calculo.resumen().equals(resumenPreparado)) {
            throw new VentasException(CodigoError.COMANDO_CAMBIO);
        }
        var ahora = reloj.instant();
        var ventaId = UuidV7.generar(ahora);
        if (calculo.cuponId() != null) {
            var canje = cupones.canjear(contexto.negocioId(), comando.codigoCupon().strip(), ventaId,
                    calculo.subtotal());
            if (canje.descuento().compareTo(calculo.descuento()) != 0) {
                throw new VentasException(CodigoError.COMANDO_CAMBIO);
            }
        }
        var folio = ventas.siguienteFolio(contexto.negocioId(), SERIE);
        var venta = new Venta(ventaId, contexto.negocioId(), SERIE, folio, EstadoVenta.REGISTRADA, comando.turnoId(),
                comando.clienteId(), calculo.cuponId(), calculo.subtotal(), calculo.descuento(), calculo.total(),
                calculo.lineas(), calculo.pagos(), ahora, contexto.actor(), null, null, null, 1);
        ventas.guardar(venta);
        eventos.publicar(FabricaSobres.de(contexto.negocioId(), ahora, ventaId, VentaRegistrada.TIPO,
                VentaRegistrada.VERSION, new VentaRegistrada(venta).datos()));
        return ResultadoComando.ok(PlantillasVentas.VENTA_REGISTRADA, ventaId, Map.of(
                "venta_id", ventaId.toString(),
                "folio", Long.toString(folio),
                "total", DecimalPlano.escribir(calculo.total()),
                "cambio", DecimalPlano.escribir(calculo.cambio())));
    }

    /**
     * Calcula la venta con los precios y el cupon de ahora.
     *
     * @param alConfirmar verdadero al confirmar: el comando es el mismo que ya paso la validacion al preparar, asi
     *                    que si los pagos dejan de cuadrar es porque el total cambio, y eso es COMANDO_CAMBIO (VT-05)
     */
    private Calculo calcular(ContextoNegocio contexto, RegistrarVentaComando comando, boolean alConfirmar) {
        if (comando.turnoId() == null) {
            throw new VentasException(CodigoError.TURNO_NO_ABIERTO);
        }
        var productos = catalogo.preciosParaVenta(contexto.negocioId(), productosDe(comando));
        var lineas = CalculadoraVenta.lineas(comando.lineas(), productos);
        var subtotal = CalculadoraVenta.subtotal(lineas);
        UUID cuponId = null;
        var descuento = Redondeo.dinero(BigDecimal.ZERO);
        if (comando.codigoCupon() != null && !comando.codigoCupon().isBlank()) {
            var cotizado = cupones.cotizar(contexto.negocioId(), comando.codigoCupon().strip(), subtotal);
            if (cotizado.descuento().signum() < 0) {
                throw new VentasException(CodigoError.CUPON_NO_APLICA, cotizado.cuponId());
            }
            if (cotizado.descuento().compareTo(subtotal) > 0) {
                throw new VentasException(CodigoError.DESCUENTO_MAYOR_AL_SUBTOTAL, cotizado.cuponId());
            }
            cuponId = cotizado.cuponId();
            descuento = Redondeo.dinero(cotizado.descuento());
        }
        var total = Redondeo.dinero(subtotal.subtract(descuento));
        var pagos = pagosDe(total, comando, alConfirmar);
        var cambio = CalculadoraVenta.cambio(pagos);
        var resumen = PlantillasVentas.resumenVenta(lineas.size(), subtotal, descuento, total, cambio);
        return new Calculo(lineas, subtotal, cuponId, descuento, total, pagos, cambio, resumen);
    }

    private static List<PagoVenta> pagosDe(BigDecimal total, RegistrarVentaComando comando, boolean alConfirmar) {
        try {
            return CalculadoraVenta.pagos(total, comando.pagos());
        } catch (VentasException error) {
            if (alConfirmar) {
                throw new VentasException(CodigoError.COMANDO_CAMBIO);
            }
            throw error;
        }
    }

    private static Set<UUID> productosDe(RegistrarVentaComando comando) {
        if (comando.lineas() == null) {
            return Set.of();
        }
        return comando.lineas().stream().filter(Objects::nonNull).map(LineaSolicitada::productoId)
                .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }
}
