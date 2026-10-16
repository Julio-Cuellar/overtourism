package mx.negocio360.inventario.aplicacion;

import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Caso de uso de un comando. {@link #preparar} valida sin escribir nada y devuelve el resumen para la persona;
 * {@link #confirmar} revalida (el mundo pudo cambiar entre los dos pasos), escribe y publica los eventos.
 */
public interface ManejadorComando<C extends Comando> {

    /** Nombre estable del comando, el que se guarda en {@code preparacion.comando_tipo}. */
    String tipo();

    Class<C> claseComando();

    /** Valida y devuelve el resumen, con las cifras que calcula el servidor. No escribe. */
    String preparar(ContextoNegocio contexto, C comando);

    /**
     * Revalida, compara contra {@code resumenPreparado} (lo que vio la persona), escribe y publica. Calcula una sola
     * vez: el estado que se compara es el mismo que respalda la escritura. Falla con COMANDO_CAMBIO si el resumen ya
     * no coincide, o con otro codigo si el comando ya no es valido.
     */
    ResultadoComando confirmar(ContextoNegocio contexto, C comando, String resumenPreparado);
}
