package mx.negocio360.inventario.aplicacion;

import static mx.negocio360.inventario.fakes.Afirmaciones.fallaCon;
import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import mx.negocio360.inventario.dominio.CausaTipo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.MotivoMerma;
import mx.negocio360.inventario.dominio.TipoMovimiento;
import mx.negocio360.inventario.eventos.MovimientoRegistrado;
import mx.negocio360.inventario.fakes.Escenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Entradas, salidas, mermas y ajustes por conteo valuados con CPP (U-05, C-02 a C-08, A-01 a A-03). */
class MovimientosTest {

    private final Escenario e = new Escenario();
    private UUID harina;

    @BeforeEach
    void crearHarina() {
        harina = e.harina(e.ctxA);
    }

    private MovimientoVista ultimo() {
        var nodos = e.api.kardex(e.ctxA, harina, 100, null).nodos();
        return nodos.get(nodos.size() - 1);
    }

    @Test
    @DisplayName("C-08 y U-05: la entrada convierte cantidad y costo a la unidad base y guarda lo capturado")
    void entrada_enKilos_convierteCantidadYCosto() {
        e.entrada(e.ctxA, harina, "2", "kg", "18");

        var movimiento = ultimo();
        assertThat(movimiento.tipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(movimiento.causaTipo()).isEqualTo(CausaTipo.COMPRA);
        assertThat(movimiento.cantidadCapturada()).isEqualTo(d("2"));
        assertThat(movimiento.unidadCapturada()).isEqualTo("kg");
        assertThat(movimiento.factorAplicado()).isEqualTo(d("1000.00000000"));
        assertThat(movimiento.cantidadBase()).isEqualTo(d("2000.0000"));
        assertThat(movimiento.costoUnitario()).isEqualTo(d("0.0180"));
        assertThat(movimiento.costoTotal()).isEqualTo(d("36.00"));
        assertThat(movimiento.cppResultante()).isEqualTo(d("0.0180"));
        assertThat(movimiento.saldoResultante()).isEqualTo(d("2000.0000"));
        assertThat(movimiento.registradoPor()).isEqualTo("locatario-a");
        assertThat(movimiento.ocurridoEn()).isEqualTo(Escenario.AHORA);
        var vista = e.vista(e.ctxA, harina);
        assertThat(vista.cantidadBase()).isEqualTo(d("2000.0000"));
        assertThat(vista.valor()).isEqualTo(d("36.00"));
    }

    @Test
    @DisplayName("C-02 y C-04: la segunda entrada promedia y la salida se valua al CPP sin cambiarlo")
    void entradasYSalida_cifrasAMano() {
        e.entrada(e.ctxA, harina, "2", "kg", "18");
        e.entrada(e.ctxA, harina, "1", "kg", "21");
        assertThat(ultimo().cppResultante()).isEqualTo(d("0.0190"));

        e.salida(e.ctxA, harina, "500", "g");

        var salida = ultimo();
        assertThat(salida.tipo()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(salida.causaTipo()).isEqualTo(CausaTipo.MANUAL);
        assertThat(salida.cantidadBase()).isEqualTo(d("-500.0000"));
        assertThat(salida.costoUnitario()).isEqualTo(d("0.0190"));
        assertThat(salida.costoTotal()).isEqualTo(d("-9.50"));
        assertThat(salida.cppResultante()).isEqualTo(d("0.0190"));
        assertThat(salida.saldoResultante()).isEqualTo(d("2500.0000"));
    }

    @Test
    @DisplayName("A-01 y C-03: la salida puede dejar saldo negativo y la entrada siguiente toma su propio costo")
    void salidaMayorAlSaldo_permitidaYAvisada() {
        e.entrada(e.ctxA, harina, "2", "kg", "20");
        e.eventos.limpiar();

        e.salida(e.ctxA, harina, "2.5", "kg");

        assertThat(ultimo().saldoResultante()).isEqualTo(d("-500.0000"));
        assertThat(e.eventos.tipos()).containsExactly("inventario.movimiento_registrado",
                "inventario.bajo_minimo", "inventario.saldo_negativo");

        e.entrada(e.ctxA, harina, "1", "costal", "750");
        assertThat(ultimo().cppResultante()).isEqualTo(d("0.0300"));
        assertThat(ultimo().saldoResultante()).isEqualTo(d("24500.0000"));
    }

    @Test
    @DisplayName("A-02: las alertas salen solo al cruzar el minimo, no en cada movimiento")
    void alertas_soloAlCruzar() {
        e.entrada(e.ctxA, harina, "2000", "g", "0.018");
        assertThat(e.eventos.deTipo("inventario.repuesto")).hasSize(1);

        e.salida(e.ctxA, harina, "500", "g");
        e.salida(e.ctxA, harina, "600", "g");
        e.salida(e.ctxA, harina, "100", "g");

        assertThat(e.eventos.deTipo("inventario.bajo_minimo")).hasSize(1);
        assertThat(e.eventos.deTipo("inventario.movimiento_registrado")).hasSize(4);
        var alerta = e.eventos.deTipo("inventario.bajo_minimo").get(0);
        assertThat(alerta.datos()).containsEntry("articulo_id", harina.toString())
                .containsEntry("nombre", "Harina")
                .containsEntry("saldo_base", "900.0000")
                .containsEntry("minimo_base", "1000.0000")
                .containsEntry("unidad_base", "g");
    }

    @Test
    @DisplayName("el evento movimiento_registrado lleva el contrato v1 con cifras en cadena")
    void evento_movimientoRegistrado_contrato() {
        e.entrada(e.ctxA, harina, "2", "kg", "18");

        var sobre = e.eventos.deTipo(MovimientoRegistrado.TIPO).get(0);
        var movimiento = ultimo();
        assertThat(sobre.version()).isEqualTo(1);
        assertThat(sobre.origen()).isEqualTo("inventario");
        assertThat(sobre.negocioId()).isEqualTo(e.negocioA);
        assertThat(sobre.correlacionId()).isEqualTo(movimiento.id());
        assertThat(sobre.ocurridoEn()).isEqualTo(Escenario.AHORA);
        assertThat(sobre.datos()).containsEntry("movimiento_id", movimiento.id().toString())
                .containsEntry("articulo_id", harina.toString())
                .containsEntry("tipo", "ENTRADA")
                .containsEntry("cantidad_base", "2000.0000")
                .containsEntry("unidad_base", "g")
                .containsEntry("costo_unitario", "0.0180")
                .containsEntry("costo_total", "36.00")
                .containsEntry("cpp_resultante", "0.0180")
                .containsEntry("saldo_resultante", "2000.0000")
                .containsEntry("causa_tipo", "COMPRA")
                .doesNotContainKey("causa_id");
    }

    @Test
    @DisplayName("C-07: una entrada con fecha pasada se registra con esa fecha y no reescribe el historial")
    void entrada_fechaPasada_seRegistraSinRecalcular() {
        e.entrada(e.ctxA, harina, "1", "kg", "20");
        var ayer = Escenario.AHORA.minus(Duration.ofDays(1));

        e.confirmar(e.ctxA, new RegistrarEntradaComando(harina, d("1"), "kg", d("30"), ayer));

        var kardex = e.api.kardex(e.ctxA, harina, 10, null).nodos();
        assertThat(kardex).hasSize(2);
        assertThat(kardex.get(0).cppResultante()).isEqualTo(d("0.0200"));
        assertThat(kardex.get(1).ocurridoEn()).isEqualTo(ayer);
        assertThat(kardex.get(1).cppResultante()).isEqualTo(d("0.0250"));
    }

    @Test
    @DisplayName("una entrada con fecha futura se rechaza")
    void entrada_fechaFutura_falla() {
        var manana = Escenario.AHORA.plus(Duration.ofMinutes(1));

        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("1"), "kg", d("1"), manana)),
                CodigoError.FECHA_FUTURA);
    }

    @Test
    @DisplayName("cantidades y costos fuera de rango se rechazan; el costo puede ser cero")
    void entrada_cantidadesYCostos_validados() {
        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("0"), "kg", d("1"), null)),
                CodigoError.CANTIDAD_INVALIDA);
        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, null, "kg", d("1"), null)),
                CodigoError.CANTIDAD_INVALIDA);
        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("0.00001"), "g", d("1"),
                null)), CodigoError.CANTIDAD_INVALIDA);
        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("10000000"), "kg", d("1"),
                null)), CodigoError.CANTIDAD_INVALIDA);
        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("1"), "kg", d("-1"), null)),
                CodigoError.COSTO_INVALIDO);
        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("1"), "kg", null, null)),
                CodigoError.COSTO_INVALIDO);
        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("1"), "kg", d("1.00001"),
                null)), CodigoError.COSTO_INVALIDO);
        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("1"), "ml", d("1"), null)),
                CodigoError.UNIDAD_NO_CONVERTIBLE);

        e.entrada(e.ctxA, harina, "1", "kg", "0");
        assertThat(ultimo().costoTotal()).isEqualTo(d("0.00"));
    }

    @Test
    @DisplayName("la merma exige motivo y se registra como MERMA al CPP")
    void merma_conMotivo_registra() {
        e.entrada(e.ctxA, harina, "2", "kg", "18");

        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarMermaComando(harina, d("1"), "kg", null)),
                CodigoError.MOTIVO_REQUERIDO);
        e.confirmar(e.ctxA, new RegistrarMermaComando(harina, d("250"), "g", MotivoMerma.DANO));

        var merma = ultimo();
        assertThat(merma.tipo()).isEqualTo(TipoMovimiento.MERMA);
        assertThat(merma.causaTipo()).isEqualTo(CausaTipo.MERMA);
        assertThat(merma.motivo()).isEqualTo(MotivoMerma.DANO);
        assertThat(merma.cantidadBase()).isEqualTo(d("-250.0000"));
        assertThat(merma.costoTotal()).isEqualTo(d("-4.50"));
    }

    @Test
    @DisplayName("A-03 y C-06: el ajuste por conteo calcula la diferencia y la registra al CPP vigente")
    void ajustePorConteo_diferencias() {
        e.entrada(e.ctxA, harina, "2.5", "kg", "19");

        e.confirmar(e.ctxA, new AjustePorConteoComando(harina, d("2.4"), "kg"));
        var negativo = ultimo();
        assertThat(negativo.tipo()).isEqualTo(TipoMovimiento.AJUSTE_NEG);
        assertThat(negativo.causaTipo()).isEqualTo(CausaTipo.CONTEO);
        assertThat(negativo.cantidadBase()).isEqualTo(d("-100.0000"));
        assertThat(negativo.costoTotal()).isEqualTo(d("-1.90"));

        e.confirmar(e.ctxA, new AjustePorConteoComando(harina, d("2600"), "g"));
        var positivo = ultimo();
        assertThat(positivo.tipo()).isEqualTo(TipoMovimiento.AJUSTE_POS);
        assertThat(positivo.cantidadBase()).isEqualTo(d("200.0000"));
        assertThat(positivo.costoUnitario()).isEqualTo(d("0.0190"));
        assertThat(positivo.cppResultante()).isEqualTo(d("0.0190"));
        assertThat(positivo.saldoResultante()).isEqualTo(d("2600.0000"));
    }

    @Test
    @DisplayName("A-03: contar lo mismo que hay no genera movimiento; contar negativo no se permite")
    void ajustePorConteo_sinDiferenciaONegativo_falla() {
        e.entrada(e.ctxA, harina, "2", "kg", "18");

        fallaCon(() -> e.api.preparar(e.ctxA, new AjustePorConteoComando(harina, d("2000"), "g")),
                CodigoError.CONTEO_SIN_DIFERENCIA);
        fallaCon(() -> e.api.preparar(e.ctxA, new AjustePorConteoComando(harina, d("-1"), "g")),
                CodigoError.CANTIDAD_INVALIDA);
    }

    @Test
    @DisplayName("A-03: contar cero con saldo negativo repone hasta cero")
    void ajustePorConteo_ceroConSaldoNegativo_repone() {
        e.salida(e.ctxA, harina, "300", "g");

        e.confirmar(e.ctxA, new AjustePorConteoComando(harina, d("0"), "g"));

        assertThat(ultimo().tipo()).isEqualTo(TipoMovimiento.AJUSTE_POS);
        assertThat(ultimo().saldoResultante()).isEqualTo(d("0.0000"));
    }

    @Test
    @DisplayName("un articulo dado de baja no admite movimientos")
    void movimiento_articuloInactivo_falla() {
        var refresco = e.crear(e.ctxA, "Refresco", "pieza", null, null, null);
        e.confirmar(e.ctxA, new BajaArticuloComando(refresco));

        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarEntradaComando(refresco, d("1"), "pieza", d("1"), null)),
                CodigoError.ARTICULO_INACTIVO);
        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarSalidaComando(refresco, d("1"), "pieza")),
                CodigoError.ARTICULO_INACTIVO);
    }

    @Test
    @DisplayName("un negocio no puede mover el inventario de otro")
    void movimiento_articuloAjeno_noEncontrado() {
        fallaCon(() -> e.api.preparar(e.ctxB, new RegistrarSalidaComando(harina, d("1"), "kg")),
                CodigoError.ARTICULO_NO_ENCONTRADO);
        fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarSalidaComando(null, d("1"), "kg")),
                CodigoError.ARTICULO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("el resumen de un movimiento muestra la conversion, el saldo y el costo que van a quedar")
    void preparar_movimiento_resumenConCifrasDelServidor() {
        e.entrada(e.ctxA, harina, "2", "kg", "18");

        var resumen = e.api.preparar(e.ctxA, new RegistrarSalidaComando(harina, d("0.5"), "kg")).resumen();

        assertThat(resumen).isEqualTo("Registrar salida de 0.5 kg (500 g) de Harina. Valor: $9.00. "
                + "Saldo: 2000 g → 1500 g. Costo promedio por g: 0.0180 → 0.0180.");
        assertThat(e.api.kardex(e.ctxA, harina, 10, null).nodos()).hasSize(1);
    }

    @Test
    @DisplayName("el kardex pagina por cursor en orden de registro")
    void kardex_paginaPorCursor() {
        e.entrada(e.ctxA, harina, "1", "kg", "20");
        e.salida(e.ctxA, harina, "100", "g");
        e.salida(e.ctxA, harina, "200", "g");

        var primera = e.api.kardex(e.ctxA, harina, 2, null);
        var segunda = e.api.kardex(e.ctxA, harina, 2, primera.cursor());

        assertThat(primera.nodos()).extracting(MovimientoVista::cantidadBase)
                .containsExactly(d("1000.0000"), d("-100.0000"));
        assertThat(primera.hayMas()).isTrue();
        assertThat(segunda.nodos()).extracting(MovimientoVista::cantidadBase).containsExactly(d("-200.0000"));
        assertThat(segunda.hayMas()).isFalse();
    }

    @Test
    @DisplayName("el kardex de un articulo ajeno o inexistente no existe")
    void kardex_articuloAjeno_noEncontrado() {
        fallaCon(() -> e.api.kardex(e.ctxB, harina, 10, null), CodigoError.ARTICULO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("si otra peticion movio el saldo al mismo tiempo, el movimiento falla con CONCURRENCIA y no se guarda")
    void movimiento_carreraDeSaldo_concurrencia() {
        var token = e.api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("1"), "kg", d("1"), null)).token();
        e.almacen.simularCarreraDeSaldo = true;

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.CONCURRENCIA);

        assertThat(e.almacen.movimientos()).isEmpty();
        assertThat(e.eventos.todos()).isEmpty();
    }

    @Test
    @DisplayName("varios movimientos seguidos: el saldo del kardex coincide con el del articulo")
    void kardex_saldoFinal_coincideConArticulo() {
        e.entrada(e.ctxA, harina, "3", "kg", "18.5");
        e.salida(e.ctxA, harina, "750", "g");
        e.confirmar(e.ctxA, new RegistrarMermaComando(harina, d("50"), "g", MotivoMerma.CADUCIDAD));
        e.confirmar(e.ctxA, new AjustePorConteoComando(harina, d("2.1"), "kg"));

        var kardex = e.api.kardex(e.ctxA, harina, 10, null).nodos();
        var suma = kardex.stream().map(MovimientoVista::cantidadBase).reduce(d("0"), (a, b) -> a.add(b));
        assertThat(suma).isEqualByComparingTo(e.vista(e.ctxA, harina).cantidadBase());
        assertThat(kardex).extracting(MovimientoVista::tipo).containsExactly(TipoMovimiento.ENTRADA,
                TipoMovimiento.SALIDA, TipoMovimiento.MERMA, TipoMovimiento.AJUSTE_NEG);
        assertThat(List.of(kardex.get(3).saldoResultante())).containsExactly(d("2100.0000"));
    }
}
