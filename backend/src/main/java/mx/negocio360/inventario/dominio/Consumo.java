package mx.negocio360.inventario.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Consumo de insumos por una causa externa, normalmente una venta (tabla {@code consumo}, K-01 a K-04). La llave es
 * (negocio, causa, consumidor): es lo que vuelve idempotentes a {@code registrarConsumo} y {@code revertirConsumo}. Es
 * inmutable; cada cambio de estado produce otra instancia.
 *
 * @param consumidor       quien consume: {@code vinculo} o {@code restaurantes}
 * @param estado           ver {@link Estado}
 * @param lineas           lo descontado, con el costo original de cada linea; vacio si solo hay marca (K-04)
 * @param costoTotal       suma del costo de las lineas, positiva
 * @param movimientosReversa movimientos que repusieron el consumo; vacio mientras no se revierta
 * @param registradoEn     cuando se guardo la fila
 */
public record Consumo(UUID negocioId, UUID causaId, String consumidor, Estado estado, List<LineaConsumida> lineas,
                      BigDecimal costoTotal, List<UUID> movimientosReversa, Instant registradoEn) {

    /** En que va el consumo. */
    public enum Estado {
        /** Se descontaron las lineas. */
        REGISTRADO,
        /** Se descontaron y luego se repusieron al costo original (K-03). */
        REVERTIDO,
        /** Llego la reversa antes que el consumo: solo hay marca y el consumo se omitira (K-04). */
        ANULADO_PREVIO
    }

    /**
     * Una linea descontada.
     *
     * @param movimientoId  movimiento de salida que la registro
     * @param cantidadBase  cantidad descontada, positiva, en unidad base
     * @param costoUnitario costo por unidad base aplicado; con el se repone al revertir
     */
    public record LineaConsumida(UUID articuloId, UUID movimientoId, BigDecimal cantidadBase,
                                 BigDecimal costoUnitario) {

        public LineaConsumida {
            Objects.requireNonNull(articuloId, "articuloId");
            Objects.requireNonNull(movimientoId, "movimientoId");
            Objects.requireNonNull(cantidadBase, "cantidadBase");
            Objects.requireNonNull(costoUnitario, "costoUnitario");
        }
    }

    public Consumo {
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(causaId, "causaId");
        Objects.requireNonNull(consumidor, "consumidor");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(costoTotal, "costoTotal");
        Objects.requireNonNull(registradoEn, "registradoEn");
        lineas = List.copyOf(Objects.requireNonNull(lineas, "lineas"));
        movimientosReversa = List.copyOf(Objects.requireNonNull(movimientosReversa, "movimientosReversa"));
    }

    /** Consumo recien descontado. */
    public static Consumo registrado(UUID negocioId, UUID causaId, String consumidor, List<LineaConsumida> lineas,
                                     BigDecimal costoTotal, Instant ahora) {
        return new Consumo(negocioId, causaId, consumidor, Estado.REGISTRADO, lineas, costoTotal, List.of(), ahora);
    }

    /** Solo la marca de que la anulacion llego primero (K-04). */
    public static Consumo marcaDeAnulacion(UUID negocioId, UUID causaId, String consumidor, Instant ahora) {
        return new Consumo(negocioId, causaId, consumidor, Estado.ANULADO_PREVIO, List.of(), BigDecimal.ZERO, List.of(),
                ahora);
    }

    /** El mismo consumo, ya repuesto por los movimientos indicados (K-03). */
    public Consumo revertido(List<UUID> movimientosDeReversa) {
        return new Consumo(negocioId, causaId, consumidor, Estado.REVERTIDO, lineas, costoTotal, movimientosDeReversa,
                registradoEn);
    }
}
