package mx.negocio360.clientes.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.clientes.eventos.EstadoConsentimiento;
import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.MedioConsentimiento;

/**
 * Una fila del historial de consentimientos. Solo se inserta (P-03): revocar agrega otra fila. Guarda la evidencia
 * de P-04: forma, medio, version del aviso, quien lo registro y cuando.
 */
public record Consentimiento(UUID id, UUID negocioId, UUID clienteId, Finalidad finalidad,
                             EstadoConsentimiento estado, FormaConsentimiento forma, MedioConsentimiento medio,
                             int avisoVersion, String registradoPor, Instant registradoEn) {

    public Consentimiento {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(clienteId, "clienteId");
        Objects.requireNonNull(finalidad, "finalidad");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(forma, "forma");
        Objects.requireNonNull(medio, "medio");
        Objects.requireNonNull(registradoPor, "registradoPor");
        Objects.requireNonNull(registradoEn, "registradoEn");
        if (avisoVersion < 1) {
            throw new IllegalArgumentException("El consentimiento exige la version del aviso (P-04): " + avisoVersion);
        }
    }
}
