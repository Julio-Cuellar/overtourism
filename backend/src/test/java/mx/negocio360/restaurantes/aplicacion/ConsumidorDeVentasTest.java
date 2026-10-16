package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.dominio.ModoInventario;
import mx.negocio360.restaurantes.infraestructura.ConsumosEnMemoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * R3 · CM-01 a CM-08: lo que pasa con los insumos cuando Ventas registra o anula una venta. El consumo lo hace Inventario, de
 * forma atomica e idempotente; Restaurantes arma las lineas con la receta que estaba vigente al momento de la venta.
 */
class ConsumidorDeVentasTest {

    private final EntornoDeRecetas e = new EntornoDeRecetas();
    private final ConsumosEnMemoria consumos = new ConsumosEnMemoria();
    private final List<SobreEvento> publicados = new ArrayList<>();
    private final ConsumidorDeVentas consumidor = new ConsumidorDeVentas(e.menuAlmacen, e.recetasAlmacen, e.inventario,
            consumos, publicados::add, e.reloj);

    private final UUID pan = e.inventario.articulo("Pan de cemita", "pieza", "6.00");
    private final UUID milanesa = e.inventario.articulo("Milanesa", "g", "0.18");
    private final UUID aguacate = e.inventario.articulo("Aguacate", "g", "0.07");
    private final UUID pollo = e.inventario.articulo("Pollo", "g", "0.09");
    private final PlatilloVista cemita = platillo("Cemita poblana", 1, ins(pan, "1", "pieza", "0"), ins(milanesa, "120", "g", "5"),
            ins(aguacate, "60", "g", "10"));
    private final PlatilloVista mole = platillo("Mole poblano", 40, ins(pollo, "4", "kg", "0"));

    private static InsumoComando ins(UUID articulo, String cantidad, String unidad, String merma) {
        return new InsumoComando(articulo, new BigDecimal(cantidad), unidad, new BigDecimal(merma));
    }

    private PlatilloVista platillo(String nombre, int rendimiento, InsumoComando... insumos) {
        var p = e.menu.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo(nombre, null, new BigDecimal("85"), null, Set.of(), null));
        var token = e.preparacion.preparar(e.ctxA, new GuardarRecetaComando(p.id(), rendimiento, List.of(insumos))).token();
        e.preparacion.confirmar(e.ctxA, token);
        return e.menu.cambiarModo(e.ctxA, e.todo, p.id(), ModoInventario.RECETA);
    }

    private static SobreEvento registrada(UUID negocio, UUID venta, Instant cuando, Object[]... lineas) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("venta_id", venta.toString());
        datos.put("turno_id", UUID.randomUUID().toString());
        datos.put("total", "100.00");
        var ls = new ArrayList<Object>();
        int n = 1;
        for (var l : lineas) {
            ls.add(Map.of("linea", n++, "producto_id", l[0].toString(), "cantidad", l[1].toString(), "unidad", "pieza",
                    "precio_unitario", "10.0000", "importe", "10.00"));
        }
        datos.put("lineas", ls);
        datos.put("campo_futuro", "ignorado");
        return new SobreEvento(UuidV7.generar(cuando), "venta.registrada", 1, negocio, "ventas", cuando, venta, null, datos);
    }

    private static SobreEvento anulada(UUID negocio, UUID venta, Instant cuando, UUID... productos) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("venta_id", venta.toString());
        datos.put("turno_id", UUID.randomUUID().toString());
        var ls = new ArrayList<Object>();
        int n = 1;
        for (var p : productos) {
            ls.add(Map.of("linea", n++, "producto_id", p.toString()));
        }
        datos.put("lineas", ls);
        return new SobreEvento(UuidV7.generar(cuando), "venta.anulada", 1, negocio, "ventas", cuando, venta, null, datos);
    }

    private SobreEvento venta(UUID venta, Object[]... lineas) {
        return registrada(EntornoDeRecetas.NEGOCIO_A, venta, EntornoDeRecetas.AHORA, lineas);
    }

    private static Object[] linea(UUID producto, String cantidad) {
        return new Object[] {producto, cantidad};
    }

    private Map<UUID, BigDecimal> consumido(int indice) {
        var m = new LinkedHashMap<UUID, BigDecimal>();
        e.inventario.consumos.get(indice).lineas().forEach(l -> m.put(l.articuloId(), l.cantidad()));
        return m;
    }

    // ---- CM-01 y CM-02 ----

    @Test
    @DisplayName("CM-01 y CM-02: dos cemitas descuentan los insumos de la receta con merma, en una sola llamada con consumidor restaurantes y la venta como causa")
    void registrada_descuentaLaReceta() {
        var ventaId = UUID.randomUUID();

        var resultado = consumidor.procesar(venta(ventaId, linea(cemita.id(), "2")));

        assertThat(resultado).isEqualTo(ResultadoConsumo.APLICADO);
        assertThat(e.inventario.llamadas.get("registrarConsumo")).isEqualTo(1);
        var consumo = e.inventario.consumos.get(0);
        assertThat(consumo.consumidor()).isEqualTo("restaurantes");
        assertThat(consumo.causaId()).isEqualTo(ventaId);
        assertThat(consumo.ocurridoEn()).isEqualTo(EntornoDeRecetas.AHORA);
        assertThat(consumo.negocioId()).isEqualTo(EntornoDeRecetas.NEGOCIO_A);
        assertThat(consumido(0)).containsOnlyKeys(pan, milanesa, aguacate);
        assertThat(consumido(0).get(pan)).isEqualByComparingTo("2");
        assertThat(consumido(0).get(milanesa)).isEqualByComparingTo("252");
        assertThat(consumido(0).get(aguacate)).isEqualByComparingTo("132");
        assertThat(e.inventario.consumos.get(0).lineas()).extracting(LineaDeConsumo::unidad).containsExactly("pieza", "g", "g");
    }

    @Test
    @DisplayName("CM-02: dos lineas del mismo platillo se suman en una sola linea por articulo")
    void registrada_sumaPorArticulo() {
        consumidor.procesar(venta(UUID.randomUUID(), linea(cemita.id(), "2"), linea(cemita.id(), "1")));

        assertThat(e.inventario.consumos).hasSize(1);
        assertThat(e.inventario.consumos.get(0).lineas()).hasSize(3);
        assertThat(consumido(0).get(milanesa)).isEqualByComparingTo("378");
    }

    @Test
    @DisplayName("CM-02: el rendimiento reparte la receta: una olla de mole de 40 porciones, dos moles gastan 200 g de pollo")
    void registrada_conRendimiento() {
        consumidor.procesar(venta(UUID.randomUUID(), linea(mole.id(), "2")));

        assertThat(consumido(0).get(pollo)).isEqualByComparingTo("200");
    }

    @Test
    @DisplayName("CM-05: se redondea una sola vez por articulo, no por linea: tres porciones de una receta para 3 gastan exactamente 1 g")
    void registrada_redondeoUnico() {
        var sal = e.inventario.articulo("Sal", "g", "0.01");
        var sopa = platillo("Sopa", 3, ins(sal, "1", "g", "0"));

        consumidor.procesar(venta(UUID.randomUUID(), linea(sopa.id(), "1"), linea(sopa.id(), "1"), linea(sopa.id(), "1")));

        assertThat(consumido(0).get(sal)).isEqualByComparingTo("1.0000");
    }

    @Test
    @DisplayName("CM-05: 2 porciones de 3 dan 0.6667 con cuatro decimales, HALF_UP")
    void registrada_cuatroDecimales() {
        var sal = e.inventario.articulo("Sal fina", "g", "0.01");
        var sopa = platillo("Sopa fina", 3, ins(sal, "1", "g", "0"));

        consumidor.procesar(venta(UUID.randomUUID(), linea(sopa.id(), "2")));

        assertThat(consumido(0).get(sal)).isEqualByComparingTo("0.6667");
    }

    @Test
    @DisplayName("CM-01: solo se procesan platillos de este negocio en modo RECETA; lo ajeno, lo que no es receta y lo desconocido se ignora sin llamar a Inventario")
    void registrada_ignoraLoAjeno() {
        var vinculo = e.menu.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Refresco", null, BigDecimal.TEN, null, Set.of(), null));
        e.menu.cambiarModo(e.ctxA, e.todo, vinculo.id(), ModoInventario.VINCULO_DIRECTO);

        assertThat(consumidor.procesar(venta(UUID.randomUUID(), linea(vinculo.id(), "1"), linea(UUID.randomUUID(), "3")))).isEqualTo(ResultadoConsumo.IGNORADO);
        assertThat(e.inventario.llamadas).doesNotContainKey("registrarConsumo");
        var deB = e.menu.crearPlatillo(e.ctxB, e.todo, new DatosPlatillo("Cemita poblana", null, BigDecimal.TEN, null, Set.of(), null));
        assertThat(consumidor.procesar(registrada(EntornoDeRecetas.NEGOCIO_B, UUID.randomUUID(), EntornoDeRecetas.AHORA, linea(cemita.id(), "1"))))
                .as("el platillo de A no es de B").isEqualTo(ResultadoConsumo.IGNORADO);
        assertThat(deB).isNotNull();
        assertThat(e.inventario.consumos).isEmpty();
    }

    @Test
    @DisplayName("CM-01: en una venta mezclada solo se descuenta lo de la receta")
    void registrada_ventaMezclada() {
        consumidor.procesar(venta(UUID.randomUUID(), linea(UUID.randomUUID(), "5"), linea(cemita.id(), "1")));

        assertThat(consumido(0)).containsOnlyKeys(pan, milanesa, aguacate);
    }

    // ---- CM-03 ----

    @Test
    @DisplayName("CM-03: el mismo evento dos veces deja un solo descuento")
    void idempotente_mismoEvento() {
        var evento = venta(UUID.randomUUID(), linea(cemita.id(), "1"));

        assertThat(consumidor.procesar(evento)).isEqualTo(ResultadoConsumo.APLICADO);
        assertThat(consumidor.procesar(evento)).isEqualTo(ResultadoConsumo.YA_PROCESADO);

        assertThat(e.inventario.consumos).hasSize(1);
        assertThat(e.inventario.llamadas.get("registrarConsumo")).isEqualTo(1);
    }

    @Test
    @DisplayName("CM-03: la misma venta en otro evento tampoco descuenta dos veces: Inventario responde YA_REGISTRADO")
    void idempotente_mismaVentaOtroEvento() {
        var ventaId = UUID.randomUUID();
        consumidor.procesar(venta(ventaId, linea(cemita.id(), "1")));

        var resultado = consumidor.procesar(venta(ventaId, linea(cemita.id(), "1")));

        assertThat(resultado).isEqualTo(ResultadoConsumo.YA_APLICADA);
        assertThat(e.inventario.consumos).hasSize(1);
    }

    // ---- CM-04 ----

    @Test
    @DisplayName("CM-04: anular una venta revierte el consumo con la causa de la venta")
    void anulada_revierte() {
        var ventaId = UUID.randomUUID();
        consumidor.procesar(venta(ventaId, linea(cemita.id(), "1")));

        var resultado = consumidor.procesar(anulada(EntornoDeRecetas.NEGOCIO_A, ventaId, EntornoDeRecetas.AHORA, cemita.id()));

        assertThat(resultado).isEqualTo(ResultadoConsumo.REVERTIDA);
        assertThat(e.inventario.consumos).isEmpty();
        assertThat(e.inventario.revertidas).containsExactly(ventaId);
    }

    @Test
    @DisplayName("CM-04: si la anulacion llega antes que la venta queda la marca, y la venta que llega despues se omite")
    void anulada_antesDeLaVenta() {
        var ventaId = UUID.randomUUID();

        assertThat(consumidor.procesar(anulada(EntornoDeRecetas.NEGOCIO_A, ventaId, EntornoDeRecetas.AHORA, cemita.id()))).isEqualTo(ResultadoConsumo.MARCADA_ANULADO_PREVIO);
        assertThat(consumidor.procesar(venta(ventaId, linea(cemita.id(), "1")))).isEqualTo(ResultadoConsumo.OMITIDA_ANULADO_PREVIO);

        assertThat(e.inventario.consumos).isEmpty();
    }

    @Test
    @DisplayName("CM-04: una anulacion de una venta sin platillos de receta no se manda a Inventario")
    void anulada_ajena() {
        var resultado = consumidor.procesar(anulada(EntornoDeRecetas.NEGOCIO_A, UUID.randomUUID(), EntornoDeRecetas.AHORA, UUID.randomUUID()));

        assertThat(resultado).isEqualTo(ResultadoConsumo.IGNORADO);
        assertThat(e.inventario.llamadas).doesNotContainKey("revertirConsumo");
    }

    @Test
    @DisplayName("CM-03 y CM-04: la anulacion repetida se reconoce, con el mismo evento y con otro")
    void anulada_idempotente() {
        var ventaId = UUID.randomUUID();
        consumidor.procesar(venta(ventaId, linea(cemita.id(), "1")));
        var anulacion = anulada(EntornoDeRecetas.NEGOCIO_A, ventaId, EntornoDeRecetas.AHORA, cemita.id());
        consumidor.procesar(anulacion);

        assertThat(consumidor.procesar(anulacion)).isEqualTo(ResultadoConsumo.YA_PROCESADO);
        assertThat(consumidor.procesar(anulada(EntornoDeRecetas.NEGOCIO_A, ventaId, EntornoDeRecetas.AHORA, cemita.id()))).isEqualTo(ResultadoConsumo.YA_REVERTIDA);
    }

    // ---- RC-07 ----

    @Test
    @DisplayName("RC-07: una venta usa la version de la receta que estaba vigente cuando ocurrio, aunque el evento se procese despues del cambio")
    void usaLaVersionDelMomentoDeLaVenta() {
        var antes = e.reloj.instant().plusSeconds(1);
        e.reloj.avanzar(Duration.ofHours(1));
        var token = e.preparacion.preparar(e.ctxA, new GuardarRecetaComando(cemita.id(), 1, List.of(ins(pan, "2", "pieza", "0")))).token();
        e.preparacion.confirmar(e.ctxA, token);

        consumidor.procesar(registrada(EntornoDeRecetas.NEGOCIO_A, UUID.randomUUID(), antes, linea(cemita.id(), "1")));

        assertThat(consumido(0)).as("la version 1 tenia tres insumos").containsOnlyKeys(pan, milanesa, aguacate);
        assertThat(consumido(0).get(pan)).isEqualByComparingTo("1");
        consumidor.procesar(registrada(EntornoDeRecetas.NEGOCIO_A, UUID.randomUUID(), e.reloj.instant(), linea(cemita.id(), "1")));
        assertThat(consumido(1)).as("la version 2 solo lleva pan").containsOnlyKeys(pan);
        assertThat(consumido(1).get(pan)).isEqualByComparingTo("2");
    }

    // ---- CM-06 ----

    @Test
    @DisplayName("CM-06: un error permanente de Inventario se guarda como consumo fallido, avisa con un evento y no se reintenta")
    void fallo_permanente() {
        e.inventario.fallaAlConsumir = new FalloDeInventario("ARTICULO_INACTIVO", false);
        var ventaId = UUID.randomUUID();
        var evento = venta(ventaId, linea(cemita.id(), "1"));

        assertThat(consumidor.procesar(evento)).isEqualTo(ResultadoConsumo.FALLIDA);

        var fallidos = consumos.fallidos(EntornoDeRecetas.NEGOCIO_A);
        assertThat(fallidos).hasSize(1);
        assertThat(fallidos.get(0).ventaId()).isEqualTo(ventaId);
        assertThat(fallidos.get(0).codigo()).isEqualTo("ARTICULO_INACTIVO");
        assertThat(fallidos.get(0).lineas()).extracting(LineaFallida::platilloId).containsExactly(cemita.id());
        assertThat(publicados).hasSize(1);
        assertThat(publicados.get(0).tipo()).isEqualTo("restaurantes.consumo_fallido");
        assertThat(publicados.get(0).origen()).isEqualTo("restaurantes");
        assertThat(publicados.get(0).negocioId()).isEqualTo(EntornoDeRecetas.NEGOCIO_A);
        assertThat(publicados.get(0).correlacionId()).isEqualTo(ventaId);
        assertThat(publicados.get(0).datos()).containsOnlyKeys("venta_id", "codigo").containsEntry("codigo", "ARTICULO_INACTIVO");
        e.inventario.fallaAlConsumir = null;
        assertThat(consumidor.procesar(evento)).as("no se reintenta: el evento quedo procesado").isEqualTo(ResultadoConsumo.YA_PROCESADO);
        assertThat(consumos.fallidos(EntornoDeRecetas.NEGOCIO_A)).hasSize(1);
    }

    @Test
    @DisplayName("CM-06: un error reintentable no se marca como procesado: el mismo evento se puede volver a procesar cuando Inventario responde")
    void fallo_reintentable() {
        e.inventario.fallaAlConsumir = new FalloDeInventario("ERROR_TEMPORAL", true);
        var evento = venta(UUID.randomUUID(), linea(cemita.id(), "1"));

        assertThat(consumidor.procesar(evento)).isEqualTo(ResultadoConsumo.REINTENTABLE);
        assertThat(consumos.fallidos(EntornoDeRecetas.NEGOCIO_A)).isEmpty();
        assertThat(publicados).isEmpty();

        e.inventario.fallaAlConsumir = null;
        assertThat(consumidor.procesar(evento)).isEqualTo(ResultadoConsumo.APLICADO);
        assertThat(e.inventario.consumos).hasSize(1);
    }

    @Test
    @DisplayName("CM-06: un platillo en modo RECETA sin receta vigente en ese momento falla con SIN_RECETA, y lo demas de la venta si se descuenta")
    void fallo_sinReceta() {
        var token = e.preparacion.preparar(e.ctxA, new GuardarRecetaComando(mole.id(), 40, List.of(ins(pollo, "4", "kg", "0")))).token();
        e.preparacion.confirmar(e.ctxA, token);
        var sinReceta = e.menu.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Nuevo platillo", null, BigDecimal.TEN, null, Set.of(), null));
        e.recetasAlmacen.insertar(mx.negocio360.restaurantes.dominio.Receta.nueva(UUID.randomUUID(), EntornoDeRecetas.NEGOCIO_A, sinReceta.id(), 1,
                List.of(new mx.negocio360.restaurantes.dominio.InsumoDeReceta(pan, BigDecimal.ONE, "pieza", BigDecimal.ZERO)),
                "ana", e.reloj.instant().plusSeconds(3600)));
        e.menu.cambiarModo(e.ctxA, e.todo, sinReceta.id(), ModoInventario.RECETA);
        var ventaId = UUID.randomUUID();

        var resultado = consumidor.procesar(venta(ventaId, linea(cemita.id(), "1"), linea(sinReceta.id(), "1")));

        assertThat(resultado).isEqualTo(ResultadoConsumo.APLICADA_CON_FALLOS);
        assertThat(e.inventario.consumos).hasSize(1);
        assertThat(consumos.fallidos(EntornoDeRecetas.NEGOCIO_A)).singleElement().satisfies(f -> {
            assertThat(f.codigo()).isEqualTo("SIN_RECETA");
            assertThat(f.lineas()).extracting(LineaFallida::platilloId).containsExactly(sinReceta.id());
        });
        assertThat(publicados).hasSize(1);
    }

    @Test
    @DisplayName("CM-06: una unidad que ya no se convierte falla al armar las lineas con UNIDAD_NO_CONVERTIBLE y no llama a registrarConsumo")
    void fallo_alConvertir() {
        e.inventario.reemplazar(new InventarioDePrueba.Articulo(milanesa, "Milanesa", "ml", new BigDecimal("0.18"), true, Rastreo.NINGUNO));

        assertThat(consumidor.procesar(venta(UUID.randomUUID(), linea(cemita.id(), "1")))).isEqualTo(ResultadoConsumo.FALLIDA);

        assertThat(consumos.fallidos(EntornoDeRecetas.NEGOCIO_A).get(0).codigo()).isEqualTo("UNIDAD_NO_CONVERTIBLE");
        assertThat(e.inventario.llamadas).doesNotContainKey("registrarConsumo");
    }

    @Test
    @DisplayName("CM-08: el aviso del fallo solo lleva el id de la venta y el codigo; nunca platillos, cantidades ni datos de la venta")
    void fallo_avisoSinDatos() {
        e.inventario.fallaAlConsumir = new FalloDeInventario("ARTICULO_INACTIVO", false);

        consumidor.procesar(venta(UUID.randomUUID(), linea(cemita.id(), "7")));

        assertThat(publicados.get(0).datos().keySet()).containsExactlyInAnyOrder("venta_id", "codigo");
        assertThat(publicados.get(0).datos().toString()).doesNotContain(cemita.id().toString());
    }

    // ---- lector tolerante (R5) ----

    @Test
    @DisplayName("lector tolerante: lo que no es de Ventas, o no cumple el contrato, o no es una venta, se descarta sin lanzar ni llamar a Inventario")
    void tolerancia() {
        var ventaId = UUID.randomUUID();
        var ajeno = new SobreEvento(UuidV7.generar(EntornoDeRecetas.AHORA), "venta.registrada", 1, EntornoDeRecetas.NEGOCIO_A, "otro",
                EntornoDeRecetas.AHORA, ventaId, null, Map.of("venta_id", ventaId.toString(), "lineas", List.of()));
        var sinLineas = new SobreEvento(UuidV7.generar(EntornoDeRecetas.AHORA), "venta.registrada", 1, EntornoDeRecetas.NEGOCIO_A, "ventas",
                EntornoDeRecetas.AHORA, ventaId, null, Map.of("venta_id", ventaId.toString()));
        var cantidadMala = new SobreEvento(UuidV7.generar(EntornoDeRecetas.AHORA), "venta.registrada", 1, EntornoDeRecetas.NEGOCIO_A, "ventas",
                EntornoDeRecetas.AHORA, ventaId, null, Map.of("venta_id", ventaId.toString(), "lineas",
                List.of(Map.of("producto_id", cemita.id().toString(), "cantidad", "dos"))));
        var cantidadCero = new SobreEvento(UuidV7.generar(EntornoDeRecetas.AHORA), "venta.registrada", 1, EntornoDeRecetas.NEGOCIO_A, "ventas",
                EntornoDeRecetas.AHORA, ventaId, null, Map.of("venta_id", ventaId.toString(), "lineas",
                List.of(Map.of("producto_id", cemita.id().toString(), "cantidad", "0"))));
        var otroTipo = new SobreEvento(UuidV7.generar(EntornoDeRecetas.AHORA), "caja.turno_abierto", 1, EntornoDeRecetas.NEGOCIO_A, "caja",
                EntornoDeRecetas.AHORA, ventaId, null, Map.of("turno_id", "x"));

        assertThat(consumidor.procesar(ajeno)).isEqualTo(ResultadoConsumo.DATOS_INVALIDOS);
        assertThat(consumidor.procesar(sinLineas)).isEqualTo(ResultadoConsumo.DATOS_INVALIDOS);
        assertThat(consumidor.procesar(cantidadMala)).isEqualTo(ResultadoConsumo.DATOS_INVALIDOS);
        assertThat(consumidor.procesar(cantidadCero)).isEqualTo(ResultadoConsumo.DATOS_INVALIDOS);
        assertThat(consumidor.procesar(otroTipo)).isEqualTo(ResultadoConsumo.IGNORADO);
        assertThat(e.inventario.llamadas).doesNotContainKeys("registrarConsumo", "revertirConsumo");
    }

    @Test
    @DisplayName("un evento mal formado no se marca como procesado: queda para revision")
    void tolerancia_noSeMarca() {
        var ventaId = UUID.randomUUID();
        var malo = new SobreEvento(UuidV7.generar(EntornoDeRecetas.AHORA), "venta.registrada", 1, EntornoDeRecetas.NEGOCIO_A, "ventas",
                EntornoDeRecetas.AHORA, ventaId, null, Map.of("venta_id", ventaId.toString()));

        consumidor.procesar(malo);

        assertThat(consumos.yaProcesado("restaurantes.ventas", malo.id())).isFalse();
    }
}
