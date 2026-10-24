package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;
import mx.negocio360.restaurantes.dominio.EstadoDeLinea;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.LineaDeCuenta;
import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;

/**
 * Una linea tal como la ve el personal. {@code estadoDeComanda} dice como va en su estacion; {@code mensajeDelMotivo} es el
 * texto fijo del motivo de anulacion, el mismo que ve el comensal (QR-10).
 */
public record LineaVista(UUID id, UUID platilloId, String nombre, int cantidad, BigDecimal precio, BigDecimal subtotal,
                         String nota, boolean marcaAlergia, EstadoDeLinea estado, Estacion estacion,
                         EstadoDeComanda estadoDeComanda, MotivoDeAnulacion motivo, String mensajeDelMotivo, Instant anuladaEn) {

    /** La vista de una linea con el estado de su comanda. */
    public static LineaVista de(LineaDeCuenta linea, EstadoDeComanda estadoDeComanda) {
        return new LineaVista(linea.id(), linea.platilloId(), linea.nombre(), linea.cantidad(), linea.precio(),
                linea.subtotal(), linea.nota(), linea.marcaAlergia(), linea.estado(), linea.estacion(), estadoDeComanda,
                linea.motivo(), MensajeDeMotivo.de(linea.motivo(), linea.textoDelMotivo()), linea.anuladaEn());
    }
}
