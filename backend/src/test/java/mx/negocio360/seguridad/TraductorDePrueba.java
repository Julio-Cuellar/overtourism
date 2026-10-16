package mx.negocio360.seguridad;

import java.util.Map;
import java.util.Optional;
import mx.negocio360.compartido.graphql.ErrorTraducido;
import mx.negocio360.compartido.graphql.TraductorDeErrores;
import org.springframework.stereotype.Component;

/** Solo pruebas: un modulo que declara como se traducen sus errores, igual que lo hacen Clientes e Inventario. */
@Component
class TraductorDePrueba implements TraductorDeErrores {

    @Override
    public Optional<ErrorTraducido> traducir(Throwable error) {
        if (error instanceof ErrorDeNegocioDePrueba) {
            return Optional.of(new ErrorTraducido("DUPLICADO_DE_PRUEBA", "Ya existe ese dato.",
                    Map.of("recursoId", ErrorDeNegocioDePrueba.RECURSO.toString())));
        }
        return Optional.empty();
    }
}
