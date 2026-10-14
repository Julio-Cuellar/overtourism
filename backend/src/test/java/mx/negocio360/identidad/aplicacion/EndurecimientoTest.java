package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.Escenario.DISPOSITIVO;
import static mx.negocio360.identidad.aplicacion.Escenario.IP;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A_E164;
import static mx.negocio360.identidad.aplicacion.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.identidad.IdentidadServicio;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.EstadoCuenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Segundo ciclo de IDN-1: lo que encontraron las revisiones de codigo y de seguridad. Cada prueba nace en rojo. */
class EndurecimientoTest {

    private final Escenario e = new Escenario();

    // ---- el canal ----

    @Test
    @DisplayName("si el canal falla al reenviar, el codigo anterior que la persona ya recibio sigue sirviendo")
    void solicitar_canalCaidoAlReenviar_noInutilizaElCodigoPrevio() {
        var previo = e.pedirCodigo(TEL_A);
        e.canal.caido(true);

        fallaCon(() -> e.api.solicitarCodigo(TEL_A, IP), CodigoError.ERROR_TEMPORAL);

        e.canal.caido(false);
        assertThatCode(() -> e.api.iniciarSesion(TEL_A, previo, "Ana López", DISPOSITIVO, IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("si el canal falla, no queda guardado ningun codigo nuevo")
    void solicitar_canalCaido_noGuardaNada() {
        e.canal.caido(true);

        fallaCon(() -> e.api.solicitarCodigo(TEL_A, IP), CodigoError.ERROR_TEMPORAL);

        assertThat(e.codigos.ultimoDe(TEL_A_E164)).isEmpty();
    }

    @Test
    @DisplayName("cualquier fallo del canal, no solo el previsto, responde ERROR_TEMPORAL sin filtrar su mensaje")
    void solicitar_canalLanzaOtraExcepcion_respondeErrorTemporal() {
        var explota = new IdentidadServicio(e.cuentas, e.codigos, e.sesiones, (tel, codigo) -> {
            throw new IllegalStateException("fallo con " + tel + " y " + codigo);
        }, e.limite, e.reloj, e.politica);

        var error = fallaCon(() -> explota.solicitarCodigo(TEL_A, IP), CodigoError.ERROR_TEMPORAL);

        assertThat(error.toString() + error.getMessage()).doesNotContain("2221110001").doesNotContain("fallo con");
        assertThat(e.codigos.ultimoDe(TEL_A_E164)).isEmpty();
    }

    // ---- el codigo ----

    @Test
    @DisplayName("el codigo siempre sale con digitos ASCII, aunque el servidor tenga un idioma con otros digitos")
    void solicitar_localeConOtrosDigitos_generaCodigoAscii() {
        var original = Locale.getDefault();
        var arabe = Locale.forLanguageTag("ar-SA-u-nu-arab");
        assumeThat(String.format(arabe, "%06d", 1)).as("el JDK usa digitos arabes con ese idioma").isNotEqualTo("000001");
        Locale.setDefault(arabe);
        try {
            e.api.solicitarCodigo(TEL_A, IP);

            assertThat(e.canal.ultimoCodigo()).matches("[0-9]{6}");
        } finally {
            Locale.setDefault(original);
        }
    }

    // ---- el tope por IP ----

    @Test
    @DisplayName("un telefono invalido no gasta el tope de IP: despues caben las 20 solicitudes validas")
    void solicitar_telefonoInvalido_noGastaElTopeDeIp() {
        fallaCon(() -> e.api.solicitarCodigo("123", IP), CodigoError.TELEFONO_INVALIDO);

        for (var i = 0; i < e.politica.solicitudesPorIp(); i++) {
            var telefono = String.format("22211%05d", i);
            assertThatCode(() -> e.api.solicitarCodigo(telefono, IP)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("la IPv4 mapeada y la IPv4 normal comparten un mismo tope")
    void solicitar_ipv4Mapeada_comparteTopeConLaIpv4() {
        for (var i = 0; i < e.politica.solicitudesPorIp(); i++) {
            var ip = i % 2 == 0 ? "189.203.10.57" : "::ffff:189.203.10.57";
            e.api.solicitarCodigo(String.format("22212%05d", i), ip);
        }

        fallaCon(() -> e.api.solicitarCodigo("2221200099", "189.203.10.57"), CodigoError.TASA_EXCEDIDA);
    }

    @Test
    @DisplayName("quien cambia de direccion dentro de su /64 IPv6 no evade el tope")
    void solicitar_ipv6EnElMismoPrefijo_comparteTope() {
        for (var i = 0; i < e.politica.solicitudesPorIp(); i++) {
            e.api.solicitarCodigo(String.format("22213%05d", i), "2001:db8:85a3:8d3:1319:8a2e:370:" + Integer.toHexString(i + 1));
        }

        fallaCon(() -> e.api.solicitarCodigo("2221300099", "2001:db8:85a3:8d3:ffff:ffff:ffff:ffff"),
                CodigoError.TASA_EXCEDIDA);
    }

    // ---- el dispositivo ----

    @Test
    @DisplayName("el dispositivo se corta por caracteres completos: nunca deja la mitad de un emoji")
    void iniciar_dispositivoConEmojiEnElLimite_noParteElPar() {
        var sesion = e.api.iniciarSesion(TEL_A, e.pedirCodigo(TEL_A), "Ana López",
                "a".repeat(79) + "😀😀", IP);

        var dispositivo = e.sesiones.porHash(TokenOpaco.hash(sesion.token())).orElseThrow().dispositivo();

        assertThat(dispositivo.codePointCount(0, dispositivo.length())).isEqualTo(80);
        assertThat(new String(dispositivo.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)).isEqualTo(dispositivo);
    }

    @Test
    @DisplayName("el dispositivo pierde los caracteres que invierten el sentido del texto")
    void iniciar_dispositivoConCaracterDeFormato_seLimpia() {
        var sesion = e.api.iniciarSesion(TEL_A, e.pedirCodigo(TEL_A), "Ana López", "Chrome‮exe.txt", IP);

        var dispositivo = e.sesiones.porHash(TokenOpaco.hash(sesion.token())).orElseThrow().dispositivo();

        assertThat(dispositivo).doesNotContain("‮").contains("Chrome");
    }

    // ---- cuentas que no estan activas ----

    @ParameterizedTest
    @EnumSource(value = EstadoCuenta.class, names = "ACTIVA", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("una cuenta suspendida o anonimizada pierde la sesion que tenia, en todas las operaciones")
    void sesion_cuentaNoActiva_noAutenticaEnNingunaOperacion(EstadoCuenta estado) {
        var token = e.entrar(TEL_A, "Ana López").token();
        e.cuentas.guardar(e.cuentas.porTelefono(TEL_A_E164).orElseThrow().conEstado(estado));

        assertThat(e.api.autenticar(token)).isEmpty();
        fallaCon(() -> e.api.yo(token), CodigoError.NO_AUTENTICADO);
        fallaCon(() -> e.api.cerrarSesion(token), CodigoError.NO_AUTENTICADO);
        fallaCon(() -> e.api.cerrarTodasLasSesiones(token), CodigoError.NO_AUTENTICADO);
        fallaCon(() -> e.api.actualizarNombre(token, "Otro Nombre"), CodigoError.NO_AUTENTICADO);
    }

    @Test
    @DisplayName("AS-09: con una sola sesion permitida, entrar de nuevo cierra la anterior")
    void iniciar_conUnaSesionMaxima_laNuevaReemplazaALaAnterior() {
        var unaSola = new PoliticaDeAcceso(Duration.ofMinutes(5), 5, 3, 20, Duration.ofMinutes(15), Duration.ofHours(8),
                Duration.ofDays(30), 1);
        var api = new IdentidadServicio(e.cuentas, e.codigos, e.sesiones, e.canal, e.limite, e.reloj, unaSola);
        api.solicitarCodigo(TEL_A, IP);
        var primera = api.iniciarSesion(TEL_A, e.canal.ultimoCodigo(), "Ana López", DISPOSITIVO, IP).token();
        e.reloj.avanzar(Duration.ofMinutes(1));
        api.solicitarCodigo(TEL_A, IP);

        var segunda = api.iniciarSesion(TEL_A, e.canal.ultimoCodigo(), null, DISPOSITIVO, IP).token();

        assertThat(api.autenticar(primera)).isEmpty();
        assertThat(api.autenticar(segunda)).isPresent();
    }

    // ---- la politica ----

    @Test
    @DisplayName("la politica acepta sus valores maximos")
    void politica_enSusCotas_esValida() {
        assertThatCode(() -> new PoliticaDeAcceso(Duration.ofMinutes(15), 10, 10, 10_000, Duration.ofHours(24),
                Duration.ofDays(30), Duration.ofDays(90), 20)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("un error de configuracion que debilite el acceso (intentos, vigencia, topes) impide el arranque")
    void politica_pasadaDeLasCotas_seRechaza() {
        var m = Duration.ofMinutes(1);
        assertThatThrownBy(() -> new PoliticaDeAcceso(Duration.ofMinutes(16), 5, 3, 20, m, m, m, 5))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("codigoVigencia");
        assertThatThrownBy(() -> new PoliticaDeAcceso(m, 11, 3, 20, m, m, m, 5))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("intentosMaximos");
        assertThatThrownBy(() -> new PoliticaDeAcceso(m, 5, 11, 20, m, m, m, 5))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("solicitudesPorTelefono");
        assertThatThrownBy(() -> new PoliticaDeAcceso(m, 5, 3, 10_001, m, m, m, 5))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("solicitudesPorIp");
        assertThatThrownBy(() -> new PoliticaDeAcceso(m, 5, 3, 20, Duration.ofHours(25), m, m, 5))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ventanaDeSolicitudes");
        assertThatThrownBy(() -> new PoliticaDeAcceso(m, 5, 3, 20, m, Duration.ofDays(31), Duration.ofDays(91), 5))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("inactividad");
        assertThatThrownBy(() -> new PoliticaDeAcceso(m, 5, 3, 20, m, m, Duration.ofDays(91), 5))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("topeAbsoluto");
        assertThatThrownBy(() -> new PoliticaDeAcceso(m, 5, 3, 20, m, m, m, 21))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sesionesMaximas");
    }
}
