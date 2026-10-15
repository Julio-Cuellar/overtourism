package mx.negocio360.compartido.comandos;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Respuesta de un comando confirmado con exito: las piezas {@code codigo}, {@code datos} y {@code mensaje_plantilla}
 * del contrato {@code { ok, codigo, datos, mensaje_plantilla }}. Los fallos no se devuelven aqui: salen como la
 * excepcion de negocio de cada modulo, con su codigo.
 *
 * @param codigo           siempre {@value #OK}
 * @param mensajePlantilla texto fijo en espanol de Mexico para mostrar a la persona
 * @param recursoId        id del recurso creado o modificado
 * @param datos            valores de respuesta como texto (folio, total, cambio...); dinero como decimal plano
 */
public record ResultadoComando(String codigo, String mensajePlantilla, UUID recursoId, Map<String, String> datos) {

    /** Unico codigo de exito. */
    public static final String OK = "OK";

    /** Exige codigo y plantilla y copia los datos para que el resultado sea inmutable. */
    public ResultadoComando {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(mensajePlantilla, "mensajePlantilla");
        datos = Map.copyOf(Objects.requireNonNull(datos, "datos"));
    }

    /**
     * Resultado exitoso.
     *
     * @param mensajePlantilla texto fijo para la persona
     * @param recursoId        id del recurso
     * @param datos            valores de respuesta
     * @return resultado con codigo {@value #OK}
     */
    public static ResultadoComando ok(String mensajePlantilla, UUID recursoId, Map<String, String> datos) {
        return new ResultadoComando(OK, mensajePlantilla, recursoId, datos);
    }
}
