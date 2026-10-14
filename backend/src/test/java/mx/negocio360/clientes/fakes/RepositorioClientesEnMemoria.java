package mx.negocio360.clientes.fakes;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioClientes;
import mx.negocio360.clientes.dominio.Cliente;
import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.dominio.CriterioBusqueda;
import mx.negocio360.clientes.dominio.EstadoCliente;

/**
 * Doble en memoria del repositorio. Imita lo que hara la base: unico parcial de telefono entre activos, bloqueo
 * optimista por version y orden de las listas por id (comparado como texto, igual que uuid en Postgres).
 */
public final class RepositorioClientesEnMemoria implements RepositorioClientes {

    private final Map<UUID, Cliente> porId = new LinkedHashMap<>();

    @Override
    public Optional<Cliente> porId(UUID negocioId, UUID id) {
        return Optional.ofNullable(porId.get(id)).filter(c -> c.negocioId().equals(negocioId));
    }

    @Override
    public Optional<Cliente> activoPorTelefono(UUID negocioId, String telefono) {
        return porId.values().stream()
                .filter(c -> c.negocioId().equals(negocioId))
                .filter(c -> c.estado() == EstadoCliente.ACTIVO)
                .filter(c -> telefono.equals(c.datos().telefono()))
                .findFirst();
    }

    @Override
    public void insertar(Cliente cliente) {
        verificarTelefonoUnico(cliente);
        porId.put(cliente.id(), cliente);
    }

    @Override
    public void actualizar(Cliente nuevo, long versionEsperada) {
        var actual = porId.get(nuevo.id());
        if (actual == null || actual.version() != versionEsperada) {
            throw new ClientesException(CodigoError.CONCURRENCIA);
        }
        verificarTelefonoUnico(nuevo);
        porId.put(nuevo.id(), nuevo);
    }

    /** Cuantas veces se consulto la lista; sirve para comprobar que un criterio vacio ni siquiera llega aqui. */
    public int consultasDeBusqueda;

    @Override
    public List<Cliente> buscar(UUID negocioId, CriterioBusqueda criterio, UUID despuesDe, int limite) {
        consultasDeBusqueda++;
        return porId.values().stream()
                .filter(c -> c.negocioId().equals(negocioId))
                .filter(criterio::coincide)
                .filter(c -> despuesDe == null || c.id().toString().compareTo(despuesDe.toString()) > 0)
                .sorted(Comparator.comparing(c -> c.id().toString()))
                .limit(limite)
                .toList();
    }

    public int total() {
        return porId.size();
    }

    private void verificarTelefonoUnico(Cliente candidato) {
        var telefono = candidato.datos().telefono();
        if (telefono == null || candidato.estado() != EstadoCliente.ACTIVO) {
            return;
        }
        activoPorTelefono(candidato.negocioId(), telefono)
                .filter(existente -> !existente.id().equals(candidato.id()))
                .ifPresent(existente -> {
                    throw new ClientesException(CodigoError.TELEFONO_DUPLICADO, existente.id());
                });
    }
}
