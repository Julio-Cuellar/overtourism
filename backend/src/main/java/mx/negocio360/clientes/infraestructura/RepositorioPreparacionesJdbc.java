package mx.negocio360.clientes.infraestructura;

import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import static mx.negocio360.compartido.transaccion.MomentosJdbc.instante;
import static mx.negocio360.compartido.transaccion.MomentosJdbc.momento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.Preparacion;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioPreparaciones;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Preparaciones (token de un solo uso) en {@code clientes.preparacion}. Guarda solo el hash del token. El comando y el
 * resumen llevan datos personales y se vacian (SC-10): la base impone que una preparacion usada no conserve el comando y
 * que una vigente si lo tenga, asi que {@link #marcarUsada} lo vacia en la misma sentencia y {@link #vaciar} de una
 * preparacion sin usar la marca como usada.
 *
 * <p>{@code marcarUsada} y {@code vaciar} corren en su propia transaccion, aparte de la del manejador, para que un fallo
 * del comando no devuelva el token a la vida ni deje el comando vivo. {@code marcarUsada} es un {@code UPDATE} condicionado
 * a {@code usado_en IS NULL}: el conteo decide cual de dos confirmaciones simultaneas gana.
 *
 * <p>El comando es {@code jsonb}: la base reordena sus claves y agrega espacios, por eso el hash del comando es canonico
 * ({@code ServicioPreparacion.hashDelComando}). El resumen es una cadena, guardada como cadena JSON; vaciado queda como
 * {@code null} JSON porque la columna no admite NULL de SQL.
 */
public final class RepositorioPreparacionesJdbc implements RepositorioPreparaciones {

    private static final String COLUMNAS = "token_hash, negocio_id, actor, comando_tipo, comando_hash, "
            + "comando::text AS comando, CASE WHEN jsonb_typeof(resumen) = 'string' THEN resumen #>> '{}' END AS resumen, "
            + "expira_en, usado_en";

    private final JdbcClient jdbc;
    private final EjecutorEnNegocio negocio;

    public RepositorioPreparacionesJdbc(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.negocio = Objects.requireNonNull(negocio, "negocio");
    }

    @Override
    public void guardar(Preparacion p) {
        negocio.en(p.negocioId(), () -> jdbc.sql("""
                INSERT INTO clientes.preparacion (token_hash, negocio_id, actor, comando_tipo, comando_hash, comando,
                    resumen, expira_en, usado_en)
                VALUES (:hash, :negocio, :actor, :tipo, :comandoHash, CAST(:comando AS jsonb),
                    coalesce(to_jsonb(CAST(:resumen AS text)), 'null'::jsonb), :expiraEn, :usadoEn)
                """)
                .param("hash", p.tokenHash()).param("negocio", p.negocioId()).param("actor", p.actor())
                .param("tipo", p.comandoTipo()).param("comandoHash", p.comandoHash())
                .param("comando", p.comando(), Types.VARCHAR).param("resumen", p.resumen(), Types.VARCHAR)
                .param("expiraEn", momento(p.expiraEn()))
                .param("usadoEn", momento(p.usadoEn()), Types.TIMESTAMP_WITH_TIMEZONE)
                .update());
    }

    @Override
    public Optional<Preparacion> porHash(UUID negocioId, String tokenHash) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS + " FROM clientes.preparacion "
                        + "WHERE negocio_id = :negocio AND token_hash = :hash")
                .param("negocio", negocioId).param("hash", tokenHash)
                .query(RepositorioPreparacionesJdbc::leer).optional());
    }

    @Override
    public boolean marcarUsada(UUID negocioId, String tokenHash, Instant ahora) {
        return negocio.enTransaccionPropia(negocioId, () -> jdbc.sql("""
                UPDATE clientes.preparacion SET usado_en = :ahora, comando = NULL, resumen = 'null'::jsonb
                WHERE negocio_id = :negocio AND token_hash = :hash AND usado_en IS NULL
                """)
                .param("ahora", momento(ahora)).param("negocio", negocioId).param("hash", tokenHash)
                .update() == 1);
    }

    @Override
    public void vaciar(UUID negocioId, String tokenHash) {
        negocio.enTransaccionPropia(negocioId, () -> jdbc.sql("""
                UPDATE clientes.preparacion SET comando = NULL, resumen = 'null'::jsonb,
                    usado_en = coalesce(usado_en, now())
                WHERE negocio_id = :negocio AND token_hash = :hash
                """)
                .param("negocio", negocioId).param("hash", tokenHash).update());
    }

    private static Preparacion leer(ResultSet fila, int numero) throws SQLException {
        return new Preparacion(fila.getString("token_hash"), fila.getObject("negocio_id", UUID.class),
                fila.getString("actor"), fila.getString("comando_tipo"), fila.getString("comando_hash"),
                fila.getString("comando"), fila.getString("resumen"), instante(fila, "expira_en"),
                instante(fila, "usado_en"));
    }
}
