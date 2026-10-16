package mx.negocio360.compartido.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Topes del WebSocket de /graphql (SG-15), bajo {@code seguridad.websocket}. Un valor fuera de rango impide el arranque.
 *
 * @param suscripcionesPorCuenta operaciones abiertas a la vez por cuenta, sumando todas sus conexiones: una suscripcion
 *                               cuenta mientras su flujo siga abierto
 * @param conexionesPorMinutoIp  conexiones nuevas por minuto desde una IP
 */
@ConfigurationProperties(prefix = "seguridad.websocket")
public record PropiedadesWebSocket(Integer suscripcionesPorCuenta, Integer conexionesPorMinutoIp) {

    public PropiedadesWebSocket {
        suscripcionesPorCuenta = rango("suscripciones-por-cuenta", suscripcionesPorCuenta, 5, 1, 100);
        conexionesPorMinutoIp = rango("conexiones-por-minuto-ip", conexionesPorMinutoIp, 30, 1, 100_000);
    }

    private static int rango(String nombre, Integer valor, int porOmision, int minimo, int maximo) {
        var usado = valor == null ? porOmision : valor;
        if (usado < minimo || usado > maximo) {
            throw new IllegalArgumentException("seguridad.websocket." + nombre + " debe estar entre " + minimo + " y "
                    + maximo);
        }
        return usado;
    }
}
