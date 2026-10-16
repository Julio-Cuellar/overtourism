package mx.negocio360.restaurantes.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.dinero.Redondeo;

/**
 * Un platillo del menu. Su {@code id} es el {@code producto_id} que Ventas conoce (D-162): unidad pieza, sin fraccion.
 * Es inmutable; cada cambio devuelve otro platillo con la version siguiente (bloqueo optimista, MN-13).
 *
 * <p>Agotado, temporada y publicado gobiernan el pedido en el salon y el menu publico, no el cobro: lo unico que Ventas
 * ve como «ya no se vende» es la baja logica (MN-14).
 *
 * @param id            producto_id de Ventas
 * @param negocioId     negocio dueño
 * @param categoriaId   categoria del menu, o nulo
 * @param nombre        2 a 80 caracteres
 * @param descripcion   hasta 300 caracteres, o nulo
 * @param precio        precio final al publico, mayor que 0, con dos decimales
 * @param modo          como descuenta inventario
 * @param agotado       marca manual
 * @param temporada     rango de mes y dia, o nulo si es de todo el año
 * @param publicado     si sale en el menu publico
 * @param activo        falso tras la baja logica
 * @param etiquetas     hasta 8, declaradas
 * @param version       para el bloqueo optimista, desde 1
 * @param creadoEn      cuando se creo
 * @param actualizadoEn ultimo cambio
 */
public record Platillo(UUID id, UUID negocioId, UUID categoriaId, String nombre, String descripcion, BigDecimal precio,
                       ModoInventario modo, boolean agotado, Temporada temporada, boolean publicado, boolean activo,
                       Set<EtiquetaPlatillo> etiquetas, long version, Instant creadoEn, Instant actualizadoEn) {

    /** Tope de caracteres del nombre. */
    public static final int MAX_NOMBRE = 80;
    /** Tope de caracteres de la descripcion. */
    public static final int MAX_DESCRIPCION = 300;
    /** Tope de etiquetas por platillo (MN-08). */
    public static final int MAX_ETIQUETAS = 8;
    /** Precio maximo (MN-01). */
    public static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999.99");

    public Platillo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(modo, "modo");
        nombre = nombreValido(nombre);
        descripcion = descripcionValida(descripcion);
        precio = precioValido(precio);
        etiquetas = etiquetas == null || etiquetas.isEmpty() ? Set.of() : Set.copyOf(etiquetas);
        if (etiquetas.size() > MAX_ETIQUETAS) {
            throw new RestaurantesException(CodigoError.LIMITE_EXCEDIDO);
        }
        Objects.requireNonNull(creadoEn, "creadoEn");
        Objects.requireNonNull(actualizadoEn, "actualizadoEn");
    }

    /**
     * Un platillo nuevo: activo, sin publicar, sin agotar, sin receta y en la version 1.
     *
     * @throws RestaurantesException NOMBRE_INVALIDO, DESCRIPCION_INVALIDA o PRECIO_INVALIDO
     */
    public static Platillo nuevo(UUID id, UUID negocioId, String nombre, String descripcion, BigDecimal precio,
                                 UUID categoriaId, Instant ahora) {
        return new Platillo(id, negocioId, categoriaId, nombre, descripcion, precio, ModoInventario.NINGUNO, false, null,
                false, true, Set.of(), 1, ahora, ahora);
    }

    /**
     * Un platillo nuevo con sus etiquetas y su temporada, en la version 1 (un solo alta, no tres cambios).
     *
     * @throws RestaurantesException NOMBRE_INVALIDO, DESCRIPCION_INVALIDA, PRECIO_INVALIDO o LIMITE_EXCEDIDO
     */
    public static Platillo nuevo(UUID id, UUID negocioId, String nombre, String descripcion, BigDecimal precio,
                                 UUID categoriaId, Set<EtiquetaPlatillo> etiquetas, Temporada temporada, Instant ahora) {
        return new Platillo(id, negocioId, categoriaId, nombre, descripcion, precio, ModoInventario.NINGUNO, false,
                temporada, false, true, etiquetas, 1, ahora, ahora);
    }

    /** Edita todos los datos que escribe la persona en un solo cambio de version. */
    public Platillo editado(String nuevoNombre, String nuevaDescripcion, BigDecimal nuevoPrecio, UUID categoria,
                            Set<EtiquetaPlatillo> nuevasEtiquetas, Temporada nuevaTemporada, Instant ahora) {
        return cambiado(categoria, nuevoNombre, nuevaDescripcion, nuevoPrecio, modo, agotado, nuevaTemporada, publicado,
                activo, nuevasEtiquetas, ahora);
    }

    private Platillo cambiado(UUID categoria, String nuevoNombre, String nuevaDescripcion, BigDecimal nuevoPrecio,
                              ModoInventario nuevoModo, boolean nuevoAgotado, Temporada nuevaTemporada,
                              boolean nuevoPublicado, boolean nuevoActivo, Set<EtiquetaPlatillo> nuevasEtiquetas,
                              Instant ahora) {
        return new Platillo(id, negocioId, categoria, nuevoNombre, nuevaDescripcion, nuevoPrecio, nuevoModo, nuevoAgotado,
                nuevaTemporada, nuevoPublicado, nuevoActivo, nuevasEtiquetas, version + 1, creadoEn, ahora);
    }

    /** Edita nombre, descripcion y precio, y vuelve a validarlos. */
    public Platillo conDatos(String nuevoNombre, String nuevaDescripcion, BigDecimal nuevoPrecio, Instant ahora) {
        return cambiado(categoriaId, nuevoNombre, nuevaDescripcion, nuevoPrecio, modo, agotado, temporada, publicado,
                activo, etiquetas, ahora);
    }

    /** Cambia la categoria del menu (nulo la quita). */
    public Platillo conCategoria(UUID categoria, Instant ahora) {
        return cambiado(categoria, nombre, descripcion, precio, modo, agotado, temporada, publicado, activo, etiquetas,
                ahora);
    }

    /** Reemplaza las etiquetas. @throws RestaurantesException LIMITE_EXCEDIDO si son mas de 8 */
    public Platillo conEtiquetas(Set<EtiquetaPlatillo> nuevas, Instant ahora) {
        return cambiado(categoriaId, nombre, descripcion, precio, modo, agotado, temporada, publicado, activo,
nuevas, ahora);
    }

    /** Marca o desmarca agotado (MN-05). */
    public Platillo conAgotado(boolean nuevo, Instant ahora) {
        return cambiado(categoriaId, nombre, descripcion, precio, modo, nuevo, temporada, publicado, activo, etiquetas,
                ahora);
    }

    /** Pone o quita la temporada (MN-06). */
    public Platillo conTemporada(Temporada nueva, Instant ahora) {
        return cambiado(categoriaId, nombre, descripcion, precio, modo, agotado, nueva, publicado, activo, etiquetas,
                ahora);
    }

    /** Publica o quita del menu publico (MN-09). */
    public Platillo conPublicado(boolean nuevo, Instant ahora) {
        return cambiado(categoriaId, nombre, descripcion, precio, modo, agotado, temporada, nuevo, activo, etiquetas,
                ahora);
    }

    /** Cambia el modo de inventario. La regla de que sea compatible con la receta la aplica el servicio (MN-04). */
    public Platillo conModo(ModoInventario nuevo, Instant ahora) {
        return cambiado(categoriaId, nombre, descripcion, precio, nuevo, agotado, temporada, publicado, activo, etiquetas,
                ahora);
    }

    /** Baja logica (MN-10): deja de venderse y de pedirse; el registro se conserva. */
    public Platillo conBaja(Instant ahora) {
        return cambiado(categoriaId, nombre, descripcion, precio, modo, agotado, temporada, false, false, etiquetas, ahora);
    }

    /** Verdadero si se puede agregar a una cuenta ese dia: activo, no agotado y en temporada. */
    public boolean ordenable(LocalDate hoy) {
        return activo && !agotado && (temporada == null || temporada.contiene(hoy));
    }

    /**
     * Falla si no se puede pedir.
     *
     * @throws RestaurantesException PLATILLO_NO_ORDENABLE (dado de baja), PLATILLO_AGOTADO o FUERA_DE_TEMPORADA
     */
    public void exigirOrdenable(LocalDate hoy) {
        if (!activo) {
            throw new RestaurantesException(CodigoError.PLATILLO_NO_ORDENABLE);
        }
        if (agotado) {
            throw new RestaurantesException(CodigoError.PLATILLO_AGOTADO);
        }
        if (temporada != null && !temporada.contiene(hoy)) {
            throw new RestaurantesException(CodigoError.FUERA_DE_TEMPORADA);
        }
    }

    /** Valida y recorta el nombre: de 2 a 80 caracteres. */
    static String nombreValido(String nombre) {
        var recortado = nombre == null ? "" : nombre.strip();
        if (recortado.length() < 2 || recortado.length() > MAX_NOMBRE) {
            throw new RestaurantesException(CodigoError.NOMBRE_INVALIDO);
        }
        return recortado;
    }

    private static String descripcionValida(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        var recortada = descripcion.strip();
        if (recortada.length() > MAX_DESCRIPCION) {
            throw new RestaurantesException(CodigoError.DESCRIPCION_INVALIDA);
        }
        return recortada;
    }

    private static BigDecimal precioValido(BigDecimal precio) {
        if (precio == null) {
            throw new RestaurantesException(CodigoError.PRECIO_INVALIDO);
        }
        var redondeado = Redondeo.dinero(precio);
        if (redondeado.signum() <= 0 || redondeado.compareTo(PRECIO_MAXIMO) > 0) {
            throw new RestaurantesException(CodigoError.PRECIO_INVALIDO);
        }
        return redondeado;
    }
}
