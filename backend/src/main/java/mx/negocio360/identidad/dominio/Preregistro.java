package mx.negocio360.identidad.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.HashConSal;

/**
 * El formulario de registro de una persona que todavia no tiene cuenta (D-84), con el codigo que se le mando al correo.
 * Vive lo que dure su codigo (15 minutos), admite los mismos intentos que un codigo de acceso y solo guarda el hash
 * del codigo. Hay uno por correo: volver a llenar el formulario o pedir otro codigo lo reemplaza. Se borra al verificarse,
 * al vencer y al reemplazarse. Es inmutable. El nombre y el giro del negocio, si se pidio uno, van juntos.
 *
 * @param id                UUIDv7
 * @param correo            en minusculas; el codigo se manda aqui y es la llave del pre-registro
 * @param telefono          E.164
 * @param nombre            nombre ya normalizado
 * @param apellidos         apellidos ya normalizados
 * @param avisoVersion      version del aviso de privacidad que acepto
 * @param avisoAceptadoEn   cuando lleno el formulario
 * @param negocioNombre     nombre del negocio que pide, o nulo
 * @param negocioGiro       giro del negocio que pide, o nulo
 * @param codigo            hash con sal del codigo
 * @param intentos          intentos fallidos
 * @param venceEn           vencimiento del pre-registro y de su codigo
 * @param creadoEn          cuando se lleno el formulario
 */
public record Preregistro(UUID id, String correo, String telefono, String nombre, String apellidos, int avisoVersion,
                          Instant avisoAceptadoEn, String negocioNombre, String negocioGiro, HashConSal codigo,
                          int intentos, Instant venceEn, Instant creadoEn) {

    public Preregistro {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(correo, "correo");
        Objects.requireNonNull(telefono, "telefono");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(apellidos, "apellidos");
        Objects.requireNonNull(avisoAceptadoEn, "avisoAceptadoEn");
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(venceEn, "venceEn");
        Objects.requireNonNull(creadoEn, "creadoEn");
        if (intentos < 0) {
            throw new IllegalArgumentException("Los intentos no pueden ser negativos");
        }
        if ((negocioNombre == null) != (negocioGiro == null)) {
            throw new IllegalArgumentException("El nombre y el giro del negocio van juntos");
        }
    }

    public static Preregistro nuevo(UUID id, String correo, String telefono, String nombre, String apellidos,
                                    int avisoVersion, String negocioNombre, String negocioGiro, String codigoEnClaro,
                                    Instant ahora, Duration vigencia) {
        return new Preregistro(id, correo, telefono, nombre, apellidos, avisoVersion, ahora, negocioNombre, negocioGiro,
                HashConSal.de(codigoEnClaro), 0, ahora.plus(vigencia), ahora);
    }

    public boolean pideNegocio() {
        return negocioNombre != null;
    }

    public boolean coincide(String intento) {
        return codigo.verifica(intento);
    }

    public boolean vencido(Instant ahora) {
        return !ahora.isBefore(venceEn);
    }

    public boolean agotado(int maximoDeIntentos) {
        return intentos >= maximoDeIntentos;
    }

    public Preregistro conIntentoFallido() {
        return new Preregistro(id, correo, telefono, nombre, apellidos, avisoVersion, avisoAceptadoEn, negocioNombre,
                negocioGiro, codigo, intentos + 1, venceEn, creadoEn);
    }

    /** Otro codigo: reemplaza al anterior, reinicia los intentos y empieza una vigencia nueva. Los datos no cambian. */
    public Preregistro conCodigoNuevo(String codigoEnClaro, Instant ahora, Duration vigencia) {
        return new Preregistro(id, correo, telefono, nombre, apellidos, avisoVersion, avisoAceptadoEn, negocioNombre,
                negocioGiro, HashConSal.de(codigoEnClaro), 0, ahora.plus(vigencia), creadoEn);
    }

    @Override
    public String toString() {
        return "Preregistro[id=" + id + ", intentos=" + intentos + ", venceEn=" + venceEn + "]";
    }
}
