package mx.negocio360.clientes.eventos;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Cambio de consentimiento de contacto. Quien quiera contactar al cliente debe respetarlo (P-02). */
public record ClienteConsentimientoCambiado(UUID clienteId, Finalidad finalidad, EstadoConsentimiento estado,
                                            MedioConsentimiento medio) {

    public static final String TIPO = "cliente.consentimiento_cambiado";
    public static final int VERSION = 1;

    public ClienteConsentimientoCambiado {
        Objects.requireNonNull(clienteId, "clienteId");
        Objects.requireNonNull(finalidad, "finalidad");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(medio, "medio");
    }

    /** Carga del sobre, con las claves del contrato. */
    public Map<String, Object> datos() {
        return Map.of("cliente_id", clienteId.toString(), "finalidad", finalidad.name(), "estado", estado.name(),
                "medio", medio.name());
    }
}
