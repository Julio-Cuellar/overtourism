package mx.negocio360.inventario.infraestructura;

import mx.negocio360.inventario.aplicacion.puertos.RepositorioPreparaciones;

/** Las preparaciones de Inventario en memoria cumplen el contrato. */
class PreparacionesDeInventarioEnMemoriaContratoTest extends ContratoPreparacionesDeInventarioTest {

    private final RepositorioPreparaciones almacen = new PreparacionesEnMemoria();

    @Override
    protected RepositorioPreparaciones almacen() {
        return almacen;
    }
}
