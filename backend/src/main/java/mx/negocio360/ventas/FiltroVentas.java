package mx.negocio360.ventas;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import mx.negocio360.ventas.dominio.EstadoVenta;

/**
 * Filtros de la consulta {@code ventas} (VF-02). Todos son opcionales y se combinan con Y. El negocio nunca va aqui: sale
 * de la sesion (SG-02).
 *
 * @param turnoId turno de Caja
 * @param estado  REGISTRADA o ANULADA
 * @param desde   inclusive
 * @param hasta   inclusive; con {@code desde} el rango no puede pasar de {@value #MAX_DIAS_DE_RANGO} dias
 * @param serie   serie del folio, de hasta {@value #MAX_CARACTERES_SERIE} caracteres
 * @param folio   folio dentro de la serie, desde 1
 */
public record FiltroVentas(UUID turnoId, EstadoVenta estado, Instant desde, Instant hasta, String serie, Long folio) {

    /** Dias maximos entre {@code desde} y {@code hasta}. */
    public static final int MAX_DIAS_DE_RANGO = 92;
    /** Largo maximo de la serie. */
    public static final int MAX_CARACTERES_SERIE = 60;

    /** Sin ningun filtro: todas las ventas del negocio. */
    public static final FiltroVentas TODAS = new FiltroVentas(null, null, null, null, null, null);

    /**
     * Dice si el filtro es aceptable.
     *
     * @return falso si el rango pasa de {@value #MAX_DIAS_DE_RANGO} dias o es al reves, la serie es demasiado larga
     *         o el folio es menor que 1
     */
    public boolean valido() {
        if (desde != null && hasta != null) {
            if (hasta.isBefore(desde) || Duration.between(desde, hasta).toDays() > MAX_DIAS_DE_RANGO) {
                return false;
            }
        }
        if (serie != null && (serie.isBlank() || serie.length() > MAX_CARACTERES_SERIE)) {
            return false;
        }
        return folio == null || folio >= 1;
    }
}
