package mx.negocio360.inventario.aplicacion;

import static mx.negocio360.inventario.fakes.Afirmaciones.fallaCon;
import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.fakes.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Revision de codigo A1, A2 y A3: precision del costo, cotas de magnitud y desbordes de las columnas. */
class PrecisionYLimitesTest {

    private static final Duration TOPE = Duration.ofMillis(500);
    private final Escenario e = new Escenario();

    private MovimientoVista ultimo(UUID articulo) {
        var nodos = e.api.kardex(e.ctxA, articulo, 100, null).nodos();
        return nodos.get(nodos.size() - 1);
    }

    @Test
    @DisplayName("A1: 25 kg a $18.55/kg cuestan $463.75 exactos, no $465.00 por redondear el costo por gramo")
    void entrada_centavosPorKilo_totalExacto() {
        var harina = e.harina(e.ctxA);

        var resumen = e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("25"), "kg", d("18.55"), null))
                .resumen();
        e.entrada(e.ctxA, harina, "25", "kg", "18.55");

        assertThat(ultimo(harina).costoTotal()).isEqualTo(d("463.75"));
        assertThat(resumen).contains("Valor: $463.75.");
    }

    @Test
    @DisplayName("A1: 1000 cajas de 24 piezas a $100 cuestan $100000.00, no $100000.80")
    void entrada_cajaDe24_totalExacto() {
        var refresco = e.crear(e.ctxA, "Refresco", "pieza", null, null,
                List.of(new UnidadPropiaPedida("caja", d("24"))));

        e.confirmar(e.ctxA, new RegistrarEntradaComando(refresco, d("1000"), "caja", d("100"), null));

        assertThat(ultimo(refresco).costoTotal()).isEqualTo(d("100000.00"));
    }

    @Test
    @DisplayName("A2: un numero con exponente enorme se rechaza de inmediato, sin agotar CPU ni memoria")
    void cantidadesConExponenteEnorme_seRechazanRapido() {
        var harina = e.harina(e.ctxA);
        var enorme = d("1E+3000000");

        assertTimeoutPreemptively(TOPE, () -> {
            fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, enorme, "kg", d("1"), null)),
                    CodigoError.CANTIDAD_INVALIDA);
            fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("1"), "kg", enorme, null)),
                    CodigoError.COSTO_INVALIDO);
            fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarSalidaComando(harina, enorme, "g")),
                    CodigoError.CANTIDAD_INVALIDA);
            fallaCon(() -> e.api.preparar(e.ctxA, new AjustePorConteoComando(harina, enorme, "g")),
                    CodigoError.CANTIDAD_INVALIDA);
            fallaCon(() -> e.api.preparar(e.ctxA, new DefinirUnidadPropiaComando(harina, "bulto", enorme)),
                    CodigoError.UNIDAD_PROPIA_INVALIDA);
            fallaCon(() -> e.api.preparar(e.ctxA, new EditarArticuloComando(harina, null, null, enorme, "g")),
                    CodigoError.CANTIDAD_INVALIDA);
            fallaCon(() -> e.api.aBase(e.ctxA, List.of(new CantidadPedida(harina, enorme, "g"))),
                    CodigoError.CANTIDAD_INVALIDA);
        });
    }

    @Test
    @DisplayName("A3: dos entradas validas cuya suma no cabe en la columna del saldo se rechazan")
    void entrada_quePasaElSaldoMaximo_falla() {
        var harina = e.harina(e.ctxA);
        e.entrada(e.ctxA, harina, "6000000000", "g", "0");

        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("6000000000"), "g", d("0"),
                null)), CodigoError.CANTIDAD_INVALIDA);
    }

    @Test
    @DisplayName("A3: salidas repetidas que llevan el saldo negativo mas alla de la columna se rechazan")
    void salida_quePasaElSaldoMinimo_falla() {
        var harina = e.harina(e.ctxA);
        e.salida(e.ctxA, harina, "6000000000", "g");

        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarSalidaComando(harina, d("6000000000"), "g")),
                CodigoError.CANTIDAD_INVALIDA);
    }

    @Test
    @DisplayName("A3: un costo total que no cabe en NUMERIC(14,2) se rechaza con COSTO_INVALIDO")
    void entrada_costoTotalEnorme_falla() {
        var harina = e.harina(e.ctxA);

        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("9000000000"), "g",
                d("9000000000"), null)), CodigoError.COSTO_INVALIDO);
    }

    @Test
    @DisplayName("A3: lo capturado tambien debe caber: 9.9e12 g sobre base kg da 9.9e9 kg pero no cabe como capturado")
    void entrada_capturadaFueraDeRango_falla() {
        var azucar = e.crear(e.ctxA, "Azúcar", "kg", null, null, null);

        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(azucar, d("9900000000000"), "g", d("0"),
                null)), CodigoError.CANTIDAD_INVALIDA);
    }
}
