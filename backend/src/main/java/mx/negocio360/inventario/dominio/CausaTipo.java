package mx.negocio360.inventario.dominio;

import org.springframework.modulith.NamedInterface;

/**
 * Por que ocurrio un movimiento (columna {@code movimiento.causa_tipo}). El nucleo usa COMPRA, CONTEO, MERMA y MANUAL;
 * VENTA y REVERSION llegan con el consumo por venta (K-01 a K-04).
 */
@NamedInterface("api")
public enum CausaTipo {
    VENTA,
    COMPRA,
    CONTEO,
    MERMA,
    MANUAL,
    REVERSION
}
