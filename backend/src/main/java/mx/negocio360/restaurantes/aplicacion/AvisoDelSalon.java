package mx.negocio360.restaurantes.aplicacion;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;

/**
 * Lo que la suscripcion {@code salonActualizado} entrega (8.4 del diseno): las alertas a meseros y caja (un pedido llego
 * por QR, un comensal pidio la cuenta), el cambio de mesas libres y la pausa automatica de un QR (QR-17). Es un aviso y no
 * la verdad: la pantalla se corrige con la consulta {@code salon}. Cada tipo llena solo sus campos; los demas van nulos.
 *
 * @param tipo           que paso
 * @param mesa           la mesa, en un pedido por QR, una cuenta pedida o un QR pausado
 * @param cuentaId       la cuenta, en un pedido por QR o una cuenta pedida
 * @param lineas         cuantas lineas trajo el pedido por QR
 * @param abrioLaCuenta  si el pedido por QR abrio la cuenta de la mesa
 * @param solicitadaPor  quien pidio la cuenta: {@code QR} o el actor
 * @param libres         mesas libres, en un cambio de disponibilidad
 * @param total          mesas contables, en un cambio de disponibilidad
 * @param abierto        si el servicio esta abierto, en un cambio de disponibilidad
 * @param hasta          hasta cuando esta pausado el QR de la mesa
 */
public record AvisoDelSalon(Tipo tipo, String mesa, UUID cuentaId, Integer lineas, Boolean abrioLaCuenta,
                            String solicitadaPor, Integer libres, Integer total, Boolean abierto, Instant hasta) {

    /** Que paso en el salon. */
    public enum Tipo {
        PEDIDO_QR,
        CUENTA_SOLICITADA,
        DISPONIBILIDAD,
        QR_PAUSADO
    }

    /**
     * El aviso de un evento del salon, o vacio si no lo es, no es de Restaurantes, es de una version que no conoce este lector
     * o viene mal formado.
     */
    public static Optional<AvisoDelSalon> de(SobreEvento evento) {
        if (!SalonServicio.ORIGEN.equals(evento.origen()) || evento.version() != 1) {
            return Optional.empty();
        }
        try {
            var d = evento.datos();
            return switch (evento.tipo()) {
                case EventosDeRestaurantes.PEDIDO_QR_RECIBIDO -> Optional.of(new AvisoDelSalon(Tipo.PEDIDO_QR,
                        (String) d.get("mesa"), UUID.fromString((String) d.get("cuenta_id")),
                        ((Number) d.get("lineas")).intValue(), (Boolean) d.get("abrio_la_cuenta"), null, null, null, null,
                        null));
                case EventosDeRestaurantes.CUENTA_SOLICITADA -> Optional.of(new AvisoDelSalon(Tipo.CUENTA_SOLICITADA,
                        (String) d.get("mesa"), UUID.fromString((String) d.get("cuenta_id")), null, null,
                        (String) d.get("solicitada_por"), null, null, null, null));
                case EventosDeRestaurantes.QR_PAUSADO -> Optional.of(new AvisoDelSalon(Tipo.QR_PAUSADO,
                        (String) d.get("mesa"), null, null, null, null, null, null, null,
                        Instant.parse((String) d.get("hasta"))));
                case SalonServicio.TIPO_DISPONIBILIDAD -> disponibilidad(d);
                default -> Optional.empty();
            };
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static Optional<AvisoDelSalon> disponibilidad(Map<String, Object> d) {
        if (!"MESAS".equals(d.get("tipo"))) {
            return Optional.empty();
        }
        return Optional.of(new AvisoDelSalon(Tipo.DISPONIBILIDAD, null, null, null, null, null,
                ((Number) d.get("libres")).intValue(), ((Number) d.get("total")).intValue(), (Boolean) d.get("abierto"),
                null));
    }
}
