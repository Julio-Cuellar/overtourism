package mx.negocio360.clientes.aplicacion;

import static mx.negocio360.clientes.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.fakes.Escenario;
import mx.negocio360.clientes.fakes.Fixtures;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** HTC-29 · D-23, D-55, SC-10: patron preparar -> confirmar con token de un solo uso. */
class PreparacionTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("preparar devuelve un token opaco, el resumen y cinco minutos de vigencia")
    void preparar_comandoValido_devuelveTokenResumenYVigencia() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.avisoValido());

        assertThat(preparacion.token()).matches("[A-Za-z0-9_-]{43}");
        assertThat(preparacion.resumen()).isNotBlank();
        assertThat(preparacion.expiraEn()).isEqualTo(Fixtures.AHORA.plus(Duration.ofMinutes(5)));
        assertThat(preparacion.advertencias()).isEmpty();
    }

    @Test
    @DisplayName("dos preparaciones iguales producen tokens distintos")
    void preparar_dosVeces_tokensDistintos() {
        var primera = e.api.preparar(e.ctxA, Fixtures.avisoValido());
        var segunda = e.api.preparar(e.ctxA, Fixtures.avisoValido());

        assertThat(primera.token()).isNotEqualTo(segunda.token());
    }

    @Test
    @DisplayName("solo se guarda el hash del token, ligado al negocio, al actor y al comando")
    void preparar_comandoValido_guardaSoloElHashDelToken() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.avisoValido());

        var fila = e.preparaciones.unica();
        assertThat(fila.tokenHash()).matches("[0-9a-f]{64}").isNotEqualTo(preparacion.token());
        assertThat(fila.comandoHash()).matches("[0-9a-f]{64}");
        assertThat(fila.comando()).isNotBlank().doesNotContain(preparacion.token());
        assertThat(fila.negocioId()).isEqualTo(e.negocioA);
        assertThat(fila.actor()).isEqualTo("locatario-a");
        assertThat(fila.comandoTipo()).isEqualTo("publicar_aviso_privacidad");
        assertThat(fila.expiraEn()).isEqualTo(preparacion.expiraEn());
        assertThat(fila.usadoEn()).isNull();
    }

    @Test
    @DisplayName("confirmar con el token ejecuta el comando una vez y responde OK")
    void confirmar_tokenValido_ejecutaElComando() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.avisoValido());

        var resultado = e.api.confirmar(e.ctxA, preparacion.token());

        assertThat(resultado.codigo()).isEqualTo("OK");
        assertThat(resultado.mensajePlantilla()).isNotBlank();
        assertThat(e.avisos.historial(e.negocioA)).hasSize(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "no-es-un-token", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    @DisplayName("un token nulo, vacio o desconocido falla con TOKEN_INVALIDO")
    void confirmar_tokenDesconocido_fallaConTokenInvalido(String token) {
        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.TOKEN_INVALIDO);
    }

    @Test
    @DisplayName("el token de otro negocio se rechaza y no se consume")
    void confirmar_tokenDeOtroNegocio_fallaYNoSeConsume() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.avisoValido());

        fallaCon(() -> e.api.confirmar(e.ctxB, preparacion.token()), CodigoError.TOKEN_INVALIDO);

        assertThat(e.api.confirmar(e.ctxA, preparacion.token()).codigo()).isEqualTo("OK");
        assertThat(e.avisos.historial(e.negocioB)).isEmpty();
    }

    @Test
    @DisplayName("el token de otro actor del mismo negocio se rechaza")
    void confirmar_tokenDeOtroActor_fallaConTokenInvalido() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.avisoValido());
        var otroActor = new ContextoNegocio(e.negocioA, "empleado-a");

        fallaCon(() -> e.api.confirmar(otroActor, preparacion.token()), CodigoError.TOKEN_INVALIDO);
    }

    @Test
    @DisplayName("a los 4 minutos 59 segundos el token todavia sirve")
    void confirmar_antesDeCincoMinutos_sigueVigente() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.avisoValido());
        e.reloj.avanzar(Duration.ofMinutes(4).plusSeconds(59));

        assertThat(e.api.confirmar(e.ctxA, preparacion.token()).codigo()).isEqualTo("OK");
    }

    @Test
    @DisplayName("a los cinco minutos el token falla con TOKEN_EXPIRADO y no ejecuta nada")
    void confirmar_aLosCincoMinutos_fallaConTokenExpirado() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.avisoValido());
        e.reloj.avanzar(Duration.ofMinutes(5));

        fallaCon(() -> e.api.confirmar(e.ctxA, preparacion.token()), CodigoError.TOKEN_EXPIRADO);

        assertThat(e.avisos.historial(e.negocioA)).isEmpty();
    }

    @Test
    @DisplayName("confirmar dos veces falla con TOKEN_USADO y ejecuta una sola vez")
    void confirmar_dosVeces_fallaConTokenUsado() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.avisoValido());
        e.api.confirmar(e.ctxA, preparacion.token());

        fallaCon(() -> e.api.confirmar(e.ctxA, preparacion.token()), CodigoError.TOKEN_USADO);

        assertThat(e.avisos.historial(e.negocioA)).hasSize(1);
    }

    @Test
    @DisplayName("si otra peticion gana la carrera por usar el token, falla con TOKEN_USADO")
    void confirmar_carreraPorElToken_fallaConTokenUsado() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.avisoValido());
        e.preparaciones.simularCarreraDeUso = true;

        fallaCon(() -> e.api.confirmar(e.ctxA, preparacion.token()), CodigoError.TOKEN_USADO);

        assertThat(e.avisos.historial(e.negocioA)).isEmpty();
    }

    @Test
    @DisplayName("un comando alterado despues de preparar falla con COMANDO_CAMBIO")
    void confirmar_comandoAlterado_fallaConComandoCambio() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.avisoValido());
        var fila = e.preparaciones.unica();
        e.preparaciones.alterarComando(fila.tokenHash(), fila.comando().replace("Taller Mixcoatl", "Otro Taller"));

        fallaCon(() -> e.api.confirmar(e.ctxA, preparacion.token()), CodigoError.COMANDO_CAMBIO);

        assertThat(e.avisos.historial(e.negocioA)).isEmpty();
    }

    @Test
    @DisplayName("un comando ausente o ilegible falla con COMANDO_CAMBIO")
    void confirmar_comandoAusenteOIlegible_fallaConComandoCambio() {
        var preparacion = e.api.preparar(e.ctxA, Fixtures.avisoValido());
        e.preparaciones.alterarComando(e.preparaciones.unica().tokenHash(), null);

        fallaCon(() -> e.api.confirmar(e.ctxA, preparacion.token()), CodigoError.COMANDO_CAMBIO);
    }

    @Test
    @DisplayName("tras confirmar se vacian el comando y el resumen, que llevan datos personales (SC-10)")
    void confirmar_comandoConDatosPersonales_quedaVacioTrasConfirmar() {
        e.conAviso(e.ctxA);
        var preparacion = e.api.preparar(e.ctxA, Fixtures.registrar("Ana", "Perez", "222 123 4567"));
        var antes = e.preparaciones.todas().stream().filter(f -> f.comandoTipo().equals("registrar_cliente"))
                .findFirst().orElseThrow();
        assertThat(antes.comando()).contains("Ana");

        e.api.confirmar(e.ctxA, preparacion.token());

        var despues = e.preparaciones.todas().stream().filter(f -> f.comandoTipo().equals("registrar_cliente"))
                .findFirst().orElseThrow();
        assertThat(despues.comando()).isNull();
        assertThat(despues.resumen()).isNull();
        assertThat(despues.usadoEn()).isEqualTo(Fixtures.AHORA);
    }

    @Test
    @DisplayName("si la revalidacion falla al confirmar, el token se consume y el comando se vacia")
    void confirmar_revalidacionFalla_consumeElTokenYVaciaElComando() {
        e.conAviso(e.ctxA);
        var preparacion = e.api.preparar(e.ctxA, Fixtures.registrar("Ana"));
        e.confirmar(e.ctxA, Fixtures.avisoValido());

        fallaCon(() -> e.api.confirmar(e.ctxA, preparacion.token()), CodigoError.AVISO_NO_VIGENTE);

        var fila = e.preparaciones.todas().stream().filter(f -> f.comandoTipo().equals("registrar_cliente"))
                .findFirst().orElseThrow();
        assertThat(fila.comando()).isNull();
        assertThat(fila.usadoEn()).isNotNull();
        fallaCon(() -> e.api.confirmar(e.ctxA, preparacion.token()), CodigoError.TOKEN_USADO);
        assertThat(e.clientes.total()).isZero();
    }

    private static String sha256(String texto) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(UTF_8)));
    }

    private mx.negocio360.clientes.aplicacion.Preparacion filaDeRegistro() {
        return e.preparaciones.todas().stream().filter(f -> f.comandoTipo().equals("registrar_cliente")).findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("un token vencido vacia el comando guardado en el primer intento (SC-10)")
    void confirmar_tokenVencido_vaciaElComando() {
        e.conAviso(e.ctxA);
        var preparacion = e.api.preparar(e.ctxA, Fixtures.registrar("Ana", "Perez", "222 123 4567"));
        e.reloj.avanzar(Duration.ofMinutes(6));

        fallaCon(() -> e.api.confirmar(e.ctxA, preparacion.token()), CodigoError.TOKEN_EXPIRADO);

        assertThat(filaDeRegistro().comando()).isNull();
        assertThat(filaDeRegistro().resumen()).isNull();
    }

    @Test
    @DisplayName("un comando alterado vacia el comando guardado")
    void confirmar_comandoAlterado_vaciaElComando() {
        e.conAviso(e.ctxA);
        var preparacion = e.api.preparar(e.ctxA, Fixtures.registrar("Ana", "Perez", "222 123 4567"));
        var fila = filaDeRegistro();
        e.preparaciones.alterarComando(fila.tokenHash(), fila.comando().replace("Ana", "Beto"));

        fallaCon(() -> e.api.confirmar(e.ctxA, preparacion.token()), CodigoError.COMANDO_CAMBIO);

        assertThat(filaDeRegistro().comando()).isNull();
    }

    @Test
    @DisplayName("el token de otro negocio no vacia el comando de quien lo preparo")
    void confirmar_tokenAjeno_noVaciaElComando() {
        e.conAviso(e.ctxA);
        var preparacion = e.api.preparar(e.ctxA, Fixtures.registrar("Ana"));

        fallaCon(() -> e.api.confirmar(e.ctxB, preparacion.token()), CodigoError.TOKEN_INVALIDO);

        assertThat(filaDeRegistro().comando()).isNotNull();
    }

    @Test
    @DisplayName("un token de mas de 64 caracteres se rechaza sin buscarlo")
    void confirmar_tokenEnorme_fallaConTokenInvalido() {
        fallaCon(() -> e.api.confirmar(e.ctxA, "a".repeat(65)), CodigoError.TOKEN_INVALIDO);
    }

    @Test
    @DisplayName("un campo desconocido en el comando guardado, aunque el hash coincida, falla sin citar el contenido")
    void confirmar_campoDesconocidoConHashCoincidente_fallaConComandoCambio() throws Exception {
        e.conAviso(e.ctxA);
        var preparacion = e.api.preparar(e.ctxA, Fixtures.registrar("Ana", "Perez", "222 123 4567"));
        var fila = filaDeRegistro();
        var forjado = fila.comando().replace("\"nombre\"", "\"campoAjeno\":\"Ana\",\"nombre\"");
        e.preparaciones.reemplazarComandoYHash(fila.tokenHash(), forjado, sha256(forjado));

        var error = fallaCon(() -> e.api.confirmar(e.ctxA, preparacion.token()), CodigoError.COMANDO_CAMBIO);

        assertThat(error.getCause()).isNull();
        assertThat(error.getMessage()).isEqualTo("COMANDO_CAMBIO");
        assertThat(e.clientes.total()).isZero();
    }

    @Test
    @DisplayName("si vaciar falla despues de un alta exitosa, la persona igual recibe el resultado")
    void confirmar_vaciarFallaTrasExito_devuelveElResultado() {
        e.conAviso(e.ctxA);
        var preparacion = e.api.preparar(e.ctxA, Fixtures.registrar("Ana"));
        e.preparaciones.fallarAlVaciar = true;

        var resultado = e.api.confirmar(e.ctxA, preparacion.token());

        assertThat(resultado.codigo()).isEqualTo("OK");
        assertThat(e.clientes.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("si el comando falla y vaciar tambien, sale el error del comando y el otro queda como suprimido")
    void confirmar_vaciarFallaTrasError_noEnmascaraElErrorOriginal() {
        e.conAviso(e.ctxA);
        e.registrar(e.ctxA, Fixtures.registrar("Ana", null, "222 123 4567"));
        var preparacion = e.api.preparar(e.ctxA, Fixtures.registrar("Beto", null, "999 888 7766"));
        e.registrar(e.ctxA, Fixtures.registrar("Carla", null, "999 888 7766"));
        e.preparaciones.fallarAlVaciar = true;

        var error = fallaCon(() -> e.api.confirmar(e.ctxA, preparacion.token()), CodigoError.TELEFONO_DUPLICADO);

        assertThat(error.getSuppressed()).hasSize(1);
        assertThat(error.getSuppressed()[0]).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("un comando sin manejador registrado es un error de programacion")
    void preparar_comandoSinManejador_lanzaIllegalArgument() {
        var desconocido = new Comando() { };

        assertThatThrownBy(() -> e.api.preparar(e.ctxA, desconocido)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("dos manejadores para el mismo comando se rechazan al armar el servicio")
    void constructor_manejadorRepetido_lanzaIllegalArgument() {
        var manejador = new ManejadorPublicarAviso(e.avisos, e.reloj);

        assertThatThrownBy(() -> new ServicioPreparacion(e.preparaciones, java.util.List.of(manejador, manejador),
                e.reloj)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el comando es obligatorio")
    void preparar_comandoNulo_lanzaNullPointer() {
        assertThatThrownBy(() -> e.api.preparar(e.ctxA, null)).isInstanceOf(NullPointerException.class);
    }
}
