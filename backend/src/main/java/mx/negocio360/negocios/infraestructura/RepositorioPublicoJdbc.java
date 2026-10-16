package mx.negocio360.negocios.infraestructura;

import static mx.negocio360.negocios.infraestructura.MomentosJdbc.entero;
import static mx.negocio360.negocios.infraestructura.MomentosJdbc.instante;
import static mx.negocio360.negocios.infraestructura.MomentosJdbc.uuid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioPublico;
import mx.negocio360.negocios.dominio.DiaHorario;
import mx.negocio360.negocios.dominio.ExcepcionHorario;
import mx.negocio360.negocios.dominio.ResumenCalificacion;
import mx.negocio360.negocios.dominio.SemanaHorario;
import mx.negocio360.negocios.dominio.Tramo;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Lee la vista {@code negocios.v_negocio_publico} con el rol {@code rol_negocios_publico}, que no tiene permiso sobre
 * ninguna tabla base (SN-14, PU-03): cambia de rol solo dentro de su transaccion y lo devuelve al terminar, para no
 * dejar el rol publico puesto en una transaccion que siga con otras consultas. La seguridad por fila no aplica aqui: la
 * vista corre con los privilegios de su dueño y solo trae negocios publicados y activos.
 */
public final class RepositorioPublicoJdbc implements RepositorioPublico {

    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final UUID SIN_ID = new UUID(0, 0);
    private static final String COLUMNAS = "id, nombre, giro, giro_nombre, descripcion, telefono, correo, zona_horaria, "
            + "simulado, duracion_visita_min, costo_medio_mxn, calle, numero, colonia, municipio, entidad_federativa, "
            + "codigo_postal, referencias, latitud, longitud, subcategorias::text AS subcategorias, "
            + "dieta::text AS dieta, accesibilidad::text AS accesibilidad, horario::text AS horario, "
            + "excepciones::text AS excepciones, disponibilidad_nivel, disponibilidad_nota, "
            + "disponibilidad_declarada_en, disponibilidad_vence_en, calificacion_c1, calificacion_c2, calificacion_c3, "
            + "calificacion_c4, calificacion_c5";
    /** Distancia en km sobre la esfera (haversine) entre la sucursal principal y el punto de busqueda. */
    private static final String DISTANCIA = "2 * 6371 * asin(sqrt(power(sin(radians(latitud::double precision - :lat) / 2), 2) "
            + "+ cos(radians(:lat)) * cos(radians(latitud::double precision)) "
            + "* power(sin(radians(longitud::double precision - :lon) / 2), 2)))";

    private final JdbcClient jdbc;
    private final EjecutorPublico publico;

    public RepositorioPublicoJdbc(JdbcClient jdbc, PlatformTransactionManager transacciones) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.publico = new EjecutorPublico(jdbc, transacciones);
    }

    @Override
    public Optional<Fila> porId(UUID id) {
        return comoRolPublico(() -> jdbc.sql("SELECT " + COLUMNAS + ", NULL::double precision AS distancia_km "
                + "FROM negocios.v_negocio_publico WHERE id = :id").param("id", id)
                .query(RepositorioPublicoJdbc::leer).optional());
    }

    @Override
    public List<Fila> buscar(Criterio c, int limite, int desplazamiento) {
        var parametros = new HashMap<String, Object>();
        var donde = new StringBuilder(" WHERE true");
        if (c.texto() != null) {
            donde.append(" AND nombre_busqueda LIKE '%' || lower(negocios.f_unaccent(:texto)) || '%' ESCAPE '\\'");
            parametros.put("texto", c.texto().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"));
        }
        if (c.giro() != null) {
            donde.append(" AND giro = :giro");
            parametros.put("giro", c.giro());
        }
        contiene(donde, parametros, "subcategorias", c.subcategorias());
        contiene(donde, parametros, "dieta", c.dieta());
        contiene(donde, parametros, "accesibilidad", c.accesibilidad());
        var conCerca = c.latitud() != null;
        String distancia = "NULL::double precision";
        if (conCerca) {
            distancia = DISTANCIA;
            parametros.put("lat", c.latitud());
            parametros.put("lon", c.longitud());
            donde.append(" AND latitud IS NOT NULL");
        }
        var interior = "SELECT " + COLUMNAS + ", " + distancia + " AS distancia_km FROM negocios.v_negocio_publico"
                + donde;
        var sql = new StringBuilder("SELECT * FROM (").append(interior).append(") t");
        if (conCerca) {
            sql.append(" WHERE distancia_km <= :radio");
            parametros.put("radio", c.radioKm());
        }
        sql.append(c.ordenarPorDistancia() && conCerca ? " ORDER BY distancia_km, nombre_busqueda_orden, id"
                : " ORDER BY nombre_busqueda_orden, id").append(" LIMIT :limite OFFSET :desplazamiento");
        parametros.put("limite", limite);
        parametros.put("desplazamiento", desplazamiento);
        var consulta = sql.toString().replace("nombre_busqueda_orden", "lower(nombre)");
        return comoRolPublico(() -> jdbc.sql(consulta).params(parametros).query(RepositorioPublicoJdbc::leer).list());
    }

    private static void contiene(StringBuilder donde, Map<String, Object> parametros, String columna,
                                 List<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return;
        }
        var elementos = new ArrayList<Map<String, String>>();
        for (var codigo : codigos) {
            elementos.add(Map.of("codigo", codigo));
        }
        try {
            parametros.put(columna, JSON.writeValueAsString(elementos));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo armar el filtro", e);
        }
        donde.append(" AND ").append(columna).append(" @> CAST(:").append(columna).append(" AS jsonb)");
    }

    private <T> T comoRolPublico(java.util.function.Supplier<T> consulta) {
        return publico.leer(consulta);
    }

    private static Fila leer(ResultSet r, int fila) throws SQLException {
        try {
            var subcategorias = new ArrayList<SubcategoriaPublica>();
            for (var n : JSON.readTree(r.getString("subcategorias"))) {
                subcategorias.add(new SubcategoriaPublica(n.get("codigo").asText(), n.get("nombre").asText(),
                        n.get("giro").asText()));
            }
            var distancia = r.getObject("distancia_km", Double.class);
            return new Fila(uuid(r, "id"), r.getString("nombre"), r.getString("giro"), r.getString("giro_nombre"),
                    r.getString("descripcion"), r.getString("telefono"), r.getString("correo"),
                    r.getString("zona_horaria"), r.getBoolean("simulado"), entero(r, "duracion_visita_min"),
                    entero(r, "costo_medio_mxn"), r.getString("calle"), r.getString("numero"), r.getString("colonia"),
                    r.getString("municipio"), r.getString("entidad_federativa"), r.getString("codigo_postal"),
                    r.getString("referencias"), r.getBigDecimal("latitud"), r.getBigDecimal("longitud"), subcategorias,
                    etiquetas(r.getString("dieta")), etiquetas(r.getString("accesibilidad")),
                    semana(r.getString("horario")), excepciones(r.getString("excepciones")),
                    r.getString("disponibilidad_nivel"), r.getString("disponibilidad_nota"),
                    instante(r, "disponibilidad_declarada_en"), instante(r, "disponibilidad_vence_en"),
                    ResumenCalificacion.de(r.getInt("calificacion_c1"), r.getInt("calificacion_c2"),
                            r.getInt("calificacion_c3"), r.getInt("calificacion_c4"), r.getInt("calificacion_c5")),
                    distancia);
        } catch (JsonProcessingException e) {
            throw new SQLException("La vista publica trajo un JSON que no se entiende", e);
        }
    }

    private static List<Etiqueta> etiquetas(String json) throws JsonProcessingException {
        var lista = new ArrayList<Etiqueta>();
        for (var n : JSON.readTree(json)) {
            lista.add(new Etiqueta(n.get("codigo").asText(), n.get("nombre").asText()));
        }
        return lista;
    }

    private static SemanaHorario semana(String json) throws JsonProcessingException {
        var porDia = new LinkedHashMap<Integer, List<Tramo>>();
        for (var n : JSON.readTree(json)) {
            porDia.computeIfAbsent(n.get("dia").asInt(), d -> new ArrayList<>())
                    .add(Tramo.de(n.get("abre").asText(), n.get("cierra").asText()));
        }
        return new SemanaHorario(porDia.entrySet().stream().map(e -> new DiaHorario(e.getKey(), e.getValue())).toList());
    }

    private static List<ExcepcionHorario> excepciones(String json) throws JsonProcessingException {
        var lista = new ArrayList<ExcepcionHorario>();
        for (var n : JSON.readTree(json)) {
            var cerrado = n.get("cerrado").asBoolean();
            lista.add(ExcepcionHorario.de(SIN_ID, LocalDate.parse(n.get("fecha").asText()), cerrado,
                    cerrado ? null : texto(n, "abre"), cerrado ? null : texto(n, "cierra"), texto(n, "motivo")));
        }
        return lista;
    }

    private static String texto(JsonNode nodo, String campo) {
        var valor = nodo.get(campo);
        return valor == null || valor.isNull() ? null : valor.asText();
    }
}
