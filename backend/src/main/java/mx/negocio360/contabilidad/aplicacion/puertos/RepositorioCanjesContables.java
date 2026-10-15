package mx.negocio360.contabilidad.aplicacion.puertos;

import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.CanjeContable;

/** Puerto de la tabla {@code contabilidad.canje_contable}. La llave natural es (negocio, cupon). */
public interface RepositorioCanjesContables {

    /**
     * Inserta la huella de un canje.
     *
     * @param canje huella
     */
    void guardar(CanjeContable canje);

    /**
     * Huella de un cupon.
     *
     * @param negocioId negocio
     * @param cuponId   cupon
     * @return la huella, o vacio si el cupon no se ha contabilizado
     */
    Optional<CanjeContable> porCupon(UUID negocioId, UUID cuponId);
}
