package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import mx.negocio360.identidad.CuentaVista;
import mx.negocio360.identidad.NegocioVista;
import mx.negocio360.identidad.PerfilVista;
import mx.negocio360.identidad.SesionIniciada;
import mx.negocio360.identidad.aplicacion.PlantillasIdentidad;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.IdentidadException;
import mx.negocio360.identidad.web.ErroresIdentidadAdvice.ErrorRespuesta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

/** SI-14 y SI-15: estado y texto fijo de cada error, errores inesperados sin filtrar datos, y la forma de las respuestas. */
class ErroresYRespuestasTest {

    private final ErroresIdentidadAdvice advice = new ErroresIdentidadAdvice();

    static Stream<Arguments> estados() {
        return Stream.of(
                arguments(CodigoError.TELEFONO_INVALIDO, 400), arguments(CodigoError.NOMBRE_INVALIDO, 400),
                arguments(CodigoError.SOLICITUD_INVALIDA, 400), arguments(CodigoError.NOMBRE_REQUERIDO, 422),
                arguments(CodigoError.CODIGO_INVALIDO, 401), arguments(CodigoError.CODIGO_EXPIRADO, 401),
                arguments(CodigoError.CODIGO_AGOTADO, 401), arguments(CodigoError.NO_AUTENTICADO, 401),
                arguments(CodigoError.TASA_EXCEDIDA, 429), arguments(CodigoError.ERROR_TEMPORAL, 503),
                arguments(CodigoError.ERROR_INTERNO, 500));
    }

    @Test
    @DisplayName("la tabla de estados cubre todos los codigos de error, sin olvidar ninguno")
    void estados_cubreTodosLosCodigos() {
        var cubiertos = estados().map(a -> (CodigoError) a.get()[0]).toList();

        assertThat(cubiertos).containsExactlyInAnyOrder(CodigoError.values());
    }

    @ParameterizedTest
    @MethodSource("estados")
    @DisplayName("cada codigo de error responde con su estado HTTP y su texto fijo")
    void deNegocio_cadaCodigo_tieneSuEstadoYSuTexto(CodigoError codigo, int estado) {
        var respuesta = advice.deNegocio(new IdentidadException(codigo));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(estado);
        assertThat(respuesta.getBody()).isEqualTo(new ErrorRespuesta(codigo.name(), PlantillasIdentidad.texto(codigo)));
    }

    @Test
    @DisplayName("solo NO_AUTENTICADO lleva WWW-Authenticate: Bearer")
    void deNegocio_soloNoAutenticadoLlevaEncabezadoDeAutenticacion() {
        for (var codigo : CodigoError.values()) {
            var encabezado = advice.deNegocio(new IdentidadException(codigo)).getHeaders().getFirst("WWW-Authenticate");

            assertThat(encabezado).isEqualTo(codigo == CodigoError.NO_AUTENTICADO ? "Bearer" : null);
        }
    }

    @Test
    @DisplayName("un error inesperado responde 500 sin su mensaje y solo registra el nombre de la clase")
    void inesperado_noFiltraElMensajeNiAlCuerpoNiAlRegistro() {
        var registro = (Logger) LoggerFactory.getLogger(ErroresIdentidadAdvice.class);
        var eventos = new ListAppender<ILoggingEvent>();
        eventos.start();
        registro.addAppender(eventos);
        try {
            var respuesta = advice.inesperado(new IllegalStateException("telefono 2225550000 codigo 123456"));

            assertThat(respuesta.getStatusCode().value()).isEqualTo(500);
            assertThat(respuesta.getBody().codigo()).isEqualTo("ERROR_INTERNO");
            assertThat(respuesta.getBody().toString()).doesNotContain("2225550000").doesNotContain("123456");
            assertThat(eventos.list).hasSize(1);
            assertThat(eventos.list.get(0).getFormattedMessage()).contains("IllegalStateException")
                    .doesNotContain("2225550000").doesNotContain("123456");
        } finally {
            registro.detachAppender(eventos);
        }
    }

    @Test
    @DisplayName("un cuerpo ilegible responde 400 y un tipo de contenido no soportado responde 415, sin repetir lo recibido")
    void cuerpoIlegibleYTipoNoSoportado_respondenSinEco() {
        var ilegible = advice.cuerpoIlegible(
                new HttpMessageNotReadableException("JSON con 2225550000", new MockHttpInputMessage(new byte[0])));
        var noSoportado = advice.tipoNoSoportado(new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN,
                List.of(MediaType.APPLICATION_JSON)));

        assertThat(ilegible.getStatusCode().value()).isEqualTo(400);
        assertThat(noSoportado.getStatusCode().value()).isEqualTo(415);
        assertThat(Arrays.asList(ilegible.getBody(), noSoportado.getBody())).allSatisfy(cuerpo -> {
            assertThat(cuerpo.codigo()).isEqualTo("SOLICITUD_INVALIDA");
            assertThat(cuerpo.mensaje()).isEqualTo(PlantillasIdentidad.texto(CodigoError.SOLICITUD_INVALIDA));
        });
    }

    @Test
    @DisplayName("las respuestas de sesion y perfil traducen los negocios cuando los haya, preparado para IDN-2")
    void respuestas_conNegocios_losTraducen() {
        var negocioId = UUID.randomUUID();
        var cuenta = new CuentaVista(UUID.randomUUID(), "Ana López", "+522221110001");
        var negocios = List.of(new NegocioVista(negocioId, "Fonda de ejemplo", "CAJERO"));
        var sesion = new SesionIniciada("token-de-prueba", Instant.parse("2026-10-14T16:00:00Z"), cuenta, negocioId, negocios);

        var respuestaSesion = AuthController.RespuestaSesion.de(sesion);
        var respuestaPerfil = AuthController.RespuestaPerfil.de(new PerfilVista(cuenta, negocioId, negocios));

        assertThat(respuestaSesion.negocioActivo()).isEqualTo(negocioId);
        assertThat(respuestaSesion.negocios()).singleElement()
                .satisfies(n -> assertThat(n.rol()).isEqualTo("CAJERO"));
        assertThat(respuestaPerfil.negocios()).singleElement()
                .satisfies(n -> assertThat(n.nombre()).isEqualTo("Fonda de ejemplo"));
    }

    @Test
    @DisplayName("SI-15: ningun DTO ni vista revela token, nombre ni telefono en su toString")
    void toString_dtosYVistas_noRevelanDatos() {
        var cuenta = new CuentaVista(UUID.randomUUID(), "Ana López", "+522221110001");
        var sesion = new SesionIniciada("token-de-prueba", Instant.now(), cuenta, null, List.of());
        var negocio = new NegocioVista(UUID.randomUUID(), "Fonda Secreta", "CAJERO");
        var textos = List.of(
                cuenta.toString(),
                negocio.toString(),
                AuthController.RespuestaNegocio.de(negocio).toString(),
                new PerfilVista(cuenta, null, List.of(negocio)).toString(),
                AuthController.RespuestaSesion.de(sesion).toString(),
                AuthController.RespuestaPerfil.de(new PerfilVista(cuenta, null, List.of())).toString(),
                AuthController.RespuestaCuenta.de(cuenta).toString(),
                new AuthController.SolicitudCodigo("2221110001").toString(),
                new AuthController.SolicitudSesion("2221110001", "123456", "Ana López").toString(),
                new AuthController.SolicitudNombre("Ana López").toString(),
                new DevCanalController.RespuestaCodigo("123456").toString());

        assertThat(textos).allSatisfy(texto -> assertThat(texto).doesNotContain("token-de-prueba")
                .doesNotContain("Ana").doesNotContain("2221110001").doesNotContain("123456")
                .doesNotContain("Fonda Secreta").doesNotContain("CAJERO"));
    }
}
