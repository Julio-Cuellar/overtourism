package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.Escenario.AHORA;
import static mx.negocio360.identidad.aplicacion.Escenario.DISPOSITIVO;
import static mx.negocio360.identidad.aplicacion.Escenario.IP;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A_E164;
import static mx.negocio360.identidad.aplicacion.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.EstadoCuenta;
import mx.negocio360.identidad.dominio.IdentidadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** IDN-1.2, IDN-1.3 y IDN-1.5 · AC-01, AC-04, AS-01, AS-02, AS-04, AS-05, AS-09: verificar el codigo e iniciar sesion. */
class IniciarSesionTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("una cuenta nueva se registra con su nombre y recibe token, cuenta y ningun negocio todavia")
    void iniciar_cuentaNueva_devuelveTokenYCuenta() {
        var sesion = e.entrar(TEL_A, "Ana López");

        assertThat(sesion.token()).matches("[A-Za-z0-9_-]{43}");
        assertThat(sesion.expiraEn()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
        assertThat(sesion.cuenta().nombre()).isEqualTo("Ana López");
        assertThat(sesion.cuenta().telefono()).isEqualTo(TEL_A_E164);
        assertThat(sesion.negocios()).isEmpty();
        assertThat(sesion.negocioActivo()).isNull();
    }

    @Test
    @DisplayName("la cuenta nueva queda ACTIVA con un UUIDv7 y el telefono normalizado")
    void iniciar_cuentaNueva_seGuardaActivaConUuidV7() {
        e.entrar(TEL_A, "Ana López");

        var cuenta = e.cuentas.porTelefono(TEL_A_E164).orElseThrow();
        assertThat(cuenta.estado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(cuenta.id().version()).isEqualTo(7);
        assertThat(cuenta.creadaEn()).isEqualTo(AHORA);
        assertThat(e.cuentas.todas()).hasSize(1);
    }

    @Test
    @DisplayName("AS-05: de la sesion solo se guarda el hash del token, con 30 dias de tope y la IP truncada")
    void iniciar_sesionAbierta_seGuardaSoloElHash() {
        var sesion = e.entrar(TEL_A, "Ana López");

        var guardada = e.sesiones.porHash(TokenOpaco.hash(sesion.token())).orElseThrow();
        assertThat(guardada.tokenHash()).isNotEqualTo(sesion.token()).matches("[0-9a-f]{64}");
        assertThat(guardada.expiraAbsolutaEn()).isEqualTo(AHORA.plus(Duration.ofDays(30)));
        assertThat(guardada.dispositivo()).isEqualTo(DISPOSITIVO);
        assertThat(guardada.ipTruncada()).isEqualTo("189.203.10.0");
        assertThat(e.sesiones.todas()).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "NULO", value = {
        "189.203.10.57|189.203.10.0",
        "2001:db8:85a3:8d3:1319:8a2e:370:7348|2001:db8:85a3:8d3::",
        "no-es-una-ip|NULO",
        "NULO|NULO"})
    @DisplayName("la IP se trunca antes de guardarla; si no es una IP no se guarda")
    void iniciar_ip_seTruncaAntesDeGuardarse(String ip, String esperada) {
        var codigo = e.pedirCodigo(TEL_A);

        var sesion = e.api.iniciarSesion(TEL_A, codigo, "Ana López", DISPOSITIVO, ip);

        var guardada = e.sesiones.porHash(TokenOpaco.hash(sesion.token())).orElseThrow();
        assertThat(guardada.ipTruncada()).isEqualTo(esperada);
    }

    @Test
    @DisplayName("el dispositivo se limpia de caracteres de control y se corta a 80 caracteres")
    void iniciar_dispositivoLargoOConControl_seLimpia() {
        var codigo = e.pedirCodigo(TEL_A);
        var sucio = "Chrome\nen\tWindows" + "x".repeat(100);

        var sesion = e.api.iniciarSesion(TEL_A, codigo, "Ana López", sucio, IP);

        var dispositivo = e.sesiones.porHash(TokenOpaco.hash(sesion.token())).orElseThrow().dispositivo();
        assertThat(dispositivo).hasSize(80).doesNotContain("\n").doesNotContain("\t");
    }

    @Test
    @DisplayName("AC-01: el mismo telefono escrito de dos formas es la misma cuenta, y se ignora el nombre del segundo inicio")
    void iniciar_cuentaExistente_reutilizaLaCuenta() {
        var primera = e.entrar(TEL_A, "Ana López");
        e.reloj.avanzar(Duration.ofMinutes(20));

        var segunda = e.api.iniciarSesion("+52 (222) 111-0001", e.pedirCodigo("+52 (222) 111-0001"), "Otro Nombre",
                DISPOSITIVO, IP);

        assertThat(segunda.cuenta().id()).isEqualTo(primera.cuenta().id());
        assertThat(segunda.cuenta().nombre()).isEqualTo("Ana López");
        assertThat(e.cuentas.todas()).hasSize(1);
        assertThat(e.cuentas.porTelefono(TEL_A_E164).orElseThrow().ultimoAccesoEn()).isEqualTo(e.reloj.instant());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("AS-04: a una cuenta nueva sin nombre se le pide NOMBRE_REQUERIDO y el codigo no se gasta")
    void iniciar_cuentaNuevaSinNombre_pideElNombreSinConsumirElCodigo(String nombre) {
        var codigo = e.pedirCodigo(TEL_A);

        fallaCon(() -> e.api.iniciarSesion(TEL_A, codigo, nombre, DISPOSITIVO, IP), CodigoError.NOMBRE_REQUERIDO);

        var guardado = e.codigos.ultimoDe(TEL_A_E164).orElseThrow();
        assertThat(guardado.usadoEn()).isNull();
        assertThat(guardado.intentos()).isZero();
        assertThat(e.api.iniciarSesion(TEL_A, codigo, "Ana López", DISPOSITIVO, IP).token()).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "Ana\u0000López", Escenario.NOMBRE_DE_81})
    @DisplayName("un nombre invalido falla con NOMBRE_INVALIDO y tampoco gasta el codigo")
    void iniciar_nombreInvalido_fallaSinConsumirElCodigo(String nombre) {
        var codigo = e.pedirCodigo(TEL_A);

        fallaCon(() -> e.api.iniciarSesion(TEL_A, codigo, nombre, DISPOSITIVO, IP), CodigoError.NOMBRE_INVALIDO);

        assertThat(e.codigos.ultimoDe(TEL_A_E164).orElseThrow().usadoEn()).isNull();
        assertThat(e.cuentas.todas()).isEmpty();
    }

    @Test
    @DisplayName("un codigo incorrecto falla con CODIGO_INVALIDO y suma un intento")
    void iniciar_codigoIncorrecto_fallaYSumaUnIntento() {
        var correcto = e.pedirCodigo(TEL_A);
        var incorrecto = "000000".equals(correcto) ? "000001" : "000000";

        fallaCon(() -> e.api.iniciarSesion(TEL_A, incorrecto, "Ana", DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);

        assertThat(e.codigos.ultimoDe(TEL_A_E164).orElseThrow().intentos()).isEqualTo(1);
        assertThat(e.cuentas.todas()).isEmpty();
    }

    @Test
    @DisplayName("AS-01: un codigo usado no sirve una segunda vez")
    void iniciar_codigoUsado_noSirveDeNuevo() {
        var codigo = e.pedirCodigo(TEL_A);
        e.api.iniciarSesion(TEL_A, codigo, "Ana López", DISPOSITIVO, IP);

        fallaCon(() -> e.api.iniciarSesion(TEL_A, codigo, "Ana López", DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        assertThat(e.sesiones.todas()).hasSize(1);
    }

    @Test
    @DisplayName("AS-01: el codigo vence exactamente a los 5 minutos y falla con CODIGO_EXPIRADO")
    void iniciar_codigoVencido_fallaConCodigoExpirado() {
        var codigo = e.pedirCodigo(TEL_A);
        e.reloj.avanzar(Duration.ofMinutes(5));

        fallaCon(() -> e.api.iniciarSesion(TEL_A, codigo, "Ana López", DISPOSITIVO, IP), CodigoError.CODIGO_EXPIRADO);
    }

    @Test
    @DisplayName("un segundo antes de vencer el codigo todavia sirve")
    void iniciar_unSegundoAntesDeVencer_sirve() {
        var codigo = e.pedirCodigo(TEL_A);
        e.reloj.avanzar(Duration.ofMinutes(5).minusSeconds(1));

        assertThat(e.api.iniciarSesion(TEL_A, codigo, "Ana López", DISPOSITIVO, IP).token()).isNotBlank();
    }

    @Test
    @DisplayName("AS-02: tras 5 intentos fallidos el codigo se agota, aunque despues se escriba el correcto")
    void iniciar_cincoFallos_agotanElCodigo() {
        var correcto = e.pedirCodigo(TEL_A);
        var incorrecto = "000000".equals(correcto) ? "000001" : "000000";
        for (var i = 0; i < 5; i++) {
            fallaCon(() -> e.api.iniciarSesion(TEL_A, incorrecto, "Ana", DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        }

        fallaCon(() -> e.api.iniciarSesion(TEL_A, correcto, "Ana López", DISPOSITIVO, IP), CodigoError.CODIGO_AGOTADO);

        assertThat(e.cuentas.todas()).isEmpty();
        assertThat(e.sesiones.todas()).isEmpty();
    }

    @Test
    @DisplayName("sin codigo solicitado el error es el mismo que con un codigo incorrecto")
    void iniciar_sinCodigoSolicitado_fallaConCodigoInvalido() {
        fallaCon(() -> e.api.iniciarSesion(TEL_A, "123456", "Ana López", DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc123", "12345", "1234567", " 123456", "12 456"})
    @DisplayName("un codigo mal escrito falla con CODIGO_INVALIDO")
    void iniciar_codigoMalEscrito_fallaConCodigoInvalido(String intento) {
        e.pedirCodigo(TEL_A);

        fallaCon(() -> e.api.iniciarSesion(TEL_A, intento, "Ana López", DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
    }

    @Test
    @DisplayName("un codigo nuevo reemplaza al anterior: el primero ya no sirve")
    void iniciar_codigoReemplazado_elAnteriorNoSirve() {
        var primero = e.pedirCodigo(TEL_A);
        var segundo = e.pedirCodigo(TEL_A);
        assumeThat(primero).isNotEqualTo(segundo);

        fallaCon(() -> e.api.iniciarSesion(TEL_A, primero, "Ana López", DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        assertThat(e.api.iniciarSesion(TEL_A, segundo, "Ana López", DISPOSITIVO, IP).token()).isNotBlank();
    }

    @Test
    @DisplayName("un telefono invalido falla con TELEFONO_INVALIDO")
    void iniciar_telefonoInvalido_fallaConTelefonoInvalido() {
        fallaCon(() -> e.api.iniciarSesion("123", "123456", "Ana López", DISPOSITIVO, IP), CodigoError.TELEFONO_INVALIDO);
    }

    @Test
    @DisplayName("una cuenta suspendida no inicia sesion y recibe el mismo error que un codigo incorrecto")
    void iniciar_cuentaSuspendida_fallaConCodigoInvalido() {
        e.entrar(TEL_A, "Ana López");
        var cuenta = e.cuentas.porTelefono(TEL_A_E164).orElseThrow();
        e.cuentas.guardar(cuenta.conEstado(EstadoCuenta.SUSPENDIDA));
        e.reloj.avanzar(Duration.ofMinutes(20));
        var codigo = e.pedirCodigo(TEL_A);

        fallaCon(() -> e.api.iniciarSesion(TEL_A, codigo, null, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);

        assertThat(e.codigos.ultimoDe(TEL_A_E164).orElseThrow().usadoEn()).isNotNull();
    }

    @Test
    @DisplayName("AS-09: la sexta sesion de una cuenta revoca la mas antigua")
    void iniciar_sextaSesion_revocaLaMasAntigua() {
        var tokens = new ArrayList<String>();
        for (var i = 0; i < 6; i++) {
            tokens.add(e.entrar(TEL_A, "Ana López").token());
            e.reloj.avanzar(Duration.ofMinutes(16));
        }

        assertThat(e.api.autenticar(tokens.get(0))).isEmpty();
        for (var i = 1; i < 6; i++) {
            assertThat(e.api.autenticar(tokens.get(i))).isPresent();
        }
    }

    @Test
    @DisplayName("un codigo de un solo uso, aun con ocho intentos simultaneos: una sola sesion y una sola cuenta")
    void iniciar_ocho_intentosSimultaneos_soloUnoGana() throws Exception {
        var codigo = e.pedirCodigo(TEL_A);
        var exitos = new AtomicInteger();
        var rechazos = new AtomicInteger();
        List<Callable<Void>> tareas = IntStream.range(0, 8).mapToObj(i -> (Callable<Void>) () -> {
            try {
                e.api.iniciarSesion(TEL_A, codigo, "Ana López", DISPOSITIVO, IP);
                exitos.incrementAndGet();
            } catch (IdentidadException ex) {
                rechazos.incrementAndGet();
            }
            return null;
        }).toList();
        var pool = Executors.newFixedThreadPool(8);

        pool.invokeAll(tareas);
        pool.shutdown();

        assertThat(exitos.get()).isEqualTo(1);
        assertThat(rechazos.get()).isEqualTo(7);
        assertThat(e.sesiones.todas()).hasSize(1);
        assertThat(e.cuentas.todas()).hasSize(1);
    }

    @Test
    @DisplayName("SI-15: toString de la sesion iniciada no revela el token")
    void iniciar_toStringDeLaSesion_noRevelaElToken() {
        var sesion = e.entrar(TEL_A, "Ana López");

        assertThat(sesion.toString()).doesNotContain(sesion.token()).doesNotContain(TEL_A_E164);
    }
}
