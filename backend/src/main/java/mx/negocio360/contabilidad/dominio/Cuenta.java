package mx.negocio360.contabilidad.dominio;

import java.util.UUID;

/**
 * Cuenta del catalogo de un negocio (tabla {@code cuenta}). Solo las hojas (afectables) reciben movimientos.
 *
 * @param id                 UUIDv7
 * @param negocioId          negocio duenio de la cuenta
 * @param codigo             codigo unico por negocio, por ejemplo {@code 101.01}
 * @param nombre             nombre visible
 * @param tipo               tipo de cuenta
 * @param naturaleza         lado en que aumenta el saldo
 * @param padreId            cuenta padre, o nulo en la raiz
 * @param afectable          si recibe movimientos (A-04)
 * @param deSistema          si la usa una regla automatica (C-04)
 * @param codigoAgrupadorSat codigo agrupador del SAT (P2), o nulo
 * @param estado             activa o inactiva
 * @param version            version para bloqueo optimista
 */
public record Cuenta(UUID id, UUID negocioId, String codigo, String nombre, TipoCuenta tipo, Naturaleza naturaleza,
                     UUID padreId, boolean afectable, boolean deSistema, String codigoAgrupadorSat,
                     EstadoCuenta estado, long version) {

    /**
     * Dice si la cuenta puede recibir movimientos: afectable y activa.
     *
     * @return verdadero si se puede cargar o abonar
     */
    public boolean recibeMovimientos() {
        return afectable && estado == EstadoCuenta.ACTIVA;
    }
}
