package mx.negocio360.caja.graphql;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import mx.negocio360.caja.aplicacion.PlantillasCaja;
import mx.negocio360.caja.dominio.CajaException;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.compartido.graphql.ErrorTraducido;
import mx.negocio360.compartido.graphql.TraductorDeErrores;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * SG-13: como sale un {@link CajaException} por GraphQL. El codigo y el texto son los fijos de {@code PlantillasCaja}; un
 * error reintentable lo dice con {@code reintentable: true} y, si ayuda, lleva el id del recurso. Nunca repite lo que
 * escribio la persona ni el mensaje de la excepcion.
 */
@Component
public final class TraductorDeErroresDeCaja implements TraductorDeErrores {

    @Override
    public Optional<ErrorTraducido> traducir(Throwable error) {
        if (!(error instanceof CajaException caja)) {
            return Optional.empty();
        }
        var codigo = caja.codigo();
        var extensiones = new LinkedHashMap<String, Object>();
        extensiones.put("regla", codigo.regla());
        if (codigo == CodigoError.CONCURRENCIA || codigo == CodigoError.ERROR_TEMPORAL) {
            extensiones.put("reintentable", true);
        }
        caja.recursoId().ifPresent(id -> extensiones.put("recursoId", id.toString()));
        return Optional.of(new ErrorTraducido(codigo.name(), PlantillasCaja.de(codigo), Map.copyOf(extensiones),
                tipoDe(codigo)));
    }

    private static ErrorType tipoDe(CodigoError codigo) {
        return switch (codigo) {
            case CAJA_NO_ENCONTRADA, TURNO_NO_ENCONTRADO -> ErrorType.NOT_FOUND;
            default -> ErrorType.BAD_REQUEST;
        };
    }
}
