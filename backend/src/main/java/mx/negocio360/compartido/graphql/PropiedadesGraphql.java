package mx.negocio360.compartido.graphql;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites de una consulta GraphQL (SG-06, SG-08 y SG-09), bajo {@code seguridad.graphql}. Los valores iniciales son los
 * que acepto el equipo (D-45 los llama iniciales: se ajustan con el uso real). Un valor fuera de rango impide el
 * arranque, en lugar de dejar un limite que no limita.
 *
 * @param profundidadMaxima        niveles de campos anidados; los de la raiz valen 1
 * @param complejidadMaxima        1 por campo mas la de sus hijos, con cada uso de un fragmento contado completo
 * @param aliasMaximos             campos con alias en toda la operacion (con alias se repite un campo para barrer datos)
 * @param camposRaizMaximos        campos que se piden en la raiz de la operacion
 * @param consultaMaximaCaracteres largo del texto de la consulta, medido antes de analizarla. No puede pasar de 10 000:
 *                                 es el largo maximo de una cadena JSON que acepta Jackson (StreamReadConstraints), y la
 *                                 consulta viaja como una cadena JSON; una mas larga ni llega aqui (HTTP 400)
 * @param tiempoMaximo             cuanto espera la respuesta a que el motor termine
 */
@ConfigurationProperties(prefix = "seguridad.graphql")
public record PropiedadesGraphql(Integer profundidadMaxima, Integer complejidadMaxima, Integer aliasMaximos,
                                 Integer camposRaizMaximos, Integer consultaMaximaCaracteres, Duration tiempoMaximo) {

    private static final int PROFUNDIDAD_POR_OMISION = 7;
    private static final int COMPLEJIDAD_POR_OMISION = 100;
    private static final int ALIAS_POR_OMISION = 10;
    private static final int CAMPOS_RAIZ_POR_OMISION = 5;
    private static final int CARACTERES_POR_OMISION = 10_000;
    private static final Duration TIEMPO_POR_OMISION = Duration.ofSeconds(10);

    public PropiedadesGraphql {
        profundidadMaxima = rango("profundidad-maxima", profundidadMaxima, PROFUNDIDAD_POR_OMISION, 1, 50);
        complejidadMaxima = rango("complejidad-maxima", complejidadMaxima, COMPLEJIDAD_POR_OMISION, 1, 10_000);
        aliasMaximos = rango("alias-maximos", aliasMaximos, ALIAS_POR_OMISION, 1, 1_000);
        camposRaizMaximos = rango("campos-raiz-maximos", camposRaizMaximos, CAMPOS_RAIZ_POR_OMISION, 1, 100);
        consultaMaximaCaracteres = rango("consulta-maxima-caracteres", consultaMaximaCaracteres,
                CARACTERES_POR_OMISION, 1, CARACTERES_POR_OMISION);
        tiempoMaximo = tiempoMaximo == null ? TIEMPO_POR_OMISION : tiempoMaximo;
        if (tiempoMaximo.compareTo(Duration.ofMillis(100)) < 0 || tiempoMaximo.compareTo(Duration.ofSeconds(60)) > 0) {
            throw new IllegalArgumentException("seguridad.graphql.tiempo-maximo debe estar entre 100 ms y 60 s");
        }
    }

    private static int rango(String nombre, Integer valor, int porOmision, int minimo, int maximo) {
        var usado = valor == null ? porOmision : valor;
        if (usado < minimo || usado > maximo) {
            throw new IllegalArgumentException("seguridad.graphql." + nombre + " debe estar entre " + minimo + " y "
                    + maximo);
        }
        return usado;
    }
}
