package mx.negocio360.contabilidad.aplicacion;

import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Caso de uso de un comando. {@link #preparar} valida sin escribir nada y devuelve el resumen para la persona;
 * {@link #confirmar} revalida (el mundo pudo cambiar entre los dos pasos), escribe y publica los eventos.
 *
 * @param <C> tipo de comando que atiende
 */
public interface ManejadorComando<C extends Comando> {

    /**
     * Nombre estable del comando.
     *
     * @return el nombre que se guarda en la preparacion
     */
    String tipo();

    /**
     * Clase del comando que atiende.
     *
     * @return la clase
     */
    Class<C> claseComando();

    /**
     * Valida y devuelve el resumen. No escribe.
     *
     * @param contexto negocio y actor
     * @param comando  comando a validar
     * @return resumen para la persona
     * @throws mx.negocio360.contabilidad.dominio.ContabilidadException si el comando no es valido
     */
    String preparar(ContextoNegocio contexto, C comando);

    /**
     * Revalida, escribe y publica.
     *
     * @param contexto negocio y actor
     * @param comando  comando a ejecutar
     * @return el resultado
     * @throws mx.negocio360.contabilidad.dominio.ContabilidadException si ya no es valido
     */
    ResultadoComando confirmar(ContextoNegocio contexto, C comando);
}
