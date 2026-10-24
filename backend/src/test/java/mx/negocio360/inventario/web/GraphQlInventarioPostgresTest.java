package mx.negocio360.inventario.web;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * La misma bateria de Inventario de punta a punta, ahora con PostgreSQL (F6): repositorios JDBC con seguridad por fila y
 * una transaccion por llamada a la API.
 */
@SpringBootTest(properties = "inventario.almacen=postgres")
@AutoConfigureMockMvc
class GraphQlInventarioPostgresTest extends GraphQlInventarioContratoTest {
}
