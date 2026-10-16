package mx.negocio360.negocios.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Conversion entre {@link Instant} y {@code TIMESTAMP WITH TIME ZONE} (siempre en UTC) y lecturas con nulos. */
final class MomentosJdbc {

    private MomentosJdbc() {
    }

    static OffsetDateTime momento(Instant instante) {
        return instante == null ? null : OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    static Instant instante(ResultSet fila, String columna) throws SQLException {
        var valor = fila.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }

    static UUID uuid(ResultSet fila, String columna) throws SQLException {
        return fila.getObject(columna, UUID.class);
    }

    static Integer entero(ResultSet fila, String columna) throws SQLException {
        var valor = fila.getInt(columna);
        return fila.wasNull() ? null : valor;
    }
}
