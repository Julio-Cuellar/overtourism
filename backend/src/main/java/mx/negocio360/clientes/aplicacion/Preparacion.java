package mx.negocio360.clientes.aplicacion;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla {@code preparacion}. Guarda solo el hash del token. El comando y el resumen llevan datos
 * personales: se vacian al confirmar (SC-10) y la fila se purga a las 24 horas.
 *
 * @param comandoHash hash del comando serializado; ata el token a ese comando
 * @param comando     JSON del comando, nulo una vez vaciado
 * @param resumen     lo que se le mostro a la persona, nulo una vez vaciado
 * @param usadoEn     nulo mientras el token no se haya usado
 */
public record Preparacion(String tokenHash, UUID negocioId, String actor, String comandoTipo, String comandoHash,
                          String comando, String resumen, Instant expiraEn, Instant usadoEn) {

    public Preparacion usada(Instant ahora) {
        return new Preparacion(tokenHash, negocioId, actor, comandoTipo, comandoHash, comando, resumen, expiraEn,
                ahora);
    }

    public Preparacion vaciada() {
        return new Preparacion(tokenHash, negocioId, actor, comandoTipo, comandoHash, null, null, expiraEn, usadoEn);
    }

    /** Dice si el comando sigue presente, pero nunca lo imprime: lleva datos personales (P-06, SC-10). */
    @Override
    public String toString() {
        return "Preparacion[comandoTipo=" + comandoTipo + ", comando=" + (comando == null ? "vacio" : "presente")
                + ", expiraEn=" + expiraEn + ", usadoEn=" + usadoEn + "]";
    }
}
