package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.Escenario.AHORA;
import static mx.negocio360.identidad.aplicacion.Escenario.DISPOSITIVO;
import static mx.negocio360.identidad.aplicacion.Escenario.IP;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A_E164;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_B;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_B_E164;
import static mx.negocio360.identidad.aplicacion.Escenario.correoDe;
import static mx.negocio360.identidad.aplicacion.Escenario.fallaCon;
import static mx.negocio360.identidad.aplicacion.Escenario.solicitud;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.identidad.IdentidadServicio;
import mx.negocio360.identidad.SolicitudDeRegistro;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioPreregistros;
import mx.negocio360.identidad.dominio.Cuenta;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.EstadoCuenta;
import mx.negocio360.identidad.dominio.IdentidadException;
import mx.negocio360.identidad.dominio.Preregistro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * D-84 · registro en dos pasos, como en Clinerya: el formulario se guarda como pre-registro y manda un codigo al
 * correo; con el codigo se crea la cuenta. El pre-registro vive 15 minutos. Primero se valida la forma de los campos
 * y despues se toca nada: un formulario mal llenado no gasta codigos ni correos.
 */
class RegistroEnDosPasosTest {

    private static final String CORREO_A = correoDe(TEL_A);
    private static final String CORREO_B = correoDe(TEL_B);

    private final Escenario e = new Escenario();

    private SolicitudDeRegistro conCampos(String nombre, String apellidos, String correo, boolean aviso) {
        return new SolicitudDeRegistro(nombre, apellidos, correo, TEL_A, aviso);
    }

    private static String distintoDe(String codigo) {
        return "000000".equals(codigo) ? "000001" : "000000";
    }

    private void exigirSinEfectos() {
        assertThat(e.preregistros.todos()).as("no se guarda ningun pre-registro").isEmpty();
        assertThat(e.canal.envios()).as("no se manda ningun correo").isEmpty();
        assertThat(e.cuentas.todas()).as("ni se crea una cuenta").isEmpty();
    }

    // ---- paso 1: el formulario ----

    @Test
    @DisplayName("el formulario guarda un pre-registro, no crea la cuenta y dice hasta cuando vale el codigo")
    void registrar_formularioValido_guardaElPreregistro() {
        var creado = e.api.registrar(solicitud(TEL_A), IP);

        assertThat(creado.correo()).isEqualTo(CORREO_A);
        assertThat(creado.venceEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
        assertThat(e.cuentas.todas()).as("todavia no hay cuenta").isEmpty();
        assertThat(e.sesiones.todas()).isEmpty();
        var pre = e.preregistros.porCorreo(CORREO_A).orElseThrow();
        assertThat(pre.telefono()).isEqualTo(TEL_A_E164);
        assertThat(pre.nombre()).isEqualTo("Ana");
        assertThat(pre.apellidos()).isEqualTo("López Pérez");
        assertThat(pre.id().version()).isEqualTo(7);
        assertThat(pre.avisoVersion()).isEqualTo(1);
        assertThat(pre.avisoAceptadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("el codigo llega al correo, tiene seis digitos y solo se guarda su hash")
    void registrar_mandaElCodigoAlCorreo() {
        e.api.registrar(solicitud(TEL_A), IP);

        assertThat(e.canal.envios()).singleElement().satisfies(envio -> {
            assertThat(envio.destino()).isEqualTo(CORREO_A);
            assertThat(envio.codigo()).matches("\\d{6}");
        });
        var pre = e.preregistros.porCorreo(CORREO_A).orElseThrow();
        assertThat(pre.coincide(e.canal.ultimoCodigo())).isTrue();
        assertThat(pre.toString()).doesNotContain(e.canal.ultimoCodigo());
    }

    @Test
    @DisplayName("el celular y el correo se normalizan y los espacios sobrantes se colapsan")
    void registrar_camposConFormatoLibre_seNormalizan() {
        var creado = e.api.registrar(
                new SolicitudDeRegistro("  Ana   María ", " López    Pérez ", "  Ana.LOPEZ@Example.COM ", TEL_A, true), IP);

        assertThat(creado.correo()).isEqualTo("ana.lopez@example.com");
        var pre = e.preregistros.porCorreo("ana.lopez@example.com").orElseThrow();
        assertThat(pre.nombre()).isEqualTo("Ana María");
        assertThat(pre.apellidos()).isEqualTo("López Pérez");
        assertThat(pre.telefono()).isEqualTo(TEL_A_E164);
        assertThat(e.canal.envios()).singleElement().satisfies(envio ->
                assertThat(envio.destino()).isEqualTo("ana.lopez@example.com"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("sin nombre se pide NOMBRE_REQUERIDO y no se toca nada")
    void registrar_sinNombre_pideElNombre(String nombre) {
        fallaCon(() -> e.api.registrar(conCampos(nombre, "López", "a@example.com", true), IP),
                CodigoError.NOMBRE_REQUERIDO);

        exigirSinEfectos();
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "Ana\u0000López", Escenario.NOMBRE_DE_81})
    @DisplayName("un nombre invalido falla con NOMBRE_INVALIDO y no se toca nada")
    void registrar_nombreInvalido_fallaSinEfectos(String nombre) {
        fallaCon(() -> e.api.registrar(conCampos(nombre, "López", "a@example.com", true), IP),
                CodigoError.NOMBRE_INVALIDO);

        exigirSinEfectos();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "L", "López\u0000", Escenario.NOMBRE_DE_81})
    @DisplayName("apellidos ausentes o invalidos fallan con APELLIDOS_INVALIDOS y no se toca nada")
    void registrar_apellidosInvalidos_fallaSinEfectos(String apellidos) {
        fallaCon(() -> e.api.registrar(conCampos("Ana", apellidos, "a@example.com", true), IP),
                CodigoError.APELLIDOS_INVALIDOS);

        exigirSinEfectos();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "sin-arroba", "a@b", "a b@x.com", "<x>@x.com"})
    @DisplayName("un correo ausente o invalido falla con CORREO_INVALIDO y no se toca nada")
    void registrar_correoInvalido_fallaSinEfectos(String correo) {
        fallaCon(() -> e.api.registrar(conCampos("Ana", "López", correo, true), IP), CodigoError.CORREO_INVALIDO);

        exigirSinEfectos();
    }

    @Test
    @DisplayName("sin aceptar el aviso de privacidad falla con AVISO_REQUERIDO y no se toca nada")
    void registrar_sinAviso_fallaSinEfectos() {
        fallaCon(() -> e.api.registrar(conCampos("Ana", "López", "a@example.com", false), IP),
                CodigoError.AVISO_REQUERIDO);

        exigirSinEfectos();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "abcdefghij"})
    @DisplayName("un celular ausente o invalido falla con TELEFONO_INVALIDO y no se toca nada")
    void registrar_celularInvalido_fallaSinEfectos(String celular) {
        fallaCon(() -> e.api.registrar(new SolicitudDeRegistro("Ana", "López", "a@example.com", celular, true), IP),
                CodigoError.TELEFONO_INVALIDO);

        exigirSinEfectos();
    }

    @Test
    @DisplayName("un correo que ya tiene cuenta falla con CORREO_NO_DISPONIBLE, sin importar mayusculas, y no manda nada")
    void registrar_correoYaRegistrado_fallaSinMandarNada() {
        e.registrar(TEL_A, "Ana");
        var enviosAntes = e.canal.envios().size();

        fallaCon(() -> e.api.registrar(
                new SolicitudDeRegistro("Beto", "Ruiz", CORREO_A.toUpperCase(), TEL_B, true), IP),
                CodigoError.CORREO_NO_DISPONIBLE);

        assertThat(e.canal.envios()).hasSize(enviosAntes);
        assertThat(e.preregistros.porCorreo(CORREO_A)).isEmpty();
    }

    @Test
    @DisplayName("un celular que ya tiene cuenta falla con CUENTA_YA_EXISTE y no manda nada")
    void registrar_celularYaRegistrado_fallaSinMandarNada() {
        e.registrar(TEL_A, "Ana");
        var enviosAntes = e.canal.envios().size();

        fallaCon(() -> e.api.registrar(new SolicitudDeRegistro("Otra", "Persona", "otra@example.com", TEL_A, true), IP),
                CodigoError.CUENTA_YA_EXISTE);

        assertThat(e.canal.envios()).hasSize(enviosAntes);
        assertThat(e.preregistros.porCorreo("otra@example.com")).isEmpty();
    }

    @Test
    @DisplayName("volver a llenar el formulario con el mismo correo reemplaza el pre-registro: el codigo viejo ya no sirve")
    void registrar_mismoCorreoOtraVez_reemplazaAlAnterior() {
        e.api.registrar(solicitud(TEL_A), IP);
        var viejo = e.canal.ultimoCodigo();

        e.api.registrar(solicitud(TEL_A, "Ana María"), IP);
        var nuevo = e.canal.ultimoCodigo();

        assertThat(e.preregistros.todos()).hasSize(1);
        assertThat(e.preregistros.porCorreo(CORREO_A).orElseThrow().nombre()).isEqualTo("Ana María");
        if (!viejo.equals(nuevo)) {
            fallaCon(() -> e.api.verificarRegistro(CORREO_A, viejo, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        }
        assertThat(e.api.verificarRegistro(CORREO_A, nuevo, DISPOSITIVO, IP).cuenta().nombre()).isEqualTo("Ana María");
    }

    @Test
    @DisplayName("dos formularios con el mismo celular y distinto correo conviven hasta que uno se verifica")
    void registrar_mismoCelularDistintoCorreo_convivenHastaVerificar() {
        e.api.registrar(new SolicitudDeRegistro("Ana", "López", "uno@example.com", TEL_A, true), IP);
        var codigoUno = e.canal.ultimoCodigo();
        e.api.registrar(new SolicitudDeRegistro("Ana", "López", "dos@example.com", TEL_A, true), IP);
        var codigoDos = e.canal.ultimoCodigo();

        e.api.verificarRegistro("uno@example.com", codigoUno, DISPOSITIVO, IP);

        fallaCon(() -> e.api.verificarRegistro("dos@example.com", codigoDos, DISPOSITIVO, IP),
                CodigoError.CUENTA_YA_EXISTE);
        assertThat(e.preregistros.todos()).as("el que perdio tambien desaparece").isEmpty();
        assertThat(e.cuentas.todas()).hasSize(1);
    }

    @Test
    @DisplayName("si el canal falla el formulario no se guarda y el error es ERROR_TEMPORAL")
    void registrar_canalCaido_noGuardaNada() {
        e.canal.caido(true);

        fallaCon(() -> e.api.registrar(solicitud(TEL_A), IP), CodigoError.ERROR_TEMPORAL);

        assertThat(e.preregistros.todos()).isEmpty();
    }

    @Test
    @DisplayName("si el canal falla al volver a llenar el formulario, el pre-registro anterior y su codigo siguen sirviendo")
    void registrar_canalCaidoAlReemplazar_conservaElAnterior() {
        e.api.registrar(solicitud(TEL_A), IP);
        var codigo = e.canal.ultimoCodigo();
        e.canal.caido(true);

        fallaCon(() -> e.api.registrar(solicitud(TEL_A, "Otra"), IP), CodigoError.ERROR_TEMPORAL);
        e.canal.caido(false);

        assertThat(e.api.verificarRegistro(CORREO_A, codigo, DISPOSITIVO, IP).cuenta().nombre()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("el formulario tiene tope de 3 por correo cada 15 minutos y despues de la ventana vuelve a servir")
    void registrar_topePorCorreo() {
        for (var i = 0; i < 3; i++) {
            e.api.registrar(solicitud(TEL_A), IP);
        }

        fallaCon(() -> e.api.registrar(solicitud(TEL_A), IP), CodigoError.TASA_EXCEDIDA);
        e.reloj.avanzar(Duration.ofMinutes(16));

        assertThat(e.api.registrar(solicitud(TEL_A), IP).correo()).isEqualTo(CORREO_A);
    }

    @Test
    @DisplayName("el formulario tiene tope por IP, aunque cada vez sea otro correo")
    void registrar_topePorIp() {
        var servicio = e.servicio(e.canal, new PoliticaDeAcceso(Duration.ofMinutes(5), 5, 3, 2, Duration.ofMinutes(15),
                Duration.ofHours(8), Duration.ofDays(30), 5));
        servicio.registrar(new SolicitudDeRegistro("Ana", "López", "a1@example.com", TEL_A, true), IP);
        servicio.registrar(new SolicitudDeRegistro("Ana", "López", "a2@example.com", TEL_A, true), IP);

        fallaCon(() -> servicio.registrar(new SolicitudDeRegistro("Ana", "López", "a3@example.com", TEL_A, true), IP),
                CodigoError.TASA_EXCEDIDA);
        assertThat(servicio.registrar(new SolicitudDeRegistro("Ana", "López", "a4@example.com", TEL_A, true),
                "189.203.10.99").correo()).isEqualTo("a4@example.com");
    }

    // ---- paso 2: la verificacion ----

    @Test
    @DisplayName("con el codigo correcto se crea la cuenta con el correo ya verificado y se abre la sesion")
    void verificar_codigoCorrecto_creaLaCuentaYAbreSesion() {
        e.api.registrar(solicitud(TEL_A), IP);
        e.reloj.avanzar(Duration.ofMinutes(3));

        var sesion = e.api.verificarRegistro(CORREO_A, e.canal.ultimoCodigo(), DISPOSITIVO, IP);

        assertThat(sesion.token()).matches("[A-Za-z0-9_-]{43}");
        assertThat(sesion.cuenta().nombre()).isEqualTo("Ana");
        assertThat(sesion.cuenta().apellidos()).isEqualTo("López Pérez");
        assertThat(sesion.cuenta().telefono()).isEqualTo(TEL_A_E164);
        assertThat(sesion.cuenta().correo()).isEqualTo(CORREO_A);
        assertThat(sesion.negocios()).isEmpty();
        var cuenta = e.cuentas.porTelefono(TEL_A_E164).orElseThrow();
        assertThat(cuenta.estado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(cuenta.id().version()).isEqualTo(7);
        assertThat(cuenta.creadaEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(3)));
        assertThat(cuenta.correoVerificadoEn()).as("el codigo llego al correo: queda verificado")
                .isEqualTo(AHORA.plus(Duration.ofMinutes(3)));
        assertThat(cuenta.avisoVersion()).isEqualTo(1);
        assertThat(cuenta.avisoAceptadoEn()).as("el aviso se acepto al llenar el formulario").isEqualTo(AHORA);
        assertThat(e.api.autenticar(sesion.token())).isPresent();
        assertThat(e.preregistros.todos()).as("el pre-registro se borra").isEmpty();
    }

    @Test
    @DisplayName("el correo de la verificacion se normaliza: mayusculas y espacios no estorban")
    void verificar_correoConFormatoLibre_seNormaliza() {
        e.api.registrar(solicitud(TEL_A), IP);

        var sesion = e.api.verificarRegistro("  " + CORREO_A.toUpperCase() + " ", e.canal.ultimoCodigo(), DISPOSITIVO, IP);

        assertThat(sesion.cuenta().correo()).isEqualTo(CORREO_A);
    }

    @Test
    @DisplayName("yo devuelve los apellidos y el correo de la cuenta recien verificada")
    void yo_despuesDeVerificar_incluyeApellidosYCorreo() {
        var token = e.registrar(TEL_A, "Ana").token();

        var perfil = e.api.yo(token);

        assertThat(perfil.cuenta().apellidos()).isEqualTo("López Pérez");
        assertThat(perfil.cuenta().correo()).isEqualTo(CORREO_A);
    }

    @Test
    @DisplayName("el codigo es de un solo uso: la segunda verificacion falla y no hay otra cuenta")
    void verificar_codigoUsado_noSirveDeNuevo() {
        e.api.registrar(solicitud(TEL_A), IP);
        var codigo = e.canal.ultimoCodigo();
        e.api.verificarRegistro(CORREO_A, codigo, DISPOSITIVO, IP);

        fallaCon(() -> e.api.verificarRegistro(CORREO_A, codigo, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);

        assertThat(e.cuentas.todas()).hasSize(1);
    }

    @Test
    @DisplayName("un codigo incorrecto falla con CODIGO_INVALIDO, suma un intento y no crea la cuenta")
    void verificar_codigoIncorrecto_fallaYSumaUnIntento() {
        e.api.registrar(solicitud(TEL_A), IP);
        var incorrecto = distintoDe(e.canal.ultimoCodigo());

        fallaCon(() -> e.api.verificarRegistro(CORREO_A, incorrecto, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);

        assertThat(e.preregistros.porCorreo(CORREO_A).orElseThrow().intentos()).isEqualTo(1);
        assertThat(e.cuentas.todas()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345", "1234567", "abcdef", "12 456"})
    @DisplayName("un codigo mal formado tambien es CODIGO_INVALIDO y cuenta como intento")
    void verificar_codigoMalFormado_cuentaComoIntento(String codigo) {
        e.api.registrar(solicitud(TEL_A), IP);

        fallaCon(() -> e.api.verificarRegistro(CORREO_A, codigo, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);

        assertThat(e.preregistros.porCorreo(CORREO_A).orElseThrow().intentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("a los 5 intentos fallidos el codigo se agota: ni el correcto sirve, y el error no cambia")
    void verificar_cincoIntentos_agotanElCodigo() {
        e.api.registrar(solicitud(TEL_A), IP);
        var correcto = e.canal.ultimoCodigo();
        var incorrecto = distintoDe(correcto);
        for (var i = 0; i < 5; i++) {
            fallaCon(() -> e.api.verificarRegistro(CORREO_A, incorrecto, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        }

        fallaCon(() -> e.api.verificarRegistro(CORREO_A, correcto, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);

        assertThat(e.cuentas.todas()).isEmpty();
    }

    @Test
    @DisplayName("el codigo vale 15 minutos: un segundo antes sirve y a la hora justa falla y borra el pre-registro")
    void verificar_vencimientoALos15Minutos() {
        e.api.registrar(solicitud(TEL_A), IP);
        var codigo = e.canal.ultimoCodigo();
        e.reloj.avanzar(Duration.ofMinutes(15).minusSeconds(1));
        assertThat(e.preregistros.porCorreo(CORREO_A)).isPresent();

        e.reloj.avanzar(Duration.ofSeconds(1));

        fallaCon(() -> e.api.verificarRegistro(CORREO_A, codigo, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        assertThat(e.preregistros.todos()).as("el vencido se borra").isEmpty();
    }

    @Test
    @DisplayName("un segundo antes de vencer el codigo todavia crea la cuenta")
    void verificar_unSegundoAntesDeVencer_sirve() {
        e.api.registrar(solicitud(TEL_A), IP);
        e.reloj.avanzar(Duration.ofMinutes(15).minusSeconds(1));

        assertThat(e.api.verificarRegistro(CORREO_A, e.canal.ultimoCodigo(), DISPOSITIVO, IP).token()).isNotBlank();
    }

    @Test
    @DisplayName("seguridad: sin pre-registro, con codigo vencido o con codigo incorrecto el error y el mensaje son los mismos")
    void verificar_losFalloNoSeDistinguen() {
        e.api.registrar(solicitud(TEL_A), IP);
        var correcto = e.canal.ultimoCodigo();
        var incorrecto = distintoDe(correcto);
        var malo = fallaCon(() -> e.api.verificarRegistro(CORREO_A, incorrecto, DISPOSITIVO, IP),
                CodigoError.CODIGO_INVALIDO);
        e.reloj.avanzar(Duration.ofMinutes(16));

        var vencido = fallaCon(() -> e.api.verificarRegistro(CORREO_A, correcto, DISPOSITIVO, IP),
                CodigoError.CODIGO_INVALIDO);
        var inexistente = fallaCon(() -> e.api.verificarRegistro("nadie@example.com", correcto, DISPOSITIVO, IP),
                CodigoError.CODIGO_INVALIDO);

        assertThat(vencido.getMessage()).isEqualTo(malo.getMessage());
        assertThat(inexistente.getMessage()).isEqualTo(malo.getMessage());
    }

    @Test
    @DisplayName("un correo con formato invalido falla con CORREO_INVALIDO")
    void verificar_correoInvalido_falla() {
        fallaCon(() -> e.api.verificarRegistro("a@", "123456", DISPOSITIVO, IP), CodigoError.CORREO_INVALIDO);
        fallaCon(() -> e.api.verificarRegistro(null, "123456", DISPOSITIVO, IP), CodigoError.CORREO_INVALIDO);
    }

    @Test
    @DisplayName("el codigo esta atado al correo: el codigo de uno no activa el pre-registro de otro")
    void verificar_codigoDeOtroCorreo_noSirve() {
        e.api.registrar(solicitud(TEL_A), IP);
        e.api.registrar(solicitud(TEL_B), IP);
        var codigoDeB = e.canal.ultimoCodigo();
        var codigoDeA = e.canal.envios().get(0).codigo();
        Runnable usarElCodigoAjeno = () -> {
            if (!codigoDeA.equals(codigoDeB)) {
                fallaCon(() -> e.api.verificarRegistro(CORREO_A, codigoDeB, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
            }
        };

        usarElCodigoAjeno.run();

        assertThat(e.cuentas.todas()).isEmpty();
        assertThat(e.api.verificarRegistro(CORREO_B, codigoDeB, DISPOSITIVO, IP).cuenta().telefono())
                .isEqualTo(TEL_B_E164);
    }

    @Test
    @DisplayName("si mientras tanto alguien creo una cuenta con ese correo, la verificacion falla con CORREO_NO_DISPONIBLE y borra el pre-registro")
    void verificar_correoTomadoMientrasTanto_falla() {
        e.api.registrar(solicitud(TEL_A), IP);
        var codigo = e.canal.ultimoCodigo();
        e.cuentas.guardar(Cuenta.nueva(UuidV7.generar(AHORA), "Otra", "Persona", "+522221119999", CORREO_A, 1, AHORA));

        fallaCon(() -> e.api.verificarRegistro(CORREO_A, codigo, DISPOSITIVO, IP), CodigoError.CORREO_NO_DISPONIBLE);

        assertThat(e.preregistros.porCorreo(CORREO_A)).isEmpty();
        assertThat(e.cuentas.todas()).hasSize(1);
    }

    @Test
    @DisplayName("consumir el pre-registro decide quien gana: si otra verificacion lo consumio justo antes, esta falla y no crea nada")
    void verificar_preregistroConsumidoPorOtra_fallaSinCrearNada() {
        var yaConsumido = new RepositorioPreregistros() {
            @Override
            public void guardar(Preregistro preregistro) {
                e.preregistros.guardar(preregistro);
            }

            @Override
            public Optional<Preregistro> porCorreo(String correo) {
                return e.preregistros.porCorreo(correo);
            }

            @Override
            public boolean eliminar(UUID id) {
                e.preregistros.eliminar(id);
                return false;
            }

            @Override
            public int borrarVencidos(Instant ahora) {
                return e.preregistros.borrarVencidos(ahora);
            }
        };
        var servicio = new IdentidadServicio(e.cuentas, e.codigos, yaConsumido, e.sesiones, e.canal, e.limite, e.reloj,
                e.politica, e.equipo);
        servicio.registrar(solicitud(TEL_A), IP);

        fallaCon(() -> servicio.verificarRegistro(CORREO_A, e.canal.ultimoCodigo(), DISPOSITIVO, IP),
                CodigoError.CODIGO_INVALIDO);

        assertThat(e.cuentas.todas()).isEmpty();
        assertThat(e.sesiones.todas()).isEmpty();
    }

    @Test
    @DisplayName("la verificacion tiene tope por IP, aparte del de los formularios")
    void verificar_topePorIp() {
        var servicio = e.servicio(e.canal, new PoliticaDeAcceso(Duration.ofMinutes(5), 5, 3, 2, Duration.ofMinutes(15),
                Duration.ofHours(8), Duration.ofDays(30), 5));
        servicio.registrar(solicitud(TEL_A), "10.0.0.1");
        var incorrecto = distintoDe(e.canal.ultimoCodigo());
        fallaCon(() -> servicio.verificarRegistro(CORREO_A, incorrecto, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        fallaCon(() -> servicio.verificarRegistro(CORREO_A, incorrecto, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);

        fallaCon(() -> servicio.verificarRegistro(CORREO_A, incorrecto, DISPOSITIVO, IP), CodigoError.TASA_EXCEDIDA);
    }

    @Test
    @DisplayName("ocho verificaciones simultaneas con el mismo codigo: una sola cuenta y una sola sesion")
    void verificar_ocho_intentosSimultaneos_soloUnoGana() throws Exception {
        e.api.registrar(solicitud(TEL_A), IP);
        var codigo = e.canal.ultimoCodigo();
        var exitos = new AtomicInteger();
        List<Callable<Void>> tareas = IntStream.range(0, 8).mapToObj(i -> (Callable<Void>) () -> {
            try {
                e.api.verificarRegistro(CORREO_A, codigo, DISPOSITIVO, IP);
                exitos.incrementAndGet();
            } catch (IdentidadException ex) {
                // rechazo esperado
            }
            return null;
        }).toList();
        var pool = Executors.newFixedThreadPool(8);

        pool.invokeAll(tareas);
        pool.shutdown();

        assertThat(exitos.get()).isEqualTo(1);
        assertThat(e.cuentas.todas()).hasSize(1);
        assertThat(e.sesiones.todas()).hasSize(1);
    }

    @Test
    @DisplayName("el id, el correo y el celular de cada cuenta son unicos: dos cuentas, identidades distintas")
    void verificar_variasCuentas_cadaUnaTieneSuIdentidad() {
        var ana = e.registrar(TEL_A, "Ana").cuenta();
        var beto = e.registrar(TEL_B, "Beto").cuenta();

        assertThat(List.of(ana.id(), beto.id())).doesNotHaveDuplicates();
        assertThat(List.of(ana.correo(), beto.correo())).doesNotHaveDuplicates();
        assertThat(List.of(ana.telefono(), beto.telefono())).doesNotHaveDuplicates();
    }

    // ---- D-79: la persona y su negocio ----

    @Test
    @DisplayName("D-79: con negocio, al verificar se crea la persona, se anota el pedido y se publica el evento")
    void verificar_conNegocio_creaLaPersonaYPideElNegocio() {
        e.api.registrar(Escenario.solicitudConNegocio(TEL_A, "  Fonda   Doña Chela ", " restaurante "), IP);
        assertThat(e.eventos.eventos()).as("antes de verificar no hay cuenta ni evento").isEmpty();

        var sesion = e.api.verificarRegistro(CORREO_A, e.canal.ultimoCodigo(), DISPOSITIVO, IP);

        assertThat(sesion.negocios()).as("el negocio aun no existe: lo registra el modulo de negocios").isEmpty();
        assertThat(sesion.negociosPendientes()).singleElement().satisfies(pendiente -> {
            assertThat(pendiente.nombre()).isEqualTo("Fonda Doña Chela");
            assertThat(pendiente.giro()).isEqualTo("RESTAURANTE");
            assertThat(pendiente.estado()).isEqualTo("PENDIENTE");
            assertThat(pendiente.id().version()).isEqualTo(7);
        });
        var negocioId = sesion.negociosPendientes().get(0).id();
        assertThat(e.eventos.eventos()).singleElement().satisfies(evento -> {
            assertThat(evento.tipo()).isEqualTo("negocio.registro_solicitado");
            assertThat(evento.negocioId()).isEqualTo(negocioId);
            assertThat(evento.datos()).containsEntry("cuenta_id", sesion.cuenta().id().toString())
                    .containsEntry("nombre", "Fonda Doña Chela").containsEntry("giro", "RESTAURANTE");
        });
        assertThat(e.solicitudes.porNegocio(negocioId)).hasValueSatisfying(pedido -> {
            assertThat(pedido.cuentaId()).isEqualTo(sesion.cuenta().id());
            assertThat(pedido.nombre()).isEqualTo("Fonda Doña Chela");
        });
        assertThat(e.negocios.porId(negocioId)).as("Identidad sigue sin registrar negocios (D-78)").isEmpty();
    }

    @Test
    @DisplayName("sin negocio no se pide ninguno: no hay pendientes ni eventos")
    void verificar_sinNegocio_noPideNinguno() {
        var sesion = e.registrar(TEL_A, "Ana");

        assertThat(sesion.negociosPendientes()).isEmpty();
        assertThat(e.eventos.eventos()).isEmpty();
        assertThat(e.solicitudes.deCuenta(sesion.cuenta().id())).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULO", value = {"Fonda,FARMACIA,GIRO_INVALIDO", "Fonda,NULO,GIRO_INVALIDO",
        "F,RESTAURANTE,NOMBRE_INVALIDO", "NULO,RESTAURANTE,NOMBRE_INVALIDO"})
    @DisplayName("un negocio mal pedido falla en el formulario: no se guarda nada ni se manda correo")
    void registrar_negocioInvalido_fallaSinEfectos(String nombre, String giro, CodigoError esperado) {
        fallaCon(() -> e.api.registrar(Escenario.solicitudConNegocio(TEL_A, nombre, giro), IP), esperado);

        exigirSinEfectos();
        assertThat(e.eventos.eventos()).isEmpty();
    }

    @Test
    @DisplayName("SI-15: toString de la solicitud y de lo que se devuelve no revelan nada")
    void toString_noRevelaDatos() {
        var texto = Escenario.solicitudConNegocio(TEL_A, "Fonda Secreta", "MUSEO").toString();
        var creado = e.api.registrar(solicitud(TEL_A), IP).toString();

        assertThat(texto).doesNotContain("Ana").doesNotContain("López").doesNotContain("example.com")
                .doesNotContain("222").doesNotContain("Fonda Secreta").doesNotContain("MUSEO");
        assertThat(creado).doesNotContain("example.com");
    }

    // ---- reenvio ----

    @Test
    @DisplayName("el reenvio manda un codigo nuevo al correo: el viejo ya no sirve, el nuevo vale 15 minutos mas y los intentos vuelven a cero")
    void reenviar_conPreregistro_renuevaElCodigo() {
        e.api.registrar(solicitud(TEL_A), IP);
        var viejo = e.canal.ultimoCodigo();
        var incorrecto = distintoDe(viejo);
        fallaCon(() -> e.api.verificarRegistro(CORREO_A, incorrecto, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        e.reloj.avanzar(Duration.ofMinutes(10));

        e.api.reenviarCodigoDeRegistro(CORREO_A, IP);
        var nuevo = e.canal.ultimoCodigo();

        assertThat(e.canal.envios()).hasSize(2).last().satisfies(envio ->
                assertThat(envio.destino()).isEqualTo(CORREO_A));
        var pre = e.preregistros.porCorreo(CORREO_A).orElseThrow();
        assertThat(pre.venceEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(25)));
        assertThat(pre.intentos()).isZero();
        if (!viejo.equals(nuevo)) {
            fallaCon(() -> e.api.verificarRegistro(CORREO_A, viejo, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        }
        e.reloj.avanzar(Duration.ofMinutes(14));
        assertThat(e.api.verificarRegistro(CORREO_A, nuevo, DISPOSITIVO, IP).token()).isNotBlank();
    }

    @Test
    @DisplayName("el reenvio de un correo sin pre-registro responde igual: no falla y no manda nada")
    void reenviar_sinPreregistro_noFallaNiManda() {
        e.api.reenviarCodigoDeRegistro("nadie@example.com", IP);

        assertThat(e.canal.envios()).isEmpty();
        assertThat(e.preregistros.todos()).isEmpty();
    }

    @Test
    @DisplayName("el reenvio de un correo que ya tiene cuenta no manda nada ni lo delata")
    void reenviar_correoConCuenta_noMandaNada() {
        e.registrar(TEL_A, "Ana");
        var enviosAntes = e.canal.envios().size();

        e.api.reenviarCodigoDeRegistro(CORREO_A, IP);

        assertThat(e.canal.envios()).hasSize(enviosAntes);
    }

    @Test
    @DisplayName("el reenvio comparte el tope del formulario: el formulario y dos reenvios, y el tercero falla")
    void reenviar_comparteElTopeDelFormulario() {
        e.api.registrar(solicitud(TEL_A), IP);
        e.api.reenviarCodigoDeRegistro(CORREO_A, IP);
        e.api.reenviarCodigoDeRegistro(CORREO_A, IP);

        fallaCon(() -> e.api.reenviarCodigoDeRegistro(CORREO_A, IP), CodigoError.TASA_EXCEDIDA);
    }

    @Test
    @DisplayName("el tope del reenvio cuenta aunque no haya pre-registro: no delata cuales correos tienen uno")
    void reenviar_topeSinPreregistro_tambienCuenta() {
        for (var i = 0; i < 3; i++) {
            e.api.reenviarCodigoDeRegistro("nadie@example.com", IP);
        }

        fallaCon(() -> e.api.reenviarCodigoDeRegistro("nadie@example.com", IP), CodigoError.TASA_EXCEDIDA);
    }

    @Test
    @DisplayName("un correo invalido en el reenvio falla con CORREO_INVALIDO")
    void reenviar_correoInvalido_falla() {
        fallaCon(() -> e.api.reenviarCodigoDeRegistro("a@", IP), CodigoError.CORREO_INVALIDO);
        fallaCon(() -> e.api.reenviarCodigoDeRegistro(null, IP), CodigoError.CORREO_INVALIDO);
    }

    @Test
    @DisplayName("si el canal falla al reenviar, el codigo anterior sigue sirviendo")
    void reenviar_canalCaido_conservaElCodigoAnterior() {
        e.api.registrar(solicitud(TEL_A), IP);
        var codigo = e.canal.ultimoCodigo();
        e.canal.caido(true);

        fallaCon(() -> e.api.reenviarCodigoDeRegistro(CORREO_A, IP), CodigoError.ERROR_TEMPORAL);
        e.canal.caido(false);

        assertThat(e.api.verificarRegistro(CORREO_A, codigo, DISPOSITIVO, IP).token()).isNotBlank();
    }
}
