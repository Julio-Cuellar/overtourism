package mx.negocio360.compartido.catalogo;

/**
 * Como descuenta inventario un producto al venderse (K-05, R-V2). Es de la plataforma y no de un vertical para que
 * Inventario y Ventas lo lean sin conocer a ninguno (R4): cada {@link FuenteDeProductos} declara el de sus productos.
 * Son excluyentes: un producto no descuenta por receta y por vinculo a la vez.
 */
public enum ModoInventario {
    /** No descuenta nada: es un servicio o un producto sin control de existencias. */
    NINGUNO,
    /** Descuenta los insumos de su receta (Restaurantes). */
    RECETA,
    /** Descuenta directamente del articulo que tiene vinculado (Miscelaneos). */
    VINCULO_DIRECTO
}
