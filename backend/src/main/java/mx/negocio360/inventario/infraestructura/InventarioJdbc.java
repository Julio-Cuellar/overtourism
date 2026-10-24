package mx.negocio360.inventario.infraestructura;

import static mx.negocio360.compartido.transaccion.MomentosJdbc.instante;
import static mx.negocio360.compartido.transaccion.MomentosJdbc.momento;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import mx.negocio360.inventario.aplicacion.ArticuloConSaldo;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioConsumos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioMovimientos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.inventario.dominio.Articulo;
import mx.negocio360.inventario.dominio.CausaTipo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.Consumo;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.dominio.MotivoMerma;
import mx.negocio360.inventario.dominio.Movimiento;
import mx.negocio360.inventario.dominio.Rastreo;
import mx.negocio360.inventario.dominio.Saldo;
import mx.negocio360.inventario.dominio.TipoMovimiento;
import mx.negocio360.inventario.dominio.UnidadEstandar;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import java.util.List;

/**
 * Tablas {@code inventario.articulo}, {@code saldo}, {@code movimiento} y {@code consumo} en PostgreSQL. Cada operacion corre con el
 * negocio fijado en la transaccion ({@link EjecutorEnNegocio}), que es lo que usa la seguridad por fila.
 *
 * <p>Articulo y saldo se actualizan con un {@code UPDATE} condicionado a la version esperada; si ya cambio, es
 * {@code CONCURRENCIA} (el servicio la reintenta). El nombre de un articulo activo es unico por negocio y lo respalda un
 * indice: si dos altas simultaneas pasan la revision del servicio, la segunda recibe {@code NOMBRE_DUPLICADO}. El kardex
 * solo se inserta y se recorre por id (UUIDv7: orden de registro). Las unidades propias se guardan como cadenas decimales
 * en {@code jsonb}, para no perder la escala.
 *
 * <p>El consumo por venta (K-01 a K-04) es una fila por (negocio, causa, consumidor). Se inserta con
 * {@code ON CONFLICT DO NOTHING} y no con la excepcion de llave duplicada: asi una carrera no aborta la transaccion de
 * quien la perdio, y el servicio puede devolverle {@code CONCURRENCIA} (que revierte todo lo suyo, K-02) en lugar de un
 * error de la base. Lo escrito en la misma transaccion (movimientos, saldos y la fila) queda todo o nada.
 */
public final class InventarioJdbc implements RepositorioArticulos, RepositorioSaldos, RepositorioMovimientos,
        RepositorioConsumos {

    private static final String COLUMNAS_ARTICULO = "a.id, a.negocio_id, a.nombre, a.categoria, a.unidad_base, "
            + "a.rastreo, a.unidades_propias::text AS unidades_propias, a.minimo_base, a.activo, a.version, "
            + "a.creado_en, a.actualizado_en, a.creado_por";
    private static final String COLUMNAS_MOVIMIENTO = "id, negocio_id, articulo_id, tipo, cantidad_capturada, "
            + "unidad_capturada, factor_aplicado, cantidad_base, costo_unitario, costo_total, cpp_resultante, "
            + "saldo_resultante, causa_tipo, causa_id, motivo, ocurrido_en, registrado_en, registrado_por";
    private static final String COLUMNAS_CONSUMO = "negocio_id, causa_id, consumidor, estado, lineas::text AS lineas, "
            + "costo_total, movimientos_reversa::text AS movimientos_reversa, registrado_en";
    private static final TypeReference<List<Map<String, String>>> LINEAS = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> IDS = new TypeReference<>() {
    };
    private static final String COLUMNAS_SALDO = "articulo_id, negocio_id, cantidad_base, cpp, ultimo_movimiento_id, version";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, String>> UNIDADES = new TypeReference<>() {
    };

    private final JdbcClient jdbc;
    private final EjecutorEnNegocio negocio;

    /**
     * Arma el repositorio.
     *
     * @param jdbc    cliente de la base
     * @param negocio ejecutor que fija el negocio en cada transaccion
     */
    public InventarioJdbc(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.negocio = Objects.requireNonNull(negocio, "negocio");
    }

    // ---- articulos ----

    @Override
    public Optional<Articulo> porId(UUID negocioId, UUID id) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS_ARTICULO + " FROM inventario.articulo a "
                        + "WHERE a.negocio_id = :negocio AND a.id = :id")
                .param("negocio", negocioId).param("id", id).query(InventarioJdbc::leerArticulo).optional());
    }

    @Override
    public Optional<Articulo> activoPorNombre(UUID negocioId, String nombre) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS_ARTICULO + " FROM inventario.articulo a "
                        + "WHERE a.negocio_id = :negocio AND a.activo AND lower(a.nombre) = lower(:nombre)")
                .param("negocio", negocioId).param("nombre", nombre).query(InventarioJdbc::leerArticulo).optional());
    }

    @Override
    public void insertar(Articulo a) {
        conNombreUnico(a.id(), () -> negocio.en(a.negocioId(), () -> jdbc.sql("INSERT INTO inventario.articulo (id, "
                        + "negocio_id, nombre, categoria, unidad_base, rastreo, unidades_propias, minimo_base, activo, version, "
                        + "creado_en, actualizado_en, creado_por) VALUES (:id, :negocio, :nombre, :categoria, :unidad, "
                        + ":rastreo, CAST(:propias AS jsonb), :minimo, :activo, :version, :creado, :actualizado, "
                        + ":creadoPor)")
                .param("id", a.id()).param("negocio", a.negocioId()).param("nombre", a.nombre())
                .param("categoria", a.categoria(), Types.VARCHAR).param("unidad", a.unidadBase().name())
                .param("rastreo", a.rastreo().name()).param("propias", unidadesAJson(a.unidadesPropias())).param("minimo", a.minimoBase())
                .param("activo", a.activo()).param("version", a.version()).param("creado", momento(a.creadoEn()))
                .param("actualizado", momento(a.actualizadoEn())).param("creadoPor", a.creadoPor()).update()));
    }

    @Override
    public void actualizar(Articulo nuevo, long versionEsperada) {
        var filas = conNombreUnico(nuevo.id(), () -> negocio.en(nuevo.negocioId(), () -> jdbc.sql("UPDATE "
                        + "inventario.articulo SET nombre = :nombre, categoria = :categoria, "
                        + "unidades_propias = CAST(:propias AS jsonb), minimo_base = :minimo, activo = :activo, "
                        + "version = :version, actualizado_en = :actualizado "
                        + "WHERE negocio_id = :negocio AND id = :id AND version = :esperada")
                .param("nombre", nuevo.nombre()).param("categoria", nuevo.categoria(), Types.VARCHAR)
                .param("propias", unidadesAJson(nuevo.unidadesPropias())).param("minimo", nuevo.minimoBase())
                .param("activo", nuevo.activo()).param("version", nuevo.version())
                .param("actualizado", momento(nuevo.actualizadoEn())).param("negocio", nuevo.negocioId())
                .param("id", nuevo.id()).param("esperada", versionEsperada).update()));
        if (filas != 1) {
            throw new InventarioException(CodigoError.CONCURRENCIA);
        }
    }

    /** Traduce la violacion del indice del nombre activo a {@code NOMBRE_DUPLICADO}: el servicio ya no la distingue. */
    private static <T> T conNombreUnico(UUID articulo, Supplier<T> trabajo) {
        try {
            return trabajo.get();
        } catch (DuplicateKeyException e) {
            if (String.valueOf(e.getMessage()).contains("ux_articulo_nombre_activo")) {
                throw new InventarioException(CodigoError.NOMBRE_DUPLICADO, articulo);
            }
            throw e;
        }
    }

    @Override
    public List<ArticuloConSaldo> buscar(UUID negocioId, String texto, boolean soloAlertas, UUID despuesDe, int limite) {
        var parte = texto == null ? "" : texto.strip();
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS_ARTICULO + ", s.articulo_id AS s_articulo, "
                        + "s.cantidad_base AS s_cantidad, s.cpp AS s_cpp, s.ultimo_movimiento_id AS s_ultimo, "
                        + "s.version AS s_version FROM inventario.articulo a "
                        + "LEFT JOIN inventario.saldo s ON s.negocio_id = a.negocio_id AND s.articulo_id = a.id "
                        + "WHERE a.negocio_id = :negocio AND a.activo "
                        + "AND position(lower(CAST(:texto AS text)) IN lower(a.nombre)) > 0 "
                        + "AND (CAST(:despues AS uuid) IS NULL OR a.id > :despues) "
                        + "AND (NOT :soloAlertas OR (a.minimo_base > 0 AND coalesce(s.cantidad_base, 0) <= a.minimo_base) "
                        + "OR coalesce(s.cantidad_base, 0) < 0) "
                        + "ORDER BY a.id LIMIT :limite")
                .param("negocio", negocioId).param("texto", parte).param("despues", despuesDe, Types.OTHER)
                .param("soloAlertas", soloAlertas).param("limite", limite)
                .query((fila, n) -> {
                    var articulo = leerArticulo(fila, n);
                    var saldo = fila.getObject("s_articulo", UUID.class) == null ? null : new Saldo(articulo.id(),
                            articulo.negocioId(), fila.getBigDecimal("s_cantidad"), fila.getBigDecimal("s_cpp"),
                            fila.getObject("s_ultimo", UUID.class), fila.getLong("s_version"));
                    return new ArticuloConSaldo(articulo, saldo);
                }).list());
    }

    // ---- saldos ----

    @Override
    public Optional<Saldo> porArticulo(UUID negocioId, UUID articuloId) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS_SALDO + " FROM inventario.saldo "
                        + "WHERE negocio_id = :negocio AND articulo_id = :articulo")
                .param("negocio", negocioId).param("articulo", articuloId).query(InventarioJdbc::leerSaldo).optional());
    }

    @Override
    public void insertar(Saldo s) {
        negocio.en(s.negocioId(), () -> jdbc.sql("INSERT INTO inventario.saldo (" + COLUMNAS_SALDO + ") VALUES (:articulo, "
                        + ":negocio, :cantidad, :cpp, :ultimo, :version)")
                .param("articulo", s.articuloId()).param("negocio", s.negocioId()).param("cantidad", s.cantidadBase())
                .param("cpp", s.cpp()).param("ultimo", s.ultimoMovimientoId(), Types.OTHER).param("version", s.version())
                .update());
    }

    @Override
    public void actualizar(Saldo nuevo, long versionEsperada) {
        var filas = negocio.en(nuevo.negocioId(), () -> jdbc.sql("UPDATE inventario.saldo SET cantidad_base = :cantidad, "
                        + "cpp = :cpp, ultimo_movimiento_id = :ultimo, version = :version "
                        + "WHERE negocio_id = :negocio AND articulo_id = :articulo AND version = :esperada")
                .param("cantidad", nuevo.cantidadBase()).param("cpp", nuevo.cpp())
                .param("ultimo", nuevo.ultimoMovimientoId(), Types.OTHER).param("version", nuevo.version())
                .param("negocio", nuevo.negocioId()).param("articulo", nuevo.articuloId())
                .param("esperada", versionEsperada).update());
        if (filas != 1) {
            throw new InventarioException(CodigoError.CONCURRENCIA);
        }
    }

    // ---- kardex ----

    @Override
    public void insertar(Movimiento m) {
        negocio.en(m.negocioId(), () -> jdbc.sql("INSERT INTO inventario.movimiento (" + COLUMNAS_MOVIMIENTO + ") VALUES ("
                        + ":id, :negocio, :articulo, :tipo, :capturada, :unidad, :factor, :base, :costoUnitario, "
                        + ":costoTotal, :cpp, :saldo, :causa, :causaId, :motivo, :ocurrido, :registrado, :registradoPor)")
                .param("id", m.id()).param("negocio", m.negocioId()).param("articulo", m.articuloId())
                .param("tipo", m.tipo().name()).param("capturada", m.cantidadCapturada())
                .param("unidad", m.unidadCapturada()).param("factor", m.factorAplicado())
                .param("base", m.cantidadBase()).param("costoUnitario", m.costoUnitario())
                .param("costoTotal", m.costoTotal()).param("cpp", m.cppResultante()).param("saldo", m.saldoResultante())
                .param("causa", m.causaTipo().name()).param("causaId", m.causaId(), Types.OTHER)
                .param("motivo", m.motivo() == null ? null : m.motivo().name(), Types.VARCHAR)
                .param("ocurrido", momento(m.ocurridoEn())).param("registrado", momento(m.registradoEn()))
                .param("registradoPor", m.registradoPor()).update());
    }

    @Override
    public List<Movimiento> kardex(UUID negocioId, UUID articuloId, UUID despuesDe, int limite) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS_MOVIMIENTO + " FROM inventario.movimiento "
                        + "WHERE negocio_id = :negocio AND articulo_id = :articulo "
                        + "AND (CAST(:despues AS uuid) IS NULL OR id > :despues) ORDER BY id LIMIT :limite")
                .param("negocio", negocioId).param("articulo", articuloId).param("despues", despuesDe, Types.OTHER)
                .param("limite", limite).query(InventarioJdbc::leerMovimiento).list());
    }

    // ---- consumos por venta (K-01 a K-04) ----

    @Override
    public Optional<Consumo> porLlave(UUID negocioId, UUID causaId, String consumidor) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS_CONSUMO + " FROM inventario.consumo "
                        + "WHERE negocio_id = :negocio AND causa_id = :causa AND consumidor = :consumidor")
                .param("negocio", negocioId).param("causa", causaId).param("consumidor", consumidor)
                .query(InventarioJdbc::leerConsumo).optional());
    }

    @Override
    public void insertar(Consumo c) {
        var filas = negocio.en(c.negocioId(), () -> jdbc.sql("INSERT INTO inventario.consumo (negocio_id, causa_id, "
                        + "consumidor, estado, lineas, costo_total, movimientos_reversa, registrado_en) VALUES (:negocio, "
                        + ":causa, :consumidor, :estado, CAST(:lineas AS jsonb), :costo, CAST(:reversa AS jsonb), "
                        + ":registrado) ON CONFLICT (negocio_id, causa_id, consumidor) DO NOTHING")
                .param("negocio", c.negocioId()).param("causa", c.causaId()).param("consumidor", c.consumidor())
                .param("estado", c.estado().name()).param("lineas", lineasAJson(c.lineas()))
                .param("costo", c.costoTotal()).param("reversa", idsAJson(c.movimientosReversa()))
                .param("registrado", momento(c.registradoEn())).update());
        if (filas != 1) {
            throw new InventarioException(CodigoError.CONCURRENCIA);
        }
    }

    @Override
    public void actualizar(Consumo c) {
        var filas = negocio.en(c.negocioId(), () -> jdbc.sql("UPDATE inventario.consumo SET estado = :estado, "
                        + "lineas = CAST(:lineas AS jsonb), costo_total = :costo, "
                        + "movimientos_reversa = CAST(:reversa AS jsonb) "
                        + "WHERE negocio_id = :negocio AND causa_id = :causa AND consumidor = :consumidor")
                .param("estado", c.estado().name()).param("lineas", lineasAJson(c.lineas()))
                .param("costo", c.costoTotal()).param("reversa", idsAJson(c.movimientosReversa()))
                .param("negocio", c.negocioId()).param("causa", c.causaId()).param("consumidor", c.consumidor())
                .update());
        if (filas != 1) {
            throw new InventarioException(CodigoError.CONCURRENCIA);
        }
    }

    // ---- lectura ----

    private static Articulo leerArticulo(ResultSet fila, int numero) throws SQLException {
        return new Articulo(fila.getObject("id", UUID.class), fila.getObject("negocio_id", UUID.class),
                fila.getString("nombre"), fila.getString("categoria"), UnidadEstandar.valueOf(fila.getString("unidad_base")),
                Rastreo.valueOf(fila.getString("rastreo")), unidadesDeJson(fila.getString("unidades_propias")), fila.getBigDecimal("minimo_base"),
                fila.getBoolean("activo"), fila.getLong("version"), instante(fila, "creado_en"),
                instante(fila, "actualizado_en"), fila.getString("creado_por"));
    }

    private static Saldo leerSaldo(ResultSet fila, int numero) throws SQLException {
        return new Saldo(fila.getObject("articulo_id", UUID.class), fila.getObject("negocio_id", UUID.class),
                fila.getBigDecimal("cantidad_base"), fila.getBigDecimal("cpp"),
                fila.getObject("ultimo_movimiento_id", UUID.class), fila.getLong("version"));
    }

    private static Movimiento leerMovimiento(ResultSet fila, int numero) throws SQLException {
        var motivo = fila.getString("motivo");
        return new Movimiento(fila.getObject("id", UUID.class), fila.getObject("negocio_id", UUID.class),
                fila.getObject("articulo_id", UUID.class), TipoMovimiento.valueOf(fila.getString("tipo")),
                fila.getBigDecimal("cantidad_capturada"), fila.getString("unidad_capturada"),
                fila.getBigDecimal("factor_aplicado"), fila.getBigDecimal("cantidad_base"),
                fila.getBigDecimal("costo_unitario"), fila.getBigDecimal("costo_total"),
                fila.getBigDecimal("cpp_resultante"), fila.getBigDecimal("saldo_resultante"),
                CausaTipo.valueOf(fila.getString("causa_tipo")), fila.getObject("causa_id", UUID.class),
                motivo == null ? null : MotivoMerma.valueOf(motivo), instante(fila, "ocurrido_en"),
                instante(fila, "registrado_en"), fila.getString("registrado_por"));
    }

    private static Consumo leerConsumo(ResultSet fila, int numero) throws SQLException {
        return new Consumo(fila.getObject("negocio_id", UUID.class), fila.getObject("causa_id", UUID.class),
                fila.getString("consumidor"), Consumo.Estado.valueOf(fila.getString("estado")),
                lineasDeJson(fila.getString("lineas")), fila.getBigDecimal("costo_total"),
                idsDeJson(fila.getString("movimientos_reversa")), instante(fila, "registrado_en"));
    }

    /** Las lineas con sus cantidades y costos como cadenas decimales, para conservar la escala. */
    private static String lineasAJson(List<Consumo.LineaConsumida> lineas) {
        var filas = lineas.stream().map(l -> Map.of("articulo_id", l.articuloId().toString(),
                "movimiento_id", l.movimientoId().toString(), "cantidad_base", l.cantidadBase().toPlainString(),
                "costo_unitario", l.costoUnitario().toPlainString())).toList();
        return escribirJson(filas);
    }

    private static List<Consumo.LineaConsumida> lineasDeJson(String json) {
        try {
            return JSON.readValue(json, LINEAS).stream().map(l -> new Consumo.LineaConsumida(
                    UUID.fromString(l.get("articulo_id")), UUID.fromString(l.get("movimiento_id")),
                    new BigDecimal(l.get("cantidad_base")), new BigDecimal(l.get("costo_unitario")))).toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Lineas de consumo ilegibles en la base", e);
        }
    }

    private static String idsAJson(List<UUID> ids) {
        return escribirJson(ids.stream().map(UUID::toString).toList());
    }

    private static List<UUID> idsDeJson(String json) {
        try {
            return JSON.readValue(json, IDS).stream().map(UUID::fromString).toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Movimientos de reversa ilegibles en la base", e);
        }
    }

    private static String escribirJson(Object valor) {
        try {
            return JSON.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo escribir el JSON", e);
        }
    }

    private static String unidadesAJson(Map<String, BigDecimal> propias) {
        var texto = new TreeMap<String, String>();
        propias.forEach((nombre, factor) -> texto.put(nombre, factor.toPlainString()));
        try {
            return JSON.writeValueAsString(texto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron serializar las unidades propias", e);
        }
    }

    private static Map<String, BigDecimal> unidadesDeJson(String json) {
        try {
            var propias = new TreeMap<String, BigDecimal>();
            JSON.readValue(json, UNIDADES).forEach((nombre, factor) -> propias.put(nombre, new BigDecimal(factor)));
            return propias;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Las unidades propias guardadas no se pueden leer", e);
        }
    }
}
