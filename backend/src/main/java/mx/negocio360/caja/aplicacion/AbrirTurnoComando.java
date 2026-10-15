package mx.negocio360.caja.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;

/**
 * CF-10 AbrirTurno (CJ-01, CJ-02).
 *
 * @param cajaId       caja del negocio
 * @param fondoInicial efectivo con que empieza, mayor o igual a 0 y con hasta 2 decimales
 */
public record AbrirTurnoComando(UUID cajaId, BigDecimal fondoInicial) implements Comando {
}
