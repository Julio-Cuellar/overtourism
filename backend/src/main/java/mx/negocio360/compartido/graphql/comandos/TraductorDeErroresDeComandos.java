package mx.negocio360.compartido.graphql.comandos;

import java.util.Map;
import java.util.Optional;
import mx.negocio360.compartido.graphql.ErrorTraducido;
import mx.negocio360.compartido.graphql.TraductorDeErrores;
import org.springframework.stereotype.Component;

/** SG-13: como sale un {@link ComandoGraphQlException}: su codigo y su texto fijo, como peticion mala. */
@Component
public final class TraductorDeErroresDeComandos implements TraductorDeErrores {

    @Override
    public Optional<ErrorTraducido> traducir(Throwable error) {
        if (error instanceof ComandoGraphQlException comando) {
            return Optional.of(new ErrorTraducido(comando.codigo().name(), comando.codigo().mensaje(), Map.of()));
        }
        return Optional.empty();
    }
}
