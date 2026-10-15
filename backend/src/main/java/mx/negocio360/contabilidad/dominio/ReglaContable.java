package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Regla de contabilizacion (tabla {@code regla_contable}): que cuentas se cargan y abonan para un disparador y una
 * condicion. Las tasas fiscales llevan su fuente y fecha de verificacion (F-01, {@code CLAUDE.md}).
 *
 * @param id               UUIDv7
 * @param negocioId        negocio duenio
 * @param disparador       hecho que la activa
 * @param condicion        medio de pago o financiador, o nulo
 * @param cuentaCargoId    cuenta que se carga
 * @param cuentaAbonoId    cuenta que se abona
 * @param cuentaImpuestoId cuenta de IVA, o nula si no se desglosa
 * @param tasaIva          tasa de IVA, o nula si es exento
 * @param fuente           de donde salio la regla y cuando se verifico
 * @param vigenteDesde     inicio de vigencia
 * @param vigenteHasta     fin de vigencia, o nulo
 * @param version          version para bloqueo optimista
 */
public record ReglaContable(UUID id, UUID negocioId, Disparador disparador, String condicion, UUID cuentaCargoId,
                            UUID cuentaAbonoId, UUID cuentaImpuestoId, BigDecimal tasaIva, String fuente,
                            Instant vigenteDesde, Instant vigenteHasta, long version) {

    /**
     * Dice si la regla esta vigente en un instante.
     *
     * @param instante momento a evaluar
     * @return verdadero si {@code vigenteDesde <= instante < vigenteHasta}
     */
    public boolean vigenteEn(Instant instante) {
        return !instante.isBefore(vigenteDesde) && (vigenteHasta == null || instante.isBefore(vigenteHasta));
    }

    /**
     * Dice si la regla usa una cuenta en cualquiera de sus tres papeles.
     *
     * @param cuentaId cuenta a buscar
     * @return verdadero si es la de cargo, abono o impuesto
     */
    public boolean usaCuenta(UUID cuentaId) {
        return cuentaId.equals(cuentaCargoId) || cuentaId.equals(cuentaAbonoId) || cuentaId.equals(cuentaImpuestoId);
    }
}
