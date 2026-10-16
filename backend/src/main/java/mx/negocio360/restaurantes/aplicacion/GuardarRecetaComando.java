package mx.negocio360.restaurantes.aplicacion;

import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;

/**
 * Guardar la receta de un platillo (RC-01, RC-08): preparar -&gt; confirmar. Cada confirmacion crea una version nueva.
 *
 * @param platilloId  platillo
 * @param rendimiento porciones que salen con estas cantidades, de 1 a 500
 * @param insumos     de 1 a 30
 */
public record GuardarRecetaComando(UUID platilloId, int rendimiento, List<InsumoComando> insumos) implements Comando {
}
