package mx.negocio360.identidad.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * La persona que usa el panel (AC-01 a AC-04). Se identifica por su telefono E.164 verificado y no pertenece a un
 * negocio: ese vinculo sera la membresia. Es inmutable.
 *
 * @param id               UUIDv7
 * @param nombre           nombre visible, de 2 a 80 caracteres
 * @param telefono         E.164
 * @param estado           ACTIVA, SUSPENDIDA o ANONIMIZADA
 * @param creadaEn         alta
 * @param ultimoAccesoEn   ultimo inicio de sesion
 */
public record Cuenta(UUID id, String nombre, String telefono, EstadoCuenta estado, Instant creadaEn,
                     Instant ultimoAccesoEn) {

    public Cuenta {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(telefono, "telefono");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(creadaEn, "creadaEn");
    }

    public static Cuenta nueva(UUID id, String nombre, String telefono, Instant ahora) {
        return new Cuenta(id, nombre, telefono, EstadoCuenta.ACTIVA, ahora, ahora);
    }

    public Cuenta conNombre(String nuevoNombre) {
        return new Cuenta(id, nuevoNombre, telefono, estado, creadaEn, ultimoAccesoEn);
    }

    public Cuenta conEstado(EstadoCuenta nuevoEstado) {
        return new Cuenta(id, nombre, telefono, nuevoEstado, creadaEn, ultimoAccesoEn);
    }

    public Cuenta conUltimoAcceso(Instant momento) {
        return new Cuenta(id, nombre, telefono, estado, creadaEn, momento);
    }

    @Override
    public String toString() {
        return "Cuenta[id=" + id + ", estado=" + estado + "]";
    }
}
