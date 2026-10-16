package mx.negocio360.identidad;

/**
 * El formulario de registro (D-76, D-84): nombre, apellidos, correo y celular de la persona, y si acepta el aviso de
 * privacidad. No lleva codigo: la persona todavia no tiene cuenta y el codigo llega despues, al correo. Opcionalmente
 * pide en la misma llamada un negocio (D-79): al verificar el correo se crea la persona y el negocio se pide al modulo de
 * negocios por evento. {@code toString} no revela nada (SI-15).
 */
public record SolicitudDeRegistro(String nombre, String apellidos, String correo, String telefono,
                                  boolean aceptaAviso, NegocioPedido negocio) {

    /** El negocio que se pide junto con el registro: su nombre y su giro. */
    public record NegocioPedido(String nombre, String giro) {
        @Override
        public String toString() {
            return "NegocioPedido[protegido]";
        }
    }

    /** Registro de la persona sin pedir un negocio. */
    public SolicitudDeRegistro(String nombre, String apellidos, String correo, String telefono, boolean aceptaAviso) {
        this(nombre, apellidos, correo, telefono, aceptaAviso, null);
    }

    @Override
    public String toString() {
        return "SolicitudDeRegistro[protegido]";
    }
}
