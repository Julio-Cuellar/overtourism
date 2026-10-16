package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.Escenario.AHORA;
import static mx.negocio360.identidad.aplicacion.Escenario.IP;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_B;
import static mx.negocio360.identidad.aplicacion.Escenario.correoDe;
import static mx.negocio360.identidad.aplicacion.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import mx.negocio360.identidad.dominio.CodigoError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * IDN-1.3 y IDN-1.4 · AS-01, AS-03, SI-01 a SI-03, D-85: pedir un codigo de acceso. El codigo siempre llega al correo de
 * la cuenta, se escriba el celular, el correo o el id; y pedirlo nunca revela si la cuenta existe.
 */
class SolicitarCodigoTest {

    private static final String CORREO_A = correoDe(TEL_A);

    private final Escenario e = new Escenario();

    /** Una cuenta registrada y la ventana de topes ya limpia, para probar el acceso desde cero. */
    private void cuentaDeAna() {
        e.registrar(TEL_A, "Ana");
        e.reloj.avanzar(Duration.ofMinutes(20));
    }

    @Test
    @DisplayName("envia al correo de la cuenta un codigo de 6 digitos")
    void solicitar_celularDeUnaCuenta_enviaAlCorreoUnCodigoDeSeisDigitos() {
        cuentaDeAna();
        var antes = e.canal.envios().size();

        e.api.solicitarCodigo(TEL_A, IP);

        assertThat(e.canal.envios()).hasSize(antes + 1);
        assertThat(e.canal.envios().get(antes).destino()).isEqualTo(CORREO_A);
        assertThat(e.canal.envios().get(antes).codigo()).matches("\\d{6}");
    }

    @Test
    @DisplayName("cien codigos son siempre de 6 digitos, con ceros a la izquierda si toca")
    void solicitar_cienVeces_siempreSeisDigitos() {
        for (var i = 0; i < 100; i++) {
            var otro = new Escenario();
            otro.registrar(TEL_A, "Ana");
            otro.reloj.avanzar(Duration.ofMinutes(20));
            assertThat(otro.pedirCodigo(TEL_A)).matches("\\d{6}");
        }
    }

    @Test
    @DisplayName("guarda solo el hash con sal, sin intentos y con vencimiento a 5 minutos")
    void solicitar_codigoEmitido_seGuardaSoloElHash() {
        cuentaDeAna();

        var claro = e.pedirCodigo(TEL_A);

        var guardado = e.codigos.ultimoDe(CORREO_A).orElseThrow();
        assertThat(guardado.codigo().hash()).matches("[0-9a-f]{64}").doesNotContain(claro);
        assertThat(guardado.coincide(claro)).isTrue();
        assertThat(guardado.intentos()).isZero();
        assertThat(guardado.usadoEn()).isNull();
        assertThat(guardado.expiraEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(20)).plus(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("dos formas de escribir el mismo numero llegan al mismo correo")
    void solicitar_dosFormatosDelMismoNumero_llegaAlMismoCorreo() {
        cuentaDeAna();
        var antes = e.canal.envios().size();

        e.api.solicitarCodigo(TEL_A, IP);
        e.api.solicitarCodigo("+52 (222) 111-0001", IP);

        assertThat(e.canal.envios().subList(antes, antes + 2)).extracting(Escenario.CanalGrabador.Envio::destino)
                .containsExactly(CORREO_A, CORREO_A);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "abcdefghij", "+1 222 111 0001", "0221110001"})
    @DisplayName("un identificador invalido falla con TELEFONO_INVALIDO y no envia nada (que no gasta el tope de IP lo prueba EndurecimientoTest)")
    void solicitar_identificadorInvalido_fallaSinEnviar(String identificador) {
        fallaCon(() -> e.api.solicitarCodigo(identificador, IP), CodigoError.TELEFONO_INVALIDO);

        assertThat(e.canal.envios()).isEmpty();
    }

    @Test
    @DisplayName("AS-03: tres solicitudes por celular cada 15 minutos; la cuarta falla con TASA_EXCEDIDA")
    void solicitar_cuartaVezEnQuinceMinutos_fallaConTasaExcedida() {
        cuentaDeAna();
        var antes = e.canal.envios().size();
        for (var i = 0; i < 3; i++) {
            e.api.solicitarCodigo(TEL_A, IP);
        }

        fallaCon(() -> e.api.solicitarCodigo(TEL_A, IP), CodigoError.TASA_EXCEDIDA);

        assertThat(e.canal.envios()).hasSize(antes + 3);
    }

    @Test
    @DisplayName("pasados 15 minutos desde la primera solicitud se permite otra")
    void solicitar_pasadaLaVentana_sePermiteDeNuevo() {
        cuentaDeAna();
        for (var i = 0; i < 3; i++) {
            e.api.solicitarCodigo(TEL_A, IP);
        }
        fallaCon(() -> e.api.solicitarCodigo(TEL_A, IP), CodigoError.TASA_EXCEDIDA);

        e.reloj.avanzar(Duration.ofMinutes(15));

        assertThatCode(() -> e.api.solicitarCodigo(TEL_A, IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("el tope de un celular no afecta a otro celular")
    void solicitar_otroCelular_notieneElTopeAgotado() {
        cuentaDeAna();
        for (var i = 0; i < 3; i++) {
            e.api.solicitarCodigo(TEL_A, IP);
        }

        assertThatCode(() -> e.api.solicitarCodigo(TEL_B, IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("AS-03: veinte solicitudes por IP cada 15 minutos; la 21 falla aunque cambie el celular")
    void solicitar_veintiunaDesdeLaMismaIp_fallaConTasaExcedida() {
        for (var i = 0; i < 20; i++) {
            e.api.solicitarCodigo(String.format("22211%05d", i), IP);
        }

        fallaCon(() -> e.api.solicitarCodigo("2229999999", IP), CodigoError.TASA_EXCEDIDA);
    }

    @Test
    @DisplayName("el tope de una IP no afecta a otra IP")
    void solicitar_otraIp_noTieneElTopeAgotado() {
        for (var i = 0; i < 20; i++) {
            e.api.solicitarCodigo(String.format("22211%05d", i), IP);
        }

        assertThatCode(() -> e.api.solicitarCodigo("2229999999", "201.100.5.9")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("una IP desconocida se agrupa con las demas desconocidas, sin fallar")
    void solicitar_ipNula_seTrataComoDesconocida() {
        assertThatCode(() -> e.api.solicitarCodigo(TEL_A, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SI-02: pedir un codigo para un celular con cuenta y para uno sin cuenta responde igual; solo la cuenta recibe correo")
    void solicitar_conYSinCuenta_respondeIgual() {
        cuentaDeAna();
        var antes = e.canal.envios().size();

        assertThatCode(() -> e.api.solicitarCodigo(TEL_A, IP)).doesNotThrowAnyException();
        assertThatCode(() -> e.api.solicitarCodigo(TEL_B, IP)).doesNotThrowAnyException();

        assertThat(e.canal.envios()).hasSize(antes + 1);
        assertThat(e.canal.ultimoCodigo()).matches("\\d{6}");
    }

    @Test
    @DisplayName("si el canal esta caido responde ERROR_TEMPORAL y no envia nada")
    void solicitar_canalCaido_fallaConErrorTemporal() {
        cuentaDeAna();
        var antes = e.canal.envios().size();
        e.canal.caido(true);

        fallaCon(() -> e.api.solicitarCodigo(TEL_A, IP), CodigoError.ERROR_TEMPORAL);

        assertThat(e.canal.envios()).hasSize(antes);
    }
}
