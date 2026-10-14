package mx.negocio360.compartido.web;

import com.fasterxml.jackson.core.StreamReadConstraints;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tope al texto que se acepta al leer JSON (SG-10, SI-16). Jackson acepta por omisión cadenas de hasta 20 millones de
 * caracteres: unas cuantas peticiones anónimas con un nombre así de largo agotarían la memoria antes de que ninguna
 * regla de negocio pudiera rechazarlas. Ningún texto de la plataforma se acerca a este largo.
 */
@Configuration
public class LimitesDeJson {

    /** Largo máximo de una sola cadena en un cuerpo JSON. */
    public static final int LARGO_MAXIMO_DE_CADENA = 10_000;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer limitesDeLecturaDeJson() {
        var limites = StreamReadConstraints.builder().maxStringLength(LARGO_MAXIMO_DE_CADENA).build();
        return constructor -> constructor.postConfigurer(mapeador -> mapeador.getFactory().setStreamReadConstraints(limites));
    }
}
