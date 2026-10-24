package mx.negocio360.compartido.graphql.comandos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** D-125: el unico dueno de prepararComando y confirmarComando reparte por variante y por prefijo del token. */
class ComandosGraphQlControllerTest {

    /** Un modulo de prueba que anota lo que recibe. */
    static final class Modulo implements ModuloDeComandos {
        final String prefijo;
        final Set<String> variantes;
        final boolean sinSesion;
        final List<String> preparadas = new ArrayList<>();
        final List<String> confirmados = new ArrayList<>();
        Map<String, Object> ultimosCampos;

        Modulo(String prefijo, boolean sinSesion, String... variantes) {
            this.prefijo = prefijo;
            this.sinSesion = sinSesion;
            this.variantes = Set.of(variantes);
        }

        @Override
        public String prefijo() {
            return prefijo;
        }

        @Override
        public Set<String> variantes() {
            return variantes;
        }

        @Override
        public boolean confirmaSinSesion() {
            return sinSesion;
        }

        @Override
        public PreparacionVista preparar(EntradaGraphQl entrada, String variante, Map<String, Object> campos) {
            preparadas.add(variante);
            ultimosCampos = campos;
            return new PreparacionVista("TOKEN43", "resumen", Instant.parse("2026-10-16T12:00:00Z"), List.of("aviso"));
        }

        @Override
        public ResultadoVista confirmar(EntradaGraphQl entrada, String tokenSinPrefijo) {
            confirmados.add(tokenSinPrefijo);
            return new ResultadoVista(true, "OK", "Hecho.", null, List.of());
        }
    }

    private static final EntradaGraphQl SESION = EntradaGraphQl.de(UUID.randomUUID(),
            new ContextoNegocio(UUID.randomUUID(), "actor"), Autorizacion.de(Set.of()), null);
    private static final EntradaGraphQl ANONIMA = EntradaGraphQl.anonima(null);

    private final Modulo ventas = new Modulo("ventas.", false, "registrarVenta", "anularVenta");
    private final Modulo negocios = new Modulo("", true, "publicarResena");
    private final ComandosGraphQlController despachador = new ComandosGraphQlController(List.of(ventas, negocios));

    private static void rechaza(Runnable accion, ComandoGraphQlException.Codigo esperado) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ComandoGraphQlException.class,
                e -> assertThat(e.codigo()).isEqualTo(esperado));
    }

    // ---- armado ----

    @Test
    @DisplayName("dos modulos con el mismo prefijo o la misma variante impiden armar el despachador")
    void armado_repetidos() {
        assertThatThrownBy(() -> new ComandosGraphQlController(List.of(ventas, new Modulo("ventas.", false, "otra"))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("prefijo");
        assertThatThrownBy(() -> new ComandosGraphQlController(List.of(ventas, new Modulo("caja.", false, "anularVenta"))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("variante");
    }

    @Test
    @DisplayName("un prefijo debe terminar en un solo punto o ser vacio")
    void armado_prefijoMalFormado() {
        for (var malo : List.of("ventas", "ve.ntas.", "ventas..")) {
            assertThatThrownBy(() -> new ComandosGraphQlController(List.of(new Modulo(malo, false, "x"))))
                    .as(malo).isInstanceOf(IllegalStateException.class);
        }
    }

    // ---- prepararComando ----

    @Test
    @DisplayName("prepara con el modulo dueno de la variante y le antepone su prefijo al token")
    void preparar_antepone_prefijo() {
        var campos = new HashMap<String, Object>(Map.of("turnoId", "t1"));

        var vista = despachador.prepararComando(Map.of("registrarVenta", campos), SESION);

        assertThat(vista.token()).isEqualTo("ventas.TOKEN43");
        assertThat(vista.resumen()).isEqualTo("resumen");
        assertThat(vista.advertencias()).containsExactly("aviso");
        assertThat(ventas.preparadas).containsExactly("registrarVenta");
        assertThat(ventas.ultimosCampos).containsEntry("turnoId", "t1");
    }

    @Test
    @DisplayName("el modulo con prefijo vacio entrega el token tal cual")
    void preparar_prefijoVacio() {
        var vista = despachador.prepararComando(Map.of("publicarResena", Map.of("calificacion", 5)), ANONIMA);

        assertThat(vista.token()).isEqualTo("TOKEN43");
    }

    @Test
    @DisplayName("sin una variante unica, o con una que no es un objeto, el comando es invalido")
    void preparar_comandoInvalido() {
        var nulos = new HashMap<String, Object>();
        nulos.put("registrarVenta", null);
        var dos = new HashMap<String, Object>();
        dos.put("registrarVenta", Map.of());
        dos.put("anularVenta", Map.of());

        rechaza(() -> despachador.prepararComando(null, SESION), ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        rechaza(() -> despachador.prepararComando(Map.of(), SESION), ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        rechaza(() -> despachador.prepararComando(nulos, SESION), ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        rechaza(() -> despachador.prepararComando(dos, SESION), ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        rechaza(() -> despachador.prepararComando(Map.of("registrarVenta", "texto"), SESION),
                ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        assertThat(ventas.preparadas).isEmpty();
    }

    @Test
    @DisplayName("una variante que ningun modulo atiende responde COMANDO_NO_DISPONIBLE")
    void preparar_varianteSinDueno() {
        rechaza(() -> despachador.prepararComando(Map.of("registrarConsentimiento", Map.of()), SESION),
                ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE);
    }

    // ---- confirmarComando ----

    @Test
    @DisplayName("confirma con el modulo que indica el prefijo y le quita el prefijo al token")
    void confirmar_quitaPrefijo() {
        var resultado = despachador.confirmarComando("ventas.abc123", SESION);

        assertThat(resultado.ok()).isTrue();
        assertThat(ventas.confirmados).containsExactly("abc123");
        assertThat(negocios.confirmados).isEmpty();
    }

    @Test
    @DisplayName("un token sin punto es del modulo de prefijo vacio, y ese confirma sin sesion")
    void confirmar_tokenSinPunto_esDelModuloHeredado() {
        despachador.confirmarComando("abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG", ANONIMA);

        assertThat(negocios.confirmados).hasSize(1);
        assertThat(ventas.confirmados).isEmpty();
    }

    @Test
    @DisplayName("los modulos que exigen sesion niegan al anonimo y a la sesion sin negocio antes de entregar el token")
    void confirmar_exigeSesion() {
        var sinNegocio = EntradaGraphQl.de(UUID.randomUUID(), null, Autorizacion.de(Set.of()), null);

        assertThatThrownBy(() -> despachador.confirmarComando("ventas.abc", ANONIMA))
                .isInstanceOf(NoAutenticadoException.class);
        assertThatThrownBy(() -> despachador.confirmarComando("ventas.abc", sinNegocio))
                .isInstanceOf(NoAutenticadoException.class);
        assertThat(ventas.confirmados).isEmpty();
    }

    @Test
    @DisplayName("un token vacio, demasiado largo o con un prefijo desconocido responde TOKEN_INVALIDO")
    void confirmar_tokenInvalido() {
        for (var malo : new String[] {null, "", "   ", "x".repeat(129), "caja.abc", "inventado.abc"}) {
            rechaza(() -> despachador.confirmarComando(malo, SESION), ComandoGraphQlException.Codigo.TOKEN_INVALIDO);
        }
        assertThat(ventas.confirmados).isEmpty();
    }

    @Test
    @DisplayName("cada codigo del despachador trae un texto fijo en espanol")
    void codigos_traenTexto() {
        for (var codigo : ComandoGraphQlException.Codigo.values()) {
            assertThat(codigo.mensaje()).isNotBlank();
            assertThat(new ComandoGraphQlException(codigo).getMessage()).isEqualTo(codigo.name());
        }
        var traducido = new TraductorDeErroresDeComandos().traducir(
                new ComandoGraphQlException(ComandoGraphQlException.Codigo.TOKEN_INVALIDO));
        assertThat(traducido).hasValueSatisfying(t -> assertThat(t.codigo()).isEqualTo("TOKEN_INVALIDO"));
        assertThat(new TraductorDeErroresDeComandos().traducir(new RuntimeException("otro"))).isEmpty();
    }
}
