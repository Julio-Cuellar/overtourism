package mx.negocio360.caja.infraestructura;

import static mx.negocio360.compartido.transaccion.MomentosJdbc.instante;
import static mx.negocio360.compartido.transaccion.MomentosJdbc.momento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Tablas {@code caja.caja} y {@code caja.turno} en PostgreSQL. Cada operacion corre con el negocio fijado en la
 * transaccion ({@link EjecutorEnNegocio}), que es lo que usa la seguridad por fila.
 *
 * <p>Un turno sin cerrar por caja es unico (CJ-01) y lo respalda el indice parcial {@code ux_turno_no_cerrado}: dos
 * aperturas simultaneas no pasan las dos. {@link #actualizarTurno} es un {@code UPDATE} condicionado a la version
 * esperada, asi que de dos cierres simultaneos solo uno gana.
 */
public final class RepositorioCajaJdbc implements RepositorioCaja {

    private static final String COLUMNAS_TURNO = "id, negocio_id, caja_id, cajero, estado, fondo_inicial, abierto_en, "
            + "cerrado_en, corte_vence_en, version";

    private final JdbcClient jdbc;
    private final EjecutorEnNegocio negocio;

    /**
     * Arma el repositorio.
     *
     * @param jdbc    cliente de la base
     * @param negocio ejecutor que fija el negocio en cada transaccion
     */
    public RepositorioCajaJdbc(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.negocio = Objects.requireNonNull(negocio, "negocio");
    }

    @Override
    public void guardarCaja(Caja caja) {
        negocio.en(caja.negocioId(), () -> jdbc.sql("INSERT INTO caja.caja (id, negocio_id, nombre, activa) "
                        + "VALUES (:id, :negocio, :nombre, :activa)")
                .param("id", caja.id()).param("negocio", caja.negocioId()).param("nombre", caja.nombre())
                .param("activa", caja.activa()).update());
    }

    @Override
    public Optional<Caja> caja(UUID negocioId, UUID cajaId) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT id, negocio_id, nombre, activa FROM caja.caja "
                        + "WHERE negocio_id = :negocio AND id = :id")
                .param("negocio", negocioId).param("id", cajaId).query(RepositorioCajaJdbc::leerCaja).optional());
    }

    @Override
    public List<Caja> cajas(UUID negocioId) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT id, negocio_id, nombre, activa FROM caja.caja "
                        + "WHERE negocio_id = :negocio ORDER BY id")
                .param("negocio", negocioId).query(RepositorioCajaJdbc::leerCaja).list());
    }

    @Override
    public Optional<Turno> turno(UUID negocioId, UUID turnoId) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS_TURNO + " FROM caja.turno "
                        + "WHERE negocio_id = :negocio AND id = :id")
                .param("negocio", negocioId).param("id", turnoId).query(RepositorioCajaJdbc::leerTurno).optional());
    }

    @Override
    public Optional<Turno> turnoNoCerradoDeCaja(UUID negocioId, UUID cajaId) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS_TURNO + " FROM caja.turno "
                        + "WHERE negocio_id = :negocio AND caja_id = :caja AND estado <> 'CERRADO'")
                .param("negocio", negocioId).param("caja", cajaId).query(RepositorioCajaJdbc::leerTurno).optional());
    }

    @Override
    public void guardarTurno(Turno turno) {
        negocio.en(turno.negocioId(), () -> jdbc.sql("INSERT INTO caja.turno (" + COLUMNAS_TURNO + ") VALUES (:id, "
                        + ":negocio, :caja, :cajero, :estado, :fondo, :abierto, :cerrado, :vence, :version)")
                .param("id", turno.id()).param("negocio", turno.negocioId()).param("caja", turno.cajaId())
                .param("cajero", turno.cajero()).param("estado", turno.estado().name())
                .param("fondo", turno.fondoInicial()).param("abierto", momento(turno.abiertoEn()))
                .param("cerrado", momento(turno.cerradoEn()), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("vence", momento(turno.corteVenceEn()), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("version", turno.version()).update());
    }

    @Override
    public boolean actualizarTurno(Turno turno, int versionEsperada) {
        return negocio.en(turno.negocioId(), () -> jdbc.sql("UPDATE caja.turno SET estado = :estado, "
                        + "cerrado_en = :cerrado, corte_vence_en = :vence, version = :version "
                        + "WHERE negocio_id = :negocio AND id = :id AND version = :esperada")
                .param("estado", turno.estado().name())
                .param("cerrado", momento(turno.cerradoEn()), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("vence", momento(turno.corteVenceEn()), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("version", turno.version()).param("negocio", turno.negocioId()).param("id", turno.id())
                .param("esperada", versionEsperada).update() == 1);
    }

    private static Caja leerCaja(ResultSet fila, int numero) throws SQLException {
        return new Caja(fila.getObject("id", UUID.class), fila.getObject("negocio_id", UUID.class),
                fila.getString("nombre"), fila.getBoolean("activa"));
    }

    private static Turno leerTurno(ResultSet fila, int numero) throws SQLException {
        return new Turno(fila.getObject("id", UUID.class), fila.getObject("negocio_id", UUID.class),
                fila.getObject("caja_id", UUID.class), fila.getString("cajero"),
                EstadoTurno.valueOf(fila.getString("estado")), fila.getBigDecimal("fondo_inicial"),
                instante(fila, "abierto_en"), instante(fila, "cerrado_en"), instante(fila, "corte_vence_en"),
                fila.getInt("version"));
    }
}
