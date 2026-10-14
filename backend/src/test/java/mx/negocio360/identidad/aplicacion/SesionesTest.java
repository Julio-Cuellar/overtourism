package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.Escenario.AHORA;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A_E164;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_B;
import static mx.negocio360.identidad.aplicacion.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.EstadoCuenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** IDN-1.5 y IDN-1.2 · AS-05, AS-07, AS-08, AC-04: autenticar, cerrar sesiones y cambiar el nombre. */
class SesionesTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("un token vigente autentica y devuelve la cuenta, sin negocio activo todavia")
    void autenticar_tokenVigente_devuelveLaSesion() {
        var iniciada = e.entrar(TEL_A, "Ana López");

        var sesion = e.api.autenticar(iniciada.token()).orElseThrow();

        assertThat(sesion.cuentaId()).isEqualTo(iniciada.cuenta().id());
        assertThat(sesion.nombre()).isEqualTo("Ana López");
        assertThat(sesion.negocioActivo()).isEmpty();
        assertThat(sesion.contextoNegocio()).isEmpty();
        assertThat(sesion.expiraEn()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "basura", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    @DisplayName("un token nulo, vacio o inventado no autentica")
    void autenticar_tokenInventado_devuelveVacio(String token) {
        assertThat(e.api.autenticar(token)).isEmpty();
    }

    @Test
    @DisplayName("AS-08: un token desconocido, uno revocado y uno vencido dan exactamente el mismo resultado")
    void autenticar_desconocidoRevocadoYVencido_dan_elMismoResultado() {
        var revocado = e.entrar(TEL_A, "Ana López").token();
        e.api.cerrarSesion(revocado);
        e.reloj.avanzar(Duration.ofMinutes(16));
        var vencido = e.entrar(TEL_A, "Ana López").token();
        e.reloj.avanzar(Duration.ofHours(8));

        var desconocido = e.api.autenticar(TokenOpaco.nuevo());

        assertThat(desconocido).isEmpty();
        assertThat(e.api.autenticar(revocado)).isEqualTo(desconocido);
        assertThat(e.api.autenticar(vencido)).isEqualTo(desconocido);
    }

    @Test
    @DisplayName("AS-05: cada uso renueva las 8 horas de inactividad; sin uso, la sesion vence")
    void autenticar_usoPeriodico_renuevaLaVigencia() {
        var token = e.entrar(TEL_A, "Ana López").token();

        e.reloj.avanzar(Duration.ofHours(5));
        assertThat(e.api.autenticar(token)).isPresent();
        e.reloj.avanzar(Duration.ofHours(5));
        assertThat(e.api.autenticar(token)).isPresent();
        e.reloj.avanzar(Duration.ofHours(8));

        assertThat(e.api.autenticar(token)).isEmpty();
    }

    @Test
    @DisplayName("AS-05: aunque se use sin parar, la sesion termina a los 30 dias")
    void autenticar_usoContinuo_terminaAlTopeDeTreintaDias() {
        var token = e.entrar(TEL_A, "Ana López").token();
        var tope = AHORA.plus(Duration.ofDays(30));
        while (e.reloj.instant().plus(Duration.ofHours(7)).isBefore(tope)) {
            e.reloj.avanzar(Duration.ofHours(7));
            assertThat(e.api.autenticar(token)).isPresent();
        }

        e.reloj.avanzar(Duration.between(e.reloj.instant(), tope));

        assertThat(e.reloj.instant()).isEqualTo(tope);
        assertThat(e.api.autenticar(token)).isEmpty();
    }

    @Test
    @DisplayName("una cuenta suspendida despues de abrir la sesion ya no autentica")
    void autenticar_cuentaSuspendida_devuelveVacio() {
        var token = e.entrar(TEL_A, "Ana López").token();
        var cuenta = e.cuentas.porTelefono(TEL_A_E164).orElseThrow();
        e.cuentas.guardar(cuenta.conEstado(EstadoCuenta.SUSPENDIDA));

        assertThat(e.api.autenticar(token)).isEmpty();
    }

    @Test
    @DisplayName("yo devuelve el perfil: cuenta, negocio activo (ninguno) y negocios (ninguno todavia)")
    void yo_sesionVigente_devuelveElPerfil() {
        var token = e.entrar(TEL_A, "Ana López").token();

        var perfil = e.api.yo(token);

        assertThat(perfil.cuenta().nombre()).isEqualTo("Ana López");
        assertThat(perfil.cuenta().telefono()).isEqualTo(TEL_A_E164);
        assertThat(perfil.negocioActivo()).isNull();
        assertThat(perfil.negocios()).isEmpty();
    }

    @Test
    @DisplayName("yo con un token invalido falla con NO_AUTENTICADO")
    void yo_tokenInvalido_fallaConNoAutenticado() {
        fallaCon(() -> e.api.yo("basura"), CodigoError.NO_AUTENTICADO);
    }

    @Test
    @DisplayName("AS-07: cerrar sesion revoca ese token y deja las demas sesiones de la cuenta")
    void cerrarSesion_unToken_dejaLasDemasSesiones() {
        var primera = e.entrar(TEL_A, "Ana López").token();
        e.reloj.avanzar(Duration.ofMinutes(16));
        var segunda = e.entrar(TEL_A, "Ana López").token();

        e.api.cerrarSesion(primera);

        assertThat(e.api.autenticar(primera)).isEmpty();
        assertThat(e.api.autenticar(segunda)).isPresent();
    }

    @Test
    @DisplayName("cerrar una sesion que ya no sirve falla con NO_AUTENTICADO")
    void cerrarSesion_tokenYaCerrado_fallaConNoAutenticado() {
        var token = e.entrar(TEL_A, "Ana López").token();
        e.api.cerrarSesion(token);

        fallaCon(() -> e.api.cerrarSesion(token), CodigoError.NO_AUTENTICADO);
    }

    @Test
    @DisplayName("AS-07: cerrar todas las sesiones revoca las de la cuenta y no toca las de otra persona")
    void cerrarTodas_unaCuenta_noTocaAOtra() {
        var a1 = e.entrar(TEL_A, "Ana López").token();
        e.reloj.avanzar(Duration.ofMinutes(16));
        var a2 = e.entrar(TEL_A, "Ana López").token();
        var b = e.entrar(TEL_B, "Beto Ruiz").token();

        e.api.cerrarTodasLasSesiones(a1);

        assertThat(e.api.autenticar(a1)).isEmpty();
        assertThat(e.api.autenticar(a2)).isEmpty();
        assertThat(e.api.autenticar(b)).isPresent();
    }

    @Test
    @DisplayName("actualizar el nombre lo normaliza, lo guarda y se ve en la siguiente autenticacion")
    void actualizarNombre_nombreValido_seGuarda() {
        var token = e.entrar(TEL_A, "Ana López").token();

        var cuenta = e.api.actualizarNombre(token, "  Ana   María López ");

        assertThat(cuenta.nombre()).isEqualTo("Ana María López");
        assertThat(e.api.autenticar(token).orElseThrow().nombre()).isEqualTo("Ana María López");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"A", "Ana\u0000", Escenario.NOMBRE_DE_81})
    @DisplayName("AC-04: un nombre invalido falla con NOMBRE_INVALIDO y no cambia nada")
    void actualizarNombre_nombreInvalido_noCambiaNada(String nombre) {
        var token = e.entrar(TEL_A, "Ana López").token();

        fallaCon(() -> e.api.actualizarNombre(token, nombre), CodigoError.NOMBRE_INVALIDO);

        assertThat(e.api.autenticar(token).orElseThrow().nombre()).isEqualTo("Ana López");
    }

    @Test
    @DisplayName("actualizar el nombre con un token invalido falla con NO_AUTENTICADO")
    void actualizarNombre_tokenInvalido_fallaConNoAutenticado() {
        fallaCon(() -> e.api.actualizarNombre("basura", "Ana López"), CodigoError.NO_AUTENTICADO);
    }

    @Test
    @DisplayName("cambiar el nombre de una cuenta no cambia el de otra")
    void actualizarNombre_unaCuenta_noAfectaAOtra() {
        var a = e.entrar(TEL_A, "Ana López").token();
        var b = e.entrar(TEL_B, "Beto Ruiz").token();

        e.api.actualizarNombre(a, "Ana Nueva");

        assertThat(e.api.autenticar(b).orElseThrow().nombre()).isEqualTo("Beto Ruiz");
    }

    @Test
    @DisplayName("el ultimo acceso de la cuenta se actualiza al iniciar sesion, no al autenticar")
    void iniciar_ultimoAcceso_seActualizaSoloAlIniciarSesion() {
        var token = e.entrar(TEL_A, "Ana López").token();
        var momento = Instant.from(e.reloj.instant());

        e.reloj.avanzar(Duration.ofHours(1));
        assertThat(e.api.autenticar(token)).isPresent();

        assertThat(e.cuentas.porTelefono(TEL_A_E164).orElseThrow().ultimoAccesoEn()).isEqualTo(momento);
    }
}
