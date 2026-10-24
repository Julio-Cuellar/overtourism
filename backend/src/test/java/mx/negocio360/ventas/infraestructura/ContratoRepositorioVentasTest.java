package mx.negocio360.ventas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.ventas.FiltroVentas;
import mx.negocio360.ventas.aplicacion.puertos.RepositorioVentas;
import mx.negocio360.ventas.dominio.EstadoVenta;
import mx.negocio360.ventas.dominio.LineaVenta;
import mx.negocio360.ventas.dominio.MetodoPago;
import mx.negocio360.ventas.dominio.PagoVenta;
import mx.negocio360.ventas.dominio.Venta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato de {@link RepositorioVentas}: lo que debe cumplir cualquier almacen de ventas, en memoria o en PostgreSQL.
 * Cada subclase aporta el almacen; la bateria es la misma (F6). Cada prueba usa su propio negocio.
 */
public abstract class ContratoRepositorioVentasTest {

    protected static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");

    /** El almacen bajo prueba; cada llamada devuelve el mismo. */
    protected abstract RepositorioVentas almacen();

    /** Venta con dos lineas y dos pagos (efectivo con cambio y tarjeta), sin cliente ni cupon. */
    public static Venta venta(UUID negocio, UUID turno, String serie, long folio, Instant cuando) {
        var lineas = List.of(
                new LineaVenta(1, UUID.randomUUID(), "Boleto general", new BigDecimal("2.0000"), "pieza",
                        new BigDecimal("45.0000"), new BigDecimal("90.00"), null),
                new LineaVenta(2, UUID.randomUUID(), "Camote poblano", new BigDecimal("0.5000"), "kg",
                        new BigDecimal("120.0000"), new BigDecimal("60.00"), "SN-0007"));
        var pagos = List.of(
                new PagoVenta(MetodoPago.EFECTIVO, new BigDecimal("100.00"), new BigDecimal("120.00"),
                        new BigDecimal("20.00"), null),
                new PagoVenta(MetodoPago.TARJETA, new BigDecimal("50.00"), null, null, "AUT-123456"));
        return new Venta(UuidV7.generar(cuando), negocio, serie, folio, EstadoVenta.REGISTRADA, turno, null, null,
                new BigDecimal("150.00"), new BigDecimal("0.00"), new BigDecimal("150.00"), lineas, pagos, cuando,
                "cajero-a", null, null, null, 1);
    }

    @Test
    @DisplayName("una venta guardada se lee igual: lineas, pagos, montos y fechas")
    void guardarYLeer() {
        var negocio = UUID.randomUUID();
        var original = venta(negocio, UUID.randomUUID(), "A", 1, AHORA);

        almacen().guardar(original);

        assertThat(almacen().porId(negocio, original.id())).contains(original);
    }

    @Test
    @DisplayName("la venta con cliente, cupon y descuento se lee igual")
    void guardarYLeerConClienteYCupon() {
        var negocio = UUID.randomUUID();
        var base = venta(negocio, UUID.randomUUID(), "A", 1, AHORA);
        var cliente = UUID.randomUUID();
        var cupon = UUID.randomUUID();
        var original = new Venta(base.id(), negocio, "A", 1, EstadoVenta.REGISTRADA, base.turnoId(), cliente, cupon,
                new BigDecimal("150.00"), new BigDecimal("15.00"), new BigDecimal("135.00"), base.lineas(),
                List.of(new PagoVenta(MetodoPago.TRANSFERENCIA, new BigDecimal("135.00"), null, null, "SPEI-1")), AHORA,
                "cajero-a", null, null, null, 1);

        almacen().guardar(original);

        assertThat(almacen().porId(negocio, original.id())).contains(original);
    }

    @Test
    @DisplayName("una venta de otro negocio no se ve")
    void otroNegocioNoLaVe() {
        var negocio = UUID.randomUUID();
        var original = venta(negocio, UUID.randomUUID(), "A", 1, AHORA);
        almacen().guardar(original);

        assertThat(almacen().porId(UUID.randomUUID(), original.id())).isEmpty();
        assertThat(almacen().porId(negocio, UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("el folio empieza en 1, sube de uno en uno y es independiente por serie y por negocio")
    void foliosConsecutivos() {
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();

        assertThat(almacen().siguienteFolio(a, "A")).isEqualTo(1);
        assertThat(almacen().siguienteFolio(a, "A")).isEqualTo(2);
        assertThat(almacen().siguienteFolio(a, "B")).isEqualTo(1);
        assertThat(almacen().siguienteFolio(b, "A")).isEqualTo(1);
        assertThat(almacen().siguienteFolio(a, "A")).isEqualTo(3);
    }

    @Test
    @DisplayName("porTurno trae las registradas y las anuladas de ese turno en orden de folio, solo del negocio")
    void porTurno() {
        var negocio = UUID.randomUUID();
        var turno = UUID.randomUUID();
        var primera = venta(negocio, turno, "A", 1, AHORA);
        var segunda = venta(negocio, turno, "A", 2, AHORA.plusSeconds(60));
        almacen().guardar(segunda);
        almacen().guardar(primera);
        almacen().guardar(venta(negocio, UUID.randomUUID(), "A", 3, AHORA));
        almacen().guardar(venta(UUID.randomUUID(), turno, "A", 1, AHORA));
        assertThat(almacen().actualizar(primera.anulada(AHORA.plusSeconds(120), "gerente-a", "Error de captura"), 1))
                .isTrue();

        var delTurno = almacen().porTurno(negocio, turno);

        assertThat(delTurno).extracting(Venta::folio).containsExactly(1L, 2L);
        assertThat(delTurno).extracting(Venta::estado).containsExactly(EstadoVenta.ANULADA, EstadoVenta.REGISTRADA);
    }

    @Test
    @DisplayName("actualizar anula la venta si la version es la esperada, y la guarda con los datos de la anulacion")
    void actualizarConLaVersionEsperada() {
        var negocio = UUID.randomUUID();
        var original = venta(negocio, UUID.randomUUID(), "A", 1, AHORA);
        almacen().guardar(original);
        var anulada = original.anulada(AHORA.plusSeconds(300), "gerente-a", "Cobro duplicado");

        var guardada = almacen().actualizar(anulada, 1);

        assertThat(guardada).isTrue();
        assertThat(almacen().porId(negocio, original.id())).contains(anulada);
        assertThat(almacen().porId(negocio, original.id()).orElseThrow().version()).isEqualTo(2);
    }

    @Test
    @DisplayName("actualizar con una version vieja no cambia nada y responde falso; la segunda anulacion tambien")
    void actualizarConVersionVieja() {
        var negocio = UUID.randomUUID();
        var original = venta(negocio, UUID.randomUUID(), "A", 1, AHORA);
        almacen().guardar(original);
        var anulada = original.anulada(AHORA.plusSeconds(300), "gerente-a", "Cobro duplicado");
        assertThat(almacen().actualizar(anulada, 1)).isTrue();

        var otraAnulacion = original.anulada(AHORA.plusSeconds(400), "gerente-b", "Otra");
        assertThat(almacen().actualizar(otraAnulacion, 1)).isFalse();
        assertThat(almacen().porId(negocio, original.id())).contains(anulada);
    }

    @Test
    @DisplayName("actualizar una venta que no existe, o de otro negocio, responde falso")
    void actualizarInexistente() {
        var negocio = UUID.randomUUID();
        var existente = venta(negocio, UUID.randomUUID(), "A", 1, AHORA);
        almacen().guardar(existente);
        var ajena = new Venta(existente.id(), UUID.randomUUID(), "A", 1, EstadoVenta.ANULADA, existente.turnoId(), null,
                null, existente.subtotal(), existente.descuento(), existente.total(), existente.lineas(),
                existente.pagos(), AHORA, "cajero-a", AHORA, "g", "m", 2);

        assertThat(almacen().actualizar(venta(negocio, UUID.randomUUID(), "A", 9, AHORA).anulada(AHORA, "g", "m"), 1))
                .isFalse();
        assertThat(almacen().actualizar(ajena, 1)).isFalse();
        assertThat(almacen().porId(negocio, existente.id())).contains(existente);
    }

    /** Diez ventas: folios 1 a 10, la mitad en cada turno, las pares anuladas, una por minuto. */
    private List<Venta> sembrarDiez(UUID negocio, UUID turnoA, UUID turnoB) {
        var sembradas = new ArrayList<Venta>();
        for (int i = 1; i <= 10; i++) {
            var original = venta(negocio, i <= 5 ? turnoA : turnoB, "A", i, AHORA.plusSeconds(60L * i));
            almacen().guardar(original);
            if (i % 2 == 0) {
                var anulada = original.anulada(AHORA.plusSeconds(60L * i + 30), "gerente-a", "Prueba");
                almacen().actualizar(anulada, 1);
                sembradas.add(anulada);
            } else {
                sembradas.add(original);
            }
        }
        return sembradas;
    }

    @Test
    @DisplayName("buscar sin filtro trae todo, de la mas reciente a la mas antigua")
    void buscarSinFiltro() {
        var negocio = UUID.randomUUID();
        sembrarDiez(negocio, UUID.randomUUID(), UUID.randomUUID());
        almacen().guardar(venta(UUID.randomUUID(), UUID.randomUUID(), "A", 1, AHORA));

        var pagina = almacen().buscar(negocio, FiltroVentas.TODAS, 100, null);

        assertThat(pagina.elementos()).extracting(Venta::folio).containsExactly(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);
        assertThat(pagina.hayMas()).isFalse();
        assertThat(pagina.cursorFinal()).isNull();
    }

    @Test
    @DisplayName("buscar filtra por turno, estado, rango de fechas, serie y folio, y los filtros se combinan con Y")
    void buscarConFiltros() {
        var negocio = UUID.randomUUID();
        var turnoA = UUID.randomUUID();
        var turnoB = UUID.randomUUID();
        sembrarDiez(negocio, turnoA, turnoB);
        almacen().guardar(venta(negocio, turnoA, "B", 1, AHORA.plusSeconds(60)));

        assertThat(folios(negocio, new FiltroVentas(turnoA, null, null, null, null, null)))
                .containsExactly(5L, 4L, 3L, 2L, 1L, 1L);
        assertThat(folios(negocio, new FiltroVentas(null, EstadoVenta.ANULADA, null, null, null, null)))
                .containsExactly(10L, 8L, 6L, 4L, 2L);
        assertThat(folios(negocio, new FiltroVentas(null, null, AHORA.plusSeconds(180), AHORA.plusSeconds(300), null, null)))
                .containsExactly(5L, 4L, 3L);
        assertThat(folios(negocio, new FiltroVentas(null, null, null, null, "B", null))).containsExactly(1L);
        assertThat(folios(negocio, new FiltroVentas(null, null, null, null, "A", 7L))).containsExactly(7L);
        assertThat(folios(negocio, new FiltroVentas(turnoB, EstadoVenta.REGISTRADA, null, null, "A", null)))
                .containsExactly(9L, 7L);
    }

    private List<Long> folios(UUID negocio, FiltroVentas filtro) {
        return almacen().buscar(negocio, filtro, 100, null).elementos().stream().map(Venta::folio).toList();
    }

    @Test
    @DisplayName("buscar pagina con cursor: recorre todo sin repetir ni saltar y la ultima pagina ya no trae cursor")
    void buscarPaginado() {
        var negocio = UUID.randomUUID();
        sembrarDiez(negocio, UUID.randomUUID(), UUID.randomUUID());

        var vistos = new ArrayList<Long>();
        String cursor = null;
        int paginas = 0;
        boolean hayMas;
        do {
            var pagina = almacen().buscar(negocio, FiltroVentas.TODAS, 4, cursor);
            pagina.elementos().forEach(v -> vistos.add(v.folio()));
            cursor = pagina.cursorFinal();
            hayMas = pagina.hayMas();
            paginas++;
        } while (hayMas);

        assertThat(vistos).containsExactly(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);
        assertThat(paginas).isEqualTo(3);
        assertThat(cursor).isNull();
    }
}
