package mx.negocio360.restaurantes.aplicacion;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.Receta;

/** Almacen de recetas. Toda consulta lleva el negocio (R1, SG-02); el reemplazo compara la version anterior (MN-13). */
public interface RepositorioRecetas {

    /** La receta de un platillo del negocio. */
    Optional<Receta> porPlatillo(UUID negocioId, UUID platilloId);

    /** Las recetas de esos platillos; los que no tienen no aparecen. */
    Map<UUID, Receta> porPlatillos(UUID negocioId, Set<UUID> platilloIds);

    /** Guarda una receta nueva; falla si el platillo ya tiene una. */
    void insertar(Receta receta);

    /** Reemplaza la receta si su version guardada es {@code versionAnterior}; si no, falso. */
    boolean reemplazar(Receta nueva, long versionAnterior);
}
