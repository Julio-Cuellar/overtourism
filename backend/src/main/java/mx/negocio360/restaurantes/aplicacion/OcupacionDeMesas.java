package mx.negocio360.restaurantes.aplicacion;

import java.util.Map;
import java.util.UUID;

/**
 * Que mesas tienen una cuenta sin pagar (MS-02, MS-03). El salon no conoce las cuentas: se lo pregunta a este puerto, que
 * implementa el almacen de cuentas.
 */
public interface OcupacionDeMesas {

    /** La cuenta abierta o por cobrar de cada mesa del negocio que la tiene. Una mesa a lo mas tiene una (MS-03). */
    Map<UUID, ResumenDeCuenta> cuentasNoPagadas(UUID negocioId);
}
