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
 * CT-03 AnularLineaPreparada: anular una linea ya lista o entregada exige {@code restaurantes.linea.anular_preparada} en
 * preparar y en confirmar (D-126), un motivo y preparar -&gt; confirmar, porque ya hay merma. Confirmar revalida y compara el
 * resumen con el que se mostro: si la linea cambio entre los dos pasos, {@code COMANDO_CAMBIO}.
 */
public final class ManejadorAnularLineaPreparada implements ManejadorComando<AnularLineaPreparadaComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "anular_linea_preparada";
    /** Mensaje de exito. */
    public static final String PLANTILLA_ANULADA = "Línea anulada.";

    private final CuentasServicio cuentas;
    private final AutorizacionDeRestaurantes autorizacion;

    /**
     * Arma el manejador.
     *
     * @param cuentas      las cuentas, donde vive la regla
     * @param autorizacion permisos de la sesion
     */
    public ManejadorAnularLineaPreparada(CuentasServicio cuentas, AutorizacionDeRestaurantes autorizacion) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.autorizacion = Objects.requireNonNull(autorizacion, "autorizacion");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public List<String> permisos(AnularLineaPreparadaComando comando) {
        return List.of(PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA);
    }

    @Override
    public Class<AnularLineaPreparadaComando> claseComando() {
        return AnularLineaPreparadaComando.class;
    }

    @Override
    public String preparar(ContextoNegocio contexto, AnularLineaPreparadaComando comando) {
        autorizacion.exigir(contexto, PermisosDeRestaurantes.LINEA_ANULAR_PREPARADA);
        if (comando.lineaId() == null) {
            throw new RestaurantesException(CodigoError.LINEA_NO_ENCONTRADA);
        }
        return cuentas.resumenDeAnulacion(contexto, comando.lineaId(), comando.motivo(), comando.texto());
    }

    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, AnularLineaPreparadaComando comando, String resumenPreparado) {
        var resumen = preparar(contexto, comando);
        if (!resumen.equals(resumenPreparado)) {
            throw new RestaurantesException(CodigoError.COMANDO_CAMBIO);
        }
        Autorizacion permisos = permiso -> autorizacion.puede(contexto, permiso);
        var cuenta = cuentas.anularLineaPreparada(contexto, permisos, comando.lineaId(), comando.motivo(), comando.texto());
        return ResultadoComando.ok(PLANTILLA_ANULADA, cuenta.id(), Map.of("cuenta_id", cuenta.id().toString(),
                "linea_id", comando.lineaId().toString()));
    }
}
