package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.util.List;
import mx.negocio360.compartido.graphql.Acceso;
import mx.negocio360.compartido.graphql.ComandoDeclarado;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.compartido.graphql.TablaDeOperaciones;
import mx.negocio360.compartido.graphql.TipoDeOperacion;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SG-21 y SG-03: la tabla de operaciones. Cada campo de la raiz del esquema tiene que estar declarado, con su permiso, y
 * cada variante de comando tambien. Si falta una o sobra otra, la aplicacion no arranca: una operacion que alguien
 * agrego al esquema y olvido declarar no puede quedar abierta ni quedar fuera del inventario.
 */
class TablaDeOperacionesTest {

    private static final String ESQUEMA = """
            type Query { listar: String ver(id: ID!): String }
            type Mutation { prepararComando(input: Comando!): String confirmar(token: String!): String }
            input Comando @oneOf { alta: AltaInput baja: BajaInput }
            input AltaInput { nombre: String! }
            input BajaInput { id: ID! }
            """;

    private static final List<PermisoDeclarado> CATALOGO = List.of(
            new PermisoDeclarado("clientes.lista.ver", "Ver la lista", false),
            new PermisoDeclarado("clientes.ficha.ver", "Ver la ficha", false),
            new PermisoDeclarado("clientes.cliente.registrar", "Registrar", false),
            new PermisoDeclarado("clientes.cliente.anonimizar", "Anonimizar", true));

    private static GraphQLSchema esquema(String sdl) {
        return new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), RuntimeWiring.MOCKED_WIRING);
    }

    private static OperacionesGraphql fuente(List<OperacionDeclarada> operaciones, List<ComandoDeclarado> comandos) {
        return new OperacionesGraphql() {
            @Override
            public List<OperacionDeclarada> operaciones() {
                return operaciones;
            }

            @Override
            public List<ComandoDeclarado> comandos() {
                return comandos;
            }
        };
    }

    private static List<OperacionDeclarada> completas() {
        return List.of(OperacionDeclarada.de(TipoDeOperacion.QUERY, "listar", "clientes.lista.ver"),
                OperacionDeclarada.de(TipoDeOperacion.QUERY, "ver", "clientes.ficha.ver"),
                OperacionDeclarada.porComando(TipoDeOperacion.MUTATION, "prepararComando", "input"),
                OperacionDeclarada.deSesion(TipoDeOperacion.MUTATION, "confirmar"));
    }

    private static List<ComandoDeclarado> variantes() {
        return List.of(new ComandoDeclarado("alta", "clientes.cliente.registrar"),
                new ComandoDeclarado("baja", "clientes.cliente.anonimizar"));
    }

    private static TablaDeOperaciones tabla(List<OperacionDeclarada> operaciones, List<ComandoDeclarado> comandos) {
        return new TablaDeOperaciones(List.of(fuente(operaciones, comandos)), CATALOGO);
    }

    private static void falla(TablaDeOperaciones tabla, String sdl, String... pistas) {
        assertThatThrownBy(() -> tabla.verificar(esquema(sdl))).isInstanceOf(IllegalStateException.class)
                .satisfies(error -> assertThat(error.getMessage()).contains(pistas));
    }

    @Test
    @DisplayName("una tabla completa y consistente verifica sin problemas")
    void tablaCompleta_verifica() {
        tabla(completas(), variantes()).verificar(esquema(ESQUEMA));
    }

    @Test
    @DisplayName("SG-21: un campo del esquema que nadie declaro impide el arranque y se nombra")
    void campoSinDeclarar_impideElArranque() {
        var sinVer = completas().stream().filter(o -> !o.campo().equals("ver")).toList();

        falla(tabla(sinVer, variantes()), ESQUEMA, "Query.ver");
    }

    @Test
    @DisplayName("una declaracion de un campo que no existe (un error de dedo) tambien impide el arranque")
    void declaracionDeCampoInexistente_impideElArranque() {
        var conFantasma = new java.util.ArrayList<>(completas());
        conFantasma.add(OperacionDeclarada.deSesion(TipoDeOperacion.QUERY, "fantasma"));

        falla(tabla(conFantasma, variantes()), ESQUEMA, "Query.fantasma");
    }

    @Test
    @DisplayName("una declaracion en un tipo de raiz que el esquema no tiene (no hay suscripciones) impide el arranque")
    void declaracionEnTipoInexistente_impideElArranque() {
        var conSuscripcion = new java.util.ArrayList<>(completas());
        conSuscripcion.add(OperacionDeclarada.deSesion(TipoDeOperacion.SUBSCRIPTION, "alertas"));

        falla(tabla(conSuscripcion, variantes()), ESQUEMA, "Subscription.alertas");
    }

    @Test
    @DisplayName("los campos de una suscripcion tambien deben declararse")
    void suscripcionSinDeclarar_impideElArranque() {
        falla(tabla(completas(), variantes()), ESQUEMA + "type Subscription { alertas: String }", "Subscription.alertas");
    }

    @Test
    @DisplayName("un permiso que ningun modulo declara impide el arranque")
    void permisoNoDeclarado_impideElArranque() {
        var malas = new java.util.ArrayList<>(completas());
        malas.set(0, OperacionDeclarada.de(TipoDeOperacion.QUERY, "listar", "clientes.permiso.inventado"));

        falla(tabla(malas, variantes()), ESQUEMA, "clientes.permiso.inventado");
    }

    @Test
    @DisplayName("una variante de comando sin declarar impide el arranque")
    void varianteSinDeclarar_impideElArranque() {
        falla(tabla(completas(), List.of(new ComandoDeclarado("alta", "clientes.cliente.registrar"))), ESQUEMA,
                "baja");
    }

    @Test
    @DisplayName("una variante declarada que el tipo de comando no tiene impide el arranque")
    void varianteInexistente_impideElArranque() {
        var conExtra = new java.util.ArrayList<>(variantes());
        conExtra.add(new ComandoDeclarado("borrarTodo", "clientes.cliente.anonimizar"));

        falla(tabla(completas(), conExtra), ESQUEMA, "borrarTodo");
    }

    @Test
    @DisplayName("el permiso de una variante tambien debe estar declarado")
    void permisoDeVarianteNoDeclarado_impideElArranque() {
        var malas = List.of(new ComandoDeclarado("alta", "clientes.permiso.inventado"),
                new ComandoDeclarado("baja", "clientes.cliente.anonimizar"));

        falla(tabla(completas(), malas), ESQUEMA, "clientes.permiso.inventado");
    }

    @Test
    @DisplayName("un argumento de comando que no es un tipo de entrada con variantes impide el arranque")
    void argumentoDeComandoInvalido_impideElArranque() {
        var malas = new java.util.ArrayList<>(completas());
        malas.set(2, OperacionDeclarada.porComando(TipoDeOperacion.MUTATION, "prepararComando", "noExiste"));

        falla(tabla(malas, variantes()), ESQUEMA, "Mutation.prepararComando");
    }

    @Test
    @DisplayName("se reportan todos los problemas juntos, no solo el primero")
    void variosProblemas_seReportanJuntos() {
        var sinVer = completas().stream().filter(o -> !o.campo().equals("ver")).toList();
        var conFantasma = new java.util.ArrayList<>(sinVer);
        conFantasma.add(OperacionDeclarada.deSesion(TipoDeOperacion.QUERY, "fantasma"));

        falla(tabla(conFantasma, List.of()), ESQUEMA, "Query.ver", "Query.fantasma", "alta", "baja");
    }

    @Test
    @DisplayName("dos declaraciones del mismo campo, o de la misma variante, son un error al construir la tabla")
    void duplicados_fallanAlConstruir() {
        var repetidas = new java.util.ArrayList<>(completas());
        repetidas.add(OperacionDeclarada.deSesion(TipoDeOperacion.QUERY, "listar"));
        assertThatThrownBy(() -> tabla(repetidas, variantes())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Query.listar");

        var variantesRepetidas = new java.util.ArrayList<>(variantes());
        variantesRepetidas.add(new ComandoDeclarado("alta", "clientes.cliente.registrar"));
        assertThatThrownBy(() -> tabla(completas(), variantesRepetidas)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alta");
    }

    @Test
    @DisplayName("las fuentes de varios modulos se juntan en una sola tabla")
    void variasFuentes_seJuntan() {
        var uno = fuente(completas().subList(0, 2), List.of());
        var dos = fuente(completas().subList(2, 4), variantes());

        var tabla = new TablaDeOperaciones(List.of(uno, dos), CATALOGO);

        tabla.verificar(esquema(ESQUEMA));
        assertThat(tabla.operaciones()).hasSize(4);
    }

    @Test
    @DisplayName("consultas: la regla de un campo, el permiso de una variante y si un permiso es sensible")
    void consultas_devuelvenLasReglas() {
        var tabla = tabla(completas(), variantes());

        assertThat(tabla.operacion(TipoDeOperacion.QUERY, "listar")).hasValueSatisfying(
                o -> assertThat(o.permiso()).isEqualTo("clientes.lista.ver"));
        assertThat(tabla.operacion(TipoDeOperacion.QUERY, "nada")).isEmpty();
        assertThat(tabla.operacion(TipoDeOperacion.MUTATION, "prepararComando")).hasValueSatisfying(
                o -> assertThat(o.argumentoDeComando()).isEqualTo("input"));
        assertThat(tabla.permisoDeVariante("alta")).contains("clientes.cliente.registrar");
        assertThat(tabla.permisoDeVariante("nada")).isEmpty();
        assertThat(tabla.sensible("clientes.cliente.anonimizar")).isTrue();
        assertThat(tabla.sensible("clientes.lista.ver")).isFalse();
        assertThat(tabla.sensible("clientes.permiso.inventado")).isFalse();
    }

    @Test
    @DisplayName("una declaracion no puede pedir a la vez un permiso y un comando")
    void declaracionContradictoria_noSeConstruye() {
        assertThatThrownBy(() -> new OperacionDeclarada(TipoDeOperacion.QUERY, "x", "a.b", "input"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OperacionDeclarada.deSesion(TipoDeOperacion.QUERY, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Acceso publico y de cuenta (Negocios: la app del turista no tiene sesion) ---

    @Test
    @DisplayName("una operacion publica no lleva permiso; una de cuenta tampoco; y se distinguen de las que piden negocio")
    void accesos_seDeclaranYSeLeen() {
        var publica = OperacionDeclarada.publica(TipoDeOperacion.QUERY, "listar");
        var deCuenta = OperacionDeclarada.deCuenta(TipoDeOperacion.QUERY, "ver");
        var conPermiso = OperacionDeclarada.de(TipoDeOperacion.QUERY, "otra", "clientes.lista.ver");
        var deSesion = OperacionDeclarada.deSesion(TipoDeOperacion.QUERY, "otra2");

        assertThat(publica.acceso()).isEqualTo(Acceso.PUBLICA);
        assertThat(publica.permiso()).isNull();
        assertThat(deCuenta.acceso()).isEqualTo(Acceso.CUENTA);
        assertThat(deCuenta.permiso()).isNull();
        assertThat(conPermiso.acceso()).isEqualTo(Acceso.NEGOCIO);
        assertThat(deSesion.acceso()).isEqualTo(Acceso.NEGOCIO);
    }

    @Test
    @DisplayName("una operacion publica o de cuenta no puede traer permiso ni comando: no se queda a medias")
    void accesosPublicoYDeCuenta_noAdmitenPermisoNiComando() {
        assertThatThrownBy(() -> new OperacionDeclarada(TipoDeOperacion.QUERY, "x", "a.b", null, Acceso.PUBLICA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperacionDeclarada(TipoDeOperacion.QUERY, "x", null, "input", Acceso.PUBLICA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperacionDeclarada(TipoDeOperacion.QUERY, "x", "a.b", null, Acceso.CUENTA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("una variante de comando puede declararse publica; sin permiso y sin la marca publica no se construye")
    void variantePublica_seDeclaraConMarcaExplicita() {
        var publica = ComandoDeclarado.publico("alta");

        assertThat(publica.publico()).isTrue();
        assertThat(publica.permiso()).isNull();
        assertThat(new ComandoDeclarado("baja", "clientes.cliente.anonimizar").publico()).isFalse();
        assertThatThrownBy(() -> new ComandoDeclarado("baja", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("la tabla distingue las variantes publicas de las que piden permiso, y de las que no existen")
    void variantesPublicas_seConsultan() {
        var tabla = tabla(completas(), List.of(ComandoDeclarado.publico("alta"),
                new ComandoDeclarado("baja", "clientes.cliente.anonimizar")));

        assertThat(tabla.esVariantePublica("alta")).isTrue();
        assertThat(tabla.esVariantePublica("baja")).isFalse();
        assertThat(tabla.esVariantePublica("nada")).isFalse();
        assertThat(tabla.permisoDeVariante("alta")).isEmpty();
        assertThat(tabla.permisoDeVariante("baja")).contains("clientes.cliente.anonimizar");
        assertThat(tabla.variantes()).containsOnlyKeys("baja");
        assertThat(tabla.variantesPublicas()).containsExactly("alta");
    }

    @Test
    @DisplayName("una variante publica y una operacion publica verifican contra el esquema sin pedir permisos al catalogo")
    void publicas_verificanSinPermisos() {
        var operaciones = List.of(OperacionDeclarada.publica(TipoDeOperacion.QUERY, "listar"),
                OperacionDeclarada.deCuenta(TipoDeOperacion.QUERY, "ver"),
                OperacionDeclarada.porComando(TipoDeOperacion.MUTATION, "prepararComando", "input"),
                OperacionDeclarada.publica(TipoDeOperacion.MUTATION, "confirmar"));

        tabla(operaciones, List.of(ComandoDeclarado.publico("alta"),
                new ComandoDeclarado("baja", "clientes.cliente.anonimizar"))).verificar(esquema(ESQUEMA));
    }

    @Test
    @DisplayName("una variante publica del esquema que nadie declaro sigue impidiendo el arranque")
    void variantePublicaSinDeclarar_impideElArranque() {
        falla(tabla(completas(), List.of(ComandoDeclarado.publico("alta"))), ESQUEMA, "baja");
    }
}
