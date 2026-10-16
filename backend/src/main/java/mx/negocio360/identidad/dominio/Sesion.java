package mx.negocio360.identidad.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sesion abierta con un token opaco (AS-05, AS-07). Guarda solo el hash del token. La vigencia es deslizante (se
 * renueva con cada uso) y nunca pasa del tope absoluto. {@code negocioId} es el negocio activo, nulo hasta elegir uno
 * (AS-06). Es inmutable: cada cambio devuelve otra sesion.
 *
 * @param tokenHash          SHA-256 hexadecimal del token
 * @param cuentaId           cuenta a la que pertenece
 * @param negocioId          negocio activo, o nulo
 * @param dispositivo        texto corto que describe el cliente
 * @param ipTruncada         IP sin el ultimo segmento
 * @param creadaEn           apertura
 * @param ultimoUsoEn        ultima peticion autenticada
 * @param expiraEn           fin por inactividad
 * @param expiraAbsolutaEn   fin por tope absoluto
 * @param revocadaEn         momento de la revocacion, o nulo
 */
public record Sesion(String tokenHash, UUID cuentaId, UUID negocioId, String dispositivo, String ipTruncada,
                     Instant creadaEn, Instant ultimoUsoEn, Instant expiraEn, Instant expiraAbsolutaEn,
                     Instant revocadaEn) {

    public Sesion {
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(cuentaId, "cuentaId");
        Objects.requireNonNull(creadaEn, "creadaEn");
        Objects.requireNonNull(ultimoUsoEn, "ultimoUsoEn");
        Objects.requireNonNull(expiraEn, "expiraEn");
        Objects.requireNonNull(expiraAbsolutaEn, "expiraAbsolutaEn");
    }

    public static Sesion abrir(String tokenHash, UUID cuentaId, String dispositivo, String ipTruncada, Instant ahora,
                               Duration inactividad, Duration topeAbsoluto) {
        return new Sesion(tokenHash, cuentaId, null, dispositivo, ipTruncada, ahora, ahora, ahora.plus(inactividad),
                ahora.plus(topeAbsoluto), null);
    }

    public boolean vigente(Instant ahora) {
        return revocadaEn == null && ahora.isBefore(expiraEn) && ahora.isBefore(expiraAbsolutaEn);
    }

    /** Corre la inactividad desde el momento de uso, sin pasar del tope absoluto. */
    public Sesion renovada(Instant ahora, Duration inactividad) {
        var nuevoFin = ahora.plus(inactividad);
        var fin = nuevoFin.isBefore(expiraAbsolutaEn) ? nuevoFin : expiraAbsolutaEn;
        return new Sesion(tokenHash, cuentaId, negocioId, dispositivo, ipTruncada, creadaEn, ahora, fin,
                expiraAbsolutaEn, revocadaEn);
    }

    /** Revoca la sesion. Si ya estaba revocada conserva la primera fecha. */
    public Sesion revocada(Instant ahora) {
        if (revocadaEn != null) {
            return this;
        }
        return new Sesion(tokenHash, cuentaId, negocioId, dispositivo, ipTruncada, creadaEn, ultimoUsoEn, expiraEn,
                expiraAbsolutaEn, ahora);
    }

    /**
     * La misma sesion con otro token y otro negocio activo (SI-07): conserva la cuenta, el dispositivo, la IP y el tope
     * absoluto, asi que cambiar de negocio no alarga la sesion.
     */
    public Sesion paraNegocio(String nuevoTokenHash, UUID negocio, Instant ahora, Duration inactividad) {
        var nuevoFin = ahora.plus(inactividad);
        var fin = nuevoFin.isBefore(expiraAbsolutaEn) ? nuevoFin : expiraAbsolutaEn;
        return new Sesion(nuevoTokenHash, cuentaId, negocio, dispositivo, ipTruncada, ahora, ahora, fin,
                expiraAbsolutaEn, null);
    }

    public Sesion conNegocio(UUID negocio) {
        return new Sesion(tokenHash, cuentaId, negocio, dispositivo, ipTruncada, creadaEn, ultimoUsoEn, expiraEn,
                expiraAbsolutaEn, revocadaEn);
    }

    @Override
    public String toString() {
        return "Sesion[cuentaId=" + cuentaId + ", negocioId=" + negocioId + ", expiraEn=" + expiraEn + "]";
    }
}
