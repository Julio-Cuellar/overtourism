package mx.negocio360.clientes.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.clientes.dominio.Cliente;
import mx.negocio360.clientes.dominio.CriterioBusqueda;

/**
 * Puerto de persistencia de clientes. Toda operacion se limita al negocio indicado (SC-04): la implementacion real
 * lo refuerza ademas con seguridad por fila en la base de datos.
 */
public interface RepositorioClientes {

    Optional<Cliente> porId(UUID negocioId, UUID id);

    /** El cliente activo del negocio con ese telefono E.164, si existe (I-03). */
    Optional<Cliente> activoPorTelefono(UUID negocioId, String telefonoE164);

    /** Inserta un cliente nuevo. Falla con TELEFONO_DUPLICADO si otro activo ya tiene el telefono. */
    void insertar(Cliente cliente);

    /**
     * Reemplaza al cliente solo si la version guardada es {@code versionEsperada} (I-06); si no, falla con
     * CONCURRENCIA. Falla con TELEFONO_DUPLICADO si otro activo ya tiene el telefono.
     */
    void actualizar(Cliente nuevo, long versionEsperada);

    /**
     * Clientes activos que cumplen el criterio, ordenados por id (UUIDv7, sin datos personales en el cursor), a
     * partir del id siguiente a {@code despuesDe} cuando no es nulo.
     */
    List<Cliente> buscar(UUID negocioId, CriterioBusqueda criterio, UUID despuesDe, int limite);
}
