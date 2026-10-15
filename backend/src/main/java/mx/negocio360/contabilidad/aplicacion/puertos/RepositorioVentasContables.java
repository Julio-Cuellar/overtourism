package mx.negocio360.contabilidad.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.VentaContable;

/** Puerto de la tabla {@code contabilidad.venta_contable}. La llave natural es (negocio, venta). */
public interface RepositorioVentasContables {

    /**
     * Inserta o reemplaza la huella de una venta.
     *
     * @param venta huella
     */
    void guardar(VentaContable venta);

    /**
     * Huella de una venta.
     *
     * @param negocioId negocio
     * @param ventaId   venta
     * @return la huella, o vacio si la venta no se ha contabilizado
     */
    Optional<VentaContable> porVenta(UUID negocioId, UUID ventaId);

    /**
     * Ventas contabilizadas de una sesion de caja.
     *
     * @param negocioId    negocio
     * @param cajaSesionId sesion de caja
     * @return las huellas
     */
    List<VentaContable> deSesion(UUID negocioId, UUID cajaSesionId);
}
