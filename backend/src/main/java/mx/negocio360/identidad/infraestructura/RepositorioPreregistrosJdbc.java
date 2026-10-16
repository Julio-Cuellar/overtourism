package mx.negocio360.identidad.infraestructura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.HashConSal;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioPreregistros;
import mx.negocio360.identidad.dominio.Preregistro;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Pre-registros en {@code identidad.preregistro} (D-86), el primer repositorio de Identidad en Postgres. Cada operacion
 * es una sola sentencia, asi que no necesita una transaccion abierta: guardar es un {@code INSERT ... ON CONFLICT} por
 * correo y eliminar es un {@code DELETE} cuyo conteo decide quien consumio el pre-registro. Lleva el esquema en cada nombre
 * porque el rol de la aplicacion no tiene {@code identidad} en su {@code search_path}.
 */
public final class RepositorioPreregistrosJdbc implements RepositorioPreregistros {

    private static final String COLUMNAS = "id, correo, telefono, nombre, apellidos, aviso_version, aviso_aceptado_en, "
            + "negocio_nombre, negocio_giro, codigo_sal, codigo_hash, intentos, expira_en, creado_en";

    private final JdbcClient jdbc;

    public RepositorioPreregistrosJdbc(JdbcClient jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    }

    @Override
    public void guardar(Preregistro p) {
        jdbc.sql("""
                INSERT INTO identidad.preregistro (%s)
                VALUES (:id, :correo, :telefono, :nombre, :apellidos, :avisoVersion, :avisoAceptadoEn, :negocioNombre,
                        :negocioGiro, :sal, :hash, :intentos, :venceEn, :creadoEn)
                ON CONFLICT (correo) DO UPDATE SET id = EXCLUDED.id, telefono = EXCLUDED.telefono,
                    nombre = EXCLUDED.nombre, apellidos = EXCLUDED.apellidos, aviso_version = EXCLUDED.aviso_version,
                    aviso_aceptado_en = EXCLUDED.aviso_aceptado_en, negocio_nombre = EXCLUDED.negocio_nombre,
                    negocio_giro = EXCLUDED.negocio_giro, codigo_sal = EXCLUDED.codigo_sal,
                    codigo_hash = EXCLUDED.codigo_hash, intentos = EXCLUDED.intentos, expira_en = EXCLUDED.expira_en,
                    creado_en = EXCLUDED.creado_en
                """.formatted(COLUMNAS))
                .param("id", p.id())
                .param("correo", p.correo())
                .param("telefono", p.telefono())
                .param("nombre", p.nombre())
                .param("apellidos", p.apellidos())
                .param("avisoVersion", p.avisoVersion())
                .param("avisoAceptadoEn", momento(p.avisoAceptadoEn()))
                .param("negocioNombre", p.negocioNombre(), Types.VARCHAR)
                .param("negocioGiro", p.negocioGiro(), Types.VARCHAR)
                .param("sal", p.codigo().sal())
                .param("hash", p.codigo().hash())
                .param("intentos", p.intentos())
                .param("venceEn", momento(p.venceEn()))
                .param("creadoEn", momento(p.creadoEn()))
                .update();
    }

    @Override
    public Optional<Preregistro> porCorreo(String correo) {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM identidad.preregistro WHERE correo = :correo")
                .param("correo", correo)
                .query(RepositorioPreregistrosJdbc::leer)
                .optional();
    }

    @Override
    public boolean eliminar(UUID id) {
        return jdbc.sql("DELETE FROM identidad.preregistro WHERE id = :id").param("id", id).update() == 1;
    }

    @Override
    public int borrarVencidos(Instant ahora) {
        return jdbc.sql("DELETE FROM identidad.preregistro WHERE expira_en <= :ahora")
                .param("ahora", momento(ahora))
                .update();
    }

    private static Preregistro leer(ResultSet fila, int numero) throws SQLException {
        return new Preregistro(fila.getObject("id", UUID.class), fila.getString("correo"), fila.getString("telefono"),
                fila.getString("nombre"), fila.getString("apellidos"), fila.getInt("aviso_version"),
                instante(fila, "aviso_aceptado_en"), fila.getString("negocio_nombre"), fila.getString("negocio_giro"),
                new HashConSal(fila.getString("codigo_sal"), fila.getString("codigo_hash")), fila.getInt("intentos"),
                instante(fila, "expira_en"), instante(fila, "creado_en"));
    }

    private static OffsetDateTime momento(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }

    private static Instant instante(ResultSet fila, String columna) throws SQLException {
        return fila.getObject(columna, OffsetDateTime.class).toInstant();
    }
}
