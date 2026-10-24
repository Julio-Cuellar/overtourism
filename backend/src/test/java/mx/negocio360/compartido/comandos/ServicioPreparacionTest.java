package mx.negocio360.compartido.comandos;

import mx.negocio360.compartido.pruebas.Permisos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.pruebas.RelojMutable;
import mx.negocio360.compartido.pruebas.RepositorioPreparacionesEnMemoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * D-23, D-55: patron preparar -> confirmar generico que usan Ventas y Caja. Mismas garantias que el de Clientes
 * ({@code clientes.aplicacion.PreparacionTest}), mas el ambito para contar preparaciones vigentes (CJ-07) y la
 * comparacion del resumen al confirmar (VT-05).
 */
class ServicioPreparacionTest {

    /** Comando de prueba: un texto y un ambito opcional. */
    record ComandoPrueba(String valor, String ambito) implements Comando {
    }

    /** Fallo traducido que lanza el servicio en estas pruebas. */
    static final class FalloPrueba extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final FalloPreparacion fallo;

        FalloPrueba(FalloPreparacion fallo) {
            super(fallo.name());
            this.fallo = fallo;
        }
    }

    /** Manejador que anota cada llamada; su resumen depende de un valor externo que la prueba puede cambiar. */
    static final class ManejadorPrueba implements ManejadorComando<ComandoPrueba> {
        final List<String> confirmados = new ArrayList<>();
        String mundo = "igual";

        @Override
        public String tipo() {
            return "prueba";
        }

        @Override
        public Class<ComandoPrueba> claseComando() {
            return ComandoPrueba.class;
        }

        @Override
        public String preparar(ContextoNegocio contexto, ComandoPrueba comando) {
            if (comando.valor().equals("malo")) {
                throw new IllegalArgumentException("comando invalido");
            }
            return "resumen de " + comando.valor() + " con mundo " + mundo;
        }

        @Override
        public ResultadoComando confirmar(ContextoNegocio contexto, ComandoPrueba comando, String resumenPreparado) {
            if (!resumenPreparado.equals(preparar(contexto, comando))) {
                throw new FalloPrueba(FalloPreparacion.COMANDO_CAMBIO);
            }
            confirmados.add(comando.valor());
            return ResultadoComando.ok("Hecho.", UUID.randomUUID(), Map.of("valor", comando.valor()));
        }

        @Override
        public String ambito(ComandoPrueba comando) {
            return comando.ambito();
        }
    }

    private final RelojMutable reloj = new RelojMutable();
    private final RepositorioPreparacionesEnMemoria repositorio = new RepositorioPreparacionesEnMemoria();
    private final ManejadorPrueba manejador = new ManejadorPrueba();
    private final ServicioPreparacion servicio = new ServicioPreparacion(repositorio, List.of(manejador), reloj,
            FalloPrueba::new);
    private final ContextoNegocio ctx = new ContextoNegocio(UUID.randomUUID(), "cajero-1");

    private static FalloPreparacion falloDe(Runnable accion) {
        var error = catchThrowable(accion::run);
        assertThat(error).isInstanceOf(FalloPrueba.class);
        return ((FalloPrueba) error).fallo;
    }

    @Test
    @DisplayName("preparar devuelve un token opaco, el resumen y cinco minutos de vigencia (D-23)")
    void preparar_comandoValido_devuelveTokenResumenYVigencia() {
        var creada = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", null));

        assertThat(creada.token()).matches("[A-Za-z0-9_-]{43}");
        assertThat(creada.resumen()).isEqualTo("resumen de a con mundo igual");
        assertThat(creada.expiraEn()).isEqualTo(RelojMutable.AHORA.plus(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("solo se guarda el hash del token, ligado al negocio, al actor, al comando y su ambito")
    void preparar_guardaSoloElHashDelToken() {
        var creada = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", "turno-7"));

        var fila = repositorio.unica();
        assertThat(fila.tokenHash()).matches("[0-9a-f]{64}").isNotEqualTo(creada.token());
        assertThat(fila.negocioId()).isEqualTo(ctx.negocioId());
        assertThat(fila.actor()).isEqualTo("cajero-1");
        assertThat(fila.comandoTipo()).isEqualTo("prueba");
        assertThat(fila.ambito()).isEqualTo("turno-7");
        assertThat(fila.usadoEn()).isNull();
    }

    @Test
    @DisplayName("preparar no ejecuta el comando; confirmar lo ejecuta una vez y devuelve sus datos")
    void confirmar_tokenValido_ejecutaUnaVez() {
        var creada = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", null));
        assertThat(manejador.confirmados).isEmpty();

        var resultado = servicio.confirmar(ctx, Permisos.de(ctx), creada.token());

        assertThat(resultado.codigo()).isEqualTo(ResultadoComando.OK);
        assertThat(resultado.datos()).containsEntry("valor", "a");
        assertThat(manejador.confirmados).containsExactly("a");
    }

    @Test
    @DisplayName("un comando invalido falla al preparar y no guarda nada")
    void preparar_comandoInvalido_noGuardaNada() {
        assertThatThrownBy(() -> servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("malo", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(repositorio.todas()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "desconocido", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    @DisplayName("un token nulo, vacio o desconocido falla con TOKEN_INVALIDO")
    void confirmar_tokenDesconocido_tokenInvalido(String token) {
        assertThat(falloDe(() -> servicio.confirmar(ctx, Permisos.de(ctx), token))).isEqualTo(FalloPreparacion.TOKEN_INVALIDO);
    }

    @Test
    @DisplayName("un token de mas de 64 caracteres falla con TOKEN_INVALIDO sin buscarlo")
    void confirmar_tokenLarguisimo_tokenInvalido() {
        assertThat(falloDe(() -> servicio.confirmar(ctx, Permisos.de(ctx), "x".repeat(65))))
                .isEqualTo(FalloPreparacion.TOKEN_INVALIDO);
    }

    @Test
    @DisplayName("el token de otro negocio o de otro actor falla con TOKEN_INVALIDO")
    void confirmar_tokenAjeno_tokenInvalido() {
        var token = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", null)).token();

        var otroNegocio = new ContextoNegocio(UUID.randomUUID(), "cajero-1");
        var otroActor = new ContextoNegocio(ctx.negocioId(), "cajero-2");
        assertThat(falloDe(() -> servicio.confirmar(otroNegocio, Permisos.de(otroNegocio), token))).isEqualTo(FalloPreparacion.TOKEN_INVALIDO);
        assertThat(falloDe(() -> servicio.confirmar(otroActor, Permisos.de(otroActor), token))).isEqualTo(FalloPreparacion.TOKEN_INVALIDO);
        assertThat(manejador.confirmados).isEmpty();
    }

    @Test
    @DisplayName("confirmar dos veces con el mismo token falla la segunda con TOKEN_USADO")
    void confirmar_dosVeces_tokenUsado() {
        var token = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", null)).token();
        servicio.confirmar(ctx, Permisos.de(ctx), token);

        assertThat(falloDe(() -> servicio.confirmar(ctx, Permisos.de(ctx), token))).isEqualTo(FalloPreparacion.TOKEN_USADO);
        assertThat(manejador.confirmados).containsExactly("a");
    }

    @Test
    @DisplayName("si otra peticion gana la carrera por el token, falla con TOKEN_USADO y no ejecuta")
    void confirmar_carreraPerdida_tokenUsado() {
        var token = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", null)).token();
        repositorio.simularCarreraDeUso = true;

        assertThat(falloDe(() -> servicio.confirmar(ctx, Permisos.de(ctx), token))).isEqualTo(FalloPreparacion.TOKEN_USADO);
        assertThat(manejador.confirmados).isEmpty();
    }

    @Test
    @DisplayName("a los cinco minutos exactos el token vencio: TOKEN_EXPIRADO")
    void confirmar_aLosCincoMinutos_tokenExpirado() {
        var token = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", null)).token();
        reloj.avanzar(Duration.ofMinutes(5));

        assertThat(falloDe(() -> servicio.confirmar(ctx, Permisos.de(ctx), token))).isEqualTo(FalloPreparacion.TOKEN_EXPIRADO);
        assertThat(repositorio.unica().comando()).isNull();
    }

    @Test
    @DisplayName("un segundo antes de vencer el token todavia sirve")
    void confirmar_unSegundoAntes_sirve() {
        var token = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", null)).token();
        reloj.avanzar(Duration.ofMinutes(5).minusSeconds(1));

        assertThat(servicio.confirmar(ctx, Permisos.de(ctx), token).codigo()).isEqualTo(ResultadoComando.OK);
    }

    @Test
    @DisplayName("un comando alterado en la base falla con COMANDO_CAMBIO y no se ejecuta")
    void confirmar_comandoAlterado_comandoCambio() {
        var token = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", null)).token();
        var fila = repositorio.unica();
        repositorio.alterarComando(fila.tokenHash(), "{\"ambito\":null,\"valor\":\"b\"}");

        assertThat(falloDe(() -> servicio.confirmar(ctx, Permisos.de(ctx), token))).isEqualTo(FalloPreparacion.COMANDO_CAMBIO);
        assertThat(manejador.confirmados).isEmpty();
    }

    @Test
    @DisplayName("si el mundo cambio entre preparar y confirmar, el manejador responde COMANDO_CAMBIO (VT-05)")
    void confirmar_mundoCambio_comandoCambio() {
        var token = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", null)).token();
        manejador.mundo = "distinto";

        assertThat(falloDe(() -> servicio.confirmar(ctx, Permisos.de(ctx), token))).isEqualTo(FalloPreparacion.COMANDO_CAMBIO);
        assertThat(repositorio.unica().usadoEn()).isNotNull();
        assertThat(repositorio.unica().comando()).isNull();
    }

    @Test
    @DisplayName("vigentes cuenta solo las preparaciones sin usar ni vencer de ese tipo y ambito (CJ-07)")
    void vigentes_cuentaPorTipoYAmbito() {
        servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", "turno-1"));
        servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("b", "turno-1"));
        servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("c", "turno-2"));
        var usada = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("d", "turno-1")).token();
        servicio.confirmar(ctx, Permisos.de(ctx), usada);

        assertThat(servicio.vigentes(ctx, "prueba", "turno-1")).isEqualTo(2);
        assertThat(servicio.vigentes(ctx, "prueba", "turno-2")).isEqualTo(1);

        reloj.avanzar(Duration.ofMinutes(5));
        assertThat(servicio.vigentes(ctx, "prueba", "turno-1")).isZero();
    }

    @Test
    @DisplayName("dos manejadores con el mismo tipo se rechazan al armar el servicio")
    void constructor_manejadorRepetido_falla() {
        assertThatThrownBy(() -> new ServicioPreparacion(repositorio, List.of(manejador, new ManejadorPrueba()),
                reloj, FalloPrueba::new)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("preparar un comando sin manejador es un error de programacion")
    void preparar_sinManejador_falla() {
        Comando otro = new Comando() {
        };
        assertThatThrownBy(() -> servicio.preparar(ctx, Permisos.de(ctx), otro)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PreparacionCreada no imprime el token")
    void preparacionCreada_toString_noImprimeElToken() {
        var creada = servicio.preparar(ctx, Permisos.de(ctx), new ComandoPrueba("a", null));

        assertThat(creada.toString()).doesNotContain(creada.token());
    }
}
