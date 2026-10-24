package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mx.negocio360.compartido.seguridad.BearerTokenFilter;
import mx.negocio360.compartido.seguridad.LimiteDeConexionesWebSocketFilter;
import mx.negocio360.compartido.seguridad.LimiteDeTasaPorCuentaFilter;
import mx.negocio360.compartido.seguridad.LimiteDeTasaPorIpFilter;
import mx.negocio360.compartido.seguridad.PeticionGraphqlEstrictaFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * El orden de los filtros de /graphql importa: primero lo barato (la puerta HTTP), luego el limite por IP, despues la
 * autenticacion y el limite por cuenta (que necesita saber quien es), y al final la autorizacion.
 *
 * <p>El limite por IP cuenta las respuestas 401, y cuando no hay token ese 401 lo escribe ExceptionTranslationFilter al
 * recibir la excepcion de la autorizacion. Para ver esa respuesta, el filtro de IP tiene que envolver a
 * ExceptionTranslationFilter, es decir, ir antes que el. Spring Security ordena por clase y, a igual posicion, por orden
 * de registro: esta prueba lo fija para que nadie lo cambie sin verlo.
 */
@SpringBootTest
class OrdenDeFiltrosTest {

    @Autowired
    private FilterChainProxy cadena;

    @Autowired
    private org.springframework.boot.web.servlet.FilterRegistrationBean<mx.negocio360.identidad.web.FiltroDeEntrada> registroDelFiltroDeEntrada;

    @Test
    @DisplayName("puerta HTTP, tasa por IP, conexiones WebSocket, traduccion de excepciones, autenticacion, tasa por cuenta y autorizacion")
    void graphql_filtrosEnElOrdenCorrecto() {
        var clases = cadena.getFilters("/graphql").stream().map(filtro -> (Class<?>) filtro.getClass()).toList();

        var esperados = List.of(PeticionGraphqlEstrictaFilter.class, LimiteDeTasaPorIpFilter.class,
                LimiteDeConexionesWebSocketFilter.class, ExceptionTranslationFilter.class, BearerTokenFilter.class,
                LimiteDeTasaPorCuentaFilter.class, AuthorizationFilter.class);
        var posiciones = esperados.stream().map(clases::indexOf).toList();
        assertThat(posiciones).doesNotContain(-1);
        assertThat(posiciones).isSorted();
        // Contiguos los tres de antes y los tres de despues: nada se mete en medio.
        assertThat(posiciones.get(2) - posiciones.get(0)).isEqualTo(2);
        assertThat(posiciones.get(6) - posiciones.get(4)).isEqualTo(2);
    }

    @Autowired
    private org.springframework.boot.web.servlet.FilterRegistrationBean<mx.negocio360.compartido.seguridad.CorsDeGraphqlFilter> registroDeCors;

    @Test
    @DisplayName("QR-21: el CORS de /graphql corre antes que el filtro de entrada: contesta el prevuelo sin sesion y rechaza origenes ajenos antes de autenticar")
    void cors_correAntesQueElFiltroDeEntrada() {
        assertThat(registroDeCors.getOrder()).isLessThan(registroDelFiltroDeEntrada.getOrder());
        assertThat(registroDeCors.getUrlPatterns()).containsExactly("/graphql");
    }

    @Test
    @DisplayName("el filtro de entrada de /graphql corre antes que la cadena de Spring Security: ella comprueba que ya paso")
    void filtroDeEntrada_correAntesQueSpringSecurity() {
        assertThat(registroDelFiltroDeEntrada.getOrder())
                .isLessThan(org.springframework.boot.autoconfigure.security.SecurityProperties.DEFAULT_FILTER_ORDER);
        assertThat(registroDelFiltroDeEntrada.getUrlPatterns()).containsExactly("/graphql");
    }
}
