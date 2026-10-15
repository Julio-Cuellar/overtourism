package mx.negocio360.contabilidad.dominio;

import java.util.UUID;

/**
 * Lo que hizo Contabilidad con un evento recibido (columna {@code evento_procesado.resultado}).
 *
 * @param resultado aplicado, omitido o pendiente
 * @param motivo    por que se omitio o quedo pendiente, o nulo
 * @param polizaId  poliza registrada, o nula
 */
public record ResultadoEvento(Resultado resultado, String motivo, UUID polizaId) {

    /** Desenlace de un evento. */
    public enum Resultado {
        /** Se registro su efecto (una poliza o una huella sin poliza). */
        APLICADO,
        /** No hizo nada: ya se habia procesado o no tiene efecto contable. */
        OMITIDO,
        /** No se pudo contabilizar solo; espera a una persona (E-04). */
        PENDIENTE
    }

    /**
     * Evento aplicado.
     *
     * @param polizaId poliza registrada, o nula si no hizo falta poliza
     * @return el resultado
     */
    public static ResultadoEvento aplicado(UUID polizaId) {
        return new ResultadoEvento(Resultado.APLICADO, null, polizaId);
    }

    /**
     * Evento omitido.
     *
     * @param motivo por que se omitio
     * @return el resultado
     */
    public static ResultadoEvento omitido(String motivo) {
        return new ResultadoEvento(Resultado.OMITIDO, motivo, null);
    }

    /**
     * Evento pendiente.
     *
     * @param motivo por que no se pudo contabilizar
     * @return el resultado
     */
    public static ResultadoEvento pendiente(String motivo) {
        return new ResultadoEvento(Resultado.PENDIENTE, motivo, null);
    }
}
