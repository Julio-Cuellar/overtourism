package mx.negocio360.restaurantes.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.aplicacion.ConsumidorDeVentas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D-122: si Inventario no respondio, la escucha lanza para que el evento no se de por entregado y vuelva a llegar; lo demas
 * (datos malos, eventos ajenos, fallos permanentes) no lanza, porque repetirlo no lo arreglaria. El mensaje del error lleva el
 * tipo y el id del evento y nunca sus datos.
 */
class EscuchaDeVentasTest {

    private static final UUID NEGOCIO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    private static final Instant T0 = Instant.parse("2026-10-16T18:00:00Z");

    private final Clock reloj = Clock.fixed(T0, ZoneOffset.UTC);
    private final EscuchaDeVentas escucha = new EscuchaDeVentas(new ConsumidorDeVentas(new MenuEnMemoria(), new RecetasEnMemoria(),
            new InventarioNoDisponible(), new ConsumosEnMemoria(), evento -> { }, reloj));

    private static SobreEvento evento(String tipo, String origen, Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(T0), tipo, 1, NEGOCIO, origen, T0, NEGOCIO, null, datos);
    }

    @Test
    @DisplayName("un evento que no es de venta o esta mal formado no lanza")
    void sinError() {
        assertThatCode(() -> escucha.alRecibir(evento("caja.turno_abierto", "caja", Map.of("turno_id", "x")))).doesNotThrowAnyException();
        assertThatCode(() -> escucha.alRecibir(evento("venta.registrada", "ventas", Map.of("falta", "todo")))).doesNotThrowAnyException();
        assertThatCode(() -> escucha.alRecibir(evento("venta.registrada", "ventas", Map.of("venta_id", UUID.randomUUID().toString(),
                "lineas", java.util.List.of())))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("un resultado reintentable lanza con el tipo y el id del evento, sin sus datos, para que Modulith lo reintente")
    void reintentableLanza() {
        var siempreReintentable = new EscuchaDeVentas(evento -> mx.negocio360.restaurantes.aplicacion.ResultadoConsumo.REINTENTABLE);
        var venta = evento("venta.registrada", "ventas", Map.of("venta_id", "dato-sensible"));

        assertThatThrownBy(() -> siempreReintentable.alRecibir(venta)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("venta.registrada").hasMessageContaining(venta.id().toString())
                .hasMessageNotContaining("dato-sensible");
    }
}
