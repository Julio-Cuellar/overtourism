package mx.negocio360.caja.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.caja.FiltroCortes;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.EstadoVentaAplicada;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.VentaAplicada;
import mx.negocio360.compartido.paginacion.Pagina;

/** Puerto de {@code movimiento_caja}, {@code venta_aplicada}, {@code evento_procesado} y {@code corte}. */
public interface LibroDeCaja {

    /**
     * Inserta un movimiento; nunca se edita ni se borra.
     *
     * @param movimiento movimiento nuevo
     */
    void agregar(MovimientoCaja movimiento);

    /**
     * Movimientos de un turno.
     *
     * @param negocioId negocio
     * @param turnoId   turno
     * @return movimientos del turno en orden
     */
    List<MovimientoCaja> movimientos(UUID negocioId, UUID turnoId);

    /**
     * Movimientos de un turno con cursor, en orden de registro (CF-02, D-131).
     *
     * @param negocioId negocio
     * @param turnoId   turno
     * @param primeros  tamano de pagina, de 1 a 100
     * @param despues   cursor de la pagina anterior, o nulo
     * @return la pagina pedida
     */
    Pagina<MovimientoCaja> movimientos(UUID negocioId, UUID turnoId, int primeros, String despues);

    /**
     * Lo que Caja hizo con una venta, si ya la vio (CJ-04).
     *
     * @param negocioId negocio
     * @param ventaId   venta
     * @return la fila de {@code venta_aplicada}, si existe
     */
    Optional<VentaAplicada> ventaAplicada(UUID negocioId, UUID ventaId);

    /**
     * Inserta o reemplaza la fila de una venta.
     *
     * @param venta fila nueva o reemplazo
     */
    void guardarVentaAplicada(VentaAplicada venta);

    /**
     * Cuenta ventas de un turno en un estado (CJ-07).
     *
     * @param negocioId negocio
     * @param turnoId   turno
     * @param estado    estado buscado
     * @return cuantas hay
     */
    long contarVentas(UUID negocioId, UUID turnoId, EstadoVentaAplicada estado);

    /**
     * Anota que un consumidor proceso un evento (R3).
     *
     * @param consumidor nombre del consumidor
     * @param eventoId   id del evento
     * @return falso si ya se habia procesado
     */
    boolean registrarEvento(String consumidor, UUID eventoId);

    /**
     * Guarda un corte confirmado.
     *
     * @param corte corte confirmado
     */
    void guardarCorte(Corte corte);

    /**
     * Cortes del negocio que cumplen el filtro, del mas reciente al mas antiguo, con cursor (CF-03, D-131).
     *
     * @param negocioId negocio
     * @param filtro    filtro ya validado
     * @param primeros  tamano de pagina, de 1 a 100
     * @param despues   cursor de la pagina anterior, o nulo
     * @return la pagina pedida
     */
    Pagina<Corte> cortes(UUID negocioId, FiltroCortes filtro, int primeros, String despues);
}
