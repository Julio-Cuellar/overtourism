package mx.negocio360.canal.web;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import mx.negocio360.caja.aplicacion.PlantillasCaja;
import mx.negocio360.caja.dominio.CajaException;
import mx.negocio360.ventas.aplicacion.PlantillasVentas;
import mx.negocio360.ventas.dominio.VentasException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;

/**
 * Traduce los fallos de dominio a un error de GraphQL con el texto fijo de su modulo (el modelo nunca improvisa un
 * error) y, en {@code extensions}, el {@code codigo} y la {@code regla} que lo produjo (D-83). Cualquier otra excepcion
 * la deja pasar: Spring responde un error interno sin detalles.
 */
public final class ErroresDeCanal extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable error, DataFetchingEnvironment entorno) {
        if (error instanceof VentasException ventas) {
            return construir(entorno, ErrorType.BAD_REQUEST, PlantillasVentas.de(ventas.codigo()),
                    ventas.codigo().name(), ventas.codigo().regla());
        }
        if (error instanceof CajaException caja) {
            return construir(entorno, ErrorType.BAD_REQUEST, PlantillasCaja.de(caja.codigo()), caja.codigo().name(),
                    caja.codigo().regla());
        }
        if (error instanceof CanalException canal) {
            var tipo = canal.codigo() == CanalException.Codigo.NO_AUTENTICADO ? ErrorType.UNAUTHORIZED
                    : ErrorType.BAD_REQUEST;
            return construir(entorno, tipo, canal.codigo().mensaje(), canal.codigo().name(), "F4");
        }
        return null;
    }

    private static GraphQLError construir(DataFetchingEnvironment entorno, ErrorType tipo, String mensaje,
                                          String codigo, String regla) {
        return GraphqlErrorBuilder.newError(entorno).errorType(tipo).message(mensaje)
                .extensions(Map.of("codigo", codigo, "regla", regla)).build();
    }
}
