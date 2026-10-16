package mx.negocio360.clientes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import mx.negocio360.clientes.ClientesApi;
import mx.negocio360.clientes.fakes.Escenario;
import mx.negocio360.clientes.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** HTC-28 · I-04, I-05, P-05, SC-02, SC-03: busqueda y sugerencias de clientes. */
class BuscarClientesTest {

    private static final String MASCARA_4567 = "•••• 4567";

    private final Escenario e = new Escenario();

    private UUID sembrar(String nombre, String apellidos, String telefono) {
        var cliente = Fixtures.cliente(e.negocioA, nombre, apellidos, telefono);
        e.clientes.insertar(cliente);
        return cliente.id();
    }

    private List<String> nombres(PaginaClientes pagina) {
        return pagina.nodos().stream().map(ClienteListado::nombreCompleto).toList();
    }

    @Test
    @DisplayName("jose encuentra a Jose Perez: ignora mayusculas y acentos")
    void buscar_sinAcentos_encuentraConAcentos() {
        sembrar("José", "Pérez", null);
        sembrar("María", "López", null);

        assertThat(nombres(e.api.buscar(e.ctxA, "jose", 20, null))).containsExactly("José Pérez");
        assertThat(nombres(e.api.buscar(e.ctxA, "PEREZ", 20, null))).containsExactly("José Pérez");
        assertThat(nombres(e.api.buscar(e.ctxA, "jos", 20, null))).containsExactly("José Pérez");
        assertThat(nombres(e.api.buscar(e.ctxA, "maria lopez", 20, null))).containsExactly("María López");
        assertThat(e.api.buscar(e.ctxA, "xyz", 20, null).nodos()).isEmpty();
    }

    @Test
    @DisplayName("por telefono solo se encuentra con el numero completo")
    void buscar_porTelefono_exigeElNumeroCompleto() {
        sembrar("Ana", null, "222 123 4567");

        assertThat(nombres(e.api.buscar(e.ctxA, "222 123 4567", 20, null))).containsExactly("Ana");
        assertThat(nombres(e.api.buscar(e.ctxA, "+52 222 123 4567", 20, null))).containsExactly("Ana");
        assertThat(e.api.buscar(e.ctxA, "5678", 20, null).nodos()).isEmpty();
        assertThat(e.api.buscar(e.ctxA, "4567", 20, null).nodos()).isEmpty();
        assertThat(e.api.buscar(e.ctxA, "222 123", 20, null).nodos()).isEmpty();
        assertThat(e.api.sugerir(e.ctxA, "45")).isEmpty();
    }

    @Test
    @DisplayName("el correo no es criterio de busqueda ni completo ni parcial")
    void buscar_porCorreo_noEncuentra() {
        e.clientes.insertar(Fixtures.clienteConCorreo(e.negocioA, "Luis", "pepe@correo.test"));

        assertThat(e.api.buscar(e.ctxA, "pepe@correo.test", 20, null).nodos()).isEmpty();
        assertThat(e.api.buscar(e.ctxA, "pepe@correo", 20, null).nodos()).isEmpty();
        assertThat(e.api.buscar(e.ctxA, "pepe", 20, null).nodos()).isEmpty();
    }

    @Test
    @DisplayName("con dos Don Pepe el sistema devuelve ambos y no elige uno (I-05)")
    void sugerir_dosCandidatos_devuelveLaListaCompleta() {
        var uno = sembrar("Don Pepe", "Ramos", "222 111 1111");
        var dos = sembrar("Don Pepe", "Soto", "222 222 2222");

        var sugeridos = e.api.sugerir(e.ctxA, "pepe");

        assertThat(sugeridos).extracting(ClienteListado::id).containsExactlyInAnyOrder(uno, dos);
    }

    @Test
    @DisplayName("ninguna operacion de ClientesApi que reciba un texto elige un solo cliente (I-05)")
    void clientesApi_operacionesConTexto_siempreDevuelvenListas() {
        var conTexto = Arrays.stream(ClientesApi.class.getDeclaredMethods())
                .filter(m -> Arrays.asList(m.getParameterTypes()).contains(String.class))
                .filter(m -> !m.getName().equals("confirmar")).toList();

        assertThat(conTexto).extracting(Method::getName).containsExactlyInAnyOrder("buscar", "sugerir");
        for (var operacion : conTexto) {
            assertThat(operacion.getReturnType()).isIn(PaginaClientes.class, List.class);
        }
    }

    @Test
    @DisplayName("un criterio que no puede coincidir con nadie ni siquiera consulta el repositorio")
    void buscar_criterioNinguno_noConsultaElRepositorio() {
        sembrar("Ana", null, "222 123 4567");

        assertThat(e.api.buscar(e.ctxA, "5678", 20, null).nodos()).isEmpty();
        assertThat(e.api.sugerir(e.ctxA, "45")).isEmpty();
        assertThat(e.clientes.consultasDeBusqueda).isZero();
        e.api.buscar(e.ctxA, "ana", 20, null);
        assertThat(e.clientes.consultasDeBusqueda).isEqualTo(1);
    }

    @Test
    @DisplayName("un texto que se queda sin letras al quitar los acentos no devuelve a todos")
    void sugerir_soloMarcasCombinantes_noDevuelveNada() {
        sembrar("Ana", null, null);

        assertThat(e.api.sugerir(e.ctxA, "́́")).isEmpty();
        assertThat(e.api.buscar(e.ctxA, "́́", 20, null).nodos()).isEmpty();
    }

    @Test
    @DisplayName("el cursor debe ser un UUID en su forma canonica, no una forma abreviada")
    void buscar_cursorAbreviado_lanzaIllegalArgument() {
        assertThatThrownBy(() -> e.api.buscar(e.ctxA, null, 20, "1-1-1-1-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un telefono pegado con espacios no separables se busca como telefono")
    void buscar_telefonoConEspaciosUnicode_encuentraAlCliente() {
        sembrar("Ana", null, "222 123 4567");

        assertThat(nombres(e.api.buscar(e.ctxA, "222 123 4567", 20, null))).containsExactly("Ana");
    }

    @Test
    @DisplayName("sugerir devuelve como maximo 5")
    void sugerir_masDeCinco_devuelveCinco() {
        for (int i = 0; i < 7; i++) {
            sembrar("Pepe " + i, null, null);
        }

        assertThat(e.api.sugerir(e.ctxA, "pepe")).hasSize(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", " a ", "   "})
    @DisplayName("sugerir exige al menos dos caracteres")
    void sugerir_textoCorto_lanzaIllegalArgument(String texto) {
        assertThatThrownBy(() -> e.api.sugerir(e.ctxA, texto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("sugerir rechaza texto nulo o de mas de 80 caracteres")
    void sugerir_textoNuloOLargo_lanzaIllegalArgument() {
        assertThatThrownBy(() -> e.api.sugerir(e.ctxA, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.api.sugerir(e.ctxA, "a".repeat(81))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("buscar rechaza texto de mas de 80 caracteres, pagina en cero y cursor invalido")
    void buscar_argumentosInvalidos_lanzaIllegalArgument() {
        assertThatThrownBy(() -> e.api.buscar(e.ctxA, "a".repeat(81), 20, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.api.buscar(e.ctxA, "ana", 0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.api.buscar(e.ctxA, "ana", 20, "no-es-un-cursor"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("las listas muestran el telefono enmascarado")
    void buscar_cliente_telefonoEnmascarado() {
        sembrar("Ana", null, "222 123 4567");
        sembrar("Beto", null, null);

        var pagina = e.api.buscar(e.ctxA, null, 20, null);

        assertThat(pagina.nodos()).extracting(ClienteListado::telefonoEnmascarado)
                .containsExactlyInAnyOrder(MASCARA_4567, null);
        assertThat(pagina.toString()).doesNotContain("2221234567");
    }

    @Test
    @DisplayName("las listas no exponen correo ni fecha de nacimiento (SC-02)")
    void clienteListado_componentes_soloIdNombreYTelefonoEnmascarado() {
        var componentes = Arrays.stream(ClienteListado.class.getRecordComponents()).map(c -> c.getName()).toList();

        assertThat(componentes).containsExactly("id", "nombreCompleto", "telefonoEnmascarado");
    }

    @Test
    @DisplayName("pagina por cursor sin repetir ni saltar clientes")
    void buscar_conCursor_recorreTodosUnaVez() {
        for (int i = 0; i < 5; i++) {
            sembrar("Cliente " + i, null, null);
        }

        var primera = e.api.buscar(e.ctxA, null, 2, null);
        var segunda = e.api.buscar(e.ctxA, null, 2, primera.cursorFinal());
        var tercera = e.api.buscar(e.ctxA, null, 2, segunda.cursorFinal());

        assertThat(primera.nodos()).hasSize(2);
        assertThat(primera.hayMas()).isTrue();
        assertThat(segunda.nodos()).hasSize(2);
        assertThat(segunda.hayMas()).isTrue();
        assertThat(tercera.nodos()).hasSize(1);
        assertThat(tercera.hayMas()).isFalse();
        var ids = new ArrayList<UUID>();
        for (var pagina : List.of(primera, segunda, tercera)) {
            pagina.nodos().forEach(n -> ids.add(n.id()));
        }
        assertThat(new HashSet<>(ids)).hasSize(5);
        assertThat(primera.cursorFinal()).isEqualTo(primera.nodos().get(1).id().toString());
    }

    @Test
    @DisplayName("la pagina se recorta a 50 aunque pidan mas")
    void buscar_pideMasDe50_devuelve50() {
        for (int i = 0; i < 60; i++) {
            sembrar("Cliente " + i, null, null);
        }

        var pagina = e.api.buscar(e.ctxA, null, 500, null);

        assertThat(pagina.nodos()).hasSize(50);
        assertThat(pagina.hayMas()).isTrue();
    }

    @Test
    @DisplayName("sin resultados la pagina viene vacia y sin cursor")
    void buscar_sinResultados_paginaVaciaSinCursor() {
        var pagina = e.api.buscar(e.ctxA, "nadie", 20, null);

        assertThat(pagina.nodos()).isEmpty();
        assertThat(pagina.hayMas()).isFalse();
        assertThat(pagina.cursorFinal()).isNull();
    }

    @Test
    @DisplayName("un negocio no ve los clientes de otro")
    void buscar_otroNegocio_noVeLosClientes() {
        sembrar("Ana", null, "222 123 4567");

        assertThat(e.api.buscar(e.ctxB, "ana", 20, null).nodos()).isEmpty();
        assertThat(e.api.buscar(e.ctxB, "222 123 4567", 20, null).nodos()).isEmpty();
        assertThat(e.api.buscar(e.ctxB, null, 20, null).nodos()).isEmpty();
        assertThat(e.api.sugerir(e.ctxB, "an")).isEmpty();
    }

    @Test
    @DisplayName("los clientes anonimizados no aparecen")
    void buscar_clienteAnonimizado_noAparece() {
        e.clientes.insertar(Fixtures.anonimizado(e.negocioA));
        sembrar("Ana", null, null);

        assertThat(nombres(e.api.buscar(e.ctxA, null, 20, null))).containsExactly("Ana");
        assertThat(e.api.buscar(e.ctxA, "anonimizado", 20, null).nodos()).isEmpty();
    }

    @Test
    @DisplayName("el cliente recien registrado por el panel se puede buscar")
    void buscar_clienteRegistradoPorElPanel_loEncuentra() {
        e.conAviso(e.ctxA);
        var id = e.registrar(e.ctxA, Fixtures.registrar("Ana", "Pérez", "222 123 4567"));

        assertThat(e.api.sugerir(e.ctxA, "perez")).extracting(ClienteListado::id).containsExactly(id);
    }
}
