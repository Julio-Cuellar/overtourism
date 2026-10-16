package mx.negocio360.caja.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Arqueo con el que se cierra un turno: esperado contra contado (CJ-12). Inmutable una vez confirmado (CJ-09).
 *
 * @param id                 UUIDv7
 * @param negocioId          negocio dueño
 * @param turnoId            turno que cierra
 * @param cajaId             caja del turno
 * @param esperadoPorMetodo  suma de movimientos por metodo, escala 2
 * @param contadoPorMetodo   lo que conto la persona, escala 2
 * @param diferenciaEfectivo contado − esperado en efectivo
 * @param autorizadoPor      encargado que autorizo una diferencia sobre el umbral (CJ-08), o nulo
 * @param abiertoEn          inicio del turno
 * @param confirmadoEn       cuando se confirmo el corte
 */
public record Corte(UUID id, UUID negocioId, UUID turnoId, UUID cajaId, Map<MetodoPago, BigDecimal> esperadoPorMetodo,
                    Map<MetodoPago, BigDecimal> contadoPorMetodo, BigDecimal diferenciaEfectivo, String autorizadoPor,
                    Instant abiertoEn, Instant confirmadoEn) {

    /** Exige los ids y la diferencia, y copia los montos para que el corte sea inmutable (CJ-09). */
    public Corte {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(turnoId, "turnoId");
        Objects.requireNonNull(diferenciaEfectivo, "diferenciaEfectivo");
        esperadoPorMetodo = Map.copyOf(new EnumMap<>(esperadoPorMetodo));
        contadoPorMetodo = Map.copyOf(new EnumMap<>(contadoPorMetodo));
    }
}
