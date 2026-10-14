package mx.negocio360.clientes.eventos;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Se anonimizo un cliente (D-50). Quien guarde su id debe tratarlo como una referencia sin persona. */
public record ClienteAnonimizado(UUID clienteId) {

    public static final String TIPO = "cliente.anonimizado";
    public static final int VERSION = 1;

    public ClienteAnonimizado {
        Objects.requireNonNull(clienteId, "clienteId");
    }

    /** Carga del sobre, con las claves del contrato. */
    public Map<String, Object> datos() {
        return Map.of("cliente_id", clienteId.toString());
    }
}
