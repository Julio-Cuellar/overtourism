package mx.negocio360.restaurantes.aplicacion;

import java.util.Objects;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.EstadoDeLinea;

/**
 * Si un platillo esta en una cuenta que todavia no se paga (MN-10): una cuenta abierta o por cobrar con una linea no
 * anulada de ese platillo. Con eso, dar de baja un platillo en servicio falla con {@code PLATILLO_EN_CUENTA_ABIERTA}.
 */
public final class UsoPorCuentas implements UsoDeLosPlatillos {

    private final RepositorioCuentas cuentas;

    /** @param cuentas almacen de cuentas */
    public UsoPorCuentas(RepositorioCuentas cuentas) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
    }

    @Override
    public boolean enCuentaAbierta(UUID negocioId, UUID platilloId) {
        return cuentas.cuentas(negocioId).stream().filter(c -> c.estado().noPagada())
                .anyMatch(c -> c.lineas().stream().anyMatch(l -> l.estado() != EstadoDeLinea.ANULADA
                        && l.platilloId().equals(platilloId)));
    }
}
