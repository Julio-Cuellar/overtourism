package mx.negocio360.ventas.aplicacion;

import static mx.negocio360.ventas.fakes.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.ventas.aplicacion.puertos.ProductoParaVenta;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.MetodoPago;
import mx.negocio360.ventas.fakes.Escenario;
import mx.negocio360.ventas.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Correcciones de la revision de seguridad y calidad del 2026-10-15 (Bitacora): turno verificado por Ventas, limites de
 * entrada con codigo propio, cupon que no se gasta si la venta falla, lineas completas en el resumen, copias
 * defensivas, {@code toString} sin datos personales y textos que no invitan a reusar un token gastado.
 */
class RevisionVentasTest {

    private static final Duration RAPIDO = Duration.ofSeconds(2);

    private final Escenario e = new Escenario();

    private static RegistrarVentaComando venta(List<LineaSolicitada> lineas, List<PagoSolicitado> pagos,
                                               String cupon) {
        return new RegistrarVentaComando(Fixtures.TURNO, lineas, pagos, cupon, null);
    }

    // VT-01: Ventas pregunta a Caja si el turno esta abierto

    @Test
    @DisplayName("VT-01: si Caja dice que el turno no esta abierto, preparar falla con TURNO_NO_ABIERTO")
    void preparar_turnoNoAbierto() {
        e.turnos.cerrar(e.negocioA, Fixtures.TURNO);

        fallaCon(() -> e.api.preparar(e.ctxA, Fixtures.ventaSinCupon()), CodigoError.TURNO_NO_ABIERTO);
    }

    @Test
    @DisplayName("VT-01: un turno que no existe en el negocio falla con TURNO_NO_ABIERTO")
    void preparar_turnoDesconocido() {
        var comando = new RegistrarVentaComando(UUID.randomUUID(), Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.efectivo("150.00", null)), null, null);

        fallaCon(() -> e.api.preparar(e.ctxA, comando), CodigoError.TURNO_NO_ABIERTO);
    }

    @Test
    @DisplayName("VT-01: si el turno entra en corte entre preparar y confirmar, no hay venta ni evento")
    void confirmar_turnoCerroEntrePasos() {
        var token = e.api.preparar(e.ctxA, Fixtures.ventaSinCupon()).token();
        e.turnos.cerrar(e.negocioA, Fixtures.TURNO);

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.TURNO_NO_ABIERTO);
        assertThat(e.ventas.todas()).isEmpty();
        assertThat(e.eventos.todos()).isEmpty();
    }

    // Limites de entrada

    @Test
    @DisplayName("mas de 100 lineas falla con DEMASIADAS_LINEAS sin consultar Catalogo")
    void preparar_demasiadasLineas() {
        var lineas = Collections.nCopies(CalculadoraVenta.MAX_LINEAS + 1,
                new LineaSolicitada(Fixtures.BOLETO, BigDecimal.ONE, null));

        fallaCon(() -> e.api.preparar(e.ctxA, venta(lineas, List.of(Fixtures.efectivo("45.00", null)), null)),
                CodigoError.DEMASIADAS_LINEAS);
        assertThat(e.catalogo.consultas()).isZero();
    }

    @Test
    @DisplayName("mas de 10 pagos falla con DEMASIADOS_PAGOS sin consultar Catalogo")
    void preparar_demasiadosPagos() {
        var pagos = Collections.nCopies(CalculadoraVenta.MAX_PAGOS + 1, Fixtures.tarjeta("10.00"));

        fallaCon(() -> e.api.preparar(e.ctxA, venta(Fixtures.dosBoletosYMedioKilo(), pagos, null)),
                CodigoError.DEMASIADOS_PAGOS);
        assertThat(e.catalogo.consultas()).isZero();
    }

    @Test
    @DisplayName("un codigo de cupon de mas de 40 caracteres falla con CUPON_INVALIDO sin llamar a Pasaporte")
    void preparar_cuponDemasiadoLargo() {
        var comando = venta(Fixtures.dosBoletosYMedioKilo(), List.of(Fixtures.efectivo("150.00", null)),
                "X".repeat(ManejadorRegistrarVenta.MAX_CARACTERES_CUPON + 1));

        fallaCon(() -> e.api.preparar(e.ctxA, comando), CodigoError.CUPON_INVALIDO);
        assertThat(e.cupones.cotizaciones()).isZero();
    }

    @Test
    @DisplayName("un monto o un recibido enorme con exponente se rechaza al instante con METODO_PAGO_INVALIDO")
    void pagos_montoEnorme_rechazoInmediato() {
        var enorme = new BigDecimal("1E+999999999");
        var total = new BigDecimal("150.00");

        assertTimeoutPreemptively(RAPIDO, () -> {
            fallaCon(() -> CalculadoraVenta.pagos(total, List.of(new PagoSolicitado(MetodoPago.TARJETA, enorme,
                    null, null))), CodigoError.METODO_PAGO_INVALIDO);
            fallaCon(() -> CalculadoraVenta.pagos(total, List.of(new PagoSolicitado(MetodoPago.EFECTIVO, total,
                    enorme, null))), CodigoError.METODO_PAGO_INVALIDO);
        });
    }

    @Test
    @DisplayName("una referencia de pago o una serie de mas de 60 caracteres falla con TEXTO_DEMASIADO_LARGO")
    void textosLargos_textoDemasiadoLargo() {
        var largo = "R".repeat(61);
        var total = new BigDecimal("10.00");

        fallaCon(() -> CalculadoraVenta.pagos(total, List.of(new PagoSolicitado(MetodoPago.TARJETA, total, null,
                largo))), CodigoError.TEXTO_DEMASIADO_LARGO);
        fallaCon(() -> CalculadoraVenta.lineas(List.of(new LineaSolicitada(Fixtures.BOLETO, BigDecimal.ONE, largo)),
                Map.of(Fixtures.BOLETO, Fixtures.boleto())), CodigoError.TEXTO_DEMASIADO_LARGO);
    }

    // Cupon: Pasaporte recibe el descuento esperado

    @Test
    @DisplayName("VT-07: el canje recibe el descuento que vio el cajero")
    void confirmar_canjeConDescuentoEsperado() {
        e.confirmar(e.ctxA, Fixtures.ventaConCupon());

        assertThat(e.cupones.canjes()).singleElement()
                .satisfies(c -> assertThat(c.descuentoEsperado()).isEqualByComparingTo("15.00"));
    }

    @Test
    @DisplayName("VT-07: si al canjear el descuento ya no es el esperado, el cupon no se gasta y no hay venta")
    void confirmar_descuentoCambiaAlCanjear_cuponIntacto() {
        var token = e.api.preparar(e.ctxA, Fixtures.ventaConCupon()).token();
        e.cupones.descuentoAlCanjear = new BigDecimal("20.00");

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.COMANDO_CAMBIO);
        assertThat(e.cupones.canjes()).isEmpty();
        assertThat(e.ventas.todas()).isEmpty();
        assertThat(e.eventos.todos()).isEmpty();
    }

    // VT-05 con lineas completas

    @Test
    @DisplayName("VT-05: el resumen muestra cada linea con descripcion, cantidad, precio e importe")
    void preparar_resumenConLineas() {
        var resumen = e.api.preparar(e.ctxA, Fixtures.ventaSinCupon()).resumen();

        assertThat(resumen).contains("Boleto general").contains("2 pieza").contains("45.00").contains("90.00")
                .contains("Camote poblano").contains("0.5 kg").contains("120.00").contains("60.00");
    }

    @Test
    @DisplayName("VT-05: si dos precios cambian y se compensan (el subtotal sigue igual), confirmar da COMANDO_CAMBIO")
    void confirmar_preciosQueSeCompensan_comandoCambio() {
        var token = e.api.preparar(e.ctxA, Fixtures.ventaSinCupon()).token();
        e.catalogo.con(e.negocioA, new ProductoParaVenta(Fixtures.BOLETO, "Boleto general", "pieza",
                new BigDecimal("40.00"), false, true));
        e.catalogo.con(e.negocioA, new ProductoParaVenta(Fixtures.CAMOTE, "Camote poblano", "kg",
                new BigDecimal("140.00"), true, true));

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.COMANDO_CAMBIO);
        assertThat(e.ventas.todas()).isEmpty();
    }

    // Copias defensivas y toString

    @Test
    @DisplayName("el comando copia sus listas: cambiar la lista original despues no lo altera")
    void comando_copiaSusListas() {
        var lineas = new ArrayList<>(Fixtures.dosBoletosYMedioKilo());
        var pagos = new ArrayList<>(List.of(Fixtures.efectivo("150.00", null)));
        var comando = new RegistrarVentaComando(Fixtures.TURNO, lineas, pagos, null, null);

        lineas.clear();
        pagos.clear();

        assertThat(comando.lineas()).hasSize(2);
        assertThat(comando.pagos()).hasSize(1);
        assertThatThrownBy(() -> comando.lineas().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(new RegistrarVentaComando(Fixtures.TURNO, null, null, null, null).lineas()).isNull();
    }

    @Test
    @DisplayName("toString de comandos y ventas no muestra cupon, cliente, motivo ni actor")
    void toString_sinDatosSensibles() {
        var cliente = UUID.randomUUID();
        var motivo = "Lo pidio la clienta Maria por telefono";
        var comando = new RegistrarVentaComando(Fixtures.TURNO, Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.efectivo("135.00", null)), Fixtures.CODIGO_CUPON, cliente);
        var ventaId = e.vender(e.ctxA, comando);
        var anular = new AnularVentaComando(ventaId, motivo);
        e.confirmar(new mx.negocio360.compartido.contexto.ContextoNegocio(e.negocioA, "encargado-a"), anular);
        var venta = e.api.venta(e.ctxA, ventaId).orElseThrow();

        assertThat(comando.toString()).doesNotContain(Fixtures.CODIGO_CUPON).doesNotContain(cliente.toString());
        assertThat(anular.toString()).doesNotContain(motivo);
        assertThat(venta.toString()).doesNotContain(motivo).doesNotContain(cliente.toString())
                .doesNotContain("cajero-a").doesNotContain("encargado-a");
    }

    @Test
    @DisplayName("los errores reintentables piden volver a empezar, porque el token ya se gasto")
    void textos_noInvitanAReusarElToken() {
        for (var codigo : CodigoError.values()) {
            assertThat(PlantillasVentas.de(codigo)).doesNotContainIgnoringCase("inténtalo de nuevo");
        }
        assertThat(PlantillasVentas.de(CodigoError.CONCURRENCIA)).contains("vuelve a empezar");
        assertThat(PlantillasVentas.de(CodigoError.ERROR_TEMPORAL)).contains("vuelve a empezar");
    }
}
