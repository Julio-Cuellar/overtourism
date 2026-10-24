package mx.negocio360.restaurantes.web;

import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.comandos.PreparacionesEnMemoria;
import mx.negocio360.compartido.comandos.ServicioPreparacion;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import mx.negocio360.compartido.graphql.comandos.ModuloDeComandos;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.CrearArticuloComando;
import mx.negocio360.inventario.aplicacion.RegistrarEntradaComando;
import mx.negocio360.inventario.infraestructura.InventarioEnMemoria;
import mx.negocio360.inventario.infraestructura.InventarioEnMemoriaConfiguracion;
import mx.negocio360.restaurantes.aplicacion.DatosPlatillo;
import mx.negocio360.restaurantes.aplicacion.ManejadorDarDeBajaPlatillo;
import mx.negocio360.restaurantes.aplicacion.ManejadorGuardarReceta;
import mx.negocio360.restaurantes.aplicacion.MenuServicio;
import mx.negocio360.restaurantes.aplicacion.RecetasServicio;
import mx.negocio360.restaurantes.aplicacion.VinculoDeSesion;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.infraestructura.InventarioApiComoPuerto;
import mx.negocio360.restaurantes.infraestructura.MenuEnMemoria;
import mx.negocio360.restaurantes.infraestructura.RecetasEnMemoria;
import mx.negocio360.restaurantes.infraestructura.SesionDeRestaurantes;
import mx.negocio360.restaurantes.infraestructura.SinCuentas;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las variantes de comando de Restaurantes para el despachador de la plataforma (D-125, D-209): {@code guardarReceta} y
 * {@code darDeBajaPlatillo}, con el mismo preparar -> confirmar y el permiso exigido en las dos puntas, antes de gastar el
 * token (D-126). Se arma a mano con Inventario en memoria de verdad, sin Spring, para no depender de la sesion web.
 */
class ComandosDeRestaurantesTest {

    private static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final UUID NEGOCIO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    private static final UUID CUENTA = UUID.fromString("0192f3a0-7c10-7000-8000-0000000000c1");

    private static final Autorizacion TODO = Autorizacion.de(Set.of(PermisosDeRestaurantes.MENU_VER,
            PermisosDeRestaurantes.MENU_EDITAR, PermisosDeRestaurantes.RECETA_VER, PermisosDeRestaurantes.RECETA_EDITAR,
            PermisosDeRestaurantes.COSTO_VER));
    private static final Autorizacion SOLO_MENU = Autorizacion.de(Set.of(PermisosDeRestaurantes.MENU_VER,
            PermisosDeRestaurantes.MENU_EDITAR));

    private final ContextoNegocio ctx = new ContextoNegocio(NEGOCIO, CUENTA.toString());
    private final MenuEnMemoria menuAlmacen = new MenuEnMemoria();
    private final RecetasEnMemoria recetasAlmacen = new RecetasEnMemoria();
    private final InventarioApi inventario = new InventarioEnMemoriaConfiguracion().inventarioApi(
            new InventarioEnMemoria(), evento -> { }, RELOJ);
    private final MenuServicio menu = new MenuServicio(menuAlmacen, recetasAlmacen, RELOJ);
    private final RecetasServicio recetas = new RecetasServicio(recetasAlmacen, menuAlmacen,
            new InventarioApiComoPuerto(inventario), RELOJ);
    private final ModuloDeComandos modulo = new ComandosDeRestaurantes(new ServicioPreparacion(
            new PreparacionesEnMemoria(),
            List.of(new ManejadorGuardarReceta(menuAlmacen, recetasAlmacen, new InventarioApiComoPuerto(inventario),
                            (contexto, permiso) -> true, RELOJ),
                    new ManejadorDarDeBajaPlatillo(menuAlmacen, new SinCuentas(), recetasAlmacen,
                            (contexto, permiso) -> true, RELOJ)),
            RELOJ, fallo -> new RestaurantesException(CodigoError.valueOf(fallo.name()))),
            new SesionDeRestaurantes());

    private EntradaGraphQl entrada(Autorizacion permisos) {
        return EntradaGraphQl.de(CUENTA, ctx, permisos, "dispositivo-1");
    }

    private UUID platillo(String nombre) {
        return menu.crearPlatillo(ctx, TODO, new DatosPlatillo(nombre, null, new BigDecimal("85"), null, Set.of(), null))
                .id();
    }

    private UUID harinaConDosKilos() {
        var articulo = inventario.confirmar(ctx, inventario.preparar(ctx, new CrearArticuloComando("Harina", null, "g",
                null, null, null)).token()).recursoId();
        inventario.confirmar(ctx, inventario.preparar(ctx, new RegistrarEntradaComando(articulo, d("2"), "kg", d("20"),
                null)).token());
        return articulo;
    }

    private Map<String, Object> receta(UUID platillo, UUID articulo) {
        return Map.of("platilloId", platillo.toString(), "rendimiento", 4, "insumos", List.of(Map.of(
                "articuloId", articulo.toString(), "cantidad", "500", "unidad", "g", "mermaPct", "10")));
    }

    @Test
    @DisplayName("el modulo atiende guardarReceta, darDeBajaPlatillo, anularLineaPreparada y cancelarCuentaPreparada con el prefijo restaurantes. y exige sesion")
    void identidadDelModulo() {
        assertThat(modulo.prefijo()).isEqualTo("restaurantes.");
        assertThat(modulo.variantes()).containsExactlyInAnyOrder("guardarReceta", "darDeBajaPlatillo",
                "anularLineaPreparada", "cancelarCuentaPreparada");
        assertThat(modulo.confirmaSinSesion()).isFalse();
    }

    @Test
    @DisplayName("guardarReceta: preparar da el resumen con costo y confirmar guarda la version 1")
    void guardarReceta() {
        var cemita = platillo("Cemita poblana");
        var harina = harinaConDosKilos();

        var preparada = modulo.preparar(entrada(TODO), "guardarReceta", receta(cemita, harina));
        var resultado = modulo.confirmar(entrada(TODO), preparada.token());

        assertThat(preparada.token()).isNotBlank();
        assertThat(preparada.resumen()).isNotBlank();
        assertThat(resultado.ok()).isTrue();
        assertThat(resultado.recursoId()).isEqualTo(cemita);
        var guardada = recetas.receta(ctx, TODO, cemita);
        assertThat(guardada.version()).isEqualTo(1);
        assertThat(guardada.insumos()).hasSize(1);
    }

    @Test
    @DisplayName("guardarReceta sin restaurantes.receta.editar se niega al preparar")
    void guardarRecetaSinPermisoAlPreparar() {
        var cemita = platillo("Cemita poblana");
        var harina = harinaConDosKilos();

        assertThatThrownBy(() -> modulo.preparar(entrada(SOLO_MENU), "guardarReceta", receta(cemita, harina)))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("D-126: si le quitan el permiso entre preparar y confirmar, se niega y el token no se gasta")
    void permisoRevocadoNoGastaElToken() {
        var cemita = platillo("Cemita poblana");
        var harina = harinaConDosKilos();
        var preparada = modulo.preparar(entrada(TODO), "guardarReceta", receta(cemita, harina));

        assertThatThrownBy(() -> modulo.confirmar(entrada(SOLO_MENU), preparada.token()))
                .isInstanceOf(PermisoDenegadoException.class);

        assertThat(modulo.confirmar(entrada(TODO), preparada.token()).ok()).isTrue();
    }

    @Test
    @DisplayName("darDeBajaPlatillo: preparar y confirmar dan de baja el platillo; pide restaurantes.menu.editar")
    void darDeBaja() {
        var cemita = platillo("Cemita poblana");

        assertThatThrownBy(() -> modulo.preparar(entrada(Autorizacion.de(Set.of(PermisosDeRestaurantes.MENU_VER))),
                "darDeBajaPlatillo", Map.of("platilloId", cemita.toString())))
                .isInstanceOf(PermisoDenegadoException.class);
        var preparada = modulo.preparar(entrada(TODO), "darDeBajaPlatillo", Map.of("platilloId", cemita.toString()));
        var resultado = modulo.confirmar(entrada(TODO), preparada.token());

        assertThat(resultado.ok()).isTrue();
        assertThat(menuAlmacen.platillo(NEGOCIO, cemita).orElseThrow().activo()).isFalse();
    }

    @Test
    @DisplayName("un campo que el comando no conoce, o un valor ilegible, es COMANDO_INVALIDO")
    void camposInvalidos() {
        var cemita = platillo("Cemita poblana");

        assertThatThrownBy(() -> modulo.preparar(entrada(TODO), "darDeBajaPlatillo",
                Map.of("platilloId", cemita.toString(), "negocioId", UUID.randomUUID().toString())))
                .isInstanceOfSatisfying(ComandoGraphQlException.class,
                        e -> assertThat(e.codigo()).isEqualTo(ComandoGraphQlException.Codigo.COMANDO_INVALIDO));
        assertThatThrownBy(() -> modulo.preparar(entrada(TODO), "darDeBajaPlatillo", Map.of("platilloId", "no-es-uuid")))
                .isInstanceOfSatisfying(ComandoGraphQlException.class,
                        e -> assertThat(e.codigo()).isEqualTo(ComandoGraphQlException.Codigo.COMANDO_INVALIDO));
    }

    @Test
    @DisplayName("una variante que no es de Restaurantes responde COMANDO_NO_DISPONIBLE")
    void varianteAjena() {
        assertThatThrownBy(() -> modulo.preparar(entrada(TODO), "registrarVenta", Map.of()))
                .isInstanceOfSatisfying(ComandoGraphQlException.class,
                        e -> assertThat(e.codigo()).isEqualTo(ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE));
    }

    @Test
    @DisplayName("preparar y confirmar corren con la sesion de la peticion vinculada, y el vinculo se libera despues")
    void correnConLaSesionVinculada() {
        var vinculados = new java.util.ArrayList<String>();
        var enCurso = new boolean[1];
        var modulo = new ComandosDeRestaurantes(new ServicioPreparacion(new PreparacionesEnMemoria(),
                List.of(new ManejadorDarDeBajaPlatillo(menuAlmacen, new SinCuentas(), recetasAlmacen,
                        (contexto, permiso) -> enCurso[0], RELOJ)),
                RELOJ, fallo -> new RestaurantesException(CodigoError.valueOf(fallo.name()))),
                new VinculoDeSesion() {
                    @Override
                    public <T> T vincular(ContextoNegocio contexto, Autorizacion autorizacion,
                                          java.util.function.Supplier<T> accion) {
                        vinculados.add(contexto.negocioId() + "/" + autorizacion.puede(PermisosDeRestaurantes.MENU_EDITAR));
                        enCurso[0] = true;
                        try {
                            return accion.get();
                        } finally {
                            enCurso[0] = false;
                        }
                    }
                });
        var cemita = platillo("Cemita poblana");

        var preparada = modulo.preparar(entrada(TODO), "darDeBajaPlatillo", Map.of("platilloId", cemita.toString()));
        var resultado = modulo.confirmar(entrada(TODO), preparada.token());

        assertThat(resultado.ok()).isTrue();
        assertThat(vinculados).containsExactly(NEGOCIO + "/true", NEGOCIO + "/true");
    }
}
