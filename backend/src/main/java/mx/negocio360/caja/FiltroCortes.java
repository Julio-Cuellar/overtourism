package mx.negocio360.caja;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Filtros de la consulta {@code cortes} (CF-03). Todos son opcionales y se combinan con Y. El negocio nunca va aqui:
 * sale de la sesion (SG-02).
 *
 * @param cajaId caja
 * @param desde  inclusive, sobre la fecha de confirmacion del corte
 * @param hasta  inclusive; con {@code desde} el rango no puede pasar de {@value #MAX_DIAS_DE_RANGO} dias
 */
public record FiltroCortes(UUID cajaId, Instant desde, Instant hasta) {

    /** Dias maximos entre {@code desde} y {@code hasta}. */
    public static final int MAX_DIAS_DE_RANGO = 92;

    /** Sin ningun filtro: todos los cortes del negocio. */
    public static final FiltroCortes TODOS = new FiltroCortes(null, null, null);

    /**
     * Dice si el filtro es aceptable.
     *
     * @return falso si el rango es al reves o pasa de {@value #MAX_DIAS_DE_RANGO} dias
     */
    public boolean valido() {
        if (desde != null && hasta != null) {
            return !hasta.isBefore(desde) && Duration.between(desde, hasta).toDays() <= MAX_DIAS_DE_RANGO;
        }
        return true;
    }
}
