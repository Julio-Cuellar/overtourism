package mx.negocio360.inventario.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.inventario.aplicacion.ArticuloConSaldo;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioConsumos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioMovimientos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.inventario.dominio.Articulo;
import mx.negocio360.inventario.dominio.CausaTipo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.Consumo;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.dominio.Movimiento;
import mx.negocio360.inventario.dominio.MotivoMerma;
import mx.negocio360.inventario.dominio.Rastreo;
import mx.negocio360.inventario.dominio.Saldo;
import mx.negocio360.inventario.dominio.TipoMovimiento;
import mx.negocio360.inventario.dominio.UnidadEstandar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato de {@link RepositorioArticulos}, {@link RepositorioSaldos} y {@link RepositorioMovimientos}: lo que debe
 * cumplir cualquier almacen de Inventario, en memoria o en PostgreSQL. Cada subclase aporta el almacen; la bateria es la
 * misma (F6). Cada prueba usa su propio negocio.
 */
public abstract class ContratoAlmacenDeInventarioTest {

    protected static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");

    /** Los articulos bajo prueba. */
    protected abstract RepositorioArticulos articulos();

    /** Los saldos bajo prueba. */
    protected abstract RepositorioSaldos saldos();

    /** El kardex bajo prueba. */
    protected abstract RepositorioMovimientos movimientos();

    /** Los consumos por venta bajo prueba (K-01 a K-04). */
    protected abstract RepositorioConsumos consumos();

    private static Consumo consumo(UUID negocio, UUID causa, String consumidor) {
        var linea = new Consumo.LineaConsumida(UuidV7.generar(AHORA), UuidV7.generar(AHORA),
                new BigDecimal("126.0000"), new BigDecimal("0.1800"));
        return Consumo.registrado(negocio, causa, consumidor, List.of(linea), new BigDecimal("22.6800"), AHORA);
    }

    public static Articulo articulo(UUID negocio, String nombre, String minimo) {
        return Articulo.nuevo(UuidV7.generar(AHORA), negocio, nombre, "Granos", UnidadEstandar.G,
                Map.of("costal", new BigDecimal("50000")), new BigDecimal(minimo), "dueno-a", AHORA);
    }

    private Articulo guardado(UUID negocio, String nombre, String minimo) {
        var articulo = articulo(negocio, nombre, minimo);
        articulos().insertar(articulo);
        return articulo;
    }

    private static Movimiento movimiento(Articulo articulo, int segundo, TipoMovimiento tipo, String base) {
        var cuando = AHORA.plusSeconds(segundo);
        var cantidad = new BigDecimal(base);
        return new Movimiento(UuidV7.generar(cuando), articulo.negocioId(), articulo.id(), tipo,
                cantidad.abs().setScale(4), "g", new BigDecimal("1.00000000"), cantidad.setScale(4),
                new BigDecimal("0.0180"), new BigDecimal("45.00"), new BigDecimal("0.0180"), new BigDecimal("2500.0000"),
                tipo == TipoMovimiento.MERMA ? CausaTipo.MERMA : CausaTipo.COMPRA, null,
                tipo == TipoMovimiento.MERMA ? MotivoMerma.CADUCIDAD : null, cuando, cuando, "cocinero-a");
    }

    // ---- articulos ----

    @Test
    @DisplayName("un articulo guardado se lee igual: unidades propias, categoria, minimo y fechas")
    void articuloGuardarYLeer() {
        var negocio = UUID.randomUUID();
        var original = articulo(negocio, "Frijol negro", "500");

        articulos().insertar(original);

        assertThat(articulos().porId(negocio, original.id())).contains(original);
    }

    @Test
    @DisplayName("un articulo sin categoria ni unidades propias se lee igual")
    void articuloSinCategoria() {
        var negocio = UUID.randomUUID();
        var original = Articulo.nuevo(UuidV7.generar(AHORA), negocio, "Sal", null, UnidadEstandar.KG, Map.of(),
                BigDecimal.ZERO, "dueno-a", AHORA);

        articulos().insertar(original);

        assertThat(articulos().porId(negocio, original.id())).contains(original);
    }

    @Test
    @DisplayName("el rastreo del articulo se guarda y se lee igual (L-01, D-27)")
    void rastreoGuardarYLeer() {
        var negocio = UUID.randomUUID();
        var original = Articulo.nuevo(UuidV7.generar(AHORA), negocio, "Leche", null, UnidadEstandar.L, Map.of(),
                BigDecimal.ZERO, "dueno-a", AHORA, Rastreo.LOTE);

        articulos().insertar(original);

        assertThat(articulos().porId(negocio, original.id())).contains(original);
        assertThat(articulos().porId(negocio, original.id()).orElseThrow().rastreo()).isEqualTo(Rastreo.LOTE);
    }

    @Test
    @DisplayName("un articulo de otro negocio no se ve")
    void articuloOtroNegocioNoSeVe() {
        var negocio = UUID.randomUUID();
        var original = guardado(negocio, "Frijol", "0");

        assertThat(articulos().porId(UUID.randomUUID(), original.id())).isEmpty();
        assertThat(articulos().porId(negocio, UUID.randomUUID())).isEmpty();
        assertThat(articulos().activoPorNombre(UUID.randomUUID(), "Frijol")).isEmpty();
    }

    @Test
    @DisplayName("activoPorNombre ignora mayusculas y no ve los dados de baja")
    void articuloPorNombre() {
        var negocio = UUID.randomUUID();
        var original = guardado(negocio, "Maiz Azul", "0");

        assertThat(articulos().activoPorNombre(negocio, "maiz AZUL")).contains(original);
        assertThat(articulos().activoPorNombre(negocio, "Maiz")).isEmpty();

        var baja = original.dadoDeBaja(AHORA.plusSeconds(60));
        articulos().actualizar(baja, original.version());

        assertThat(articulos().activoPorNombre(negocio, "Maiz Azul")).isEmpty();
        assertThat(articulos().porId(negocio, original.id())).contains(baja);
    }

    @Test
    @DisplayName("actualizar guarda los cambios con la version esperada; con una vieja falla con CONCURRENCIA")
    void articuloActualizar() {
        var negocio = UUID.randomUUID();
        var original = guardado(negocio, "Frijol", "0");
        var nuevo = original.conDatos("Frijol bayo", "Granos", new BigDecimal("100"), AHORA.plusSeconds(60));

        articulos().actualizar(nuevo, original.version());

        assertThat(articulos().porId(negocio, original.id())).contains(nuevo);
        assertThatThrownBy(() -> articulos().actualizar(
                original.conDatos("Otro", null, BigDecimal.ZERO, AHORA), original.version()))
                .isInstanceOfSatisfying(InventarioException.class, e -> assertThat(e.codigo()).isEqualTo(CodigoError.CONCURRENCIA));
        assertThat(articulos().porId(negocio, original.id())).contains(nuevo);
    }

    @Test
    @DisplayName("actualizar no toca el articulo de otro negocio aunque el id coincida")
    void articuloActualizarAjeno() {
        var negocio = UUID.randomUUID();
        var original = guardado(negocio, "Frijol", "0");
        var ajeno = new Articulo(original.id(), UUID.randomUUID(), "Hackeado", null, UnidadEstandar.G,
                Rastreo.NINGUNO, Map.of(), BigDecimal.ZERO, true, original.version() + 1, AHORA, AHORA, "x");

        assertThatThrownBy(() -> articulos().actualizar(ajeno, original.version())).isInstanceOf(InventarioException.class);
        assertThat(articulos().porId(negocio, original.id())).contains(original);
    }

    @Test
    @DisplayName("una unidad propia nueva se guarda con el articulo")
    void articuloConUnidadPropiaNueva() {
        var negocio = UUID.randomUUID();
        var original = guardado(negocio, "Frijol", "0");
        var nuevo = original.conUnidadPropia("bulto", new BigDecimal("25000"), AHORA.plusSeconds(1));

        articulos().actualizar(nuevo, original.version());

        assertThat(articulos().porId(negocio, original.id()).orElseThrow().unidadesPropias())
                .containsOnlyKeys("bulto", "costal");
    }

    // ---- busqueda ----

    @Test
    @DisplayName("buscar filtra por texto sin importar mayusculas, solo activos y solo del negocio, en orden de id")
    void buscarPorTexto() {
        var negocio = UUID.randomUUID();
        var a = guardado(negocio, "Frijol negro", "0");
        var b = guardado(negocio, "FRIJOL bayo", "0");
        guardado(negocio, "Arroz", "0");
        var baja = guardado(negocio, "Frijol viejo", "0");
        articulos().actualizar(baja.dadoDeBaja(AHORA), baja.version());
        guardado(UUID.randomUUID(), "Frijol ajeno", "0");

        var encontrados = articulos().buscar(negocio, "frijol", false, null, 50);

        assertThat(encontrados).extracting(f -> f.articulo().id())
                .containsExactlyInAnyOrder(a.id(), b.id());
        assertThat(encontrados).isSortedAccordingTo((x, y) -> x.articulo().id().compareTo(y.articulo().id()));
        assertThat(articulos().buscar(negocio, null, false, null, 50)).hasSize(3);
        assertThat(articulos().buscar(negocio, "   ", false, null, 50)).hasSize(3);
        assertThat(articulos().buscar(negocio, "100% inexistente", false, null, 50)).isEmpty();
    }

    @Test
    @DisplayName("buscar trata % y _ como texto, no como comodines")
    void buscarSinComodines() {
        var negocio = UUID.randomUUID();
        guardado(negocio, "Frijol", "0");
        guardado(negocio, "Cafe 100% arabica", "0");

        assertThat(articulos().buscar(negocio, "%", false, null, 50)).hasSize(1);
        assertThat(articulos().buscar(negocio, "_", false, null, 50)).isEmpty();
    }

    @Test
    @DisplayName("buscar pagina por llave: despuesDe y limite recorren todo sin repetir ni saltar")
    void buscarPaginado() {
        var negocio = UUID.randomUUID();
        var esperados = new ArrayList<UUID>();
        for (int i = 0; i < 7; i++) {
            esperados.add(guardado(negocio, "Articulo " + i, "0").id());
        }
        esperados.sort(UUID::compareTo);

        var vistos = new ArrayList<UUID>();
        UUID despues = null;
        while (true) {
            var pagina = articulos().buscar(negocio, null, false, despues, 3);
            if (pagina.isEmpty()) {
                break;
            }
            pagina.forEach(f -> vistos.add(f.articulo().id()));
            despues = pagina.get(pagina.size() - 1).articulo().id();
        }

        assertThat(vistos).containsExactlyElementsOf(esperados);
    }

    @Test
    @DisplayName("buscar trae el saldo de cada articulo, o nulo si aun no tiene")
    void buscarConSaldo() {
        var negocio = UUID.randomUUID();
        var conSaldo = guardado(negocio, "Con saldo", "0");
        guardado(negocio, "Sin saldo", "0");
        var saldo = new Saldo(conSaldo.id(), negocio, new BigDecimal("1200.5"), new BigDecimal("0.0180"), null, 1);
        saldos().insertar(saldo);

        var filas = articulos().buscar(negocio, null, false, null, 50);

        assertThat(filas).filteredOn(f -> f.articulo().id().equals(conSaldo.id())).extracting(ArticuloConSaldo::saldo)
                .containsExactly(saldo);
        assertThat(filas).filteredOn(f -> !f.articulo().id().equals(conSaldo.id())).extracting(ArticuloConSaldo::saldo)
                .containsExactly((Saldo) null);
    }

    @Test
    @DisplayName("soloAlertas trae lo que esta en el minimo o por debajo, lo que no tiene saldo con minimo, y lo negativo")
    void buscarSoloAlertas() {
        var negocio = UUID.randomUUID();
        var bajo = guardado(negocio, "Bajo", "500");
        var justo = guardado(negocio, "Justo en el minimo", "500");
        var sano = guardado(negocio, "Sano", "500");
        var negativo = guardado(negocio, "Negativo", "0");
        var sinSaldoConMinimo = guardado(negocio, "Sin saldo con minimo", "10");
        var sinSaldoSinMinimo = guardado(negocio, "Sin saldo sin minimo", "0");
        var cpp = new BigDecimal("0.0180");
        saldos().insertar(new Saldo(bajo.id(), negocio, new BigDecimal("100"), cpp, null, 1));
        saldos().insertar(new Saldo(justo.id(), negocio, new BigDecimal("500"), cpp, null, 1));
        saldos().insertar(new Saldo(sano.id(), negocio, new BigDecimal("501"), cpp, null, 1));
        saldos().insertar(new Saldo(negativo.id(), negocio, new BigDecimal("-1"), cpp, null, 1));

        var alertas = articulos().buscar(negocio, null, true, null, 50);

        assertThat(alertas).extracting(f -> f.articulo().id())
                .containsExactlyInAnyOrder(bajo.id(), justo.id(), negativo.id(), sinSaldoConMinimo.id());
        assertThat(alertas).extracting(f -> f.articulo().id()).doesNotContain(sano.id(), sinSaldoSinMinimo.id());
    }

    // ---- saldos ----

    @Test
    @DisplayName("un saldo guardado se lee igual, incluso negativo y con su ultimo movimiento")
    void saldoGuardarYLeer() {
        var negocio = UUID.randomUUID();
        var articulo = guardado(negocio, "Frijol", "0");
        var original = new Saldo(articulo.id(), negocio, new BigDecimal("-12.5"), new BigDecimal("0.0180"),
                UuidV7.generar(AHORA), 1);

        saldos().insertar(original);

        assertThat(saldos().porArticulo(negocio, articulo.id())).contains(original);
        assertThat(saldos().porArticulo(UUID.randomUUID(), articulo.id())).isEmpty();
        assertThat(saldos().porArticulo(negocio, UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("actualizar un saldo con la version esperada lo guarda; con una vieja falla con CONCURRENCIA")
    void saldoActualizar() {
        var negocio = UUID.randomUUID();
        var articulo = guardado(negocio, "Frijol", "0");
        var original = Saldo.inicial(articulo.id(), negocio);
        saldos().insertar(original);
        var nuevo = original.conEntrada(new BigDecimal("1000"), new BigDecimal("0.0200"), UuidV7.generar(AHORA));

        saldos().actualizar(nuevo, original.version());

        assertThat(saldos().porArticulo(negocio, articulo.id())).contains(nuevo);
        assertThatThrownBy(() -> saldos().actualizar(nuevo, original.version()))
                .isInstanceOfSatisfying(InventarioException.class, e -> assertThat(e.codigo()).isEqualTo(CodigoError.CONCURRENCIA));
    }

    @Test
    @DisplayName("actualizar un saldo no toca el de otro negocio aunque el id del articulo coincida")
    void saldoActualizarAjeno() {
        var negocio = UUID.randomUUID();
        var articulo = guardado(negocio, "Frijol", "0");
        var original = Saldo.inicial(articulo.id(), negocio);
        saldos().insertar(original);
        var ajeno = new Saldo(articulo.id(), UUID.randomUUID(), BigDecimal.TEN, BigDecimal.ONE, null, 2);

        assertThatThrownBy(() -> saldos().actualizar(ajeno, 1)).isInstanceOf(InventarioException.class);
        assertThat(saldos().porArticulo(negocio, articulo.id())).contains(original);
    }

    // ---- kardex ----

    @Test
    @DisplayName("un movimiento guardado se lee igual en el kardex, con motivo de merma y causa")
    void kardexGuardarYLeer() {
        var negocio = UUID.randomUUID();
        var articulo = guardado(negocio, "Frijol", "0");
        var entrada = movimiento(articulo, 1, TipoMovimiento.ENTRADA, "2500");
        var merma = movimiento(articulo, 2, TipoMovimiento.MERMA, "-300");

        movimientos().insertar(entrada);
        movimientos().insertar(merma);

        assertThat(movimientos().kardex(negocio, articulo.id(), null, 50)).containsExactly(entrada, merma);
    }

    @Test
    @DisplayName("el kardex es de un articulo y un negocio, en orden de registro, y se recorre por llave")
    void kardexPaginado() {
        var negocio = UUID.randomUUID();
        var articulo = guardado(negocio, "Frijol", "0");
        var otro = guardado(negocio, "Arroz", "0");
        var esperados = new ArrayList<UUID>();
        for (int i = 1; i <= 7; i++) {
            var m = movimiento(articulo, i, TipoMovimiento.ENTRADA, "100");
            movimientos().insertar(m);
            esperados.add(m.id());
        }
        movimientos().insertar(movimiento(otro, 1, TipoMovimiento.ENTRADA, "100"));

        var vistos = new ArrayList<UUID>();
        UUID despues = null;
        while (true) {
            var pagina = movimientos().kardex(negocio, articulo.id(), despues, 3);
            if (pagina.isEmpty()) {
                break;
            }
            pagina.forEach(m -> vistos.add(m.id()));
            despues = pagina.get(pagina.size() - 1).id();
        }

        assertThat(vistos).containsExactlyElementsOf(esperados);
        assertThat(movimientos().kardex(UUID.randomUUID(), articulo.id(), null, 50)).isEmpty();
    }

    // ---- consumos por venta (K-01 a K-04) ----

    @Test
    @DisplayName("K-01: un consumo guardado se lee igual por su llave (negocio, causa, consumidor)")
    void consumoGuardarYLeer() {
        var negocio = UUID.randomUUID();
        var causa = UUID.randomUUID();
        var original = consumo(negocio, causa, "restaurantes");

        consumos().insertar(original);

        assertThat(consumos().porLlave(negocio, causa, "restaurantes")).contains(original);
    }

    @Test
    @DisplayName("la llave incluye al consumidor y al negocio: otra causa, otro consumidor u otro negocio no se ven")
    void consumoLlaveCompleta() {
        var negocio = UUID.randomUUID();
        var causa = UUID.randomUUID();
        consumos().insertar(consumo(negocio, causa, "restaurantes"));

        assertThat(consumos().porLlave(negocio, UUID.randomUUID(), "restaurantes")).isEmpty();
        assertThat(consumos().porLlave(negocio, causa, "vinculo")).isEmpty();
        assertThat(consumos().porLlave(UUID.randomUUID(), causa, "restaurantes")).isEmpty();
    }

    @Test
    @DisplayName("el mismo consumidor puede consumir la misma causa en dos negocios, y dos consumidores la misma causa en uno")
    void consumoMismaCausaOtraLlave() {
        var causa = UUID.randomUUID();
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();

        consumos().insertar(consumo(a, causa, "restaurantes"));
        consumos().insertar(consumo(b, causa, "restaurantes"));
        consumos().insertar(consumo(a, causa, "vinculo"));

        assertThat(consumos().porLlave(a, causa, "restaurantes")).isPresent();
        assertThat(consumos().porLlave(b, causa, "restaurantes")).isPresent();
        assertThat(consumos().porLlave(a, causa, "vinculo")).isPresent();
    }

    @Test
    @DisplayName("K-01: insertar dos veces la misma llave falla con CONCURRENCIA y no pisa el primero")
    void consumoInsertarDosVeces() {
        var negocio = UUID.randomUUID();
        var causa = UUID.randomUUID();
        var primero = consumo(negocio, causa, "restaurantes");
        consumos().insertar(primero);

        assertThatThrownBy(() -> consumos().insertar(consumo(negocio, causa, "restaurantes")))
                .isInstanceOfSatisfying(InventarioException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoError.CONCURRENCIA));
        assertThat(consumos().porLlave(negocio, causa, "restaurantes")).contains(primero);
    }

    @Test
    @DisplayName("K-03: actualizar reemplaza el consumo por su version revertida, con las mismas lineas")
    void consumoActualizarARevertido() {
        var negocio = UUID.randomUUID();
        var causa = UUID.randomUUID();
        var original = consumo(negocio, causa, "restaurantes");
        consumos().insertar(original);
        var reversa = List.of(UuidV7.generar(AHORA), UuidV7.generar(AHORA));

        consumos().actualizar(original.revertido(reversa));

        var leido = consumos().porLlave(negocio, causa, "restaurantes").orElseThrow();
        assertThat(leido.estado()).isEqualTo(Consumo.Estado.REVERTIDO);
        assertThat(leido.lineas()).isEqualTo(original.lineas());
        assertThat(leido.movimientosReversa()).containsExactlyElementsOf(reversa);
        assertThat(leido.costoTotal()).isEqualByComparingTo("22.6800");
    }

    @Test
    @DisplayName("actualizar un consumo que no existe falla con CONCURRENCIA y no lo crea")
    void consumoActualizarInexistente() {
        var negocio = UUID.randomUUID();
        var causa = UUID.randomUUID();

        assertThatThrownBy(() -> consumos().actualizar(consumo(negocio, causa, "restaurantes")))
                .isInstanceOfSatisfying(InventarioException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoError.CONCURRENCIA));
        assertThat(consumos().porLlave(negocio, causa, "restaurantes")).isEmpty();
    }

    @Test
    @DisplayName("K-04: la marca de anulacion previa se guarda sin lineas y con costo cero")
    void consumoMarcaDeAnulacion() {
        var negocio = UUID.randomUUID();
        var causa = UUID.randomUUID();

        consumos().insertar(Consumo.marcaDeAnulacion(negocio, causa, "restaurantes", AHORA));

        var leido = consumos().porLlave(negocio, causa, "restaurantes").orElseThrow();
        assertThat(leido.estado()).isEqualTo(Consumo.Estado.ANULADO_PREVIO);
        assertThat(leido.lineas()).isEmpty();
        assertThat(leido.movimientosReversa()).isEmpty();
        assertThat(leido.costoTotal()).isEqualByComparingTo("0");
    }
}
