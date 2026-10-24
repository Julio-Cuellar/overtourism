package mx.negocio360.ventas.graphql;

import java.util.Map;
import java.util.Optional;
import mx.negocio360.compartido.graphql.ErrorTraducido;
import mx.negocio360.compartido.graphql.TraductorDeErrores;
import mx.negocio360.ventas.aplicacion.PlantillasVentas;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.VentasException;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * SG-13: como sale un {@link VentasException} por GraphQL. El codigo y el texto son los fijos de {@code PlantillasVentas};
 * un error reintentable lo dice con {@code reintentable: true} y, si ayuda, lleva el id del recurso. Nunca repite lo que
 * escribio la persona ni el mensaje de la excepcion.
 */
@Component
public final class TraductorDeErroresDeVentas implements TraductorDeErrores {

    @Override
    public Optional<ErrorTraducido> traducir(Throwable error) {
        if (!(error instanceof VentasException ventas)) {
            return Optional.empty();
        }
        var codigo = ventas.codigo();
        var extensiones = new java.util.LinkedHashMap<String, Object>();
        extensiones.put("regla", codigo.regla());
        if (codigo.reintentable()) {
            extensiones.put("reintentable", true);
        }
        ventas.recursoId().ifPresent(id -> extensiones.put("recursoId", id.toString()));
        return Optional.of(new ErrorTraducido(codigo.name(), PlantillasVentas.de(codigo), Map.copyOf(extensiones),
                tipoDe(codigo)));
    }

    private static ErrorType tipoDe(CodigoError codigo) {
        return switch (codigo) {
            case VENTA_NO_ENCONTRADA, PRODUCTO_NO_ENCONTRADO -> ErrorType.NOT_FOUND;
            default -> ErrorType.BAD_REQUEST;
        };
    }
}
