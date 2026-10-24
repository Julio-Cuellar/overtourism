package mx.negocio360.ventas.infraestructura;

import mx.negocio360.ventas.aplicacion.puertos.RepositorioVentas;

/** El almacen de Ventas en memoria cumple el contrato de {@link RepositorioVentas}. */
class VentasEnMemoriaContratoTest extends ContratoRepositorioVentasTest {

    private final RepositorioVentas almacen = new VentasEnMemoria();

    @Override
    protected RepositorioVentas almacen() {
        return almacen;
    }
}
