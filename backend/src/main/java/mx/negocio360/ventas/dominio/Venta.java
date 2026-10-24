package mx.negocio360.ventas.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raiz de Ventas: una venta cobrada. Nace {@link EstadoVenta#REGISTRADA} al confirmar y solo cambia para
 * anularse (VT-11); cualquier otra correccion es anular y volver a vender.
 *
 * <p>Montos en escala 2 con HALF_UP (D-20). {@code total = subtotal − descuento} y es neto de cupon (VT-08, D-65).
 *
 * @param id              UUIDv7
 * @param negocioId       negocio dueño (seguridad por fila)
 * @param serie           serie del folio
 * @param folio           consecutivo por negocio y serie, sin huecos (VT-06)
 * @param estado          REGISTRADA o ANULADA
 * @param turnoId         referencia opaca al turno de Caja (VT-01)
 * @param clienteId       referencia opaca a Clientes, o nula (VT-13)
 * @param cuponId         referencia opaca al cupon de Pasaporte, o nula (VT-07)
 * @param subtotal        suma de importes
 * @param descuento       descuento del cupon, 0.00 si no hay
 * @param total           lo que se cobro
 * @param lineas          renglones, al menos uno
 * @param pagos           formas de pago, al menos una
 * @param ocurridoEn      cuando se registro, en UTC
 * @param registradaPor   actor que cobro
 * @param anuladaEn       cuando se anulo, o nulo
 * @param anuladaPor      actor que anulo, o nulo
 * @param motivoAnulacion motivo, o nulo
 * @param version         bloqueo optimista, desde 1
 * @param referencia      texto opaco de quien cobra para reconocer la venta (por ejemplo {@code restaurantes:cuenta:<id>}), o
 *                        nulo; de 1 a 80 caracteres de {@code [A-Za-z0-9._:-]}
 */
public record Venta(UUID id, UUID negocioId, String serie, long folio, EstadoVenta estado, UUID turnoId,
                    UUID clienteId, UUID cuponId, BigDecimal subtotal, BigDecimal descuento, BigDecimal total,
                    List<LineaVenta> lineas, List<PagoVenta> pagos, Instant ocurridoEn, String registradaPor,
                    Instant anuladaEn, String anuladaPor, String motivoAnulacion, int version, String referencia) {

    /** Una venta sin referencia: lo que existia antes de R-V1. */
    public Venta(UUID id, UUID negocioId, String serie, long folio, EstadoVenta estado, UUID turnoId, UUID clienteId,
                 UUID cuponId, BigDecimal subtotal, BigDecimal descuento, BigDecimal total, List<LineaVenta> lineas,
                 List<PagoVenta> pagos, Instant ocurridoEn, String registradaPor, Instant anuladaEn, String anuladaPor,
                 String motivoAnulacion, int version) {
        this(id, negocioId, serie, folio, estado, turnoId, clienteId, cuponId, subtotal, descuento, total, lineas, pagos,
                ocurridoEn, registradaPor, anuladaEn, anuladaPor, motivoAnulacion, version, null);
    }

    /** Exige una venta completa y que {@code total = subtotal − descuento} (VT-08); copia lineas y pagos. */
    public Venta {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(serie, "serie");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(turnoId, "turnoId");
        Objects.requireNonNull(subtotal, "subtotal");
        Objects.requireNonNull(descuento, "descuento");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(registradaPor, "registradaPor");
        lineas = List.copyOf(lineas);
        pagos = List.copyOf(pagos);
        if (folio < 1 || version < 1 || lineas.isEmpty() || pagos.isEmpty()) {
            throw new IllegalArgumentException("Venta incompleta: folio, version, lineas y pagos son obligatorios");
        }
        if (total.compareTo(subtotal.subtract(descuento)) != 0) {
            throw new IllegalArgumentException("El total debe ser el subtotal menos el descuento (VT-08)");
        }
    }

    /**
     * Copia anulada de esta venta, con la version siguiente (VT-09, VT-11).
     *
     * @param cuando cuando se anula
     * @param actor  quien anula
     * @param motivo por que
     * @return la venta ANULADA; lineas, pagos y montos no cambian
     * @throws VentasException con {@code VENTA_YA_ANULADA} si ya estaba anulada
     */
    public Venta anulada(Instant cuando, String actor, String motivo) {
        if (estado == EstadoVenta.ANULADA) {
            throw new VentasException(CodigoError.VENTA_YA_ANULADA, id);
        }
        return new Venta(id, negocioId, serie, folio, EstadoVenta.ANULADA, turnoId, clienteId, cuponId, subtotal,
                descuento, total, lineas, pagos, ocurridoEn, registradaPor, cuando, actor, motivo, version + 1, referencia);
    }

    /** Sin cliente, actores ni motivo: son datos personales o texto libre y no deben llegar a los registros. */
    @Override
    public String toString() {
        return "Venta[id=" + id + ", negocioId=" + negocioId + ", folio=" + serie + "-" + folio + ", estado=" + estado
                + ", turnoId=" + turnoId + ", total=" + total + ", version=" + version + "]";
    }
}
