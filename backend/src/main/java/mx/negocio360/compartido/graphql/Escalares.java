package mx.negocio360.compartido.graphql;

import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/** Escalares propios del esquema: Decimal, Date y DateTime. */
@Configuration(proxyBeanMethods = false)
public class Escalares {

    public static final GraphQLScalarType DECIMAL = GraphQLScalarType.newScalar()
            .name("Decimal")
            .description("Decimal exacto como cadena, por ejemplo \"207.00\". Nunca punto flotante.")
            .coercing(new DecimalCoercing())
            .build();

    public static final GraphQLScalarType FECHA = GraphQLScalarType.newScalar()
            .name("Date")
            .description("Fecha ISO-8601 sin hora, por ejemplo 2026-10-05.")
            .coercing(new FechaCoercing())
            .build();

    public static final GraphQLScalarType FECHA_HORA = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("Fecha y hora ISO-8601 en UTC, por ejemplo 2026-10-05T18:22:41Z.")
            .coercing(new FechaHoraCoercing())
            .build();

    @Bean
    RuntimeWiringConfigurer registrarEscalares() {
        return constructor -> constructor.scalar(DECIMAL).scalar(FECHA).scalar(FECHA_HORA);
    }
}
