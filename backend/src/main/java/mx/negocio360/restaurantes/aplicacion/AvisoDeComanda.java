package mx.negocio360.restaurantes.aplicacion;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;

/**
 * Lo que la suscripcion {@code comandasDeCocina} entrega por cada evento de comanda (8.4 del diseno): la comanda enviada o
 * actualizada tal como la dice el evento, sin consultar nada. Es un aviso y no la verdad: la pantalla se corrige con la
 * consulta {@code comandas}. Una comanda enviada trae mesa, cuenta y lineas; una actualizada, su estado nuevo y las lineas
 * anuladas.
 *
 * @param tipo            enviada o actualizada
 * @param comandaId       la comanda
 * @param estacion        cocina o barra
 * @param estado          {@code NUEVA} si es recien enviada; el estado nuevo si es una actualizacion
 * @param numeroDeCuenta  solo en una comanda enviada
 * @param mesa            solo en una comanda enviada
 * @param lineas          solo en una comanda enviada
 * @param lineasAnuladas  solo en una actualizacion
 */
public record AvisoDeComanda(Tipo tipo, UUID comandaId, Estacion estacion, EstadoDeComanda estado, Integer numeroDeCuenta,
                             String mesa, List<Linea> lineas, List<UUID> lineasAnuladas) {

    /** Si la comanda acaba de enviarse o ya existia y cambio. */
    public enum Tipo {
        ENVIADA,
        ACTUALIZADA
    }

    /** Una linea de la comanda enviada. */
    public record Linea(String platillo, int cantidad, String nota, boolean marcaAlergia) {
    }

    public AvisoDeComanda {
        lineas = List.copyOf(lineas);
        lineasAnuladas = List.copyOf(lineasAnuladas);
    }

    /**
     * El aviso de un evento de comanda, o vacio si no es de comanda, no es de Restaurantes, es de una version que no conoce
     * este lector o viene mal formado.
     */
    public static Optional<AvisoDeComanda> de(SobreEvento evento) {
        if (!SalonServicio.ORIGEN.equals(evento.origen()) || evento.version() != 1) {
            return Optional.empty();
        }
        try {
            return switch (evento.tipo()) {
                case EventosDeRestaurantes.COMANDA_ENVIADA -> Optional.of(enviada(evento.datos()));
                case EventosDeRestaurantes.COMANDA_ACTUALIZADA -> Optional.of(actualizada(evento.datos()));
                default -> Optional.empty();
            };
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static AvisoDeComanda enviada(Map<String, Object> datos) {
        var lineas = ((List<?>) datos.get("lineas")).stream().map(f -> (Map<?, ?>) f)
                .map(f -> new Linea((String) f.get("platillo"), ((Number) f.get("cantidad")).intValue(),
                        (String) f.get("nota"), Boolean.TRUE.equals(f.get("marca_alergia")))).toList();
        return new AvisoDeComanda(Tipo.ENVIADA, UUID.fromString((String) datos.get("comanda_id")),
                Estacion.valueOf((String) datos.get("estacion")), EstadoDeComanda.NUEVA,
                ((Number) datos.get("numero")).intValue(), (String) datos.get("mesa"), lineas, List.of());
    }

    private static AvisoDeComanda actualizada(Map<String, Object> datos) {
        var anuladas = ((List<?>) datos.get("lineas_anuladas")).stream().map(id -> UUID.fromString((String) id)).toList();
        return new AvisoDeComanda(Tipo.ACTUALIZADA, UUID.fromString((String) datos.get("comanda_id")),
                Estacion.valueOf((String) datos.get("estacion")), EstadoDeComanda.valueOf((String) datos.get("estado")),
                null, null, List.of(), anuladas);
    }
}
