package mx.negocio360.negocios.infraestructura;

import static mx.negocio360.negocios.infraestructura.MomentosJdbc.momento;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.negocios.AltaVista;
import mx.negocio360.negocios.AltaVista.EstadoAlta;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioAltas;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Altas de negocio en {@code negocios.alta_negocio}, aisladas por cuenta (AL-04), y los eventos ya procesados en
 * {@code negocios.evento_procesado} (EV-02). El resultado de cada alta se inserta una sola vez: el segundo intento del
 * mismo negocio no cambia nada (NG-03).
 */
public final class RepositorioAltasJdbc implements RepositorioAltas {

    private final JdbcClient jdbc;
    private final EjecutorEnContexto contexto;

    public RepositorioAltasJdbc(JdbcClient jdbc, EjecutorEnContexto contexto) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public boolean marcarEventoProcesado(UUID eventoId, String listener, Instant ahora) {
        return jdbc.sql("INSERT INTO negocios.evento_procesado (evento_id, listener, procesado_en) "
                        + "VALUES (:evento, :listener, :ahora) ON CONFLICT DO NOTHING")
                .param("evento", eventoId).param("listener", listener).param("ahora", momento(ahora)).update() == 1;
    }

    @Override
    public boolean insertarAlta(Registro registro) {
        return contexto.enCuenta(registro.cuentaId(), () -> jdbc.sql("""
                        INSERT INTO negocios.alta_negocio (negocio_id, solicitado_por, nombre, giro, estado, motivo,
                            evento_id, creada_en)
                        VALUES (:negocio, :cuenta, :nombre, :giro, :estado, :motivo, :evento, :creada)
                        ON CONFLICT (negocio_id) DO NOTHING""")
                .param("negocio", registro.negocioId()).param("cuenta", registro.cuentaId())
                .param("nombre", registro.nombre()).param("giro", registro.giro())
                .param("estado", registro.estado().name()).param("motivo", registro.motivo())
                .param("evento", registro.eventoId()).param("creada", momento(registro.creadaEn())).update() == 1);
    }

    @Override
    public List<AltaVista> altasDeCuenta(UUID cuentaId) {
        return contexto.enCuenta(cuentaId, () -> jdbc.sql("SELECT negocio_id, estado, motivo FROM negocios.alta_negocio "
                        + "ORDER BY creada_en DESC, negocio_id DESC")
                .query((fila, n) -> new AltaVista(fila.getObject("negocio_id", UUID.class),
                        EstadoAlta.valueOf(fila.getString("estado")), fila.getString("motivo"))).list());
    }

    @Override
    public boolean giroActivo(String codigo) {
        return jdbc.sql("SELECT count(*) FROM negocios.giro WHERE codigo = :codigo AND activo")
                .param("codigo", codigo).query(Integer.class).single() == 1;
    }
}
