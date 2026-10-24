package mx.negocio360.restaurantes.aplicacion;

import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;

/**
 * El texto con que se avisa por que se cancelo algo (QR-10). Lo compone el servidor a partir del codigo; el texto libre
 * solo acompaña a {@code OTRO}.
 */
public final class MensajeDeMotivo {

    private MensajeDeMotivo() {
    }

    /** El mensaje fijo del motivo, con el texto libre al final solo si el motivo es OTRO; nulo sin motivo. */
    public static String de(MotivoDeAnulacion motivo, String texto) {
        if (motivo == null) {
            return null;
        }
        return motivo == MotivoDeAnulacion.OTRO && texto != null && !texto.isBlank()
                ? motivo.mensaje() + ": " + texto.strip() : motivo.mensaje();
    }
}
