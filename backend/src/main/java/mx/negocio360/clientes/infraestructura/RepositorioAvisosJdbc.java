package mx.negocio360.clientes.infraestructura;

import static mx.negocio360.clientes.infraestructura.MomentosJdbc.instante;
import static mx.negocio360.clientes.infraestructura.MomentosJdbc.momento;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioAvisos;
import mx.negocio360.clientes.dominio.AvisoPrivacidad;
import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.dominio.FinalidadAviso;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Avisos de privacidad en {@code clientes.aviso_privacidad}. El contenido de un aviso publicado no se edita: publicar
 * cierra el vigente (solo su {@code vigente_hasta}, lo unico que el rol puede actualizar) e inserta el nuevo en la misma
 * transaccion. Una version repetida o un segundo vigente chocan con los unicos de la tabla y fallan con CONCURRENCIA.
 */
public final class RepositorioAvisosJdbc implements RepositorioAvisos {

    private static final String COLUMNAS = "id, negocio_id, version, responsable_nombre, responsable_domicilio, "
            + "finalidades::text AS finalidades, texto_simplificado, texto_integral, contacto_arco, vigente_desde, "
            + "vigente_hasta, publicado_por";
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;
    private final EjecutorEnNegocio negocio;

    public RepositorioAvisosJdbc(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.negocio = Objects.requireNonNull(negocio, "negocio");
    }

    @Override
    public Optional<AvisoPrivacidad> vigente(UUID negocioId) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS + " FROM clientes.aviso_privacidad "
                        + "WHERE negocio_id = :negocio AND vigente_hasta IS NULL")
                .param("negocio", negocioId).query(RepositorioAvisosJdbc::leer).optional());
    }

    @Override
    public int ultimaVersion(UUID negocioId) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT coalesce(max(version), 0) FROM clientes.aviso_privacidad "
                        + "WHERE negocio_id = :negocio")
                .param("negocio", negocioId).query(Integer.class).single());
    }

    @Override
    public void publicar(AvisoPrivacidad nuevo) {
        negocio.en(nuevo.negocioId(), () -> {
            try {
                jdbc.sql("UPDATE clientes.aviso_privacidad SET vigente_hasta = :hasta "
                                + "WHERE negocio_id = :negocio AND vigente_hasta IS NULL")
                        .param("hasta", momento(nuevo.vigenteDesde())).param("negocio", nuevo.negocioId()).update();
                jdbc.sql("""
                        INSERT INTO clientes.aviso_privacidad (id, negocio_id, version, responsable_nombre,
                            responsable_domicilio, finalidades, texto_simplificado, texto_integral, contacto_arco,
                            vigente_desde, vigente_hasta, publicado_por)
                        VALUES (:id, :negocio, :version, :responsable, :domicilio, CAST(:finalidades AS jsonb),
                            :simplificado, :integral, :contacto, :desde, :hasta, :publicadoPor)
                        """)
                        .param("id", nuevo.id()).param("negocio", nuevo.negocioId()).param("version", nuevo.version())
                        .param("responsable", nuevo.responsableNombre())
                        .param("domicilio", nuevo.responsableDomicilio())
                        .param("finalidades", finalidadesComoJson(nuevo.finalidades()))
                        .param("simplificado", nuevo.textoSimplificado()).param("integral", nuevo.textoIntegral())
                        .param("contacto", nuevo.contactoArco()).param("desde", momento(nuevo.vigenteDesde()))
                        .param("hasta", momento(nuevo.vigenteHasta()), java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                        .param("publicadoPor", nuevo.publicadoPor())
                        .update();
            } catch (DuplicateKeyException repetida) {
                throw new ClientesException(CodigoError.CONCURRENCIA);
            }
            return null;
        });
    }

    private static String finalidadesComoJson(List<FinalidadAviso> finalidades) {
        var arreglo = JSON.createArrayNode();
        for (var finalidad : finalidades) {
            arreglo.addObject().put("codigo", finalidad.codigo())
                    .put("requiere_consentimiento", finalidad.requiereConsentimiento());
        }
        return arreglo.toString();
    }

    private static AvisoPrivacidad leer(ResultSet fila, int numero) throws SQLException {
        return new AvisoPrivacidad(fila.getObject("id", UUID.class), fila.getObject("negocio_id", UUID.class),
                fila.getInt("version"), fila.getString("responsable_nombre"), fila.getString("responsable_domicilio"),
                finalidades(fila.getString("finalidades")), fila.getString("texto_simplificado"),
                fila.getString("texto_integral"), fila.getString("contacto_arco"), instante(fila, "vigente_desde"),
                instante(fila, "vigente_hasta"), fila.getString("publicado_por"));
    }

    private static List<FinalidadAviso> finalidades(String json) {
        try {
            var resultado = new ArrayList<FinalidadAviso>();
            for (JsonNode nodo : JSON.readTree(json)) {
                resultado.add(new FinalidadAviso(nodo.get("codigo").asText(),
                        nodo.get("requiere_consentimiento").asBoolean()));
            }
            return resultado;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Las finalidades guardadas del aviso no son un JSON valido");
        }
    }
}
