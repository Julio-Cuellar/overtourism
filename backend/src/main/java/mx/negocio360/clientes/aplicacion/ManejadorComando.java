package mx.negocio360.clientes.aplicacion;

import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Caso de uso de un comando. {@link #preparar} valida sin escribir nada y devuelve el resumen para la persona;
 * {@link #confirmar} revalida (el mundo pudo cambiar entre los dos pasos), escribe y publica los eventos.
 */
public interface ManejadorComando<C extends Comando> {

    /** Nombre estable del comando, el que se guarda en {@code preparacion.comando_tipo}. */
    String tipo();

    Class<C> claseComando();

    /** Valida y devuelve el resumen. No escribe. Falla con ClientesException si el comando no es valido. */
    String preparar(ContextoNegocio contexto, C comando);

    /** Revalida, escribe y publica. Falla con ClientesException si ya no es valido. */
    ResultadoComando confirmar(ContextoNegocio contexto, C comando);
}
