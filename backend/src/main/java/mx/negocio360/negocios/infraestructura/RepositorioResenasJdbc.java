package mx.negocio360.negocios.infraestructura;

import static mx.negocio360.negocios.infraestructura.MomentosJdbc.instante;
import static mx.negocio360.negocios.infraestructura.MomentosJdbc.momento;
import static mx.negocio360.negocios.infraestructura.MomentosJdbc.uuid;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioResenas;
import mx.negocio360.negocios.dominio.Resena;
import mx.negocio360.negocios.dominio.ResumenCalificacion;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;

/** Reseñas y su resumen en el esquema {@code negocios}, con seguridad por fila; lo publico, por la vista sin autor. */
public final class RepositorioResenasJdbc implements RepositorioResenas {

    private static final String COLUMNAS = "id, negocio_id, visitante_id, calificacion, comentario, estado, origen, "
            + "respuesta, respondida_por, respondida_en, creada_en, editada_en, version";

    private final JdbcClient jdbc;
    private final EjecutorEnContexto contexto;
    private final EjecutorPublico publico;

    public RepositorioResenasJdbc(JdbcClient jdbc, EjecutorEnContexto contexto,
                                  PlatformTransactionManager transacciones) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.contexto = Objects.requireNonNull(contexto, "contexto");
        this.publico = new EjecutorPublico(jdbc, transacciones);
    }

    @Override
    public boolean insertar(Resena r) {
        return contexto.enNegocio(r.negocioId(), () -> jdbc.sql("""
                INSERT INTO negocios.resena (id, negocio_id, visitante_id, calificacion, comentario, estado, origen,
                    respuesta, respondida_por, respondida_en, creada_en, editada_en, version)
                VALUES (:id, :negocio, :visitante, :calificacion, :comentario, :estado, :origen, :respuesta,
                    :respondidaPor, :respondidaEn, :creada, :editada, :version)
                ON CONFLICT (negocio_id, visitante_id) DO NOTHING""")
                .param("id", r.id()).param("negocio", r.negocioId()).param("visitante", r.visitanteId())
                .param("calificacion", r.calificacion()).param("comentario", r.comentario())
                .param("estado", r.estado().name()).param("origen", r.origen().name())
                .param("respuesta", r.respuesta()).param("respondidaPor", r.respondidaPor())
                .param("respondidaEn", momento(r.respondidaEn()), java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                .param("creada", momento(r.creadaEn()))
                .param("editada", momento(r.editadaEn()), java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                .param("version", r.version()).update() == 1);
    }

    @Override
    public Optional<Resena> resena(UUID negocioId, UUID resenaId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS + " FROM negocios.resena "
                + "WHERE negocio_id = :negocio AND id = :id").param("negocio", negocioId).param("id", resenaId)
                .query(RepositorioResenasJdbc::leer).optional());
    }

    @Override
    public boolean existePorAutor(UUID negocioId, UUID visitanteId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("SELECT count(*) FROM negocios.resena "
                + "WHERE negocio_id = :negocio AND visitante_id = :visitante").param("negocio", negocioId)
                .param("visitante", visitanteId).query(Integer.class).single() > 0);
    }

    @Override
    public List<Resena> pagina(UUID negocioId, boolean sinRespuesta, int limite, int desplazamiento) {
        var filtro = sinRespuesta ? " AND estado = 'PUBLICADA' AND respuesta IS NULL" : "";
        return contexto.enNegocio(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS + " FROM negocios.resena "
                + "WHERE negocio_id = :negocio" + filtro + " ORDER BY creada_en DESC, id DESC "
                + "LIMIT :limite OFFSET :desplazamiento").param("negocio", negocioId).param("limite", limite)
                .param("desplazamiento", desplazamiento).query(RepositorioResenasJdbc::leer).list());
    }

    @Override
    public boolean responder(UUID negocioId, UUID resenaId, String respuesta, UUID respondidaPor, Instant ahora) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("""
                UPDATE negocios.resena SET respuesta = :respuesta, respondida_por = :por, respondida_en = :ahora,
                    version = version + 1
                WHERE negocio_id = :negocio AND id = :id AND respuesta IS NULL AND estado = 'PUBLICADA'""")
                .param("respuesta", respuesta).param("por", respondidaPor).param("ahora", momento(ahora))
                .param("negocio", negocioId).param("id", resenaId).update() == 1);
    }

    @Override
    public boolean cambiarEstado(UUID negocioId, UUID resenaId, Resena.Estado estado, Instant ahora) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("""
                UPDATE negocios.resena SET estado = :estado, editada_en = :ahora, version = version + 1
                WHERE negocio_id = :negocio AND id = :id""").param("estado", estado.name())
                .param("ahora", momento(ahora)).param("negocio", negocioId).param("id", resenaId).update() == 1);
    }

    @Override
    public ResumenCalificacion recalcularResumen(UUID negocioId, Instant ahora) {
        contexto.enNegocio(negocioId, () -> jdbc.sql("""
                INSERT INTO negocios.resumen_resenas (negocio_id, total, suma, c1, c2, c3, c4, c5, actualizado_en)
                SELECT :negocio, count(*)::int, coalesce(sum(calificacion), 0)::int,
                    (count(*) FILTER (WHERE calificacion = 1))::int, (count(*) FILTER (WHERE calificacion = 2))::int,
                    (count(*) FILTER (WHERE calificacion = 3))::int, (count(*) FILTER (WHERE calificacion = 4))::int,
                    (count(*) FILTER (WHERE calificacion = 5))::int, :ahora
                FROM negocios.resena WHERE negocio_id = :negocio AND estado = 'PUBLICADA'
                ON CONFLICT (negocio_id) DO UPDATE SET total = EXCLUDED.total, suma = EXCLUDED.suma, c1 = EXCLUDED.c1,
                    c2 = EXCLUDED.c2, c3 = EXCLUDED.c3, c4 = EXCLUDED.c4, c5 = EXCLUDED.c5,
                    actualizado_en = EXCLUDED.actualizado_en""")
                .param("negocio", negocioId).param("ahora", momento(ahora)).update());
        return resumen(negocioId);
    }

    @Override
    public ResumenCalificacion resumen(UUID negocioId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("SELECT c1, c2, c3, c4, c5 FROM negocios.resumen_resenas "
                        + "WHERE negocio_id = :negocio").param("negocio", negocioId)
                .query((r, n) -> ResumenCalificacion.de(r.getInt("c1"), r.getInt("c2"), r.getInt("c3"), r.getInt("c4"),
                        r.getInt("c5"))).optional().orElse(ResumenCalificacion.vacio()));
    }

    @Override
    public List<Publica> publicas(UUID negocioId, int limite, int desplazamiento) {
        return publico.leer(() -> jdbc.sql("SELECT id, calificacion, comentario, creada_en, respuesta, respondida_en, "
                + "demostracion FROM negocios.v_resena_publica WHERE negocio_id = :negocio "
                + "ORDER BY creada_en DESC, id DESC LIMIT :limite OFFSET :desplazamiento")
                .param("negocio", negocioId).param("limite", limite).param("desplazamiento", desplazamiento)
                .query((r, n) -> new Publica(uuid(r, "id"), r.getInt("calificacion"), r.getString("comentario"),
                        instante(r, "creada_en"), r.getString("respuesta"), instante(r, "respondida_en"),
                        r.getBoolean("demostracion"))).list());
    }

    @Override
    public int borrarTodas(UUID negocioId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("DELETE FROM negocios.resena WHERE negocio_id = :negocio")
                .param("negocio", negocioId).update());
    }

    private static Resena leer(ResultSet r, int fila) throws SQLException {
        return new Resena(uuid(r, "id"), uuid(r, "negocio_id"), uuid(r, "visitante_id"), r.getInt("calificacion"),
                r.getString("comentario"), Resena.Estado.valueOf(r.getString("estado")),
                Resena.Origen.valueOf(r.getString("origen")), r.getString("respuesta"), uuid(r, "respondida_por"),
                instante(r, "respondida_en"), instante(r, "creada_en"), instante(r, "editada_en"), r.getLong("version"));
    }
}
