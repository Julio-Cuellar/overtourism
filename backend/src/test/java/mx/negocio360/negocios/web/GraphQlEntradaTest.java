package mx.negocio360.negocios.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.negocios.ActualizarSucursal;
import mx.negocio360.negocios.DefinirAtributos;
import mx.negocio360.negocios.DiaHorarioComando;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.TramoComando;
import mx.negocio360.negocios.aplicacion.AltasApi;
import mx.negocio360.negocios.contratos.NegocioRegistroSolicitadoV1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * IDN-4 y los resolvedores de Negocios, de punta a punta por HTTP: el filtro de entrada, la sesion como contexto y
 * permisos, los limites de la consulta, los errores enmascarados y cada operacion del esquema. La sesion se simula con
 * una Identidad de mentira porque los roles base de Identidad todavia no traen los permisos {@code negocios.*}.
 */
@SpringBootTest(properties = "negocios.demo.activo=true")
@AutoConfigureMockMvc
class GraphQlEntradaTest {

    private static final String TODOS = "negocios.perfil.ver negocios.perfil.editar negocios.sucursal.gestionar "
            + "negocios.horario.editar negocios.negocio.publicar negocios.disponibilidad.editar negocios.resena.ver "
            + "negocios.resena.responder";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AltasApi altas;
    @Autowired
    private NegociosApi panel;
    @Autowired
    private mx.negocio360.negocios.infraestructura.AvisosDeAlta avisos;
    @MockitoBean
    private IdentidadApi identidad;

    private int ip;

    @BeforeEach
    void ipNueva() {
        ip = (int) (Math.random() * 200) + 20;
    }

    /** Da de alta un negocio y arma la sesion de su dueño con esos permisos. */
    private record Dueno(UUID negocioId, UUID cuentaId, String token) {
    }

    private Dueno dueno(String permisos, boolean conNegocio) {
        var ahora = java.time.Instant.parse("2026-10-16T16:00:00Z");
        var pedido = new NegocioRegistroSolicitadoV1(UuidV7.generar(ahora), UuidV7.generar(ahora), "Fonda Zg"
                + UUID.randomUUID().toString().substring(0, 8), "RESTAURANTE", UuidV7.generar(ahora),
                UuidV7.generar(ahora));
        altas.registrarAlta(pedido);
        var token = "T" + UUID.randomUUID().toString().replace("-", "") + "xxxxxxxxxx";
        when(identidad.autenticar(token)).thenReturn(Optional.of(new SesionAutenticada(pedido.cuentaId(), "Ana",
                conNegocio ? pedido.negocioId() : null, conNegocio ? "DUENO" : null,
                conNegocio ? Set.of(permisos.split(" ")) : Set.of(), ahora.plusSeconds(1800))));
        return new Dueno(pedido.negocioId(), pedido.cuentaId(), token);
    }

    private JsonNode graphql(String token, String dispositivo, String consulta, Map<String, Object> variables)
            throws Exception {
        var cuerpo = JSON.writeValueAsString(Map.of("query", consulta, "variables", variables));
        var peticion = post("/graphql").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .with(r -> {
                    r.setRemoteAddr("10.9." + ip + "." + (int) (Math.random() * 250 + 1));
                    return r;
                });
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        if (dispositivo != null) {
            peticion.header("X-Dispositivo-Id", dispositivo);
        }
        var respuesta = mvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return JSON.readTree(respuesta);
    }

    private JsonNode graphql(String token, String consulta) throws Exception {
        return graphql(token, null, consulta, Map.of());
    }

    private static String codigoDelPrimerError(JsonNode respuesta) {
        return respuesta.at("/errors/0/extensions/codigo").asText();
    }

    // ---- sesion y permisos ----

    @Test
    @DisplayName("SG-01: miNegocio sin sesion responde NO_AUTENTICADO, sin datos")
    void miNegocio_sinSesion() throws Exception {
        var r = graphql(null, "{ miNegocio { id nombre } }");

        assertThat(codigoDelPrimerError(r)).isEqualTo("NO_AUTENTICADO");
        assertThat(r.get("data").isNull() || r.at("/data/miNegocio").isNull()).isTrue();
    }

    @Test
    @DisplayName("una sesion sin negocio activo no puede usar el panel: SIN_NEGOCIO_ACTIVO")
    void miNegocio_sinNegocioActivo() throws Exception {
        var d = dueno(TODOS, false);

        assertThat(codigoDelPrimerError(graphql(d.token(), "{ miNegocio { id } }"))).isEqualTo("SIN_NEGOCIO_ACTIVO");
    }

    @Test
    @DisplayName("SG-04: sin el permiso, PERMISO_DENEGADO, y el mensaje no dice cual permiso falto")
    void miNegocio_sinPermiso() throws Exception {
        var d = dueno("negocios.horario.editar", true);

        var r = graphql(d.token(), "{ miNegocio { id } }");

        assertThat(codigoDelPrimerError(r)).isEqualTo("PERMISO_DENEGADO");
        assertThat(r.toString()).doesNotContain("negocios.perfil.ver");
    }

    @Test
    @DisplayName("un token que Identidad no reconoce es 401 y nunca llega al resolvedor")
    void tokenInvalido() throws Exception {
        when(identidad.autenticar("a".repeat(43))).thenReturn(Optional.empty());

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"{ miNegocio { id } }\"}")
                .header("Authorization", "Bearer " + "a".repeat(43))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("G1: miNegocio de la sesion devuelve el negocio de la sesion, con el giro como objeto")
    void miNegocio() throws Exception {
        var d = dueno(TODOS, true);

        var r = graphql(d.token(), "{ miNegocio { id nombre giro { codigo nombre } estado publicado completitud "
                + "{ porcentaje faltantes } principal { esPrincipal nombre } atributos { dieta { codigo } } "
                + "esSimulado version } }");

        assertThat(r.has("errors")).isFalse();
        assertThat(r.at("/data/miNegocio/id").asText()).isEqualTo(d.negocioId().toString());
        assertThat(r.at("/data/miNegocio/giro/codigo").asText()).isEqualTo("RESTAURANTE");
        assertThat(r.at("/data/miNegocio/giro/nombre").asText()).isEqualTo("Restaurante");
        assertThat(r.at("/data/miNegocio/estado").asText()).isEqualTo("ACTIVO");
        assertThat(r.at("/data/miNegocio/completitud/porcentaje").asInt()).isEqualTo(40);
        assertThat(r.at("/data/miNegocio/completitud/faltantes").size()).isEqualTo(4);
        assertThat(r.at("/data/miNegocio/principal/esPrincipal").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("SG-02: una sesion no ve el negocio de otra: cada una trae el suyo")
    void aislamientoEntreSesiones() throws Exception {
        var a = dueno(TODOS, true);
        var b = dueno(TODOS, true);

        assertThat(graphql(a.token(), "{ miNegocio { id } }").at("/data/miNegocio/id").asText())
                .isEqualTo(a.negocioId().toString());
        assertThat(graphql(b.token(), "{ miNegocio { id } }").at("/data/miNegocio/id").asText())
                .isEqualTo(b.negocioId().toString());
    }

    // ---- mutaciones del panel ----

    @Test
    @DisplayName("G10 y G14 y G15: se edita el perfil, la ubicacion y el horario por GraphQL, con version")
    void mutaciones_perfilUbicacionYHorario() throws Exception {
        var d = dueno(TODOS, true);
        var sucursal = graphql(d.token(), "{ sucursales { id version } }").at("/data/sucursales/0");

        var perfil = graphql(d.token(), null, "mutation($i: ActualizarNegocioInput!) { actualizarNegocio(input: $i) "
                + "{ descripcion telefonoContacto version completitud { porcentaje } } }",
                Map.of("i", Map.of("version", 0, "descripcion", "Comida de la casa", "telefonoContacto", "222 555 0101")));
        var ubicacion = graphql(d.token(), null, "mutation($id: ID!, $i: ActualizarSucursalInput!) "
                + "{ actualizarSucursal(id: $id, input: $i) { calle latitud longitud version } }",
                Map.of("id", sucursal.get("id").asText(), "i", Map.of("version", 0, "calle", "5 de Mayo", "latitud",
                        19.0433, "longitud", -98.1982)));
        var horario = graphql(d.token(), null, "mutation($id: ID!, $s: [DiaDeHorarioInput!]!) "
                + "{ definirHorario(sucursalId: $id, version: 0, semana: $s) { semana { dia tramos { abre cierra } } "
                + "version } }", Map.of("id", sucursal.get("id").asText(), "s", java.util.List.of(Map.of("dia", 4,
                        "tramos", java.util.List.of(Map.of("abre", "08:00", "cierra", "17:00"))))));

        assertThat(perfil.has("errors")).as(perfil.toString()).isFalse();
        assertThat(perfil.at("/data/actualizarNegocio/telefonoContacto").asText()).isEqualTo("+522225550101");
        assertThat(perfil.at("/data/actualizarNegocio/version").asInt()).isEqualTo(1);
        assertThat(ubicacion.has("errors")).as(ubicacion.toString()).isFalse();
        assertThat(ubicacion.at("/data/actualizarSucursal/latitud").asDouble()).isEqualTo(19.0433);
        assertThat(horario.has("errors")).as(horario.toString()).isFalse();
        assertThat(horario.at("/data/definirHorario/semana/0/tramos/0/abre").asText()).isEqualTo("08:00");
    }

    @Test
    @DisplayName("NG-10: una version vieja da CONCURRENCIA; PU-01: publicar incompleto dice que falta")
    void errores_deNegocio_conSuCodigo() throws Exception {
        var d = dueno(TODOS, true);
        graphql(d.token(), null, "mutation($i: ActualizarNegocioInput!) { actualizarNegocio(input: $i) { version } }",
                Map.of("i", Map.of("version", 0, "nombre", "Primero")));

        var vieja = graphql(d.token(), null, "mutation($i: ActualizarNegocioInput!) "
                + "{ actualizarNegocio(input: $i) { version } }", Map.of("i", Map.of("version", 0, "nombre", "Segundo")));
        var incompleta = graphql(d.token(), "mutation { publicarNegocio(publicar: true) { publicado } }");

        assertThat(codigoDelPrimerError(vieja)).isEqualTo("CONCURRENCIA");
        assertThat(vieja.at("/errors/0/message").asText()).isEqualTo("Otra persona cambió esto mientras lo editabas. Vuelve a abrirlo.");
        assertThat(codigoDelPrimerError(incompleta)).isEqualTo("PUBLICACION_INCOMPLETA");
        assertThat(incompleta.at("/errors/0/extensions/faltantes").toString()).contains("UBICACION").contains("HORARIO");
    }

    @Test
    @DisplayName("G11, G25 y G26: subcategorias, atributos y disponibilidad por GraphQL")
    void mutaciones_categoriasAtributosYDisponibilidad() throws Exception {
        var d = dueno(TODOS, true);

        var subcategorias = graphql(d.token(), "mutation { definirSubcategorias(version: 0, subcategorias: "
                + "[\"GASTRONOMIA_TIPICA\", \"ARTESANIAS\"]) { subcategorias { codigo capacidad } version } }");
        var atributos = graphql(d.token(), null, "mutation($i: DefinirAtributosInput!) { definirAtributos(input: $i) "
                + "{ atributos { dieta { codigo nombre } duracionVisitaMin costoMedioMxn } } }",
                Map.of("i", Map.of("version", 1, "dieta", java.util.List.of("VEGANA"), "accesibilidad",
                        java.util.List.of(), "duracionVisitaMin", 90, "costoMedioMxn", 180)));
        var disponibilidad = graphql(d.token(), "mutation { definirDisponibilidad(nivel: LLENA, nota: \"Solo barra\") "
                + "{ nivel nota venceEn } }");

        assertThat(subcategorias.has("errors")).as(subcategorias.toString()).isFalse();
        assertThat(subcategorias.at("/data/definirSubcategorias/subcategorias/1/capacidad").asBoolean()).isTrue();
        assertThat(atributos.has("errors")).as(atributos.toString()).isFalse();
        assertThat(atributos.at("/data/definirAtributos/atributos/dieta/0/nombre").asText()).isEqualTo("Vegana");
        assertThat(disponibilidad.at("/data/definirDisponibilidad/nivel").asText()).isEqualTo("LLENA");
        assertThat(disponibilidad.at("/data/definirDisponibilidad/venceEn").asText()).endsWith("Z");
    }

    // ---- altas ----

    @Test
    @DisplayName("G8: misAltasDeNegocio se permite sin negocio activo y trae solo las de la cuenta")
    void misAltas_sinNegocioActivo() throws Exception {
        var d = dueno(TODOS, false);

        var r = graphql(d.token(), "{ misAltasDeNegocio { negocioId estado motivo } }");

        assertThat(r.has("errors")).isFalse();
        assertThat(r.at("/data/misAltasDeNegocio/0/negocioId").asText()).isEqualTo(d.negocioId().toString());
        assertThat(r.at("/data/misAltasDeNegocio/0/estado").asText()).isEqualTo("REGISTRADO");
        assertThat(codigoDelPrimerError(graphql(null, "{ misAltasDeNegocio { estado } }"))).isEqualTo("NO_AUTENTICADO");
    }

    // ---- superficie publica ----

    private Dueno publicado(String permisos) throws Exception {
        var d = dueno(permisos, true);
        var sucursal = panel.sucursales(new ContextoNegocio(d.negocioId(), "t"), mx.negocio360.compartido.seguridad
                .Autorizacion.de(Set.of("negocios.perfil.ver"))).get(0);
        var todo = mx.negocio360.compartido.seguridad.Autorizacion.de(Set.of(TODOS.split(" ")));
        var contexto = new ContextoNegocio(d.negocioId(), "t");
        panel.actualizarSucursal(contexto, todo, sucursal.id(), new ActualizarSucursal(0, null, null, null, null, null,
                null, null, null, new BigDecimal("19.04"), new BigDecimal("-98.19"), null));
        panel.definirHorario(contexto, todo, sucursal.id(), 0,
                java.util.List.of(new DiaHorarioComando(4, java.util.List.of(new TramoComando("08:00", "17:00")))));
        panel.definirAtributos(contexto, todo, new DefinirAtributos(0, java.util.List.of("VEGANA"), java.util.List.of(),
                null, null));
        panel.publicarNegocio(contexto, todo, true);
        return d;
    }

    @Test
    @DisplayName("T1 y T2: la superficie publica funciona SIN sesion y el giro sale como objeto")
    void publico_sinSesion() throws Exception {
        var d = publicado(TODOS);
        var nombre = panel.miNegocio(new ContextoNegocio(d.negocioId(), "t"), mx.negocio360.compartido.seguridad
                .Autorizacion.de(Set.of(TODOS.split(" ")))).nombre();

        var ficha = graphql(null, null, "query($id: ID!) { negocioPublico(id: $id) { id nombre giro { codigo nombre } "
                + "atributos { dieta { codigo } } calificacion { total promedio distribucion } simulado "
                + "horario { zonaHoraria semana { dia } } ubicacion { calle latitud } } }",
                Map.of("id", d.negocioId().toString()));
        var busqueda = graphql(null, null, "query($f: FiltroPublico) { negociosPublicos(filtro: $f, primeros: 500) "
                + "{ negocios { id nombre distanciaKm } pageInfo { hayMas cursorFinal } } }",
                Map.of("f", Map.of("texto", nombre.toLowerCase(), "dieta", java.util.List.of("VEGANA"))));

        assertThat(ficha.has("errors")).as(ficha.toString()).isFalse();
        assertThat(ficha.at("/data/negocioPublico/nombre").asText()).isEqualTo(nombre);
        assertThat(ficha.at("/data/negocioPublico/giro/nombre").asText()).isEqualTo("Restaurante");
        assertThat(ficha.at("/data/negocioPublico/calificacion/total").asInt()).isZero();
        assertThat(ficha.at("/data/negocioPublico/horario/zonaHoraria").asText()).isEqualTo("America/Mexico_City");
        assertThat(busqueda.has("errors")).as(busqueda.toString()).isFalse();
        assertThat(busqueda.at("/data/negociosPublicos/negocios/0/id").asText()).isEqualTo(d.negocioId().toString());
        assertThat(busqueda.at("/data/negociosPublicos/pageInfo/hayMas").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("PU-01: un negocio sin publicar responde nulo, igual que uno que no existe")
    void publico_noPublicado() throws Exception {
        var d = dueno(TODOS, true);

        var r = graphql(null, null, "query($id: ID!) { negocioPublico(id: $id) { id } }",
                Map.of("id", d.negocioId().toString()));

        assertThat(r.has("errors")).isFalse();
        assertThat(r.at("/data/negocioPublico").isNull()).isTrue();
    }

    @Test
    @DisplayName("PU-05: un texto de mas de 80 caracteres o un radio de mas de 25 km da CONSULTA_INVALIDA")
    void publico_topes() throws Exception {
        var texto = graphql(null, null, "query($f: FiltroPublico) { negociosPublicos(filtro: $f) { negocios { id } } }",
                Map.of("f", Map.of("texto", "x".repeat(81))));
        var radio = graphql(null, null, "query($f: FiltroPublico) { negociosPublicos(filtro: $f) { negocios { id } } }",
                Map.of("f", Map.of("cerca", Map.of("latitud", 19.04, "longitud", -98.19, "radioKm", 100))));

        assertThat(codigoDelPrimerError(texto)).isEqualTo("CONSULTA_INVALIDA");
        assertThat(codigoDelPrimerError(radio)).isEqualTo("CONSULTA_INVALIDA");
    }

    // ---- reseñas (T3, T4, G9, G22) ----

    @Test
    @DisplayName("T4, T3, G9 y G22: reseñar con el dispositivo (preparar y confirmar), verla, responderla y ver el resumen")
    void resenas_deIdaYVuelta() throws Exception {
        var d = publicado(TODOS);
        var dispositivo = UUID.randomUUID().toString();

        var preparar = graphql(null, dispositivo, "mutation($i: ComandoInput!) { prepararComando(input: $i) "
                + "{ token resumen expiraEn advertencias } }", Map.of("i", Map.of("publicarResena", Map.of("negocioId",
                d.negocioId().toString(), "calificacion", 5, "comentario", "Muy rico"))));
        assertThat(preparar.has("errors")).as(preparar.toString()).isFalse();
        var token = preparar.at("/data/prepararComando/token").asText();
        var confirmar = graphql(null, dispositivo, "mutation($t: String!) { confirmarComando(token: $t) "
                + "{ ok codigo mensajePlantilla recursoId } }", Map.of("t", token));
        var publicas = graphql(null, null, "query($id: ID!) { resenasDeNegocio(negocioId: $id) { resenas { id "
                + "calificacion comentario demostracion respuesta { texto } } pageInfo { hayMas } } }",
                Map.of("id", d.negocioId().toString()));
        var panelResenas = graphql(d.token(), "{ resenas { resenas { id calificacion estado } hayMas resumen "
                + "{ total promedio } } }");
        var respuesta = graphql(d.token(), null, "mutation($id: ID!) { responderResena(id: $id, texto: \"Gracias\") "
                + "{ respuesta { texto } } }", Map.of("id", confirmar.at("/data/confirmarComando/recursoId").asText()));

        assertThat(preparar.at("/data/prepararComando/token").asText()).hasSize(43);
        assertThat(confirmar.at("/data/confirmarComando/ok").asBoolean()).as(confirmar.toString()).isTrue();
        assertThat(confirmar.at("/data/confirmarComando/codigo").asText()).isEqualTo("OK");
        assertThat(publicas.at("/data/resenasDeNegocio/resenas/0/comentario").asText()).isEqualTo("Muy rico");
        assertThat(publicas.at("/data/resenasDeNegocio/resenas/0/demostracion").asBoolean()).isTrue();
        assertThat(publicas.toString()).doesNotContain(dispositivo);
        assertThat(panelResenas.at("/data/resenas/resumen/total").asInt()).isEqualTo(1);
        assertThat(panelResenas.at("/data/resenas/resumen/promedio").asDouble()).isEqualTo(5.0);
        assertThat(respuesta.has("errors")).as(respuesta.toString()).isFalse();
        assertThat(respuesta.at("/data/responderResena/respuesta/texto").asText()).isEqualTo("Gracias");
        assertThat(graphql(null, null, "query($id: ID!) { resenasDeNegocio(negocioId: $id) { resenas { respuesta "
                + "{ texto } } } }", Map.of("id", d.negocioId().toString()))
                .at("/data/resenasDeNegocio/resenas/0/respuesta/texto").asText()).isEqualTo("Gracias");
    }

    @Test
    @DisplayName("RS-11: sin X-Dispositivo-Id no se puede preparar una reseña; con un token ajeno, tampoco confirmar")
    void resenas_dispositivo() throws Exception {
        var d = publicado(TODOS);
        var entrada = Map.<String, Object>of("i", Map.of("publicarResena", Map.of("negocioId", d.negocioId().toString(),
                "calificacion", 4)));
        var consulta = "mutation($i: ComandoInput!) { prepararComando(input: $i) { token } }";

        var sinDispositivo = graphql(null, null, consulta, entrada);
        var uno = UUID.randomUUID().toString();
        var token = graphql(null, uno, consulta, entrada).at("/data/prepararComando/token").asText();
        var otro = graphql(null, UUID.randomUUID().toString(), "mutation($t: String!) { confirmarComando(token: $t) "
                + "{ ok codigo } }", Map.of("t", token));

        assertThat(codigoDelPrimerError(sinDispositivo)).isEqualTo("DISPOSITIVO_INVALIDO");
        assertThat(otro.at("/data/confirmarComando/ok").asBoolean()).isFalse();
        assertThat(otro.at("/data/confirmarComando/codigo").asText()).isEqualTo("TOKEN_INVALIDO");
    }

    // ---- limites y cierre de la superficie ----

    @Test
    @DisplayName("SG-12: la introspeccion esta apagada")
    void introspeccionApagada() throws Exception {
        var r = graphql(null, "{ __schema { types { name } } }");

        assertThat(r.at("/data/__schema").isMissingNode() || r.at("/data/__schema").isNull()).isTrue();
        assertThat(r.has("errors")).isTrue();
    }

    @Test
    @DisplayName("SG-06 y SG-08: una consulta con demasiada profundidad o demasiados alias se rechaza")
    void limitesDeConsulta() throws Exception {
        var alias = new StringBuilder("{ ");
        for (var i = 0; i < 12; i++) {
            alias.append("a").append(i).append(": negocioPublico(id: \"").append(UUID.randomUUID()).append("\") { id } ");
        }
        alias.append("}");

        var r = graphql(null, alias.toString());

        // Doce campos de la raiz con alias pasan el tope de campos de la raiz (5) antes que el de alias.
        assertThat(r.at("/errors/0/message").asText()).isEqualTo("DEMASIADOS_CAMPOS_RAIZ");

        var anidados = new StringBuilder("{ negocioPublico(id: \"" + UUID.randomUUID() + "\") { ");
        for (var i = 0; i < 12; i++) {
            anidados.append("a").append(i).append(": id ");
        }
        anidados.append("} }");

        assertThat(graphql(null, anidados.toString()).at("/errors/0/message").asText()).isEqualTo("DEMASIADOS_ALIAS");
    }

    @Test
    @DisplayName("SG-13: un error interno no muestra clases, trazas ni mensajes de la base")
    void erroresInternos_enmascarados() throws Exception {
        var d = dueno(TODOS, true);

        var r = graphql(d.token(), null, "query($id: ID!) { horarioDeSucursal(sucursalId: $id) { version } }",
                Map.of("id", "no-es-un-uuid"));

        assertThat(r.toString()).doesNotContain("Exception").doesNotContain("java.").doesNotContain("SQL")
                .doesNotContain("negocios.").doesNotContain("at mx");
        assertThat(r.has("errors")).isTrue();
    }

    @Test
    @DisplayName("SG-14: GET a /graphql es 405; un cuerpo enorme, 413")
    void httpEstricto() throws Exception {
        mvc.perform(get("/graphql").queryParam("query", "{ miNegocio { id } }")).andExpect(status().isMethodNotAllowed());
        // El tope del cuerpo es de 64 KB (seguridad.http.cuerpo-maximo-bytes): uno de 70 000 bytes es 413 sin leerlo.
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content("{\"query\":\""
                + "x".repeat(70_000) + "\"}")).andExpect(status().isPayloadTooLarge());
        // Uno que cabe en el cuerpo pero tiene una consulta de mas de 10 000 caracteres tambien se rechaza (400).
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content("{\"query\":\""
                + "x".repeat(40_000) + "\"}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("W1: altaDeNegocio por SSE en el mismo /graphql avisa el rechazo a la cuenta de la sesion y se cierra sola")
    void suscripcion_porSse() throws Exception {
        var d = dueno(TODOS, false);
        var inicio = mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM).header("Authorization", "Bearer " + d.token())
                .content("{\"query\":\"subscription { altaDeNegocio { negocioId estado motivo } }\"}"))
                .andExpect(request().asyncStarted()).andReturn();
        var ahora = java.time.Instant.parse("2026-10-16T16:00:00Z");
        var negocio = UuidV7.generar(ahora);

        avisos.alRecibir(new mx.negocio360.compartido.eventos.SobreEvento(UuidV7.generar(ahora),
                "negocio.registro_rechazado", 1, negocio, "negocios", ahora, UuidV7.generar(ahora), null,
                Map.of("solicitado_por", d.cuentaId().toString(), "nombre", "Farmacia", "giro", "FARMACIA",
                        "motivo", "GIRO_INVALIDO")));
        var cuerpo = mvc.perform(asyncDispatch(inicio)).andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).contains(negocio.toString()).contains("RECHAZADO").contains("GIRO_INVALIDO");
        assertThat(avisos.suscritas()).isZero();
    }
}
