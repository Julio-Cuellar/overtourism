package mx.negocio360.caja.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.caja.fakes.Escenario;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.ApplicationModuleListener;

/**
 * Las dos escuchas de Caja: la de memoria (sincrona, con {@code @EventListener}) y la de PostgreSQL (con el outbox y
 * {@code @ApplicationModuleListener}, F5). Ambas pasan el evento al mismo consumidor; la del outbox ademas deja
 * pendiente en el outbox lo que Caja todavia no pudo aplicar, para que se reintente en lugar de perderse.
 */
class EscuchasDeVentasTest {

    private final Escenario e = new Escenario();
    private final UUID turno = e.abrirTurno(e.ctxA, "500.00");
    private final EscuchaDeVentasEnOutbox outbox = new EscuchaDeVentasEnOutbox(e.consumidor);
    private final EscuchaDeVentas memoria = new EscuchaDeVentas(e.consumidor);

    private SobreEvento ventaDeUnTurnoQueNoExiste() {
        return e.ventaRegistrada(e.negocioA, UUID.randomUUID(), UUID.randomUUID(), Map.of("EFECTIVO", "10.00"));
    }

    /** Un evento que dice ser de Ventas pero llega de otro modulo: Caja no se fia de el. */
    private SobreEvento ventaDeOtroOrigen() {
        var legitimo = e.ventaRegistrada(e.negocioA, turno, UUID.randomUUID(), Map.of("EFECTIVO", "10.00"));
        return new SobreEvento(legitimo.id(), legitimo.tipo(), legitimo.version(), legitimo.negocioId(), "otro",
                legitimo.ocurridoEn(), legitimo.correlacionId(), null, legitimo.datos());
    }

    @Test
    @DisplayName("la escucha del outbox es un @ApplicationModuleListener y la de memoria un @EventListener")
    void anotaciones() throws Exception {
        Method deOutbox = EscuchaDeVentasEnOutbox.class.getMethod("alRecibir", SobreEvento.class);
        Method deMemoria = EscuchaDeVentas.class.getMethod("alPublicarse", SobreEvento.class);

        assertThat(deOutbox.isAnnotationPresent(ApplicationModuleListener.class)).isTrue();
        assertThat(deMemoria.isAnnotationPresent(EventListener.class)).isTrue();
        assertThat(deMemoria.isAnnotationPresent(ApplicationModuleListener.class)).isFalse();
    }

    @Test
    @DisplayName("F5: un evento que se aplica no lanza nada y su efecto queda en Caja")
    void aplicado_noLanza() {
        var evento = e.ventaRegistrada(e.negocioA, turno, UUID.randomUUID(), Map.of("EFECTIVO", "10.00"));

        assertThatCode(() -> outbox.alRecibir(evento)).doesNotThrowAnyException();

        assertThat(e.libro.todos()).hasSize(2);
    }

    @Test
    @DisplayName("F5: sin turno abierto la escucha del outbox lanza, para que el evento quede pendiente y se reintente")
    void sinTurnoAbierto_quedaPendiente() {
        var evento = ventaDeUnTurnoQueNoExiste();

        assertThatThrownBy(() -> outbox.alRecibir(evento)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(evento.id().toString()).hasMessageContaining("venta.registrada")
                .hasMessageNotContaining("10.00");
        assertThat(e.libro.todos()).hasSize(1);
    }

    @Test
    @DisplayName("F5: un evento que no cumple el contrato no se reintenta: reintentarlo no lo arreglaria")
    void datosInvalidos_noLanza() {
        assertThatCode(() -> outbox.alRecibir(ventaDeOtroOrigen())).doesNotThrowAnyException();
        assertThat(e.libro.todos()).hasSize(1);
    }

    @Test
    @DisplayName("F5: un evento de otro tipo se ignora")
    void otroTipo_seIgnora() {
        var otro = new SobreEvento(UuidV7.generar(e.reloj.instant()), "cliente.registrado", 1, e.negocioA, "clientes",
                e.reloj.instant(), UUID.randomUUID(), null, Map.of("cliente_id", UUID.randomUUID().toString()));

        assertThatCode(() -> outbox.alRecibir(otro)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("la escucha de memoria nunca lanza: el bus en proceso es sincrono y no reintenta")
    void memoria_nuncaLanza() {
        assertThatCode(() -> memoria.alPublicarse(ventaDeUnTurnoQueNoExiste())).doesNotThrowAnyException();
        assertThatCode(() -> memoria.alPublicarse(ventaDeOtroOrigen())).doesNotThrowAnyException();
    }
}
