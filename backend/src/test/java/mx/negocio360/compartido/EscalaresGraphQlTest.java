package mx.negocio360.compartido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import mx.negocio360.compartido.graphql.Escalares;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** El dinero viaja como texto decimal y las fechas en ISO-8601: nunca como numero de punto flotante. */
class EscalaresGraphQlTest {

    private static final GraphQLContext CONTEXTO = GraphQLContext.getDefault();

    private static Object serializar(GraphQLScalarType escalar, Object valor) {
        return escalar.getCoercing().serialize(valor, CONTEXTO, Locale.ROOT);
    }

    private static Object parsear(GraphQLScalarType escalar, Object valor) {
        return escalar.getCoercing().parseValue(valor, CONTEXTO, Locale.ROOT);
    }

    @Test
    @DisplayName("Decimal: los nombres de los escalares coinciden con el esquema")
    void nombres_escalares_coincidenConElEsquema() {
        assertThat(Escalares.DECIMAL.getName()).isEqualTo("Decimal");
        assertThat(Escalares.FECHA.getName()).isEqualTo("Date");
        assertThat(Escalares.FECHA_HORA.getName()).isEqualTo("DateTime");
    }

    @Test
    @DisplayName("Decimal: serializa conservando los dos decimales")
    void decimal_serializarBigDecimal_conservaLaEscala() {
        assertThat(serializar(Escalares.DECIMAL, new BigDecimal("207.50"))).isEqualTo("207.50");
    }

    @Test
    @DisplayName("Decimal: nunca serializa en notacion cientifica")
    void decimal_serializarExponente_usaNotacionPlana() {
        assertThat(serializar(Escalares.DECIMAL, new BigDecimal("1E+2"))).isEqualTo("100");
    }

    @Test
    @DisplayName("Decimal: no serializa un double")
    void decimal_serializarDouble_lanzaExcepcion() {
        assertThatThrownBy(() -> serializar(Escalares.DECIMAL, 1.5d))
                .isInstanceOf(CoercingSerializeException.class);
    }

    @Test
    @DisplayName("Decimal: lee una cadena y conserva la escala")
    void decimal_parsearCadena_conservaLaEscala() {
        assertThat(parsear(Escalares.DECIMAL, "207.00")).isEqualTo(new BigDecimal("207.00"));
    }

    @Test
    @DisplayName("Decimal: rechaza numeros y texto que no sea decimal")
    void decimal_parsearNumeroOTextoInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> parsear(Escalares.DECIMAL, 207.0d))
                .isInstanceOf(CoercingParseValueException.class);
        assertThatThrownBy(() -> parsear(Escalares.DECIMAL, 207))
                .isInstanceOf(CoercingParseValueException.class);
        assertThatThrownBy(() -> parsear(Escalares.DECIMAL, "abc"))
                .isInstanceOf(CoercingParseValueException.class);
    }

    @Test
    @DisplayName("Decimal: el literal debe ser una cadena, no un numero")
    void decimal_parsearLiteral_soloAceptaCadenas() {
        var coercing = Escalares.DECIMAL.getCoercing();

        assertThat(coercing.parseLiteral(new StringValue("12.50"), CoercedVariables.emptyVariables(), CONTEXTO,
                Locale.ROOT)).isEqualTo(new BigDecimal("12.50"));
        assertThatThrownBy(() -> coercing.parseLiteral(new FloatValue(new BigDecimal("12.5")),
                CoercedVariables.emptyVariables(), CONTEXTO, Locale.ROOT))
                .isInstanceOf(CoercingParseLiteralException.class);
    }

    @Test
    @DisplayName("Date: lee y escribe fechas ISO-8601")
    void fecha_isoValida_idaYVuelta() {
        assertThat(parsear(Escalares.FECHA, "2026-10-05")).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(serializar(Escalares.FECHA, LocalDate.of(2026, 10, 5))).isEqualTo("2026-10-05");
    }

    @Test
    @DisplayName("Date: rechaza formatos que no son ISO")
    void fecha_formatoLocal_lanzaExcepcion() {
        assertThatThrownBy(() -> parsear(Escalares.FECHA, "05/10/2026"))
                .isInstanceOf(CoercingParseValueException.class);
    }

    @Test
    @DisplayName("DateTime: serializa un Instant en UTC con Z")
    void fechaHora_serializarInstant_usaUtcConZ() {
        assertThat(serializar(Escalares.FECHA_HORA, Instant.parse("2026-10-05T18:22:41Z")))
                .isEqualTo("2026-10-05T18:22:41Z");
    }

    @Test
    @DisplayName("DateTime: normaliza cualquier offset a UTC")
    void fechaHora_parsearConOffset_normalizaAUtc() {
        assertThat(parsear(Escalares.FECHA_HORA, "2026-10-05T12:22:41-06:00"))
                .isEqualTo(Instant.parse("2026-10-05T18:22:41Z"));
    }

    @Test
    @DisplayName("DateTime: rechaza una fecha sin hora")
    void fechaHora_soloFecha_lanzaExcepcion() {
        assertThatThrownBy(() -> parsear(Escalares.FECHA_HORA, "2026-10-05"))
                .isInstanceOf(CoercingParseValueException.class);
    }

    @Test
    @DisplayName("Decimal: un literal con texto no decimal se rechaza")
    void decimal_parsearLiteralInvalido_lanzaExcepcion() {
        var coercing = Escalares.DECIMAL.getCoercing();

        assertThatThrownBy(() -> coercing.parseLiteral(new StringValue("abc"), CoercedVariables.emptyVariables(),
                CONTEXTO, Locale.ROOT)).isInstanceOf(CoercingParseLiteralException.class);
    }

    @Test
    @DisplayName("Date: el literal ISO se lee y cualquier otra cosa se rechaza")
    void fecha_parsearLiteral_aceptaIsoYRechazaElResto() {
        var coercing = Escalares.FECHA.getCoercing();

        assertThat(coercing.parseLiteral(new StringValue("2026-10-05"), CoercedVariables.emptyVariables(), CONTEXTO,
                Locale.ROOT)).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThatThrownBy(() -> coercing.parseLiteral(new StringValue("05/10/2026"),
                CoercedVariables.emptyVariables(), CONTEXTO, Locale.ROOT))
                .isInstanceOf(CoercingParseLiteralException.class);
        assertThatThrownBy(() -> coercing.parseLiteral(new IntValue(BigInteger.ONE),
                CoercedVariables.emptyVariables(), CONTEXTO, Locale.ROOT))
                .isInstanceOf(CoercingParseLiteralException.class);
    }

    @Test
    @DisplayName("Date: no serializa ni lee algo que no sea fecha ni cadena")
    void fecha_tiposIncorrectos_lanzanExcepcion() {
        assertThatThrownBy(() -> serializar(Escalares.FECHA, "2026-10-05"))
                .isInstanceOf(CoercingSerializeException.class);
        assertThatThrownBy(() -> parsear(Escalares.FECHA, 20261005))
                .isInstanceOf(CoercingParseValueException.class);
    }

    @Test
    @DisplayName("DateTime: el literal con zona se normaliza a UTC y el resto se rechaza")
    void fechaHora_parsearLiteral_normalizaYRechaza() {
        var coercing = Escalares.FECHA_HORA.getCoercing();

        assertThat(coercing.parseLiteral(new StringValue("2026-10-05T12:22:41-06:00"),
                CoercedVariables.emptyVariables(), CONTEXTO, Locale.ROOT))
                .isEqualTo(Instant.parse("2026-10-05T18:22:41Z"));
        assertThatThrownBy(() -> coercing.parseLiteral(new StringValue("ayer"),
                CoercedVariables.emptyVariables(), CONTEXTO, Locale.ROOT))
                .isInstanceOf(CoercingParseLiteralException.class);
        assertThatThrownBy(() -> coercing.parseLiteral(new IntValue(BigInteger.ONE),
                CoercedVariables.emptyVariables(), CONTEXTO, Locale.ROOT))
                .isInstanceOf(CoercingParseLiteralException.class);
    }

    @Test
    @DisplayName("DateTime: serializa OffsetDateTime en UTC y rechaza otros tipos")
    void fechaHora_serializarOffsetYTipoInvalido() {
        assertThat(serializar(Escalares.FECHA_HORA, OffsetDateTime.parse("2026-10-05T12:22:41-06:00")))
                .isEqualTo("2026-10-05T18:22:41Z");
        assertThatThrownBy(() -> serializar(Escalares.FECHA_HORA, "2026-10-05T18:22:41Z"))
                .isInstanceOf(CoercingSerializeException.class);
        assertThatThrownBy(() -> parsear(Escalares.FECHA_HORA, 1))
                .isInstanceOf(CoercingParseValueException.class);
    }

    @Test
    @DisplayName("Los tres escalares convierten un valor a literal de cadena")
    void escalares_valueToLiteral_devuelveCadena() {
        assertThat(textoDelLiteral(Escalares.DECIMAL, "207.00")).isEqualTo("207.00");
        assertThat(textoDelLiteral(Escalares.FECHA, "2026-10-05")).isEqualTo("2026-10-05");
        assertThat(textoDelLiteral(Escalares.FECHA_HORA, "2026-10-05T18:22:41Z")).isEqualTo("2026-10-05T18:22:41Z");
    }

    @Test
    @DisplayName("Los tres escalares escriben el literal de los tipos reales en forma plana e ISO")
    void escalares_valueToLiteralConTiposReales_usaFormaPlanaEIso() {
        assertThat(textoDelLiteral(Escalares.DECIMAL, new BigDecimal("1E+2"))).isEqualTo("100");
        assertThat(textoDelLiteral(Escalares.FECHA, LocalDate.of(2026, 10, 5))).isEqualTo("2026-10-05");
        assertThat(textoDelLiteral(Escalares.FECHA_HORA, Instant.parse("2026-10-05T18:22:41Z")))
                .isEqualTo("2026-10-05T18:22:41Z");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1E+3", "+5", ".5", "5.", "1E+999999999", "12345678901234567890", ""})
    @DisplayName("Decimal: rechaza exponentes, signos, puntos sueltos y numeros demasiado largos")
    void decimal_parsearFormasNoPlanas_lanzaExcepcion(String texto) {
        assertThatThrownBy(() -> parsear(Escalares.DECIMAL, texto))
                .isInstanceOf(CoercingParseValueException.class);
    }

    private static String textoDelLiteral(GraphQLScalarType escalar, Object valor) {
        var literal = escalar.getCoercing().valueToLiteral(valor, CONTEXTO, Locale.ROOT);
        assertThat(literal).isInstanceOf(StringValue.class);
        return ((StringValue) literal).getValue();
    }
}
