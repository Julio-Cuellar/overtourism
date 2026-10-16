package mx.negocio360.compartido.graphql;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SG-15: cuantas operaciones tiene abiertas cada cuenta a la vez por WebSocket, sumando todas sus conexiones. Una
 * suscripcion ocupa un lugar mientras su flujo siga abierto. Una cuenta sin operaciones abiertas se olvida, asi que la
 * memoria no crece con las cuentas que ya se fueron.
 */
public final class ControlDeSuscripciones {

    private final int maximoPorCuenta;
    private final ConcurrentHashMap<String, Integer> abiertas = new ConcurrentHashMap<>();

    public ControlDeSuscripciones(int maximoPorCuenta) {
        if (maximoPorCuenta < 1) {
            throw new IllegalArgumentException("El maximo por cuenta debe ser positivo");
        }
        this.maximoPorCuenta = maximoPorCuenta;
    }

    /** Reserva un lugar si la cuenta no llego al tope. */
    public boolean adquirir(String cuenta) {
        var concedido = new boolean[1];
        abiertas.compute(cuenta, (clave, actuales) -> {
            var n = actuales == null ? 0 : actuales;
            concedido[0] = n < maximoPorCuenta;
            return concedido[0] ? n + 1 : actuales;
        });
        return concedido[0];
    }

    /** Devuelve un lugar; sin lugares ocupados no hace nada, para que un doble aviso no deje cuentas en negativo. */
    public void liberar(String cuenta) {
        abiertas.computeIfPresent(cuenta, (clave, actuales) -> actuales <= 1 ? null : actuales - 1);
    }

    public int activas(String cuenta) {
        return abiertas.getOrDefault(cuenta, 0);
    }

    /** Cuantas cuentas tienen algo abierto ahora. */
    public int cuentas() {
        return abiertas.size();
    }
}
