package mx.negocio360.identidad.eventos;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.identidad.dominio.EstadoMembresia;

/** Cambio el rol o el estado (suspendida o reactivada) de una membresia. Solo ids, roles y estado (AP-02). */
public record MembresiaCambiada(UUID cuentaId, String rolAnterior, String rolNuevo, EstadoMembresia estado) {

    public static final String TIPO = "identidad.membresia_cambiada";
    public static final int VERSION = 1;

    public MembresiaCambiada {
        Objects.requireNonNull(cuentaId, "cuentaId");
        Objects.requireNonNull(rolAnterior, "rolAnterior");
        Objects.requireNonNull(rolNuevo, "rolNuevo");
        Objects.requireNonNull(estado, "estado");
    }

    public Map<String, Object> datos() {
        return Map.of("cuenta_id", cuentaId.toString(), "rol_anterior", rolAnterior, "rol_nuevo", rolNuevo,
                "estado", estado.name());
    }
}
