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
    ZONA_NO_ENCONTRADA("No se encontró la zona."),
    ZONA_CON_MESAS("La zona todavía tiene mesas; da de baja sus mesas primero."),
    MESA_NO_ENCONTRADA("No se encontró la mesa."),
    MESA_DUPLICADA("Ya hay una mesa con ese nombre."),
    MESA_OCUPADA("La mesa tiene una cuenta sin pagar."),
    CAPACIDAD_INVALIDA("La capacidad debe ser de 1 a 20 personas."),
    SERVICIO_CERRADO("El servicio está cerrado."),
    MOTIVO_REQUERIDO("Falta el motivo."),
    MOTIVO_INVALIDO("El motivo no es válido."),
    MESA_BLOQUEADA("La mesa está bloqueada."),
    MESA_REQUERIDA("Elige la mesa de la cuenta."),
    MESA_NO_PERMITIDA("El restaurante atiende solo en mostrador; la cuenta no lleva mesa."),
    COMENSALES_INVALIDOS("Los comensales deben ser de 1 a 50."),
    CANTIDAD_INVALIDA("La cantidad debe ser un entero de 1 a 99."),
    NOTA_INVALIDA("La nota puede tener hasta 80 caracteres."),
    CUENTA_NO_ENCONTRADA("No se encontró la cuenta."),
    CUENTA_NO_ABIERTA("La cuenta ya no está abierta."),
    CUENTA_NO_SOLICITADA("La cuenta todavía no se ha pedido."),
    CUENTA_VACIA("La cuenta no tiene líneas."),
    CUENTA_CON_LINEAS_PREPARADAS("La cuenta ya tiene platillos preparados; pide a un gerente que la cancele."),
    LINEA_NO_ENCONTRADA("No se encontró la línea."),
    LINEA_PREPARADA("La línea ya está lista; pide a un gerente que la anule."),
    SIN_LINEAS_PENDIENTES("No hay líneas por enviar a cocina."),
    LINEAS_SIN_ENVIAR("Hay líneas sin enviar a cocina."),
    COMANDA_NO_ENCONTRADA("No se encontró la comanda."),
    TRANSICION_INVALIDA("La comanda no puede pasar a ese estado."),
    CONCURRENCIA("Alguien más cambió esto. Vuelve a cargarlo e inténtalo de nuevo."),
    COMANDO_CAMBIO("Algo cambió desde que lo revisaste. Vuelve a prepararlo."),
    TOKEN_INVALIDO("La confirmación no es válida."),
    TOKEN_EXPIRADO("La confirmación venció. Vuelve a prepararlo."),
    TOKEN_USADO("Esa confirmación ya se usó."),
    QR_INVALIDO("El código QR no es válido."),
    QR_PAUSADO("Esta mesa no recibe pedidos por ahora; pide al mesero."),
    QR_DESHABILITADO("El restaurante no recibe pedidos por QR; pide al mesero."),
    LIMITE_QR_EXCEDIDO("El pedido es demasiado grande para hacerlo desde el QR; pide al mesero."),
    TASA_EXCEDIDA("Demasiados pedidos seguidos. Espera un momento e inténtalo de nuevo."),
    CUENTA_SOLICITADA("La cuenta ya se pidió; pide al mesero."),
    LLAVE_REQUERIDA("Falta la llave de idempotencia del pedido."),
    LLAVE_REUTILIZADA("Esa llave de idempotencia ya se usó con un pedido distinto."),
    DISPOSITIVO_INVALIDO("Falta el identificador del dispositivo."),
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
