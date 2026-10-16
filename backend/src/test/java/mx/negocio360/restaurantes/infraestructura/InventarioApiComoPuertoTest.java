package mx.negocio360.restaurantes.infraestructura;

import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.ArticuloBase;
import mx.negocio360.inventario.aplicacion.BajaArticuloComando;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.fakes.Escenario;
import mx.negocio360.restaurantes.aplicacion.FalloDeInventario;
import mx.negocio360.restaurantes.aplicacion.LineaDeConsumo;
import mx.negocio360.restaurantes.aplicacion.LineaDeInsumo;
import mx.negocio360.restaurantes.aplicacion.Rastreo;
import mx.negocio360.restaurantes.aplicacion.ResultadoDeConsumo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El adaptador que conecta {@code InventarioApi} con el puerto de Restaurantes (R-I1 a R-I4), probado contra el
 * Inventario real en memoria y, para lo que Inventario todavia no puede producir (series, errores temporales), contra un
 * doble.
 */
class InventarioApiComoPuertoTest {

    private static final String RESTAURANTES = "restaurantes";

    private final Escenario e = new Escenario();
    private final InventarioApiComoPuerto puerto = new InventarioApiComoPuerto(e.api);
    private final UUID venta = UUID.randomUUID();
    private UUID harina;

    @BeforeEach
    void harinaConExistencias() {
        harina = e.harina(e.ctxA);
        e.entrada(e.ctxA, harina, "10", "kg", "20");
    }

    private static LineaDeConsumo linea(UUID articulo, String cantidad, String unidad) {
        return new LineaDeConsumo(articulo, d(cantidad), unidad);
    }

    private static FalloDeInventario fallo(Runnable accion) {
        return catchThrowableOfType(FalloDeInventario.class, accion::run);
    }

    @Test
    @DisplayName("articulos: traduce nombre, unidad base, CPP, activo y rastreo, y omite los que no existen")
    void articulos_traduce() {
        var inactivo = e.crear(e.ctxA, "Sal", "g", null, null, null);
        e.confirmar(e.ctxA, new BajaArticuloComando(inactivo));

        var respuesta = puerto.articulos(e.ctxA, Set.of(harina, inactivo, UUID.randomUUID()));

        assertThat(respuesta).containsOnlyKeys(harina, inactivo);
        var h = respuesta.get(harina);
        assertThat(h.nombre()).isEqualTo("Harina");
        assertThat(h.unidadBase()).isEqualTo("g");
        assertThat(h.cpp()).isEqualByComparingTo("0.02");
        assertThat(h.activo()).isTrue();
        assertThat(h.rastreo()).isEqualTo(Rastreo.NINGUNO);
        assertThat(respuesta.get(inactivo).activo()).isFalse();
    }

    @Test
    @DisplayName("articulos: con mas de 100 ids los pide por tandas para no topar con el limite de Inventario")
    void articulos_porTandas() {
        var ids = new HashSet<UUID>();
        IntStream.range(0, 250).forEach(i -> ids.add(UUID.randomUUID()));
        ids.add(harina);

        assertThat(puerto.articulos(e.ctxA, ids)).containsOnlyKeys(harina);
    }

    @Test
    @DisplayName("rastreo: LOTE y SERIE de Inventario llegan como LOTE y SERIE de Restaurantes")
    void articulos_traduceRastreo() {
        var api = mock(InventarioApi.class);
        var serie = UUID.randomUUID();
        var lote = UUID.randomUUID();
        when(api.articulosPorId(any(), any())).thenReturn(Map.of(
                serie, new ArticuloBase(serie, "Licuadora", "pieza", BigDecimal.ZERO, true,
                        mx.negocio360.inventario.dominio.Rastreo.SERIE),
                lote, new ArticuloBase(lote, "Queso", "g", BigDecimal.ONE, true,
                        mx.negocio360.inventario.dominio.Rastreo.LOTE)));

        var respuesta = new InventarioApiComoPuerto(api).articulos(e.ctxA, Set.of(serie, lote));

        assertThat(respuesta.get(serie).rastreo()).isEqualTo(Rastreo.SERIE);
        assertThat(respuesta.get(lote).rastreo()).isEqualTo(Rastreo.LOTE);
    }

    @Test
    @DisplayName("aBase: lleva cada cantidad a la unidad base de su articulo, en el mismo orden")
    void aBase_convierte() {
        var azucar = e.crear(e.ctxA, "Azúcar", "g", null, null, null);

        var respuesta = puerto.aBase(e.ctxA, List.of(new LineaDeInsumo(harina, d("1.5"), "kg"),
                new LineaDeInsumo(azucar, d("200"), "g")));

        assertThat(respuesta).hasSize(2);
        assertThat(respuesta.get(0).articuloId()).isEqualTo(harina);
        assertThat(respuesta.get(0).cantidadBase()).isEqualByComparingTo("1500");
        assertThat(respuesta.get(0).unidadBase()).isEqualTo("g");
        assertThat(respuesta.get(1).articuloId()).isEqualTo(azucar);
    }

    @Test
    @DisplayName("aBase: los errores de Inventario salen como FalloDeInventario permanente con su codigo")
    void aBase_errores() {
        var desconocido = fallo(() -> puerto.aBase(e.ctxA, List.of(new LineaDeInsumo(UUID.randomUUID(), d("1"), "g"))));
        var sinConversion = fallo(() -> puerto.aBase(e.ctxA, List.of(new LineaDeInsumo(harina, d("1"), "l"))));
        var invalida = fallo(() -> puerto.aBase(e.ctxA, List.of(new LineaDeInsumo(harina, d("0"), "g"))));

        assertThat(desconocido.codigo()).isEqualTo("ARTICULO_NO_ENCONTRADO");
        assertThat(sinConversion.codigo()).isEqualTo("UNIDAD_NO_CONVERTIBLE");
        assertThat(invalida.codigo()).isEqualTo("CANTIDAD_INVALIDA");
        assertThat(List.of(desconocido, sinConversion, invalida)).noneMatch(FalloDeInventario::reintentable);
    }

    @Test
    @DisplayName("registrarConsumo: descuenta, es idempotente y devuelve el estado de Inventario")
    void registrar_descuentaYEsIdempotente() {
        var ocurrido = Escenario.AHORA;

        var primero = puerto.registrarConsumo(e.ctxA, RESTAURANTES, venta, ocurrido, List.of(linea(harina, "500", "g")));
        var repetido = puerto.registrarConsumo(e.ctxA, RESTAURANTES, venta, ocurrido, List.of(linea(harina, "500", "g")));

        assertThat(primero).isEqualTo(ResultadoDeConsumo.REGISTRADO);
        assertThat(repetido).isEqualTo(ResultadoDeConsumo.YA_REGISTRADO);
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("9500");
    }

    @Test
    @DisplayName("registrarConsumo: un articulo dado de baja es un fallo permanente y no descuenta nada")
    void registrar_articuloInactivo() {
        var sal = e.crear(e.ctxA, "Sal", "g", null, null, null);
        e.confirmar(e.ctxA, new BajaArticuloComando(sal));

        var fallo = fallo(() -> puerto.registrarConsumo(e.ctxA, RESTAURANTES, venta, Escenario.AHORA,
                List.of(linea(harina, "500", "g"), linea(sal, "1", "g"))));

        assertThat(fallo.codigo()).isEqualTo("ARTICULO_INACTIVO");
        assertThat(fallo.reintentable()).isFalse();
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("registrarConsumo: una carrera por el saldo es CONCURRENCIA reintentable")
    void registrar_concurrenciaEsReintentable() {
        e.almacen.simularCarreraDeSaldo = true;

        var fallo = fallo(() -> puerto.registrarConsumo(e.ctxA, RESTAURANTES, venta, Escenario.AHORA,
                List.of(linea(harina, "500", "g"))));

        assertThat(fallo.codigo()).isEqualTo("CONCURRENCIA");
        assertThat(fallo.reintentable()).isTrue();
    }

    @Test
    @DisplayName("ERROR_TEMPORAL de Inventario tambien es reintentable")
    void errorTemporalEsReintentable() {
        var api = mock(InventarioApi.class);
        when(api.revertirConsumo(any(), any(), any())).thenThrow(new InventarioException(CodigoError.ERROR_TEMPORAL));

        var fallo = fallo(() -> new InventarioApiComoPuerto(api).revertirConsumo(e.ctxA, RESTAURANTES, venta));

        assertThat(fallo.codigo()).isEqualTo("ERROR_TEMPORAL");
        assertThat(fallo.reintentable()).isTrue();
    }

    @Test
    @DisplayName("registrarConsumo: el limite de 100 lineas y un consumidor desconocido son fallos permanentes")
    void registrar_limitesYConsumidor() {
        var muchas = IntStream.range(0, 101).mapToObj(i -> linea(harina, "1", "g")).toList();

        var demasiadas = fallo(() -> puerto.registrarConsumo(e.ctxA, RESTAURANTES, venta, Escenario.AHORA, muchas));
        var consumidor = fallo(() -> puerto.registrarConsumo(e.ctxA, "otro", venta, Escenario.AHORA,
                List.of(linea(harina, "1", "g"))));

        assertThat(demasiadas.codigo()).isEqualTo("CANTIDAD_INVALIDA");
        assertThat(demasiadas.reintentable()).isFalse();
        assertThat(consumidor.codigo()).isEqualTo("CONSUMIDOR_INVALIDO");
    }

    @Test
    @DisplayName("revertirConsumo: repone, es idempotente y deja la marca si el consumo no llego")
    void revertir_cicloCompleto() {
        puerto.registrarConsumo(e.ctxA, RESTAURANTES, venta, Escenario.AHORA, List.of(linea(harina, "500", "g")));

        var primero = puerto.revertirConsumo(e.ctxA, RESTAURANTES, venta);
        var repetido = puerto.revertirConsumo(e.ctxA, RESTAURANTES, venta);
        var otra = UUID.randomUUID();
        var marca = puerto.revertirConsumo(e.ctxA, RESTAURANTES, otra);
        var tarde = puerto.registrarConsumo(e.ctxA, RESTAURANTES, otra, Escenario.AHORA,
                List.of(linea(harina, "500", "g")));

        assertThat(primero).isEqualTo(ResultadoDeConsumo.REVERTIDO);
        assertThat(repetido).isEqualTo(ResultadoDeConsumo.YA_REVERTIDO);
        assertThat(marca).isEqualTo(ResultadoDeConsumo.MARCADO_ANULADO_PREVIO);
        assertThat(tarde).isEqualTo(ResultadoDeConsumo.OMITIDO_ANULADO_PREVIO);
        assertThat(e.vista(e.ctxA, harina).cantidadBase()).isEqualByComparingTo("10000");
    }
}
