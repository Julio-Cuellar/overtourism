package mx.negocio360.restaurantes.aplicacion;

import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;

/**
 * Dar de baja un platillo (MN-10): preparar -&gt; confirmar.
 *
 * @param platilloId platillo a dar de baja
 */
public record DarDeBajaPlatilloComando(UUID platilloId) implements Comando {
}
