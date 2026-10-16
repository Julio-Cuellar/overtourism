package mx.negocio360.inventario.aplicacion;

import mx.negocio360.inventario.dominio.CodigoError;

/**
 * Textos fijos en espanol de Mexico para cada codigo de error. La capa que responde a la persona usa estas plantillas
 * y no improvisa texto.
 */
public final class PlantillasInventario {

    public static final String ARTICULO_GUARDADO = "Artículo guardado.";
    public static final String ARTICULO_ACTUALIZADO = "Artículo actualizado.";
    public static final String UNIDAD_DEFINIDA = "Unidad guardada.";
    public static final String ARTICULO_DADO_DE_BAJA = "Artículo dado de baja.";
    public static final String MOVIMIENTO_REGISTRADO = "Movimiento registrado.";

    private PlantillasInventario() {
    }

    public static String de(CodigoError codigo) {
        return switch (codigo) {
            case ARTICULO_NO_ENCONTRADO -> "No encontramos ese artículo.";
            case ARTICULO_INACTIVO -> "Ese artículo está dado de baja y ya no se puede modificar.";
            case NOMBRE_INVALIDO -> "El nombre debe tener entre 1 y 80 caracteres, sin caracteres especiales.";
            case NOMBRE_DUPLICADO -> "Ya tienes un artículo con ese nombre. ¿Quieres abrirlo?";
            case CATEGORIA_INVALIDA -> "La categoría debe tener 40 caracteres o menos.";
            case RASTREO_NO_SOPORTADO -> "Por ahora solo se pueden crear artículos sin lote ni número de serie.";
            case BAJA_CON_SALDO -> "Solo se puede dar de baja un artículo con saldo en cero. Haz un ajuste por conteo.";
            case UNIDAD_NO_CONVERTIBLE -> "Esa unidad no se puede convertir a la unidad base del artículo.";
            case UNIDAD_PROPIA_INVALIDA ->
                    "La unidad propia no es válida: usa un nombre nuevo (solo letras) y un factor mayor que cero.";
            case CANTIDAD_INVALIDA -> "La cantidad no es válida. Debe ser mayor que cero, con hasta 4 decimales.";
            case COSTO_INVALIDO -> "El costo no es válido. No puede ser negativo y lleva hasta 4 decimales.";
            case FECHA_FUTURA -> "La fecha del movimiento no puede ser posterior a este momento.";
            case MOTIVO_REQUERIDO -> "Indica el motivo de la merma.";
            case CONTEO_SIN_DIFERENCIA -> "Lo contado es igual al saldo; no hay nada que ajustar.";
            case CONSUMIDOR_INVALIDO -> "Ese consumidor de inventario no existe.";
            case TOKEN_INVALIDO -> "La confirmación no es válida. Vuelve a empezar.";
            case TOKEN_EXPIRADO -> "La confirmación venció. Vuelve a empezar.";
            case TOKEN_USADO -> "Esa confirmación ya se usó.";
            case COMANDO_CAMBIO -> "El saldo cambió desde que lo preparaste. Vuelve a empezar.";
            case CONCURRENCIA -> "Alguien más modificó este registro. Recarga e inténtalo de nuevo.";
            case ERROR_TEMPORAL -> "Hubo un problema temporal. Inténtalo de nuevo en un momento.";
        };
    }
}
