package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.Escenario.DISPOSITIVO;
import static mx.negocio360.identidad.aplicacion.Escenario.IP;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_B;
import static mx.negocio360.identidad.aplicacion.Escenario.correoDe;
import static mx.negocio360.identidad.aplicacion.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.EstadoCuenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D-76 y D-85: el id, el correo y el celular identifican a la persona. Con cualquiera de los tres se pide el codigo, que
 * siempre llega al correo de la cuenta (por ahora, la consola). Pedirlo nunca revela si la cuenta existe (SI-02), y un
 * codigo que falla por la razon que sea responde siempre lo mismo.
 */
class AccesoPorIdentificadorTest {

    private static final String CORREO_A = correoDe(TEL_A);

    private final Escenario e = new Escenario();

    /** Una cuenta registrada y la ventana de topes limpia. */
    private void cuentaDeAna() {
        e.registrar(TEL_A, "Ana");
        e.reloj.avanzar(Duration.ofMinutes(20));
    }

    @Test
    @DisplayName("pedir el codigo con el correo lo envia al correo de la cuenta, sin importar mayusculas")
    void solicitar_conCorreo_loEnviaAlCorreo() {
        cuentaDeAna();

        e.api.solicitarCodigo(CORREO_A.toUpperCase(), IP);

        assertThat(e.canal.envios()).last().satisfies(envio -> assertThat(envio.destino()).isEqualTo(CORREO_A));
    }

    @Test
    @DisplayName("pedir el codigo con el id lo envia al correo de la cuenta")
    void solicitar_conId_loEnviaAlCorreo() {
        var cuenta = e.registrar(TEL_A, "Ana").cuenta();
        e.reloj.avanzar(Duration.ofMinutes(20));
        var enviosAntes = e.canal.envios().size();

        e.api.solicitarCodigo(cuenta.id().toString(), IP);

        assertThat(e.canal.envios()).hasSize(enviosAntes + 1);
        assertThat(e.canal.envios()).last().satisfies(envio -> assertThat(envio.destino()).isEqualTo(CORREO_A));
    }

    @Test
    @DisplayName("pedir el codigo con el celular tambien lo envia al correo de la cuenta")
    void solicitar_conCelular_loEnviaAlCorreo() {
        cuentaDeAna();

        e.api.solicitarCodigo(TEL_A, IP);

        assertThat(e.canal.envios()).last().satisfies(envio -> assertThat(envio.destino()).isEqualTo(CORREO_A));
    }

    @Test
    @DisplayName("SI-02: un correo, un id o un celular sin cuenta responde igual que uno con cuenta: no falla y no envia nada")
    void solicitar_identificadorSinCuenta_noFallaNiEnvia() {
        assertThatCode(() -> e.api.solicitarCodigo("nadie@example.com", IP)).doesNotThrowAnyException();
        assertThatCode(() -> e.api.solicitarCodigo("0192f39f-3333-7000-8000-00000000f006", IP)).doesNotThrowAnyException();
        assertThatCode(() -> e.api.solicitarCodigo(TEL_B, IP)).doesNotThrowAnyException();

        assertThat(e.canal.envios()).isEmpty();
    }

    @Test
    @DisplayName("iniciar sesion con el correo y el codigo recibido abre la sesion de esa cuenta")
    void iniciar_conCorreo_abreLaSesionDeLaCuenta() {
        var registrada = e.registrar(TEL_A, "Ana");
        e.reloj.avanzar(Duration.ofMinutes(20));

        var sesion = e.iniciar(CORREO_A);

        assertThat(sesion.cuenta().id()).isEqualTo(registrada.cuenta().id());
        assertThat(e.api.autenticar(sesion.token())).isPresent();
    }

    @Test
    @DisplayName("iniciar sesion con el id y el codigo recibido abre la sesion de esa cuenta")
    void iniciar_conId_abreLaSesionDeLaCuenta() {
        var registrada = e.registrar(TEL_A, "Ana");
        e.reloj.avanzar(Duration.ofMinutes(20));

        var sesion = e.iniciar(registrada.cuenta().id().toString().toUpperCase());

        assertThat(sesion.cuenta().id()).isEqualTo(registrada.cuenta().id());
    }

    @Test
    @DisplayName("el codigo que se pidio con el celular tambien sirve para iniciar sesion con el correo: es el mismo codigo de la cuenta")
    void iniciar_codigoPedidoConCelular_sirveConElCorreo() {
        var registrada = e.registrar(TEL_A, "Ana");
        e.reloj.avanzar(Duration.ofMinutes(20));
        var codigo = e.pedirCodigo(TEL_A);

        var sesion = e.api.iniciarSesion(CORREO_A, codigo, DISPOSITIVO, IP);

        assertThat(sesion.cuenta().id()).isEqualTo(registrada.cuenta().id());
    }

    @Test
    @DisplayName("iniciar sesion con un correo que no tiene cuenta falla igual que un codigo incorrecto")
    void iniciar_correoSinCuenta_fallaConCodigoInvalido() {
        fallaCon(() -> e.api.iniciarSesion("nadie@example.com", "123456", DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
    }

    @Test
    @DisplayName("un identificador con formato invalido falla con TELEFONO_INVALIDO o CORREO_INVALIDO")
    void identificadorInvalido_falla() {
        fallaCon(() -> e.api.solicitarCodigo("123", IP), CodigoError.TELEFONO_INVALIDO);
        fallaCon(() -> e.api.solicitarCodigo("a@", IP), CodigoError.CORREO_INVALIDO);
        fallaCon(() -> e.api.iniciarSesion("a@", "123456", DISPOSITIVO, IP), CodigoError.CORREO_INVALIDO);
    }

    @Test
    @DisplayName("AS-03: pedir el codigo con el correo tiene tope de 3 por 15 minutos, exista o no la cuenta")
    void solicitar_conCorreo_tieneTopePorIdentificador() {
        for (var i = 0; i < 3; i++) {
            e.api.solicitarCodigo("nadie@example.com", IP);
        }

        fallaCon(() -> e.api.solicitarCodigo("nadie@example.com", IP), CodigoError.TASA_EXCEDIDA);
    }

    @Test
    @DisplayName("pedir el codigo por correo y por celular comparte el tope de la cuenta: nadie le llena el correo a otra persona")
    void solicitar_porCorreoYPorCelular_comparteElTopeDeLaCuenta() {
        cuentaDeAna();
        for (var i = 0; i < 3; i++) {
            e.api.solicitarCodigo(CORREO_A, IP);
        }
        var enviados = e.canal.envios().size();

        assertThatCode(() -> e.api.solicitarCodigo(TEL_A, IP)).doesNotThrowAnyException();

        assertThat(e.canal.envios()).as("el tope ya estaba agotado: no manda un cuarto correo").hasSize(enviados);
    }

    @Test
    @DisplayName("seguridad: agotar el tope de la cuenta no revela nada con ningun identificador: responde igual que sin cuenta y no envia")
    void solicitar_topeDeLaCuentaAgotado_respondeIgualYNoEnvia() {
        var cuenta = e.registrar(TEL_A, "Ana").cuenta();
        e.reloj.avanzar(Duration.ofMinutes(20));
        for (var i = 0; i < 3; i++) {
            e.api.solicitarCodigo(TEL_A, IP);
        }
        var enviados = e.canal.envios().size();

        assertThatCode(() -> e.api.solicitarCodigo(CORREO_A, IP)).doesNotThrowAnyException();
        assertThatCode(() -> e.api.solicitarCodigo(cuenta.id().toString(), IP)).doesNotThrowAnyException();

        assertThat(e.canal.envios()).hasSize(enviados);
    }

    @Test
    @DisplayName("seguridad: un codigo vencido da CODIGO_INVALIDO con cualquier identificador, igual que un correo que no existe")
    void iniciar_codigoVencido_noSeDistingueDeUnaCuentaInexistente() {
        var cuenta = e.registrar(TEL_A, "Ana").cuenta();
        e.reloj.avanzar(Duration.ofMinutes(20));
        e.api.solicitarCodigo(CORREO_A, IP);
        var codigo = e.canal.ultimoCodigo();
        e.reloj.avanzar(Duration.ofMinutes(6));

        var conCuenta = fallaCon(() -> e.api.iniciarSesion(CORREO_A, codigo, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        fallaCon(() -> e.api.iniciarSesion(TEL_A, codigo, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        fallaCon(() -> e.api.iniciarSesion(cuenta.id().toString(), codigo, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        var sinCuenta = fallaCon(() -> e.api.iniciarSesion("nadie@example.com", codigo, DISPOSITIVO, IP),
                CodigoError.CODIGO_INVALIDO);

        assertThat(conCuenta.getMessage()).isEqualTo(sinCuenta.getMessage());
    }

    @Test
    @DisplayName("seguridad: un codigo agotado da CODIGO_INVALIDO con el id, el correo y el celular")
    void iniciar_codigoAgotado_noSeDistingueDeUnaCuentaInexistente() {
        var cuenta = e.registrar(TEL_A, "Ana").cuenta();
        e.reloj.avanzar(Duration.ofMinutes(20));
        e.api.solicitarCodigo(cuenta.id().toString(), IP);
        var correcto = e.canal.ultimoCodigo();
        var incorrecto = "000000".equals(correcto) ? "000001" : "000000";
        for (var i = 0; i < 5; i++) {
            fallaCon(() -> e.api.iniciarSesion(cuenta.id().toString(), incorrecto, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        }

        fallaCon(() -> e.api.iniciarSesion(cuenta.id().toString(), correcto, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        fallaCon(() -> e.api.iniciarSesion(CORREO_A, correcto, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
        fallaCon(() -> e.api.iniciarSesion(TEL_A, correcto, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);
    }

    @Test
    @DisplayName("una cuenta suspendida no inicia sesion con el correo y el codigo se consume")
    void iniciar_cuentaSuspendidaPorCorreo_fallaConCodigoInvalido() {
        cuentaDeAna();
        e.cuentas.guardar(e.cuentas.porCorreo(CORREO_A).orElseThrow().conEstado(EstadoCuenta.SUSPENDIDA));
        e.api.solicitarCodigo(TEL_A, IP);
        var codigo = e.canal.ultimoCodigo();

        fallaCon(() -> e.api.iniciarSesion(CORREO_A, codigo, DISPOSITIVO, IP), CodigoError.CODIGO_INVALIDO);

        assertThat(e.codigos.ultimoDe(CORREO_A).orElseThrow().usadoEn()).isNotNull();
    }

    @Test
    @DisplayName("dos cuentas distintas no se confunden al pedir el codigo por correo")
    void solicitar_conCorreoDeOtra_noEnviaAlCorreoEquivocado() {
        e.registrar(TEL_A, "Ana");
        e.registrar(TEL_B, "Beto");
        e.reloj.avanzar(Duration.ofMinutes(20));

        e.api.solicitarCodigo(correoDe(TEL_B), IP);

        assertThat(e.canal.envios()).last().satisfies(envio -> assertThat(envio.destino()).isEqualTo(correoDe(TEL_B)));
    }
}
