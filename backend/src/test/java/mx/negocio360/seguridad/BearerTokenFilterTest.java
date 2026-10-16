package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.FilterChain;
import mx.negocio360.compartido.seguridad.Autenticador;
import mx.negocio360.compartido.seguridad.BearerTokenFilter;
import mx.negocio360.compartido.seguridad.EntradaNoAutenticada;
import mx.negocio360.compartido.seguridad.SesionAuthentication;
import mx.negocio360.compartido.seguridad.SesionDePeticion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** SG-01: el filtro de Bearer. Todo token que no sirva recibe la misma respuesta (AS-08), sin consultar de mas. */
class BearerTokenFilterTest {

    private static final UUID NEGOCIO = UUID.randomUUID();

    private final AtomicInteger consultas = new AtomicInteger();
    private final AtomicReference<String> ultimoToken = new AtomicReference<>();
    private final AtomicBoolean cadenaInvocada = new AtomicBoolean();
    private final AtomicReference<Authentication> autenticacionVista = new AtomicReference<>();

    private final Autenticador autenticador = token -> {
        consultas.incrementAndGet();
        ultimoToken.set(token);
        return "token-bueno".equals(token)
                ? Optional.of(new SesionDePeticion("cuenta-1", NEGOCIO, Set.of("clientes.sugerir")))
                : Optional.empty();
    };
    private final BearerTokenFilter filtro = new BearerTokenFilter(autenticador, List.of("/auth/**", "/registro/**"));
    private final FilterChain cadena = (peticion, respuesta) -> {
        cadenaInvocada.set(true);
        autenticacionVista.set(SecurityContextHolder.getContext().getAuthentication());
    };

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse filtrar(MockHttpServletRequest peticion) throws Exception {
        var respuesta = new MockHttpServletResponse();
        filtro.doFilter(peticion, respuesta, cadena);
        return respuesta;
    }

    private static MockHttpServletRequest a(String ruta, String... autorizaciones) {
        var peticion = new MockHttpServletRequest("POST", ruta);
        peticion.setServletPath(ruta);
        for (var valor : autorizaciones) {
            peticion.addHeader("Authorization", valor);
        }
        return peticion;
    }

    @Test
    @DisplayName("un token bueno deja la sesion en el contexto de seguridad mientras corre la cadena")
    void tokenBueno_dejaLaSesionParaLaCadena() throws Exception {
        var respuesta = filtrar(a("/graphql", "Bearer token-bueno"));

        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(cadenaInvocada).isTrue();
        assertThat(autenticacionVista.get()).isInstanceOf(SesionAuthentication.class);
        var sesion = ((SesionAuthentication) autenticacionVista.get()).sesion();
        assertThat(sesion.actor()).isEqualTo("cuenta-1");
        assertThat(sesion.negocioId()).isEqualTo(NEGOCIO);
        assertThat(autenticacionVista.get().getAuthorities()).extracting(Object::toString)
                .containsExactly("clientes.sugerir");
    }

    @Test
    @DisplayName("el esquema Bearer no distingue mayusculas, como pide el estandar")
    void esquemaBearer_enMinusculas_sirve() throws Exception {
        filtrar(a("/graphql", "bearer token-bueno"));

        assertThat(autenticacionVista.get()).isInstanceOf(SesionAuthentication.class);
    }

    @Test
    @DisplayName("sin cabecera pasa sin sesion: la cadena de filtros decide con 401 (SG-01)")
    void sinCabecera_pasaSinSesion() throws Exception {
        var respuesta = filtrar(a("/graphql"));

        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(cadenaInvocada).isTrue();
        assertThat(autenticacionVista.get()).isNull();
        assertThat(consultas).hasValue(0);
    }

    @Test
    @DisplayName("un token que Identidad no reconoce (falso, vencido o revocado) recibe el 401 fijo y no corre la cadena")
    void tokenDesconocido_responde401Fijo() throws Exception {
        var respuesta = filtrar(a("/graphql", "Bearer token-falso"));

        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getContentAsString()).isEqualTo(EntradaNoAutenticada.CUERPO);
        assertThat(respuesta.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(cadenaInvocada).isFalse();
        assertThat(consultas).hasValue(1);
        assertThat(ultimoToken).hasValue("token-falso");
    }

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {"Basic dXN1YXJpbzpjbGF2ZQ==", "Bearer", "Bearer   ", "Bearer a b", "Token token-bueno",
            "Bearertoken-bueno", " "})
    @DisplayName("una cabecera mal formada recibe el mismo 401 sin siquiera preguntarle a Identidad")
    void cabeceraMalFormada_responde401SinConsultar(String cabecera) throws Exception {
        var respuesta = filtrar(a("/graphql", cabecera));

        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getContentAsString()).isEqualTo(EntradaNoAutenticada.CUERPO);
        assertThat(cadenaInvocada).isFalse();
        assertThat(consultas).hasValue(0);
    }

    @Test
    @DisplayName("dos cabeceras Authorization son ambiguas: 401 sin consultar")
    void dosCabeceras_responde401() throws Exception {
        var respuesta = filtrar(a("/graphql", "Bearer token-bueno", "Bearer token-bueno"));

        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(consultas).hasValue(0);
    }

    @Test
    @DisplayName("un token absurdamente largo se rechaza sin consultar (los reales miden 43 caracteres)")
    void tokenLargo_responde401SinConsultar() throws Exception {
        var respuesta = filtrar(a("/graphql", "Bearer " + "x".repeat(257)));

        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(consultas).hasValue(0);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/auth/yo", "/auth/sesion", "/registro/verificacion"})
    @DisplayName("las rutas publicas no se tocan: validan su propio token y el filtro no las interpreta")
    void rutaPublica_noSeFiltra(String ruta) throws Exception {
        var respuesta = filtrar(a(ruta, "Bearer token-falso"));

        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(cadenaInvocada).isTrue();
        assertThat(consultas).hasValue(0);
    }

    @Test
    @DisplayName("cada peticion arranca sin la sesion de la anterior")
    void sesion_noSeFiltraEntrePeticiones() throws Exception {
        filtrar(a("/graphql", "Bearer token-bueno"));
        SecurityContextHolder.clearContext();
        autenticacionVista.set(null);

        filtrar(a("/graphql"));

        assertThat(autenticacionVista.get()).isNull();
    }
}
