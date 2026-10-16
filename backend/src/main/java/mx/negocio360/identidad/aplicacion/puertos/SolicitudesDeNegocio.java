package mx.negocio360.identidad.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.dominio.SolicitudDeNegocio;

/** Almacen de los negocios pedidos y aun no confirmados por el modulo de negocios (D-78). */
public interface SolicitudesDeNegocio {

    void guardar(SolicitudDeNegocio solicitud);

    Optional<SolicitudDeNegocio> porNegocio(UUID negocioId);

    /** Las solicitudes pendientes de una cuenta, de la mas antigua a la mas reciente. */
    List<SolicitudDeNegocio> deCuenta(UUID cuentaId);

    /** Quita la solicitud ya aplicada. Quitar una que no existe no hace nada. */
    void eliminar(UUID negocioId);
}
