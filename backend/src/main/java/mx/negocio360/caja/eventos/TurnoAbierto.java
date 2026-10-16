package mx.negocio360.caja.eventos;

import java.util.LinkedHashMap;
import java.util.Map;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.compartido.dinero.DecimalPlano;

/**
 * {@code caja.turno_abierto} v1: se publica al confirmar AbrirTurno. Datos: {@code turno_id}, {@code caja_id},
 * {@code fondo_inicial}.
 *
 * @param turno turno recien abierto
 */
public record TurnoAbierto(Turno turno) {

    /** Tipo del evento en el sobre. */
    public static final String TIPO = "caja.turno_abierto";
    /** Version del contrato (R5). */
    public static final int VERSION = 1;

    /**
     * Carga del sobre con las claves del contrato.
     *
     * @return mapa listo para {@code SobreEvento}
     */
    public Map<String, Object> datos() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("turno_id", turno.id().toString());
        datos.put("caja_id", turno.cajaId().toString());
        datos.put("fondo_inicial", DecimalPlano.escribir(turno.fondoInicial()));
        return datos;
    }
}
