package mx.negocio360.negocios.web;

import java.util.LinkedHashMap;
import java.util.Optional;
import mx.negocio360.compartido.graphql.ErrorTraducido;
import mx.negocio360.compartido.graphql.TraductorDeErrores;
import mx.negocio360.negocios.dominio.NegociosException;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * SG-13: como sale un {@link NegociosException} por GraphQL (seccion 7.8). El codigo y el texto son los fijos de su
 * {@code CodigoError}; un error que pide algo que falta ({@code PUBLICACION_INCOMPLETA}) dice que falta. Nunca repite lo que
 * escribio la persona ni el mensaje de la excepcion. El tipo sigue el estado que ya usa la API REST de este modulo.
 */
@Component
public final class TraductorDeErroresDeNegocios implements TraductorDeErrores {

    @Override
    public Optional<ErrorTraducido> traducir(Throwable error) {
        if (!(error instanceof NegociosException negocios)) {
            return Optional.empty();
        }
        var extensiones = new LinkedHashMap<String, Object>();
        if (!negocios.faltantes().isEmpty()) {
            extensiones.put("faltantes", negocios.faltantes());
        }
        return Optional.of(new ErrorTraducido(negocios.codigo().name(), negocios.codigo().mensaje(), extensiones,
                tipoDe(negocios)));
    }

    private static ErrorType tipoDe(NegociosException e) {
        return switch (ErroresNegociosAdvice.estadoDe(e.codigo())) {
            case NOT_FOUND -> ErrorType.NOT_FOUND;
            case FORBIDDEN -> ErrorType.FORBIDDEN;
            default -> ErrorType.BAD_REQUEST;
        };
    }
}
