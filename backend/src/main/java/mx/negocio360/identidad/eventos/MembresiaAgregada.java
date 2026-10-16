package mx.negocio360.identidad.eventos;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Se creo una membresia: el dueño inicial o, mas adelante, una invitacion aceptada. Solo ids y rol (AP-02). */
public record MembresiaAgregada(UUID cuentaId, String rol, Origen origen) {

    public enum Origen { DUENO_INICIAL, INVITACION }

    public static final String TIPO = "identidad.membresia_agregada";
    public static final int VERSION = 1;

    public MembresiaAgregada {
        Objects.requireNonNull(cuentaId, "cuentaId");
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(origen, "origen");
    }

    public Map<String, Object> datos() {
        return Map.of("cuenta_id", cuentaId.toString(), "rol", rol, "origen", origen.name());
    }
}
