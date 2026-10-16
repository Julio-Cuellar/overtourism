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
            case CODIGO_INVALIDO ->
                    "El código no es correcto o ya venció. Revisa el que recibiste en tu correo o pide uno nuevo.";
            case NOMBRE_REQUERIDO -> "Escribe tu nombre para crear tu cuenta.";
            case APELLIDOS_INVALIDOS ->
                    "Los apellidos deben tener entre 2 y 80 caracteres y no pueden llevar caracteres de control.";
            case CORREO_INVALIDO -> "El correo no es válido. Revisa que tenga la forma nombre@dominio.com.";
            case CORREO_NO_DISPONIBLE -> "Ese correo no está disponible. Usa otro correo.";
            case CUENTA_YA_EXISTE -> "Ya existe una cuenta con ese celular. Inicia sesión.";
            case AVISO_REQUERIDO -> "Para crear tu cuenta debes aceptar el aviso de privacidad.";
            case PERMISO_DENEGADO -> "No tienes permiso para hacer esto.";
            case NEGOCIO_NO_DISPONIBLE -> "Ese negocio no está disponible. Elige otro negocio.";
            case GIRO_INVALIDO -> "El giro no es válido. Elige restaurante, miscelánea o museo.";
            case ULTIMO_DUENO ->
                    "El negocio debe conservar al menos un dueño activo. Nombra a otro dueño antes de continuar.";
            case ROL_NO_PERMITIDO -> "Ese rol no está disponible para este negocio o supera tus permisos.";
            case OPERACION_SOBRE_SI_MISMO ->
                    "No puedes hacer esto con tu propia cuenta. Pídele a otra persona del equipo que lo haga.";
            case NO_ES_MIEMBRO -> "Esa persona no forma parte de este negocio.";
            case NOMBRE_INVALIDO -> "El nombre debe tener entre 2 y 80 caracteres y no puede llevar caracteres de control.";
            case NO_AUTENTICADO -> "Tu sesión no es válida. Inicia sesión de nuevo.";
            case ERROR_TEMPORAL -> "No pudimos enviar el código a tu correo en este momento. Inténtalo de nuevo en unos minutos.";
            case SOLICITUD_INVALIDA -> "La solicitud no se pudo leer. Revisa los datos que enviaste.";
            case ERROR_INTERNO -> "Ocurrió un error inesperado. Inténtalo de nuevo.";
        };
    }
}
