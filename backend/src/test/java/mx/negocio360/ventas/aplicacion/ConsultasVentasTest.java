package mx.negocio360.ventas.aplicacion;

import static mx.negocio360.ventas.fakes.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;
import mx.negocio360.compartido.pruebas.Permisos;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.ventas.FiltroVentas;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.EstadoVenta;
import mx.negocio360.ventas.fakes.Escenario;
import mx.negocio360.ventas.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** VF-01 a VF-03 y G1: consultas con permiso, filtros y cursor (D-126, D-131), aisladas por negocio. */
class ConsultasVentasTest {

    private final Escenario e = new Escenario();

    private void vender(int veces) {
        for (var i = 0; i < veces; i++) {
            e.vender(e.ctxA, Fixtures.ventaSinCupon());
            e.reloj.avanzar(Duration.ofMinutes(1));
        }
    }

    @Test
    @DisplayName("ventas lista de la mas reciente a la mas antigua y pagina con cursor sin repetir ni saltar")
    void ventas_paginaConCursor() {
        vender(5);

        var primera = e.api.ventas(e.ctxA, Permisos.TODOS, null, 2, null);
        var segunda = e.api.ventas(e.ctxA, Permisos.TODOS, null, 2, primera.cursorFinal());
        var tercera = e.api.ventas(e.ctxA, Permisos.TODOS, null, 2, segunda.cursorFinal());

        assertThat(primera.elementos()).extracting(v -> v.folio()).containsExactly(5L, 4L);
        assertThat(segunda.elementos()).extracting(v -> v.folio()).containsExactly(3L, 2L);
        assertThat(tercera.elementos()).extracting(v -> v.folio()).containsExactly(1L);
        assertThat(primera.hayMas()).isTrue();
        assertThat(tercera.hayMas()).isFalse();
        assertThat(tercera.cursorFinal()).isNull();
    }

    @Test
    @DisplayName("ventas solo devuelve las del negocio de la sesion")
    void ventas_aislaPorNegocio() {
        vender(2);
        e.vender(e.ctxB, Fixtures.ventaSinCupon());

        assertThat(e.api.ventas(e.ctxA, Permisos.TODOS, null, 20, null).elementos()).hasSize(2);
        assertThat(e.api.ventas(e.ctxB, Permisos.TODOS, null, 20, null).elementos()).hasSize(1);
    }

    @Test
    @DisplayName("el filtro por estado, folio y turno acota la lista")
    void ventas_filtros() {
        vender(3);
        var anulada = e.api.ventas(e.ctxA, Permisos.TODOS, null, 20, null).elementos().get(1).id();
        e.confirmar(new mx.negocio360.compartido.contexto.ContextoNegocio(e.negocioA, "encargado-a"),
                new AnularVentaComando(anulada, "Error"));

        assertThat(e.api.ventas(e.ctxA, Permisos.TODOS, new FiltroVentas(null, EstadoVenta.ANULADA, null, null, null,
                null), 20, null).elementos()).extracting(v -> v.id()).containsExactly(anulada);
        assertThat(e.api.ventas(e.ctxA, Permisos.TODOS, new FiltroVentas(null, null, null, null, "A", 3L), 20, null)
                .elementos()).singleElement().satisfies(v -> assertThat(v.folio()).isEqualTo(3L));
        assertThat(e.api.ventas(e.ctxA, Permisos.TODOS, new FiltroVentas(UUID.randomUUID(), null, null, null, null,
                null), 20, null).elementos()).isEmpty();
    }

    @Test
    @DisplayName("un tamano de pagina fuera de 1 a 100 o un cursor ajeno falla con PAGINACION_INVALIDA")
    void ventas_paginacionInvalida() {
        fallaCon(() -> e.api.ventas(e.ctxA, Permisos.TODOS, null, 0, null), CodigoError.PAGINACION_INVALIDA);
        fallaCon(() -> e.api.ventas(e.ctxA, Permisos.TODOS, null, 101, null), CodigoError.PAGINACION_INVALIDA);
        fallaCon(() -> e.api.ventas(e.ctxA, Permisos.TODOS, null, 20, "inventado"), CodigoError.PAGINACION_INVALIDA);
    }

    @Test
    @DisplayName("un rango de mas de 92 dias, al reves, un folio menor que 1 o una serie larga falla con FILTRO_INVALIDO")
    void ventas_filtroInvalido() {
        var ahora = e.reloj.instant();

        fallaCon(() -> e.api.ventas(e.ctxA, Permisos.TODOS, new FiltroVentas(null, null, ahora.minus(Duration.ofDays(93)),
                ahora, null, null), 20, null), CodigoError.FILTRO_INVALIDO);
        fallaCon(() -> e.api.ventas(e.ctxA, Permisos.TODOS, new FiltroVentas(null, null, ahora, ahora.minusSeconds(1),
                null, null), 20, null), CodigoError.FILTRO_INVALIDO);
        fallaCon(() -> e.api.ventas(e.ctxA, Permisos.TODOS, new FiltroVentas(null, null, null, null, null, 0L), 20,
                null), CodigoError.FILTRO_INVALIDO);
        fallaCon(() -> e.api.ventas(e.ctxA, Permisos.TODOS, new FiltroVentas(null, null, null, null, "x".repeat(61),
                null), 20, null), CodigoError.FILTRO_INVALIDO);
    }

    @Test
    @DisplayName("D-126: venta, ventas y resumenTurno piden ventas.venta.ver; sin el permiso fallan con PERMISO_DENEGADO")
    void consultas_sinPermisoVer_permisoDenegado() {
        vender(1);
        var sinVer = Permisos.todosMenos("ventas.venta.ver");

        assertThatThrownBy(() -> e.api.venta(e.ctxA, sinVer, UUID.randomUUID())).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.api.ventas(e.ctxA, sinVer, null, 20, null)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.api.resumenTurno(e.ctxA, sinVer, Fixtures.TURNO))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThat(e.api.resumenTurno(e.ctxA, Permisos.CAJERO, Fixtures.TURNO).ventasRegistradas()).isEqualTo(1);
    }

    @Test
    @DisplayName("G1: productosParaVenta busca activos del negocio, pide ventas.venta.registrar y acota texto y cantidad")
    void productosParaVenta() {
        assertThat(e.api.productosParaVenta(e.ctxA, Permisos.CAJERO, "camo", 10)).extracting(p -> p.productoId())
                .containsExactly(Fixtures.CAMOTE);
        assertThat(e.api.productosParaVenta(e.ctxA, Permisos.CAJERO, null, 50)).extracting(p -> p.productoId())
                .containsExactlyInAnyOrder(Fixtures.BOLETO, Fixtures.CAMOTE);
        assertThat(e.api.productosParaVenta(e.ctxA, Permisos.CAJERO, "descontinuado", 10)).isEmpty();
        assertThatThrownBy(() -> e.api.productosParaVenta(e.ctxA, Permisos.todosMenos("ventas.venta.registrar"), null, 5))
                .isInstanceOf(PermisoDenegadoException.class);
        fallaCon(() -> e.api.productosParaVenta(e.ctxA, Permisos.CAJERO, null, 51), CodigoError.PAGINACION_INVALIDA);
        fallaCon(() -> e.api.productosParaVenta(e.ctxA, Permisos.CAJERO, "x".repeat(81), 5),
                CodigoError.TEXTO_DEMASIADO_LARGO);
    }
}
