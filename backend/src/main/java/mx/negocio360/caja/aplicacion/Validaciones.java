package mx.negocio360.caja.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.dominio.CajaException;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.compartido.dinero.Redondeo;

/** Validaciones que comparten los manejadores de Caja. */
final class Validaciones {

    private Validaciones() {
    }

    /**
     * Turno donde se puede operar: existe en el negocio y su estado efectivo es ABIERTO (CJ-10).
     *
     * @throws CajaException TURNO_NO_ABIERTO si no existe o esta cerrado; TURNO_EN_CORTE si esta en corte
     */
    static Turno turnoAbierto(RepositorioCaja cajas, UUID negocioId, UUID turnoId, Instant ahora) {
        var turno = turnoNoCerrado(cajas, negocioId, turnoId, ahora);
        if (turno.estadoEn(ahora) == EstadoTurno.EN_CORTE) {
            throw new CajaException(CodigoError.TURNO_EN_CORTE, turnoId);
        }
        return turno;
    }

    /**
     * Turno que existe en el negocio y no esta CERRADO (CJ-09).
     *
     * @throws CajaException TURNO_NO_ABIERTO
     */
    static Turno turnoNoCerrado(RepositorioCaja cajas, UUID negocioId, UUID turnoId, Instant ahora) {
        if (turnoId == null) {
            throw new CajaException(CodigoError.TURNO_NO_ABIERTO);
        }
        var turno = cajas.turno(negocioId, turnoId)
                .orElseThrow(() -> new CajaException(CodigoError.TURNO_NO_ABIERTO, turnoId));
        if (turno.estadoEn(ahora) == EstadoTurno.CERRADO) {
            throw new CajaException(CodigoError.TURNO_NO_ABIERTO, turnoId);
        }
        return turno;
    }

    /**
     * Dice si un monto es dinero valido: acotado y con a lo mas 2 decimales. Se revisa antes de redondear, para que un
     * exponente enorme se rechace sin calcular (revision M1). El signo lo revisa quien llama.
     */
    static boolean dineroValido(BigDecimal monto) {
        return Redondeo.dineroValido(monto);
    }

    /** Texto obligatorio recortado, o nulo si viene vacio o pasa del maximo. */
    static String textoObligatorio(String texto, int maximo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        var limpio = texto.strip();
        return limpio.length() > maximo ? null : limpio;
    }
}
