package mx.negocio360.restaurantes.aplicacion;

import java.util.Optional;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.Restaurante;

/** Almacen de la extension de restaurante y de los eventos que ya se procesaron (R3). */
public interface RepositorioRestaurantes {

    /** La extension de ese negocio, si es un restaurante. */
    Optional<Restaurante> porNegocio(UUID negocioId);

    /** Guarda la extension: crea o reemplaza. */
    void guardar(Restaurante restaurante);

    /**
     * Anota que este consumidor proceso este evento.
     *
     * @return verdadero si es la primera vez; falso si ya estaba anotado
     */
    boolean marcarProcesado(String consumidor, UUID eventoId);

    /** Verdadero si ese consumidor ya proceso ese evento. */
    boolean yaProcesado(String consumidor, UUID eventoId);
}
