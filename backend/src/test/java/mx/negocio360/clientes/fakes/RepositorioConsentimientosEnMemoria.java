package mx.negocio360.clientes.fakes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioConsentimientos;
import mx.negocio360.clientes.dominio.Consentimiento;

/** Doble en memoria de un historial solo de insercion (P-03). */
public final class RepositorioConsentimientosEnMemoria implements RepositorioConsentimientos {

    private final List<Consentimiento> filas = new ArrayList<>();

    @Override
    public void insertar(Consentimiento consentimiento) {
        filas.add(consentimiento);
    }

    @Override
    public List<Consentimiento> porCliente(UUID negocioId, UUID clienteId) {
        return filas.stream().filter(c -> c.negocioId().equals(negocioId) && c.clienteId().equals(clienteId))
                .toList();
    }

    public int total() {
        return filas.size();
    }
}
