package mx.negocio360.inventario.infraestructura;

import static mx.negocio360.compartido.transaccion.MomentosJdbc.instante;
import static mx.negocio360.compartido.transaccion.MomentosJdbc.momento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import mx.negocio360.inventario.aplicacion.Preparacion;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioPreparaciones;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Preparaciones (token de un solo uso) en {@code inventario.preparacion}. Guarda solo el hash del token. El comando de
 * Inventario no lleva datos personales, asi que se conserva despues de usarse.
 *
 * <p>{@code marcarUsada} corre en su propia transaccion, aparte de la del caso de uso, para que un fallo del comando no
 * devuelva el token a la vida; es un {@code UPDATE} condicionado a {@code usado_en IS NULL}, asi que el conteo decide cual
 * de dos confirmaciones simultaneas gana.
 */
public final class PreparacionesJdbc implements RepositorioPreparaciones {

    private static final String COLUMNAS = "token_hash, negocio_id, actor, comando_tipo, comando_hash, comando, resumen, "
            + "expira_en, usado_en";

    private final JdbcClient jdbc;
    private final EjecutorEnNegocio negocio;

    /**
     * Arma el repositorio.
     *
     * @param jdbc    cliente de la base
     * @param negocio ejecutor que fija el negocio en cada transaccion
     */
    public PreparacionesJdbc(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.negocio = Objects.requireNonNull(negocio, "negocio");
    }

    @Override
    public void guardar(Preparacion p) {
        negocio.en(p.negocioId(), () -> jdbc.sql("INSERT INTO inventario.preparacion (" + COLUMNAS + ") VALUES (:hash, "
                        + ":negocio, :actor, :tipo, :comandoHash, :comando, :resumen, :expiraEn, :usadoEn)")
                .param("hash", p.tokenHash()).param("negocio", p.negocioId()).param("actor", p.actor())
                .param("tipo", p.comandoTipo()).param("comandoHash", p.comandoHash()).param("comando", p.comando())
                .param("resumen", p.resumen()).param("expiraEn", momento(p.expiraEn()))
                .param("usadoEn", momento(p.usadoEn()), Types.TIMESTAMP_WITH_TIMEZONE).update());
    }

    @Override
    public Optional<Preparacion> porHash(UUID negocioId, String tokenHash) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS + " FROM inventario.preparacion "
                        + "WHERE negocio_id = :negocio AND token_hash = :hash")
                .param("negocio", negocioId).param("hash", tokenHash).query(PreparacionesJdbc::leer).optional());
    }

    @Override
    public boolean marcarUsada(UUID negocioId, String tokenHash, Instant ahora) {
        return negocio.enTransaccionPropia(negocioId, () -> jdbc.sql("UPDATE inventario.preparacion SET usado_en = :ahora "
                        + "WHERE negocio_id = :negocio AND token_hash = :hash AND usado_en IS NULL")
                .param("ahora", momento(ahora)).param("negocio", negocioId).param("hash", tokenHash).update() == 1);
    }

    private static Preparacion leer(ResultSet fila, int numero) throws SQLException {
        return new Preparacion(fila.getString("token_hash"), fila.getObject("negocio_id", UUID.class),
                fila.getString("actor"), fila.getString("comando_tipo"), fila.getString("comando_hash"),
                fila.getString("comando"), fila.getString("resumen"), instante(fila, "expira_en"),
                instante(fila, "usado_en"));
    }
}
