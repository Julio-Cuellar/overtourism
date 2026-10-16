package mx.negocio360.negocios.infraestructura;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioDemostracion;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Los negocios simulados: la marca {@code es_simulado} del negocio y la lista {@code demostracion_negocio}, que permite
 * restablecerlos sin leer ningun otro negocio (DM-04).
 */
public final class RepositorioDemostracionJdbc implements RepositorioDemostracion {

    private final JdbcClient jdbc;
    private final EjecutorEnContexto contexto;

    public RepositorioDemostracionJdbc(JdbcClient jdbc, EjecutorEnContexto contexto) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public List<UUID> negocios() {
        return jdbc.sql("SELECT negocio_id FROM negocios.demostracion_negocio ORDER BY negocio_id")
                .query(UUID.class).list();
    }

    @Override
    public void marcarSimulado(UUID negocioId) {
        contexto.enNegocio(negocioId, () -> {
            jdbc.sql("UPDATE negocios.negocio SET es_simulado = true WHERE id = :id").param("id", negocioId).update();
            jdbc.sql("INSERT INTO negocios.demostracion_negocio (negocio_id) VALUES (:id) ON CONFLICT DO NOTHING")
                    .param("id", negocioId).update();
            return null;
        });
    }

    @Override
    public boolean esSimulado(UUID negocioId) {
        return jdbc.sql("SELECT count(*) FROM negocios.demostracion_negocio WHERE negocio_id = :id")
                .param("id", negocioId).query(Integer.class).single() > 0;
    }
}
