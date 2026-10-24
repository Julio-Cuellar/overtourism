package mx.negocio360.compartido.almacen;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * Guarda del perfil {@code produccion} (H-15): Ventas, Caja e Inventario solo se encienden si su propiedad
 * {@code <modulo>.almacen} lo pide, y sin ella la plataforma arrancaria sin esos modulos y sin avisar. En produccion cada
 * uno debe valer {@code postgres}; {@code memoria} se rechaza tambien, porque se perderia todo al reiniciar. Si falta o
 * es otro valor, la aplicacion no arranca y el mensaje dice que propiedad definir.
 *
 * <p>Fuera de {@code produccion} esta clase no se carga: desarrollo puede usar {@code memoria} o no tener los modulos.
 */
@Configuration(proxyBeanMethods = false)
@Profile("produccion")
public class AlmacenesObligatoriosEnProduccion {

    /** Propiedad del almacen de cada modulo que guarda dinero o existencias. */
    static final List<String> PROPIEDADES = List.of("ventas.almacen", "caja.almacen", "inventario.almacen");

    private static final String POSTGRES = "postgres";

    /**
     * Comprueba los almacenes al crear la configuracion.
     *
     * @param entorno entorno de Spring
     * @throws IllegalStateException si algun almacen falta o no es {@code postgres}
     */
    public AlmacenesObligatoriosEnProduccion(Environment entorno) {
        var pendientes = PROPIEDADES.stream()
                .filter(propiedad -> !POSTGRES.equalsIgnoreCase(entorno.getProperty(propiedad, "").strip()))
                .map(propiedad -> propiedad + "=" + POSTGRES + " (hoy: "
                        + describir(entorno.getProperty(propiedad)) + ")")
                .toList();
        if (!pendientes.isEmpty()) {
            throw new IllegalStateException("Con el perfil produccion cada modulo necesita un almacen persistente; "
                    + "defina: " + String.join(", ", pendientes));
        }
    }

    private static String describir(String valor) {
        return valor == null || valor.isBlank() ? "sin definir" : valor.strip();
    }
}
