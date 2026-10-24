package mx.negocio360.compartido.comandos;

import static mx.negocio360.compartido.transaccion.MomentosJdbc.instante;
import static mx.negocio360.compartido.transaccion.MomentosJdbc.momento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Tabla {@code preparacion} de un modulo en PostgreSQL (la de {@code ventas} y la de {@code caja} tienen la misma
 * forma). Guarda solo el hash del token. El comando y el resumen se vacian (SC-10): la base impone que una preparacion
 * usada no los conserve, asi que {@link #marcarUsada} los vacia en la misma sentencia.
 *
 * <p>{@code marcarUsada} y {@code vaciar} corren en su propia transaccion, aparte de la del caso de uso, para que un
 * fallo del comando no devuelva el token a la vida ni deje el comando vivo. {@code marcarUsada} es un {@code UPDATE}
 * condicionado a {@code usado_en IS NULL}: el conteo decide cual de dos confirmaciones simultaneas gana.
 *
 * <p>El comando es texto, no {@code jsonb}: su hash se calcula sobre el texto exacto, y {@code jsonb} lo reordenaria.
 * El nombre del esquema nunca viene del usuario: lo fija el cableado del modulo y se valida como identificador.
 */
public final class RepositorioPreparacionesJdbc implements RepositorioPreparaciones {

    private static final Pattern IDENTIFICADOR = Pattern.compile("[a-z][a-z_]{0,30}");
    private static final String COLUMNAS = "token_hash, negocio_id, actor, comando_tipo, comando_hash, comando, "
            + "resumen, ambito, expira_en, usado_en";

    private final JdbcClient jdbc;
    private final EjecutorEnNegocio negocio;
    private final String tabla;

    /**
     * Arma el repositorio de la tabla {@code <esquema>.preparacion}.
     *
     * @param jdbc    cliente de la base
     * @param negocio ejecutor que fija el negocio en cada transaccion
     * @param esquema esquema del modulo, por ejemplo {@code ventas}
     * @throws IllegalArgumentException si el esquema no es un identificador simple
     */
    public RepositorioPreparacionesJdbc(JdbcClient jdbc, EjecutorEnNegocio negocio, String esquema) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.negocio = Objects.requireNonNull(negocio, "negocio");
        if (!IDENTIFICADOR.matcher(Objects.requireNonNull(esquema, "esquema")).matches()) {
            throw new IllegalArgumentException("Esquema no valido: " + esquema);
        }
        this.tabla = esquema + ".preparacion";
    }

    @Override
    public void guardar(Preparacion p) {
        negocio.en(p.negocioId(), () -> jdbc.sql("INSERT INTO " + tabla + " (" + COLUMNAS + ") VALUES (:hash, :negocio, "
                        + ":actor, :tipo, :comandoHash, :comando, :resumen, :ambito, :expiraEn, :usadoEn)")
                .param("hash", p.tokenHash()).param("negocio", p.negocioId()).param("actor", p.actor())
                .param("tipo", p.comandoTipo()).param("comandoHash", p.comandoHash())
                .param("comando", p.comando(), Types.VARCHAR).param("resumen", p.resumen(), Types.VARCHAR)
                .param("ambito", p.ambito(), Types.VARCHAR).param("expiraEn", momento(p.expiraEn()))
                .param("usadoEn", momento(p.usadoEn()), Types.TIMESTAMP_WITH_TIMEZONE)
                .update());
    }

    @Override
    public Optional<Preparacion> porHash(UUID negocioId, String tokenHash) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS + " FROM " + tabla
                        + " WHERE negocio_id = :negocio AND token_hash = :hash")
                .param("negocio", negocioId).param("hash", tokenHash)
                .query(RepositorioPreparacionesJdbc::leer).optional());
    }

    @Override
    public boolean marcarUsada(UUID negocioId, String tokenHash, Instant ahora) {
        return negocio.enTransaccionPropia(negocioId, () -> jdbc.sql("UPDATE " + tabla
                        + " SET usado_en = :ahora, comando = NULL, resumen = NULL"
                        + " WHERE negocio_id = :negocio AND token_hash = :hash AND usado_en IS NULL")
                .param("ahora", momento(ahora)).param("negocio", negocioId).param("hash", tokenHash)
                .update() == 1);
    }

    @Override
    public void vaciar(UUID negocioId, String tokenHash) {
        negocio.enTransaccionPropia(negocioId, () -> jdbc.sql("UPDATE " + tabla
                        + " SET comando = NULL, resumen = NULL WHERE negocio_id = :negocio AND token_hash = :hash")
                .param("negocio", negocioId).param("hash", tokenHash).update());
    }

    @Override
    public long vigentes(UUID negocioId, String comandoTipo, String ambito, Instant ahora) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT count(*) FROM " + tabla
                        + " WHERE negocio_id = :negocio AND comando_tipo = :tipo AND ambito IS NOT DISTINCT FROM :ambito"
                        + " AND usado_en IS NULL AND expira_en > :ahora")
                .param("negocio", negocioId).param("tipo", comandoTipo).param("ambito", ambito, Types.VARCHAR)
                .param("ahora", momento(ahora)).query(Long.class).single());
    }

    private static Preparacion leer(ResultSet fila, int numero) throws SQLException {
        return new Preparacion(fila.getString("token_hash"), fila.getObject("negocio_id", UUID.class),
                fila.getString("actor"), fila.getString("comando_tipo"), fila.getString("comando_hash"),
                fila.getString("comando"), fila.getString("resumen"), fila.getString("ambito"),
                instante(fila, "expira_en"), instante(fila, "usado_en"));
    }
}
