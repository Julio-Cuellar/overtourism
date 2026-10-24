package mx.negocio360.ventas.web;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** La bateria de Ventas y Caja de punta a punta con los almacenes en memoria (desarrollo y demo). */
@SpringBootTest(properties = {"ventas.almacen=memoria", "caja.almacen=memoria"})
@AutoConfigureMockMvc
@Import(GraphQlVentasCajaContratoTest.ProductosDePrueba.class)
class GraphQlVentasCajaMemoriaTest extends GraphQlVentasCajaContratoTest {
}
