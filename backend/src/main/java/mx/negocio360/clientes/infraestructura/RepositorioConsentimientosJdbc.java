package mx.negocio360.clientes.infraestructura;

import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import static mx.negocio360.compartido.transaccion.MomentosJdbc.instante;
import static mx.negocio360.compartido.transaccion.MomentosJdbc.momento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioConsentimientos;
import mx.negocio360.clientes.dominio.Consentimiento;
import mx.negocio360.clientes.dominio.FormaConsentimiento;
import mx.negocio360.clientes.eventos.EstadoConsentimiento;
import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.MedioConsentimiento;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Historial de consentimientos en {@code clientes.consentimiento}. Solo inserta y lee (P-03): el rol de la aplicacion no
 * tiene UPDATE ni DELETE y un disparador lo impone a todos los roles.
 */
public final class RepositorioConsentimientosJdbc implements RepositorioConsentimientos {

    private static final String COLUMNAS = "id, negocio_id, cliente_id, finalidad, estado, forma, medio, aviso_version, "
            + "registrado_por, registrado_en";

    private final JdbcClient jdbc;
    private final EjecutorEnNegocio negocio;

    public RepositorioConsentimientosJdbc(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.negocio = Objects.requireNonNull(negocio, "negocio");
    }

    @Override
    public void insertar(Consentimiento c) {
        negocio.en(c.negocioId(), () -> jdbc.sql("""
                INSERT INTO clientes.consentimiento (%s)
                VALUES (:id, :negocio, :cliente, :finalidad, :estado, :forma, :medio, :avisoVersion, :registradoPor,
                        :registradoEn)
                """.formatted(COLUMNAS))
                .param("id", c.id()).param("negocio", c.negocioId()).param("cliente", c.clienteId())
                .param("finalidad", c.finalidad().name()).param("estado", c.estado().name())
                .param("forma", c.forma().name()).param("medio", c.medio().name())
                .param("avisoVersion", c.avisoVersion()).param("registradoPor", c.registradoPor())
                .param("registradoEn", momento(c.registradoEn()))
                .update());
    }

    @Override
    public List<Consentimiento> porCliente(UUID negocioId, UUID clienteId) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS + " FROM clientes.consentimiento "
                        + "WHERE negocio_id = :negocio AND cliente_id = :cliente ORDER BY registrado_en, id")
                .param("negocio", negocioId).param("cliente", clienteId)
                .query(RepositorioConsentimientosJdbc::leer).list());
    }

    private static Consentimiento leer(ResultSet fila, int numero) throws SQLException {
        return new Consentimiento(fila.getObject("id", UUID.class), fila.getObject("negocio_id", UUID.class),
                fila.getObject("cliente_id", UUID.class), Finalidad.valueOf(fila.getString("finalidad")),
                EstadoConsentimiento.valueOf(fila.getString("estado")),
                FormaConsentimiento.valueOf(fila.getString("forma")), MedioConsentimiento.valueOf(fila.getString("medio")),
                fila.getInt("aviso_version"), fila.getString("registrado_por"), instante(fila, "registrado_en"));
    }
}
