package mx.negocio360.caja.infraestructura;

import static mx.negocio360.compartido.transaccion.MomentosJdbc.instante;
import static mx.negocio360.compartido.transaccion.MomentosJdbc.momento;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import mx.negocio360.caja.FiltroCortes;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.EstadoVentaAplicada;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.TipoMovimiento;
import mx.negocio360.caja.dominio.VentaAplicada;
import mx.negocio360.compartido.paginacion.Cursores;
import mx.negocio360.compartido.paginacion.Pagina;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Tablas {@code caja.movimiento_caja}, {@code venta_aplicada}, {@code evento_procesado} y {@code corte} en PostgreSQL.
 * Movimientos y cortes son de solo insercion (CJ-09): la base lo impone. El orden de los movimientos es el de registro
 * ({@code secuencia}). Los montos de un corte por metodo se guardan como cadenas decimales en {@code jsonb}, para no
 * perder la escala.
 *
 * <p>{@link #registrarEvento} no lleva negocio (el candado es del evento) y corre en la transaccion en curso: si el
 * consumidor falla, el candado se deshace con lo demas y el evento se reintenta (R3).
 */
public final class LibroDeCajaJdbc implements LibroDeCaja {

    private static final String COLUMNAS_MOVIMIENTO = "id, negocio_id, turno_id, tipo, metodo, monto, venta_id, evento_id, "
            + "nota, registrado_en, registrado_por";
    private static final String COLUMNAS_CORTE = "id, negocio_id, turno_id, caja_id, esperado_por_metodo::text AS esperado, "
            + "contado_por_metodo::text AS contado, diferencia_efectivo, autorizado_por, abierto_en, confirmado_en";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MONTOS = new TypeReference<>() {
    };

    private final JdbcClient jdbc;
    private final EjecutorEnNegocio negocio;

    /**
     * Arma el libro.
     *
     * @param jdbc    cliente de la base
     * @param negocio ejecutor que fija el negocio en cada transaccion
     */
    public LibroDeCajaJdbc(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.negocio = Objects.requireNonNull(negocio, "negocio");
    }

    @Override
    public void agregar(MovimientoCaja m) {
        negocio.en(m.negocioId(), () -> jdbc.sql("INSERT INTO caja.movimiento_caja (" + COLUMNAS_MOVIMIENTO + ") VALUES ("
                        + ":id, :negocio, :turno, :tipo, :metodo, :monto, :venta, :evento, :nota, :registradoEn, :registradoPor)")
                .param("id", m.id()).param("negocio", m.negocioId()).param("turno", m.turnoId())
                .param("tipo", m.tipo().name()).param("metodo", m.metodo().name()).param("monto", m.monto())
                .param("venta", m.ventaId(), Types.OTHER).param("evento", m.eventoId(), Types.OTHER)
                .param("nota", m.nota(), Types.VARCHAR).param("registradoEn", momento(m.registradoEn()))
                .param("registradoPor", m.registradoPor(), Types.VARCHAR).update());
    }

    @Override
    public List<MovimientoCaja> movimientos(UUID negocioId, UUID turnoId) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT " + COLUMNAS_MOVIMIENTO + " FROM caja.movimiento_caja "
                        + "WHERE negocio_id = :negocio AND turno_id = :turno ORDER BY secuencia")
                .param("negocio", negocioId).param("turno", turnoId).query(LibroDeCajaJdbc::leerMovimiento).list());
    }

    @Override
    public Pagina<MovimientoCaja> movimientos(UUID negocioId, UUID turnoId, int primeros, String despues) {
        return negocio.en(negocioId, () -> Cursores.pagina((limite, desplazamiento) -> jdbc.sql("SELECT "
                        + COLUMNAS_MOVIMIENTO + " FROM caja.movimiento_caja WHERE negocio_id = :negocio AND turno_id = :turno "
                        + "ORDER BY secuencia LIMIT :limite OFFSET :desplazamiento")
                .param("negocio", negocioId).param("turno", turnoId).param("limite", limite)
                .param("desplazamiento", desplazamiento).query(LibroDeCajaJdbc::leerMovimiento).list(), primeros, despues));
    }

    /**
     * Lo que Caja hizo con una venta. Antes de leer toma un candado de la transaccion sobre esa venta
     * ({@code pg_advisory_xact_lock}), que se suelta al confirmar o deshacer: el evento de la venta y el de su anulacion
     * pueden llegar a la vez, en hilos distintos, y sin el candado los dos verian la venta como desconocida y la dejarian
     * aplicada aunque se anulo. La fila no existe todavia la primera vez, asi que un {@code FOR UPDATE} no bastaria.
     */
    @Override
    public Optional<VentaAplicada> ventaAplicada(UUID negocioId, UUID ventaId) {
        return negocio.en(negocioId, () -> {
            jdbc.sql("SELECT count(*) FROM (SELECT pg_advisory_xact_lock(hashtextextended(:clave, 0))) candado")
                    .param("clave", "caja.venta_aplicada:" + negocioId + "/" + ventaId).query(Long.class).single();
            return leerVentaAplicada(negocioId, ventaId);
        });
    }

    private Optional<VentaAplicada> leerVentaAplicada(UUID negocioId, UUID ventaId) {
        return jdbc.sql("SELECT negocio_id, venta_id, turno_id, estado FROM caja.venta_aplicada "
                        + "WHERE negocio_id = :negocio AND venta_id = :venta")
                .param("negocio", negocioId).param("venta", ventaId).query((fila, n) -> new VentaAplicada(
                        fila.getObject("negocio_id", UUID.class), fila.getObject("venta_id", UUID.class),
                        fila.getObject("turno_id", UUID.class), EstadoVentaAplicada.valueOf(fila.getString("estado"))))
                .optional();
    }

    @Override
    public void guardarVentaAplicada(VentaAplicada venta) {
        negocio.en(venta.negocioId(), () -> jdbc.sql("INSERT INTO caja.venta_aplicada (negocio_id, venta_id, turno_id, estado) "
                        + "VALUES (:negocio, :venta, :turno, :estado) ON CONFLICT (negocio_id, venta_id) "
                        + "DO UPDATE SET turno_id = EXCLUDED.turno_id, estado = EXCLUDED.estado")
                .param("negocio", venta.negocioId()).param("venta", venta.ventaId())
                .param("turno", venta.turnoId(), Types.OTHER).param("estado", venta.estado().name()).update());
    }

    @Override
    public long contarVentas(UUID negocioId, UUID turnoId, EstadoVentaAplicada estado) {
        return negocio.en(negocioId, () -> jdbc.sql("SELECT count(*) FROM caja.venta_aplicada "
                        + "WHERE negocio_id = :negocio AND turno_id = :turno AND estado = :estado")
                .param("negocio", negocioId).param("turno", turnoId).param("estado", estado.name())
                .query(Long.class).single());
    }

    @Override
    public boolean registrarEvento(String consumidor, UUID eventoId) {
        return jdbc.sql("INSERT INTO caja.evento_procesado (consumidor, evento_id) VALUES (:consumidor, :evento) "
                        + "ON CONFLICT DO NOTHING")
                .param("consumidor", consumidor).param("evento", eventoId).update() == 1;
    }

    @Override
    public void guardarCorte(Corte c) {
        negocio.en(c.negocioId(), () -> jdbc.sql("INSERT INTO caja.corte (id, negocio_id, turno_id, caja_id, "
                        + "esperado_por_metodo, contado_por_metodo, diferencia_efectivo, autorizado_por, abierto_en, "
                        + "confirmado_en) VALUES (:id, :negocio, :turno, :caja, CAST(:esperado AS jsonb), "
                        + "CAST(:contado AS jsonb), :diferencia, :autorizado, :abierto, :confirmado)")
                .param("id", c.id()).param("negocio", c.negocioId()).param("turno", c.turnoId())
                .param("caja", c.cajaId(), Types.OTHER).param("esperado", aJson(c.esperadoPorMetodo()))
                .param("contado", aJson(c.contadoPorMetodo())).param("diferencia", c.diferenciaEfectivo())
                .param("autorizado", c.autorizadoPor(), Types.VARCHAR).param("abierto", momento(c.abiertoEn()))
                .param("confirmado", momento(c.confirmadoEn())).update());
    }

    @Override
    public Pagina<Corte> cortes(UUID negocioId, FiltroCortes filtro, int primeros, String despues) {
        return negocio.en(negocioId, () -> Cursores.pagina((limite, desplazamiento) -> jdbc.sql("SELECT " + COLUMNAS_CORTE
                        + " FROM caja.corte WHERE negocio_id = :negocio"
                        + " AND (CAST(:caja AS uuid) IS NULL OR caja_id = :caja)"
                        + " AND (CAST(:desde AS timestamptz) IS NULL OR confirmado_en >= :desde)"
                        + " AND (CAST(:hasta AS timestamptz) IS NULL OR confirmado_en <= :hasta)"
                        + " ORDER BY confirmado_en DESC, id DESC LIMIT :limite OFFSET :desplazamiento")
                .param("negocio", negocioId).param("caja", filtro.cajaId(), Types.OTHER)
                .param("desde", momento(filtro.desde()), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("hasta", momento(filtro.hasta()), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("limite", limite).param("desplazamiento", desplazamiento)
                .query(LibroDeCajaJdbc::leerCorte).list(), primeros, despues));
    }

    private static MovimientoCaja leerMovimiento(ResultSet fila, int numero) throws SQLException {
        return new MovimientoCaja(fila.getObject("id", UUID.class), fila.getObject("negocio_id", UUID.class),
                fila.getObject("turno_id", UUID.class), TipoMovimiento.valueOf(fila.getString("tipo")),
                MetodoPago.valueOf(fila.getString("metodo")), fila.getBigDecimal("monto"),
                fila.getObject("venta_id", UUID.class), fila.getObject("evento_id", UUID.class), fila.getString("nota"),
                instante(fila, "registrado_en"), fila.getString("registrado_por"));
    }

    private static Corte leerCorte(ResultSet fila, int numero) throws SQLException {
        return new Corte(fila.getObject("id", UUID.class), fila.getObject("negocio_id", UUID.class),
                fila.getObject("turno_id", UUID.class), fila.getObject("caja_id", UUID.class),
                deJson(fila.getString("esperado")), deJson(fila.getString("contado")),
                fila.getBigDecimal("diferencia_efectivo"), fila.getString("autorizado_por"),
                instante(fila, "abierto_en"), instante(fila, "confirmado_en"));
    }

    private static String aJson(Map<MetodoPago, BigDecimal> montos) {
        var texto = new TreeMap<String, String>();
        montos.forEach((metodo, monto) -> texto.put(metodo.name(), monto.toPlainString()));
        try {
            return JSON.writeValueAsString(texto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron serializar los montos del corte", e);
        }
    }

    private static Map<MetodoPago, BigDecimal> deJson(String json) {
        try {
            var montos = new EnumMap<MetodoPago, BigDecimal>(MetodoPago.class);
            JSON.readValue(json, MONTOS).forEach((metodo, monto) -> montos.put(MetodoPago.valueOf(metodo), new BigDecimal(monto)));
            return montos;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Los montos del corte guardados no se pueden leer", e);
        }
    }
}
