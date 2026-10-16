package mx.negocio360.caja.aplicacion;

import mx.negocio360.compartido.pruebas.Permisos;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import static mx.negocio360.caja.fakes.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.eventos.CorteConfirmado;
import mx.negocio360.caja.eventos.DiferenciaDetectada;
import mx.negocio360.caja.fakes.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CF-12 y CF-13: corte de caja (CJ-06 a CJ-10, CJ-12, D-140). */
class CorteTest {

    private final Escenario e = new Escenario();
    private final UUID turno = e.abrirTurno(e.ctxA, "500.00");

    private static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }

    /** Fondo 500 + una venta de 150.00 en efectivo y 100.00 con tarjeta: esperado 650.00 y 100.00. */
    private void conUnaVenta() {
        e.consumidor.procesar(e.ventaRegistrada(e.negocioA, turno, UUID.randomUUID(),
                Map.of("EFECTIVO", "150.00", "TARJETA", "100.00")));
        e.ventas.fijar(turno, 1, 0, 0);
    }

    private CerrarTurnoComando contado(String efectivo, String tarjeta, boolean autorizar) {
        return new CerrarTurnoComando(turno, Map.of(MetodoPago.EFECTIVO, d(efectivo), MetodoPago.TARJETA, d(tarjeta)),
                autorizar);
    }

    @Test
    @DisplayName("CJ-12: el resumen muestra esperado, contado y diferencia; faltan 10.00 en efectivo")
    void preparar_resumenConDiferencia() {
        conUnaVenta();

        var resumen = e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("640.00", "100.00", false)).resumen();

        assertThat(resumen).contains("EFECTIVO 650.00").contains("EFECTIVO 640.00").contains("-10.00");
    }

    @Test
    @DisplayName("D-140: preparar el corte pone el turno EN_CORTE y bloquea movimientos manuales (CJ-10)")
    void preparar_ponerEnCorte() {
        e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("500.00", "0.00", false));

        assertThat(e.api.saldoTurno(e.ctxA, Permisos.de(e.ctxA), turno).estado()).isEqualTo(EstadoTurno.EN_CORTE);
        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), new RegistrarMovimientoCajaComando(turno, TipoMovimientoManual.ENTRADA,
                d("10.00"), "x")), CodigoError.TURNO_EN_CORTE);
    }

    @Test
    @DisplayName("confirmar cierra el turno, guarda el corte y publica caja.corte_confirmado")
    void confirmar_cierraYPublica() {
        conUnaVenta();

        var resultado = e.confirmar(e.ctxA, contado("640.00", "100.00", false));

        assertThat(e.api.saldoTurno(e.ctxA, Permisos.de(e.ctxA), turno).estado()).isEqualTo(EstadoTurno.CERRADO);
        assertThat(e.api.cortes(e.ctxA, Permisos.TODOS, null, 20, null).elementos()).singleElement().satisfies(c -> {
            assertThat(c.diferenciaEfectivo()).isEqualTo(d("-10.00"));
            assertThat(c.esperadoPorMetodo()).containsEntry(MetodoPago.EFECTIVO, d("650.00"));
            assertThat(c.autorizadoPor()).isNull();
        });
        assertThat(resultado.datos()).containsEntry("diferencia_efectivo", "-10.00");
        var evento = e.eventos.deTipo(CorteConfirmado.TIPO);
        assertThat(evento).singleElement().satisfies(ev -> assertThat(ev.datos())
                .containsEntry("turno_id", turno.toString()).containsEntry("diferencia_efectivo", "-10.00")
                .containsKeys("corte_id", "caja_id", "esperado_por_metodo", "contado_por_metodo", "abierto_en",
                        "cerrado_en"));
        assertThat(e.eventos.deTipo(DiferenciaDetectada.TIPO)).isEmpty();
        assertThat(e.api.turnoActivo(e.ctxA, Permisos.de(e.ctxA), e.api.saldoTurno(e.ctxA, Permisos.de(e.ctxA), turno).cajaId())).isEmpty();
    }

    @Test
    @DisplayName("CJ-07: con ventas preparadas sin confirmar, el corte no cierra (CORTE_VENTAS_PENDIENTES)")
    void confirmar_ventasPendientes() {
        e.ventas.fijar(turno, 0, 0, 1);
        var token = e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("500.00", "0.00", false)).token();

        fallaCon(() -> e.api.confirmar(e.ctxA, Permisos.de(e.ctxA), token), CodigoError.CORTE_VENTAS_PENDIENTES);
        assertThat(e.api.saldoTurno(e.ctxA, Permisos.de(e.ctxA), turno).estado()).isEqualTo(EstadoTurno.EN_CORTE);
    }

    @Test
    @DisplayName("CJ-07: si Caja aun no aplica todas las ventas que reporta Ventas, falla con CORTE_EVENTOS_PENDIENTES")
    void confirmar_eventosPendientes() {
        e.ventas.fijar(turno, 1, 0, 0);
        var token = e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("500.00", "0.00", false)).token();

        fallaCon(() -> e.api.confirmar(e.ctxA, Permisos.de(e.ctxA), token), CodigoError.CORTE_EVENTOS_PENDIENTES);
    }

    @Test
    @DisplayName("CJ-08: una diferencia mayor al umbral pide autorizacion de un encargado")
    void diferenciaSobreUmbral_pideAutorizacion() {
        conUnaVenta();

        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("590.00", "100.00", false)),
                CodigoError.CORTE_REQUIERE_AUTORIZACION);
        // Un gerente que no marca «autorizar» tampoco cierra con esa diferencia.
        fallaCon(() -> e.api.preparar(e.encargadoA, Permisos.TODOS, contado("590.00", "100.00", false)),
                CodigoError.CORTE_REQUIERE_AUTORIZACION);
    }

    @Test
    @DisplayName("CJ-08 y D-127: pedir autorizar sin caja.corte.autorizar falla con PERMISO_DENEGADO, al preparar y al confirmar")
    void autorizar_sinPermiso_permisoDenegado() {
        conUnaVenta();

        assertThatThrownBy(() -> e.api.preparar(e.ctxA, Permisos.CAJERO, contado("590.00", "100.00", true)))
                .isInstanceOf(PermisoDenegadoException.class);
        var token = e.api.preparar(e.encargadoA, Permisos.TODOS, contado("590.00", "100.00", true)).token();
        assertThatThrownBy(() -> e.api.confirmar(e.encargadoA, Permisos.todosMenos("caja.corte.autorizar"), token))
                .isInstanceOf(PermisoDenegadoException.class);
        e.api.confirmar(e.encargadoA, Permisos.TODOS, token);
    }

    @Test
    @DisplayName("CJ-08: con autorizacion del encargado cierra, anota quien autorizo y avisa la diferencia")
    void diferenciaSobreUmbral_conEncargado_cierra() {
        conUnaVenta();

        e.confirmar(e.encargadoA, contado("590.00", "100.00", true));

        assertThat(e.api.cortes(e.ctxA, Permisos.TODOS, null, 20, null).elementos()).singleElement()
                .satisfies(c -> assertThat(c.autorizadoPor()).isEqualTo("encargado-a"));
        assertThat(e.eventos.deTipo(DiferenciaDetectada.TIPO)).singleElement().satisfies(ev -> assertThat(ev.datos())
                .containsEntry("diferencia", "-60.00").containsEntry("umbral", "50.00"));
    }

    @Test
    @DisplayName("CJ-08: la diferencia de exactamente el umbral no pide autorizacion")
    void diferenciaIgualAlUmbral_noPide() {
        conUnaVenta();

        e.confirmar(e.ctxA, contado("600.00", "100.00", false));

        assertThat(e.api.saldoTurno(e.ctxA, Permisos.de(e.ctxA), turno).estado()).isEqualTo(EstadoTurno.CERRADO);
    }

    @Test
    @DisplayName("CJ-09: un turno cerrado no se vuelve a cortar ni acepta movimientos")
    void turnoCerrado_inmutable() {
        e.confirmar(e.ctxA, contado("500.00", "0.00", false));

        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("500.00", "0.00", false)), CodigoError.TURNO_NO_ABIERTO);
        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), new RegistrarMovimientoCajaComando(turno, TipoMovimientoManual.ENTRADA,
                d("10.00"), "x")), CodigoError.TURNO_NO_ABIERTO);
    }

    @Test
    @DisplayName("si llega una venta entre preparar y confirmar, el esperado cambio: COMANDO_CAMBIO")
    void ventaEntrePasos_comandoCambio() {
        var token = e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("500.00", "0.00", false)).token();
        conUnaVenta();

        fallaCon(() -> e.api.confirmar(e.ctxA, Permisos.de(e.ctxA), token), CodigoError.COMANDO_CAMBIO);
    }

    @Test
    @DisplayName("CF-13: descartar el corte regresa el turno a ABIERTO")
    void descartar_regresaAAbierto() {
        e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("500.00", "0.00", false));

        e.confirmar(e.ctxA, new DescartarCorteComando(turno));

        assertThat(e.api.saldoTurno(e.ctxA, Permisos.de(e.ctxA), turno).estado()).isEqualTo(EstadoTurno.ABIERTO);
    }

    @Test
    @DisplayName("D-140: si el corte preparado vence sin confirmar, el turno vuelve a operar como ABIERTO")
    void corteVencido_vuelveAAbierto() {
        e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("500.00", "0.00", false));
        e.reloj.avanzar(Duration.ofMinutes(5));

        assertThat(e.api.saldoTurno(e.ctxA, Permisos.de(e.ctxA), turno).estado()).isEqualTo(EstadoTurno.ABIERTO);
        e.confirmar(e.ctxA, new RegistrarMovimientoCajaComando(turno, TipoMovimientoManual.ENTRADA, d("10.00"), "x"));
    }

    @Test
    @DisplayName("un conteo negativo o con mas de 2 decimales falla con CONTEO_INVALIDO")
    void conteoInvalido() {
        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("-1.00", "0.00", false)), CodigoError.CONTEO_INVALIDO);
        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("1.001", "0.00", false)), CodigoError.CONTEO_INVALIDO);
        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), new CerrarTurnoComando(turno, null, false)),
                CodigoError.CONTEO_INVALIDO);
    }

    @Test
    @DisplayName("si otra peticion cambio el turno al cerrar, falla con CONCURRENCIA y no publica")
    void confirmar_concurrencia() {
        var token = e.api.preparar(e.ctxA, Permisos.de(e.ctxA), contado("500.00", "0.00", false)).token();
        e.cajas.simularConcurrencia = true;

        fallaCon(() -> e.api.confirmar(e.ctxA, Permisos.de(e.ctxA), token), CodigoError.CONCURRENCIA);
        assertThat(e.eventos.deTipo(CorteConfirmado.TIPO)).isEmpty();
    }
}
