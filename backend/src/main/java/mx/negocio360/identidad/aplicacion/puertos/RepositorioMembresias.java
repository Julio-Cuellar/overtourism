package mx.negocio360.identidad.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.dominio.Membresia;

/** Almacen de membresias. {@code guardar} inserta o reemplaza por id y devuelve lo guardado. */
public interface RepositorioMembresias {

    Membresia guardar(Membresia membresia);

    /** La membresia de esa cuenta en ese negocio, en cualquier estado (es unica, AM-01). */
    Optional<Membresia> porCuentaYNegocio(UUID cuentaId, UUID negocioId);

    /** Todas las membresias de una cuenta, en cualquier estado y en todos sus negocios. */
    List<Membresia> deCuenta(UUID cuentaId);

    /** Todas las membresias de un negocio, en cualquier estado. */
    List<Membresia> deNegocio(UUID negocioId);
}
