package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.MonthDay;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EtiquetaPlatillo;
import mx.negocio360.restaurantes.dominio.Restaurante;
import mx.negocio360.restaurantes.dominio.Temporada;
import mx.negocio360.restaurantes.infraestructura.RestaurantesEnMemoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** PB-01 a PB-06: el menu que ve el turista, sin sesion. Solo lo permitido y solo si el negocio es visible. */
class MenuPublicoServicioTest {

    private final EntornoDeMenu e = new EntornoDeMenu();
    private final RestaurantesEnMemoria restaurantes = new RestaurantesEnMemoria();
    private final MenuPublicoServicio publico = new MenuPublicoServicio(e.menu, restaurantes, e.reloj);

    private void visible(UUID negocio) {
        restaurantes.guardar(Restaurante.nuevo(negocio, EntornoDeMenu.AHORA).conPublicado(true, EntornoDeMenu.AHORA));
    }

    private PlatilloVista publicado(String nombre, String precio) {
        var p = e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo(nombre, "Rico", new BigDecimal(precio), null,
                Set.of(EtiquetaPlatillo.TIPICO_POBLANO), null));
        return e.servicio.publicarPlatillo(e.ctxA, e.todo, p.id(), true);
    }

    @Test
    @DisplayName("PB-01: un negocio que no es restaurante, o no esta publicado, o esta suspendido o cerrado, responde nulo")
    void visibilidad() {
        publicado("Mole poblano", "120");
        assertThat(publico.menuPublico(EntornoDeMenu.NEGOCIO_A, null)).as("sin extension de restaurante").isNull();

        restaurantes.guardar(Restaurante.nuevo(EntornoDeMenu.NEGOCIO_A, EntornoDeMenu.AHORA));
        assertThat(publico.menuPublico(EntornoDeMenu.NEGOCIO_A, null)).as("sin publicar").isNull();

        restaurantes.guardar(Restaurante.nuevo(EntornoDeMenu.NEGOCIO_A, EntornoDeMenu.AHORA).conPublicado(true, EntornoDeMenu.AHORA).conEstado("SUSPENDIDO", EntornoDeMenu.AHORA));
        assertThat(publico.menuPublico(EntornoDeMenu.NEGOCIO_A, null)).as("suspendido").isNull();

        visible(EntornoDeMenu.NEGOCIO_A);
        assertThat(publico.menuPublico(EntornoDeMenu.NEGOCIO_A, null)).as("visible").isNotNull();
        assertThat(publico.menuPublico(UUID.randomUUID(), null)).as("negocio inexistente igual que uno oculto").isNull();
    }

    @Test
    @DisplayName("PB-01 y MN-09: solo salen los platillos publicados y activos; uno sin publicar o dado de baja no sale")
    void soloPublicados() {
        visible(EntornoDeMenu.NEGOCIO_A);
        publicado("Mole poblano", "120");
        e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Secreto", null, BigDecimal.TEN, null, Set.of(), null));
        var baja = publicado("Antiguo", "50");
        e.menu.reemplazar(e.menu.platillo(EntornoDeMenu.NEGOCIO_A, baja.id()).orElseThrow().conBaja(EntornoDeMenu.AHORA), baja.version());

        var menu = publico.menuPublico(EntornoDeMenu.NEGOCIO_A, null);

        assertThat(menu.sinCategoria()).extracting(PlatilloPublicoVista::nombre).containsExactly("Mole poblano");
    }

    @Test
    @DisplayName("MN-05 y MN-06: un agotado sale con su marca y uno fuera de temporada no sale")
    void agotadoYTemporada() {
        visible(EntornoDeMenu.NEGOCIO_A);
        var agotado = publicado("Mole poblano", "120");
        e.servicio.marcarAgotado(e.ctxA, e.todo, agotado.id(), true);
        var nogada = e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo("Chiles en nogada", null, new BigDecimal("260"), null, Set.of(),
                new Temporada(MonthDay.of(8, 1), MonthDay.of(9, 30))));
        e.servicio.publicarPlatillo(e.ctxA, e.todo, nogada.id(), true);

        var menu = publico.menuPublico(EntornoDeMenu.NEGOCIO_A, null);

        assertThat(menu.sinCategoria()).extracting(PlatilloPublicoVista::nombre).containsExactly("Mole poblano");
        assertThat(menu.sinCategoria().get(0).agotado()).isTrue();
    }

    @Test
    @DisplayName("agrupa por categoria en el orden del menu y deja aparte lo que no tiene categoria")
    void categorias() {
        visible(EntornoDeMenu.NEGOCIO_A);
        var bebidas = e.servicio.crearCategoria(e.ctxA, e.todo, new DatosCategoria("Bebidas", 2, Estacion.BARRA));
        var antojitos = e.servicio.crearCategoria(e.ctxA, e.todo, new DatosCategoria("Antojitos", 1, null));
        for (var datos : new Object[][] {{"Agua de horchata", bebidas.id()}, {"Cemita poblana", antojitos.id()}, {"Chalupas", antojitos.id()}, {"Suelto", null}}) {
            var p = e.servicio.crearPlatillo(e.ctxA, e.todo, new DatosPlatillo((String) datos[0], null, BigDecimal.TEN, (UUID) datos[1], Set.of(), null));
            e.servicio.publicarPlatillo(e.ctxA, e.todo, p.id(), true);
        }

        var menu = publico.menuPublico(EntornoDeMenu.NEGOCIO_A, null);

        assertThat(menu.categorias()).extracting(CategoriaPublicaVista::nombre).containsExactly("Antojitos", "Bebidas");
        assertThat(menu.categorias().get(0).platillos()).extracting(PlatilloPublicoVista::nombre).containsExactly("Cemita poblana", "Chalupas");
        assertThat(menu.sinCategoria()).extracting(PlatilloPublicoVista::nombre).containsExactly("Suelto");
    }

    @Test
    @DisplayName("una categoria sin platillos publicados no sale")
    void categoriaVacia() {
        visible(EntornoDeMenu.NEGOCIO_A);
        e.servicio.crearCategoria(e.ctxA, e.todo, new DatosCategoria("Vacia", 1, null));
        publicado("Mole poblano", "120");

        assertThat(publico.menuPublico(EntornoDeMenu.NEGOCIO_A, null).categorias()).isEmpty();
    }

    @Test
    @DisplayName("PB-03: la vista publica solo tiene los campos permitidos; ni costos, ni recetas, ni notas, ni modo de inventario")
    void camposPermitidos() {
        assertThat(nombres(PlatilloPublicoVista.class)).containsExactlyInAnyOrder("id", "nombre", "descripcion", "precio",
                "etiquetas", "agotado", "traducido");
        assertThat(nombres(CategoriaPublicaVista.class)).containsExactlyInAnyOrder("id", "nombre", "platillos");
        assertThat(nombres(MenuPublicoVista.class)).containsExactlyInAnyOrder("negocioId", "etiqueta", "idioma", "categorias",
                "sinCategoria", "actualizadoEn", "aviso");
    }

    private static java.util.List<String> nombres(Class<?> tipo) {
        return Arrays.stream(tipo.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    @Test
    @DisplayName("PB-02: sin traduccion, cualquier idioma sale en español con traducido falso; es-MX por omision")
    void idioma() {
        visible(EntornoDeMenu.NEGOCIO_A);
        publicado("Mole poblano", "120");

        var porOmision = publico.menuPublico(EntornoDeMenu.NEGOCIO_A, null);
        var ingles = publico.menuPublico(EntornoDeMenu.NEGOCIO_A, "en");

        assertThat(porOmision.idioma()).isEqualTo("es-MX");
        assertThat(porOmision.sinCategoria().get(0).traducido()).isFalse();
        assertThat(ingles.idioma()).as("se devuelve el idioma real del contenido").isEqualTo("es-MX");
        assertThat(ingles.sinCategoria().get(0).nombre()).isEqualTo("Mole poblano");
        assertThat(ingles.sinCategoria().get(0).traducido()).isFalse();
    }

    @Test
    @DisplayName("PB-05: el precio es el vigente y la vista trae la etiqueta Menu, el aviso y la fecha del ultimo cambio")
    void precioAvisoYFecha() {
        visible(EntornoDeMenu.NEGOCIO_A);
        var p = publicado("Mole poblano", "120");
        e.servicio.editarPlatillo(e.ctxA, e.todo, p.id(), p.version(), new DatosPlatillo("Mole poblano", "Rico", new BigDecimal("130"), null,
                Set.of(EtiquetaPlatillo.TIPICO_POBLANO), null));

        var menu = publico.menuPublico(EntornoDeMenu.NEGOCIO_A, null);

        assertThat(menu.etiqueta()).isEqualTo("Menú");
        assertThat(menu.aviso()).isEqualTo("Precios y disponibilidad sujetos a cambio; verifica en el establecimiento.");
        assertThat(menu.sinCategoria().get(0).precio()).isEqualByComparingTo("130.00");
        assertThat(menu.actualizadoEn()).isEqualTo(EntornoDeMenu.AHORA);
        assertThat(menu.sinCategoria().get(0).etiquetas()).containsExactly(EtiquetaPlatillo.TIPICO_POBLANO);
    }

    @Test
    @DisplayName("el menu de un negocio no trae platillos de otro")
    void aislamiento() {
        visible(EntornoDeMenu.NEGOCIO_A);
        visible(EntornoDeMenu.NEGOCIO_B);
        publicado("Mole poblano", "120");

        assertThat(publico.menuPublico(EntornoDeMenu.NEGOCIO_B, null).sinCategoria()).isEmpty();
    }
}
