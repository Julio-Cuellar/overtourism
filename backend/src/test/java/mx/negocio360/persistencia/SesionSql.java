package mx.negocio360.persistencia;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Una sesión de pruebas contra el PostgreSQL de Docker: abre una transacción y la deshace siempre al cerrar, así las
 * pruebas no se estorban entre sí. {@link #intentar} devuelve el SQLSTATE del error en vez de lanzarlo, y deja la
 * transacción usable gracias a un punto de guardado, para poder comprobar varios errores seguidos.
 */
final class SesionSql implements AutoCloseable {

    private final Connection conexion;

    private SesionSql(Connection conexion) throws SQLException {
        this.conexion = conexion;
        conexion.setAutoCommit(false);
    }

    /** Con el mismo rol que usa la aplicación: sin superusuario y sujeto a la seguridad por fila. */
    static SesionSql comoAplicacion() throws SQLException {
        return new SesionSql(PostgresDePruebas.conexionDeLaAplicacion());
    }

    /** Como dueño de la base: superusuario, no le aplica la seguridad por fila. */
    static SesionSql comoDueno() throws SQLException {
        return new SesionSql(PostgresDePruebas.conexionDelDueno());
    }

    /** Fija el negocio de esta transacción, como lo hará la aplicación en cada petición. */
    SesionSql enNegocio(UUID negocio) throws SQLException {
        consultar("select set_config('app.negocio_id', ?, true)", String.class, negocio.toString());
        return this;
    }

    SesionSql sinNegocio() throws SQLException {
        consultar("select set_config('app.negocio_id', '', true)", String.class);
        return this;
    }

    int ejecutar(String sql, Object... parametros) throws SQLException {
        try (var sentencia = conexion.prepareStatement(sql)) {
            enlazar(sentencia, parametros);
            return sentencia.executeUpdate();
        }
    }

    /** Primera columna de la primera fila. */
    <T> T consultar(String sql, Class<T> tipo, Object... parametros) throws SQLException {
        try (var sentencia = conexion.prepareStatement(sql)) {
            enlazar(sentencia, parametros);
            try (var resultado = sentencia.executeQuery()) {
                return resultado.next() ? resultado.getObject(1, tipo) : null;
            }
        }
    }

    /** Primera columna de todas las filas. */
    <T> List<T> columna(String sql, Class<T> tipo, Object... parametros) throws SQLException {
        var valores = new ArrayList<T>();
        try (var sentencia = conexion.prepareStatement(sql)) {
            enlazar(sentencia, parametros);
            try (var resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    valores.add(resultado.getObject(1, tipo));
                }
            }
        }
        return valores;
    }

    /** Confirma la transacción en curso y abre otra en la misma conexión (así se prueba que nada se filtra entre ellas). */
    SesionSql confirmar() throws SQLException {
        conexion.commit();
        return this;
    }

    /** Ejecuta la sentencia y devuelve el SQLSTATE del error, o nulo si no falló. La transacción sigue usable. */
    String intentar(String sql, Object... parametros) throws SQLException {
        Savepoint punto = conexion.setSavepoint();
        try (var sentencia = conexion.prepareStatement(sql)) {
            enlazar(sentencia, parametros);
            sentencia.execute();
            conexion.releaseSavepoint(punto);
            return null;
        } catch (SQLException e) {
            conexion.rollback(punto);
            return e.getSQLState();
        }
    }

    private static void enlazar(java.sql.PreparedStatement sentencia, Object[] parametros) throws SQLException {
        for (var i = 0; i < parametros.length; i++) {
            sentencia.setObject(i + 1, parametros[i]);
        }
    }

    @Override
    public void close() throws SQLException {
        try {
            conexion.rollback();
        } finally {
            conexion.close();
        }
    }
}
