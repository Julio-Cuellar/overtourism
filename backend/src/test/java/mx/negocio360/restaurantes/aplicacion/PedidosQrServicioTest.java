package mx.negocio360.restaurantes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeCuenta;
import mx.negocio360.restaurantes.dominio.EstadoDeLinea;
import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;
import mx.negocio360.restaurantes.dominio.OrigenDeCuenta;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.dominio.Restaurante;
import mx.negocio360.restaurantes.dominio.Temporada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** R6 · QR-03 a QR-20: la mesa por QR, el pedido del comensal sin cuenta, su seguimiento y las defensas. Cada prueba cita su regla. */
class PedidosQrServicioTest {

    private static final String DISPOSITIVO_A = "11111111-1111-4111-8111-111111111111";
    private static final String DISPOSITIVO_B = "22222222-2222-4222-8222-222222222222";

    private final EntornoDeCuentas e = new EntornoDeCuentas().armado();

    private static void falla(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOf(RestaurantesException.class).extracting("codigo").isEqualTo(esperado);
    }

    private static LineaDePedidoQr linea(UUID platillo, int cantidad) {
        return new LineaDePedidoQr(platillo, cantidad, null, false);
    }

    private String token() {
        return e.token(e.mesa1);
    }

    private ResultadoPedidoQrVista pedir(String dispositivo, String llave, LineaDePedidoQr... lineas) {
        return e.qr.pedirPorQr(token(), dispositivo, List.of(lineas), llave);
    }

    // ---- QR-03: mesaPorQr ----

    @Test
    @DisplayName("QR-03: mesaPorQr devuelve el negocio, la mesa y si acepta pedidos, sin datos de la cuenta")
    void mesaPorQr() {
        var mesa = e.qr.mesaPorQr(token()).orElseThrow();

        assertThat(mesa.negocioId()).isEqualTo(EntornoDeCuentas.NEGOCIO_A);
        assertThat(mesa.mesa()).isEqualTo("Mesa 1");
        assertThat(mesa.servicioAbierto()).isTrue();
        assertThat(mesa.aceptaPedidos()).isTrue();
        assertThat(mesa.estadoCuenta()).isEqualTo(EstadoPublicoDeCuenta.SIN_CUENTA);
        assertThat(MesaPublicaVista.class.getRecordComponents()).extracting("name")
                .containsExactlyInAnyOrder("negocioId", "mesa", "servicioAbierto", "aceptaPedidos", "estadoCuenta");
    }

    @Test
    @DisplayName("QR-03: un token inexistente, rotado o de una mesa dada de baja responde igual: vacio")
    void mesaPorQr_invalido() {
        var viejo = token();
        e.salon.regenerarQrDeMesa(e.ctxA, e.dueno, e.mesa1);
        var dadaDeBaja = e.token(e.mesa2);
        e.salon.darDeBajaMesa(e.ctxA, e.dueno, e.mesa2);

        assertThat(e.qr.mesaPorQr("no-existe")).isEmpty();
        assertThat(e.qr.mesaPorQr(viejo)).isEmpty();
        assertThat(e.qr.mesaPorQr(dadaDeBaja)).isEmpty();
        assertThat(e.qr.mesaPorQr(null)).isEmpty();
    }

    @Test
    @DisplayName("QR-04, PB-01: con el negocio no visible el QR responde vacio; al publicarlo, vuelve")
    void mesaPorQr_negocioNoVisible() {
        e.restaurantes.guardar(Restaurante.nuevo(EntornoDeCuentas.NEGOCIO_A, EntornoDeCuentas.AHORA));
        assertThat(e.qr.mesaPorQr(token())).isEmpty();
        falla(() -> pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1)), CodigoError.QR_INVALIDO);

        e.restaurantes.guardar(Restaurante.nuevo(EntornoDeCuentas.NEGOCIO_A, EntornoDeCuentas.AHORA)
                .conPublicado(true, EntornoDeCuentas.AHORA));
        assertThat(e.qr.mesaPorQr(token())).isPresent();
    }

    // ---- QR-05: acepta_pedidos ----

    @Test
    @DisplayName("QR-05: no acepta pedidos con el servicio cerrado, el QR apagado, la mesa bloqueada o pausada")
    void aceptaPedidos() {
        e.salon.cerrarServicio(e.ctxA, e.dueno, null);
        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isFalse();
        falla(() -> pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1)), CodigoError.SERVICIO_CERRADO);
        e.salon.abrirServicio(e.ctxA, e.dueno);

        e.salon.habilitarQr(e.ctxA, e.dueno, false);
        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isFalse();
        falla(() -> pedir(DISPOSITIVO_A, "llave-0002", linea(e.cemita, 1)), CodigoError.QR_DESHABILITADO);
        e.salon.habilitarQr(e.ctxA, e.dueno, true);

        e.salon.pausarQrDeMesa(e.ctxA, e.dueno, e.mesa1, true);
        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isFalse();
        falla(() -> pedir(DISPOSITIVO_A, "llave-0003", linea(e.cemita, 1)), CodigoError.QR_PAUSADO);
        e.salon.pausarQrDeMesa(e.ctxA, e.dueno, e.mesa1, false);

        e.salon.bloquearMesa(e.ctxA, e.dueno, e.mesa1, true);
        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isFalse();
        falla(() -> pedir(DISPOSITIVO_A, "llave-0004", linea(e.cemita, 1)), CodigoError.QR_PAUSADO);
    }

    @Test
    @DisplayName("QR-05: la pausa de una hora se acaba sola")
    void pausaSeAcaba() {
        e.salon.pausarQrDeMesa(e.ctxA, e.dueno, e.mesa1, true);
        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isFalse();

        e.reloj.avanzar(Duration.ofMinutes(61));

        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isTrue();
    }

    // ---- QR-05, QR-06, QR-07: el primer pedido ----

    @Test
    @DisplayName("QR-05: el primer pedido de una mesa libre abre la cuenta, de origen QR y sin comensales")
    void primerPedido() {
        var resultado = pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 2), linea(e.horchata, 1));

        assertThat(resultado.abrioLaCuenta()).isTrue();
        var cuenta = e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0);
        assertThat(cuenta.origen()).isEqualTo(OrigenDeCuenta.QR);
        assertThat(cuenta.comensales()).isNull();
        assertThat(cuenta.numero()).isEqualTo(1);
        assertThat(cuenta.total()).isEqualByComparingTo("205.00");
        assertThat(e.qr.mesaPorQr(token()).orElseThrow().estadoCuenta()).isEqualTo(EstadoPublicoDeCuenta.ABIERTA);
    }

    @Test
    @DisplayName("MS-06: el primer pedido ocupa la mesa y publica la disponibilidad; el segundo no la repite")
    void ocupaLaMesa() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1));
        pedir(DISPOSITIVO_B, "llave-0002", linea(e.cemita, 1));

        var cambios = e.eventosDe("negocio.disponibilidad_cambiada");

        assertThat(cambios).hasSize(1);
        assertThat(cambios.get(0).datos()).containsEntry("libres", 1).containsEntry("total", 2);
    }

    @Test
    @DisplayName("QR-06: las lineas del QR nacen ENVIADA y el pedido crea una comanda por estacion sin ninguna accion del mesero")
    void vaDirectoACocina() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 2), linea(e.horchata, 1));

        var tablero = e.cocina.comandas(e.ctxA, e.cocinero, null, null, 50);

        assertThat(tablero).extracting(ComandaVista::estacion).containsExactlyInAnyOrder(Estacion.COCINA, Estacion.BARRA);
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0).lineas()).extracting(LineaVista::estado)
                .containsOnly(EstadoDeLinea.ENVIADA);
        assertThat(e.eventosDe("restaurantes.comanda_enviada")).hasSize(2);
    }

    @Test
    @DisplayName("QR-07: cada pedido publica pedido_qr_recibido con la mesa, el numero de lineas y si abrio la cuenta")
    void alertaAMeseros() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1), linea(e.horchata, 1));
        pedir(DISPOSITIVO_B, "llave-0002", linea(e.cemita, 1));

        var avisos = e.eventosDe("restaurantes.pedido_qr_recibido");

        assertThat(avisos).hasSize(2);
        assertThat(avisos.get(0).datos()).containsEntry("mesa", "Mesa 1").containsEntry("lineas", 2)
                .containsEntry("abrio_la_cuenta", true).containsKey("cuenta_id");
        assertThat(avisos.get(1).datos()).containsEntry("lineas", 1).containsEntry("abrio_la_cuenta", false);
    }

    @Test
    @DisplayName("QR-20: dos dispositivos en la misma mesa comparten la cuenta, y la cuenta para cobro suma todas las lineas")
    void cuentaCompartida() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 2));
        pedir(DISPOSITIVO_B, "llave-0002", linea(e.cemita, 1), linea(e.horchata, 1));
        var cuenta = e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1);
        assertThat(cuenta).hasSize(1);

        e.qr.pedirCuentaPorQr(token(), DISPOSITIVO_A);
        var cobro = e.servicio.cuentaParaCobro(e.ctxA, e.mesero, cuenta.get(0).id());

        assertThat(cobro.total()).isEqualByComparingTo("290.00");
        assertThat(cobro.lineas()).extracting(CuentaParaCobro.LineaDeCobro::cantidad).containsExactlyInAnyOrder(3, 1);
    }

    @Test
    @DisplayName("QR-05: dos primeros pedidos simultaneos dejan una sola cuenta y dos pedidos")
    void primerosPedidosSimultaneos() throws Exception {
        var hilos = Executors.newFixedThreadPool(2);
        var salida = new CountDownLatch(1);
        var errores = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();
        try {
            for (var i = 0; i < 2; i++) {
                var dispositivo = i == 0 ? DISPOSITIVO_A : DISPOSITIVO_B;
                var llave = "llave-simultanea-" + i;
                hilos.submit(() -> {
                    try {
                        salida.await();
                        pedir(dispositivo, llave, linea(e.cemita, 1));
                    } catch (Throwable t) {
                        errores.add(t);
                    }
                });
            }
            salida.countDown();
        } finally {
            hilos.shutdown();
            assertThat(hilos.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(errores).isEmpty();
        var cuentas = e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1);
        assertThat(cuentas).hasSize(1);
        assertThat(cuentas.get(0).lineas()).hasSize(2);
    }

    // ---- QR-06: platillos que no se pueden pedir ----

    @Test
    @DisplayName("QR-06: un platillo agotado, fuera de temporada, dado de baja o de otro negocio se rechaza con su codigo")
    void platillosNoOrdenables() {
        e.menuServicio.marcarAgotado(e.ctxA, e.dueno, e.cemita, true);
        falla(() -> pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1)), CodigoError.PLATILLO_AGOTADO);

        var invierno = e.menuServicio.crearPlatillo(e.ctxA, e.dueno, new DatosPlatillo("Chiles en nogada", null,
                new BigDecimal("180.00"), null, Set.of(), Temporada.de("12-01", "12-31"))).id();
        falla(() -> pedir(DISPOSITIVO_A, "llave-0002", linea(invierno, 1)), CodigoError.FUERA_DE_TEMPORADA);

        var baja = e.menuServicio.crearPlatillo(e.ctxA, e.dueno, new DatosPlatillo("Tacos", null, new BigDecimal("60.00"),
                null, Set.of(), null)).id();
        var tacos = e.menu.platillo(EntornoDeCuentas.NEGOCIO_A, baja).orElseThrow();
        e.menu.reemplazar(tacos.conBaja(EntornoDeCuentas.AHORA), tacos.version());
        falla(() -> pedir(DISPOSITIVO_A, "llave-0003", linea(baja, 1)), CodigoError.PLATILLO_NO_ORDENABLE);
        falla(() -> pedir(DISPOSITIVO_A, "llave-0004", linea(UUID.randomUUID(), 1)), CodigoError.PLATILLO_NO_ENCONTRADO);
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1)).isEmpty();
    }

    @Test
    @DisplayName("QR-19: un pedido rechazado no deja cuenta ni comandas; el token de una mesa solo pide en esa mesa")
    void tokenDeUnaMesa() {
        falla(() -> pedir(DISPOSITIVO_A, "llave-0001", linea(UUID.randomUUID(), 1)), CodigoError.PLATILLO_NO_ENCONTRADO);
        assertThat(e.cocina.comandas(e.ctxA, e.cocinero, null, null, 50)).isEmpty();

        e.qr.pedirPorQr(e.token(e.mesa2), DISPOSITIVO_A, List.of(linea(e.cemita, 1)), "llave-0002");

        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1)).isEmpty();
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa2)).hasSize(1);
    }

    // ---- QR-11, QR-12: seguimiento del pedido ----

    @Test
    @DisplayName("QR-11: cada dispositivo ve solo las lineas que pidio, con su estado, y nunca totales ni lo de otros")
    void seguimiento() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 2));
        pedir(DISPOSITIVO_B, "llave-0002", linea(e.horchata, 1));

        var deA = e.qr.estadoDePedidoQr(token(), DISPOSITIVO_A).orElseThrow();
        var deB = e.qr.estadoDePedidoQr(token(), DISPOSITIVO_B).orElseThrow();

        assertThat(deA.lineas()).extracting(LineaDePedidoQrVista::platillo, LineaDePedidoQrVista::cantidad,
                LineaDePedidoQrVista::estado).containsExactly(org.assertj.core.groups.Tuple.tuple("Cemita poblana", 2,
                EstadoDeLineaQr.EN_COCINA));
        assertThat(deB.lineas()).extracting(LineaDePedidoQrVista::platillo).containsExactly("Agua de horchata");
        assertThat(deA.cuenta()).isEqualTo(EstadoPublicoDeCuenta.ABIERTA);
        assertThat(EstadoDePedidoQrVista.class.getRecordComponents()).extracting("name")
                .containsExactlyInAnyOrder("cuenta", "lineas");
    }

    @Test
    @DisplayName("QR-11: el estado de la linea sigue a su comanda: PREPARANDO, LISTO y ENTREGADO")
    void seguimiento_avanza() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1));
        var comanda = e.cocina.comandas(e.ctxA, e.cocinero, Estacion.COCINA, null, 5).get(0).id();

        e.cocina.avanzarComanda(e.ctxA, e.cocinero, comanda);
        assertThat(e.qr.estadoDePedidoQr(token(), DISPOSITIVO_A).orElseThrow().lineas().get(0).estado())
                .isEqualTo(EstadoDeLineaQr.PREPARANDO);
        e.cocina.avanzarComanda(e.ctxA, e.cocinero, comanda);
        assertThat(e.qr.estadoDePedidoQr(token(), DISPOSITIVO_A).orElseThrow().lineas().get(0).estado())
                .isEqualTo(EstadoDeLineaQr.LISTO);
        e.cocina.avanzarComanda(e.ctxA, e.mesero, comanda);
        assertThat(e.qr.estadoDePedidoQr(token(), DISPOSITIVO_A).orElseThrow().lineas().get(0).estado())
                .isEqualTo(EstadoDeLineaQr.ENTREGADO);
    }

    @Test
    @DisplayName("QR-10, QR-11: una linea cancelada sale CANCELADO con el mensaje fijo del motivo")
    void seguimiento_cancelada() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1), linea(e.horchata, 1));
        var linea = e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0).lineas().get(0).id();
        e.servicio.anularLinea(e.ctxA, e.mesero, linea, MotivoDeAnulacion.AGOTADO, null);

        var lineas = e.qr.estadoDePedidoQr(token(), DISPOSITIVO_A).orElseThrow().lineas();

        assertThat(lineas.get(0).estado()).isEqualTo(EstadoDeLineaQr.CANCELADO);
        assertThat(lineas.get(0).mensaje()).isEqualTo("Cancelado: agotado");
        assertThat(lineas.get(1).estado()).isEqualTo(EstadoDeLineaQr.EN_COCINA);
        assertThat(lineas.get(1).mensaje()).isNull();
    }

    @Test
    @DisplayName("QR-11: con un token invalido o sin cuenta, el estado es vacio y sin lineas")
    void seguimiento_sinCuenta() {
        assertThat(e.qr.estadoDePedidoQr("no-existe", DISPOSITIVO_A)).isEmpty();

        var estado = e.qr.estadoDePedidoQr(token(), DISPOSITIVO_A).orElseThrow();

        assertThat(estado.cuenta()).isEqualTo(EstadoPublicoDeCuenta.SIN_CUENTA);
        assertThat(estado.lineas()).isEmpty();
    }

    @Test
    @DisplayName("QR-12: el servidor guarda el hash con clave del dispositivo, nunca su id, y es el mismo para el mismo dispositivo")
    void hashDelDispositivo() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1));
        var linea = e.almacen.cuentas(EntornoDeCuentas.NEGOCIO_A).get(0).lineas().get(0);

        assertThat(linea.dispositivoHash()).isNotNull().isNotEqualTo(DISPOSITIVO_A).doesNotContain(DISPOSITIVO_A);
        assertThat(linea.dispositivoHash()).isEqualTo(e.hash.de(DISPOSITIVO_A)).isNotEqualTo(e.hash.de(DISPOSITIVO_B));
        assertThat(HashDeDispositivo.conClave("otra clave".getBytes()).de(DISPOSITIVO_A)).isNotEqualTo(linea.dispositivoHash());
    }

    @Test
    @DisplayName("QR-12, CT-12: pasados 30 dias el hash del dispositivo y la nota se borran")
    void hashSePurga() {
        e.qr.pedirPorQr(token(), DISPOSITIVO_A, List.of(new LineaDePedidoQr(e.cemita, 1, "sin cebolla", true)), "llave-0001");
        e.reloj.avanzar(Duration.ofDays(31));

        e.servicio.purgarDatosViejos(e.reloj.instant());

        var linea = e.almacen.cuentas(EntornoDeCuentas.NEGOCIO_A).get(0).lineas().get(0);
        assertThat(linea.dispositivoHash()).isNull();
        assertThat(linea.nota()).isNull();
    }

    // ---- QR-13, QR-14: pedir la cuenta y volver a pedir ----

    @Test
    @DisplayName("QR-13: pedir la cuenta por QR la pasa a POR_COBRAR y avisa a meseros y caja; despues no se puede pedir mas")
    void pedirLaCuenta() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1));

        var estado = e.qr.pedirCuentaPorQr(token(), DISPOSITIVO_A);

        assertThat(estado.cuenta()).isEqualTo(EstadoPublicoDeCuenta.SOLICITADA);
        var avisos = e.eventosDe("restaurantes.cuenta_solicitada");
        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0).datos()).containsEntry("solicitada_por", "QR").containsEntry("mesa", "Mesa 1");
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0).estado()).isEqualTo(EstadoDeCuenta.POR_COBRAR);
        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isFalse();
        falla(() -> pedir(DISPOSITIVO_B, "llave-0002", linea(e.cemita, 1)), CodigoError.CUENTA_SOLICITADA);
    }

    @Test
    @DisplayName("QR-13: pedir la cuenta otra vez no la repite; sin cuenta o sin lineas vivas falla")
    void pedirLaCuenta_requisitos() {
        falla(() -> e.qr.pedirCuentaPorQr(token(), DISPOSITIVO_A), CodigoError.CUENTA_VACIA);
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1));
        e.qr.pedirCuentaPorQr(token(), DISPOSITIVO_A);

        var otraVez = e.qr.pedirCuentaPorQr(token(), DISPOSITIVO_B);

        assertThat(otraVez.cuenta()).isEqualTo(EstadoPublicoDeCuenta.SOLICITADA);
        assertThat(e.eventosDe("restaurantes.cuenta_solicitada")).hasSize(1);
        falla(() -> e.qr.pedirCuentaPorQr("no-existe", DISPOSITIVO_A), CodigoError.QR_INVALIDO);
    }

    @Test
    @DisplayName("QR-13: el personal puede reabrir la cuenta y el QR vuelve a aceptar pedidos")
    void reabrir() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1));
        e.qr.pedirCuentaPorQr(token(), DISPOSITIVO_A);
        var cuenta = e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0);

        e.servicio.reabrirCuenta(e.ctxA, e.mesero, cuenta.id());

        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isTrue();
        assertThat(pedir(DISPOSITIVO_B, "llave-0002", linea(e.horchata, 1)).abrioLaCuenta()).isFalse();
    }

    @Test
    @DisplayName("QR-14: tras el pago la mesa se libera y el siguiente escaneo abre una cuenta nueva con otro numero")
    void despuesDelPago() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1));
        e.qr.pedirCuentaPorQr(token(), DISPOSITIVO_A);
        var primera = e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0);
        e.servicio.liberarMesa(e.ctxA, e.gerente, primera.id(), "Cobrada a mano");

        var resultado = pedir(DISPOSITIVO_B, "llave-0002", linea(e.horchata, 1));

        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isTrue();
        assertThat(resultado.abrioLaCuenta()).isTrue();
        var segunda = e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0);
        assertThat(segunda.id()).isNotEqualTo(primera.id());
        assertThat(segunda.numero()).isEqualTo(2);
        assertThat(e.qr.estadoDePedidoQr(token(), DISPOSITIVO_A).orElseThrow().lineas()).isEmpty();
    }

    // ---- QR-15: limites ----

    @Test
    @DisplayName("QR-15: un pedido de mas de 20 lineas o de una linea con mas de 10 piezas falla con LIMITE_QR_EXCEDIDO")
    void limitesDelPedido() {
        var veintiuna = java.util.stream.IntStream.range(0, 21).mapToObj(i -> linea(e.cemita, 1))
                .toArray(LineaDePedidoQr[]::new);

        falla(() -> pedir(DISPOSITIVO_A, "llave-0001", veintiuna), CodigoError.LIMITE_QR_EXCEDIDO);
        falla(() -> pedir(DISPOSITIVO_A, "llave-0002", linea(e.cemita, 11)), CodigoError.LIMITE_QR_EXCEDIDO);
        falla(() -> pedir(DISPOSITIVO_A, "llave-0003", linea(e.cemita, 0)), CodigoError.CANTIDAD_INVALIDA);
        falla(() -> e.qr.pedirPorQr(token(), DISPOSITIVO_A, List.of(), "llave-0004"), CodigoError.CUENTA_VACIA);
        falla(() -> e.qr.pedirPorQr(token(), DISPOSITIVO_A, List.of(new LineaDePedidoQr(e.cemita, 1, "x".repeat(81), false)),
                "llave-0005"), CodigoError.NOTA_INVALIDA);
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1)).isEmpty();
        assertThat(pedir(DISPOSITIVO_A, "llave-0006", linea(e.cemita, 10)).abrioLaCuenta()).isTrue();
    }

    @Test
    @DisplayName("QR-15: una cuenta por QR no pasa de 60 lineas ni del monto estimado maximo; el personal puede seguir agregando")
    void limitesDeLaCuenta() {
        for (var i = 0; i < 3; i++) {
            var veinte = java.util.stream.IntStream.range(0, 20).mapToObj(n -> linea(e.horchata, 1))
                    .toArray(LineaDePedidoQr[]::new);
            pedir(DISPOSITIVO_A + i, "llave-lineas-" + i, veinte);
        }

        falla(() -> pedir(DISPOSITIVO_B, "llave-lineas-extra", linea(e.horchata, 1)), CodigoError.LIMITE_QR_EXCEDIDO);
        var cuenta = e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0);
        assertThat(e.servicio.agregarLinea(e.ctxA, e.mesero, cuenta.id(), e.horchata, 1, null, false).lineas()).hasSize(61);
    }

    @Test
    @DisplayName("QR-15: el monto estimado maximo de una cuenta por QR es de 3,000.00 por omision")
    void montoMaximo() {
        for (var i = 1; i <= 3; i++) {
            pedir(DISPOSITIVO_A, "llave-monto-" + i, linea(e.cemita, 10));
        }

        falla(() -> pedir(DISPOSITIVO_A, "llave-monto-4", linea(e.cemita, 10)), CodigoError.LIMITE_QR_EXCEDIDO);
        assertThat(pedir(DISPOSITIVO_A, "llave-monto-5", linea(e.cemita, 5)).abrioLaCuenta()).isFalse();
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0).total()).isEqualByComparingTo("2975.00");
    }

    @Test
    @DisplayName("QR-15: un pedido que por si solo pasa del monto maximo no deja una cuenta vacia abierta")
    void montoMaximo_enElPrimerPedido() {
        var caro = e.menuServicio.crearPlatillo(e.ctxA, e.dueno, new DatosPlatillo("Langosta", null,
                new BigDecimal("400.00"), null, Set.of(), null)).id();

        falla(() -> pedir(DISPOSITIVO_A, "llave-0001", linea(caro, 8)), CodigoError.LIMITE_QR_EXCEDIDO);

        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1)).isEmpty();
        assertThat(e.qr.mesaPorQr(token()).orElseThrow().estadoCuenta()).isEqualTo(EstadoPublicoDeCuenta.SIN_CUENTA);
    }

    @Test
    @DisplayName("QR-15: una llave reutilizada con un pedido distinto se rechaza, y sin dispositivo no hay pedido")
    void llaveReutilizada() {
        pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1));

        falla(() -> pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 2)), CodigoError.LLAVE_REUTILIZADA);
        falla(() -> pedir(" ", "llave-0002", linea(e.cemita, 1)), CodigoError.DISPOSITIVO_INVALIDO);
        falla(() -> pedir(null, "llave-0003", linea(e.cemita, 1)), CodigoError.DISPOSITIVO_INVALIDO);
        falla(() -> e.qr.pedirCuentaPorQr(token(), null), CodigoError.DISPOSITIVO_INVALIDO);
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0).lineas()).hasSize(1);
    }

    @Test
    @DisplayName("QR-05: el restaurante que atiende solo en mostrador no recibe pedidos por QR de mesa")
    void soloMostrador() {
        e.salon.configurarModo(e.ctxA, e.dueno, mx.negocio360.restaurantes.dominio.ModoServicio.MOSTRADOR);

        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isFalse();
        falla(() -> pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1)), CodigoError.QR_DESHABILITADO);
    }

    @Test
    @DisplayName("CT-14, QR-15: repetir el mismo Idempotency-Key no duplica el pedido y devuelve lo mismo; sin llave, LLAVE_REQUERIDA")
    void idempotencia() {
        var primero = pedir(DISPOSITIVO_A, "llave-repetida", linea(e.cemita, 2));
        var segundo = pedir(DISPOSITIVO_A, "llave-repetida", linea(e.cemita, 2));

        assertThat(segundo).isEqualTo(primero);
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0).lineas()).hasSize(1);
        assertThat(e.eventosDe("restaurantes.pedido_qr_recibido")).hasSize(1);
        falla(() -> pedir(DISPOSITIVO_A, null, linea(e.cemita, 1)), CodigoError.LLAVE_REQUERIDA);
        falla(() -> pedir(DISPOSITIVO_A, "  ", linea(e.cemita, 1)), CodigoError.LLAVE_REQUERIDA);
        pedir(DISPOSITIVO_A, "llave-distinta", linea(e.cemita, 1));
        assertThat(e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0).lineas()).hasSize(2);
    }

    // ---- QR-16: limite de tasa ----

    @Test
    @DisplayName("QR-16: el sexto pedido en un minuto con el mismo token se rechaza con TASA_EXCEDIDA y se acepta pasado el minuto")
    void tasa() {
        for (var i = 1; i <= 5; i++) {
            pedir("dispositivo-" + i, "llave-tasa-" + i, linea(e.horchata, 1));
        }

        falla(() -> pedir("dispositivo-6", "llave-tasa-6", linea(e.horchata, 1)), CodigoError.TASA_EXCEDIDA);
        e.reloj.avanzar(Duration.ofSeconds(61));
        assertThat(pedir("dispositivo-6", "llave-tasa-7", linea(e.horchata, 1)).abrioLaCuenta()).isFalse();
    }

    @Test
    @DisplayName("QR-16: tambien se limita por dispositivo, aunque cambie de mesa")
    void tasaPorDispositivo() {
        for (var i = 1; i <= 5; i++) {
            e.qr.pedirPorQr(i % 2 == 0 ? e.token(e.mesa1) : e.token(e.mesa2), DISPOSITIVO_A, List.of(linea(e.horchata, 1)),
                    "llave-disp-" + i);
        }

        falla(() -> e.qr.pedirPorQr(e.token(e.mesa2), DISPOSITIVO_A, List.of(linea(e.horchata, 1)), "llave-disp-6"),
                CodigoError.TASA_EXCEDIDA);
    }

    // ---- QR-17: defensa contra bromas ----

    @Test
    @DisplayName("QR-17: tres cuentas por QR canceladas por SIN_COMENSALES en una hora pausan el QR de esa mesa y avisan")
    void bromas() {
        for (var i = 0; i < 3; i++) {
            pedir(DISPOSITIVO_A + i, "llave-broma-" + i, linea(e.horchata, 1));
            var cuenta = e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0);
            if (i < 2) {
                assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isTrue();
            }
            e.servicio.cancelarCuenta(e.ctxA, e.mesero, cuenta.id(), MotivoDeAnulacion.SIN_COMENSALES, null);
            e.reloj.avanzar(Duration.ofMinutes(5));
        }

        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isFalse();
        falla(() -> pedir(DISPOSITIVO_B, "llave-despues", linea(e.horchata, 1)), CodigoError.QR_PAUSADO);
        assertThat(e.eventosDe("restaurantes.qr_pausado")).hasSize(1);
        assertThat(e.eventosDe("restaurantes.qr_pausado").get(0).datos()).containsEntry("mesa", "Mesa 1")
                .containsKey("hasta");
        e.reloj.avanzar(Duration.ofMinutes(61));
        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isTrue();
    }

    @Test
    @DisplayName("QR-17: dos cancelaciones, o tres con otro motivo o en mesas distintas, no pausan nada")
    void bromas_noAlcanza() {
        for (var i = 0; i < 2; i++) {
            pedir(DISPOSITIVO_A + i, "llave-broma-" + i, linea(e.horchata, 1));
            e.servicio.cancelarCuenta(e.ctxA, e.mesero, e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0).id(),
                    MotivoDeAnulacion.SIN_COMENSALES, null);
        }
        pedir(DISPOSITIVO_B, "llave-otro-motivo", linea(e.horchata, 1));
        e.servicio.cancelarCuenta(e.ctxA, e.mesero, e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0).id(),
                MotivoDeAnulacion.ERROR_DEL_PEDIDO, null);

        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isTrue();
        assertThat(e.eventosDe("restaurantes.qr_pausado")).isEmpty();
    }

    @Test
    @DisplayName("QR-17: tres cancelaciones por SIN_COMENSALES repartidas en mas de una hora no pausan la mesa")
    void bromas_fueraDeLaVentana() {
        for (var i = 0; i < 3; i++) {
            pedir(DISPOSITIVO_A + i, "llave-broma-" + i, linea(e.horchata, 1));
            e.servicio.cancelarCuenta(e.ctxA, e.mesero, e.servicio.cuentasAbiertas(e.ctxA, e.mesero, e.mesa1).get(0).id(),
                    MotivoDeAnulacion.SIN_COMENSALES, null);
            e.reloj.avanzar(Duration.ofMinutes(40));
        }

        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isTrue();
        assertThat(e.eventosDe("restaurantes.qr_pausado")).isEmpty();
    }

    @Test
    @DisplayName("QR-17: una cuenta del personal cancelada por SIN_COMENSALES no cuenta para la defensa")
    void bromas_soloQr() {
        for (var i = 0; i < 3; i++) {
            var cuenta = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 2);
            e.servicio.cancelarCuenta(e.ctxA, e.mesero, cuenta.id(), MotivoDeAnulacion.SIN_COMENSALES, null);
        }

        assertThat(e.qr.mesaPorQr(token()).orElseThrow().aceptaPedidos()).isTrue();
    }

    // ---- una cuenta del personal ya abierta ----

    @Test
    @DisplayName("QR-05: si el personal ya abrio la cuenta de la mesa, el pedido por QR se suma a ella")
    void cuentaDelPersonal() {
        var cuenta = e.servicio.abrirCuenta(e.ctxA, e.mesero, e.mesa1, 3);

        var resultado = pedir(DISPOSITIVO_A, "llave-0001", linea(e.cemita, 1));

        assertThat(resultado.abrioLaCuenta()).isFalse();
        var despues = e.servicio.cuenta(e.ctxA, e.mesero, cuenta.id());
        assertThat(despues.origen()).isEqualTo(OrigenDeCuenta.PERSONAL);
        assertThat(despues.lineas()).hasSize(1);
    }
}
