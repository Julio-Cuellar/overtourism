package mx.negocio360.restaurantes.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.aplicacion.ResultadoConsumo;
import mx.negocio360.restaurantes.aplicacion.SeguimientoDeNegocios;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Con Ventas (u otro modulo) en memoria, el evento se publica con {@code ApplicationEventPublisher} sin ninguna transaccion.
 * Una escucha {@code @ApplicationModuleListener} pura solo recibe eventos publicados dentro de una transaccion y descartaba
 * ese en silencio: la venta no descontaba insumos en el perfil {@code desarrollo}. Con el Spring real de los
 * oyentes transaccionales (hace falta {@code @EnableTransactionManagement}), las escuchas de Restaurantes tambien reciben
 * lo publicado fuera de una transaccion.
 */
class EscuchasSinTransaccionTest {

    private static final UUID NEGOCIO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    private static final Instant T0 = Instant.parse("2026-10-16T10:00:00Z");

    @Configuration
    @EnableTransactionManagement
    static class Transacciones {
        @Bean
        PlatformTransactionManager transacciones() {
            var gestor = mock(PlatformTransactionManager.class);
            when(gestor.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
            return gestor;
        }
    }

    private static final RestaurantesEnMemoria RESTAURANTES = new RestaurantesEnMemoria();
    private static final AtomicInteger VENTAS = new AtomicInteger();

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(Transacciones.class)
            .withBean(EscuchaDeVentas.class, () -> new EscuchaDeVentas(evento -> {
                VENTAS.incrementAndGet();
                return ResultadoConsumo.APLICADO;
            }))
            .withBean(EscuchaDeNegocios.class, () -> new EscuchaDeNegocios(new SeguimientoDeNegocios(RESTAURANTES)));

    private static SobreEvento evento(String tipo, String origen, Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(T0), tipo, 1, NEGOCIO, origen, T0, NEGOCIO, null, datos);
    }

    @Test
    @DisplayName("la escucha de ventas recibe venta.registrada publicada sin transaccion")
    void ventasSinTransaccion() {
        VENTAS.set(0);
        contexto.run(c -> {
            c.publishEvent(evento("venta.registrada", "ventas", Map.of("venta_id", UUID.randomUUID().toString())));

            assertThat(VENTAS.get()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("la escucha de negocios recibe negocio.registrado publicado sin transaccion")
    void negociosSinTransaccion() {
        contexto.run(c -> {
            c.publishEvent(evento("negocio.registrado", "negocios", Map.of("giro", "RESTAURANTE")));

            assertThat(RESTAURANTES.porNegocio(NEGOCIO)).isPresent();
        });
    }
}
