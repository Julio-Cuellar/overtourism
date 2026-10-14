package mx.negocio360.compartido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import mx.negocio360.compartido.eventos.MapeadorEventos;
import mx.negocio360.compartido.eventos.SobreEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** R3 y R5: todo evento lleva el sobre comun con UUIDv7 y nombres de campo estables. */
class SobreEventoTest {

    private static final UUID ID_V7 = UUID.fromString("0192f3a0-7c1e-7b52-9d41-5a1c3e8f2b10");
    private static final UUID ID_V4 = UUID.fromString("5f8c1f3e-7c1e-4b52-9d41-5a1c3e8f2b10");
    private static final UUID NEGOCIO = UUID.fromString("0192f39f-1111-7000-8000-00000000a001");
    private static final UUID CORRELACION = UUID.fromString("0192f3a0-6a00-7000-8000-00000000b002");
    private static final UUID CAUSA = UUID.fromString("0192f3a0-6b00-7000-8000-00000000c003");
    private static final Instant OCURRIDO = Instant.parse("2026-10-08T18:22:41Z");

    private final ObjectMapper mapper = MapeadorEventos.crear();

    private static SobreEvento sobre(UUID id, String tipo, int version, UUID negocio, UUID causa) {
        return new SobreEvento(id, tipo, version, negocio, "clientes", OCURRIDO, CORRELACION, causa,
                Map.of("cliente_id", "0192f3a0-1c00-7000-8000-00000000d004"));
    }

    @Test
    @DisplayName("un sobre con todos los campos validos se construye")
    void constructor_camposValidos_construyeElSobre() {
        var sobre = sobre(ID_V7, "cliente.registrado", 1, NEGOCIO, null);

        assertThat(sobre.id()).isEqualTo(ID_V7);
        assertThat(sobre.negocioId()).isEqualTo(NEGOCIO);
        assertThat(sobre.causaId()).isNull();
    }

    @Test
    @DisplayName("rechaza un id que no es UUIDv7")
    void constructor_idQueNoEsV7_lanzaIllegalArgument() {
        assertThatThrownBy(() -> sobre(ID_V4, "cliente.registrado", 1, NEGOCIO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUIDv7");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sinpunto", "Cliente.registrado", "cliente.Registrado", "cliente.registrado.extra"})
    @DisplayName("rechaza un tipo que no sea modulo.hecho_en_pasado")
    void constructor_tipoInvalido_lanzaIllegalArgument(String tipo) {
        assertThatThrownBy(() -> sobre(ID_V7, tipo, 1, NEGOCIO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rechaza una version menor a uno")
    void constructor_versionCero_lanzaIllegalArgument() {
        assertThatThrownBy(() -> sobre(ID_V7, "cliente.registrado", 0, NEGOCIO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el negocio es obligatorio")
    void constructor_negocioNulo_lanzaNullPointer() {
        assertThatNullPointerException().isThrownBy(() -> sobre(ID_V7, "cliente.registrado", 1, null, null));
    }

    @Test
    @DisplayName("los datos del sobre no se pueden modificar")
    void datos_sobreCreado_esInmutable() {
        var sobre = sobre(ID_V7, "cliente.registrado", 1, NEGOCIO, null);

        assertThatThrownBy(() -> sobre.datos().put("nombre", "Ana"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("el JSON usa los nueve nombres del contrato en snake_case")
    void serializar_sobre_usaLosNombresDelContrato() throws Exception {
        JsonNode json = mapper.valueToTree(sobre(ID_V7, "cliente.registrado", 1, NEGOCIO, CAUSA));

        var nombres = new HashSet<String>();
        json.fieldNames().forEachRemaining(nombres::add);
        assertThat(nombres).containsExactlyInAnyOrder("id", "tipo", "version", "negocio_id", "origen",
                "ocurrido_en", "correlacion_id", "causa_id", "datos");
    }

    @Test
    @DisplayName("ocurrido_en sale en ISO-8601 UTC y causa_id nulo sigue presente")
    void serializar_sobre_fechaIsoUtcYCausaNulaExplicita() throws Exception {
        JsonNode json = mapper.valueToTree(sobre(ID_V7, "cliente.registrado", 1, NEGOCIO, null));

        assertThat(json.get("ocurrido_en").asText()).isEqualTo("2026-10-08T18:22:41Z");
        assertThat(json.has("causa_id")).isTrue();
        assertThat(json.get("causa_id").isNull()).isTrue();
    }

    @Test
    @DisplayName("ida y vuelta por JSON conserva el sobre")
    void roundTrip_sobre_seConserva() throws Exception {
        var original = sobre(ID_V7, "cliente.consentimiento_cambiado", 1, NEGOCIO, CAUSA);

        var leido = mapper.readValue(mapper.writeValueAsString(original), SobreEvento.class);

        assertThat(leido).isEqualTo(original);
    }

    @Test
    @DisplayName("el origen no puede estar vacio")
    void constructor_origenVacio_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new SobreEvento(ID_V7, "cliente.registrado", 1, NEGOCIO, " ", OCURRIDO,
                CORRELACION, null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rechaza un UUID cuya variante no es la de RFC 4122")
    void constructor_varianteDistinta_lanzaIllegalArgument() {
        var variante0 = UUID.fromString("0192f3a0-7c1e-7b52-0d41-5a1c3e8f2b10");

        assertThatThrownBy(() -> sobre(variante0, "cliente.registrado", 1, NEGOCIO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Map<String, Object>> datosConNumerosFlotantes() {
        return Stream.of(
                Map.of("total", 207.1d),
                Map.of("total", 207.1f),
                Map.of("total", new BigDecimal("207.10")),
                Map.of("lineas", List.of(Map.of("precio", 1.5d))),
                Map.of("anidado", Map.of("total", new BigDecimal("1"))));
    }

    @ParameterizedTest
    @MethodSource("datosConNumerosFlotantes")
    @DisplayName("el dinero no viaja como numero dentro de los datos, ni anidado")
    void constructor_datosConFlotantes_lanzaIllegalArgument(Map<String, Object> datos) {
        assertThatThrownBy(() -> new SobreEvento(ID_V7, "venta.registrada", 1, NEGOCIO, "ventas", OCURRIDO,
                CORRELACION, null, datos)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("los datos solo admiten cadenas, enteros, booleanos, listas y mapas")
    void constructor_datosConTipoDesconocido_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new SobreEvento(ID_V7, "cliente.registrado", 1, NEGOCIO, "clientes", OCURRIDO,
                CORRELACION, null, Map.of("x", new Object()))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("las listas y mapas anidados de los datos tambien son inmutables y se copian")
    void datos_anidados_seCopianYSonInmutables() {
        var original = new ArrayList<>(List.of("a"));
        var sobre = new SobreEvento(ID_V7, "venta.registrada", 1, NEGOCIO, "ventas", OCURRIDO, CORRELACION, null,
                Map.of("lista", original, "cantidad", 2, "activo", true, "texto", "x"));

        original.add("b");

        @SuppressWarnings("unchecked")
        var copia = (List<Object>) sobre.datos().get("lista");
        assertThat(copia).containsExactly("a");
        assertThatThrownBy(() -> copia.add("c")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("el dinero se escribe como cadena decimal plana, nunca como numero")
    void dinero_serializar_escribeCadenaDecimalPlana() throws Exception {
        assertThat(mapper.writeValueAsString(new BigDecimal("1E+2"))).isEqualTo("\"100\"");
        assertThat(mapper.writeValueAsString(Map.of("total", new BigDecimal("207.00"))))
                .isEqualTo("{\"total\":\"207.00\"}");
    }
}
