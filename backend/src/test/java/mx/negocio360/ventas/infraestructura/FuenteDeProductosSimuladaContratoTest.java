package mx.negocio360.ventas.infraestructura;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import mx.negocio360.compartido.catalogo.ContratoDeFuenteDeProductosTest;
import mx.negocio360.compartido.catalogo.FuenteDeProductos;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;

/**
 * La fuente simulada de desarrollo cumple el contrato de cualquier {@link FuenteDeProductos}. No aisla por negocio (son
 * los mismos seis productos para todos) ni distingue la baja, asi que esas dos pruebas se omiten (HZ-28).
 */
class FuenteDeProductosSimuladaContratoTest extends ContratoDeFuenteDeProductosTest {

    private final FuenteDeProductosSimulada fuente = new FuenteDeProductosSimulada();
    private final UUID negocio = UUID.randomUUID();

    @Override
    protected FuenteDeProductos fuente() {
        return fuente;
    }

    @Override
    protected UUID negocioConProductos() {
        return negocio;
    }

    @Override
    protected Set<UUID> idsVendibles() {
        return fuente.buscar(negocio, null, 50).stream().map(ProductoParaVenta::productoId).collect(Collectors.toSet());
    }
}
