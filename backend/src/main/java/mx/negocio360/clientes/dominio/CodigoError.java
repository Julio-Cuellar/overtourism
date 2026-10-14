package mx.negocio360.clientes.dominio;

/** Codigos de error fijos del modulo (seccion 7.4 del diseno). Ninguno lleva datos personales. */
public enum CodigoError {
    // Datos del cliente
    NOMBRE_INVALIDO,
    TELEFONO_INVALIDO,
    TELEFONO_DUPLICADO,
    CORREO_INVALIDO,
    FECHA_NACIMIENTO_INVALIDA,
    NOTAS_MUY_LARGAS,
    // Existencia y estado
    CLIENTE_NO_ENCONTRADO,
    CLIENTE_ANONIMIZADO,
    SOLICITUD_NO_ENCONTRADA,
    SOLICITUD_CERRADA,
    // Consentimiento y aviso
    FINALIDAD_INVALIDA,
    CONSENTIMIENTO_INCOMPLETO,
    AVISO_NO_VIGENTE,
    AVISO_INCOMPLETO,
    AVISO_VERSION_REQUERIDA,
    // ARCO
    IDENTIDAD_NO_VERIFICADA,
    MOTIVO_REQUERIDO,
    PLAZO_YA_AMPLIADO,
    // Preparar y confirmar
    TOKEN_INVALIDO,
    TOKEN_EXPIRADO,
    TOKEN_USADO,
    COMANDO_CAMBIO,
    // Reintentables
    CONCURRENCIA,
    ERROR_TEMPORAL
}
