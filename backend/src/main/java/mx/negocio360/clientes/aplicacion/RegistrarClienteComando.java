package mx.negocio360.clientes.aplicacion;

import java.time.LocalDate;
import java.util.List;

/**
 * Alta de un cliente (7.2). {@code avisoVersion} es la version del aviso de privacidad que se puso a su disposicion
 * (L-03); es un {@code Integer} para poder detectar que falta y responder AVISO_VERSION_REQUERIDA.
 */
public record RegistrarClienteComando(String nombre, String apellidos, String telefono, String correo,
                                      LocalDate fechaNacimiento, String notas, Integer avisoVersion,
                                      List<ConsentimientoSolicitado> consentimientos) implements Comando {

    /** No imprime los datos personales del alta (P-06). */
    @Override
    public String toString() {
        return "RegistrarClienteComando[***]";
    }
}
