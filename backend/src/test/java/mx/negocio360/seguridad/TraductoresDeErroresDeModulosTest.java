package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import mx.negocio360.clientes.aplicacion.PlantillasClientes;
import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.graphql.TraductorDeErroresDeClientes;
import mx.negocio360.inventario.aplicacion.PlantillasInventario;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.graphql.TraductorDeErroresDeInventario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

/**
 * SG-13: cada modulo dice como se traducen sus errores de negocio, con el codigo y el texto fijo que ya tiene. El
 * mensaje de la excepcion no se usa nunca; si lleva el id del recurso involucrado, sale como extension.
 */
class TraductoresDeErroresDeModulosTest {

    private final TraductorDeErroresDeClientes clientes = new TraductorDeErroresDeClientes();
    private final TraductorDeErroresDeInventario inventario = new TraductorDeErroresDeInventario();

    @Test
    @DisplayName("Clientes: cada codigo sale con su texto fijo y como error de peticion")
    void clientes_cadaCodigoTieneSuTextoFijo() {
        for (var codigo : mx.negocio360.clientes.dominio.CodigoError.values()) {
            var traducido = clientes.traducir(new ClientesException(codigo)).orElseThrow();

            assertThat(traducido.codigo()).isEqualTo(codigo.name());
            assertThat(traducido.mensaje()).isEqualTo(PlantillasClientes.de(codigo)).isNotBlank();
        }
    }

    @Test
    @DisplayName("Clientes: el id del cliente que ya tenia ese telefono sale como extension, sin el telefono")
    void clientes_telefonoDuplicado_llevaElIdDelRecurso() {
        var existente = UUID.randomUUID();

        var traducido = clientes.traducir(
                new ClientesException(mx.negocio360.clientes.dominio.CodigoError.TELEFONO_DUPLICADO, existente))
                .orElseThrow();

        assertThat(traducido.extensiones()).containsEntry("recursoId", existente.toString());
        assertThat(traducido.mensaje()).doesNotContain(existente.toString());
    }

    @Test
    @DisplayName("Clientes: CONCURRENCIA y ERROR_TEMPORAL se marcan como reintentables; los demas no")
    void clientes_reintentables() {
        for (var codigo : mx.negocio360.clientes.dominio.CodigoError.values()) {
            var traducido = clientes.traducir(new ClientesException(codigo)).orElseThrow();
            var esperado = codigo.name().equals("CONCURRENCIA") || codigo.name().equals("ERROR_TEMPORAL");

            if (esperado) {
                assertThat(traducido.extensiones()).containsEntry("reintentable", true);
            } else {
                assertThat(traducido.extensiones()).doesNotContainKey("reintentable");
            }
        }
    }

    @Test
    @DisplayName("Inventario: cada codigo sale con su texto fijo")
    void inventario_cadaCodigoTieneSuTextoFijo() {
        for (var codigo : mx.negocio360.inventario.dominio.CodigoError.values()) {
            var traducido = inventario.traducir(new InventarioException(codigo)).orElseThrow();

            assertThat(traducido.codigo()).isEqualTo(codigo.name());
            assertThat(traducido.mensaje()).isEqualTo(PlantillasInventario.de(codigo)).isNotBlank();
        }
    }

    @Test
    @DisplayName("Inventario: el id del recurso sale como extension y CONCURRENCIA es reintentable")
    void inventario_recursoYReintentable() {
        var existente = UUID.randomUUID();

        var duplicado = inventario.traducir(
                new InventarioException(mx.negocio360.inventario.dominio.CodigoError.NOMBRE_DUPLICADO, existente))
                .orElseThrow();
        var concurrencia = inventario.traducir(
                new InventarioException(mx.negocio360.inventario.dominio.CodigoError.CONCURRENCIA)).orElseThrow();

        assertThat(duplicado.extensiones()).containsEntry("recursoId", existente.toString());
        assertThat(concurrencia.extensiones()).containsEntry("reintentable", true);
    }

    @Test
    @DisplayName("un traductor solo reconoce las excepciones de su modulo")
    void cadaTraductor_soloReconocesuModulo() {
        assertThat(clientes.traducir(new RuntimeException("x"))).isEmpty();
        assertThat(clientes.traducir(
                new InventarioException(mx.negocio360.inventario.dominio.CodigoError.CONCURRENCIA))).isEmpty();
        assertThat(inventario.traducir(
                new ClientesException(mx.negocio360.clientes.dominio.CodigoError.CONCURRENCIA))).isEmpty();
        assertThat(inventario.traducir(new IllegalStateException())).isEmpty();
    }

    @Test
    @DisplayName("el error de negocio de un modulo es BAD_REQUEST, no un error interno")
    void errorDeNegocio_esBadRequest() {
        assertThat(clientes.traducir(
                new ClientesException(mx.negocio360.clientes.dominio.CodigoError.NOMBRE_INVALIDO)).orElseThrow()
                .clasificacion()).isEqualTo(ErrorType.BAD_REQUEST);
    }
}
