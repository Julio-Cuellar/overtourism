package mx.negocio360.caja.aplicacion;

import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;

/**
 * CF-13 DescartarCorte: regresa a ABIERTO un turno EN_CORTE, por ejemplo si se conto mal.
 *
 * @param turnoId turno
 */
public record DescartarCorteComando(UUID turnoId) implements Comando {
}
