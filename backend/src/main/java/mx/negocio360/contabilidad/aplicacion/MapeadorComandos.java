package mx.negocio360.contabilidad.aplicacion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * JSON de los comandos guardados en {@code preparacion}. A diferencia del de eventos, es estricto al leer (un campo
 * desconocido es una alteracion) y escribe las propiedades en orden alfabetico, para que el mismo comando produzca
 * siempre el mismo texto y el mismo hash.
 */
public final class MapeadorComandos {

    private MapeadorComandos() {
    }

    /**
     * Crea el mapeador estricto de comandos.
     *
     * @return un mapeador nuevo
     */
    public static ObjectMapper crear() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
    }
}
