package mx.negocio360.identidad.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.HashConSal;

/**
 * Codigo de acceso enviado a un destino, hoy el correo de la cuenta (AS-01, AS-02, D-85): de un solo uso, vence a los
 * 5 minutos y se agota a los 5 intentos. Solo guarda el hash con sal; el codigo en claro no se puede recuperar. Es inmutable.
 *
 * @param id        identificador
 * @param destino   correo (en minusculas) al que se envio
 * @param codigo    hash con sal del codigo
 * @param intentos  intentos fallidos
 * @param expiraEn  vencimiento
 * @param usadoEn   momento de uso, o nulo
 * @param creadoEn  emision
 */
public record CodigoAcceso(UUID id, String destino, HashConSal codigo, int intentos, Instant expiraEn,
                           Instant usadoEn, Instant creadoEn) {

    public CodigoAcceso {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(destino, "destino");
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(expiraEn, "expiraEn");
        Objects.requireNonNull(creadoEn, "creadoEn");
        if (intentos < 0) {
            throw new IllegalArgumentException("Los intentos no pueden ser negativos");
        }
    }

    public static CodigoAcceso emitir(UUID id, String destino, String codigoEnClaro, Instant ahora,
                                      Duration vigencia) {
        return new CodigoAcceso(id, destino, HashConSal.de(codigoEnClaro), 0, ahora.plus(vigencia), null, ahora);
    }

    public boolean coincide(String intento) {
        return codigo.verifica(intento);
    }

    public boolean vencido(Instant ahora) {
        return !ahora.isBefore(expiraEn);
    }

    public boolean agotado(int maximoDeIntentos) {
        return intentos >= maximoDeIntentos;
    }

    public CodigoAcceso conIntentoFallido() {
        return new CodigoAcceso(id, destino, codigo, intentos + 1, expiraEn, usadoEn, creadoEn);
    }

    public CodigoAcceso usado(Instant ahora) {
        return new CodigoAcceso(id, destino, codigo, intentos, expiraEn, ahora, creadoEn);
    }

    @Override
    public String toString() {
        return "CodigoAcceso[id=" + id + ", intentos=" + intentos + ", expiraEn=" + expiraEn + "]";
    }
}
