package mx.negocio360.compartido.transaccion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Conversion entre {@link Instant} y {@code TIMESTAMP WITH TIME ZONE}: siempre en UTC. */
public final class MomentosJdbc {

    private MomentosJdbc() {
    }

    public static OffsetDateTime momento(Instant instante) {
        return instante == null ? null : OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    public static Instant instante(ResultSet fila, String columna) throws SQLException {
        var valor = fila.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }
}
