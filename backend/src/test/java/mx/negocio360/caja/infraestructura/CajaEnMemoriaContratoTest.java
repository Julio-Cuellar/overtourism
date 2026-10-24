package mx.negocio360.caja.infraestructura;

import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;

/** El almacen de Caja en memoria cumple el contrato de {@link RepositorioCaja} y {@link LibroDeCaja}. */
class CajaEnMemoriaContratoTest extends ContratoAlmacenDeCajaTest {

    private final CajaEnMemoria almacen = new CajaEnMemoria();

    @Override
    protected RepositorioCaja cajas() {
        return almacen;
    }

    @Override
    protected LibroDeCaja libro() {
        return almacen;
    }
}
