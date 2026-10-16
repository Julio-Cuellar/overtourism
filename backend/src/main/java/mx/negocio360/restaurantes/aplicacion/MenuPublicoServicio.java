package mx.negocio360.restaurantes.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.Categoria;
import mx.negocio360.restaurantes.dominio.Platillo;

/**
 * El menu publico (PB-01 a PB-06, D-170): sin sesion y de solo lectura. Solo responde si el negocio es un restaurante
 * visible (publicado y activo); si no, responde nulo, igual que uno que no existe. Entrega solo los platillos publicados,
 * activos y en temporada, con el marcador de agotado, y solo los campos permitidos.
 */
public final class MenuPublicoServicio {

    /** Idioma del contenido hoy: no hay traducciones todavia (PB-02). */
    public static final String IDIOMA = "es-MX";
    /** Leyenda que acompaña a los precios (PB-05). */
    public static final String AVISO = "Precios y disponibilidad sujetos a cambio; verifica en el establecimiento.";
    /** Tope de platillos que se entregan (PB-04). */
    public static final int MAX_PLATILLOS = 200;

    private final RepositorioMenu menu;
    private final RepositorioRestaurantes restaurantes;
    private final Clock reloj;

    /**
     * Arma el servicio.
     *
     * @param menu         almacen del menu
     * @param restaurantes extension de restaurante, con su visibilidad
     * @param reloj        reloj de la plataforma
     */
    public MenuPublicoServicio(RepositorioMenu menu, RepositorioRestaurantes restaurantes, Clock reloj) {
        this.menu = Objects.requireNonNull(menu, "menu");
        this.restaurantes = Objects.requireNonNull(restaurantes, "restaurantes");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * El menu publico de un negocio.
     *
     * @param negocioId negocio consultado (es el objeto de la consulta, no el contexto de una sesion)
     * @param idioma    idioma pedido; hoy siempre se entrega en es-MX con {@code traducido = false}
     * @return el menu, o nulo si el negocio no es un restaurante visible
     */
    public MenuPublicoVista menuPublico(UUID negocioId, String idioma) {
        var visible = restaurantes.porNegocio(negocioId).filter(r -> r.visible()).isPresent();
        if (!visible) {
            return null;
        }
        var hoy = LocalDate.now(reloj.withZone(MenuServicio.ZONA));
        var publicados = menu.platillos(negocioId).stream()
                .filter(p -> p.activo() && p.publicado() && (p.temporada() == null || p.temporada().contiene(hoy)))
                .sorted(Comparator.comparing((Platillo p) -> p.nombre().toLowerCase(Locale.ROOT)).thenComparing(Platillo::id))
                .limit(MAX_PLATILLOS).toList();
        var categorias = menu.categorias(negocioId).stream().filter(Categoria::activa)
                .sorted(Comparator.comparingInt(Categoria::orden).thenComparing(c -> c.nombre().toLowerCase(Locale.ROOT)))
                .map(c -> new CategoriaPublicaVista(c.id(), c.nombre(),
                        publicados.stream().filter(p -> c.id().equals(p.categoriaId())).map(MenuPublicoServicio::vista).toList()))
                .filter(c -> !c.platillos().isEmpty()).toList();
        var conCategoriaActiva = categorias.stream().flatMap(c -> c.platillos().stream()).map(PlatilloPublicoVista::id).toList();
        List<PlatilloPublicoVista> sinCategoria = publicados.stream().map(MenuPublicoServicio::vista)
                .filter(v -> !conCategoriaActiva.contains(v.id())).toList();
        var actualizadoEn = publicados.stream().map(Platillo::actualizadoEn).max(Comparator.naturalOrder())
                .orElse(Instant.EPOCH);
        return new MenuPublicoVista(negocioId, "Menú", IDIOMA, categorias, sinCategoria, actualizadoEn, AVISO);
    }

    private static PlatilloPublicoVista vista(Platillo p) {
        return new PlatilloPublicoVista(p.id(), p.nombre(), p.descripcion(), p.precio(), p.etiquetas(), p.agotado(), false);
    }
}
