package mx.negocio360.inventario.aplicacion;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla {@code preparacion}. Guarda solo el hash del token. El comando de Inventario no lleva datos
 * personales, asi que (a diferencia de Clientes) no hace falta vaciarlo al confirmar.
 *
 * @param comandoHash hash del comando serializado; ata el token a ese comando
 * @param resumen     lo que se le mostro a la persona; al confirmar se recalcula y debe coincidir
 * @param usadoEn     nulo mientras el token no se haya usado
 */
public record Preparacion(String tokenHash, UUID negocioId, String actor, String comandoTipo, String comandoHash,
                          String comando, String resumen, Instant expiraEn, Instant usadoEn) {

    public Preparacion usada(Instant ahora) {
        return new Preparacion(tokenHash, negocioId, actor, comandoTipo, comandoHash, comando, resumen, expiraEn,
                ahora);
    }
}
