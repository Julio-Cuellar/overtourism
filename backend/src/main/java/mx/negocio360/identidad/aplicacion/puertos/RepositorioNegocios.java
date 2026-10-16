package mx.negocio360.identidad.aplicacion.puertos;

import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.dominio.NegocioReferencia;

/** Almacen de la copia minima de los negocios (D-78). */
public interface RepositorioNegocios {

    NegocioReferencia guardar(NegocioReferencia negocio);

    Optional<NegocioReferencia> porId(UUID negocioId);
}
