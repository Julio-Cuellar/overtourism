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
import mx.negocio360.ventas.VerificadorDeTurno;
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
 * <p>Preparar y confirmar calculan lo mismo con el turno, los precios y el cupon del momento: primero los limites de
 * entrada (sin consultar a nadie), luego Caja confirma que el turno esta ABIERTO (VT-01) y despues Catalogo y
 * Pasaporte. Confirmar compara su resumen, que lleva cada linea, con el que se mostro (VT-05), canjea el cupon pasando
 * el descuento esperado para que Pasaporte rechace sin canjear si cambio (VT-07), toma el folio (VT-06), guarda y
 * publica {@code venta.registrada} v1. El canje va antes del folio para que un canje fallido no gaste folio; en
 * produccion todo es una sola transaccion con el outbox, y un fallo despues del canje la revierte entera.
 */
public final class ManejadorRegistrarVenta implements ManejadorComando<RegistrarVentaComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "registrar_venta";
    /** Serie unica del MVP; series por caja estan por definir. */
    public static final String SERIE = "A";
    /** Largo maximo del codigo de cupon; uno mas largo no se manda a Pasaporte. */
    public static final int MAX_CARACTERES_CUPON = 40;

    private final CatalogoDeVentas catalogo;
    private final CuponesDePasaporte cupones;
    private final VerificadorDeTurno turnos;
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
     * @param turnos   Caja dice si el turno esta abierto (VT-01)
     * @param ventas   repositorio de ventas y folios
     * @param eventos  outbox
     * @param reloj    reloj de la plataforma
     */
    public ManejadorRegistrarVenta(CatalogoDeVentas catalogo, CuponesDePasaporte cupones, VerificadorDeTurno turnos,
                                   RepositorioVentas ventas, PublicadorEventos eventos, Clock reloj) {
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.cupones = Objects.requireNonNull(cupones, "cupones");
        this.turnos = Objects.requireNonNull(turnos, "turnos");
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
     * @throws VentasException los de {@link CalculadoraVenta}, TURNO_NO_ABIERTO o los del cupon
     */
    @Override
    public String preparar(ContextoNegocio contexto, RegistrarVentaComando comando) {
        return calcular(contexto, comando, false).resumen();
    }

    /**
     * Registra la venta.
     *
     * @throws VentasException COMANDO_CAMBIO si una linea, un precio o el cupon cambiaron (VT-05), TURNO_NO_ABIERTO si
     *                         el turno entro en corte o cerro, o los de preparar y canje
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
                    calculo.subtotal(), calculo.descuento());
            // El puerto ya rechaza sin canjear si el descuento cambio; esto cuida a un adaptador que no lo haga.
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
     * Calcula la venta con el turno, los precios y el cupon de ahora. Los limites de entrada van primero para no
     * consultar a Caja, Catalogo ni Pasaporte con una peticion desmedida (revision M2).
     *
     * @param alConfirmar verdadero al confirmar: el comando es el mismo que ya paso la validacion al preparar, asi
     *                    que si los pagos dejan de cuadrar es porque el total cambio, y eso es COMANDO_CAMBIO (VT-05)
     */
    private Calculo calcular(ContextoNegocio contexto, RegistrarVentaComando comando, boolean alConfirmar) {
        CalculadoraVenta.validarTamano(comando.lineas(), comando.pagos());
        var codigoCupon = codigoCupon(comando);
        if (comando.turnoId() == null || !turnos.abierto(contexto, comando.turnoId())) {
            throw new VentasException(CodigoError.TURNO_NO_ABIERTO);
        }
        var productos = catalogo.preciosParaVenta(contexto.negocioId(), productosDe(comando));
        var lineas = CalculadoraVenta.lineas(comando.lineas(), productos);
        var subtotal = CalculadoraVenta.subtotal(lineas);
        UUID cuponId = null;
        var descuento = Redondeo.dinero(BigDecimal.ZERO);
        if (codigoCupon != null) {
            var cotizado = cupones.cotizar(contexto.negocioId(), codigoCupon, subtotal);
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
        var resumen = PlantillasVentas.resumenVenta(lineas, subtotal, descuento, total, cambio);
        return new Calculo(lineas, subtotal, cuponId, descuento, total, pagos, cambio, resumen);
    }

    /** Codigo de cupon recortado, o nulo si no hay; uno desmedido es CUPON_INVALIDO sin preguntar a Pasaporte. */
    private static String codigoCupon(RegistrarVentaComando comando) {
        if (comando.codigoCupon() == null || comando.codigoCupon().isBlank()) {
            return null;
        }
        var codigo = comando.codigoCupon().strip();
        if (codigo.length() > MAX_CARACTERES_CUPON) {
            throw new VentasException(CodigoError.CUPON_INVALIDO);
        }
        return codigo;
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
        return comando.lineas().stream().filter(Objects::nonNull).map(LineaSolicitada::productoId)
                .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }
}
