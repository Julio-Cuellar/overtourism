package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/**
 * Los limites del pedido por QR (QR-15, QR-16, QR-17). Son parametros y no constantes porque el diseno los quiere como
 * regla versionada; mientras llega el motor de reglas, se dan al armar el servicio y los valores por omision son los del
 * diseno.
 *
 * @param lineasPorPedido     lineas que cabe en un pedido
 * @param cantidadPorLinea    piezas de una linea
 * @param lineasPorCuenta     lineas vivas que puede tener una cuenta abierta por QR
 * @param montoPorCuenta      monto estimado maximo de una cuenta abierta por QR
 * @param pedidosPorVentana   pedidos aceptados por token y por dispositivo en una ventana
 * @param ventanaDeTasa       el largo de esa ventana
 * @param cancelacionesParaPausar cuentas por QR canceladas por {@code SIN_COMENSALES} que pausan la mesa
 * @param ventanaDeBromas     dentro de cuanto tiempo se cuentan
 * @param retencionDeLlaves   cuanto se recuerda un pedido para repetirlo sin duplicar (CT-14)
 */
public record LimitesDeQr(int lineasPorPedido, int cantidadPorLinea, int lineasPorCuenta, BigDecimal montoPorCuenta,
                          int pedidosPorVentana, Duration ventanaDeTasa, int cancelacionesParaPausar,
                          Duration ventanaDeBromas, Duration retencionDeLlaves) {

    /** Los valores del diseno. */
    public static final LimitesDeQr PREDETERMINADOS = new LimitesDeQr(20, 10, 60, new BigDecimal("3000.00"), 5,
            Duration.ofMinutes(1), 3, Duration.ofHours(1), Duration.ofHours(24));

    public LimitesDeQr {
        Objects.requireNonNull(montoPorCuenta, "montoPorCuenta");
        Objects.requireNonNull(ventanaDeTasa, "ventanaDeTasa");
        Objects.requireNonNull(ventanaDeBromas, "ventanaDeBromas");
        Objects.requireNonNull(retencionDeLlaves, "retencionDeLlaves");
        if (lineasPorPedido < 1 || cantidadPorLinea < 1 || lineasPorCuenta < 1 || pedidosPorVentana < 1
                || cancelacionesParaPausar < 1 || montoPorCuenta.signum() <= 0) {
            throw new IllegalArgumentException("Los limites del QR deben ser positivos.");
        }
    }
}
