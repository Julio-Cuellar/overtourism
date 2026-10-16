package mx.negocio360.negocios.infraestructura;

import static mx.negocio360.negocios.infraestructura.MomentosJdbc.instante;
import static mx.negocio360.negocios.infraestructura.MomentosJdbc.momento;
import static mx.negocio360.negocios.infraestructura.MomentosJdbc.uuid;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioPreparacionesResena;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Reseñas preparadas en {@code negocios.preparacion_resena}. Solo guarda el hash del token. Marcar el token como usado
 * vacia el comentario en la misma sentencia (SN-09), y {@code marcarUsada} es un {@code UPDATE} condicionado a que siga sin
 * usar: el conteo decide cual de dos confirmaciones simultaneas gana.
 */
public final class RepositorioPreparacionesResenaJdbc implements RepositorioPreparacionesResena {

    private final JdbcClient jdbc;

    public RepositorioPreparacionesResenaJdbc(JdbcClient jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    public void guardar(Preparada p) {
        jdbc.sql("""
                INSERT INTO negocios.preparacion_resena (token_hash, negocio_id, visitante_id, calificacion, comentario,
                    creada_en, expira_en, usada_en)
                VALUES (:hash, :negocio, :visitante, :calificacion, :comentario, :creada, :expira, :usada)""")
                .param("hash", p.tokenHash()).param("negocio", p.negocioId()).param("visitante", p.visitanteId())
                .param("calificacion", p.calificacion()).param("comentario", p.comentario())
                .param("creada", momento(p.creadaEn())).param("expira", momento(p.expiraEn()))
                .param("usada", momento(p.usadaEn()), java.sql.Types.TIMESTAMP_WITH_TIMEZONE).update();
    }

    @Override
    public Optional<Preparada> porHash(String tokenHash) {
        return jdbc.sql("SELECT token_hash, negocio_id, visitante_id, calificacion, comentario, creada_en, expira_en, "
                        + "usada_en FROM negocios.preparacion_resena WHERE token_hash = :hash").param("hash", tokenHash)
                .query((r, n) -> new Preparada(r.getString("token_hash"), uuid(r, "negocio_id"),
                        uuid(r, "visitante_id"), r.getInt("calificacion"), r.getString("comentario"),
                        instante(r, "creada_en"), instante(r, "expira_en"), instante(r, "usada_en"))).optional();
    }

    @Override
    public boolean marcarUsada(String tokenHash, Instant ahora) {
        return jdbc.sql("UPDATE negocios.preparacion_resena SET usada_en = :ahora, comentario = NULL "
                        + "WHERE token_hash = :hash AND usada_en IS NULL")
                .param("ahora", momento(ahora)).param("hash", tokenHash).update() == 1;
    }

    @Override
    public int preparadasDesde(UUID visitanteId, Instant desde) {
        return jdbc.sql("SELECT count(*) FROM negocios.preparacion_resena WHERE visitante_id = :visitante "
                + "AND creada_en > :desde").param("visitante", visitanteId).param("desde", momento(desde))
                .query(Integer.class).single();
    }

    @Override
    public int borrarVencidas(Instant antesDe) {
        return jdbc.sql("DELETE FROM negocios.preparacion_resena WHERE expira_en < :antes")
                .param("antes", momento(antesDe)).update();
    }

    @Override
    public int borrarDeNegocio(UUID negocioId) {
        return jdbc.sql("DELETE FROM negocios.preparacion_resena WHERE negocio_id = :negocio")
                .param("negocio", negocioId).update();
    }
}
