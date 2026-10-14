package mx.negocio360.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.clientes.contratos.VentaAnuladaV1;
import mx.negocio360.clientes.contratos.VentaRegistradaV1;
import mx.negocio360.compartido.eventos.MapeadorEventos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Lo que Clientes espera de Ventas (D-48, D-65). Es un lector tolerante: acepta campos nuevos (R5)
 * pero exige que el dinero sea una cadena decimal.
 */
class ContratoVentasConsumidoTest {

    private static final String VENTA = "0192f3a0-6a00-7000-8000-00000000b002";
    private static final String CLIENTE = "0192f3a0-1c00-7000-8000-00000000d004";

    private final ObjectMapper mapper = MapeadorEventos.crear();

    @Test
    @DisplayName("venta.registrada con cliente: lee ids y total decimal")
    void ventaRegistrada_conCliente_leeIdsYTotal() throws Exception {
        var json = """
                {"venta_id":"%s","cliente_id":"%s","total":"207.00","lineas":[{"producto_id":"x","cantidad":"2"}]}
                """.formatted(VENTA, CLIENTE);

        var venta = mapper.readValue(json, VentaRegistradaV1.class);

        assertThat(venta.ventaId()).isEqualTo(UUID.fromString(VENTA));
        assertThat(venta.cliente()).contains(UUID.fromString(CLIENTE));
        assertThat(venta.total()).isEqualTo(new BigDecimal("207.00"));
    }

    @Test
    @DisplayName("venta de mostrador: sin cliente_id deja el cliente vacio (H-05)")
    void ventaRegistrada_sinCliente_dejaElClienteVacio() throws Exception {
        var json = """
                {"venta_id":"%s","total":"50.00"}
                """.formatted(VENTA);

        var venta = mapper.readValue(json, VentaRegistradaV1.class);

        assertThat(venta.cliente()).isEmpty();
    }

    @Test
    @DisplayName("ignora campos que Ventas agregue en el futuro (R5)")
    void ventaRegistrada_conCamposNuevos_losIgnora() throws Exception {
        var json = """
                {"venta_id":"%s","cliente_id":"%s","total":"10.00","campo_nuevo":{"a":1},"otro":[1,2]}
                """.formatted(VENTA, CLIENTE);

        var venta = mapper.readValue(json, VentaRegistradaV1.class);

        assertThat(venta.total()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("rechaza un total que llegue como numero y no como cadena")
    void ventaRegistrada_totalNumerico_seRechaza() {
        var json = """
                {"venta_id":"%s","total":207.00}
                """.formatted(VENTA);

        assertThatThrownBy(() -> mapper.readValue(json, VentaRegistradaV1.class))
                .isInstanceOf(JsonMappingException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"doscientos", "1E+3", "+5", ".5", "5.", " 207.00 ", "1E+999999999",
            "12345678901234567890", "-1.00"})
    @DisplayName("rechaza un total que no sea decimal plano, no negativo y de largo razonable")
    void ventaRegistrada_totalInvalido_seRechaza(String total) {
        var json = """
                {"venta_id":"%s","total":"%s"}
                """.formatted(VENTA, total);

        assertThatThrownBy(() -> mapper.readValue(json, VentaRegistradaV1.class))
                .isInstanceOf(JsonMappingException.class);
    }

    @Test
    @DisplayName("un total en cero es valido (cupon del cien por ciento, D-65)")
    void ventaRegistrada_totalCero_esValido() throws Exception {
        var json = """
                {"venta_id":"%s","total":"0.00"}
                """.formatted(VENTA);

        assertThat(mapper.readValue(json, VentaRegistradaV1.class).total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("rechaza una venta sin venta_id")
    void ventaRegistrada_sinVentaId_seRechaza() {
        assertThatThrownBy(() -> mapper.readValue("{\"total\":\"10.00\"}", VentaRegistradaV1.class))
                .isInstanceOf(JsonMappingException.class);
    }

    @Test
    @DisplayName("rechaza una venta sin total")
    void ventaRegistrada_sinTotal_seRechaza() {
        var json = """
                {"venta_id":"%s"}
                """.formatted(VENTA);

        assertThatThrownBy(() -> mapper.readValue(json, VentaRegistradaV1.class))
                .isInstanceOf(JsonMappingException.class);
    }

    @Test
    @DisplayName("venta.anulada solo necesita el venta_id")
    void ventaAnulada_soloNecesitaElVentaId() throws Exception {
        var json = """
                {"venta_id":"%s","motivo":"error de captura","lineas":[]}
                """.formatted(VENTA);

        var anulada = mapper.readValue(json, VentaAnuladaV1.class);

        assertThat(anulada.ventaId()).isEqualTo(UUID.fromString(VENTA));
    }
}
