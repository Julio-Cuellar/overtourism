package mx.negocio360.ventas.aplicacion;

import java.math.BigDecimal;
import mx.negocio360.compartido.dinero.DecimalPlano;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.Venta;

/**
 * Textos fijos en espanol de Mexico. La capa que responde a la persona usa estas plantillas y no improvisa texto;
 * toda cifra la calcula el codigo antes de llegar aqui.
 */
public final class PlantillasVentas {

    /** Mensaje al confirmar RegistrarVenta. */
    public static final String VENTA_REGISTRADA = "Venta registrada.";
    /** Mensaje al confirmar AnularVenta. */
    public static final String VENTA_ANULADA = "Venta anulada.";

    private PlantillasVentas() {
    }

    /**
     * Resumen que el cajero revisa antes de cobrar. Es determinista: confirmar lo recalcula y lo compara (VT-05).
     *
     * @param lineas    numero de renglones
     * @param subtotal  escala 2
     * @param descuento escala 2
     * @param total     escala 2
     * @param cambio    escala 2
     * @return texto del resumen
     */
    public static String resumenVenta(int lineas, BigDecimal subtotal, BigDecimal descuento, BigDecimal total,
                                      BigDecimal cambio) {
        return "Venta de %d %s. Subtotal %s, descuento %s, total %s. Cambio %s.".formatted(lineas,
                lineas == 1 ? "línea" : "líneas", texto(subtotal), texto(descuento), texto(total), texto(cambio));
    }

    /**
     * Resumen de una anulacion.
     *
     * @param venta venta que se anularia
     * @return texto con folio, total y lo que se revierte por metodo
     */
    public static String resumenAnulacion(Venta venta) {
        var pagos = venta.pagos().stream().map(p -> p.metodo().name() + " " + texto(p.monto()))
                .reduce((a, b) -> a + ", " + b).orElse("");
        return "Anular la venta %s-%d por %s. Se revierte: %s.".formatted(venta.serie(), venta.folio(),
                texto(venta.total()), pagos);
    }

    /**
     * Texto fijo de cada codigo de error.
     *
     * @param codigo codigo de error
     * @return mensaje para la persona
     */
    public static String de(CodigoError codigo) {
        return switch (codigo) {
            case VENTA_SIN_LINEAS -> "Agrega al menos un producto a la venta.";
            case CANTIDAD_INVALIDA -> "La cantidad debe ser mayor que cero y tener hasta 4 decimales.";
            case CANTIDAD_NO_FRACCIONABLE -> "Ese producto se vende por pieza: usa una cantidad entera.";
            case PRODUCTO_NO_ENCONTRADO -> "No encontramos ese producto en tu catálogo.";
            case PRODUCTO_INACTIVO -> "Ese producto ya no está a la venta.";
            case PAGOS_NO_CUADRAN -> "Los pagos no suman el total de la venta.";
            case EFECTIVO_INSUFICIENTE -> "El efectivo recibido no alcanza para ese pago.";
            case METODO_PAGO_INVALIDO -> "Revisa la forma de pago y su monto.";
            case CUPON_INVALIDO -> "Ese cupón no es válido.";
            case CUPON_YA_CANJEADO -> "Ese cupón ya se usó.";
            case CUPON_NO_APLICA -> "Ese cupón no aplica a esta venta.";
            case DESCUENTO_MAYOR_AL_SUBTOTAL -> "El descuento no puede ser mayor que la venta.";
            case TURNO_NO_ABIERTO -> "Abre un turno de caja antes de vender.";
            case VENTA_NO_ENCONTRADA -> "No encontramos esa venta.";
            case VENTA_YA_ANULADA -> "Esa venta ya estaba anulada.";
            case MOTIVO_REQUERIDO -> "Escribe el motivo de la anulación (hasta 200 caracteres).";
            case SIN_PERMISO_ANULAR -> "No tienes permiso para anular ventas.";
            case TOKEN_INVALIDO -> "La confirmación no es válida. Vuelve a empezar.";
            case TOKEN_EXPIRADO -> "La confirmación venció. Vuelve a empezar.";
            case TOKEN_USADO -> "Esa confirmación ya se usó.";
            case COMANDO_CAMBIO -> "Lo que ibas a confirmar cambió. Vuelve a empezar.";
            case CONCURRENCIA -> "Alguien más modificó esta venta. Recarga e inténtalo de nuevo.";
            case ERROR_TEMPORAL -> "Hubo un problema temporal. Inténtalo de nuevo en un momento.";
        };
    }

    private static String texto(BigDecimal valor) {
        return DecimalPlano.escribir(valor);
    }
}
