package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;
import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 8.4 del diseno: como se leen los eventos de Restaurantes para las suscripciones. Lo que llega por la suscripcion es lo
 * que dice el evento, sin consultar nada, y lo que no es de Restaurantes o de una version desconocida se ignora.
 */
class AvisosEnVivoTest {

    private final EntornoDeCuentas e = new EntornoDeCuentas().armado();

    private static SobreEvento ajeno(String tipo) {
        var ahora = EntornoDeCuentas.AHORA;
        return new SobreEvento(UuidV7.generar(ahora), tipo, 1, EntornoDeCuentas.NEGOCIO_A, "otro-modulo", ahora,
                UuidV7.generar(ahora), null, Map.of("comanda_id", UUID.randomUUID().toString()));
    }

    private UUID cuentaConCemita() {
        var cuenta = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 2);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 2, "sin cebolla", true);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.horchata, 1, null, false);
        e.eventos.clear();
        return cuenta.id();
    }

    @Test
    @DisplayName("una comanda enviada llega como aviso ENVIADA, NUEVA, con la mesa, la cuenta y las lineas con su nota y alergia")
    void comandaEnviada() {
        var cuenta = cuentaConCemita();
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta);

        var avisos = e.eventosDe("restaurantes.comanda_enviada").stream().map(AvisoDeComanda::de).flatMap(java.util.Optional::stream)
                .toList();

        assertThat(avisos).hasSize(2);
        var cocina = avisos.stream().filter(a -> a.estacion() == Estacion.COCINA).findFirst().orElseThrow();
        assertThat(cocina.tipo()).isEqualTo(AvisoDeComanda.Tipo.ENVIADA);
        assertThat(cocina.estado()).isEqualTo(EstadoDeComanda.NUEVA);
        assertThat(cocina.mesa()).isEqualTo("Mesa 1");
        assertThat(cocina.numeroDeCuenta()).isEqualTo(1);
        assertThat(cocina.lineas()).containsExactly(new AvisoDeComanda.Linea("Cemita poblana", 2, "sin cebolla", true));
        assertThat(cocina.lineasAnuladas()).isEmpty();
    }

    @Test
    @DisplayName("una comanda actualizada llega como ACTUALIZADA con su estado nuevo y las lineas anuladas")
    void comandaActualizada() {
        var cuenta = cuentaConCemita();
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta);
        var comanda = e.cocina.comandas(e.ctxA, e.cocinero, Estacion.COCINA, null, 5).get(0).id();
        e.eventos.clear();
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, comanda);
        var linea = e.servicio.cuenta(e.ctxA, e.mesero, cuenta).lineas().get(0).id();
        e.servicio.anularLinea(e.ctxA, e.mesero, linea, MotivoDeAnulacion.AGOTADO, null);

        var avisos = e.eventosDe("restaurantes.comanda_actualizada").stream().map(AvisoDeComanda::de)
                .flatMap(java.util.Optional::stream).toList();

        assertThat(avisos).hasSize(2);
        assertThat(avisos.get(0).tipo()).isEqualTo(AvisoDeComanda.Tipo.ACTUALIZADA);
        assertThat(avisos.get(0).estado()).isEqualTo(EstadoDeComanda.EN_PREPARACION);
        assertThat(avisos.get(0).comandaId()).isEqualTo(comanda);
        assertThat(avisos.get(0).lineas()).isEmpty();
        assertThat(avisos.get(1).lineasAnuladas()).containsExactly(linea);
    }

    @Test
    @DisplayName("los eventos que no son de comanda, de otro modulo o de otra version no producen aviso de comanda")
    void comandaIgnora() {
        var cuenta = cuentaConCemita();
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta);
        var enviada = e.eventosDe("restaurantes.comanda_enviada").get(0);
        var deOtraVersion = new SobreEvento(enviada.id(), enviada.tipo(), 2, enviada.negocioId(), enviada.origen(),
                enviada.ocurridoEn(), enviada.correlacionId(), null, enviada.datos());

        assertThat(AvisoDeComanda.de(ajeno("restaurantes.comanda_enviada"))).isEmpty();
        assertThat(AvisoDeComanda.de(deOtraVersion)).isEmpty();
        e.servicio.pedirCuenta(e.ctxA, e.mesero, cuenta);
        assertThat(AvisoDeComanda.de(e.eventosDe("restaurantes.cuenta_solicitada").get(0))).isEmpty();
        assertThat(AvisoDeComanda.de(ajeno("negocio.disponibilidad_cambiada"))).isEmpty();
    }

    @Test
    @DisplayName("el pedido por QR, la cuenta pedida, la pausa del QR y los cambios de disponibilidad llegan al salon")
    void salon() {
        e.qr.pedirPorQr(e.token(e.mesa1), "11111111-1111-4111-8111-111111111111",
                List.of(new LineaDePedidoQr(e.cemita, 2, null, false)), "llave-salon-1");
        e.qr.pedirCuentaPorQr(e.token(e.mesa1), "11111111-1111-4111-8111-111111111111");
        var avisos = e.eventos.stream().map(AvisoDelSalon::de).flatMap(java.util.Optional::stream).toList();

        assertThat(avisos).extracting(AvisoDelSalon::tipo).containsExactly(AvisoDelSalon.Tipo.PEDIDO_QR,
                AvisoDelSalon.Tipo.DISPONIBILIDAD, AvisoDelSalon.Tipo.CUENTA_SOLICITADA);
        var pedido = avisos.get(0);
        assertThat(pedido.mesa()).isEqualTo("Mesa 1");
        assertThat(pedido.lineas()).isEqualTo(1);
        assertThat(pedido.abrioLaCuenta()).isTrue();
        assertThat(pedido.cuentaId()).isNotNull();
        var disponibilidad = avisos.get(1);
        assertThat(disponibilidad.libres()).isEqualTo(1);
        assertThat(disponibilidad.total()).isEqualTo(2);
        assertThat(disponibilidad.abierto()).isTrue();
        assertThat(avisos.get(2).solicitadaPor()).isEqualTo("QR");
    }

    @Test
    @DisplayName("QR-17: la pausa del QR llega al salon con la mesa y hasta cuando")
    void salonPausa() {
        for (var i = 0; i < 3; i++) {
            e.qr.pedirPorQr(e.token(e.mesa1), "11111111-1111-4111-8111-11111111111" + i,
                    List.of(new LineaDePedidoQr(e.horchata, 1, null, false)), "llave-pausa-" + i);
            e.servicio.cancelarCuenta(e.ctxA, e.mesero, e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0).id(),
                    MotivoDeAnulacion.SIN_COMENSALES, null);
        }

        var pausa = e.eventosDe("restaurantes.qr_pausado").stream().map(AvisoDelSalon::de)
                .flatMap(java.util.Optional::stream).findFirst().orElseThrow();

        assertThat(pausa.tipo()).isEqualTo(AvisoDelSalon.Tipo.QR_PAUSADO);
        assertThat(pausa.mesa()).isEqualTo("Mesa 1");
        assertThat(pausa.hasta()).isNotNull();
    }

    @Test
    @DisplayName("CT-08: la cuenta pagada con una venta distinta llega al salon como DISCREPANCIA con la mesa y la cuenta")
    void salonDiscrepancia() {
        var cuenta = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 2);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 2, null, false);
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        e.servicio.pedirCuenta(e.ctxA, e.mesero, cuenta.id());
        e.eventos.clear();
        var ahora = e.reloj.instant();
        var venta = UUID.randomUUID();
        e.vinculo.procesar(new SobreEvento(UuidV7.generar(ahora), "venta.registrada", 1, EntornoDeCuentas.NEGOCIO_A,
                "ventas", ahora, venta, null, Map.of("venta_id", venta.toString(), "referencia", "restaurantes:cuenta:" + cuenta.id(),
                "lineas", List.of(Map.of("linea", 1, "producto_id", e.cemita.toString(), "cantidad", "1")))));

        var aviso = e.eventosDe("restaurantes.cuenta_con_discrepancia").stream().map(AvisoDelSalon::de)
                .flatMap(java.util.Optional::stream).findFirst().orElseThrow();

        assertThat(aviso.tipo()).isEqualTo(AvisoDelSalon.Tipo.DISCREPANCIA);
        assertThat(aviso.mesa()).isEqualTo("Mesa 1");
        assertThat(aviso.cuentaId()).isEqualTo(cuenta.id());
    }

    @Test
    @DisplayName("la disponibilidad de otro tipo o de otro modulo y los eventos ajenos no producen aviso del salon")
    void salonIgnora() {
        var ahora = EntornoDeCuentas.AHORA;
        var deMuseo = new SobreEvento(UuidV7.generar(ahora), "negocio.disponibilidad_cambiada", 1,
                EntornoDeCuentas.NEGOCIO_A, "restaurantes", ahora, UuidV7.generar(ahora), null,
                Map.of("tipo", "BOLETOS", "libres", 3, "total", 10, "abierto", true));

        assertThat(AvisoDelSalon.de(deMuseo)).isEmpty();
        assertThat(AvisoDelSalon.de(ajeno("restaurantes.pedido_qr_recibido"))).isEmpty();
        assertThat(AvisoDelSalon.de(ajeno("negocio.disponibilidad_cambiada"))).isEmpty();
    }
}
