package mx.negocio360.clientes.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.ClienteListado;
import mx.negocio360.clientes.aplicacion.EditarClienteComando;
import mx.negocio360.clientes.aplicacion.PaginaClientes;
import mx.negocio360.clientes.aplicacion.Preparacion;
import mx.negocio360.clientes.aplicacion.PreparacionCreada;
import mx.negocio360.clientes.aplicacion.RegistrarClienteComando;
import mx.negocio360.clientes.eventos.OrigenCliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * P-06 y SC-05: el toString que Java genera para un record vuelca todos sus campos. Ningun record con datos
 * personales ni con el token puede imprimirlos, porque un registro, un mensaje de error o un depurador los
 * copiaria. Los valores sembrados no pueden aparecer en un UUID.
 */
class ToStringSinDatosPersonalesTest {

    private static final Instant AHORA = Instant.parse("2026-10-14T16:00:00Z");
    private static final DatosCliente DATOS = new DatosCliente("Zoila", "Quiroz", "222 123 4567",
            "zoila@correo.test", LocalDate.of(1987, 3, 30), "nota secreta");

    private static void sinDatos(String texto) {
        assertThat(texto).doesNotContain("Zoila").doesNotContain("Quiroz").doesNotContain("2221234567")
                .doesNotContain("zoila@").doesNotContain("1987-03-30").doesNotContain("nota secreta")
                .doesNotContain("TOKEN-SECRETO");
    }

    @Test
    @DisplayName("DatosCliente no imprime sus campos")
    void datosCliente_toString_noImprimeDatos() {
        sinDatos(DATOS.toString());
    }

    @Test
    @DisplayName("Cliente no imprime sus datos pero si su id, estado y version")
    void cliente_toString_noImprimeDatos() {
        var id = UUID.randomUUID();
        var cliente = Cliente.nuevo(id, UUID.randomUUID(), DATOS, OrigenCliente.PANEL, 1, "locatario", AHORA);

        var texto = cliente.toString();

        sinDatos(texto);
        assertThat(texto).contains(id.toString()).contains("ACTIVO");
    }

    @Test
    @DisplayName("los comandos de registrar y editar no imprimen datos personales")
    void comandos_toString_noImprimenDatos() {
        var registrar = new RegistrarClienteComando("Zoila", "Quiroz", "222 123 4567", "zoila@correo.test",
                LocalDate.of(1987, 3, 30), "nota secreta", 1, List.of());
        var editar = new EditarClienteComando(UUID.randomUUID(), 1, "Zoila", "Quiroz", "222 123 4567",
                "zoila@correo.test", LocalDate.of(1987, 3, 30), "nota secreta");

        sinDatos(registrar.toString());
        sinDatos(editar.toString());
    }

    @Test
    @DisplayName("Preparacion no imprime el comando ni el resumen, solo si estan presentes")
    void preparacion_toString_noImprimeComandoNiResumen() {
        var fila = new Preparacion("hash", UUID.randomUUID(), "locatario", "registrar_cliente", "hashComando",
                "{\"nombre\":\"Zoila\",\"telefono\":\"222 123 4567\"}", "Registrar a Zoila Quiroz", AHORA, null);

        var texto = fila.toString();

        sinDatos(texto);
        assertThat(texto).contains("registrar_cliente");
        assertThat(fila.vaciada().toString()).contains("vacio");
    }

    @Test
    @DisplayName("PreparacionCreada no imprime el token ni el resumen")
    void preparacionCreada_toString_noImprimeElToken() {
        var creada = new PreparacionCreada("TOKEN-SECRETO-1234", "Registrar a Zoila Quiroz", AHORA, List.of());

        sinDatos(creada.toString());
        assertThat(creada.toString()).contains(AHORA.toString());
    }

    @Test
    @DisplayName("ClienteListado y PaginaClientes no imprimen el nombre")
    void listados_toString_noImprimenElNombre() {
        var listado = new ClienteListado(UUID.randomUUID(), "Zoila Quiroz", "•••• 4567");
        var pagina = new PaginaClientes(List.of(listado), true, listado.id().toString());

        sinDatos(listado.toString());
        sinDatos(pagina.toString());
        assertThat(pagina.toString()).contains("hayMas=true");
    }
}
