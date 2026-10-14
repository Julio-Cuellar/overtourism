package mx.negocio360.compartido.graphql;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Fecha sin hora en ISO-8601 (aaaa-mm-dd). */
public final class FechaCoercing implements Coercing<LocalDate, String> {

    @Override
    public String serialize(Object resultado, GraphQLContext contexto, Locale idioma)
            throws CoercingSerializeException {
        if (resultado instanceof LocalDate fecha) {
            return DateTimeFormatter.ISO_LOCAL_DATE.format(fecha);
        }
        throw new CoercingSerializeException("Date solo serializa LocalDate");
    }

    @Override
    public LocalDate parseValue(Object entrada, GraphQLContext contexto, Locale idioma)
            throws CoercingParseValueException {
        if (entrada instanceof String texto) {
            try {
                return LocalDate.parse(texto, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                throw new CoercingParseValueException("Date debe ser ISO-8601, por ejemplo 2026-10-05", e);
            }
        }
        throw new CoercingParseValueException("Date debe venir como cadena ISO-8601");
    }

    @Override
    public LocalDate parseLiteral(Value<?> entrada, CoercedVariables variables, GraphQLContext contexto,
                                  Locale idioma) throws CoercingParseLiteralException {
        if (entrada instanceof StringValue cadena) {
            try {
                return LocalDate.parse(cadena.getValue(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                throw new CoercingParseLiteralException("Date debe ser ISO-8601, por ejemplo 2026-10-05", e);
            }
        }
        throw new CoercingParseLiteralException("Date debe escribirse como cadena ISO-8601");
    }

    @Override
    public Value<?> valueToLiteral(Object entrada, GraphQLContext contexto, Locale idioma) {
        var texto = entrada instanceof LocalDate fecha ? DateTimeFormatter.ISO_LOCAL_DATE.format(fecha)
                : String.valueOf(entrada);
        return StringValue.of(texto);
    }
}
