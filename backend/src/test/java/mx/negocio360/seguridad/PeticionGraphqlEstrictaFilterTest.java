package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import mx.negocio360.compartido.seguridad.PeticionGraphqlEstrictaFilter;
import mx.negocio360.compartido.seguridad.PropiedadesHttp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * SG-14 y SG-09: la puerta HTTP de /graphql. Solo POST, solo JSON en UTF-8, un solo objeto (sin lotes), un cuerpo
 * de hasta 64 KB y un origen de navegador solo si esta permitido. Todo se rechaza con una respuesta fija.
 */
class PeticionGraphqlEstrictaFilterTest {

    private static final int TOPE = 65_536;
    private static final String ORIGEN_BUENO = "https://panel.ejemplo.test";
    private static final String OBJETO = "{\"query\":\"{ __typename }\"}";

    private final AtomicBoolean cadenaInvocada = new AtomicBoolean();
    private final AtomicReference<byte[]> cuerpoVisto = new AtomicReference<>();
    private final FilterChain cadena = (peticion, respuesta) -> {
        cadenaInvocada.set(true);
        cuerpoVisto.set(peticion.getInputStream().readAllBytes());
    };
    private final PeticionGraphqlEstrictaFilter filtro = new PeticionGraphqlEstrictaFilter(
            new PropiedadesHttp(TOPE, List.of(ORIGEN_BUENO)));

    private static MockHttpServletRequest post(String tipo, byte[] cuerpo) {
        var peticion = new MockHttpServletRequest("POST", "/graphql");
        peticion.setServletPath("/graphql");
        if (tipo != null) {
            peticion.setContentType(tipo);
        }
        peticion.setContent(cuerpo);
        return peticion;
    }

    private static MockHttpServletRequest post(String cuerpo) {
        return post("application/json", cuerpo.getBytes(StandardCharsets.UTF_8));
    }

    private MockHttpServletResponse filtrar(MockHttpServletRequest peticion) throws Exception {
        var respuesta = new MockHttpServletResponse();
        filtro.doFilter(peticion, respuesta, cadena);
        return respuesta;
    }

    private void rechazada(MockHttpServletResponse respuesta, int estado, String cuerpo) throws Exception {
        assertThat(respuesta.getStatus()).isEqualTo(estado);
        assertThat(respuesta.getContentAsString()).isEqualTo(cuerpo);
        assertThat(respuesta.getContentType()).startsWith("application/json");
        assertThat(cadenaInvocada).isFalse();
    }

    @Test
    @DisplayName("un POST de JSON valido pasa y la cadena recibe exactamente el mismo cuerpo")
    void postValido_pasaConElMismoCuerpo() throws Exception {
        var respuesta = filtrar(post(OBJETO));

        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(cadenaInvocada).isTrue();
        assertThat(new String(cuerpoVisto.get(), StandardCharsets.UTF_8)).isEqualTo(OBJETO);
    }

    @Test
    @DisplayName("el cuerpo se puede leer como flujo y como lector, y la longitud anunciada es la real")
    void cuerpo_seLeeDeLasDosFormas() throws Exception {
        var visto = new AtomicReference<String>();
        var longitud = new AtomicReference<Long>();
        var leyendo = new MockHttpServletResponse();
        filtro.doFilter(post(OBJETO), leyendo, (peticion, respuesta) -> {
            longitud.set(peticion.getContentLengthLong());
            visto.set(new java.io.BufferedReader(peticion.getReader()).readLine());
        });

        assertThat(visto.get()).isEqualTo(OBJETO);
        assertThat(longitud.get()).isEqualTo(OBJETO.length());
    }

    @Test
    @DisplayName("el espacio en blanco antes del objeto es valido")
    void espacioAntesDelObjeto_pasa() throws Exception {
        filtrar(post("  \n\t " + OBJETO));

        assertThat(cadenaInvocada).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE"})
    @DisplayName("SG-14: cualquier metodo distinto de POST responde 405 con Allow: POST")
    void metodoDistintoDePost_responde405(String metodo) throws Exception {
        var peticion = new MockHttpServletRequest(metodo, "/graphql");
        peticion.setServletPath("/graphql");

        var respuesta = filtrar(peticion);

        rechazada(respuesta, 405, "{\"codigo\":\"METODO_NO_PERMITIDO\"}");
        assertThat(respuesta.getHeader("Allow")).isEqualTo("POST");
    }

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {"text/plain", "application/x-www-form-urlencoded", "application/graphql",
            "application/graphql+json", "application/json-patch+json", "multipart/form-data", "application/xml",
            "application/json;charset=iso-8859-1", "application/json;charset=utf-16"})
    @DisplayName("SG-14: solo application/json en UTF-8; cualquier otro tipo responde 415")
    void tipoDeContenidoNoPermitido_responde415(String tipo) throws Exception {
        rechazada(filtrar(post(tipo, OBJETO.getBytes(StandardCharsets.UTF_8))), 415,
                "{\"codigo\":\"TIPO_NO_SOPORTADO\"}");
    }

    @Test
    @DisplayName("SG-14: sin Content-Type tambien es 415")
    void sinTipoDeContenido_responde415() throws Exception {
        rechazada(filtrar(post(null, OBJETO.getBytes(StandardCharsets.UTF_8))), 415,
                "{\"codigo\":\"TIPO_NO_SOPORTADO\"}");
    }

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {"application/json", "application/json;charset=UTF-8", "Application/JSON; charset=utf-8"})
    @DisplayName("application/json pasa, con o sin charset UTF-8 y sin importar mayusculas")
    void tipoJson_pasa(String tipo) throws Exception {
        filtrar(post(tipo, OBJETO.getBytes(StandardCharsets.UTF_8)));

        assertThat(cadenaInvocada).isTrue();
    }

    @Test
    @DisplayName("SG-09: un cuerpo de exactamente 64 KB pasa; con un byte mas responde 413")
    void cuerpoEnElTope_pasa_yUnByteMasNo() throws Exception {
        var relleno = "x".repeat(TOPE - "{\"query\":\"\"}".length());
        var exacto = "{\"query\":\"" + relleno + "\"}";
        assertThat(exacto.length()).isEqualTo(TOPE);

        filtrar(post(exacto));
        assertThat(cadenaInvocada).isTrue();

        cadenaInvocada.set(false);
        rechazada(filtrar(post(exacto + " ")), 413, "{\"codigo\":\"CUERPO_MUY_GRANDE\"}");
    }

    @Test
    @DisplayName("SG-09: si el Content-Length anunciado ya excede el tope se rechaza sin leer el cuerpo")
    void contentLengthExcedido_seRechazaSinLeer() throws Exception {
        var leido = new AtomicBoolean();
        var peticion = new MockHttpServletRequest("POST", "/graphql") {
            @Override
            public long getContentLengthLong() {
                return TOPE + 1L;
            }

            @Override
            public ServletInputStream getInputStream() {
                leido.set(true);
                return super.getInputStream();
            }
        };
        peticion.setServletPath("/graphql");
        peticion.setContentType("application/json");

        rechazada(filtrar(peticion), 413, "{\"codigo\":\"CUERPO_MUY_GRANDE\"}");
        assertThat(leido).isFalse();
    }

    @Test
    @DisplayName("SG-09: un cuerpo sin Content-Length (chunked) se corta al pasar el tope, sin cargarlo entero")
    void cuerpoSinContentLength_seCortaAlPasarElTope() throws Exception {
        var enorme = new byte[TOPE * 40];
        java.util.Arrays.fill(enorme, (byte) 'x');
        enorme[0] = '{';
        var leidos = new java.util.concurrent.atomic.AtomicLong();
        var peticion = new MockHttpServletRequest("POST", "/graphql") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }

            @Override
            public int getContentLength() {
                return -1;
            }

            @Override
            public ServletInputStream getInputStream() {
                var origen = new java.io.ByteArrayInputStream(enorme);
                return new ServletInputStream() {
                    @Override
                    public int read() {
                        var b = origen.read();
                        if (b >= 0) {
                            leidos.incrementAndGet();
                        }
                        return b;
                    }

                    @Override
                    public boolean isFinished() {
                        return origen.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(jakarta.servlet.ReadListener listener) {
                    }
                };
            }
        };
        peticion.setServletPath("/graphql");
        peticion.setContentType("application/json");

        rechazada(filtrar(peticion), 413, "{\"codigo\":\"CUERPO_MUY_GRANDE\"}");
        assertThat(leidos.get()).isLessThanOrEqualTo(TOPE + 1L);
    }

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {"[{\"query\":\"{ __typename }\"}]", "[]", "\"texto\"", "123", "true", "null", "",
            "   ", "query { x }"})
    @DisplayName("SG-14: el cuerpo debe ser un solo objeto JSON; los lotes (arreglos) y todo lo demas responden 400")
    void cuerpoQueNoEsUnObjeto_responde400(String cuerpo) throws Exception {
        rechazada(filtrar(post(cuerpo)), 400, "{\"codigo\":\"PETICION_INVALIDA\"}");
    }

    @Test
    @DisplayName("SG-14: un origen que no esta en la lista responde 403, tambien 'null' y los casi iguales")
    void origenNoPermitido_responde403() throws Exception {
        for (var origen : List.of("https://malo.test", "null", "https://panel.ejemplo.test.malo.test",
                "http://panel.ejemplo.test", "https://panel.ejemplo.test:8443", "https://panel.ejemplo.test/x", "")) {
            var peticion = post(OBJETO);
            peticion.addHeader("Origin", origen);
            rechazada(filtrar(peticion), 403, "{\"codigo\":\"ORIGEN_NO_PERMITIDO\"}");
        }
    }

    @Test
    @DisplayName("SG-14: un origen permitido pasa, sin importar mayusculas; sin Origin (apps nativas) tambien")
    void origenPermitidoOAusente_pasa() throws Exception {
        var conOrigen = post(OBJETO);
        conOrigen.addHeader("Origin", "HTTPS://Panel.Ejemplo.test");
        filtrar(conOrigen);
        assertThat(cadenaInvocada).isTrue();

        cadenaInvocada.set(false);
        filtrar(post(OBJETO));
        assertThat(cadenaInvocada).isTrue();
    }

    @Test
    @DisplayName("SG-14: dos cabeceras Origin son ambiguas y se rechazan")
    void dosOrigenes_responde403() throws Exception {
        var peticion = post(OBJETO);
        peticion.addHeader("Origin", ORIGEN_BUENO);
        peticion.addHeader("Origin", ORIGEN_BUENO);

        rechazada(filtrar(peticion), 403, "{\"codigo\":\"ORIGEN_NO_PERMITIDO\"}");
    }

    @Test
    @DisplayName("sin ningun origen configurado, cualquier peticion que traiga Origin se rechaza")
    void sinOrigenesConfigurados_rechazaTodoOrigen() throws Exception {
        var estricto = new PeticionGraphqlEstrictaFilter(new PropiedadesHttp(TOPE, List.of()));
        var peticion = post(OBJETO);
        peticion.addHeader("Origin", ORIGEN_BUENO);
        var respuesta = new MockHttpServletResponse();

        estricto.doFilter(peticion, respuesta, cadena);

        assertThat(respuesta.getStatus()).isEqualTo(403);
        assertThat(cadenaInvocada).isFalse();
    }

    @Test
    @DisplayName("otras rutas no se tocan: la puerta es solo de /graphql")
    void otraRuta_noSeTocaNiSeLeeElCuerpo() throws Exception {
        var peticion = new MockHttpServletRequest("GET", "/auth/yo");
        peticion.setServletPath("/auth/yo");

        var respuesta = filtrar(peticion);

        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(cadenaInvocada).isTrue();
    }
}
