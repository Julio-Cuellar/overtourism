package mx.negocio360.contabilidad.aplicacion;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.EstadoCuenta;
import mx.negocio360.contabilidad.dominio.Naturaleza;
import mx.negocio360.contabilidad.dominio.TipoCuenta;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** C-03: una cuenta con movimientos no cambia naturaleza, tipo ni padre, y no se borra: se desactiva. */
class EditarCuentaTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private void venta() {
        e.confirmar(e.ctxA, new RegistrarPolizaManualComando(TipoPoliza.INGRESO, Fixtures.HOY, "Venta", List.of(
                new RenglonManual(e.cuenta(e.ctxA, "101.01").id(), Fixtures.m("10.00"), null, null),
                new RenglonManual(e.cuenta(e.ctxA, "401.01").id(), null, Fixtures.m("10.00"), null))));
    }

    private UUID raizSinMovimientos() {
        return e.confirmar(e.ctxA, new CrearCuentaComando("301", "Capital", null, TipoCuenta.CAPITAL,
                Naturaleza.ACREEDORA, true, null)).recursoId();
    }

    private static EditarCuentaComando editar(UUID id, long version, String nombre, TipoCuenta tipo,
                                              Naturaleza naturaleza, UUID padreId) {
        return new EditarCuentaComando(id, version, nombre, null, tipo, naturaleza, padreId);
    }

    @Test
    @DisplayName("C-03: cambiar la naturaleza de Caja tras una venta falla con NATURALEZA_CON_MOVIMIENTOS")
    void editar_naturalezaConMovimientos_falla() {
        venta();
        var caja = e.cuenta(e.ctxA, "101.01");

        fallaCon(() -> e.api.preparar(e.ctxA, editar(caja.id(), caja.version(), null, null, Naturaleza.ACREEDORA,
                null)), CodigoError.NATURALEZA_CON_MOVIMIENTOS);
    }

    @Test
    @DisplayName("C-03: cambiar el tipo o el padre de una cuenta con movimientos tambien falla")
    void editar_tipoOPadreConMovimientos_falla() {
        venta();
        var caja = e.cuenta(e.ctxA, "101.01");

        fallaCon(() -> e.api.preparar(e.ctxA, editar(caja.id(), caja.version(), null, TipoCuenta.PASIVO, null,
                null)), CodigoError.NATURALEZA_CON_MOVIMIENTOS);
        fallaCon(() -> e.api.preparar(e.ctxA, editar(caja.id(), caja.version(), null, null, null,
                e.cuenta(e.ctxA, "102").id())), CodigoError.NATURALEZA_CON_MOVIMIENTOS);
    }

    @Test
    @DisplayName("C-03: el nombre de una cuenta con movimientos si se puede cambiar")
    void editar_nombreConMovimientos_seAcepta() {
        venta();
        var caja = e.cuenta(e.ctxA, "101.01");

        e.confirmar(e.ctxA, editar(caja.id(), caja.version(), "Caja principal", null, null, null));

        assertThat(e.cuenta(e.ctxA, "101.01").nombre()).isEqualTo("Caja principal");
        assertThat(e.cuenta(e.ctxA, "101.01").version()).isEqualTo(caja.version() + 1);
    }

    @Test
    @DisplayName("C-03: una cuenta raiz sin movimientos si puede cambiar de naturaleza")
    void editar_raizSinMovimientos_cambiaNaturaleza() {
        var id = raizSinMovimientos();
        var cuenta = e.cuenta(e.ctxA, "301");

        e.confirmar(e.ctxA, editar(id, cuenta.version(), null, null, Naturaleza.DEUDORA, null));

        assertThat(e.cuenta(e.ctxA, "301").naturaleza()).isEqualTo(Naturaleza.DEUDORA);
    }

    @Test
    @DisplayName("C-03 y C-02: una hija no puede tomar una naturaleza distinta a la de su padre")
    void editar_hijaContraElPadre_falla() {
        var renta = e.confirmar(e.ctxA, new CrearCuentaComando("601.02", "Renta", e.cuenta(e.ctxA, "601").id(), null,
                null, true, null)).recursoId();

        fallaCon(() -> e.api.preparar(e.ctxA, editar(renta, 0, null, null, Naturaleza.ACREEDORA, null)),
                CodigoError.NATURALEZA_INVALIDA);
    }

    @Test
    @DisplayName("C-03: una cuenta no se borra; se desactiva y deja de recibir movimientos")
    void desactivar_cuentaConMovimientos_quedaInactiva() {
        var id = raizSinMovimientos();
        var cuenta = e.cuenta(e.ctxA, "301");

        e.confirmar(e.ctxA, new DesactivarCuentaComando(id, cuenta.version()));

        assertThat(e.cuenta(e.ctxA, "301").estado()).isEqualTo(EstadoCuenta.INACTIVA);
        assertThat(e.api.catalogo(e.ctxA)).anyMatch(c -> c.id().equals(id));
    }

    @Test
    @DisplayName("C-03: editar con una version vieja falla con CONCURRENCIA")
    void editar_versionVieja_falla() {
        var caja = e.cuenta(e.ctxA, "101.01");
        e.confirmar(e.ctxA, editar(caja.id(), caja.version(), "Caja 1", null, null, null));

        fallaCon(() -> e.api.preparar(e.ctxA, editar(caja.id(), caja.version(), "Caja 2", null, null, null)),
                CodigoError.CONCURRENCIA);
    }

    @Test
    @DisplayName("C-03: una cuenta de otro negocio falla con CUENTA_NO_ENCONTRADA")
    void editar_cuentaAjena_falla() {
        e.activar(e.ctxB);
        var cajaDeB = e.cuenta(e.ctxB, "101.01");

        fallaCon(() -> e.api.preparar(e.ctxA, editar(cajaDeB.id(), 0, "x", null, null, null)),
                CodigoError.CUENTA_NO_ENCONTRADA);
    }
}
