package mx.negocio360.compartido.comandos;

/** El almacen en memoria de produccion cumple el contrato de {@link RepositorioPreparaciones}. */
class PreparacionesEnMemoriaContratoTest extends ContratoRepositorioPreparacionesTest {

    private final RepositorioPreparaciones almacen = new PreparacionesEnMemoria();

    @Override
    protected RepositorioPreparaciones almacen() {
        return almacen;
    }
}
