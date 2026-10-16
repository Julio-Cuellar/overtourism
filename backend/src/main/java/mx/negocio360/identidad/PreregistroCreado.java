package mx.negocio360.identidad;

import java.time.Instant;

/**
 * Lo que recibe quien llena el formulario de registro (D-84): el correo (normalizado) al que se mando el codigo y hasta
 * cuando vale. La cuenta todavia no existe. {@code toString} no revela el correo (SI-15).
 */
public record PreregistroCreado(String correo, Instant venceEn) {

    @Override
    public String toString() {
        return "PreregistroCreado[venceEn=" + venceEn + "]";
    }
}
