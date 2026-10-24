package mx.negocio360.inventario.infraestructura;

import static mx.negocio360.inventario.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.inventario.aplicacion.Preparacion;
import mx.negocio360.inventario.dominio.Articulo;
import mx.negocio360.inventario.dominio.CausaTipo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.Consumo;
import mx.negocio360.inventario.dominio.MotivoMerma;
import mx.negocio360.inventario.dominio.Movimiento;
import mx.negocio360.inventario.dominio.Saldo;
import mx.negocio360.inventario.dominio.TipoMovimiento;
import mx.negocio360.inventario.dominio.UnidadEstandar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Los almacenes en memoria de desarrollo: aislamiento por negocio, bloqueo optimista y escritura unica. */
class InventarioEnMemoriaTest {

    private static final Instant T = Instant.parse("2026-10-16T12:00:00Z");

    private final InventarioEnMemoria almacen = new InventarioEnMemoria();
    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();

    private int creados;

    /** Cada articulo nace un milisegundo despues del anterior: los UUIDv7 del mismo milisegundo no tienen orden. */
    private Articulo articulo(UUID negocio, String nombre) {
        return Articulo.nuevo(UuidV7.generar(T.plusMillis(creados++)), negocio, nombre, null, UnidadEstandar.G, Map.of(), BigDecimal.ZERO,
                "yo", T);
    }

    @Test
    @DisplayName("articulos: un negocio no ve los de otro, el nombre activo se busca sin importar mayusculas y la version protege")
    void articulos() {
        var harina = articulo(negocioA, "Harina");
        almacen.insertar(harina);

        assertThat(almacen.porId(negocioA, harina.id())).contains(harina);
        assertThat(almacen.porId(negocioB, harina.id())).isEmpty();
        assertThat(almacen.activoPorNombre(negocioA, "HARINA")).contains(harina);
        assertThat(almacen.activoPorNombre(negocioB, "harina")).isEmpty();
        fallaCon(() -> almacen.actualizar(harina.dadoDeBaja(T), 7L), CodigoError.CONCURRENCIA);
        almacen.actualizar(harina.dadoDeBaja(T), harina.version());
        assertThat(almacen.activoPorNombre(negocioA, "Harina")).isEmpty();
    }

    @Test
    @DisplayName("buscar: filtra por negocio, texto y alertas, ordena por id y pagina por cursor")
    void buscar() {
        var a1 = articulo(negocioA, "Harina");
        var a2 = articulo(negocioA, "Harina integral");
        var a3 = articulo(negocioA, "Azúcar");
        var ajeno = articulo(negocioB, "Harina");
        List.of(a1, a2, a3, ajeno).forEach(almacen::insertar);
        almacen.insertar(new Saldo(a3.id(), negocioA, new BigDecimal("-1"), BigDecimal.ONE, null, 1));

        var porTexto = almacen.buscar(negocioA, "harina", false, null, 10);
        var alertas = almacen.buscar(negocioA, null, true, null, 10);
        var pagina = almacen.buscar(negocioA, null, false, a1.id(), 10);

        assertThat(porTexto).extracting(f -> f.articulo().id()).containsExactly(a1.id(), a2.id());
        assertThat(alertas).extracting(f -> f.articulo().id()).containsExactly(a3.id());
        assertThat(pagina).extracting(f -> f.articulo().id()).containsExactly(a2.id(), a3.id());
        assertThat(almacen.buscar(negocioA, null, false, null, 2)).hasSize(2);
    }

    @Test
    @DisplayName("saldos: aislados por negocio y con bloqueo optimista por version")
    void saldos() {
        var articulo = articulo(negocioA, "Sal");
        var inicial = Saldo.inicial(articulo.id(), negocioA);
        almacen.insertar(inicial);

        assertThat(almacen.porArticulo(negocioA, articulo.id())).contains(inicial);
        assertThat(almacen.porArticulo(negocioB, articulo.id())).isEmpty();
        fallaCon(() -> almacen.actualizar(inicial.conSalida(BigDecimal.ONE, UUID.randomUUID()), 9L),
                CodigoError.CONCURRENCIA);
        almacen.actualizar(inicial.conSalida(BigDecimal.ONE, UUID.randomUUID()), inicial.version());
        assertThat(almacen.porArticulo(negocioA, articulo.id()).orElseThrow().cantidadBase())
                .isEqualByComparingTo("-1");
    }

    @Test
    @DisplayName("kardex: solo del articulo y del negocio, en orden de id y desde el cursor")
    void kardex() {
        var articulo = articulo(negocioA, "Sal");
        var ids = IntStream.range(0, 3).mapToObj(i -> UuidV7.generar(T.plusMillis(i))).toList();
        for (var id : ids) {
            almacen.insertar(new Movimiento(id, negocioA, articulo.id(), TipoMovimiento.MERMA, BigDecimal.ONE, "g",
                    BigDecimal.ONE, BigDecimal.ONE.negate(), BigDecimal.ONE, BigDecimal.ONE.negate(), BigDecimal.ONE,
                    BigDecimal.ZERO, CausaTipo.MERMA, null, MotivoMerma.CADUCIDAD, T, T, "yo"));
        }

        assertThat(almacen.kardex(negocioA, articulo.id(), null, 10)).extracting(Movimiento::id).containsExactlyElementsOf(ids);
        assertThat(almacen.kardex(negocioA, articulo.id(), ids.get(0), 1)).extracting(Movimiento::id).containsExactly(ids.get(1));
        assertThat(almacen.kardex(negocioB, articulo.id(), null, 10)).isEmpty();
    }

    @Test
    @DisplayName("consumos: una fila por llave; insertar repetido o actualizar una inexistente es CONCURRENCIA")
    void consumos() {
        var causa = UUID.randomUUID();
        var marca = Consumo.marcaDeAnulacion(negocioA, causa, "restaurantes", T);

        almacen.insertar(marca);

        assertThat(almacen.porLlave(negocioA, causa, "restaurantes")).contains(marca);
        assertThat(almacen.porLlave(negocioB, causa, "restaurantes")).isEmpty();
        assertThat(almacen.porLlave(negocioA, causa, "vinculo")).isEmpty();
        fallaCon(() -> almacen.insertar(marca), CodigoError.CONCURRENCIA);
        fallaCon(() -> almacen.actualizar(Consumo.marcaDeAnulacion(negocioA, causa, "vinculo", T)),
                CodigoError.CONCURRENCIA);
    }

    @Test
    @DisplayName("preparaciones: un token se marca como usado una sola vez, aunque lleguen dos confirmaciones a la vez")
    void preparaciones() throws Exception {
        var preparaciones = new PreparacionesEnMemoria();
        var preparacion = new Preparacion("hash", negocioA, "yo", "tipo", "h", "{}", "resumen", T.plusSeconds(300), null);
        preparaciones.guardar(preparacion);
        var gana = new AtomicInteger();
        var salida = new CountDownLatch(1);
        var hilos = Executors.newFixedThreadPool(8);

        var tareas = new ArrayList<java.util.concurrent.Future<?>>();
        for (var i = 0; i < 8; i++) {
            tareas.add(hilos.submit(() -> {
                salida.await();
                if (preparaciones.marcarUsada(negocioA, "hash", T)) {
                    gana.incrementAndGet();
                }
                return null;
            }));
        }
        salida.countDown();
        for (var tarea : tareas) {
            tarea.get();
        }
        hilos.shutdown();

        assertThat(gana).hasValue(1);
        assertThat(preparaciones.porHash(negocioA, "hash")).map(Preparacion::usadoEn).contains(T);
        assertThat(preparaciones.marcarUsada(negocioA, "no existe", T)).isFalse();
        assertThat(preparaciones.porHash(negocioA, "no existe")).isEmpty();
    }
}
