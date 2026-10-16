package mx.negocio360.restaurantes.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

/** SG-13: como sale un error de negocio de Restaurantes por GraphQL: codigo y texto fijos, nunca el mensaje de la excepcion. */
class TraductorDeErroresDeRestaurantesTest {

    private final TraductorDeErroresDeRestaurantes traductor = new TraductorDeErroresDeRestaurantes();

    @Test
    @DisplayName("cada codigo sale con su texto fijo y su propio nombre como codigo")
    void cadaCodigo() {
        for (var codigo : CodigoError.values()) {
            var traducido = traductor.traducir(new RestaurantesException(codigo)).orElseThrow();

            assertThat(traducido.codigo()).isEqualTo(codigo.name());
            assertThat(traducido.mensaje()).isEqualTo(codigo.mensaje()).isNotBlank();
        }
    }

    @Test
    @DisplayName("lo que no existe sale como NOT_FOUND y lo demas como BAD_REQUEST")
    void tipos() {
        for (var noExiste : Set.of(CodigoError.PLATILLO_NO_ENCONTRADO, CodigoError.CATEGORIA_NO_ENCONTRADA,
                CodigoError.RECETA_NO_ENCONTRADA, CodigoError.ARTICULO_NO_ENCONTRADO)) {
            assertThat(traductor.traducir(new RestaurantesException(noExiste)).orElseThrow().clasificacion())
                    .as(noExiste.name()).isEqualTo(ErrorType.NOT_FOUND);
        }
        assertThat(traductor.traducir(new RestaurantesException(CodigoError.PRECIO_INVALIDO)).orElseThrow().clasificacion())
                .isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(traductor.traducir(new RestaurantesException(CodigoError.CONCURRENCIA)).orElseThrow().clasificacion())
                .isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    @DisplayName("un error que no es de Restaurantes no lo traduce el")
    void ajeno() {
        assertThat(traductor.traducir(new IllegalStateException("otro"))).isEmpty();
        assertThat(traductor.traducir(new RuntimeException("otro"))).isEmpty();
    }
}
