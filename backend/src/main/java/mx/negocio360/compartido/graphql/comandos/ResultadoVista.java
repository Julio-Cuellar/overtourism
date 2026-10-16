package mx.negocio360.compartido.graphql.comandos;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.comandos.ResultadoComando;

/**
 * Resultado de confirmar un comando, con la forma del tipo {@code Resultado} de GraphQL. {@code datos} nunca es nulo:
 * si no hay datos es una lista vacia (el tipo lo exige, H-06).
 *
 * @param ok               verdadero si el codigo es {@code OK}
 * @param codigo           codigo fijo
 * @param mensajePlantilla texto fijo para la persona
 * @param recursoId        id de lo creado o cambiado, o nulo
 * @param datos            datos del resultado, ordenados por clave
 */
public record ResultadoVista(boolean ok, String codigo, String mensajePlantilla, UUID recursoId, List<DatoVista> datos) {

    /** Copia los datos para que la vista sea inmutable. */
    public ResultadoVista {
        datos = List.copyOf(datos);
    }

    /**
     * Convierte el resultado de un manejador de {@code compartido.comandos}.
     *
     * @param resultado resultado exitoso del manejador
     * @return la vista, con los datos ordenados por clave
     */
    public static ResultadoVista de(ResultadoComando resultado) {
        var datos = resultado.datos().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> new DatoVista(e.getKey(), e.getValue())).toList();
        return new ResultadoVista(ResultadoComando.OK.equals(resultado.codigo()), resultado.codigo(),
                resultado.mensajePlantilla(), resultado.recursoId(), datos);
    }

    /**
     * Un fallo que se entrega como resultado y no como error de GraphQL (lo usa la resena del turista).
     *
     * @param codigo           codigo fijo
     * @param mensajePlantilla texto fijo
     * @return resultado con {@code ok = false} y sin datos
     */
    public static ResultadoVista fallo(String codigo, String mensajePlantilla) {
        return new ResultadoVista(false, codigo, mensajePlantilla, null, List.of());
    }
}
