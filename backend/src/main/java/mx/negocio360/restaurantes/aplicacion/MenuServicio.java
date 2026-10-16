package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.dominio.Categoria;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.ModoInventario;
import mx.negocio360.restaurantes.dominio.Platillo;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/**
 * El menu del restaurante (MN-01 a MN-13): platillos y categorias. El negocio y el actor salen del contexto y cada operacion
 * exige su permiso, tambien las de lectura (AZ-04). Las dos barreras, la puerta de GraphQL y este servicio, piden lo mismo.
 *
 * <p>Los cambios usan bloqueo optimista con {@code version}: dos personas editando el mismo platillo no se pisan (MN-13).
 * Dar de baja un platillo es un comando aparte, con preparar y confirmar ({@link ManejadorDarDeBajaPlatillo}).
 */
public final class MenuServicio {

    /** Tope de platillos activos por negocio (MN-12), regla versionada mas adelante. */
    public static final int MAX_PLATILLOS = 200;
    /** Tope de una pagina de platillos (D-131). */
    public static final int MAX_PAGINA = 100;
    /** Zona del negocio para decidir si hoy esta en temporada (D-177); Negocios llevara la de cada sucursal. */
    static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private final RepositorioMenu menu;
    private final ConsultaDeRecetas recetas;
    private final Clock reloj;

    /**
     * Arma el servicio.
     *
     * @param menu    almacen del menu
     * @param recetas que platillos tienen receta (MN-04)
     * @param reloj   reloj de la plataforma
     */
    public MenuServicio(RepositorioMenu menu, ConsultaDeRecetas recetas, Clock reloj) {
        this.menu = Objects.requireNonNull(menu, "menu");
        this.recetas = Objects.requireNonNull(recetas, "recetas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ---- categorias ----

    /** Categorias activas, por orden y nombre. Exige {@code restaurantes.menu.ver}. */
    public List<CategoriaVista> categorias(ContextoNegocio contexto, Autorizacion autorizacion) {
        autorizacion.exigir(PermisosDeRestaurantes.MENU_VER);
        return menu.categorias(contexto.negocioId()).stream().filter(Categoria::activa)
                .sorted(Comparator.comparingInt(Categoria::orden).thenComparing(c -> c.nombre().toLowerCase(Locale.ROOT)))
                .map(CategoriaVista::de).toList();
    }

    /**
     * Crea una categoria (MN-07). Exige {@code restaurantes.menu.editar}.
     *
     * @throws RestaurantesException NOMBRE_INVALIDO, CONSULTA_INVALIDA (orden) o LIMITE_EXCEDIDO si ya hay 30
     */
    public CategoriaVista crearCategoria(ContextoNegocio contexto, Autorizacion autorizacion, DatosCategoria datos) {
        autorizacion.exigir(PermisosDeRestaurantes.MENU_EDITAR);
        var activas = menu.categorias(contexto.negocioId()).stream().filter(Categoria::activa).count();
        if (activas >= Categoria.MAX_POR_NEGOCIO) {
            throw new RestaurantesException(CodigoError.LIMITE_EXCEDIDO);
        }
        var ahora = reloj.instant();
        var nueva = Categoria.nueva(UuidV7.generar(ahora), contexto.negocioId(), datos.nombre(), datos.orden(),
                datos.estacion(), ahora);
        menu.insertar(nueva);
        return CategoriaVista.de(nueva);
    }

    /**
     * Edita una categoria con la version que se vio (MN-13). Exige {@code restaurantes.menu.editar}.
     *
     * @throws RestaurantesException CATEGORIA_NO_ENCONTRADA o CONCURRENCIA
     */
    public CategoriaVista editarCategoria(ContextoNegocio contexto, Autorizacion autorizacion, UUID id, long version,
                                          DatosCategoria datos) {
        autorizacion.exigir(PermisosDeRestaurantes.MENU_EDITAR);
        var actual = menu.categoria(contexto.negocioId(), id)
                .orElseThrow(() -> new RestaurantesException(CodigoError.CATEGORIA_NO_ENCONTRADA));
        if (actual.version() != version) {
            throw new RestaurantesException(CodigoError.CONCURRENCIA);
        }
        var nueva = actual.conDatos(datos.nombre(), datos.orden(), datos.estacion(), reloj.instant());
        if (!menu.reemplazar(nueva, actual.version())) {
            throw new RestaurantesException(CodigoError.CONCURRENCIA);
        }
        return CategoriaVista.de(nueva);
    }

    // ---- platillos: consultas ----

    /**
     * Platillos activos del negocio, por nombre, filtrados y paginados con cursor (D-131). {@code primeros} menor que 1 es
     * una consulta invalida y mas de 100 se recorta a 100. Exige {@code restaurantes.menu.ver}.
     *
     * @throws RestaurantesException CONSULTA_INVALIDA si {@code primeros} o el cursor no valen
     */
    public PaginaDePlatillos platillos(ContextoNegocio contexto, Autorizacion autorizacion, FiltroPlatillos filtro,
                                       int primeros, String despues) {
        autorizacion.exigir(PermisosDeRestaurantes.MENU_VER);
        if (primeros < 1) {
            throw new RestaurantesException(CodigoError.CONSULTA_INVALIDA);
        }
        var limite = Math.min(primeros, MAX_PAGINA);
        var hoy = hoy();
        var conReceta = recetas.platillosConReceta(contexto.negocioId());
        var texto = filtro.texto() == null ? null : filtro.texto().strip().toLowerCase(Locale.ROOT);
        Predicate<Platillo> pasa = p -> p.activo()
                && (texto == null || texto.isEmpty() || p.nombre().toLowerCase(Locale.ROOT).contains(texto))
                && (filtro.categoriaId() == null || filtro.categoriaId().equals(p.categoriaId()))
                && (!filtro.soloAgotados() || p.agotado())
                && (!filtro.soloFueraDeTemporada() || fueraDeTemporada(p, hoy))
                && (!filtro.soloSinReceta() || !conReceta.contains(p.id()));
        var ordenados = menu.platillos(contexto.negocioId()).stream().filter(pasa)
                .sorted(Comparator.comparing((Platillo p) -> p.nombre().toLowerCase(Locale.ROOT)).thenComparing(Platillo::id))
                .toList();
        var inicio = 0;
        if (despues != null) {
            var cursor = cursor(despues);
            var posicion = -1;
            for (int i = 0; i < ordenados.size(); i++) {
                if (ordenados.get(i).id().equals(cursor)) {
                    posicion = i;
                    break;
                }
            }
            if (posicion < 0) {
                throw new RestaurantesException(CodigoError.CONSULTA_INVALIDA);
            }
            inicio = posicion + 1;
        }
        var fin = Math.min(inicio + limite, ordenados.size());
        var nodos = ordenados.subList(inicio, fin).stream()
                .map(p -> PlatilloVista.de(p, conReceta.contains(p.id()), hoy)).toList();
        var siguiente = fin < ordenados.size() ? ordenados.get(fin - 1).id().toString() : null;
        return new PaginaDePlatillos(nodos, siguiente);
    }

    /**
     * Un platillo del negocio. Exige {@code restaurantes.menu.ver}.
     *
     * @throws RestaurantesException PLATILLO_NO_ENCONTRADO
     */
    public PlatilloVista platillo(ContextoNegocio contexto, Autorizacion autorizacion, UUID id) {
        autorizacion.exigir(PermisosDeRestaurantes.MENU_VER);
        return vista(contexto, buscar(contexto, id));
    }

    /** Conteos del menu y cobertura de recetas. Exige {@code restaurantes.menu.ver}. */
    public ResumenDeMenu resumenDeMenu(ContextoNegocio contexto, Autorizacion autorizacion) {
        autorizacion.exigir(PermisosDeRestaurantes.MENU_VER);
        var hoy = hoy();
        var conReceta = recetas.platillosConReceta(contexto.negocioId());
        var activos = menu.platillos(contexto.negocioId()).stream().filter(Platillo::activo).toList();
        var total = activos.size();
        var con = (int) activos.stream().filter(p -> conReceta.contains(p.id())).count();
        var cobertura = total == 0 ? Redondeo.dinero(BigDecimal.ZERO)
                : Redondeo.dinero(BigDecimal.valueOf(con).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 8, Redondeo.MODO));
        return new ResumenDeMenu(total, (int) activos.stream().filter(Platillo::agotado).count(),
                (int) activos.stream().filter(p -> fueraDeTemporada(p, hoy)).count(), con, total - con, cobertura);
    }

    // ---- platillos: escrituras ----

    /**
     * Crea un platillo (MN-01, MN-12). Exige {@code restaurantes.menu.editar}.
     *
     * @throws RestaurantesException NOMBRE_INVALIDO, NOMBRE_DUPLICADO, PRECIO_INVALIDO, DESCRIPCION_INVALIDA,
     *                               LIMITE_EXCEDIDO o CATEGORIA_NO_ENCONTRADA
     */
    public PlatilloVista crearPlatillo(ContextoNegocio contexto, Autorizacion autorizacion, DatosPlatillo datos) {
        autorizacion.exigir(PermisosDeRestaurantes.MENU_EDITAR);
        var activos = menu.platillos(contexto.negocioId()).stream().filter(Platillo::activo).toList();
        if (activos.size() >= MAX_PLATILLOS) {
            throw new RestaurantesException(CodigoError.LIMITE_EXCEDIDO);
        }
        exigirCategoria(contexto, datos.categoriaId());
        var ahora = reloj.instant();
        var nuevo = Platillo.nuevo(UuidV7.generar(ahora), contexto.negocioId(), datos.nombre(), datos.descripcion(),
                datos.precio(), datos.categoriaId(), datos.etiquetas(), datos.temporada(), ahora);
        exigirNombreLibre(activos, nuevo.nombre(), null);
        menu.insertar(nuevo);
        return vista(contexto, nuevo);
    }

    /**
     * Edita un platillo con la version que se vio (MN-13). Exige {@code restaurantes.menu.editar}.
     *
     * @throws RestaurantesException PLATILLO_NO_ENCONTRADO, CONCURRENCIA, NOMBRE_DUPLICADO o los de la validacion
     */
    public PlatilloVista editarPlatillo(ContextoNegocio contexto, Autorizacion autorizacion, UUID id, long version,
                                        DatosPlatillo datos) {
        autorizacion.exigir(PermisosDeRestaurantes.MENU_EDITAR);
        var actual = buscar(contexto, id);
        if (actual.version() != version) {
            throw new RestaurantesException(CodigoError.CONCURRENCIA);
        }
        exigirCategoria(contexto, datos.categoriaId());
        var ahora = reloj.instant();
        var editado = actual.editado(datos.nombre(), datos.descripcion(), datos.precio(), datos.categoriaId(),
                datos.etiquetas(), datos.temporada(), ahora);
        exigirNombreLibre(menu.platillos(contexto.negocioId()).stream().filter(Platillo::activo).toList(),
                editado.nombre(), id);
        return guardar(contexto, actual, editado);
    }

    /** Marca o desmarca agotado (MN-05). Exige {@code restaurantes.menu.agotar}, que tambien tiene el cocinero. */
    public PlatilloVista marcarAgotado(ContextoNegocio contexto, Autorizacion autorizacion, UUID id, boolean agotado) {
        autorizacion.exigir(PermisosDeRestaurantes.MENU_AGOTAR);
        var actual = buscar(contexto, id);
        return guardar(contexto, actual, actual.conAgotado(agotado, reloj.instant()));
    }

    /** Publica o quita del menu publico (MN-09). Exige {@code restaurantes.menu.editar}. */
    public PlatilloVista publicarPlatillo(ContextoNegocio contexto, Autorizacion autorizacion, UUID id, boolean publicar) {
        autorizacion.exigir(PermisosDeRestaurantes.MENU_EDITAR);
        var actual = buscar(contexto, id);
        return guardar(contexto, actual, actual.conPublicado(publicar, reloj.instant()));
    }

    /**
     * Cambia el modo de inventario (MN-04). Exige {@code restaurantes.menu.editar}.
     *
     * @throws RestaurantesException RECETA_REQUERIDA si pide RECETA sin receta activa, o MODO_INVENTARIO_CONFLICTO si
     *                               pide vinculo directo con una receta activa
     */
    public PlatilloVista cambiarModo(ContextoNegocio contexto, Autorizacion autorizacion, UUID id, ModoInventario modo) {
        autorizacion.exigir(PermisosDeRestaurantes.MENU_EDITAR);
        var actual = buscar(contexto, id);
        var conReceta = recetas.tieneRecetaActiva(contexto.negocioId(), id);
        if (modo == ModoInventario.RECETA && !conReceta) {
            throw new RestaurantesException(CodigoError.RECETA_REQUERIDA);
        }
        if (modo == ModoInventario.VINCULO_DIRECTO && conReceta) {
            throw new RestaurantesException(CodigoError.MODO_INVENTARIO_CONFLICTO);
        }
        return guardar(contexto, actual, actual.conModo(modo, reloj.instant()));
    }

    // ---- ayudas ----

    private Platillo buscar(ContextoNegocio contexto, UUID id) {
        return menu.platillo(contexto.negocioId(), id)
                .orElseThrow(() -> new RestaurantesException(CodigoError.PLATILLO_NO_ENCONTRADO));
    }

    private PlatilloVista guardar(ContextoNegocio contexto, Platillo anterior, Platillo nuevo) {
        if (!menu.reemplazar(nuevo, anterior.version())) {
            throw new RestaurantesException(CodigoError.CONCURRENCIA);
        }
        return vista(contexto, nuevo);
    }

    private PlatilloVista vista(ContextoNegocio contexto, Platillo platillo) {
        return PlatilloVista.de(platillo, recetas.tieneRecetaActiva(contexto.negocioId(), platillo.id()), hoy());
    }

    private void exigirCategoria(ContextoNegocio contexto, UUID categoriaId) {
        if (categoriaId != null && menu.categoria(contexto.negocioId(), categoriaId).filter(Categoria::activa).isEmpty()) {
            throw new RestaurantesException(CodigoError.CATEGORIA_NO_ENCONTRADA);
        }
    }

    private static void exigirNombreLibre(List<Platillo> activos, String nombre, UUID propio) {
        var buscado = nombre.toLowerCase(Locale.ROOT);
        var repetido = activos.stream().anyMatch(p -> !p.id().equals(propio) && p.nombre().toLowerCase(Locale.ROOT).equals(buscado));
        if (repetido) {
            throw new RestaurantesException(CodigoError.NOMBRE_DUPLICADO);
        }
    }

    private static boolean fueraDeTemporada(Platillo p, LocalDate hoy) {
        return p.temporada() != null && !p.temporada().contiene(hoy);
    }

    private static UUID cursor(String texto) {
        try {
            return UUID.fromString(texto);
        } catch (IllegalArgumentException e) {
            throw new RestaurantesException(CodigoError.CONSULTA_INVALIDA);
        }
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj.withZone(ZONA));
    }
}
