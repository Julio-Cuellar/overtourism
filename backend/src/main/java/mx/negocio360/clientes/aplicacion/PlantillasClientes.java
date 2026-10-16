package mx.negocio360.clientes.aplicacion;

import mx.negocio360.clientes.dominio.CodigoError;

/**
 * Textos fijos en espanol de Mexico para cada codigo de error. La capa que responde a la persona usa estas plantillas
 * y no improvisa texto. Ninguna lleva datos personales (P-06).
 */
public final class PlantillasClientes {

    public static final String CLIENTE_GUARDADO = "Cliente guardado.";
    public static final String CLIENTE_ACTUALIZADO = "Cliente actualizado.";
    public static final String AVISO_PUBLICADO = "Aviso de privacidad publicado.";

    private PlantillasClientes() {
    }

    public static String de(CodigoError codigo) {
        return switch (codigo) {
            case NOMBRE_INVALIDO -> "El nombre debe tener entre 1 y 80 caracteres, igual que los apellidos.";
            case TELEFONO_INVALIDO -> "El teléfono no es válido. Escríbelo con lada, a 10 dígitos.";
            case TELEFONO_DUPLICADO -> "Ya tienes un cliente con ese teléfono. ¿Quieres abrirlo?";
            case CORREO_INVALIDO -> "El correo no es válido. Revisa que tenga @ y un dominio.";
            case FECHA_NACIMIENTO_INVALIDA -> "La fecha de nacimiento no puede ser posterior a hoy.";
            case NOTAS_MUY_LARGAS -> "Las notas son muy largas. Déjalas en 500 caracteres o menos.";
            case CLIENTE_NO_ENCONTRADO -> "No encontramos a ese cliente.";
            case CLIENTE_ANONIMIZADO -> "Ese cliente ya fue anonimizado y no se puede modificar.";
            case SOLICITUD_NO_ENCONTRADA -> "No encontramos esa solicitud.";
            case SOLICITUD_CERRADA -> "Esa solicitud ya está cerrada.";
            case FINALIDAD_INVALIDA -> "La finalidad del consentimiento no es válida o está repetida.";
            case CONSENTIMIENTO_INCOMPLETO -> "Falta indicar el medio por el que se dio el consentimiento.";
            case AVISO_NO_VIGENTE -> "Antes de registrar clientes publica el aviso de privacidad vigente.";
            case AVISO_INCOMPLETO -> "Al aviso de privacidad le falta información obligatoria o se pasa del largo.";
            case AVISO_VERSION_REQUERIDA -> "Indica la versión del aviso de privacidad que se entregó.";
            case IDENTIDAD_NO_VERIFICADA -> "Primero hay que verificar la identidad de la persona titular.";
            case MOTIVO_REQUERIDO -> "Si la solicitud se niega, hay que explicar el motivo.";
            case PLAZO_YA_AMPLIADO -> "El plazo de esa solicitud ya se amplió una vez.";
            case TOKEN_INVALIDO -> "La confirmación no es válida. Vuelve a empezar.";
            case TOKEN_EXPIRADO -> "La confirmación venció. Vuelve a empezar.";
            case TOKEN_USADO -> "Esa confirmación ya se usó.";
            case COMANDO_CAMBIO -> "Lo que ibas a confirmar cambió. Vuelve a empezar.";
            case CONCURRENCIA -> "Alguien más modificó este registro. Recarga e inténtalo de nuevo.";
            case ERROR_TEMPORAL -> "Hubo un problema temporal. Inténtalo de nuevo en un momento.";
        };
    }
}
