package mx.negocio360.ventas.aplicacion;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.compartido.comandos.ManejadorComando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.ventas.aplicacion.puertos.AutorizacionVentas;
import mx.negocio360.ventas.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.ventas.aplicacion.puertos.RepositorioVentas;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.EstadoVenta;
import mx.negocio360.ventas.dominio.Venta;
import mx.negocio360.ventas.dominio.VentasException;
import mx.negocio360.ventas.eventos.VentaAnulada;

/**
 * VF-11 AnularVenta: VT-09 y VT-11 (D-82). Revisa permiso, motivo y estado al preparar y otra vez al confirmar;
 * confirmar cambia la venta con bloqueo optimista y publica {@code venta.anulada} v1.
 *
 * <p>Pendiente (VT-10, P2): revertir el canje del cupon en Pasaporte; la operacion aun no esta acordada.
 */
public final class ManejadorAnularVenta implements ManejadorComando<AnularVentaComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "anular_venta";
    /** Largo maximo del motivo. */
    public static final int MAX_CARACTERES_MOTIVO = 200;

    private final RepositorioVentas ventas;
    private final AutorizacionVentas autorizacion;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    /**
     * Arma el manejador.
     *
     * @param ventas       repositorio de ventas
     * @param autorizacion permiso de anular (VT-09)
     * @param eventos      outbox
     * @param reloj        reloj de la plataforma
     */
    public ManejadorAnularVenta(RepositorioVentas ventas, AutorizacionVentas autorizacion, PublicadorEventos eventos,
                                Clock reloj) {
        this.ventas = Objects.requireNonNull(ventas, "ventas");
        this.autorizacion = Objects.requireNonNull(autorizacion, "autorizacion");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<AnularVentaComando> claseComando() {
        return AnularVentaComando.class;
    }

    /**
     * Valida y muestra lo que se revertiria.
     *
     * @throws VentasException SIN_PERMISO_ANULAR, MOTIVO_REQUERIDO, VENTA_NO_ENCONTRADA o VENTA_YA_ANULADA
     */
    @Override
    public String preparar(ContextoNegocio contexto, AnularVentaComando comando) {
        return PlantillasVentas.resumenAnulacion(ventaAnulable(contexto, comando));
    }

    /**
     * Anula la venta.
     *
     * @throws VentasException los de preparar, COMANDO_CAMBIO o CONCURRENCIA
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, AnularVentaComando comando, String resumenPreparado) {
        var venta = ventaAnulable(contexto, comando);
        if (!PlantillasVentas.resumenAnulacion(venta).equals(resumenPreparado)) {
            throw new VentasException(CodigoError.COMANDO_CAMBIO, venta.id());
        }
        var anulada = venta.anulada(reloj.instant(), contexto.actor(), comando.motivo().strip());
        if (!ventas.actualizar(anulada, venta.version())) {
            throw new VentasException(CodigoError.CONCURRENCIA, venta.id());
        }
        eventos.publicar(FabricaSobres.de(contexto.negocioId(), anulada.anuladaEn(), venta.id(), VentaAnulada.TIPO,
                VentaAnulada.VERSION, new VentaAnulada(anulada).datos()));
        return ResultadoComando.ok(PlantillasVentas.VENTA_ANULADA, venta.id(),
                Map.of("venta_id", venta.id().toString(), "estado", EstadoVenta.ANULADA.name()));
    }

    private Venta ventaAnulable(ContextoNegocio contexto, AnularVentaComando comando) {
        if (!autorizacion.puedeAnular(contexto)) {
            throw new VentasException(CodigoError.SIN_PERMISO_ANULAR);
        }
        var motivo = comando.motivo();
        if (motivo == null || motivo.isBlank() || motivo.strip().length() > MAX_CARACTERES_MOTIVO) {
            throw new VentasException(CodigoError.MOTIVO_REQUERIDO);
        }
        if (comando.ventaId() == null) {
            throw new VentasException(CodigoError.VENTA_NO_ENCONTRADA);
        }
        var venta = ventas.porId(contexto.negocioId(), comando.ventaId())
                .orElseThrow(() -> new VentasException(CodigoError.VENTA_NO_ENCONTRADA, comando.ventaId()));
        if (venta.estado() == EstadoVenta.ANULADA) {
            throw new VentasException(CodigoError.VENTA_YA_ANULADA, venta.id());
        }
        return venta;
    }
}
