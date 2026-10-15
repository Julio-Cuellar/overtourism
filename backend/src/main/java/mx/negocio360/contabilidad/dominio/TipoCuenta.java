package mx.negocio360.contabilidad.dominio;

/** Tipo de cuenta del catalogo. Una cuenta hija tiene el mismo tipo que su padre (C-02). */
public enum TipoCuenta {
    /** Bienes y derechos: caja, bancos, cuentas por cobrar. */
    ACTIVO,
    /** Obligaciones: impuestos por pagar, propinas por entregar. */
    PASIVO,
    /** Patrimonio del negocio. */
    CAPITAL,
    /** Ventas y otros ingresos, incluidas sus cuentas de contra (descuentos). */
    INGRESO,
    /** Costo de lo vendido (fuera del MVP, K-4). */
    COSTO,
    /** Gastos de operacion, como los faltantes de caja. */
    GASTO
}
