package mx.negocio360.caja.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Periodo en que un cajero opera una caja con un fondo inicial. ABIERTO → EN_CORTE → CERRADO (D-140).
 *
 * <p>EN_CORTE tiene vencimiento: si el corte preparado no se confirma a tiempo, el turno vuelve a operar como ABIERTO
 * sin que corra ninguna tarea ({@link #estadoEn}).
 *
 * @param id            UUIDv7
 * @param negocioId     negocio dueño
 * @param cajaId        caja del turno
 * @param cajero        actor que abrio el turno (referencia opaca a Identidad)
 * @param estado        estado guardado
 * @param fondoInicial  escala 2
 * @param abiertoEn     UTC
 * @param cerradoEn     UTC, o nulo
 * @param corteVenceEn  hasta cuando dura EN_CORTE, o nulo
 * @param version       bloqueo optimista, desde 1
 */
public record Turno(UUID id, UUID negocioId, UUID cajaId, String cajero, EstadoTurno estado, BigDecimal fondoInicial,
                    Instant abiertoEn, Instant cerradoEn, Instant corteVenceEn, int version) {

    /** Exige los datos del turno y una version desde 1. */
    public Turno {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(cajaId, "cajaId");
        Objects.requireNonNull(cajero, "cajero");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(fondoInicial, "fondoInicial");
        Objects.requireNonNull(abiertoEn, "abiertoEn");
        if (version < 1) {
            throw new IllegalArgumentException("La version empieza en 1");
        }
    }

    /**
     * Estado efectivo en un instante: un EN_CORTE vencido cuenta como ABIERTO.
     *
     * @param ahora instante de referencia
     * @return estado efectivo
     */
    public EstadoTurno estadoEn(Instant ahora) {
        if (estado == EstadoTurno.EN_CORTE && corteVenceEn != null && !ahora.isBefore(corteVenceEn)) {
            return EstadoTurno.ABIERTO;
        }
        return estado;
    }

    /**
     * El turno como se ve en un instante: si el corte preparado ya vencio, una copia ABIERTA y sin vencimiento, con la
     * misma version (no es un cambio guardado). Asi quien consulta no ve EN_CORTE en un turno que ya puede vender.
     *
     * @param ahora instante de referencia
     * @return este mismo turno, o la copia con su estado efectivo
     */
    public Turno vigenteEn(Instant ahora) {
        if (estadoEn(ahora) == estado) {
            return this;
        }
        return new Turno(id, negocioId, cajaId, cajero, EstadoTurno.ABIERTO, fondoInicial, abiertoEn, cerradoEn, null,
                version);
    }

    /**
     * Copia en corte hasta un vencimiento (D-140).
     *
     * @param vence fin del corte preparado
     * @return turno EN_CORTE con la version siguiente
     */
    public Turno enCorte(Instant vence) {
        return new Turno(id, negocioId, cajaId, cajero, EstadoTurno.EN_CORTE, fondoInicial, abiertoEn, null, vence,
                version + 1);
    }

    /**
     * Copia abierta, al descartar un corte (CF-13).
     *
     * @return turno ABIERTO, sin corte pendiente, con la version siguiente
     */
    public Turno abierto() {
        return new Turno(id, negocioId, cajaId, cajero, EstadoTurno.ABIERTO, fondoInicial, abiertoEn, null, null,
                version + 1);
    }

    /**
     * Copia cerrada (CJ-09).
     *
     * @param ahora cuando se cierra
     * @return turno CERRADO con la version siguiente
     */
    public Turno cerrado(Instant ahora) {
        return new Turno(id, negocioId, cajaId, cajero, EstadoTurno.CERRADO, fondoInicial, abiertoEn, ahora, null,
                version + 1);
    }
}
