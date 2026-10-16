package mx.negocio360.restaurantes.web;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mx.negocio360.compartido.graphql.ErrorTraducido;
import mx.negocio360.compartido.graphql.TraductorDeErrores;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * SG-13: como sale un {@link RestaurantesException} por GraphQL. El codigo y el texto son los fijos de {@link CodigoError};
 * nunca el mensaje de la excepcion ni lo que escribio la persona. Lo que no existe sale como NOT_FOUND y lo demas como
 * BAD_REQUEST.
 */
@Component
public final class TraductorDeErroresDeRestaurantes implements TraductorDeErrores {

    private static final Set<CodigoError> NO_ENCONTRADOS = Set.of(CodigoError.PLATILLO_NO_ENCONTRADO,
            CodigoError.CATEGORIA_NO_ENCONTRADA, CodigoError.RECETA_NO_ENCONTRADA, CodigoError.ARTICULO_NO_ENCONTRADO);

    @Override
    public Optional<ErrorTraducido> traducir(Throwable error) {
        if (!(error instanceof RestaurantesException restaurantes)) {
            return Optional.empty();
        }
        var codigo = restaurantes.codigo();
        return Optional.of(new ErrorTraducido(codigo.name(), codigo.mensaje(), Map.of(),
                NO_ENCONTRADOS.contains(codigo) ? ErrorType.NOT_FOUND : ErrorType.BAD_REQUEST));
    }
}
