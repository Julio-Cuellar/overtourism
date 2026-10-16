package mx.negocio360.compartido.comandos;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla {@code preparacion} de un modulo. Guarda solo el hash del token (nunca el token en claro).
 *
 * @param tokenHash   SHA-256 del token, en hexadecimal
 * @param negocioId   negocio que preparo el comando
 * @param actor       quien lo preparo; solo el mismo actor puede confirmarlo
 * @param comandoTipo nombre estable del comando ({@link ManejadorComando#tipo()})
 * @param comandoHash hash del comando serializado; ata el token a ese comando
 * @param comando     JSON del comando, nulo una vez vaciado
 * @param resumen     lo que se le mostro a la persona, nulo una vez vaciado
 * @param ambito      ambito para contar preparaciones vigentes (por ejemplo el turno), o nulo
 * @param expiraEn    instante en que vence el token (5 minutos, D-23)
 * @param usadoEn     nulo mientras el token no se haya usado
 */
public record Preparacion(String tokenHash, UUID negocioId, String actor, String comandoTipo, String comandoHash,
                          String comando, String resumen, String ambito, Instant expiraEn, Instant usadoEn) {

    /**
     * Copia marcada como usada.
     *
     * @param ahora instante de uso
     * @return la misma preparacion con {@code usadoEn}
     */
    public Preparacion usada(Instant ahora) {
        return new Preparacion(tokenHash, negocioId, actor, comandoTipo, comandoHash, comando, resumen, ambito,
                expiraEn, ahora);
    }

    /**
     * Copia sin el comando ni el resumen, que pueden llevar datos de la persona.
     *
     * @return la misma preparacion vaciada
     */
    public Preparacion vaciada() {
        return new Preparacion(tokenHash, negocioId, actor, comandoTipo, comandoHash, null, null, ambito, expiraEn,
                usadoEn);
    }

    /** No imprime el comando ni el resumen. */
    @Override
    public String toString() {
        return "Preparacion[comandoTipo=" + comandoTipo + ", comando=" + (comando == null ? "vacio" : "presente")
                + ", expiraEn=" + expiraEn + ", usadoEn=" + usadoEn + "]";
    }
}
