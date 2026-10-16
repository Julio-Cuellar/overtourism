package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import mx.negocio360.compartido.seguridad.LimitadorDeTasa;
import mx.negocio360.compartido.seguridad.LimiteDeTasaPorCuentaFilter;
import mx.negocio360.compartido.seguridad.LimiteDeTasaPorIpFilter;
import mx.negocio360.compartido.seguridad.SesionAuthentication;
import mx.negocio360.compartido.seguridad.SesionDePeticion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SG-10: los dos filtros de tasa de /graphql. Por IP se cuentan los intentos sin sesion valida (las respuestas 401), no
 * todas las peticiones; por cuenta, cada peticion con sesion. Se rechaza con 429 y el tiempo de espera.
 */
class LimiteDeTasaFiltrosTest {

    private static final String CUERPO_429 = "{\"codigo\":\"DEMASIADAS_SOLICITUDES\"}";

    private final RelojManual reloj = new RelojManual(Instant.parse("2026-10-16T12:00:00Z"));
    private final AtomicInteger cadenaInvocada = new AtomicInteger();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest a(String ruta, String ip) {
        var peticion = new MockHttpServletRequest("POST", ruta);
        peticion.setServletPath(ruta);
        peticion.setRemoteAddr(ip);
        return peticion;
    }

    private FilterChain respondiendo(int estado) {
        return (peticion, respuesta) -> {
            cadenaInvocada.incrementAndGet();
            ((HttpServletResponse) respuesta).setStatus(estado);
        };
    }

    // --- Por IP: intentos sin sesion valida ---

    private LimiteDeTasaPorIpFilter porIp() {
        return new LimiteDeTasaPorIpFilter(new LimitadorDeTasa(20, Duration.ofMinutes(1), 1_000, reloj));
    }

    private MockHttpServletResponse pasar(LimiteDeTasaPorIpFilter filtro, String ip, int estado) throws Exception {
        var respuesta = new MockHttpServletResponse();
        filtro.doFilter(a("/graphql", ip), respuesta, respondiendo(estado));
        return respuesta;
    }

    @Test
    @DisplayName("20 intentos fallidos por minuto pasan; el 21 recibe 429 con el tiempo de espera y no corre la cadena")
    void porIp_veinteFallosYElSiguienteSeNiega() throws Exception {
        var filtro = porIp();
        for (var i = 0; i < 20; i++) {
            assertThat(pasar(filtro, "10.0.0.1", 401).getStatus()).isEqualTo(401);
        }
        cadenaInvocada.set(0);

        var respuesta = pasar(filtro, "10.0.0.1", 401);

        assertThat(respuesta.getStatus()).isEqualTo(429);
        assertThat(respuesta.getContentAsString()).isEqualTo(CUERPO_429);
        assertThat(respuesta.getContentType()).startsWith("application/json");
        assertThat(respuesta.getHeader("Retry-After")).isEqualTo("3");
        assertThat(cadenaInvocada).hasValue(0);
    }

    @Test
    @DisplayName("las peticiones que salen bien no gastan fichas: solo cuentan los 401")
    void porIp_losExitosNoCuentan() throws Exception {
        var filtro = porIp();

        for (var i = 0; i < 100; i++) {
            assertThat(pasar(filtro, "10.0.0.2", 200).getStatus()).isEqualTo(200);
        }
        assertThat(pasar(filtro, "10.0.0.2", 401).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("otros rechazos (403, 400...) tampoco cuentan; una IP no afecta a otra")
    void porIp_soloLos401Cuentan_yCadaIpEsIndependiente() throws Exception {
        var filtro = porIp();
        for (var i = 0; i < 50; i++) {
            pasar(filtro, "10.0.0.3", 403);
            pasar(filtro, "10.0.0.3", 400);
        }
        for (var i = 0; i < 20; i++) {
            pasar(filtro, "10.0.0.4", 401);
        }

        assertThat(pasar(filtro, "10.0.0.3", 401).getStatus()).isEqualTo(401);
        assertThat(pasar(filtro, "10.0.0.4", 401).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("pasado el tiempo se recupera: 3 segundos devuelven una ficha de las 20 por minuto")
    void porIp_conElTiempoSeRecupera() throws Exception {
        var filtro = porIp();
        for (var i = 0; i < 20; i++) {
            pasar(filtro, "10.0.0.5", 401);
        }
        assertThat(pasar(filtro, "10.0.0.5", 401).getStatus()).isEqualTo(429);

        reloj.avanzar(Duration.ofSeconds(3));

        assertThat(pasar(filtro, "10.0.0.5", 401).getStatus()).isEqualTo(401);
        assertThat(pasar(filtro, "10.0.0.5", 401).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("solo se aplica a /graphql")
    void porIp_otraRutaNoSeToca() throws Exception {
        var filtro = porIp();
        var respuesta = new MockHttpServletResponse();
        for (var i = 0; i < 100; i++) {
            filtro.doFilter(a("/auth/yo", "10.0.0.6"), respuesta, respondiendo(401));
        }

        assertThat(cadenaInvocada).hasValue(100);
        assertThat(respuesta.getStatus()).isEqualTo(401);
    }

    // --- Por cuenta ---

    private LimiteDeTasaPorCuentaFilter porCuenta() {
        return new LimiteDeTasaPorCuentaFilter(new LimitadorDeTasa(60, Duration.ofMinutes(1), 1_000, reloj));
    }

    private static void entraComo(String actor) {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new SesionAuthentication(new SesionDePeticion(actor, UUID.randomUUID(), Set.of())));
        SecurityContextHolder.setContext(contexto);
    }

    private MockHttpServletResponse pasarCuenta(LimiteDeTasaPorCuentaFilter filtro, String actor) throws Exception {
        entraComo(actor);
        var respuesta = new MockHttpServletResponse();
        filtro.doFilter(a("/graphql", "10.1.1.1"), respuesta, respondiendo(200));
        return respuesta;
    }

    @Test
    @DisplayName("60 peticiones por minuto de una cuenta pasan; la 61 recibe 429 con Retry-After")
    void porCuenta_sesentaYLaSiguienteSeNiega() throws Exception {
        var filtro = porCuenta();
        for (var i = 0; i < 60; i++) {
            assertThat(pasarCuenta(filtro, "cuenta-1").getStatus()).isEqualTo(200);
        }
        cadenaInvocada.set(0);

        var respuesta = pasarCuenta(filtro, "cuenta-1");

        assertThat(respuesta.getStatus()).isEqualTo(429);
        assertThat(respuesta.getContentAsString()).isEqualTo(CUERPO_429);
        assertThat(respuesta.getHeader("Retry-After")).isEqualTo("1");
        assertThat(cadenaInvocada).hasValue(0);
    }

    @Test
    @DisplayName("cada cuenta tiene su cubeta, aunque compartan IP")
    void porCuenta_cadaCuentaEsIndependiente() throws Exception {
        var filtro = porCuenta();
        for (var i = 0; i < 60; i++) {
            pasarCuenta(filtro, "cuenta-1");
        }

        assertThat(pasarCuenta(filtro, "cuenta-1").getStatus()).isEqualTo(429);
        assertThat(pasarCuenta(filtro, "cuenta-2").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("sin sesion el filtro por cuenta no cuenta nada y deja pasar (la IP y la autenticacion deciden)")
    void porCuenta_sinSesion_dejaPasarSinContar() throws Exception {
        var filtro = porCuenta();
        var respuesta = new MockHttpServletResponse();
        for (var i = 0; i < 200; i++) {
            filtro.doFilter(a("/graphql", "10.1.1.2"), respuesta, respondiendo(200));
        }

        assertThat(cadenaInvocada).hasValue(200);
    }

    @Test
    @DisplayName("la cuenta recupera una ficha por segundo")
    void porCuenta_conElTiempoSeRecupera() throws Exception {
        var filtro = porCuenta();
        for (var i = 0; i < 60; i++) {
            pasarCuenta(filtro, "cuenta-3");
        }
        assertThat(pasarCuenta(filtro, "cuenta-3").getStatus()).isEqualTo(429);

        reloj.avanzar(Duration.ofSeconds(2));

        assertThat(pasarCuenta(filtro, "cuenta-3").getStatus()).isEqualTo(200);
        assertThat(pasarCuenta(filtro, "cuenta-3").getStatus()).isEqualTo(200);
        assertThat(pasarCuenta(filtro, "cuenta-3").getStatus()).isEqualTo(429);
    }
}
