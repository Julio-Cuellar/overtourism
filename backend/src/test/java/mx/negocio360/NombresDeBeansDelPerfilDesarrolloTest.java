package mx.negocio360;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mx.negocio360.caja.infraestructura.CajaEnMemoriaConfiguracion;
import mx.negocio360.inventario.infraestructura.InventarioEnMemoriaConfiguracion;
import mx.negocio360.restaurantes.infraestructura.RestaurantesConfiguracion;
import mx.negocio360.ventas.infraestructura.VentasConfiguracion;
import mx.negocio360.ventas.infraestructura.VentasEnMemoriaConfiguracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

/**
 * El perfil {@code desarrollo} enciende a la vez los almacenes en memoria de Ventas, Caja, Inventario y Restaurantes. Spring
 * no permite que dos configuraciones registren un bean con el mismo nombre, aunque sean de tipos distintos: la aplicacion no
 * arranca ({@code BeanDefinitionOverrideException}). Ninguna prueba de contexto enciende los cuatro a la vez, asi que se
 * revisa aqui, sin base de datos: ningun nombre de bean puede repetirse entre ellas.
 */
class NombresDeBeansDelPerfilDesarrolloTest {

    private static final List<Class<?>> CONFIGURACIONES = List.of(VentasConfiguracion.class,
            VentasEnMemoriaConfiguracion.class, CajaEnMemoriaConfiguracion.class,
            InventarioEnMemoriaConfiguracion.class, RestaurantesConfiguracion.class);

    @Test
    @DisplayName("ningun nombre de bean se repite entre las configuraciones que el perfil desarrollo enciende juntas")
    void sinNombresRepetidos() {
        Map<String, List<String>> porNombre = new HashMap<>();
        for (Class<?> configuracion : CONFIGURACIONES) {
            for (Method metodo : configuracion.getDeclaredMethods()) {
                Bean bean = metodo.getAnnotation(Bean.class);
                if (bean == null) {
                    continue;
                }
                String[] nombres = bean.name().length > 0 ? bean.name() : bean.value();
                if (nombres.length == 0) {
                    nombres = new String[] {metodo.getName()};
                }
                for (String nombre : nombres) {
                    porNombre.computeIfAbsent(nombre, k -> new ArrayList<>()).add(configuracion.getSimpleName());
                }
            }
        }

        var repetidos = new HashMap<>(porNombre);
        repetidos.values().removeIf(donde -> donde.size() < 2);
        assertThat(repetidos).as("nombres de bean registrados por mas de una configuracion: %s", Arrays.asList(repetidos))
                .isEmpty();
    }
}
