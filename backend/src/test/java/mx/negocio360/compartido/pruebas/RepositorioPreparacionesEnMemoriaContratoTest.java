package mx.negocio360.compartido.pruebas;

import mx.negocio360.compartido.comandos.ContratoRepositorioPreparacionesTest;
import mx.negocio360.compartido.comandos.RepositorioPreparaciones;

/** El doble que usan las pruebas de servicio cumple el mismo contrato que los almacenes reales. */
class RepositorioPreparacionesEnMemoriaContratoTest extends ContratoRepositorioPreparacionesTest {

    private final RepositorioPreparaciones almacen = new RepositorioPreparacionesEnMemoria();

    @Override
    protected RepositorioPreparaciones almacen() {
        return almacen;
    }
}
