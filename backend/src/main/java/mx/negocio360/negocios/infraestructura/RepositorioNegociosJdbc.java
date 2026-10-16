package mx.negocio360.negocios.infraestructura;

import static mx.negocio360.negocios.infraestructura.MomentosJdbc.entero;
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
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.Disponibilidad;
import mx.negocio360.negocios.dominio.EstadoNegocio;
import mx.negocio360.negocios.dominio.Negocio;
import mx.negocio360.negocios.dominio.NegociosException;
import mx.negocio360.negocios.dominio.OrigenSuspension;
import mx.negocio360.negocios.dominio.Sucursal;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Negocio, subcategorias, atributos y disponibilidad en el esquema {@code negocios}, con seguridad por fila. */
public final class RepositorioNegociosJdbc implements RepositorioNegocios {

    static final String COLUMNAS_NEGOCIO = "id, nombre, giro, estado, suspendido_por, publicado, descripcion, "
            + "telefono_contacto, correo_contacto, contacto_publico, zona_horaria, logo_archivo_id, solicitado_por, "
            + "duracion_visita_min, costo_medio_mxn, es_simulado, motivo_estado, creado_en, actualizado_en, "
            + "cerrado_en, version";
    static final String COLUMNAS_SUCURSAL = "id, negocio_id, nombre, es_principal, activa, calle, numero, colonia, "
            + "municipio, entidad_federativa, codigo_postal, referencias, latitud, longitud, telefono, creada_en, "
            + "actualizada_en, version";

    private final JdbcClient jdbc;
    private final EjecutorEnContexto contexto;

    public RepositorioNegociosJdbc(JdbcClient jdbc, EjecutorEnContexto contexto) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.contexto = Objects.requireNonNull(contexto, "contexto");
    }

    @Override
    public void crear(Negocio n, Sucursal principal) {
        contexto.enNegocio(n.id(), () -> {
            jdbc.sql("""
                    INSERT INTO negocios.negocio (id, nombre, giro, estado, suspendido_por, publicado, descripcion,
                        telefono_contacto, correo_contacto, contacto_publico, zona_horaria, logo_archivo_id,
                        solicitado_por, duracion_visita_min, costo_medio_mxn, es_simulado, motivo_estado, creado_en,
                        actualizado_en, cerrado_en, version)
                    VALUES (:id, :nombre, :giro, :estado, :suspendidoPor, :publicado, :descripcion, :telefono, :correo,
                        :contactoPublico, :zona, :logo, :solicitadoPor, :duracion, :costo, :simulado, :motivo, :creado,
                        :actualizado, :cerrado, :version)""")
                    .param("id", n.id()).param("nombre", n.nombre()).param("giro", n.giro())
                    .param("estado", n.estado().name())
                    .param("suspendidoPor", n.suspendidoPor() == null ? null : n.suspendidoPor().name())
                    .param("publicado", n.publicado()).param("descripcion", n.descripcion())
                    .param("telefono", n.telefonoContacto()).param("correo", n.correoContacto())
                    .param("contactoPublico", n.contactoPublico()).param("zona", n.zonaHoraria())
                    .param("logo", n.logoArchivoId()).param("solicitadoPor", n.solicitadoPor())
                    .param("duracion", n.duracionVisitaMin()).param("costo", n.costoMedioMxn())
                    .param("simulado", n.esSimulado()).param("motivo", n.motivoEstado())
                    .param("creado", momento(n.creadoEn())).param("actualizado", momento(n.actualizadoEn()))
                    .param("cerrado", momento(n.cerradoEn())).param("version", n.version()).update();
            jdbc.sql("""
                    INSERT INTO negocios.sucursal (id, negocio_id, nombre, es_principal, activa, creada_en,
                        actualizada_en, version)
                    VALUES (:id, :negocio, :nombre, :principal, :activa, :creada, :actualizada, :version)""")
                    .param("id", principal.id()).param("negocio", principal.negocioId())
                    .param("nombre", principal.nombre()).param("principal", principal.esPrincipal())
                    .param("activa", principal.activa()).param("creada", momento(principal.creadaEn()))
                    .param("actualizada", momento(principal.actualizadaEn())).param("version", principal.version())
                    .update();
            return null;
        });
    }

    @Override
    public Optional<Negocio> negocio(UUID id) {
        return contexto.enNegocio(id, () -> jdbc.sql("SELECT " + COLUMNAS_NEGOCIO + " FROM negocios.negocio "
                + "WHERE id = :id").param("id", id).query(RepositorioNegociosJdbc::leerNegocio).optional());
    }

    @Override
    public Optional<Sucursal> principal(UUID negocioId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS_SUCURSAL + " FROM negocios.sucursal "
                + "WHERE negocio_id = :negocio AND es_principal AND activa").param("negocio", negocioId)
                .query(RepositorioNegociosJdbc::leerSucursal).optional());
    }

    @Override
    public void actualizar(Negocio n, long versionEsperada) {
        var filas = contexto.enNegocio(n.id(), () -> jdbc.sql("""
                UPDATE negocios.negocio SET nombre = :nombre, publicado = :publicado, descripcion = :descripcion,
                    telefono_contacto = :telefono, correo_contacto = :correo, contacto_publico = :contactoPublico,
                    duracion_visita_min = :duracion, costo_medio_mxn = :costo, actualizado_en = :actualizado,
                    version = version + 1
                WHERE id = :id AND version = :esperada""")
                .param("nombre", n.nombre()).param("publicado", n.publicado()).param("descripcion", n.descripcion())
                .param("telefono", n.telefonoContacto()).param("correo", n.correoContacto())
                .param("contactoPublico", n.contactoPublico()).param("duracion", n.duracionVisitaMin())
                .param("costo", n.costoMedioMxn()).param("actualizado", momento(n.actualizadoEn()))
                .param("id", n.id()).param("esperada", versionEsperada).update());
        if (filas != 1) {
            throw new NegociosException(CodigoError.CONCURRENCIA);
        }
    }

    @Override
    public List<String> subcategorias(UUID negocioId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("""
                SELECT ns.subcategoria_codigo FROM negocios.negocio_subcategoria ns
                    JOIN negocios.subcategoria s ON s.codigo = ns.subcategoria_codigo
                WHERE ns.negocio_id = :negocio ORDER BY s.orden, s.codigo""").param("negocio", negocioId)
                .query(String.class).list());
    }

    @Override
    public void reemplazarSubcategorias(UUID negocioId, List<String> codigos, Instant ahora) {
        contexto.enNegocio(negocioId, () -> {
            jdbc.sql("DELETE FROM negocios.negocio_subcategoria WHERE negocio_id = :negocio")
                    .param("negocio", negocioId).update();
            for (var codigo : codigos) {
                jdbc.sql("INSERT INTO negocios.negocio_subcategoria (negocio_id, subcategoria_codigo, creada_en) "
                                + "VALUES (:negocio, :codigo, :ahora)")
                        .param("negocio", negocioId).param("codigo", codigo).param("ahora", momento(ahora)).update();
            }
            return null;
        });
    }

    @Override
    public AtributosElegidos atributos(UUID negocioId) {
        return contexto.enNegocio(negocioId, () -> {
            var filas = jdbc.sql("""
                    SELECT na.tipo, na.codigo FROM negocios.negocio_atributo na
                        JOIN negocios.catalogo_atributo c ON c.tipo = na.tipo AND c.codigo = na.codigo
                    WHERE na.negocio_id = :negocio ORDER BY c.orden, c.codigo""").param("negocio", negocioId)
                    .query((r, n) -> new String[] {r.getString("tipo"), r.getString("codigo")}).list();
            return new AtributosElegidos(filas.stream().filter(f -> f[0].equals("DIETA")).map(f -> f[1]).toList(),
                    filas.stream().filter(f -> f[0].equals("ACCESIBILIDAD")).map(f -> f[1]).toList());
        });
    }

    @Override
    public void reemplazarAtributos(UUID negocioId, List<String> dieta, List<String> accesibilidad, Instant ahora) {
        contexto.enNegocio(negocioId, () -> {
            jdbc.sql("DELETE FROM negocios.negocio_atributo WHERE negocio_id = :negocio")
                    .param("negocio", negocioId).update();
            insertarAtributos(negocioId, "DIETA", dieta, ahora);
            insertarAtributos(negocioId, "ACCESIBILIDAD", accesibilidad, ahora);
            return null;
        });
    }

    private void insertarAtributos(UUID negocioId, String tipo, List<String> codigos, Instant ahora) {
        for (var codigo : codigos) {
            jdbc.sql("INSERT INTO negocios.negocio_atributo (negocio_id, tipo, codigo, creada_en) "
                            + "VALUES (:negocio, :tipo, :codigo, :ahora)")
                    .param("negocio", negocioId).param("tipo", tipo).param("codigo", codigo)
                    .param("ahora", momento(ahora)).update();
        }
    }

    @Override
    public Optional<Disponibilidad> disponibilidad(UUID negocioId) {
        return contexto.enNegocio(negocioId, () -> jdbc.sql("""
                SELECT negocio_id, nivel, nota, fuente, declarada_por, declarada_en, vence_en
                FROM negocios.disponibilidad WHERE negocio_id = :negocio""").param("negocio", negocioId)
                .query((r, n) -> new Disponibilidad(uuid(r, "negocio_id"), Disponibilidad.Nivel.valueOf(r.getString("nivel")),
                        r.getString("nota"), Disponibilidad.Fuente.valueOf(r.getString("fuente")),
                        uuid(r, "declarada_por"), instante(r, "declarada_en"), instante(r, "vence_en"))).optional());
    }

    @Override
    public void guardarDisponibilidad(Disponibilidad d) {
        contexto.enNegocio(d.negocioId(), () -> jdbc.sql("""
                INSERT INTO negocios.disponibilidad (negocio_id, nivel, nota, fuente, declarada_por, declarada_en,
                    vence_en)
                VALUES (:negocio, :nivel, :nota, :fuente, :por, :declarada, :vence)
                ON CONFLICT (negocio_id) DO UPDATE SET nivel = EXCLUDED.nivel, nota = EXCLUDED.nota,
                    fuente = EXCLUDED.fuente, declarada_por = EXCLUDED.declarada_por,
                    declarada_en = EXCLUDED.declarada_en, vence_en = EXCLUDED.vence_en""")
                .param("negocio", d.negocioId()).param("nivel", d.nivel().name()).param("nota", d.nota())
                .param("fuente", d.fuente().name()).param("por", d.declaradaPor())
                .param("declarada", momento(d.declaradaEn())).param("vence", momento(d.venceEn())).update());
    }

    @Override
    public void borrarDisponibilidad(UUID negocioId) {
        contexto.enNegocio(negocioId, () -> jdbc.sql("DELETE FROM negocios.disponibilidad WHERE negocio_id = :negocio")
                .param("negocio", negocioId).update());
    }

    @Override
    public void registrarHistorial(UUID negocioId, String accion, List<String> campos, String actor, Instant ahora) {
        contexto.enNegocio(negocioId, () -> jdbc.sql("""
                INSERT INTO negocios.historial_negocio (id, negocio_id, accion, campos, actor, ocurrido_en)
                VALUES (:id, :negocio, :accion, string_to_array(:campos, ','), :actor, :ahora)""")
                .param("id", UuidV7.generar(ahora)).param("negocio", negocioId).param("accion", accion)
                .param("campos", String.join(",", campos)).param("actor", actor).param("ahora", momento(ahora))
                .update());
    }

    static Negocio leerNegocio(ResultSet f, int fila) throws SQLException {
        var suspendido = f.getString("suspendido_por");
        return new Negocio(uuid(f, "id"), f.getString("nombre"), f.getString("giro"),
                EstadoNegocio.valueOf(f.getString("estado")),
                suspendido == null ? null : OrigenSuspension.valueOf(suspendido), f.getBoolean("publicado"),
                f.getString("descripcion"), f.getString("telefono_contacto"), f.getString("correo_contacto"),
                f.getBoolean("contacto_publico"), f.getString("zona_horaria"), uuid(f, "logo_archivo_id"),
                uuid(f, "solicitado_por"), entero(f, "duracion_visita_min"), entero(f, "costo_medio_mxn"),
                f.getBoolean("es_simulado"), f.getString("motivo_estado"), instante(f, "creado_en"),
                instante(f, "actualizado_en"), instante(f, "cerrado_en"), f.getLong("version"));
    }

    static Sucursal leerSucursal(ResultSet f, int fila) throws SQLException {
        return new Sucursal(uuid(f, "id"), uuid(f, "negocio_id"), f.getString("nombre"), f.getBoolean("es_principal"),
                f.getBoolean("activa"), f.getString("calle"), f.getString("numero"), f.getString("colonia"),
                f.getString("municipio"), f.getString("entidad_federativa"), f.getString("codigo_postal"),
                f.getString("referencias"), f.getBigDecimal("latitud"), f.getBigDecimal("longitud"),
                f.getString("telefono"), instante(f, "creada_en"), instante(f, "actualizada_en"), f.getLong("version"));
    }
}
