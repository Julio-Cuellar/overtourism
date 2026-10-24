package mx.negocio360.ventas.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.paginacion.Pagina;
import mx.negocio360.ventas.FiltroVentas;
import mx.negocio360.ventas.dominio.Venta;

/** Puerto de las tablas {@code venta}, {@code linea_venta}, {@code pago_venta} y {@code serie_folio}. */
public interface RepositorioVentas {

    /**
     * Inserta una venta nueva con sus lineas y pagos.
     *
     * @param venta venta REGISTRADA
     */
    void guardar(Venta venta);

    /**
     * Busca una venta del negocio.
     *
     * @param negocioId negocio del contexto
     * @param ventaId   venta
     * @return la venta, si existe en ese negocio
     */
    Optional<Venta> porId(UUID negocioId, UUID ventaId);

    /**
     * Ventas de un turno del negocio, registradas y anuladas.
     *
     * @param negocioId negocio del contexto
     * @param turnoId   turno de Caja
     * @return ventas en orden de folio
     */
    List<Venta> porTurno(UUID negocioId, UUID turnoId);

    /**
     * Ventas del negocio que cumplen el filtro, de la mas reciente a la mas antigua, con cursor (VF-02, D-131).
     *
     * @param negocioId negocio del contexto
     * @param filtro    filtro ya validado
     * @param primeros  tamano de pagina, de 1 a 100
     * @param despues   cursor de la pagina anterior, o nulo
     * @return la pagina pedida
     */
    Pagina<Venta> buscar(UUID negocioId, FiltroVentas filtro, int primeros, String despues);

    /**
     * Siguiente folio de la serie, con bloqueo de fila en la misma transaccion de confirmar (VT-06).
     *
     * @param negocioId negocio
     * @param serie     serie del folio
     * @return el folio asignado, desde 1
     */
    long siguienteFolio(UUID negocioId, String serie);

    /**
     * Reemplaza la venta solo si su version sigue siendo la esperada (bloqueo optimista).
     *
     * @param venta           venta con la version nueva
     * @param versionEsperada version que se leyo
     * @return falso si otra peticion la cambio antes
     */
    boolean actualizar(Venta venta, int versionEsperada);
}
