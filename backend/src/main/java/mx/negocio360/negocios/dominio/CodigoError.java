package mx.negocio360.negocios.dominio;

/**
 * Codigos de error de Negocios (seccion 7.8 del diseno). El codigo y su texto son fijos y no llevan datos personales:
 * quien pide aprende la causa, no el dato. Los reintentables son {@link #CONCURRENCIA} y {@link #ERROR_TEMPORAL}.
 */
public enum CodigoError {
    NOMBRE_INVALIDO("El nombre del negocio debe tener entre 2 y 80 caracteres y no puede llevar caracteres de control."),
    GIRO_INVALIDO("Ese giro no está disponible."),
    DESCRIPCION_INVALIDA("La descripción puede tener hasta 280 caracteres y no puede llevar caracteres de control."),
    CONTACTO_INVALIDO("El teléfono o el correo de contacto no tienen un formato válido."),
    NEGOCIO_NO_DISPONIBLE("Ese negocio no está disponible."),
    NEGOCIO_CERRADO("El negocio está cerrado y ya no se puede editar."),
    NEGOCIO_SUSPENDIDO("El negocio está suspendido."),
    ESTADO_INVALIDO("Ese cambio de estado no está permitido."),
    PUBLICACION_INCOMPLETA("Faltan datos para publicar el negocio."),
    SUBCATEGORIA_INVALIDA("Una de las subcategorías no existe o no está disponible."),
    SUBCATEGORIAS_EXCEDIDAS("Un negocio puede tener hasta 8 subcategorías."),
    SUCURSAL_NO_ENCONTRADA("Esa sucursal no existe."),
    DIRECCION_INVALIDA("Algún dato de la dirección es demasiado largo o no es válido."),
    COORDENADAS_INVALIDAS("La latitud y la longitud van juntas y dentro de su rango."),
    CODIGO_POSTAL_INVALIDO("El código postal debe tener cinco dígitos."),
    HORARIO_INVALIDO("El horario no es válido."),
    TRAMOS_EXCEDIDOS("Un día puede tener hasta tres tramos de horario."),
    EXCEPCION_DUPLICADA("Ya hay una excepción para esa fecha."),
    EXCEPCION_NO_ENCONTRADA("Esa excepción no existe."),
    ATRIBUTO_INVALIDO("Uno de los atributos no es válido."),
    ATRIBUTOS_EXCEDIDOS("Hay demasiados atributos de un mismo tipo."),
    DISPONIBILIDAD_INVALIDA("El nivel de disponibilidad no es válido."),
    CALIFICACION_INVALIDA("La calificación debe ser un número de 1 a 5."),
    COMENTARIO_INVALIDO("El comentario puede tener hasta 1000 caracteres y no puede llevar caracteres de control."),
    RESPUESTA_INVALIDA("La respuesta debe tener entre 1 y 500 caracteres y no puede llevar caracteres de control."),
    RESENA_YA_EXISTE("Ya dejaste una reseña en este negocio."),
    RESENA_YA_RESPONDIDA("Esa reseña ya tiene una respuesta."),
    RESENA_NO_ENCONTRADA("Esa reseña no existe."),
    DISPOSITIVO_INVALIDO("El identificador del dispositivo no es válido."),
    MODO_DEMOSTRACION_APAGADO("Esta función solo está disponible en el modo de demostración."),
    DEMASIADAS_SOLICITUDES("Hiciste demasiadas solicitudes. Inténtalo más tarde."),
    TOKEN_INVALIDO("El permiso para confirmar no es válido."),
    TOKEN_EXPIRADO("El permiso para confirmar venció. Vuelve a empezar."),
    TOKEN_USADO("Ese permiso ya se usó."),
    CONSULTA_INVALIDA("La consulta pide más de lo permitido o está mal formada."),
    CONCURRENCIA("Otra persona cambió esto mientras lo editabas. Vuelve a abrirlo."),
    ERROR_TEMPORAL("Hubo un problema temporal. Inténtalo de nuevo.");

    private final String mensaje;

    CodigoError(String mensaje) {
        this.mensaje = mensaje;
    }

    public String mensaje() {
        return mensaje;
    }
}
