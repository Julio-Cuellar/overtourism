package mx.negocio360.inventario.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import mx.negocio360.compartido.dinero.Redondeo;

/**
 * Articulo de inventario (tabla {@code articulo} mas sus filas de {@code unidad_articulo}). Inmutable: cada cambio
 * produce otra instancia con la version siguiente, que es la base del bloqueo optimista.
 *
 * <p>La unidad base no se puede cambiar (U-01): no hay ningun metodo que lo haga. Todo saldo y movimiento se guarda en
 * ella.
 *
 * @param rastreo         como se rastrea (L-01); hoy los articulos nuevos solo pueden ser {@code NINGUNO}
 * @param unidadesPropias nombre canonico de la unidad propia y su factor a la unidad base (U-03)
 * @param minimoBase      minimo en unidad base; las alertas comparan contra el (A-02)
 * @param activo          falso tras la baja logica (A-04)
 * @param version         contador de bloqueo optimista, desde 1
 */
public record Articulo(UUID id, UUID negocioId, String nombre, String categoria, UnidadEstandar unidadBase,
                       Rastreo rastreo, Map<String, BigDecimal> unidadesPropias, BigDecimal minimoBase, boolean activo,
                       long version, Instant creadoEn, Instant actualizadoEn, String creadoPor) {

    public static final long VERSION_INICIAL = 1L;
    public static final int MAX_NOMBRE = 80;
    public static final int MAX_CATEGORIA = 40;
    public static final int MAX_UNIDADES_PROPIAS = 20;
    private static final Pattern NOMBRE_DE_UNIDAD = Pattern.compile("\\p{L}[\\p{L} ]{0,19}");

    public Articulo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(unidadBase, "unidadBase");
        Objects.requireNonNull(rastreo, "rastreo");
        Objects.requireNonNull(minimoBase, "minimoBase");
        Objects.requireNonNull(creadoEn, "creadoEn");
        Objects.requireNonNull(actualizadoEn, "actualizadoEn");
        Objects.requireNonNull(creadoPor, "creadoPor");
        if (version < VERSION_INICIAL) {
            throw new IllegalArgumentException("La version del articulo debe ser 1 o mayor: " + version);
        }
        unidadesPropias = copiarUnidades(Objects.requireNonNull(unidadesPropias, "unidadesPropias"));
        minimoBase = Redondeo.cantidad(minimoBase);
    }

    /** Articulo nuevo sin rastreo. */
    public static Articulo nuevo(UUID id, UUID negocioId, String nombre, String categoria, UnidadEstandar unidadBase,
                                 Map<String, BigDecimal> unidadesPropias, BigDecimal minimoBase, String creadoPor,
                                 Instant ahora) {
        return nuevo(id, negocioId, nombre, categoria, unidadBase, unidadesPropias, minimoBase, creadoPor, ahora,
                Rastreo.NINGUNO);
    }

    /** Articulo nuevo con el rastreo indicado. */
    public static Articulo nuevo(UUID id, UUID negocioId, String nombre, String categoria, UnidadEstandar unidadBase,
                                 Map<String, BigDecimal> unidadesPropias, BigDecimal minimoBase, String creadoPor,
                                 Instant ahora, Rastreo rastreo) {
        return new Articulo(id, negocioId, nombre, categoria, unidadBase, rastreo, unidadesPropias, minimoBase, true,
                VERSION_INICIAL, ahora, ahora, creadoPor);
    }

    /** El mismo articulo con otro nombre, categoria y minimo, y la version siguiente. */
    public Articulo conDatos(String nuevoNombre, String nuevaCategoria, BigDecimal nuevoMinimo, Instant ahora) {
        return new Articulo(id, negocioId, nuevoNombre, nuevaCategoria, unidadBase, rastreo, unidadesPropias, nuevoMinimo,
                activo, version + 1, creadoEn, ahora, creadoPor);
    }

    /** El mismo articulo con una unidad propia mas (U-03). Falla si ya existe una con ese nombre. */
    public Articulo conUnidadPropia(String nombreUnidad, BigDecimal factor, Instant ahora) {
        var clave = nombreDeUnidadValido(nombreUnidad);
        if (unidadesPropias.containsKey(clave)) {
            throw new InventarioException(CodigoError.UNIDAD_PROPIA_INVALIDA);
        }
        var nuevas = new TreeMap<>(unidadesPropias);
        nuevas.put(clave, factor);
        return new Articulo(id, negocioId, nombre, categoria, unidadBase, rastreo, nuevas, minimoBase, activo, version + 1,
                creadoEn, ahora, creadoPor);
    }

    /** Baja logica (A-04). Quien llama verifica antes que el saldo sea cero. */
    public Articulo dadoDeBaja(Instant ahora) {
        return new Articulo(id, negocioId, nombre, categoria, unidadBase, rastreo, unidadesPropias, minimoBase, false,
                version + 1, creadoEn, ahora, creadoPor);
    }

    /**
     * Lleva una cantidad a la unidad base (U-02 a U-04). Busca primero la unidad propia y luego la estandar; una
     * estandar solo convierte dentro de la dimension de la base. Nunca adivina: lo que no convierte falla con
     * UNIDAD_NO_CONVERTIBLE. No valida el signo de la cantidad: eso depende de la operacion.
     */
    public Conversion aBase(BigDecimal cantidad, String unidad) {
        if (cantidad == null || !Limites.cantidadEnRango(cantidad)) {
            throw new InventarioException(CodigoError.CANTIDAD_INVALIDA);
        }
        if (unidad == null || unidad.isBlank()) {
            throw new InventarioException(CodigoError.UNIDAD_NO_CONVERTIBLE);
        }
        var clave = UnidadEstandar.normalizar(unidad);
        var factor = unidadesPropias.get(clave);
        if (factor == null) {
            var estandar = UnidadEstandar.deCodigo(clave)
                    .filter(u -> u.dimension() == unidadBase.dimension())
                    .orElseThrow(() -> new InventarioException(CodigoError.UNIDAD_NO_CONVERTIBLE));
            factor = Redondeo.dividir(estandar.factorADimension(), unidadBase.factorADimension(),
                    Redondeo.ESCALA_FACTOR);
        }
        return new Conversion(cantidad, clave, factor, Redondeo.cantidad(cantidad.multiply(factor)));
    }

    /** Nombre sin espacios de los extremos, de 1 a 80 caracteres y sin caracteres invisibles o de control. */
    public static String nombreValido(String texto) {
        if (texto == null) {
            throw new InventarioException(CodigoError.NOMBRE_INVALIDO);
        }
        var limpio = texto.strip();
        if (limpio.isEmpty() || limpio.length() > MAX_NOMBRE || !imprimible(limpio)) {
            throw new InventarioException(CodigoError.NOMBRE_INVALIDO);
        }
        return limpio;
    }

    /** Categoria opcional: nula o en blanco es "sin categoria"; si trae texto, hasta 40 caracteres imprimibles. */
    public static String categoriaValida(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        var limpio = texto.strip();
        if (limpio.length() > MAX_CATEGORIA || !imprimible(limpio)) {
            throw new InventarioException(CodigoError.CATEGORIA_INVALIDA);
        }
        return limpio;
    }

    private static Map<String, BigDecimal> copiarUnidades(Map<String, BigDecimal> origen) {
        if (origen.size() > MAX_UNIDADES_PROPIAS) {
            throw new InventarioException(CodigoError.UNIDAD_PROPIA_INVALIDA);
        }
        var copia = new TreeMap<String, BigDecimal>();
        for (var entrada : origen.entrySet()) {
            var clave = nombreDeUnidadValido(entrada.getKey());
            if (copia.put(clave, factorValido(entrada.getValue())) != null) {
                throw new InventarioException(CodigoError.UNIDAD_PROPIA_INVALIDA);
            }
        }
        return Collections.unmodifiableMap(copia);
    }

    /** U-03: una letra y hasta 19 letras o espacios mas, y no puede llamarse como una unidad estandar. */
    private static String nombreDeUnidadValido(String texto) {
        if (texto == null) {
            throw new InventarioException(CodigoError.UNIDAD_PROPIA_INVALIDA);
        }
        var clave = UnidadEstandar.normalizar(texto);
        if (!NOMBRE_DE_UNIDAD.matcher(clave).matches() || UnidadEstandar.deCodigo(clave).isPresent()) {
            throw new InventarioException(CodigoError.UNIDAD_PROPIA_INVALIDA);
        }
        return clave;
    }

    /** U-03: el factor es mayor que cero, con hasta 8 decimales y dentro del rango de la columna. */
    private static BigDecimal factorValido(BigDecimal factor) {
        if (factor == null || factor.signum() <= 0 || factor.compareTo(Limites.MAX_FACTOR) > 0
                || factor.stripTrailingZeros().scale() > Limites.MAX_DECIMALES_FACTOR) {
            throw new InventarioException(CodigoError.UNIDAD_PROPIA_INVALIDA);
        }
        return Redondeo.factor(factor);
    }

    private static boolean imprimible(String texto) {
        return texto.codePoints().noneMatch(c -> switch (Character.getType(c)) {
            case Character.CONTROL, Character.FORMAT, Character.PRIVATE_USE, Character.SURROGATE,
                 Character.UNASSIGNED, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR -> true;
            default -> false;
        });
    }
}
