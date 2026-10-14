package mx.negocio360.clientes.aplicacion.puertos;

import java.util.List;
import java.util.UUID;
import mx.negocio360.clientes.dominio.Consentimiento;

/** Puerto del historial de consentimientos. Solo inserta y lee: nunca actualiza ni borra (P-03). */
public interface RepositorioConsentimientos {

    void insertar(Consentimiento consentimiento);

    /** El historial del cliente, de la fila mas antigua a la mas reciente. */
    List<Consentimiento> porCliente(UUID negocioId, UUID clienteId);
}
