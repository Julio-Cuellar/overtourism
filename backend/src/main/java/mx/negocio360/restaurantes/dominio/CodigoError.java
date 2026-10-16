package mx.negocio360.restaurantes.dominio;

/**
 * Codigos de error de Restaurantes (seccion 7 del diseno). Cada uno lleva su texto en español de Mexico: la capa que
 * responde a la persona no improvisa mensajes. Ninguno repite lo que escribio la persona.
 */
public enum CodigoError {
    NOMBRE_INVALIDO("El nombre debe tener entre 2 y 80 caracteres."),
    NOMBRE_DUPLICADO("Ya hay un platillo activo con ese nombre."),
    DESCRIPCION_INVALIDA("La descripción puede tener hasta 300 caracteres."),
    PRECIO_INVALIDO("El precio debe ser mayor que 0 y de hasta 99,999.99."),
    TEMPORADA_INVALIDA("La temporada debe darse como mes y día, por ejemplo 08-01."),
    ETIQUETA_INVALIDA("Esa etiqueta no existe."),
    LIMITE_EXCEDIDO("Se alcanzó el límite permitido."),
    CATEGORIA_NO_ENCONTRADA("No se encontró la categoría."),
    PLATILLO_NO_ENCONTRADO("No se encontró el platillo."),
    RECETA_NO_ENCONTRADA("Ese platillo todavía no tiene receta."),
    RECETA_REQUERIDA("Un platillo en modo receta necesita una receta activa."),
    MODO_INVENTARIO_CONFLICTO("Un platillo no puede descontar por receta y por vínculo directo a la vez."),
    PLATILLO_AGOTADO("El platillo está agotado."),
    FUERA_DE_TEMPORADA("El platillo está fuera de temporada."),
    PLATILLO_NO_ORDENABLE("El platillo ya no se vende."),
    PLATILLO_EN_CUENTA_ABIERTA("El platillo está en una cuenta abierta; no se puede dar de baja todavía."),
    RECETA_INVALIDA("La receta no es válida."),
    INSUMO_REPETIDO("Un artículo no puede repetirse en la misma receta."),
    UNIDAD_NO_CONVERTIBLE("La unidad no se puede convertir a la del artículo."),
    SERIE_EN_RECETA("Un artículo con números de serie no puede ser insumo de una receta."),
    ARTICULO_NO_ENCONTRADO("No se encontró el artículo en el inventario."),
    ARTICULO_INACTIVO("El artículo ya no está activo en el inventario."),
    INVENTARIO_NO_DISPONIBLE("El inventario no responde. Inténtalo de nuevo en un momento."),
    CONCURRENCIA("Alguien más cambió esto. Vuelve a cargarlo e inténtalo de nuevo."),
    COMANDO_CAMBIO("Algo cambió desde que lo revisaste. Vuelve a prepararlo."),
    TOKEN_INVALIDO("La confirmación no es válida."),
    TOKEN_EXPIRADO("La confirmación venció. Vuelve a prepararlo."),
    TOKEN_USADO("Esa confirmación ya se usó."),
    CONSULTA_INVALIDA("La consulta no es válida.");

    private final String mensaje;

    CodigoError(String mensaje) {
        this.mensaje = mensaje;
    }

    /** Texto fijo para la persona. */
    public String mensaje() {
        return mensaje;
    }
}
