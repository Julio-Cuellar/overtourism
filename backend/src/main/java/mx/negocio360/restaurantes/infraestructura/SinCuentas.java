package mx.negocio360.restaurantes.infraestructura;

import java.util.UUID;
import mx.negocio360.restaurantes.aplicacion.UsoDeLosPlatillos;

/** Las cuentas llegan con P1b: hasta entonces ningun platillo esta en una cuenta abierta. */
public final class SinCuentas implements UsoDeLosPlatillos {

    @Override
    public boolean enCuentaAbierta(UUID negocioId, UUID platilloId) {
        return false;
    }
}
