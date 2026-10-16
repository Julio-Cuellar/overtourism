package mx.negocio360.inventario.aplicacion;

import static mx.negocio360.inventario.fakes.Afirmaciones.fallaCon;
import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.fakes.Escenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 6.2: preparar -> confirmar con token de un solo uso, ligado al negocio, al actor y al comando (D-22, D-23). */
class PreparacionTest {

    private final Escenario e = new Escenario();
    private UUID harina;

    @BeforeEach
    void crearHarina() {
        harina = e.harina(e.ctxA);
        e.entrada(e.ctxA, harina, "2", "kg", "18");
    }

    private String prepararSalida() {
        return e.api.preparar(e.ctxA, new RegistrarSalidaComando(harina, d("100"), "g")).token();
    }

    private int movimientos() {
        return e.almacen.movimientos().size();
    }

    @Test
    @DisplayName("preparar no escribe; guarda solo el hash del token y vence a los 5 minutos")
    void preparar_noEscribeYGuardaHash() {
        var antes = movimientos();
        var preparada = e.api.preparar(e.ctxA, new RegistrarSalidaComando(harina, d("100"), "g"));

        assertThat(movimientos()).isEqualTo(antes);
        assertThat(preparada.expiraEn()).isEqualTo(Escenario.AHORA.plus(Duration.ofMinutes(5)));
        assertThat(preparada.token()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(preparada.toString()).doesNotContain(preparada.token());
    }

    @Test
    @DisplayName("confirmar escribe una vez; el mismo token otra vez falla con TOKEN_USADO")
    void confirmar_dosVeces_tokenUsado() {
        var token = prepararSalida();
        var antes = movimientos();

        var resultado = e.api.confirmar(e.ctxA, token);

        assertThat(resultado.codigo()).isEqualTo(ResultadoComando.OK);
        assertThat(resultado.mensajePlantilla()).isEqualTo(PlantillasInventario.MOVIMIENTO_REGISTRADO);
        assertThat(movimientos()).isEqualTo(antes + 1);
        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.TOKEN_USADO);
        assertThat(movimientos()).isEqualTo(antes + 1);
    }

    @Test
    @DisplayName("un token vencido falla con TOKEN_EXPIRADO y no escribe")
    void confirmar_vencido_tokenExpirado() {
        var token = prepararSalida();
        var antes = movimientos();
        e.reloj.avanzar(Duration.ofMinutes(5));

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.TOKEN_EXPIRADO);
        assertThat(movimientos()).isEqualTo(antes);
    }

    @Test
    @DisplayName("el token de otro negocio o de otro actor no existe para quien confirma")
    void confirmar_otroNegocioOActor_tokenInvalido() {
        var token = prepararSalida();

        fallaCon(() -> e.api.confirmar(e.ctxB, token), CodigoError.TOKEN_INVALIDO);
        fallaCon(() -> e.api.confirmar(new ContextoNegocio(e.negocioA, "otro-empleado"), token),
                CodigoError.TOKEN_INVALIDO);
        e.api.confirmar(e.ctxA, token);
    }

    @Test
    @DisplayName("tokens vacios, largos o desconocidos fallan con TOKEN_INVALIDO")
    void confirmar_tokenMalFormado_tokenInvalido() {
        fallaCon(() -> e.api.confirmar(e.ctxA, null), CodigoError.TOKEN_INVALIDO);
        fallaCon(() -> e.api.confirmar(e.ctxA, " "), CodigoError.TOKEN_INVALIDO);
        fallaCon(() -> e.api.confirmar(e.ctxA, "x".repeat(65)), CodigoError.TOKEN_INVALIDO);
        fallaCon(() -> e.api.confirmar(e.ctxA, "desconocido"), CodigoError.TOKEN_INVALIDO);
    }

    @Test
    @DisplayName("si el saldo cambio entre preparar y confirmar, el resumen ya no vale: COMANDO_CAMBIO")
    void confirmar_saldoCambio_comandoCambio() {
        var token = e.api.preparar(e.ctxA, new AjustePorConteoComando(harina, d("1.5"), "kg")).token();
        e.salida(e.ctxA, harina, "300", "g");
        var antes = movimientos();

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.COMANDO_CAMBIO);
        assertThat(movimientos()).isEqualTo(antes);
        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.TOKEN_USADO);
    }

    @Test
    @DisplayName("un comando alterado en la tabla no coincide con su hash: COMANDO_CAMBIO")
    void confirmar_comandoAlterado_comandoCambio() {
        var token = prepararSalida();
        var fila = e.preparaciones.ultima();
        e.preparaciones.alterarComando(fila.tokenHash(), fila.comando().replace("100", "999"));

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.COMANDO_CAMBIO);
    }

    @Test
    @DisplayName("si otra confirmacion gana la carrera por el token, esta falla con TOKEN_USADO")
    void confirmar_carreraDeUso_tokenUsado() {
        var token = prepararSalida();
        e.preparaciones.simularCarreraDeUso = true;

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.TOKEN_USADO);
    }

    @Test
    @DisplayName("un comando sin manejador es un error de programacion, no de la persona")
    void preparar_comandoSinManejador_falla() {
        Comando desconocido = new Comando() {
        };

        assertThatThrownBy(() -> e.api.preparar(e.ctxA, desconocido)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("dos manejadores con el mismo tipo no se pueden registrar")
    void servicio_manejadorRepetido_falla() {
        var manejadores = Manejadores.todos(e.almacen, e.almacen, e.almacen, e.eventos, e.reloj);
        var repetidos = new java.util.ArrayList<ManejadorComando<?>>(manejadores);
        repetidos.add(manejadores.get(0));

        assertThatThrownBy(() -> new ServicioPreparacion(e.preparaciones, List.copyOf(repetidos), e.reloj))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
