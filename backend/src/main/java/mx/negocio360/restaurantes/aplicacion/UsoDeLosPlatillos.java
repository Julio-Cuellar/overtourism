package mx.negocio360.restaurantes.aplicacion;

import java.util.UUID;

/** Si un platillo esta en una cuenta que todavia no se paga (MN-10). Las cuentas llegan con P1b; hasta entonces nunca. */
public interface UsoDeLosPlatillos {

    /** Verdadero si alguna cuenta abierta o por cobrar lo tiene. */
    boolean enCuentaAbierta(UUID negocioId, UUID platilloId);
}
