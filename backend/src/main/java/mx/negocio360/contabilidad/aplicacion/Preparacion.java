package mx.negocio360.contabilidad.aplicacion;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla {@code contabilidad.preparacion}. Guarda solo el hash del token. El comando y el resumen se
 * vacian al confirmar y la fila se purga a las 24 horas (purga pendiente, depende de A1 de la Bitacora).
 *
 * @param comandoHash hash del comando serializado; ata el token a ese comando
 * @param comando     JSON del comando, nulo una vez vaciado
 * @param resumen     lo que se le mostro a la persona, nulo una vez vaciado
 * @param usadoEn     nulo mientras el token no se haya usado
 */
public record Preparacion(String tokenHash, UUID negocioId, String actor, String comandoTipo, String comandoHash,
                          String comando, String resumen, Instant expiraEn, Instant usadoEn) {

    /**
     * Copia de la fila marcada como usada.
     *
     * @param ahora momento del uso
     * @return la fila con {@code usadoEn}
     */
    public Preparacion usada(Instant ahora) {
        return new Preparacion(tokenHash, negocioId, actor, comandoTipo, comandoHash, comando, resumen, expiraEn,
                ahora);
    }

    /**
     * Copia de la fila sin comando ni resumen.
     *
     * @return la fila vaciada
     */
    public Preparacion vaciada() {
        return new Preparacion(tokenHash, negocioId, actor, comandoTipo, comandoHash, null, null, expiraEn, usadoEn);
    }

    /** Dice si el comando sigue presente, pero nunca lo imprime. */
    @Override
    public String toString() {
        return "Preparacion[comandoTipo=" + comandoTipo + ", comando=" + (comando == null ? "vacio" : "presente")
                + ", expiraEn=" + expiraEn + ", usadoEn=" + usadoEn + "]";
    }
}
