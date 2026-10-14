package mx.negocio360.compartido.graphql;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Fecha y hora en ISO-8601. Siempre sale en UTC con Z; al leer, cualquier offset se normaliza a UTC. */
public final class FechaHoraCoercing implements Coercing<Instant, String> {

    @Override
    public String serialize(Object resultado, GraphQLContext contexto, Locale idioma)
            throws CoercingSerializeException {
        if (resultado instanceof Instant instante) {
            return DateTimeFormatter.ISO_INSTANT.format(instante);
        }
        if (resultado instanceof OffsetDateTime fechaHora) {
            return DateTimeFormatter.ISO_INSTANT.format(fechaHora.toInstant());
        }
        throw new CoercingSerializeException("DateTime solo serializa Instant u OffsetDateTime");
    }

    @Override
    public Instant parseValue(Object entrada, GraphQLContext contexto, Locale idioma)
            throws CoercingParseValueException {
        if (entrada instanceof String texto) {
            try {
                return OffsetDateTime.parse(texto).toInstant();
            } catch (DateTimeParseException e) {
                throw new CoercingParseValueException("DateTime debe ser ISO-8601 con zona, por ejemplo "
                        + "2026-10-05T18:22:41Z", e);
            }
        }
        throw new CoercingParseValueException("DateTime debe venir como cadena ISO-8601");
    }

    @Override
    public Instant parseLiteral(Value<?> entrada, CoercedVariables variables, GraphQLContext contexto,
                                Locale idioma) throws CoercingParseLiteralException {
        if (entrada instanceof StringValue cadena) {
            try {
                return OffsetDateTime.parse(cadena.getValue()).toInstant();
            } catch (DateTimeParseException e) {
                throw new CoercingParseLiteralException("DateTime debe ser ISO-8601 con zona", e);
            }
        }
        throw new CoercingParseLiteralException("DateTime debe escribirse como cadena ISO-8601");
    }

    @Override
    public Value<?> valueToLiteral(Object entrada, GraphQLContext contexto, Locale idioma) {
        var texto = entrada instanceof Instant instante ? DateTimeFormatter.ISO_INSTANT.format(instante)
                : String.valueOf(entrada);
        return StringValue.of(texto);
    }
}
