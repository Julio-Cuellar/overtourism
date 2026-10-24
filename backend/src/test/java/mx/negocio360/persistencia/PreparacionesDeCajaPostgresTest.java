package mx.negocio360.persistencia;

/** {@code caja.preparacion} cumple el contrato de las preparaciones. */
class PreparacionesDeCajaPostgresTest extends PreparacionesPostgresTest {

    @Override
    protected String esquema() {
        return "caja";
    }
}
