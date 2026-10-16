package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.infraestructura.ConsumosEnMemoria;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CM-06: la lista de consumos fallidos que ve quien tiene restaurantes.consumo.ver, del negocio de la sesion y del mas nuevo al mas viejo. */
class ConsumosServicioTest {

    private final EntornoDeRecetas e = new EntornoDeRecetas();
    private final ConsumosEnMemoria consumos = new ConsumosEnMemoria();
    private final ConsumosServicio servicio = new ConsumosServicio(consumos);
    private final Autorizacion puede = Autorizacion.de(Set.of(PermisosDeRestaurantes.CONSUMO_VER));

    private ConsumoFallido fallido(UUID negocio, String codigo, java.time.Instant cuando) {
        return new ConsumoFallido(UUID.randomUUID(), negocio, UUID.randomUUID(), codigo,
                List.of(new LineaFallida(UUID.randomUUID(), new java.math.BigDecimal("2"))), cuando, cuando);
    }

    @Test
    @DisplayName("lista los fallos del negocio, del mas nuevo al mas viejo, y no ve los de otro negocio")
    void lista() {
        var t0 = EntornoDeRecetas.AHORA;
        consumos.guardarFallido(fallido(EntornoDeRecetas.NEGOCIO_A, "ARTICULO_INACTIVO", t0));
        consumos.guardarFallido(fallido(EntornoDeRecetas.NEGOCIO_A, "SIN_RECETA", t0.plus(Duration.ofMinutes(5))));
        consumos.guardarFallido(fallido(EntornoDeRecetas.NEGOCIO_B, "SIN_RECETA", t0));

        var pagina = servicio.consumosFallidos(e.ctxA, puede, 20, null);

        assertThat(pagina.nodos()).extracting(ConsumoFallido::codigo).containsExactly("SIN_RECETA", "ARTICULO_INACTIVO");
        assertThat(pagina.siguiente()).isNull();
        assertThat(servicio.consumosFallidos(e.ctxB, puede, 20, null).nodos()).hasSize(1);
    }

    @Test
    @DisplayName("pagina con cursor sin repetir ni saltar, y primeros invalido o cursor desconocido es CONSULTA_INVALIDA")
    void paginas() {
        for (int i = 0; i < 5; i++) {
            consumos.guardarFallido(fallido(EntornoDeRecetas.NEGOCIO_A, "F" + i, EntornoDeRecetas.AHORA.plusSeconds(i)));
        }

        var primera = servicio.consumosFallidos(e.ctxA, puede, 2, null);
        var segunda = servicio.consumosFallidos(e.ctxA, puede, 2, primera.siguiente());
        var tercera = servicio.consumosFallidos(e.ctxA, puede, 2, segunda.siguiente());

        assertThat(primera.nodos()).extracting(ConsumoFallido::codigo).containsExactly("F4", "F3");
        assertThat(segunda.nodos()).extracting(ConsumoFallido::codigo).containsExactly("F2", "F1");
        assertThat(tercera.nodos()).extracting(ConsumoFallido::codigo).containsExactly("F0");
        assertThat(tercera.siguiente()).isNull();
        assertThatThrownBy(() -> servicio.consumosFallidos(e.ctxA, puede, 0, null)).isInstanceOf(RestaurantesException.class)
                .extracting("codigo").isEqualTo(CodigoError.CONSULTA_INVALIDA);
        assertThatThrownBy(() -> servicio.consumosFallidos(e.ctxA, puede, 2, UUID.randomUUID().toString()))
                .isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(CodigoError.CONSULTA_INVALIDA);
        assertThat(servicio.consumosFallidos(e.ctxA, puede, 500, null).nodos()).hasSize(5);
    }

    @Test
    @DisplayName("exige restaurantes.consumo.ver")
    void permiso() {
        assertThatThrownBy(() -> servicio.consumosFallidos(e.ctxA, Autorizacion.de(Set.of()), 20, null))
                .isInstanceOf(PermisoDenegadoException.class);
    }
}
