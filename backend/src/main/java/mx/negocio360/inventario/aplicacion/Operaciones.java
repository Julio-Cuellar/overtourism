package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.dominio.Articulo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.dominio.Limites;

/** Validaciones y formatos que comparten los manejadores. */
final class Operaciones {

    private Operaciones() {
    }

    /** Decimal sin ceros sobrantes ni notacion cientifica, para los resumenes: 500.0000 se escribe 500. */
    static String plano(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    /** El resumen que vio la persona debe ser el que se calcula ahora; si no, el mundo cambio (6.2). */
    static void exigirMismoResumen(String preparado, String actual) {
        if (!actual.equals(preparado)) {
            throw new InventarioException(CodigoError.COMANDO_CAMBIO);
        }
    }

    static Articulo existente(RepositorioArticulos articulos, ContextoNegocio contexto, UUID articuloId) {
        if (articuloId == null) {
            throw new InventarioException(CodigoError.ARTICULO_NO_ENCONTRADO);
        }
        return articulos.porId(contexto.negocioId(), articuloId)
                .orElseThrow(() -> new InventarioException(CodigoError.ARTICULO_NO_ENCONTRADO));
    }

    static Articulo activo(RepositorioArticulos articulos, ContextoNegocio contexto, UUID articuloId) {
        var articulo = existente(articulos, contexto, articuloId);
        if (!articulo.activo()) {
            throw new InventarioException(CodigoError.ARTICULO_INACTIVO, articulo.id());
        }
        return articulo;
    }

    /** El nombre es unico entre los articulos activos del negocio; {@code propio} es el articulo que se edita. */
    static void exigirNombreLibre(RepositorioArticulos articulos, UUID negocioId, String nombre, UUID propio) {
        articulos.activoPorNombre(negocioId, nombre).filter(otro -> !otro.id().equals(propio)).ifPresent(otro -> {
            throw new InventarioException(CodigoError.NOMBRE_DUPLICADO, otro.id());
        });
    }

    /** El minimo capturado en una unidad, llevado a unidad base. Sin unidad se entiende en la unidad base. */
    static BigDecimal minimoEnBase(Articulo articulo, BigDecimal minimo, String unidad) {
        exigirCantidadCapturada(minimo, true);
        var unidadUsada = unidad == null || unidad.isBlank() ? articulo.unidadBase().codigo() : unidad;
        var base = articulo.aBase(minimo, unidadUsada).cantidadBase();
        if (!Limites.cantidadEnRango(base)) {
            throw new InventarioException(CodigoError.CANTIDAD_INVALIDA);
        }
        return base;
    }

    /** Una cantidad capturada existe, no es negativa (ni cero, salvo {@code permiteCero}) y lleva hasta 4 decimales. */
    static void exigirCantidadCapturada(BigDecimal cantidad, boolean permiteCero) {
        if (cantidad == null || !Limites.cantidadEnRango(cantidad) || cantidad.signum() < 0
                || (!permiteCero && cantidad.signum() == 0)
                || cantidad.stripTrailingZeros().scale() > Limites.MAX_DECIMALES_CAPTURA) {
            throw new InventarioException(CodigoError.CANTIDAD_INVALIDA);
        }
    }
}
