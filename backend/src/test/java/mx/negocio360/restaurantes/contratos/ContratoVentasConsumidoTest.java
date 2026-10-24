package mx.negocio360.restaurantes.contratos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.aplicacion.ConsumidorDeVentas;
import mx.negocio360.restaurantes.aplicacion.ResultadoConsumo;
import mx.negocio360.restaurantes.infraestructura.ConsumosEnMemoria;
import mx.negocio360.restaurantes.infraestructura.MenuEnMemoria;
import mx.negocio360.restaurantes.infraestructura.RecetasEnMemoria;
import mx.negocio360.ventas.dominio.EstadoVenta;
import mx.negocio360.ventas.dominio.LineaVenta;
import mx.negocio360.ventas.dominio.MetodoPago;
import mx.negocio360.ventas.dominio.PagoVenta;
import mx.negocio360.ventas.dominio.Venta;
import mx.negocio360.ventas.eventos.VentaAnulada;
import mx.negocio360.ventas.eventos.VentaRegistrada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba de contrato (plan de integracion 10.4): el lector de Restaurantes entiende el evento que Ventas arma de verdad, con
 * {@code VentaRegistrada(...).datos()} y {@code VentaAnulada(...).datos()}, no uno escrito a mano. Si Ventas cambia el
 * contrato sin avisar, esta prueba se rompe.
 */
class ContratoVentasConsumidoTest {

    private static final UUID NEGOCIO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    private static final Instant T0 = Instant.parse("2026-10-16T18:00:00Z");

    private final ConsumosEnMemoria consumos = new ConsumosEnMemoria();
    private final ConsumidorDeVentas consumidor = new ConsumidorDeVentas(new MenuEnMemoria(), new RecetasEnMemoria(),
            new mx.negocio360.restaurantes.infraestructura.InventarioNoDisponible(), consumos, evento -> { },
            java.time.Clock.fixed(T0, java.time.ZoneOffset.UTC));

    private static Venta venta(EstadoVenta estado, UUID producto) {
        var linea = new LineaVenta(1, producto, "Cemita poblana", new BigDecimal("2"), "pieza", new BigDecimal("85.0000"),
                new BigDecimal("170.00"), null);
        var pago = new PagoVenta(MetodoPago.EFECTIVO, new BigDecimal("170.00"), new BigDecimal("200.00"), new BigDecimal("30.00"), null);
        return new Venta(UUID.randomUUID(), NEGOCIO, "A", 1042, estado, UUID.randomUUID(), null, null, new BigDecimal("170.00"),
                new BigDecimal("0.00"), new BigDecimal("170.00"), List.of(linea), List.of(pago), T0, "ana",
                estado == EstadoVenta.ANULADA ? T0 : null, estado == EstadoVenta.ANULADA ? "ana" : null,
                estado == EstadoVenta.ANULADA ? "Error de captura" : null, 1);
    }

    @Test
    @DisplayName("venta.registrada real: el lector la entiende (sin platillos de receta, la ignora sin error ni llamadas)")
    void registradaReal() {
        var venta = venta(EstadoVenta.REGISTRADA, UUID.randomUUID());
        var sobre = new SobreEvento(UuidV7.generar(T0), VentaRegistrada.TIPO, VentaRegistrada.VERSION, NEGOCIO, "ventas", T0,
                venta.id(), null, new VentaRegistrada(venta).datos());

        assertThat(consumidor.procesar(sobre)).isEqualTo(ResultadoConsumo.IGNORADO);
    }

    @Test
    @DisplayName("venta.anulada real: el lector la entiende (sin platillos de receta, la ignora)")
    void anuladaReal() {
        var venta = venta(EstadoVenta.ANULADA, UUID.randomUUID());
        var sobre = new SobreEvento(UuidV7.generar(T0), VentaAnulada.TIPO, VentaAnulada.VERSION, NEGOCIO, "ventas", T0,
                venta.id(), null, new VentaAnulada(venta).datos());

        assertThat(consumidor.procesar(sobre)).isEqualTo(ResultadoConsumo.IGNORADO);
    }

    @Test
    @DisplayName("R-V1: venta.registrada real con referencia y venta.anulada real: el vinculo de cuentas las entiende y paga la cuenta")
    void vinculoConLosEventosReales() {
        var ahora = T0;
        var cuentas = new mx.negocio360.restaurantes.infraestructura.CuentasEnMemoria();
        var salonAlmacen = new mx.negocio360.restaurantes.infraestructura.SalonEnMemoria();
        var reloj = java.time.Clock.fixed(ahora, java.time.ZoneOffset.UTC);
        var salon = new mx.negocio360.restaurantes.aplicacion.SalonServicio(salonAlmacen, cuentas, evento -> { },
                mx.negocio360.restaurantes.aplicacion.GeneradorDeTokens.ALEATORIO, "https://x/", reloj);
        var servicio = new mx.negocio360.restaurantes.aplicacion.CuentasServicio(cuentas, new MenuEnMemoria(), salonAlmacen,
                salon, evento -> { }, reloj);
        var vinculo = new mx.negocio360.restaurantes.aplicacion.VinculoDeCuentasConVentas(cuentas, servicio, salon,
                evento -> { });
        var cuenta = mx.negocio360.restaurantes.dominio.Cuenta.nueva(UuidV7.generar(ahora), NEGOCIO, 1, null,
                mx.negocio360.restaurantes.dominio.OrigenDeCuenta.PERSONAL, 2, "ana", ahora);
        cuentas.insertar(cuenta);
        var producto = UUID.randomUUID();
        var base = venta(EstadoVenta.REGISTRADA, producto);
        var conReferencia = new Venta(base.id(), NEGOCIO, "A", 1042, EstadoVenta.REGISTRADA, base.turnoId(), null, null,
                base.subtotal(), base.descuento(), base.total(), base.lineas(), base.pagos(), T0, "ana", null, null, null, 1,
                cuenta.referencia());

        var registrada = new SobreEvento(UuidV7.generar(T0), VentaRegistrada.TIPO, VentaRegistrada.VERSION, NEGOCIO, "ventas",
                T0, base.id(), null, new VentaRegistrada(conReferencia).datos());
        var anulada = new SobreEvento(UuidV7.generar(T0), VentaAnulada.TIPO, VentaAnulada.VERSION, NEGOCIO, "ventas", T0,
                base.id(), null, new VentaAnulada(conReferencia.anulada(T0, "ana", "Error")).datos());

        assertThat(vinculo.procesar(registrada))
                .isEqualTo(mx.negocio360.restaurantes.aplicacion.VinculoDeCuentasConVentas.Resultado.VINCULADA);
        assertThat(cuentas.cuenta(NEGOCIO, cuenta.id()).orElseThrow().ventaId()).isEqualTo(base.id());
        assertThat(vinculo.procesar(anulada))
                .isEqualTo(mx.negocio360.restaurantes.aplicacion.VinculoDeCuentasConVentas.Resultado.ANULACION_REGISTRADA);
        assertThat(cuentas.cuenta(NEGOCIO, cuenta.id()).orElseThrow().ventaAnulada()).isTrue();
    }

    @Test
    @DisplayName("el contrato real trae los campos que Restaurantes lee: venta_id y lineas con producto_id y cantidad como cadena decimal")
    void camposQueSeLeen() {
        var datos = new VentaRegistrada(venta(EstadoVenta.REGISTRADA, UUID.randomUUID())).datos();

        assertThat(datos).containsKeys("venta_id", "lineas");
        var linea = (java.util.Map<?, ?>) ((List<?>) datos.get("lineas")).get(0);
        assertThat(linea.get("producto_id")).isInstanceOf(String.class);
        assertThat(linea.get("cantidad")).isInstanceOf(String.class);
        var anulada = new VentaAnulada(venta(EstadoVenta.ANULADA, UUID.randomUUID())).datos();
        assertThat(((java.util.Map<?, ?>) ((List<?>) anulada.get("lineas")).get(0)).get("producto_id")).isInstanceOf(String.class);
    }
}
