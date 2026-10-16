package mx.negocio360.restaurantes.aplicacion;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.Categoria;
import mx.negocio360.restaurantes.dominio.Platillo;

/**
 * Almacen del menu. Toda consulta lleva el negocio: un repositorio nunca devuelve algo de otro negocio (R1, SG-02). Las
 * escrituras de reemplazo comparan la version anterior y devuelven falso si otra persona se adelanto (MN-13).
 */
public interface RepositorioMenu {

    /** Categorias del negocio, activas o no. */
    List<Categoria> categorias(UUID negocioId);

    /** Una categoria del negocio. */
    Optional<Categoria> categoria(UUID negocioId, UUID id);

    /** Guarda una categoria nueva. */
    void insertar(Categoria categoria);

    /** Reemplaza la categoria si su version guardada es {@code versionAnterior}; si no, falso. */
    boolean reemplazar(Categoria nueva, long versionAnterior);

    /** Todos los platillos del negocio, incluidos los dados de baja. */
    List<Platillo> platillos(UUID negocioId);

    /** Un platillo del negocio, activo o no. */
    Optional<Platillo> platillo(UUID negocioId, UUID id);

    /** Los platillos del negocio con esos ids; los que no son suyos no aparecen. */
    Map<UUID, Platillo> platillos(UUID negocioId, Set<UUID> ids);

    /** Guarda un platillo nuevo. */
    void insertar(Platillo platillo);

    /** Reemplaza el platillo si su version guardada es {@code versionAnterior}; si no, falso. */
    boolean reemplazar(Platillo nuevo, long versionAnterior);
}
