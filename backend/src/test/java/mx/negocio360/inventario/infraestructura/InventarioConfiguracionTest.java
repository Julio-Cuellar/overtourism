package mx.negocio360.inventario.infraestructura;

import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.CrearArticuloComando;
import mx.negocio360.inventario.aplicacion.EstadoDeConsumo;
import mx.negocio360.inventario.aplicacion.LineaDeConsumo;
import mx.negocio360.inventario.aplicacion.RegistrarEntradaComando;
import mx.negocio360.inventario.eventos.MovimientoRegistrado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.event.EventListener;

/**
 * Inventario se enciende solo con {@code inventario.almacen=memoria}, igual que Restaurantes: sin base de datos los
 * saldos se pierden al reiniciar, asi que no se enciende por accidente. La version con PostgreSQL llega despues.
 */
class InventarioConfiguracionTest {

    /** Recoge los eventos del bus de Spring para comprobar que Inventario publica ahi. */
    static final class Escucha {
        final List<SobreEvento> recibidos = new java.util.concurrent.CopyOnWriteArrayList<>();

        @EventListener
        void alRecibir(SobreEvento evento) {
            recibidos.add(evento);
        }
    }

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(InventarioEnMemoriaConfiguracion.class).withBean(Clock.class, Clock::systemUTC)
            .withBean(Escucha.class);

    @Test
    @DisplayName("con inventario.almacen=memoria expone InventarioApi y funciona de punta a punta, publicando en el bus")
    void conAlmacenEnMemoria() {
        contexto.withPropertyValues("inventario.almacen=memoria").run(c -> {
            var api = c.getBean(InventarioApi.class);
            var ctx = new ContextoNegocio(UUID.randomUUID(), "demo");
            var harina = api.confirmar(ctx, api.preparar(ctx, new CrearArticuloComando("Harina", null, "g", null, null,
                    null)).token()).recursoId();
            api.confirmar(ctx, api.preparar(ctx, new RegistrarEntradaComando(harina, d("2"), "kg", d("20"), null))
                    .token());

            var consumo = api.registrarConsumo(ctx, "restaurantes", UUID.randomUUID(), null,
                    List.of(new LineaDeConsumo(harina, d("500"), "g")));

            assertThat(consumo.estado()).isEqualTo(EstadoDeConsumo.REGISTRADO);
            assertThat(api.articulo(ctx, harina).orElseThrow().cantidadBase()).isEqualByComparingTo("1500");
            assertThat(c.getBean(Escucha.class).recibidos).extracting(SobreEvento::tipo)
                    .contains(MovimientoRegistrado.TIPO);
        });
    }

    @Test
    @DisplayName("sin la propiedad no se enciende nada")
    void sinAlmacen() {
        contexto.run(c -> assertThat(c).doesNotHaveBean(InventarioApi.class));
    }
}
