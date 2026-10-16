package mx.negocio360.identidad.eventos;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Se quito a una persona de un negocio: la revocaron o salio ella misma. Solo ids y motivo (AP-02). */
public record MembresiaRevocada(UUID cuentaId, Motivo motivo) {

    public enum Motivo { REVOCADA, SALIO }

    public static final String TIPO = "identidad.membresia_revocada";
    public static final int VERSION = 1;

    public MembresiaRevocada {
        Objects.requireNonNull(cuentaId, "cuentaId");
        Objects.requireNonNull(motivo, "motivo");
    }

    public Map<String, Object> datos() {
        return Map.of("cuenta_id", cuentaId.toString(), "motivo", motivo.name());
    }
}
