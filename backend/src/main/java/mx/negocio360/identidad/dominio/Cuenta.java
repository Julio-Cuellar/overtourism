package mx.negocio360.identidad.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * La persona que usa el panel (AC-01 a AC-04, D-76). Se identifica por su id, su correo y su telefono E.164, que son
 * unicos, y no pertenece a un negocio: ese vinculo es la membresia. Es inmutable.
 *
 * @param id                   UUIDv7
 * @param nombre               nombre, de 2 a 80 caracteres
 * @param apellidos            apellidos, de 2 a 80 caracteres
 * @param telefono             E.164
 * @param correo               en minusculas
 * @param correoVerificadoEn   cuando se verifico el correo con el codigo que llego a el, o nulo
 * @param avisoVersion         version del aviso de privacidad que acepto
 * @param avisoAceptadoEn      cuando lo acepto
 * @param estado               ACTIVA, SUSPENDIDA o ANONIMIZADA
 * @param creadaEn             alta
 * @param ultimoAccesoEn       ultimo inicio de sesion
 */
public record Cuenta(UUID id, String nombre, String apellidos, String telefono, String correo,
                     Instant correoVerificadoEn, int avisoVersion, Instant avisoAceptadoEn, EstadoCuenta estado,
                     Instant creadaEn, Instant ultimoAccesoEn) {

    public Cuenta {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(apellidos, "apellidos");
        Objects.requireNonNull(telefono, "telefono");
        Objects.requireNonNull(correo, "correo");
        Objects.requireNonNull(avisoAceptadoEn, "avisoAceptadoEn");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(creadaEn, "creadaEn");
    }

    public static Cuenta nueva(UUID id, String nombre, String apellidos, String telefono, String correo,
                               int avisoVersion, Instant ahora) {
        return new Cuenta(id, nombre, apellidos, telefono, correo, null, avisoVersion, ahora, EstadoCuenta.ACTIVA,
                ahora, ahora);
    }

    /**
     * La cuenta que nace de verificar el correo (D-84): el correo queda verificado en {@code ahora}, porque el codigo
     * llego a el, y el aviso de privacidad vale desde que se lleno el formulario.
     */
    public static Cuenta verificada(UUID id, String nombre, String apellidos, String telefono, String correo,
                                    int avisoVersion, Instant avisoAceptadoEn, Instant ahora) {
        return new Cuenta(id, nombre, apellidos, telefono, correo, ahora, avisoVersion, avisoAceptadoEn,
                EstadoCuenta.ACTIVA, ahora, ahora);
    }

    public Cuenta conNombre(String nuevoNombre) {
        return new Cuenta(id, nuevoNombre, apellidos, telefono, correo, correoVerificadoEn, avisoVersion,
                avisoAceptadoEn, estado, creadaEn, ultimoAccesoEn);
    }

    public Cuenta conEstado(EstadoCuenta nuevoEstado) {
        return new Cuenta(id, nombre, apellidos, telefono, correo, correoVerificadoEn, avisoVersion, avisoAceptadoEn,
                nuevoEstado, creadaEn, ultimoAccesoEn);
    }

    public Cuenta conUltimoAcceso(Instant momento) {
        return new Cuenta(id, nombre, apellidos, telefono, correo, correoVerificadoEn, avisoVersion, avisoAceptadoEn,
                estado, creadaEn, momento);
    }

    @Override
    public String toString() {
        return "Cuenta[id=" + id + ", estado=" + estado + "]";
    }
}
