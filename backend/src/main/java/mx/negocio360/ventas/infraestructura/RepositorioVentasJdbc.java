package mx.negocio360.ventas.infraestructura;

import static mx.negocio360.compartido.transaccion.MomentosJdbc.instante;
import static mx.negocio360.compartido.transaccion.MomentosJdbc.momento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.paginacion.Cursores;
import mx.negocio360.compartido.paginacion.Pagina;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import mx.negocio360.ventas.FiltroVentas;
import mx.negocio360.ventas.aplicacion.puertos.RepositorioVentas;
import mx.negocio360.ventas.dominio.EstadoVenta;
import mx.negocio360.ventas.dominio.LineaVenta;
import mx.negocio360.ventas.dominio.MetodoPago;
import mx.negocio360.ventas.dominio.PagoVenta;
import mx.negocio360.ventas.dominio.Venta;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Tablas {@code ventas.venta}, {@code linea_venta}, {@code pago_venta} y {@code serie_folio} en PostgreSQL. Cada
 * operacion corre con el negocio fijado en la transaccion ({@link EjecutorEnNegocio}), que es lo que usa la seguridad
 * por fila: sin negocio la base no muestra ni deja escribir nada.
 *
 * <p>Una venta se guarda entera en la transaccion en curso (o en una propia si no hay): venta, lineas y pagos, o nada.
 * Lineas y pagos son de solo insercion y, de la venta, solo cambian el estado, la anulacion y la version
 * ({@code V040}). {@link #actualizar} es un {@code UPDATE} condicionado a la version esperada: el conteo decide cual de
 * dos anulaciones simultaneas gana (VT-11).
 *
 * <p>El folio sale de {@code serie_folio} con un {@code INSERT ... ON CONFLICT DO UPDATE}, que bloquea la fila hasta que
 * termina la transaccion de la venta: dos confirmaciones simultaneas no reciben el mismo folio, y si la venta se
 * deshace el folio tambien, asi que no queda un hueco (VT-06).
 */
public final class RepositorioVentasJdbc implements RepositorioVentas {

    private static final String COLUMNAS = "id, negocio_id, serie, folio, estado, turno_id, cliente_id, cupon_id, subtotal, "
            + "descuento, total, ocurrido_en, registrada_por, anulada_en, anulada_por, motivo_anulacion, version";

    private final JdbcClient jdbc;
    private final EjecutorEnNegocio negocio;

    /**
     * Arma el repositorio.
     *
     * @param jdbc    cliente de la base
     * @param negocio ejecutor que fija el negocio en cada transaccion
     */
    public RepositorioVentasJdbc(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.negocio = Objects.requireNonNull(negocio, "negocio");
    }

    @Override
    public void guardar(Venta venta) {
        negocio.en(venta.negocioId(), () -> {
            jdbc.sql("INSERT INTO ventas.venta (" + COLUMNAS + ") VALUES (:id, :negocio, :serie, :folio, :estado, :turno, "
                            + ":cliente, :cupon, :subtotal, :descuento, :total, :ocurrido, :registradaPor, :anuladaEn, "
                            + ":anuladaPor, :motivo, :version)")
                    .param("id", venta.id()).param("negocio", venta.negocioId()).param("serie", venta.serie())
                    .param("folio", venta.folio()).param("estado", venta.estado().name())
                    .param("turno", venta.turnoId()).param("cliente", venta.clienteId(), Types.OTHER)
                    .param("cupon", venta.cuponId(), Types.OTHER).param("subtotal", venta.subtotal())
                    .param("descuento", venta.descuento()).param("total", venta.total())
                    .param("ocurrido", momento(venta.ocurridoEn())).param("registradaPor", venta.registradaPor())
                    .param("anuladaEn", momento(venta.anuladaEn()), Types.TIMESTAMP_WITH_TIMEZONE)
                    .param("anuladaPor", venta.anuladaPor(), Types.VARCHAR)
                    .param("motivo", venta.motivoAnulacion(), Types.VARCHAR).param("version", venta.version())
                    .update();
            for (var linea : venta.lineas()) {
                jdbc.sql("INSERT INTO ventas.linea_venta (venta_id, negocio_id, linea, producto_id, descripcion, cantidad, "
                                + "unidad, precio_unitario, importe, serie) VALUES (:venta, :negocio, :linea, :producto, "
                                + ":descripcion, :cantidad, :unidad, :precio, :importe, :serie)")
                        .param("venta", venta.id()).param("negocio", venta.negocioId()).param("linea", linea.linea())
                        .param("producto", linea.productoId()).param("descripcion", linea.descripcion())
                        .param("cantidad", linea.cantidad()).param("unidad", linea.unidad())
                        .param("precio", linea.precioUnitario()).param("importe", linea.importe())
                        .param("serie", linea.serie(), Types.VARCHAR).update();
            }
            var orden = 1;
            for (var pago : venta.pagos()) {
                jdbc.sql("INSERT INTO ventas.pago_venta (venta_id, negocio_id, orden, metodo, monto, recibido, cambio, "
                                + "referencia) VALUES (:venta, :negocio, :orden, :metodo, :monto, :recibido, :cambio, "
                                + ":referencia)")
                        .param("venta", venta.id()).param("negocio", venta.negocioId()).param("orden", orden++)
                        .param("metodo", pago.metodo().name()).param("monto", pago.monto())
                        .param("recibido", pago.recibido(), Types.NUMERIC).param("cambio", pago.cambio(), Types.NUMERIC)
                        .param("referencia", pago.referencia(), Types.VARCHAR).update();
            }
            return null;
        });
    }

    @Override
    public Optional<Venta> porId(UUID negocioId, UUID ventaId) {
        return negocio.en(negocioId, () -> {
            var filas = jdbc.sql("SELECT " + COLUMNAS + " FROM ventas.venta WHERE negocio_id = :negocio AND id = :id")
                    .param("negocio", negocioId).param("id", ventaId).query(RepositorioVentasJdbc::leerCabecera).list();
            return completar(negocioId, filas).stream().findFirst();
        });
    }

    @Override
    public List<Venta> porTurno(UUID negocioId, UUID turnoId) {
        return negocio.en(negocioId, () -> completar(negocioId, jdbc.sql("SELECT " + COLUMNAS
                        + " FROM ventas.venta WHERE negocio_id = :negocio AND turno_id = :turno ORDER BY folio, serie")
                .param("negocio", negocioId).param("turno", turnoId).query(RepositorioVentasJdbc::leerCabecera).list()));
    }

    @Override
    public Pagina<Venta> buscar(UUID negocioId, FiltroVentas filtro, int primeros, String despues) {
        return negocio.en(negocioId, () -> Cursores.pagina((limite, desplazamiento) -> completar(negocioId,
                jdbc.sql("SELECT " + COLUMNAS + " FROM ventas.venta WHERE negocio_id = :negocio"
                                + " AND (CAST(:turno AS uuid) IS NULL OR turno_id = :turno)"
                                + " AND (CAST(:estado AS text) IS NULL OR estado = :estado)"
                                + " AND (CAST(:desde AS timestamptz) IS NULL OR ocurrido_en >= :desde)"
                                + " AND (CAST(:hasta AS timestamptz) IS NULL OR ocurrido_en <= :hasta)"
                                + " AND (CAST(:serie AS text) IS NULL OR serie = :serie)"
                                + " AND (CAST(:folio AS bigint) IS NULL OR folio = :folio)"
                                + " ORDER BY ocurrido_en DESC, folio DESC, serie DESC, id DESC LIMIT :limite OFFSET :desplazamiento")
                        .param("negocio", negocioId).param("turno", filtro.turnoId(), Types.OTHER)
                        .param("estado", filtro.estado() == null ? null : filtro.estado().name(), Types.VARCHAR)
                        .param("desde", momento(filtro.desde()), Types.TIMESTAMP_WITH_TIMEZONE)
                        .param("hasta", momento(filtro.hasta()), Types.TIMESTAMP_WITH_TIMEZONE)
                        .param("serie", filtro.serie(), Types.VARCHAR).param("folio", filtro.folio(), Types.BIGINT)
                        .param("limite", limite).param("desplazamiento", desplazamiento)
                        .query(RepositorioVentasJdbc::leerCabecera).list()), primeros, despues));
    }

    @Override
    public long siguienteFolio(UUID negocioId, String serie) {
        return negocio.en(negocioId, () -> jdbc.sql("INSERT INTO ventas.serie_folio (negocio_id, serie, ultimo) "
                        + "VALUES (:negocio, :serie, 1) ON CONFLICT (negocio_id, serie) "
                        + "DO UPDATE SET ultimo = ventas.serie_folio.ultimo + 1 RETURNING ultimo")
                .param("negocio", negocioId).param("serie", serie).query(Long.class).single());
    }

    @Override
    public boolean actualizar(Venta venta, int versionEsperada) {
        return negocio.en(venta.negocioId(), () -> jdbc.sql("UPDATE ventas.venta SET estado = :estado, "
                        + "anulada_en = :anuladaEn, anulada_por = :anuladaPor, motivo_anulacion = :motivo, version = :version "
                        + "WHERE negocio_id = :negocio AND id = :id AND version = :esperada")
                .param("estado", venta.estado().name()).param("anuladaEn", momento(venta.anuladaEn()), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("anuladaPor", venta.anuladaPor(), Types.VARCHAR).param("motivo", venta.motivoAnulacion(), Types.VARCHAR)
                .param("version", venta.version()).param("negocio", venta.negocioId()).param("id", venta.id())
                .param("esperada", versionEsperada).update() == 1);
    }

    /** Columnas de {@code ventas.venta}: la venta sin sus lineas ni pagos, que {@link #completar} agrega. */
    private record Cabecera(UUID id, UUID negocioId, String serie, long folio, EstadoVenta estado, UUID turnoId,
                            UUID clienteId, UUID cuponId, java.math.BigDecimal subtotal, java.math.BigDecimal descuento,
                            java.math.BigDecimal total, java.time.Instant ocurridoEn, String registradaPor,
                            java.time.Instant anuladaEn, String anuladaPor, String motivoAnulacion, int version) {

        Venta conDetalle(List<LineaVenta> lineas, List<PagoVenta> pagos) {
            return new Venta(id, negocioId, serie, folio, estado, turnoId, clienteId, cuponId, subtotal, descuento,
                    total, lineas, pagos, ocurridoEn, registradaPor, anuladaEn, anuladaPor, motivoAnulacion, version);
        }
    }

    private static Cabecera leerCabecera(ResultSet fila, int numero) throws SQLException {
        return new Cabecera(fila.getObject("id", UUID.class), fila.getObject("negocio_id", UUID.class),
                fila.getString("serie"), fila.getLong("folio"), EstadoVenta.valueOf(fila.getString("estado")),
                fila.getObject("turno_id", UUID.class), fila.getObject("cliente_id", UUID.class),
                fila.getObject("cupon_id", UUID.class), fila.getBigDecimal("subtotal"), fila.getBigDecimal("descuento"),
                fila.getBigDecimal("total"), instante(fila, "ocurrido_en"), fila.getString("registrada_por"),
                instante(fila, "anulada_en"), fila.getString("anulada_por"), fila.getString("motivo_anulacion"),
                fila.getInt("version"));
    }

    /** Agrega lineas y pagos a las cabeceras con dos consultas, sin importar cuantas ventas sean. */
    private List<Venta> completar(UUID negocioId, List<Cabecera> cabeceras) {
        if (cabeceras.isEmpty()) {
            return List.of();
        }
        var ids = cabeceras.stream().map(Cabecera::id).toList();
        Map<UUID, List<LineaVenta>> lineas = new HashMap<>();
        jdbc.sql("SELECT venta_id, linea, producto_id, descripcion, cantidad, unidad, precio_unitario, importe, serie "
                        + "FROM ventas.linea_venta WHERE negocio_id = :negocio AND venta_id IN (:ids) ORDER BY venta_id, linea")
                .param("negocio", negocioId).param("ids", ids).query(fila -> {
                    lineas.computeIfAbsent(fila.getObject("venta_id", UUID.class), k -> new ArrayList<>())
                            .add(new LineaVenta(fila.getInt("linea"), fila.getObject("producto_id", UUID.class),
                                    fila.getString("descripcion"), fila.getBigDecimal("cantidad"), fila.getString("unidad"),
                                    fila.getBigDecimal("precio_unitario"), fila.getBigDecimal("importe"),
                                    fila.getString("serie")));
                });
        Map<UUID, List<PagoVenta>> pagos = new HashMap<>();
        jdbc.sql("SELECT venta_id, metodo, monto, recibido, cambio, referencia FROM ventas.pago_venta "
                        + "WHERE negocio_id = :negocio AND venta_id IN (:ids) ORDER BY venta_id, orden")
                .param("negocio", negocioId).param("ids", ids).query(fila -> {
                    pagos.computeIfAbsent(fila.getObject("venta_id", UUID.class), k -> new ArrayList<>())
                            .add(new PagoVenta(MetodoPago.valueOf(fila.getString("metodo")), fila.getBigDecimal("monto"),
                                    fila.getBigDecimal("recibido"), fila.getBigDecimal("cambio"),
                                    fila.getString("referencia")));
                });
        return cabeceras.stream().map(c -> c.conDetalle(lineas.get(c.id()), pagos.get(c.id()))).toList();
    }
}
