package mx.negocio360.clientes.graphql;

import java.util.LinkedHashMap;
import java.util.Optional;
import mx.negocio360.clientes.aplicacion.PlantillasClientes;
import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.compartido.graphql.ErrorTraducido;
import mx.negocio360.compartido.graphql.TraductorDeErrores;
import org.springframework.stereotype.Component;

/**
 * SG-13: como sale un {@link ClientesException} por GraphQL. El codigo es el del diseno; el texto, el de la plantilla (no
 * el mensaje de la excepcion, que nunca lleva datos personales pero tampoco es para el cliente); el id del recurso
 * involucrado, por ejemplo el cliente que ya tenia ese telefono, sale como extension.
 */
@Component
public final class TraductorDeErroresDeClientes implements TraductorDeErrores {

    @Override
    public Optional<ErrorTraducido> traducir(Throwable error) {
        if (!(error instanceof ClientesException cliente)) {
            return Optional.empty();
        }
        var extensiones = new LinkedHashMap<String, Object>();
        cliente.recursoId().ifPresent(id -> extensiones.put("recursoId", id.toString()));
        if (cliente.codigo() == CodigoError.CONCURRENCIA || cliente.codigo() == CodigoError.ERROR_TEMPORAL) {
            extensiones.put("reintentable", true);
        }
        return Optional.of(new ErrorTraducido(cliente.codigo().name(), PlantillasClientes.de(cliente.codigo()),
                extensiones));
    }
}
