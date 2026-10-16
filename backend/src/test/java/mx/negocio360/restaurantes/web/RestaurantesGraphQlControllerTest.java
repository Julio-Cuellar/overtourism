package mx.negocio360.restaurantes.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.restaurantes.aplicacion.ArticuloDeInventario;
import mx.negocio360.restaurantes.aplicacion.CantidadEnBase;
import mx.negocio360.restaurantes.aplicacion.InventarioPuerto;
import mx.negocio360.restaurantes.aplicacion.LineaDeConsumo;
import mx.negocio360.restaurantes.aplicacion.LineaDeInsumo;
import mx.negocio360.restaurantes.aplicacion.ConsumoFallido;
import mx.negocio360.restaurantes.aplicacion.ConsumosServicio;
import mx.negocio360.restaurantes.aplicacion.LineaFallida;
import mx.negocio360.restaurantes.aplicacion.Rastreo;
import mx.negocio360.restaurantes.aplicacion.RecetasServicio;
import mx.negocio360.restaurantes.aplicacion.ResultadoDeConsumo;
import mx.negocio360.restaurantes.infraestructura.ConsumosEnMemoria;
import mx.negocio360.restaurantes.infraestructura.RecetasEnMemoria;
import mx.negocio360.restaurantes.aplicacion.FiltroPlatillos;
import mx.negocio360.restaurantes.aplicacion.MenuPublicoServicio;
import mx.negocio360.restaurantes.aplicacion.MenuServicio;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.EtiquetaPlatillo;
import mx.negocio360.restaurantes.dominio.ModoInventario;
import mx.negocio360.restaurantes.dominio.Restaurante;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.infraestructura.MenuEnMemoria;
import mx.negocio360.restaurantes.infraestructura.RestaurantesEnMemoria;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Los resolvedores de R1 solo traducen GraphQL al servicio: el negocio y los permisos salen de la sesion (SG-02), la
 * temporada se arma de sus dos textos y sin sesion o sin negocio activo todo falla cerrado.
 */
class RestaurantesGraphQlControllerTest {

    private static final UUID NEGOCIO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    private static final UUID CUENTA = UUID.fromString("0192f3a0-7c10-7000-8000-0000000000c1");

    private final Clock reloj = Clock.fixed(java.time.Instant.parse("2026-10-16T18:00:00Z"), java.time.ZoneOffset.UTC);
    private final MenuEnMemoria menu = new MenuEnMemoria();
    private final RestaurantesEnMemoria restaurantes = new RestaurantesEnMemoria();
    private final ConsumosEnMemoria consumos = new ConsumosEnMemoria();
    private final RecetasEnMemoria recetasAlmacen = new RecetasEnMemoria();
    private final InventarioFijo inventario = new InventarioFijo();
    private final RestaurantesGraphQlController controlador = new RestaurantesGraphQlController(
            new MenuServicio(menu, recetasAlmacen, reloj), new MenuPublicoServicio(menu, restaurantes, reloj),
            new RecetasServicio(recetasAlmacen, menu, inventario, reloj), new ConsumosServicio(consumos));

    /** Un Inventario con un solo articulo: harina a 0.02 el gramo. */
    private static final class InventarioFijo implements InventarioPuerto {
        static final UUID HARINA = UUID.fromString("0192f3a0-7c10-7000-8000-0000000000f1");

        @Override
        public java.util.Map<UUID, ArticuloDeInventario> articulos(ContextoNegocio contexto, Set<UUID> ids) {
            return ids.contains(HARINA) ? java.util.Map.of(HARINA, new ArticuloDeInventario(HARINA, "Harina", "g",
                    new BigDecimal("0.02"), true, Rastreo.NINGUNO)) : java.util.Map.of();
        }

        @Override
        public List<CantidadEnBase> aBase(ContextoNegocio contexto, List<LineaDeInsumo> lineas) {
            return lineas.stream().map(l -> new CantidadEnBase(l.articuloId(), l.cantidad(), "g")).toList();
        }

        @Override
        public ResultadoDeConsumo registrarConsumo(ContextoNegocio contexto, String consumidor, UUID causaId,
                                                   java.time.Instant ocurridoEn, List<LineaDeConsumo> lineas) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResultadoDeConsumo revertirConsumo(ContextoNegocio contexto, String consumidor, UUID causaId) {
            throw new UnsupportedOperationException();
        }
    }

    private static EntradaGraphQl sesion(String... permisos) {
        return EntradaGraphQl.de(CUENTA, new ContextoNegocio(NEGOCIO, CUENTA.toString()), Autorizacion.de(Set.of(permisos)), null);
    }

    private static EntradaGraphQl todo() {
        return sesion(PermisosDeRestaurantes.MENU_VER, PermisosDeRestaurantes.MENU_EDITAR, PermisosDeRestaurantes.MENU_AGOTAR);
    }

    private static PlatilloInput entrada(String nombre, String desde, String hasta) {
        return new PlatilloInput(nombre, "Rico", new BigDecimal("85"), null, Set.of(EtiquetaPlatillo.PICANTE), desde, hasta);
    }

    @Test
    @DisplayName("crear y leer un platillo: el negocio sale de la sesion y la temporada se arma de sus dos textos")
    void crearYLeer() {
        var creado = controlador.crearPlatillo(entrada("Chiles en nogada", "08-01", "09-30"), todo());

        assertThat(creado.temporadaDesde()).isEqualTo("08-01");
        assertThat(creado.temporadaHasta()).isEqualTo("09-30");
        assertThat(controlador.platillo(creado.id(), todo()).nombre()).isEqualTo("Chiles en nogada");
        assertThat(controlador.platillos(null, 20, null, todo()).nodos()).hasSize(1);
        assertThat(controlador.platillos(FiltroPlatillos.TODOS, 20, null, todo()).nodos()).hasSize(1);
    }

    @Test
    @DisplayName("una temporada con un solo extremo o con texto que no es mes-dia falla con TEMPORADA_INVALIDA")
    void temporadaInvalida() {
        assertThatThrownBy(() -> controlador.crearPlatillo(entrada("Nogada", "08-01", null), todo()))
                .isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(CodigoError.TEMPORADA_INVALIDA);
        assertThatThrownBy(() -> controlador.crearPlatillo(entrada("Nogada", null, "09-30"), todo()))
                .isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(CodigoError.TEMPORADA_INVALIDA);
        assertThatThrownBy(() -> controlador.crearPlatillo(entrada("Nogada", "agosto", "septiembre"), todo()))
                .isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(CodigoError.TEMPORADA_INVALIDA);
        assertThat(controlador.crearPlatillo(entrada("Mole", null, null), todo()).temporadaDesde()).isNull();
    }

    @Test
    @DisplayName("editar, agotar, publicar y cambiar el modo pasan por el servicio con la version")
    void mutaciones() {
        var p = controlador.crearPlatillo(entrada("Cemita", null, null), todo());

        var editado = controlador.editarPlatillo(p.id(), (int) p.version(), entrada("Cemita especial", null, null), todo());
        assertThat(editado.version()).isEqualTo(2);
        assertThat(controlador.marcarAgotado(p.id(), true, todo()).agotado()).isTrue();
        assertThat(controlador.publicarPlatillo(p.id(), true, todo()).publicado()).isTrue();
        assertThat(controlador.cambiarModoDeInventario(p.id(), ModoInventario.VINCULO_DIRECTO, todo()).modo())
                .isEqualTo(ModoInventario.VINCULO_DIRECTO);
    }

    @Test
    @DisplayName("categorias: crear, editar y listar")
    void categorias() {
        var c = controlador.crearCategoria(new CategoriaInput("Bebidas", 2, mx.negocio360.restaurantes.dominio.Estacion.BARRA), todo());

        assertThat(controlador.editarCategoria(c.id(), (int) c.version(), new CategoriaInput("Bebidas frias", 1, null), todo()).nombre())
                .isEqualTo("Bebidas frias");
        assertThat(controlador.categoriasDeMenu(todo())).extracting("nombre").containsExactly("Bebidas frias");
        assertThat(controlador.resumenDeMenu(todo()).total()).isZero();
    }

    @Test
    @DisplayName("sin el permiso, falla PermisoDenegado; sin sesion o sin negocio activo, falla cerrado")
    void permisosYSesion() {
        assertThatThrownBy(() -> controlador.platillos(null, 20, null, sesion())).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> controlador.crearPlatillo(entrada("Mole", null, null), sesion(PermisosDeRestaurantes.MENU_VER)))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> controlador.platillos(null, 20, null, EntradaGraphQl.anonima(null)))
                .isInstanceOf(NoAutenticadoException.class);
        var sinNegocio = EntradaGraphQl.de(CUENTA, null, Autorizacion.de(Set.of(PermisosDeRestaurantes.MENU_VER)), null);
        assertThatThrownBy(() -> controlador.platillos(null, 20, null, sinNegocio)).isInstanceOf(NoAutenticadoException.class);
    }

    @Test
    @DisplayName("menuPublico no necesita sesion: devuelve el menu de un restaurante visible y nulo si no lo es")
    void menuPublico() {
        var p = controlador.crearPlatillo(entrada("Mole poblano", null, null), todo());
        controlador.publicarPlatillo(p.id(), true, todo());

        assertThat(controlador.menuPublico(NEGOCIO, null)).as("sin extension de restaurante").isNull();
        restaurantes.guardar(Restaurante.nuevo(NEGOCIO, reloj.instant()).conPublicado(true, reloj.instant()));

        assertThat(controlador.menuPublico(NEGOCIO, null).sinCategoria()).extracting("nombre").containsExactly("Mole poblano");
        assertThat(List.of(controlador.menuPublico(NEGOCIO, "en").idioma())).containsExactly("es-MX");
    }

    @Test
    @DisplayName("receta, costoDeReceta y desactivarReceta: sin receta falla con RECETA_NO_ENCONTRADA y cada una exige su permiso")
    void recetas() {
        var p = controlador.crearPlatillo(entrada("Tortilla", null, null), todo());
        var conTodo = sesion(PermisosDeRestaurantes.RECETA_VER, PermisosDeRestaurantes.COSTO_VER, PermisosDeRestaurantes.RECETA_EDITAR);
        assertThatThrownBy(() -> controlador.receta(p.id(), conTodo)).isInstanceOf(RestaurantesException.class)
                .extracting("codigo").isEqualTo(CodigoError.RECETA_NO_ENCONTRADA);
        assertThatThrownBy(() -> controlador.costoDeReceta(p.id(), conTodo)).isInstanceOf(RestaurantesException.class);

        var receta = mx.negocio360.restaurantes.dominio.Receta.nueva(UUID.randomUUID(), NEGOCIO, p.id(), 10,
                List.of(new mx.negocio360.restaurantes.dominio.InsumoDeReceta(InventarioFijo.HARINA, new BigDecimal("500"), "g", BigDecimal.ZERO)),
                CUENTA.toString(), reloj.instant());
        recetasAlmacen.insertar(receta);

        assertThat(controlador.receta(p.id(), conTodo).insumos()).extracting("nombre").containsExactly("Harina");
        assertThat(controlador.costoDeReceta(p.id(), conTodo).costoPorPorcion()).isEqualByComparingTo("1.00");
        assertThatThrownBy(() -> controlador.costoDeReceta(p.id(), sesion(PermisosDeRestaurantes.RECETA_VER))).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> controlador.receta(p.id(), sesion(PermisosDeRestaurantes.COSTO_VER))).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> controlador.desactivarReceta(p.id(), sesion(PermisosDeRestaurantes.RECETA_VER))).isInstanceOf(PermisoDenegadoException.class);
        assertThat(controlador.desactivarReceta(p.id(), conTodo).activa()).isFalse();
    }

    @Test
    @DisplayName("consumosFallidos: lista los del negocio con su texto fijo y exige restaurantes.consumo.ver")
    void consumosFallidos() {
        var venta = UUID.randomUUID();
        consumos.guardarFallido(new ConsumoFallido(UUID.randomUUID(), NEGOCIO, venta, "SIN_RECETA",
                List.of(new LineaFallida(UUID.randomUUID(), BigDecimal.ONE)), reloj.instant(), reloj.instant()));
        consumos.guardarFallido(new ConsumoFallido(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ARTICULO_INACTIVO",
                List.of(), reloj.instant(), reloj.instant()));
        var puede = sesion(PermisosDeRestaurantes.CONSUMO_VER);

        var pagina = controlador.consumosFallidos(20, null, puede);

        assertThat(pagina.nodos()).singleElement().satisfies(f -> {
            assertThat(f.ventaId()).isEqualTo(venta);
            assertThat(controlador.mensaje(f)).isEqualTo("El platillo no tenía receta cuando se vendió.");
        });
        assertThatThrownBy(() -> controlador.consumosFallidos(20, null, sesion())).isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("el texto de cada codigo de fallo es fijo y un codigo desconocido de Inventario tiene un texto general")
    void mensajes() {
        for (var codigo : List.of("SIN_RECETA", "ARTICULO_INACTIVO", "UNIDAD_NO_CONVERTIBLE", "ARTICULO_NO_ENCONTRADO", "OTRO_CODIGO")) {
            var f = new ConsumoFallido(UUID.randomUUID(), NEGOCIO, UUID.randomUUID(), codigo, List.of(), reloj.instant(), reloj.instant());

            assertThat(controlador.mensaje(f)).isNotBlank().doesNotContain(codigo);
        }
        assertThat(controlador.mensaje(new ConsumoFallido(UUID.randomUUID(), NEGOCIO, UUID.randomUUID(), "OTRO_CODIGO", List.of(),
                reloj.instant(), reloj.instant()))).isEqualTo("Inventario no pudo descontar los insumos de esta venta.");
    }
}
