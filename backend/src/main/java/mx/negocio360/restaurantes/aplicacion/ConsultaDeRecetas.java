package mx.negocio360.restaurantes.aplicacion;

import java.util.Set;
import java.util.UUID;

/** Lo que el menu necesita saber de las recetas (MN-04, resumen de cobertura). Lo implementa el paquete de recetas. */
public interface ConsultaDeRecetas {

    /** Verdadero si el platillo tiene una receta activa. */
    boolean tieneRecetaActiva(UUID negocioId, UUID platilloId);

    /** Ids de los platillos del negocio que tienen receta activa. */
    Set<UUID> platillosConReceta(UUID negocioId);
}
