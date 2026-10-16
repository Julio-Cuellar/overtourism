package mx.negocio360.inventario.graphql;

import java.util.LinkedHashMap;
import java.util.Optional;
import mx.negocio360.compartido.graphql.ErrorTraducido;
import mx.negocio360.compartido.graphql.TraductorDeErrores;
import mx.negocio360.inventario.aplicacion.PlantillasInventario;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.InventarioException;
import org.springframework.stereotype.Component;

/** SG-13: como sale un {@link InventarioException} por GraphQL, con el codigo y el texto de su plantilla. */
@Component
public final class TraductorDeErroresDeInventario implements TraductorDeErrores {

    @Override
    public Optional<ErrorTraducido> traducir(Throwable error) {
        if (!(error instanceof InventarioException inventario)) {
            return Optional.empty();
        }
        var extensiones = new LinkedHashMap<String, Object>();
        inventario.recursoId().ifPresent(id -> extensiones.put("recursoId", id.toString()));
        if (inventario.codigo() == CodigoError.CONCURRENCIA || inventario.codigo() == CodigoError.ERROR_TEMPORAL) {
            extensiones.put("reintentable", true);
        }
        return Optional.of(new ErrorTraducido(inventario.codigo().name(), PlantillasInventario.de(inventario.codigo()),
                extensiones));
    }
}
