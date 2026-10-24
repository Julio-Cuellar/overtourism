package mx.negocio360.persistencia;

/** {@code ventas.preparacion} cumple el contrato de las preparaciones. */
class PreparacionesDeVentasPostgresTest extends PreparacionesPostgresTest {

    @Override
    protected String esquema() {
        return "ventas";
    }
}
