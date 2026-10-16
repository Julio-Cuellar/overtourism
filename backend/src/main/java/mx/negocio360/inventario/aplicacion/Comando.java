package mx.negocio360.inventario.aplicacion;

import org.springframework.modulith.NamedInterface;

/**
 * Comando de escritura que inicia una persona. Todos pasan por preparar -> confirmar. Cada comando se serializa a
 * JSON para guardarse en la preparacion, asi que debe ser un record de datos simples.
 */
@NamedInterface("api")
public interface Comando {
}
