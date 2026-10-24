package mx.negocio360.ventas.web;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * La misma bateria de Ventas y Caja de punta a punta, ahora con PostgreSQL (F6): repositorios JDBC con seguridad por fila,
 * una transaccion por llamada a la API y los eventos de Ventas hacia Caja por el outbox.
 */
@SpringBootTest(properties = {"ventas.almacen=postgres", "caja.almacen=postgres"})
@AutoConfigureMockMvc
@Import(GraphQlVentasCajaContratoTest.ProductosDePrueba.class)
class GraphQlVentasCajaPostgresTest extends GraphQlVentasCajaContratoTest {
}
