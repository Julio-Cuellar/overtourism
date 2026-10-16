package mx.negocio360.caja.aplicacion;

import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;
import org.springframework.modulith.NamedInterface;

/**
 * CF-13 DescartarCorte: regresa a ABIERTO un turno EN_CORTE, por ejemplo si se conto mal.
 *
 * @param turnoId turno
 */
@NamedInterface("comandos")
public record DescartarCorteComando(UUID turnoId) implements Comando {
}
