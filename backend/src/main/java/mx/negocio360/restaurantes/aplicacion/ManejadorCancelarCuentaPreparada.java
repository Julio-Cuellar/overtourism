package mx.negocio360.restaurantes.aplicacion;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.compartido.comandos.ManejadorComando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/**
 * CT-11 CancelarCuentaPreparada: cancelar una cuenta con platillos ya listos o entregados exige
 * {@code restaurantes.linea.anular_preparada} en preparar y en confirmar (D-126), un motivo y preparar -&gt; confirmar,
 * porque ya hay merma. Anula sus lineas y libera la mesa.
 */
public final class ManejadorCancelarCuentaPreparada implements ManejadorComando<CancelarCuentaPreparadaComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "cancelar_cuenta_preparada";
    /** Mensaje de exito. */
    public static final String PLANTILLA_CANCELADA = "Cuenta cancelada.";

    private final CuentasServicio cuentas;
    private final AutorizacionDeRestaurantes autorizacion;

    /**
     * Arma el manejador.
     *
     * @param cuentas      las cuentas, donde vive la regla
     * @param autorizacion permisos de la sesion
     */
    public ManejadorCancelarCuentaPreparada(CuentasServicio cuentas, AutorizacionDeRestaurantes autorizacion) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.autorizacion = Objects.requireNonNull(autorizacion, "autorizacion");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public List<String> permisos(CancelarCuentaPreparadaComando comando) {
        return List.of(PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA);
    }

    @Override
    public Class<CancelarCuentaPreparadaComando> claseComando() {
        return CancelarCuentaPreparadaComando.class;
    }

    @Override
    public String preparar(ContextoNegocio contexto, CancelarCuentaPreparadaComando comando) {
        autorizacion.exigir(contexto, PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA);
        if (comando.cuentaId() == null) {
            throw new RestaurantesException(CodigoError.CUENTA_NO_ENCONTRADA);
        }
        return cuentas.resumenDeCancelacion(contexto, comando.cuentaId(), comando.motivo(), comando.texto());
    }

    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, CancelarCuentaPreparadaComando comando,
                                      String resumenPreparado) {
        var resumen = preparar(contexto, comando);
        if (!resumen.equals(resumenPreparado)) {
            throw new RestaurantesException(CodigoError.COMANDO_CAMBIO);
        }
        Autorizacion permisos = permiso -> autorizacion.puede(contexto, permiso);
        var cuenta = cuentas.cancelarCuentaPreparada(contexto, permisos, comando.cuentaId(), comando.motivo(),
                comando.texto());
        return ResultadoComando.ok(PLANTILLA_CANCELADA, cuenta.id(), Map.of("cuenta_id", cuenta.id().toString()));
    }
}
