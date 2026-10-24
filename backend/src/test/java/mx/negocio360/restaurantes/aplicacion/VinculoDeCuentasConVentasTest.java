package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.aplicacion.VinculoDeCuentasConVentas.Resultado;
import mx.negocio360.restaurantes.dominio.EstadoDeCuenta;
import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CT-08 a CT-10: cuando Ventas registra una venta con la referencia de una cuenta, la cuenta pasa a pagada con su venta y la
 * mesa se libera; si la venta anula, la cuenta lo recuerda. Es un lector tolerante de eventos con entrega al menos una vez:
 * repetir un evento no cambia nada, y lo que no es de este negocio o de esta cuenta se ignora.
 */
class VinculoDeCuentasConVentasTest {

    private final EntornoDeCuentas e = new EntornoDeCuentas().armado();

    /** Una cuenta por cobrar de la mesa 1 con 2 cemitas y 1 horchata ($205.00). */
    private UUID cuentaPorCobrar() {
        var cuenta = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 2);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.cemita, 2, null, false);
        e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.horchata, 1, null, false);
        e.servicio.enviarACocina(e.ctxA, e.mesero, cuenta.id());
        e.servicio.pedirCuenta(e.ctxA, e.mesero, cuenta.id());
        e.eventos.clear();
        return cuenta.id();
    }

    private static String referencia(UUID cuentaId) {
        return "restaurantes:cuenta:" + cuentaId;
    }

    private SobreEvento registrada(UUID negocio, UUID ventaId, String referencia, Map<UUID, String> lineas) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("venta_id", ventaId.toString());
        datos.put("folio", 1);
        if (referencia != null) {
            datos.put("referencia", referencia);
        }
        datos.put("total", "205.00");
        var filas = new ArrayList<Map<String, Object>>();
        var n = 1;
        for (var linea : lineas.entrySet()) {
            filas.add(Map.of("linea", n++, "producto_id", linea.getKey().toString(), "cantidad", linea.getValue(),
                    "unidad", "pieza", "precio_unitario", "10.00", "importe", "10.00"));
        }
        datos.put("lineas", filas);
        var ahora = e.reloj.instant();
        return new SobreEvento(UuidV7.generar(ahora), "venta.registrada", 1, negocio, "ventas", ahora, ventaId, null, datos);
    }

    private SobreEvento registrada(UUID ventaId, UUID cuentaId, Map<UUID, String> lineas) {
        return registrada(EntornoDeCuentas.NEGOCIO_A, ventaId, referencia(cuentaId), lineas);
    }

    private Map<UUID, String> loPedido() {
        var lineas = new LinkedHashMap<UUID, String>();
        lineas.put(e.cemita, "2");
        lineas.put(e.horchata, "1.0000");
        return lineas;
    }

    private SobreEvento anulada(UUID negocio, UUID ventaId) {
        var ahora = e.reloj.instant();
        return new SobreEvento(UuidV7.generar(ahora), "venta.anulada", 1, negocio, "ventas", ahora, ventaId, null,
                Map.of("venta_id", ventaId.toString(), "turno_id", UUID.randomUUID().toString(), "lineas",
                        List.of(Map.of("linea", 1, "producto_id", e.cemita.toString())), "pagos", List.of()));
    }

    private mx.negocio360.restaurantes.dominio.Cuenta cuenta(UUID id) {
        return e.almacen.cuenta(EntornoDeCuentas.NEGOCIO_A, id).orElseThrow();
    }

    @Test
    @DisplayName("CT-08: una venta con la referencia de la cuenta por cobrar la deja PAGADA con su venta y libera la mesa")
    void ventaPagaLaCuenta() {
        var cuentaId = cuentaPorCobrar();
        var ventaId = UuidV7.generar(e.reloj.instant());
        e.reloj.avanzar(Duration.ofMinutes(3));

        var resultado = e.vinculo.procesar(registrada(ventaId, cuentaId, loPedido()));

        assertThat(resultado).isEqualTo(Resultado.VINCULADA);
        var pagada = cuenta(cuentaId);
        assertThat(pagada.estado()).isEqualTo(EstadoDeCuenta.PAGADA);
        assertThat(pagada.ventaId()).isEqualTo(ventaId);
        assertThat(pagada.discrepancia()).isFalse();
        assertThat(pagada.ventaAnulada()).isFalse();
        assertThat(pagada.cerradaEn()).isEqualTo(e.reloj.instant());
        assertThat(e.salon.salon(e.ctxA, e.mesero).disponibilidad().libres()).isEqualTo(2);
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1)).isEmpty();
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).hasSize(1);
        assertThat(e.eventosDe("restaurantes.cuenta_con_discrepancia")).isEmpty();
    }

    @Test
    @DisplayName("CT-08, R3: el mismo evento dos veces, o la misma venta en otro evento, deja una cuenta pagada y no cambia nada")
    void esIdempotente() {
        var cuentaId = cuentaPorCobrar();
        var ventaId = UuidV7.generar(e.reloj.instant());
        var evento = registrada(ventaId, cuentaId, loPedido());
        e.vinculo.procesar(evento);
        var version = cuenta(cuentaId).version();

        var otraVez = e.vinculo.procesar(evento);
        var otroEvento = e.vinculo.procesar(registrada(ventaId, cuentaId, loPedido()));

        assertThat(otraVez).isEqualTo(Resultado.YA_VINCULADA);
        assertThat(otroEvento).isEqualTo(Resultado.YA_VINCULADA);
        assertThat(cuenta(cuentaId).version()).isEqualTo(version);
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).hasSize(1);
    }

    @Test
    @DisplayName("CT-08: si las lineas vendidas difieren de las de la cuenta queda discrepancia y se avisa una sola vez")
    void discrepancia() {
        var cuentaId = cuentaPorCobrar();
        var ventaId = UuidV7.generar(e.reloj.instant());
        var vendido = new LinkedHashMap<UUID, String>();
        vendido.put(e.cemita, "1");
        vendido.put(e.horchata, "1");
        var evento = registrada(ventaId, cuentaId, vendido);

        e.vinculo.procesar(evento);
        e.vinculo.procesar(evento);

        var pagada = cuenta(cuentaId);
        assertThat(pagada.estado()).isEqualTo(EstadoDeCuenta.PAGADA);
        assertThat(pagada.discrepancia()).isTrue();
        var avisos = e.eventosDe("restaurantes.cuenta_con_discrepancia");
        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0).datos()).containsEntry("cuenta_id", cuentaId.toString()).containsEntry("mesa", "Mesa 1")
                .containsEntry("venta_id", ventaId.toString());
    }

    @Test
    @DisplayName("CT-08: un platillo vendido que no estaba, o uno que faltaba, tambien es discrepancia; las lineas anuladas no cuentan")
    void discrepancia_casos() {
        var cuentaId = cuentaPorCobrar();
        var conExtra = new LinkedHashMap<>(loPedido());
        conExtra.put(UUID.randomUUID(), "1");
        var sinHorchata = Map.of(e.cemita, "2");

        e.vinculo.procesar(registrada(UuidV7.generar(e.reloj.instant()), cuentaId, conExtra));
        assertThat(cuenta(cuentaId).discrepancia()).isTrue();

        e.reloj.avanzar(Duration.ofMinutes(5));
        e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa2, 2);
        var otra = e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa2).get(0).id();
        e.servicio.agregarLinea(e.ctxA, e.mesero, otra, e.cemita, 2, null, false);
        e.servicio.agregarLinea(e.ctxA, e.mesero, otra, e.horchata, 1, null, false);
        e.servicio.enviarACocina(e.ctxA, e.mesero, otra);
        var horchata = e.servicio.cuenta(e.ctxA, e.mesero, otra).lineas().get(1).id();
        e.servicio.anularLinea(e.ctxA, e.mesero, horchata, MotivoDeAnulacion.AGOTADO, null);
        e.servicio.pedirCuenta(e.ctxA, e.mesero, otra);
        e.vinculo.procesar(registrada(UuidV7.generar(e.reloj.instant()), otra, sinHorchata));
        assertThat(cuenta(otra).estado()).isEqualTo(EstadoDeCuenta.PAGADA);
        assertThat(cuenta(otra).discrepancia()).isFalse();
    }

    @Test
    @DisplayName("CT-09: una venta sobre una cuenta ABIERTA tambien la deja PAGADA, con discrepancia")
    void laVentaGana() {
        var abierta = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 2);
        e.servicio.agregarLinea(e.ctxA, e.mesero, abierta.id(), e.cemita, 2, null, false);
        e.servicio.enviarACocina(e.ctxA, e.mesero, abierta.id());
        e.eventos.clear();
        var ventaId = UuidV7.generar(e.reloj.instant());

        var resultado = e.vinculo.procesar(registrada(ventaId, abierta.id(), Map.of(e.cemita, "2")));

        assertThat(resultado).isEqualTo(Resultado.VINCULADA);
        var pagada = cuenta(abierta.id());
        assertThat(pagada.estado()).isEqualTo(EstadoDeCuenta.PAGADA);
        assertThat(pagada.ventaId()).isEqualTo(ventaId);
        assertThat(pagada.discrepancia()).isTrue();
        assertThat(e.eventosDe("restaurantes.cuenta_con_discrepancia")).hasSize(1);
        assertThat(e.salon.salon(e.ctxA, e.mesero).disponibilidad().libres()).isEqualTo(2);
    }

    @Test
    @DisplayName("CT-08: la referencia de una cuenta de otro negocio, sin prefijo, mal formada o ausente no toca ninguna cuenta")
    void referenciaAjena() {
        var cuentaId = cuentaPorCobrar();
        var version = cuenta(cuentaId).version();
        var ventaId = UuidV7.generar(e.reloj.instant());

        assertThat(e.vinculo.procesar(registrada(EntornoDeCuentas.NEGOCIO_B, ventaId, referencia(cuentaId), loPedido())))
                .isEqualTo(Resultado.SIN_CUENTA);
        assertThat(e.vinculo.procesar(registrada(EntornoDeCuentas.NEGOCIO_A, ventaId, "tienda:pedido:" + cuentaId, loPedido())))
                .isEqualTo(Resultado.IGNORADO);
        assertThat(e.vinculo.procesar(registrada(EntornoDeCuentas.NEGOCIO_A, ventaId, "restaurantes:cuenta:no-es-uuid",
                loPedido()))).isEqualTo(Resultado.IGNORADO);
        assertThat(e.vinculo.procesar(registrada(EntornoDeCuentas.NEGOCIO_A, ventaId, null, loPedido())))
                .isEqualTo(Resultado.IGNORADO);
        assertThat(e.vinculo.procesar(registrada(EntornoDeCuentas.NEGOCIO_A, ventaId, referencia(UUID.randomUUID()),
                loPedido()))).isEqualTo(Resultado.SIN_CUENTA);
        assertThat(cuenta(cuentaId).estado()).isEqualTo(EstadoDeCuenta.POR_COBRAR);
        assertThat(cuenta(cuentaId).version()).isEqualTo(version);
    }

    @Test
    @DisplayName("una cuenta CANCELADA no se paga, y una ya pagada con otra venta conserva la primera")
    void cuentaCerrada() {
        var cancelada = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa2, 2);
        e.servicio.cancelarCuenta(e.ctxA, e.mesero, cancelada.id(), MotivoDeAnulacion.SIN_COMENSALES, null);
        var cuentaId = cuentaPorCobrar();
        var primera = UuidV7.generar(e.reloj.instant());
        e.vinculo.procesar(registrada(primera, cuentaId, loPedido()));

        var contraCancelada = e.vinculo.procesar(registrada(UuidV7.generar(e.reloj.instant()), cancelada.id(), loPedido()));
        var segunda = e.vinculo.procesar(registrada(UuidV7.generar(e.reloj.instant()), cuentaId, loPedido()));

        assertThat(contraCancelada).isEqualTo(Resultado.CUENTA_CANCELADA);
        assertThat(cuenta(cancelada.id()).estado()).isEqualTo(EstadoDeCuenta.CANCELADA);
        assertThat(segunda).isEqualTo(Resultado.YA_PAGADA);
        assertThat(cuenta(cuentaId).ventaId()).isEqualTo(primera);
    }

    @Test
    @DisplayName("CT-10: al anularse la venta la cuenta sigue PAGADA, lo recuerda y la mesa no se vuelve a ocupar")
    void ventaAnulada() {
        var cuentaId = cuentaPorCobrar();
        var ventaId = UuidV7.generar(e.reloj.instant());
        e.vinculo.procesar(registrada(ventaId, cuentaId, loPedido()));
        e.eventos.clear();

        var resultado = e.vinculo.procesar(anulada(EntornoDeCuentas.NEGOCIO_A, ventaId));
        var repetida = e.vinculo.procesar(anulada(EntornoDeCuentas.NEGOCIO_A, ventaId));

        assertThat(resultado).isEqualTo(Resultado.ANULACION_REGISTRADA);
        assertThat(repetida).isEqualTo(Resultado.ANULACION_REGISTRADA);
        var pagada = cuenta(cuentaId);
        assertThat(pagada.estado()).isEqualTo(EstadoDeCuenta.PAGADA);
        assertThat(pagada.ventaAnulada()).isTrue();
        assertThat(pagada.ventaId()).isEqualTo(ventaId);
        assertThat(e.salon.salon(e.ctxA, e.mesero).disponibilidad().libres()).isEqualTo(2);
        assertThat(e.eventosDe("negocio.disponibilidad_cambiada")).isEmpty();
    }

    @Test
    @DisplayName("CT-10: anular una venta que no esta vinculada, o de otro negocio, no toca ninguna cuenta")
    void anulacionAjena() {
        var cuentaId = cuentaPorCobrar();
        var ventaId = UuidV7.generar(e.reloj.instant());
        e.vinculo.procesar(registrada(ventaId, cuentaId, loPedido()));

        assertThat(e.vinculo.procesar(anulada(EntornoDeCuentas.NEGOCIO_A, UUID.randomUUID()))).isEqualTo(Resultado.IGNORADO);
        assertThat(e.vinculo.procesar(anulada(EntornoDeCuentas.NEGOCIO_B, ventaId))).isEqualTo(Resultado.IGNORADO);
        assertThat(cuenta(cuentaId).ventaAnulada()).isFalse();
    }

    @Test
    @DisplayName("anular y volver a vender: la nueva venta con la misma referencia reemplaza a la anulada")
    void anularYVolverAVender() {
        var cuentaId = cuentaPorCobrar();
        var primera = UuidV7.generar(e.reloj.instant());
        e.vinculo.procesar(registrada(primera, cuentaId, loPedido()));
        e.vinculo.procesar(anulada(EntornoDeCuentas.NEGOCIO_A, primera));
        var cerradaLaPrimeraVez = cuenta(cuentaId).cerradaEn();
        e.reloj.avanzar(Duration.ofMinutes(10));
        var segunda = UuidV7.generar(e.reloj.instant());

        var resultado = e.vinculo.procesar(registrada(segunda, cuentaId, Map.of(e.cemita, "2")));

        assertThat(resultado).isEqualTo(Resultado.VINCULADA);
        var pagada = cuenta(cuentaId);
        assertThat(pagada.ventaId()).isEqualTo(segunda);
        assertThat(pagada.ventaAnulada()).isFalse();
        assertThat(pagada.discrepancia()).isTrue();
        assertThat(pagada.estado()).isEqualTo(EstadoDeCuenta.PAGADA);
        assertThat(pagada.cerradaEn()).as("la cuenta se cerro la primera vez").isEqualTo(cerradaLaPrimeraVez);
    }

    @Test
    @DisplayName("un evento de otro origen, de otra version, de otro tipo o con datos que no cumplen el contrato se ignora sin lanzar")
    void lectorTolerante() {
        var cuentaId = cuentaPorCobrar();
        var valido = registrada(UuidV7.generar(e.reloj.instant()), cuentaId, loPedido());
        var deOtroOrigen = new SobreEvento(valido.id(), valido.tipo(), 1, valido.negocioId(), "otro", valido.ocurridoEn(),
                valido.correlacionId(), null, valido.datos());
        var deOtraVersion = new SobreEvento(valido.id(), valido.tipo(), 2, valido.negocioId(), valido.origen(),
                valido.ocurridoEn(), valido.correlacionId(), null, valido.datos());
        var otroTipo = new SobreEvento(valido.id(), "venta.cancelada", 1, valido.negocioId(), valido.origen(),
                valido.ocurridoEn(), valido.correlacionId(), null, valido.datos());
        var malo = new SobreEvento(valido.id(), valido.tipo(), 1, valido.negocioId(), valido.origen(), valido.ocurridoEn(),
                valido.correlacionId(), null, Map.of("referencia", referencia(cuentaId)));

        assertThat(e.vinculo.procesar(deOtroOrigen)).isEqualTo(Resultado.IGNORADO);
        assertThat(e.vinculo.procesar(deOtraVersion)).isEqualTo(Resultado.IGNORADO);
        assertThat(e.vinculo.procesar(otroTipo)).isEqualTo(Resultado.IGNORADO);
        assertThat(e.vinculo.procesar(malo)).isEqualTo(Resultado.DATOS_INVALIDOS);
        assertThat(cuenta(cuentaId).estado()).isEqualTo(EstadoDeCuenta.POR_COBRAR);
    }
}
