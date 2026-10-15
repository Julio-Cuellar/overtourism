package mx.negocio360.contabilidad.aplicacion;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.EstadoCuenta;
import mx.negocio360.contabilidad.dominio.Naturaleza;
import mx.negocio360.contabilidad.dominio.TipoCuenta;
import mx.negocio360.contabilidad.fakes.Escenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** C-02: el codigo es unico por negocio; tipo y naturaleza de una hija son los de su padre. */
class CrearCuentaTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private Cuenta cuenta(String codigo) {
        return e.api.catalogo(e.ctxA).stream().filter(c -> c.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("C-02: una hija sin tipo ni naturaleza hereda las de su padre")
    void crear_hija_heredaTipoYNaturaleza() {
        var padre = cuenta("601");

        var id = e.confirmar(e.ctxA, new CrearCuentaComando("601.02", "Renta del local", padre.id(), null, null,
                true, null)).recursoId();

        var creada = cuenta("601.02");
        assertThat(creada.id()).isEqualTo(id);
        assertThat(creada.tipo()).isEqualTo(TipoCuenta.GASTO);
        assertThat(creada.naturaleza()).isEqualTo(Naturaleza.DEUDORA);
        assertThat(creada.padreId()).isEqualTo(padre.id());
        assertThat(creada.afectable()).isTrue();
        assertThat(creada.deSistema()).isFalse();
        assertThat(creada.estado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(creada.negocioId()).isEqualTo(e.negocioA);
    }

    @Test
    @DisplayName("C-02: repetir 101.01 falla con CODIGO_CUENTA_DUPLICADO")
    void crear_codigoRepetido_falla() {
        var padre = cuenta("101");

        fallaCon(() -> e.api.preparar(e.ctxA, new CrearCuentaComando("101.01", "Otra caja", padre.id(), null, null,
                true, null)), CodigoError.CODIGO_CUENTA_DUPLICADO);
    }

    @Test
    @DisplayName("C-02: el mismo codigo en otro negocio no choca")
    void crear_mismoCodigoEnOtroNegocio_seAcepta() {
        e.activar(e.ctxB);
        var padreB = e.api.catalogo(e.ctxB).stream().filter(c -> c.codigo().equals("601")).findFirst().orElseThrow();
        e.confirmar(e.ctxA, new CrearCuentaComando("601.02", "Renta", cuenta("601").id(), null, null, true, null));

        e.confirmar(e.ctxB, new CrearCuentaComando("601.02", "Renta", padreB.id(), null, null, true, null));

        assertThat(e.api.catalogo(e.ctxB)).anyMatch(c -> c.codigo().equals("601.02"));
    }

    @Test
    @DisplayName("C-02: una hija ACREEDORA bajo un padre DEUDOR falla con NATURALEZA_INVALIDA")
    void crear_naturalezaDistintaDelPadre_falla() {
        var padre = cuenta("101");

        fallaCon(() -> e.api.preparar(e.ctxA, new CrearCuentaComando("101.02", "Caja chica", padre.id(), null,
                Naturaleza.ACREEDORA, true, null)), CodigoError.NATURALEZA_INVALIDA);
    }

    @Test
    @DisplayName("C-02: una hija con tipo distinto del padre falla con NATURALEZA_INVALIDA")
    void crear_tipoDistintoDelPadre_falla() {
        var padre = cuenta("101");

        fallaCon(() -> e.api.preparar(e.ctxA, new CrearCuentaComando("101.02", "Caja chica", padre.id(),
                TipoCuenta.PASIVO, null, true, null)), CodigoError.NATURALEZA_INVALIDA);
    }

    @Test
    @DisplayName("C-02: una cuenta raiz necesita tipo y naturaleza")
    void crear_raizSinNaturaleza_falla() {
        fallaCon(() -> e.api.preparar(e.ctxA, new CrearCuentaComando("301", "Capital", null, TipoCuenta.CAPITAL,
                null, false, null)), CodigoError.NATURALEZA_INVALIDA);

        e.confirmar(e.ctxA, new CrearCuentaComando("301", "Capital", null, TipoCuenta.CAPITAL, Naturaleza.ACREEDORA,
                false, null));
        assertThat(cuenta("301").naturaleza()).isEqualTo(Naturaleza.ACREEDORA);
    }

    @Test
    @DisplayName("C-02: un padre de otro negocio o inexistente falla con CUENTA_NO_ENCONTRADA")
    void crear_padreAjeno_falla() {
        e.activar(e.ctxB);
        var padreDeB = e.api.catalogo(e.ctxB).get(0);

        fallaCon(() -> e.api.preparar(e.ctxA, new CrearCuentaComando("999.01", "X", padreDeB.id(), null, null, true,
                null)), CodigoError.CUENTA_NO_ENCONTRADA);
        fallaCon(() -> e.api.preparar(e.ctxA, new CrearCuentaComando("999.01", "X", UUID.randomUUID(), null, null,
                true, null)), CodigoError.CUENTA_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("C-02: sin contabilidad activa falla con CONTABILIDAD_NO_ACTIVA")
    void crear_sinActivar_falla() {
        fallaCon(() -> e.api.preparar(e.ctxB, new CrearCuentaComando("301", "Capital", null, TipoCuenta.CAPITAL,
                Naturaleza.ACREEDORA, false, null)), CodigoError.CONTABILIDAD_NO_ACTIVA);
    }

    @Test
    @DisplayName("C-02: si otro crea el mismo codigo entre preparar y confirmar, confirmar falla con duplicado")
    void crear_duplicadoEntrePrepararYConfirmar_falla() {
        var padre = cuenta("601");
        var token = e.api.preparar(e.ctxA, new CrearCuentaComando("601.02", "Renta", padre.id(), null, null, true,
                null)).token();
        e.confirmar(e.ctxA, new CrearCuentaComando("601.02", "Renta", padre.id(), null, null, true, null));

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.CODIGO_CUENTA_DUPLICADO);
    }
}
