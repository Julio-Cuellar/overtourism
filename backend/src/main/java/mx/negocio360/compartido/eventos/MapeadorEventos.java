package mx.negocio360.compartido.eventos;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.math.BigDecimal;
import mx.negocio360.compartido.dinero.DecimalPlano;

/**
 * Mapeador de JSON de los contratos de eventos. Es un lector tolerante (ignora campos nuevos, R5), escribe las
 * fechas en ISO-8601 y trata el dinero como cadena decimal plana: nunca lo acepta ni lo escribe como numero.
 *
 * <p>Todo consumidor de eventos debe usar este mapeador. Si usa el que Spring trae por defecto, pierde la
 * validacion estricta del dinero.
 */
public final class MapeadorEventos {

    private MapeadorEventos() {
    }

    public static ObjectMapper crear() {
        var dineroComoCadena = new SimpleModule("DineroComoCadena");
        dineroComoCadena.addSerializer(BigDecimal.class, new EscritorDecimal());
        dineroComoCadena.addDeserializer(BigDecimal.class, new LectorDecimalEstricto());

        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(dineroComoCadena)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    private static final class EscritorDecimal extends StdSerializer<BigDecimal> {

        EscritorDecimal() {
            super(BigDecimal.class);
        }

        @Override
        public void serialize(BigDecimal valor, JsonGenerator generador, SerializerProvider proveedor)
                throws IOException {
            generador.writeString(DecimalPlano.escribir(valor));
        }
    }

    private static final class LectorDecimalEstricto extends StdDeserializer<BigDecimal> {

        LectorDecimalEstricto() {
            super(BigDecimal.class);
        }

        @Override
        public BigDecimal deserialize(JsonParser lector, DeserializationContext contexto) throws IOException {
            if (lector.currentToken() != JsonToken.VALUE_STRING) {
                return (BigDecimal) contexto.handleUnexpectedToken(BigDecimal.class, lector);
            }
            var texto = lector.getText();
            var decimal = DecimalPlano.leer(texto);
            if (decimal.isEmpty()) {
                return (BigDecimal) contexto.handleWeirdStringValue(BigDecimal.class, texto,
                        "no es un decimal plano (sin exponente, signo mas ni espacios, hasta 14 enteros y 8 decimales)");
            }
            return decimal.get();
        }
    }
}
