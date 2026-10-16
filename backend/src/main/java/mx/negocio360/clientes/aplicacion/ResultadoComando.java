package mx.negocio360.clientes.aplicacion;

import java.util.UUID;

/**
 * Respuesta de un comando confirmado con exito. Los fallos no se devuelven aqui: salen como ClientesException con su
 * codigo. El mensaje es una plantilla fija; nunca lleva datos personales.
 *
 * @param codigo           siempre {@value #OK}
 * @param mensajePlantilla texto en espanol de Mexico para mostrar a la persona
 * @param recursoId        id del recurso creado o modificado
 */
public record ResultadoComando(String codigo, String mensajePlantilla, UUID recursoId) {

    public static final String OK = "OK";

    public static ResultadoComando ok(String mensajePlantilla, UUID recursoId) {
        return new ResultadoComando(OK, mensajePlantilla, recursoId);
    }
}
