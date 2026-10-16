package mx.negocio360.inventario.aplicacion.puertos;

import java.util.List;
import java.util.UUID;
import mx.negocio360.inventario.dominio.Movimiento;

/** Puerto de la tabla {@code movimiento}, que solo admite inserciones (sin UPDATE ni DELETE). */
public interface RepositorioMovimientos {

    void insertar(Movimiento movimiento);

    /** Movimientos de un articulo ordenados por id, a partir del siguiente a {@code despuesDe} cuando no es nulo. */
    List<Movimiento> kardex(UUID negocioId, UUID articuloId, UUID despuesDe, int limite);
}
