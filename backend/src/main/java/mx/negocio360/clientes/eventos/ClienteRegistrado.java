package mx.negocio360.clientes.eventos;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Se registro un cliente. Sin datos personales: solo el id y de donde vino. */
public record ClienteRegistrado(UUID clienteId, OrigenCliente origen) {

    public static final String TIPO = "cliente.registrado";
    public static final int VERSION = 1;

    public ClienteRegistrado {
        Objects.requireNonNull(clienteId, "clienteId");
        Objects.requireNonNull(origen, "origen");
    }

    /** Carga del sobre, con las claves del contrato. */
    public Map<String, Object> datos() {
        return Map.of("cliente_id", clienteId.toString(), "origen", origen.name());
    }
}
