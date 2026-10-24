package mx.negocio360.inventario.infraestructura;

import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioConsumos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioMovimientos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;

/** El almacen de Inventario en memoria cumple el contrato de sus tres repositorios. */
class InventarioEnMemoriaContratoTest extends ContratoAlmacenDeInventarioTest {

    private final InventarioEnMemoria almacen = new InventarioEnMemoria();

    @Override
    protected RepositorioArticulos articulos() {
        return almacen;
    }

    @Override
    protected RepositorioSaldos saldos() {
        return almacen;
    }

    @Override
    protected RepositorioMovimientos movimientos() {
        return almacen;
    }

    @Override
    protected RepositorioConsumos consumos() {
        return almacen;
    }
}
