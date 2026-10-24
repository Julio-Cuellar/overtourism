package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import mx.negocio360.caja.CajaApi;
import mx.negocio360.caja.aplicacion.AbrirTurnoComando;
import mx.negocio360.caja.aplicacion.CrearCajaComando;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.dominio.EstadoVentaAplicada;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.pruebas.Permisos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * F5: los eventos de Ventas llegan a Caja por el outbox de Spring Modulith contra PostgreSQL de verdad. Cubre el
 * contrato de entrega (al menos una vez, sin orden global, R3): el evento se completa, el mismo evento repetido deja un
 * efecto, la anulacion que llega antes que la venta no suma, un evento que Caja todavia no puede aplicar queda pendiente
 * y, si la transaccion se deshace, no sale ningun evento.
 */
@SpringBootTest(properties = {"ventas.almacen=postgres", "caja.almacen=postgres"})
class VentasACajaPorOutboxPostgresTest {

    private static final Duration ESPERA = Duration.ofSeconds(10);

    @Autowired
    private CajaApi caja;
    @Autowired
    private LibroDeCaja libro;
    @Autowired
    private ApplicationEventPublisher bus;
    @Autowired
    private TransactionTemplate transaccion;
    @Autowired
    private PlatformTransactionManager transacciones;

    /** Un negocio con una caja y un turno abierto con 500.00 de fondo; devuelve el contexto y el turno. */
    private record Montaje(ContextoNegocio contexto, UUID turno) {
    }

    private Montaje turnoAbierto() {
        var contexto = new ContextoNegocio(UUID.randomUUID(), "encargado-a");
        var cajaId = confirmar(contexto, new CrearCajaComando("Taquilla")).recursoId();
        var turno = confirmar(contexto, new AbrirTurnoComando(cajaId, new BigDecimal("500.00"))).recursoId();
        return new Montaje(contexto, turno);
    }

    private mx.negocio360.compartido.comandos.ResultadoComando confirmar(ContextoNegocio contexto,
                                                                           mx.negocio360.compartido.comandos.Comando comando) {
        return caja.confirmar(contexto, Permisos.TODOS, caja.preparar(contexto, Permisos.TODOS, comando).token());
    }

    private static SobreEvento evento(String tipo, UUID negocio, UUID turno, UUID venta, String efectivo, Instant cuando) {
        var datos = tipo.equals("venta.registrada")
                ? Map.<String, Object>of("venta_id", venta.toString(), "turno_id", turno.toString(), "total", efectivo,
                        "pagos", List.of(Map.of("metodo", "EFECTIVO", "monto", efectivo)))
                : Map.<String, Object>of("venta_id", venta.toString(), "turno_id", turno.toString(), "motivo", "Error",
                        "pagos", List.of(Map.of("metodo", "EFECTIVO", "monto", efectivo)));
        return new SobreEvento(UuidV7.generar(cuando), tipo, 1, negocio, "ventas", cuando, venta, null, datos);
    }

    private void publicar(SobreEvento evento) {
        transaccion.executeWithoutResult(estado -> bus.publishEvent(evento));
    }

    private BigDecimal efectivoEsperado(Montaje m) {
        return caja.saldoTurno(m.contexto(), Permisos.TODOS, m.turno()).esperadoPorMetodo().get(MetodoPago.EFECTIVO);
    }

    /** Filas del outbox de ese evento: el listener de cada modulo deja una. Sin filas, vacio. */
    private static List<Boolean> completadas(UUID eventoId) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            return sesion.columna("select completion_date is not null from plataforma.event_publication "
                    + "where serialized_event like '%' || ? || '%'", Boolean.class, eventoId.toString());
        }
    }

    private static void quitarDelOutbox(UUID eventoId) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            sesion.ejecutar("delete from plataforma.event_publication where serialized_event like '%' || ? || '%'",
                    eventoId.toString());
            sesion.confirmar();
        }
    }

    @Test
    @DisplayName("F5: publicar venta.registrada deja el evento en el outbox, Caja lo aplica y el evento se completa")
    void ventaRegistrada_seAplicaYSeCompleta() {
        var m = turnoAbierto();
        var venta = UUID.randomUUID();
        var evento = evento("venta.registrada", m.contexto().negocioId(), m.turno(), venta, "150.00", Instant.now());

        publicar(evento);

        await().atMost(ESPERA).untilAsserted(() -> {
            assertThat(efectivoEsperado(m)).isEqualByComparingTo("650.00");
            assertThat(completadas(evento.id())).isNotEmpty().containsOnly(true);
        });
        assertThat(libro.ventaAplicada(m.contexto().negocioId(), venta).orElseThrow().estado())
                .isEqualTo(EstadoVentaAplicada.APLICADA);
    }

    @Test
    @DisplayName("R3: el mismo evento tres veces, o la misma venta con otro id de evento, deja un solo efecto")
    void mismoEvento_unSoloEfecto() throws Exception {
        var m = turnoAbierto();
        var venta = UUID.randomUUID();
        var evento = evento("venta.registrada", m.contexto().negocioId(), m.turno(), venta, "150.00", Instant.now());

        publicar(evento);
        publicar(evento);
        publicar(evento);
        publicar(evento("venta.registrada", m.contexto().negocioId(), m.turno(), venta, "150.00", Instant.now()));

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            assertThat(efectivoEsperado(m)).isEqualByComparingTo("650.00");
            assertThat(caja.saldoTurno(m.contexto(), Permisos.TODOS, m.turno()).ventasAplicadas()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("R3: la anulacion que llega antes que la venta se marca y la venta tardia no suma")
    void anuladaAntesDeLaVenta_noSuma() {
        var m = turnoAbierto();
        var venta = UUID.randomUUID();
        var anulada = evento("venta.anulada", m.contexto().negocioId(), m.turno(), venta, "150.00", Instant.now());
        publicar(anulada);
        await().atMost(ESPERA).untilAsserted(() -> assertThat(libro.ventaAplicada(m.contexto().negocioId(), venta))
                .hasValueSatisfying(v -> assertThat(v.estado()).isEqualTo(EstadoVentaAplicada.ANULADO_PREVIO)));

        publicar(evento("venta.registrada", m.contexto().negocioId(), m.turno(), venta, "150.00", Instant.now()));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(libro.ventaAplicada(m.contexto().negocioId(), venta))
                .hasValueSatisfying(v -> assertThat(v.estado()).isEqualTo(EstadoVentaAplicada.REVERTIDA)));
        assertThat(efectivoEsperado(m)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("la anulacion de una venta ya aplicada la revierte")
    void anuladaDespuesDeLaVenta_revierte() {
        var m = turnoAbierto();
        var venta = UUID.randomUUID();
        publicar(evento("venta.registrada", m.contexto().negocioId(), m.turno(), venta, "150.00", Instant.now()));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(efectivoEsperado(m)).isEqualByComparingTo("650.00"));

        publicar(evento("venta.anulada", m.contexto().negocioId(), m.turno(), venta, "150.00", Instant.now()));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(efectivoEsperado(m)).isEqualByComparingTo("500.00"));
    }

    @Test
    @DisplayName("el evento de un turno que no existe no se pierde: queda pendiente en el outbox y Caja no lo marca procesado")
    void sinTurnoAbierto_quedaPendiente() throws Exception {
        var negocio = UUID.randomUUID();
        var evento = evento("venta.registrada", negocio, UUID.randomUUID(), UUID.randomUUID(), "10.00", Instant.now());
        try {
            publicar(evento);

            await().atMost(ESPERA).untilAsserted(() -> assertThat(completadas(evento.id())).contains(false));
            await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(4))
                    .untilAsserted(() -> assertThat(completadas(evento.id())).contains(false));
            assertThat(libro.registrarEvento("caja.ventas", evento.id()))
                    .as("el candado del evento no se uso: Caja no lo aplico").isTrue();
        } finally {
            quitarDelOutbox(evento.id());
        }
    }

    @Test
    @DisplayName("si la transaccion se deshace no sale ningun evento: ni en el outbox ni para Caja")
    void transaccionDeshecha_sinEvento() throws Exception {
        var m = turnoAbierto();
        var evento = evento("venta.registrada", m.contexto().negocioId(), m.turno(), UUID.randomUUID(), "150.00", Instant.now());

        transaccion.executeWithoutResult(estado -> {
            bus.publishEvent(evento);
            estado.setRollbackOnly();
        });

        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(completadas(evento.id())).isEmpty();
            assertThat(efectivoEsperado(m)).isEqualByComparingTo("500.00");
        });
    }

    @Test
    @DisplayName("una venta y su anulacion procesadas a la vez no se pisan: Caja las atiende de una en una por venta")
    void ventaYAnulacionSimultaneas_seSerializan() throws Exception {
        var m = turnoAbierto();
        var venta = UUID.randomUUID();
        var negocio = m.contexto().negocioId();
        var enCurso = new CountDownLatch(1);
        var soltar = new CountDownLatch(1);
        var segundoTermino = new AtomicLong();
        var primeroTermino = new AtomicLong();
        var pool = Executors.newFixedThreadPool(2);
        try {
            var primero = pool.submit(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
                libro.ventaAplicada(negocio, venta);
                enCurso.countDown();
                try {
                    soltar.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                primeroTermino.set(System.nanoTime());
            }));
            assertThat(enCurso.await(10, TimeUnit.SECONDS)).isTrue();
            var segundo = pool.submit(() -> {
                new TransactionTemplate(transacciones).executeWithoutResult(estado -> libro.ventaAplicada(negocio, venta));
                segundoTermino.set(System.nanoTime());
            });

            Thread.sleep(700);
            assertThat(segundo.isDone()).as("el segundo espera a que el primero termine con la venta").isFalse();
            soltar.countDown();
            primero.get(10, TimeUnit.SECONDS);
            segundo.get(10, TimeUnit.SECONDS);

            assertThat(segundoTermino.get()).isGreaterThanOrEqualTo(primeroTermino.get());
            // Otra venta no espera.
            libro.ventaAplicada(negocio, UUID.randomUUID());
        } finally {
            soltar.countDown();
            pool.shutdownNow();
        }
    }
}
