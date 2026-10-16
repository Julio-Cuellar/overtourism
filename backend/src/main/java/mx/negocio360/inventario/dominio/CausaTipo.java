package mx.negocio360.inventario.dominio;

/**
 * Por que ocurrio un movimiento (columna {@code movimiento.causa_tipo}). El nucleo usa COMPRA, CONTEO, MERMA y MANUAL;
 * VENTA y REVERSION llegan con el consumo por venta (K-01 a K-04).
 */
public enum CausaTipo {
    VENTA,
    COMPRA,
    CONTEO,
    MERMA,
    MANUAL,
    REVERSION
}
