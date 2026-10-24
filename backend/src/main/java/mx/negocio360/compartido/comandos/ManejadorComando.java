package mx.negocio360.compartido.comandos;

import java.util.List;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Caso de uso de un comando dentro del patron preparar -&gt; confirmar.
 *
 * <p>{@link #preparar} valida y devuelve el resumen que ve la persona; salvo excepciones documentadas (D-140) no
 * escribe. {@link #confirmar} recibe el mismo comando y el resumen que se mostro, revalida (el mundo pudo cambiar
 * entre los dos pasos), escribe y publica los eventos en una sola transaccion. Si el resumen recalculado ya no es el
 * que se mostro, debe fallar con el codigo {@code COMANDO_CAMBIO} de su modulo (VT-05).
 *
 * @param <C> tipo de comando que atiende
 */
public interface ManejadorComando<C extends Comando> {

    /**
     * Nombre estable del comando, el que se guarda en {@code preparacion.comando_tipo}.
     *
     * @return nombre en minusculas con guion bajo, por ejemplo {@code registrar_venta}
     */
    String tipo();

    /**
     * Clase del comando, para deserializarlo al confirmar.
     *
     * @return la clase concreta del record del comando
     */
    Class<C> claseComando();

    /**
     * Valida el comando y arma el resumen para confirmar.
     *
     * @param contexto negocio y actor de la peticion
     * @param comando  comando a validar
     * @return resumen en espanol de Mexico; debe ser determinista para el mismo estado del mundo
     */
    String preparar(ContextoNegocio contexto, C comando);

    /**
     * Revalida, escribe y publica.
     *
     * @param contexto         negocio y actor de la peticion
     * @param comando          el comando que se preparo
     * @param resumenPreparado el resumen que se le mostro a la persona al preparar
     * @return resultado con codigo {@code OK}, plantilla, id del recurso y datos de respuesta
     */
    ResultadoComando confirmar(ContextoNegocio contexto, C comando, String resumenPreparado);

    /**
     * Ambito de la preparacion, para contar las que siguen vigentes (por ejemplo el turno de una venta, CJ-07).
     *
     * @param comando comando que se prepara
     * @return ambito como texto, o {@code null} si no aplica
     */
    default String ambito(C comando) {
        return null;
    }

    /**
     * Permisos que exige este comando, ademas del que la puerta de GraphQL comprueba por variante (D-126). El servicio
     * los exige <b>todos</b> al preparar y otra vez al confirmar, antes de gastar el token: si a la persona le quitan
     * el rol entre los dos pasos, recibe {@code PERMISO_DENEGADO} y el token no se gasta.
     *
     * @param comando comando que se prepara o se confirma
     * @return codigos de permiso; vacia si el comando no pide ninguno
     */
    default List<String> permisos(C comando) {
        return List.of();
    }
}
