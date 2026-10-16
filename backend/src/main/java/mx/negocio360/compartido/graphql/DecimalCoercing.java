package mx.negocio360.compartido.graphql;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import java.math.BigDecimal;
import java.util.Locale;
import mx.negocio360.compartido.dinero.DecimalPlano;

/** Decimal exacto que viaja como cadena. Nunca acepta ni devuelve un numero de punto flotante. */
public final class DecimalCoercing implements Coercing<BigDecimal, String> {

    @Override
    public String serialize(Object resultado, GraphQLContext contexto, Locale idioma)
            throws CoercingSerializeException {
        if (resultado instanceof BigDecimal decimal) {
            return DecimalPlano.escribir(decimal);
        }
        throw new CoercingSerializeException("Decimal solo serializa BigDecimal, no " + tipoDe(resultado));
    }

    @Override
    public BigDecimal parseValue(Object entrada, GraphQLContext contexto, Locale idioma)
            throws CoercingParseValueException {
        if (entrada instanceof String texto) {
            return DecimalPlano.leer(texto).orElseThrow(() -> new CoercingParseValueException(
                    "Decimal debe ser plano y de largo razonable, por ejemplo \"207.00\""));
        }
        throw new CoercingParseValueException("Decimal debe venir como cadena, por ejemplo \"207.00\"");
    }

    @Override
    public BigDecimal parseLiteral(Value<?> entrada, CoercedVariables variables, GraphQLContext contexto,
                                   Locale idioma) throws CoercingParseLiteralException {
        if (entrada instanceof StringValue cadena) {
            return DecimalPlano.leer(cadena.getValue()).orElseThrow(() -> new CoercingParseLiteralException(
                    "Decimal debe ser plano y de largo razonable, por ejemplo \"207.00\""));
        }
        throw new CoercingParseLiteralException("Decimal debe escribirse como cadena, por ejemplo \"207.00\"");
    }

    @Override
    public Value<?> valueToLiteral(Object entrada, GraphQLContext contexto, Locale idioma) {
        var texto = entrada instanceof BigDecimal decimal ? DecimalPlano.escribir(decimal) : String.valueOf(entrada);
        return StringValue.of(texto);
    }

    private static String tipoDe(Object valor) {
        return valor == null ? "null" : valor.getClass().getSimpleName();
    }
}
