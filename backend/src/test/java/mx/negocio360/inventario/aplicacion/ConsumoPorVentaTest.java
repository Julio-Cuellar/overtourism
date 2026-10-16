package mx.negocio360.inventario.aplicacion;

import static mx.negocio360.inventario.fakes.Afirmaciones.fallaCon;
import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import mx.negocio360.inventario.dominio.CausaTipo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.TipoMovimiento;
import mx.negocio360.inventario.eventos.AlertaDeSaldo;
import mx.negocio360.inventario.eventos.MovimientoRegistrado;
import mx.negocio360.inventario.fakes.Escenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Consumo por venta, sin confirmacion humana (6.4, D-22): {@code registrarConsumo} y {@code revertirConsumo} son
 * atomicos e idempotentes por (negocio, causa, consumidor) y dejan la marca de anulacion previa (K-01 a K-04).
 */
class ConsumoPorVentaTest {

    private static final String RESTAURANTES = "restaurantes";

    private final Escenario e = new Escenario();
    private final UUID venta = UUID.randomUUID();
    private UUID harina;
    private UUID azucar;

    @BeforeEach
    void inventarioConExistencias() {
        harina = e.harina(e.ctxA);
        azucar = e.crear(e.ctxA, "Azúcar", "g", null, null, null);
        e.entrada(e.ctxA, harina, "10", "kg", "20");
        e.entrada(e.ctxA, azucar, "5", "kg", "30");
        e.eventos.limpiar();
    }

    private static LineaDeConsumo linea(UUID articulo, String cantidad, String unidad) {
        return new LineaDeConsumo(articulo, d(cantidad), unidad);
    }

    private ConsumoResultado consumir(UUID causa, String consumidor, LineaDeConsumo... lineas) {
        return e.api.registrarConsumo(e.ctxA, consumidor, causa, null, List.of(lineas));
    }

    private int movimientosTotales() {
        return e.almacen.movimientos().size();
    }

    @Test
    @DisplayName("K-01: registrar descuenta cada linea al CPP, devuelve REGISTRADO con el costo total y deja movimientos de venta")
    void registrar_descuentaAlCpp() {
        var resultado = consumir(venta, RESTAURANTES, linea(harina, "500", "g"), linea(azucar, "0.2", "kg"));

        assertThat(resultado.estado()).isEqualTo(EstadoDeConsumo.REGISTRADO);
        assertThat(resultado.costoTotal()).isEqualByComparingTo("16.00");
        assertThat(resultado.movimientos()).hasSize(2);
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("9500");
        assertThat(e.vista(e.ctxA, azucar).cantidadBase()).isEqualByComparingTo("4800");
        var kardex = e.api.kardex(e.ctxA, harina, 100, null).nodos();
        var ultimo = kardex.get(kardex.size() - 1);
        assertThat(ultimo.tipo()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(ultimo.causaTipo()).isEqualTo(CausaTipo.VENTA);
        assertThat(ultimo.costoUnitario()).isEqualByComparingTo("0.02");
        assertThat(ultimo.costoTotal()).isEqualByComparingTo("-10.00");
        assertThat(ultimo.registradoPor()).isEqualTo("locatario-a");
        assertThat(resultado.movimientos()).contains(ultimo.id());
    }

    @Test
    @DisplayName("el consumo no usa preparar -> confirmar (D-22): no guarda ningun token")
    void registrar_noUsaPreparacion() {
        var antes = e.preparaciones.cuantas();

        consumir(venta, RESTAURANTES, linea(harina, "100", "g"));
        e.api.revertirConsumo(e.ctxA, RESTAURANTES, venta);

        assertThat(antes).isPositive();
        assertThat(e.preparaciones.cuantas()).isEqualTo(antes);
    }

    @Test
    @DisplayName("el movimiento publicado lleva causa_tipo VENTA y causa_id con el id de la venta")
    void registrar_publicaMovimientoConCausa() {
        consumir(venta, RESTAURANTES, linea(harina, "100", "g"));

        var sobres = e.eventos.deTipo(MovimientoRegistrado.TIPO);
        assertThat(sobres).hasSize(1);
        assertThat(sobres.get(0).datos()).containsEntry("causa_tipo", "VENTA")
                .containsEntry("causa_id", venta.toString());
    }

    @Test
    @DisplayName("K-01: repetir la llamada devuelve YA_REGISTRADO con lo guardado y no vuelve a descontar")
    void registrar_esIdempotente() {
        var primero = consumir(venta, RESTAURANTES, linea(harina, "500", "g"));
        var movimientos = movimientosTotales();
        e.eventos.limpiar();

        var repetido = consumir(venta, RESTAURANTES, linea(harina, "500", "g"));

        assertThat(repetido.estado()).isEqualTo(EstadoDeConsumo.YA_REGISTRADO);
        assertThat(repetido.movimientos()).isEqualTo(primero.movimientos());
        assertThat(repetido.costoTotal()).isEqualByComparingTo(primero.costoTotal());
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("9500");
        assertThat(movimientosTotales()).isEqualTo(movimientos);
        assertThat(e.eventos.todos()).isEmpty();
    }

    @Test
    @DisplayName("K-01: la llave incluye al consumidor y al negocio, asi que otros consumen aparte")
    void registrar_llaveCompleta() {
        consumir(venta, RESTAURANTES, linea(harina, "500", "g"));

        var otroConsumidor = consumir(venta, "vinculo", linea(harina, "100", "g"));
        var ajeno = e.harina(e.ctxB);
        e.entrada(e.ctxB, ajeno, "1", "kg", "20");
        var otroNegocio = e.api.registrarConsumo(e.ctxB, RESTAURANTES, venta, null, List.of(linea(ajeno, "100", "g")));

        assertThat(otroConsumidor.estado()).isEqualTo(EstadoDeConsumo.REGISTRADO);
        assertThat(otroNegocio.estado()).isEqualTo(EstadoDeConsumo.REGISTRADO);
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("9400");
    }

    @Test
    @DisplayName("K-02: si una linea no sirve, no se descuenta ninguna y despues se puede registrar completo")
    void registrar_esAtomico() {
        var movimientos = movimientosTotales();

        fallaCon(() -> consumir(venta, RESTAURANTES, linea(harina, "500", "g"), linea(UUID.randomUUID(), "1", "g")),
                CodigoError.ARTICULO_NO_ENCONTRADO);

        assertThat(movimientosTotales()).isEqualTo(movimientos);
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("10000");
        assertThat(e.consumos.cuantos()).isZero();
        assertThat(consumir(venta, RESTAURANTES, linea(harina, "500", "g")).estado())
                .isEqualTo(EstadoDeConsumo.REGISTRADO);
    }

    @Test
    @DisplayName("K-02: un articulo dado de baja en la segunda linea tambien deja todo intacto")
    void registrar_articuloInactivo() {
        var sal = e.crear(e.ctxA, "Sal", "g", null, null, null);
        e.confirmar(e.ctxA, new BajaArticuloComando(sal));

        fallaCon(() -> consumir(venta, RESTAURANTES, linea(harina, "500", "g"), linea(sal, "1", "g")),
                CodigoError.ARTICULO_INACTIVO);

        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("lineas con unidad no convertible, cantidad cero o sin lineas se rechazan")
    void registrar_validaLineas() {
        fallaCon(() -> consumir(venta, RESTAURANTES, linea(harina, "1", "l")), CodigoError.UNIDAD_NO_CONVERTIBLE);
        fallaCon(() -> consumir(venta, RESTAURANTES, linea(harina, "0", "g")), CodigoError.CANTIDAD_INVALIDA);
        fallaCon(() -> consumir(venta, RESTAURANTES, linea(harina, "-5", "g")), CodigoError.CANTIDAD_INVALIDA);
        fallaCon(() -> consumir(venta, RESTAURANTES), CodigoError.CANTIDAD_INVALIDA);
        assertThat(movimientosTotales()).isEqualTo(2);
    }

    @Test
    @DisplayName("el consumidor solo puede ser vinculo o restaurantes")
    void registrar_consumidorInvalido() {
        fallaCon(() -> consumir(venta, "otro", linea(harina, "1", "g")), CodigoError.CONSUMIDOR_INVALIDO);
        fallaCon(() -> e.api.revertirConsumo(e.ctxA, "otro", venta), CodigoError.CONSUMIDOR_INVALIDO);
        fallaCon(() -> e.api.revertirConsumo(e.ctxA, null, venta), CodigoError.CONSUMIDOR_INVALIDO);
        assertThat(consumir(venta, "vinculo", linea(harina, "1", "g")).estado()).isEqualTo(EstadoDeConsumo.REGISTRADO);
    }

    @Test
    @DisplayName("el mismo articulo en dos lineas se descuenta en cadena y puede dejar el saldo en negativo (A-01)")
    void registrar_mismoArticuloDosVeces_yNegativo() {
        var resultado = consumir(venta, RESTAURANTES, linea(harina, "6", "kg"), linea(harina, "5", "kg"));

        assertThat(resultado.movimientos()).hasSize(2);
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("-1000");
        assertThat(resultado.costoTotal()).isEqualByComparingTo("220.00");
        assertThat(e.eventos.tipos()).contains(AlertaDeSaldo.SALDO_NEGATIVO);
        assertThat(resultado.alertas()).extracting(AlertaDeConsumo::tipo).contains(AlertaDeSaldo.SALDO_NEGATIVO);
    }

    @Test
    @DisplayName("A-02: cruzar el minimo avisa una sola vez, en el evento y en el resultado")
    void registrar_alertaPorCruce() {
        var resultado = consumir(venta, RESTAURANTES, linea(harina, "9500", "g"));

        assertThat(e.eventos.tipos()).containsOnlyOnce(AlertaDeSaldo.BAJO_MINIMO);
        assertThat(resultado.alertas()).containsExactly(new AlertaDeConsumo(harina, AlertaDeSaldo.BAJO_MINIMO));
    }

    @Test
    @DisplayName("ocurridoEn en el pasado se respeta, nulo es ahora y uno futuro se rechaza")
    void registrar_fechaDelHecho() {
        var ayer = Escenario.AHORA.minus(Duration.ofDays(1));

        e.api.registrarConsumo(e.ctxA, RESTAURANTES, venta, ayer, List.of(linea(harina, "10", "g")));
        e.api.registrarConsumo(e.ctxA, RESTAURANTES, UUID.randomUUID(), null, List.of(linea(harina, "10", "g")));
        var futura = Escenario.AHORA.plus(Duration.ofMinutes(5));

        var kardex = e.api.kardex(e.ctxA, harina, 100, null).nodos();
        assertThat(kardex.get(kardex.size() - 2).ocurridoEn()).isEqualTo(ayer);
        assertThat(kardex.get(kardex.size() - 1).ocurridoEn()).isEqualTo(Escenario.AHORA);
        fallaCon(() -> e.api.registrarConsumo(e.ctxA, RESTAURANTES, UUID.randomUUID(), futura,
                List.of(linea(harina, "10", "g"))), CodigoError.FECHA_FUTURA);
    }

    @Test
    @DisplayName("mas de 100 lineas se rechaza como aBase")
    void registrar_demasiadasLineas() {
        var lineas = IntStream.range(0, 101).mapToObj(i -> linea(harina, "1", "g")).toList();

        assertThatThrownBy(() -> e.api.registrarConsumo(e.ctxA, RESTAURANTES, venta, null, lineas))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("si otra peticion gana la carrera por el saldo, falla con CONCURRENCIA y no queda consumo guardado")
    void registrar_carreraDeSaldo() {
        e.almacen.simularCarreraDeSaldo = true;

        fallaCon(() -> consumir(venta, RESTAURANTES, linea(harina, "500", "g")), CodigoError.CONCURRENCIA);

        e.almacen.simularCarreraDeSaldo = false;
        assertThat(e.consumos.cuantos()).isZero();
        assertThat(consumir(venta, RESTAURANTES, linea(harina, "500", "g")).estado())
                .isEqualTo(EstadoDeConsumo.REGISTRADO);
    }

    @Test
    @DisplayName("K-03: revertir repone al costo original aunque el CPP haya cambiado, y marca REVERTIDO")
    void revertir_reponeAlCostoOriginal() {
        consumir(venta, RESTAURANTES, linea(harina, "500", "g"));
        e.entrada(e.ctxA, harina, "10", "kg", "40");
        var cppAntes = e.vista(e.ctxA, harina).cpp();
        assertThat(cppAntes).isGreaterThan(d("0.02"));
        e.eventos.limpiar();

        var resultado = e.api.revertirConsumo(e.ctxA, RESTAURANTES, venta);

        assertThat(resultado.estado()).isEqualTo(EstadoDeConsumo.REVERTIDO);
        assertThat(resultado.movimientos()).hasSize(1);
        assertThat(resultado.costoTotal()).isEqualByComparingTo("10.00");
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("20000");
        var kardex = e.api.kardex(e.ctxA, harina, 100, null).nodos();
        var reversa = kardex.get(kardex.size() - 1);
        assertThat(reversa.tipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(reversa.causaTipo()).isEqualTo(CausaTipo.REVERSION);
        assertThat(reversa.costoUnitario()).isEqualByComparingTo("0.02");
        assertThat(reversa.costoTotal()).isEqualByComparingTo("10.00");
        assertThat(e.eventos.deTipo(MovimientoRegistrado.TIPO).get(0).datos())
                .containsEntry("causa_tipo", "REVERSION").containsEntry("causa_id", venta.toString());
    }

    @Test
    @DisplayName("K-03: revertir dos veces devuelve YA_REVERTIDO sin movimientos nuevos, y registrar despues no vuelve a descontar")
    void revertir_esIdempotente() {
        consumir(venta, RESTAURANTES, linea(harina, "500", "g"));
        e.api.revertirConsumo(e.ctxA, RESTAURANTES, venta);
        var movimientos = movimientosTotales();

        var repetido = e.api.revertirConsumo(e.ctxA, RESTAURANTES, venta);
        var registrarOtraVez = consumir(venta, RESTAURANTES, linea(harina, "500", "g"));

        assertThat(repetido.estado()).isEqualTo(EstadoDeConsumo.YA_REVERTIDO);
        assertThat(registrarOtraVez.estado()).isEqualTo(EstadoDeConsumo.YA_REGISTRADO);
        assertThat(movimientosTotales()).isEqualTo(movimientos);
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("K-04: revertir sin consumo previo deja la marca; el consumo que llega despues se omite")
    void revertir_antesDelConsumo() {
        var movimientos = movimientosTotales();

        var marca = e.api.revertirConsumo(e.ctxA, RESTAURANTES, venta);
        var repetida = e.api.revertirConsumo(e.ctxA, RESTAURANTES, venta);
        var tarde = consumir(venta, RESTAURANTES, linea(harina, "500", "g"));

        assertThat(marca.estado()).isEqualTo(EstadoDeConsumo.MARCADO_ANULADO_PREVIO);
        assertThat(marca.movimientos()).isEmpty();
        assertThat(repetida.estado()).isEqualTo(EstadoDeConsumo.MARCADO_ANULADO_PREVIO);
        assertThat(tarde.estado()).isEqualTo(EstadoDeConsumo.OMITIDO_ANULADO_PREVIO);
        assertThat(tarde.movimientos()).isEmpty();
        assertThat(movimientosTotales()).isEqualTo(movimientos);
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("la marca de anulacion previa es de un solo negocio y consumidor")
    void revertir_marcaAcotada() {
        e.api.revertirConsumo(e.ctxA, RESTAURANTES, venta);

        var otroConsumidor = consumir(venta, "vinculo", linea(harina, "100", "g"));

        assertThat(otroConsumidor.estado()).isEqualTo(EstadoDeConsumo.REGISTRADO);
    }

    @Test
    @DisplayName("revertir un consumo cuyo articulo ya se dio de baja falla con ARTICULO_INACTIVO y se puede reintentar")
    void revertir_articuloInactivo() {
        var sal = e.crear(e.ctxA, "Sal", "g", null, null, null);
        e.entrada(e.ctxA, sal, "100", "g", "1");
        consumir(venta, RESTAURANTES, linea(sal, "100", "g"));
        e.confirmar(e.ctxA, new BajaArticuloComando(sal));

        fallaCon(() -> e.api.revertirConsumo(e.ctxA, RESTAURANTES, venta), CodigoError.ARTICULO_INACTIVO);

        assertThat(consumir(venta, RESTAURANTES, linea(sal, "100", "g")).estado())
                .isEqualTo(EstadoDeConsumo.YA_REGISTRADO);
    }

    @Test
    @DisplayName("un consumo de un negocio no se puede revertir desde otro")
    void revertir_otroNegocio() {
        consumir(venta, RESTAURANTES, linea(harina, "500", "g"));

        var desdeB = e.api.revertirConsumo(e.ctxB, RESTAURANTES, venta);

        assertThat(desdeB.estado()).isEqualTo(EstadoDeConsumo.MARCADO_ANULADO_PREVIO);
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("9500");
    }
}
