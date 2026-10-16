package mx.negocio360.compartido.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites de tasa de /graphql (SG-10), bajo {@code seguridad.tasa}. Valores iniciales aceptados; un valor fuera de
 * rango impide el arranque. Los contadores viven en la memoria de cada instancia.
 *
 * @param peticionesPorMinutoCuenta peticiones por minuto de una cuenta con sesion valida
 * @param fallosPorMinutoIp         intentos por minuto de una IP sin sesion valida (respuestas 401): sin token, o con
 *                                  uno falso, vencido o revocado
 * @param maximoDeClaves            cuentas e IP que se recuerdan a la vez; al pasar se descartan las menos usadas
 */
@ConfigurationProperties(prefix = "seguridad.tasa")
public record PropiedadesTasa(Integer peticionesPorMinutoCuenta, Integer fallosPorMinutoIp, Integer maximoDeClaves) {

    public PropiedadesTasa {
        peticionesPorMinutoCuenta = rango("peticiones-por-minuto-cuenta", peticionesPorMinutoCuenta, 60, 1, 100_000);
        fallosPorMinutoIp = rango("fallos-por-minuto-ip", fallosPorMinutoIp, 20, 1, 100_000);
        maximoDeClaves = rango("maximo-de-claves", maximoDeClaves, 100_000, 100, 10_000_000);
    }

    private static int rango(String nombre, Integer valor, int porOmision, int minimo, int maximo) {
        var usado = valor == null ? porOmision : valor;
        if (usado < minimo || usado > maximo) {
            throw new IllegalArgumentException("seguridad.tasa." + nombre + " debe estar entre " + minimo + " y "
                    + maximo);
        }
        return usado;
    }
}
