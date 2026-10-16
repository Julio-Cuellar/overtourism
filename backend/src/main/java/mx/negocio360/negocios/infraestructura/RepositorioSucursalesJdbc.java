package mx.negocio360.negocios.infraestructura;

import static mx.negocio360.negocios.infraestructura.MomentosJdbc.momento;
import static mx.negocio360.negocios.infraestructura.MomentosJdbc.uuid;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioSucursales;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.DiaHorario;
import mx.negocio360.negocios.dominio.ExcepcionHorario;
import mx.negocio360.negocios.dominio.NegociosException;
import mx.negocio360.negocios.dominio.SemanaHorario;
import mx.negocio360.negocios.dominio.Sucursal;
import mx.negocio360.negocios.dominio.Tramo;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Sucursales, tramos de horario y excepciones en el esquema {@code negocios}, con seguridad por fila. */
public final class RepositorioSucursalesJdbc implements RepositorioSucursales {

    private final JdbcClient jdbc;
    private final EjecutorEnContexto contexto;

    public RepositorioSucursalesJdbc(JdbcClient jdbc, EjecutorEnContexto contexto) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public List<Sucursal> sucursales(UUID negocioId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("SELECT " + RepositorioNegociosJdbc.COLUMNAS_SUCURSAL
                + " FROM negocios.sucursal WHERE negocio_id = :negocio AND activa ORDER BY es_principal DESC, creada_en, id")
                .param("negocio", negocioId).query(RepositorioNegociosJdbc::leerSucursal).list());
    }

    @Override
    public Optional<Sucursal> sucursal(UUID negocioId, UUID sucursalId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("SELECT " + RepositorioNegociosJdbc.COLUMNAS_SUCURSAL
                + " FROM negocios.sucursal WHERE negocio_id = :negocio AND id = :id")
                .param("negocio", negocioId).param("id", sucursalId)
                .query(RepositorioNegociosJdbc::leerSucursal).optional());
    }

    @Override
    public void actualizar(Sucursal s, long versionEsperada) {
        var filas = contexto.enNegocio(s.negocioId(), () -> jdbc.sql("""
                UPDATE negocios.sucursal SET nombre = :nombre, calle = :calle, numero = :numero, colonia = :colonia,
                    municipio = :municipio, entidad_federativa = :entidad, codigo_postal = :cp,
                    referencias = :referencias, latitud = :latitud, longitud = :longitud, telefono = :telefono,
                    actualizada_en = :actualizada, version = version + 1
                WHERE negocio_id = :negocio AND id = :id AND version = :esperada""")
                .param("nombre", s.nombre()).param("calle", s.calle()).param("numero", s.numero())
                .param("colonia", s.colonia()).param("municipio", s.municipio())
                .param("entidad", s.entidadFederativa()).param("cp", s.codigoPostal())
                .param("referencias", s.referencias()).param("latitud", s.latitud()).param("longitud", s.longitud())
                .param("telefono", s.telefono()).param("actualizada", momento(s.actualizadaEn()))
                .param("negocio", s.negocioId()).param("id", s.id()).param("esperada", versionEsperada).update());
        if (filas != 1) {
            throw new NegociosException(CodigoError.CONCURRENCIA);
        }
    }

    @Override
    public SemanaHorario semana(UUID negocioId, UUID sucursalId) {
        return contexto.enNegocio(negocioId, () -> {
            var porDia = new LinkedHashMap<Integer, List<Tramo>>();
            jdbc.sql("SELECT dia, abre, cierra FROM negocios.horario_tramo WHERE negocio_id = :negocio "
                            + "AND sucursal_id = :sucursal ORDER BY dia, abre")
                    .param("negocio", negocioId).param("sucursal", sucursalId)
                    .query((r, n) -> new Object[] {r.getInt("dia"), r.getObject("abre", LocalTime.class),
                            r.getObject("cierra", LocalTime.class)}).list()
                    .forEach(f -> porDia.computeIfAbsent((Integer) f[0], d -> new ArrayList<>())
                            .add(new Tramo((LocalTime) f[1], (LocalTime) f[2])));
            return new SemanaHorario(porDia.entrySet().stream().map(e -> new DiaHorario(e.getKey(), e.getValue()))
                    .toList());
        });
    }

    @Override
    public List<ExcepcionHorario> excepciones(UUID negocioId, UUID sucursalId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("SELECT id, fecha, cerrado, abre, cierra, motivo "
                        + "FROM negocios.horario_excepcion WHERE negocio_id = :negocio AND sucursal_id = :sucursal "
                        + "ORDER BY fecha")
                .param("negocio", negocioId).param("sucursal", sucursalId)
                .query(RepositorioSucursalesJdbc::leerExcepcion).list());
    }

    @Override
    public long versionDelHorario(UUID negocioId, UUID sucursalId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("SELECT horario_version FROM negocios.sucursal "
                        + "WHERE negocio_id = :negocio AND id = :id").param("negocio", negocioId).param("id", sucursalId)
                .query(Long.class).optional().orElse(0L));
    }

    @Override
    public void reemplazarSemana(UUID negocioId, UUID sucursalId, long versionEsperada, SemanaHorario semana,
                                 Instant ahora) {
        contexto.enNegocio(negocioId, () -> {
            var filas = jdbc.sql("UPDATE negocios.sucursal SET horario_version = horario_version + 1, "
                            + "actualizada_en = :ahora WHERE negocio_id = :negocio AND id = :id "
                            + "AND horario_version = :esperada")
                    .param("ahora", momento(ahora)).param("negocio", negocioId).param("id", sucursalId)
                    .param("esperada", versionEsperada).update();
            if (filas != 1) {
                throw new NegociosException(CodigoError.CONCURRENCIA);
            }
            jdbc.sql("DELETE FROM negocios.horario_tramo WHERE negocio_id = :negocio AND sucursal_id = :sucursal")
                    .param("negocio", negocioId).param("sucursal", sucursalId).update();
            for (var dia : semana.dias()) {
                for (var tramo : dia.tramos()) {
                    jdbc.sql("INSERT INTO negocios.horario_tramo (id, negocio_id, sucursal_id, dia, abre, cierra) "
                                    + "VALUES (:id, :negocio, :sucursal, :dia, :abre, :cierra)")
                            .param("id", UuidV7.generar(ahora)).param("negocio", negocioId)
                            .param("sucursal", sucursalId).param("dia", dia.dia()).param("abre", tramo.abre())
                            .param("cierra", tramo.cierra()).update();
                }
            }
            return null;
        });
    }

    @Override
    public boolean agregarExcepcion(UUID negocioId, UUID sucursalId, ExcepcionHorario e) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("""
                INSERT INTO negocios.horario_excepcion (id, negocio_id, sucursal_id, fecha, cerrado, abre, cierra,
                    motivo)
                VALUES (:id, :negocio, :sucursal, :fecha, :cerrado, :abre, :cierra, :motivo)
                ON CONFLICT (sucursal_id, fecha) DO NOTHING""")
                .param("id", e.id()).param("negocio", negocioId).param("sucursal", sucursalId)
                .param("fecha", e.fecha()).param("cerrado", e.cerrado()).param("abre", e.abre())
                .param("cierra", e.cierra()).param("motivo", e.motivo()).update() == 1);
    }

    @Override
    public Optional<UUID> quitarExcepcion(UUID negocioId, UUID excepcionId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("DELETE FROM negocios.horario_excepcion "
                        + "WHERE negocio_id = :negocio AND id = :id RETURNING sucursal_id")
                .param("negocio", negocioId).param("id", excepcionId)
                .query((r, n) -> uuid(r, "sucursal_id")).optional());
    }

    static ExcepcionHorario leerExcepcion(ResultSet r, int fila) throws SQLException {
        return new ExcepcionHorario(uuid(r, "id"), r.getObject("fecha", java.time.LocalDate.class),
                r.getBoolean("cerrado"), r.getObject("abre", LocalTime.class), r.getObject("cierra", LocalTime.class),
                r.getString("motivo"));
    }
}
