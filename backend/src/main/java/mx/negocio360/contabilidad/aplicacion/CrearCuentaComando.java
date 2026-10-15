package mx.negocio360.contabilidad.aplicacion;

import java.util.UUID;
import mx.negocio360.contabilidad.dominio.Naturaleza;
import mx.negocio360.contabilidad.dominio.TipoCuenta;

/**
 * Crea una cuenta en el catalogo del negocio (C-02).
 *
 * @param codigo             codigo unico por negocio, por ejemplo {@code 601.02}
 * @param nombre             nombre visible
 * @param padreId            cuenta padre, o nula para una cuenta raiz
 * @param tipo               tipo; con padre, si se manda debe ser el del padre; sin padre, obligatorio
 * @param naturaleza         naturaleza; con padre, si se manda debe ser la del padre; sin padre, obligatoria
 * @param afectable          si recibe movimientos; nulo cuenta como verdadero
 * @param codigoAgrupadorSat codigo agrupador del SAT (P2), o nulo
 */
public record CrearCuentaComando(String codigo, String nombre, UUID padreId, TipoCuenta tipo, Naturaleza naturaleza,
                                 Boolean afectable, String codigoAgrupadorSat) implements Comando {
}
