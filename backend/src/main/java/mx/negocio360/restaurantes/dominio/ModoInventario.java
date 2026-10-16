package mx.negocio360.restaurantes.dominio;

/** Como descuenta inventario un platillo (K-05): excluyentes. Mientras no exista Catalogo, lo lleva Restaurantes (D-163). */
public enum ModoInventario {
    /** No descuenta nada. */
    NINGUNO,
    /** Descuenta los insumos de su receta, la version vigente al momento de la venta (CM-01). */
    RECETA,
    /** Es un articulo de Inventario que se vende tal cual (una bebida embotellada); lo descuenta Inventario. */
    VINCULO_DIRECTO
}
