package mx.negocio360.contabilidad.aplicacion;

import java.util.UUID;
import mx.negocio360.contabilidad.dominio.Naturaleza;
import mx.negocio360.contabilidad.dominio.TipoCuenta;

/**
 * Edita una cuenta del catalogo. Los campos nulos no cambian. Tipo, naturaleza y padre solo cambian si la cuenta no
 * tiene movimientos (C-03).
 *
 * @param cuentaId           cuenta a editar
 * @param version            version que la persona tenia a la vista
 * @param nombre             nombre nuevo, o nulo
 * @param codigoAgrupadorSat codigo agrupador nuevo (P2), o nulo
 * @param tipo               tipo nuevo, o nulo
 * @param naturaleza         naturaleza nueva, o nula
 * @param padreId            padre nuevo, o nulo
 */
public record EditarCuentaComando(UUID cuentaId, long version, String nombre, String codigoAgrupadorSat,
                                  TipoCuenta tipo, Naturaleza naturaleza, UUID padreId) implements Comando {
}
