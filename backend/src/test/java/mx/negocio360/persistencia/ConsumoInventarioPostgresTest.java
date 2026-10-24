package mx.negocio360.persistencia;

import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.CrearArticuloComando;
import mx.negocio360.inventario.aplicacion.EstadoDeConsumo;
import mx.negocio360.inventario.aplicacion.LineaDeConsumo;
import mx.negocio360.inventario.aplicacion.RegistrarEntradaComando;
import mx.negocio360.inventario.dominio.InventarioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * El consumo por venta de Inventario (K-01 a K-04) contra PostgreSQL de verdad, por la API publica con su transaccion
 * ({@code FronteraTransaccional}): lo que el almacen en memoria no puede probar, que es la idempotencia por la llave de la
 * tabla {@code consumo}, la atomicidad (K-02: todo o nada) y la carrera de varias peticiones de la misma causa.
 */
@SpringBootTest(properties = "inventario.almacen=postgres")
class ConsumoInventarioPostgresTest {

    private static final String CONSUMIDOR = "restaurantes";

    @Autowired
    private InventarioApi api;

    /** Un articulo nuevo con 2 kg (2,000 g) de existencia, en el negocio del contexto. */
    private UUID articuloConDosKilos(ContextoNegocio ctx, String nombre) {
        var articulo = api.confirmar(ctx, api.preparar(ctx, new CrearArticuloComando(nombre, null, "g", null, null,
                null)).token()).recursoId();
        api.confirmar(ctx, api.preparar(ctx, new RegistrarEntradaComando(articulo, d("2"), "kg", d("20"), null))
                .token());
        return articulo;
    }

    private BigDecimal existencia(ContextoNegocio ctx, UUID articulo) {
        return api.articulo(ctx, articulo).orElseThrow().cantidadBase();
    }

    @Test
    @DisplayName("K-01: registrar dos veces la misma causa descuenta una sola vez y la segunda responde YA_REGISTRADO")
    void idempotente() {
        var ctx = new ContextoNegocio(UUID.randomUUID(), "demo");
        var harina = articuloConDosKilos(ctx, "Harina");
        var causa = UUID.randomUUID();
        var lineas = List.of(new LineaDeConsumo(harina, d("500"), "g"));

        var primera = api.registrarConsumo(ctx, CONSUMIDOR, causa, null, lineas);
        var segunda = api.registrarConsumo(ctx, CONSUMIDOR, causa, null, lineas);

        assertThat(primera.estado()).isEqualTo(EstadoDeConsumo.REGISTRADO);
        assertThat(segunda.estado()).isEqualTo(EstadoDeConsumo.YA_REGISTRADO);
        assertThat(existencia(ctx, harina)).isEqualByComparingTo("1500");
    }

    @Test
    @DisplayName("K-03: revertir repone al costo original, una sola vez")
    void revertirUnaVez() {
        var ctx = new ContextoNegocio(UUID.randomUUID(), "demo");
        var harina = articuloConDosKilos(ctx, "Harina");
        var causa = UUID.randomUUID();
        api.registrarConsumo(ctx, CONSUMIDOR, causa, null, List.of(new LineaDeConsumo(harina, d("500"), "g")));

        var reversa = api.revertirConsumo(ctx, CONSUMIDOR, causa);
        var otraVez = api.revertirConsumo(ctx, CONSUMIDOR, causa);

        assertThat(reversa.estado()).isEqualTo(EstadoDeConsumo.REVERTIDO);
        assertThat(otraVez.estado()).isEqualTo(EstadoDeConsumo.YA_REVERTIDO);
        assertThat(existencia(ctx, harina)).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("K-04: si la anulacion llega antes, queda la marca y el consumo posterior se omite")
    void anulacionPrevia() {
        var ctx = new ContextoNegocio(UUID.randomUUID(), "demo");
        var harina = articuloConDosKilos(ctx, "Harina");
        var causa = UUID.randomUUID();

        var marca = api.revertirConsumo(ctx, CONSUMIDOR, causa);
        var consumo = api.registrarConsumo(ctx, CONSUMIDOR, causa, null,
                List.of(new LineaDeConsumo(harina, d("500"), "g")));

        assertThat(marca.estado()).isEqualTo(EstadoDeConsumo.MARCADO_ANULADO_PREVIO);
        assertThat(consumo.estado()).isEqualTo(EstadoDeConsumo.OMITIDO_ANULADO_PREVIO);
        assertThat(existencia(ctx, harina)).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("K-02: si una linea falla no se descuenta ninguna ni queda la fila del consumo")
    void atomico() {
        var ctx = new ContextoNegocio(UUID.randomUUID(), "demo");
        var harina = articuloConDosKilos(ctx, "Harina");
        var causa = UUID.randomUUID();
        var inexistente = UUID.randomUUID();

        assertThatThrownBy(() -> api.registrarConsumo(ctx, CONSUMIDOR, causa, null,
                List.of(new LineaDeConsumo(harina, d("500"), "g"), new LineaDeConsumo(inexistente, d("1"), "g"))))
                .isInstanceOf(InventarioException.class);

        assertThat(existencia(ctx, harina)).isEqualByComparingTo("2000");
        // Si hubiera quedado la fila, esto responderia YA_REGISTRADO.
        var despues = api.registrarConsumo(ctx, CONSUMIDOR, causa, null,
                List.of(new LineaDeConsumo(harina, d("500"), "g")));
        assertThat(despues.estado()).isEqualTo(EstadoDeConsumo.REGISTRADO);
    }

    @Test
    @DisplayName("K-01: cuatro peticiones simultaneas de la misma causa descuentan exactamente una vez")
    void carrera() throws Exception {
        var ctx = new ContextoNegocio(UUID.randomUUID(), "demo");
        var harina = articuloConDosKilos(ctx, "Harina");
        var causa = UUID.randomUUID();
        var lineas = List.of(new LineaDeConsumo(harina, d("500"), "g"));
        var hilos = Executors.newFixedThreadPool(4);
        var salida = new CountDownLatch(1);
        var tareas = new ArrayList<Future<EstadoDeConsumo>>();

        for (var i = 0; i < 4; i++) {
            tareas.add(hilos.submit(() -> {
                salida.await();
                try {
                    return api.registrarConsumo(ctx, CONSUMIDOR, causa, null, lineas).estado();
                } catch (InventarioException perdioLaCarrera) {
                    return null;
                }
            }));
        }
        salida.countDown();
        var resultados = new ArrayList<EstadoDeConsumo>();
        for (var tarea : tareas) {
            resultados.add(tarea.get());
        }
        hilos.shutdown();
        // Quien perdio la carrera reintenta: el estado final es estable.
        resultados.add(api.registrarConsumo(ctx, CONSUMIDOR, causa, null, lineas).estado());

        assertThat(resultados.stream().filter(e -> e == EstadoDeConsumo.REGISTRADO)).hasSize(1);
        assertThat(existencia(ctx, harina)).isEqualByComparingTo("1500");
    }
}
