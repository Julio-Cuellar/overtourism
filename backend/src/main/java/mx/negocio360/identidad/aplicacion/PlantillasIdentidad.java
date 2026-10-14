package mx.negocio360.identidad.aplicacion;

import mx.negocio360.identidad.dominio.CodigoError;

/**
 * Texto fijo en español de Mexico de cada codigo de error. Quien responde no improvisa y los textos nunca llevan
 * datos de la persona. Los de autenticacion no dicen cual fue la causa (SI-14).
 */
public final class PlantillasIdentidad {

    private PlantillasIdentidad() {
    }

    public static String texto(CodigoError codigo) {
        return switch (codigo) {
            case TELEFONO_INVALIDO -> "El teléfono no es válido. Escríbelo con diez dígitos, con o sin la lada +52.";
            case TASA_EXCEDIDA -> "Hiciste demasiadas solicitudes. Espera unos minutos e inténtalo de nuevo.";
            case CODIGO_INVALIDO -> "El código no es correcto. Revisa el número que recibiste o pide uno nuevo.";
            case CODIGO_EXPIRADO -> "El código venció. Pide uno nuevo.";
            case CODIGO_AGOTADO -> "El código ya no admite más intentos. Pide uno nuevo.";
            case NOMBRE_REQUERIDO -> "Escribe tu nombre para crear tu cuenta.";
            case NOMBRE_INVALIDO -> "El nombre debe tener entre 2 y 80 caracteres y no puede llevar caracteres de control.";
            case NO_AUTENTICADO -> "Tu sesión no es válida. Inicia sesión de nuevo.";
            case ERROR_TEMPORAL -> "No pudimos enviar el código en este momento. Inténtalo de nuevo en unos minutos.";
            case SOLICITUD_INVALIDA -> "La solicitud no se pudo leer. Revisa los datos que enviaste.";
            case ERROR_INTERNO -> "Ocurrió un error inesperado. Inténtalo de nuevo.";
        };
    }
}
