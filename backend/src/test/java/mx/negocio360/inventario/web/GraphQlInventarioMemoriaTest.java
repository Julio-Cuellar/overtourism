package mx.negocio360.inventario.web;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/** La bateria de Inventario de punta a punta con el almacen en memoria (desarrollo y demo). */
@SpringBootTest(properties = "inventario.almacen=memoria")
@AutoConfigureMockMvc
class GraphQlInventarioMemoriaTest extends GraphQlInventarioContratoTest {
}
