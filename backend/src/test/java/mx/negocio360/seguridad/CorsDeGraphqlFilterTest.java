package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import mx.negocio360.compartido.seguridad.CorsDeGraphqlFilter;
import mx.negocio360.compartido.seguridad.LimitadorDeTasa;
import mx.negocio360.compartido.seguridad.PropiedadesHttp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * QR-21 y D-211: CORS acotado a la lista de origenes de {@code seguridad.http.origenes-permitidos} para que la web publica del
 * comensal llame a {@code /graphql} desde otro origen. Un origen que no esta en la lista no recibe ninguna cabecera CORS y se
 * rechaza antes de autenticar (403); el prevuelo de uno permitido se contesta aqui, sin sesion, con tope por IP. No hay cookies
 * ni credenciales: el token viaja en Authorization (SI-05).
 */
class CorsDeGraphqlFilterTest {

    private static final String WEB = "https://web.servicial.test";

    private final FilterChain cadena = mock(FilterChain.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-24T18:00:00Z"), ZoneOffset.UTC);
    private final CorsDeGraphqlFilter filtro = new CorsDeGraphqlFilter(
            new PropiedadesHttp(null, List.of(WEB, "https://otra.servicial.test:8443")),
            new LimitadorDeTasa(3, Duration.ofMinutes(1), 100, reloj));

    private static MockHttpServletRequest peticion(String metodo) {
        var p = new MockHttpServletRequest(metodo, "/graphql");
        p.setRemoteAddr("10.0.0.1");
        return p;
    }

    private static MockHttpServletRequest prevuelo(String origen) {
        var p = peticion("OPTIONS");
        p.addHeader("Origin", origen);
        p.addHeader("Access-Control-Request-Method", "POST");
        p.addHeader("Access-Control-Request-Headers", "content-type, authorization, idempotency-key");
        return p;
    }

    private MockHttpServletResponse ejecutar(MockHttpServletRequest p) throws Exception {
        var r = new MockHttpServletResponse();
        filtro.doFilter(p, r, cadena);
        return r;
    }

    @Test
    @DisplayName("sin Origin (apps nativas, pruebas, servidor a servidor) la peticion sigue igual y no recibe cabeceras CORS")
    void sinOrigen() throws Exception {
        var p = peticion("POST");

        var r = ejecutar(p);

        verify(cadena).doFilter(p, r);
        assertThat(r.getHeaderNames()).doesNotContain("Access-Control-Allow-Origin", "Vary");
    }

    @Test
    @DisplayName("un origen permitido en un POST pasa y la respuesta lleva su origen, Vary y los encabezados que el navegador puede leer")
    void postDeOrigenPermitido() throws Exception {
        var p = peticion("POST");
        p.addHeader("Origin", WEB);

        var r = ejecutar(p);

        verify(cadena).doFilter(p, r);
        assertThat(r.getHeader("Access-Control-Allow-Origin")).isEqualTo(WEB);
        assertThat(r.getHeaders("Vary")).contains("Origin");
        assertThat(r.getHeader("Access-Control-Expose-Headers")).isEqualTo("Retry-After");
        assertThat(r.getHeader("Access-Control-Allow-Credentials")).isNull();
    }

    @Test
    @DisplayName("el prevuelo de un origen permitido se contesta 204 con metodo, encabezados y vigencia, sin llegar a ningun otro filtro")
    void prevueloPermitido() throws Exception {
        var p = prevuelo(WEB);

        var r = ejecutar(p);

        assertThat(r.getStatus()).isEqualTo(204);
        verify(cadena, never()).doFilter(any(), any());
        assertThat(r.getHeader("Access-Control-Allow-Origin")).isEqualTo(WEB);
        assertThat(r.getHeader("Access-Control-Allow-Methods")).isEqualTo("POST");
        assertThat(r.getHeader("Access-Control-Allow-Headers"))
                .isEqualTo("Authorization, Content-Type, Idempotency-Key, X-Dispositivo-Id");
        assertThat(r.getHeader("Access-Control-Max-Age")).isEqualTo("600");
        assertThat(r.getHeaders("Vary")).contains("Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers");
        assertThat(r.getHeader("Access-Control-Allow-Credentials")).isNull();
        assertThat(r.getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("un origen con puerto se compara completo y sin importar mayusculas")
    void origenConPuerto() throws Exception {
        assertThat(ejecutar(prevuelo("https://OTRA.servicial.test:8443")).getStatus()).isEqualTo(204);
        assertThat(ejecutar(prevuelo("https://otra.servicial.test")).getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("un origen que no esta en la lista se rechaza con 403 antes de autenticar, tambien el prevuelo, sin cabeceras CORS")
    void origenAjeno() throws Exception {
        var post = peticion("POST");
        post.addHeader("Origin", "https://malo.test");

        var rPost = ejecutar(post);
        var rPrevuelo = ejecutar(prevuelo("https://malo.test"));

        for (var r : List.of(rPost, rPrevuelo)) {
            assertThat(r.getStatus()).isEqualTo(403);
            assertThat(r.getContentAsString()).isEqualTo("{\"codigo\":\"ORIGEN_NO_PERMITIDO\"}");
            assertThat(r.getHeader("Access-Control-Allow-Origin")).isNull();
        }
        verify(cadena, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Origin repetido, o nulo ('null' de un archivo local), se rechaza")
    void origenRaro() throws Exception {
        var repetido = peticion("POST");
        repetido.addHeader("Origin", WEB);
        repetido.addHeader("Origin", WEB);
        var nulo = peticion("POST");
        nulo.addHeader("Origin", "null");

        assertThat(ejecutar(repetido).getStatus()).isEqualTo(403);
        assertThat(ejecutar(nulo).getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("un prevuelo que pide otro metodo u otro encabezado se rechaza con 403 y sin cabeceras CORS")
    void prevueloNoPermitido() throws Exception {
        var otroMetodo = prevuelo(WEB);
        otroMetodo.removeHeader("Access-Control-Request-Method");
        otroMetodo.addHeader("Access-Control-Request-Method", "DELETE");
        var otroEncabezado = prevuelo(WEB);
        otroEncabezado.removeHeader("Access-Control-Request-Headers");
        otroEncabezado.addHeader("Access-Control-Request-Headers", "content-type, x-secreto");

        for (var p : List.of(otroMetodo, otroEncabezado)) {
            var r = ejecutar(p);
            assertThat(r.getStatus()).isEqualTo(403);
            assertThat(r.getContentAsString()).isEqualTo("{\"codigo\":\"PREVUELO_NO_PERMITIDO\"}");
            assertThat(r.getHeader("Access-Control-Allow-Origin")).isNull();
        }
    }

    @Test
    @DisplayName("sin origenes configurados toda peticion con Origin se rechaza")
    void sinOrigenesConfigurados() throws Exception {
        var sinLista = new CorsDeGraphqlFilter(new PropiedadesHttp(null, null),
                new LimitadorDeTasa(3, Duration.ofMinutes(1), 100, reloj));
        var r = new MockHttpServletResponse();

        sinLista.doFilter(prevuelo(WEB), r, cadena);

        assertThat(r.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("SG-10: los prevuelos de una IP tienen tope por minuto: el cuarto recibe 429 con Retry-After")
    void prevueloConTope() throws Exception {
        for (var i = 0; i < 3; i++) {
            assertThat(ejecutar(prevuelo(WEB)).getStatus()).isEqualTo(204);
        }

        var r = ejecutar(prevuelo(WEB));

        assertThat(r.getStatus()).isEqualTo(429);
        assertThat(r.getHeader("Retry-After")).isNotBlank();
    }

    @Test
    @DisplayName("solo actua sobre /graphql")
    void soloGraphql() throws Exception {
        var p = new MockHttpServletRequest("OPTIONS", "/auth/yo");
        p.addHeader("Origin", "https://malo.test");
        var r = new MockHttpServletResponse();

        filtro.doFilter(p, r, cadena);

        verify(cadena).doFilter(p, r);
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
