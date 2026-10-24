package mx.negocio360.caja.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.caja.FiltroCortes;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.EstadoVentaAplicada;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.TipoMovimiento;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.caja.dominio.VentaAplicada;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato de {@link RepositorioCaja} y {@link LibroDeCaja}: lo que debe cumplir cualquier almacen de Caja, en memoria o
 * en PostgreSQL. Cada subclase aporta el almacen; la bateria es la misma (F6). Cada prueba usa su propio negocio.
 */
public abstract class ContratoAlmacenDeCajaTest {

    protected static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");

    /** El repositorio de cajas y turnos bajo prueba. */
    protected abstract RepositorioCaja cajas();

    /** El libro de movimientos, ventas aplicadas, eventos y cortes bajo prueba. */
    protected abstract LibroDeCaja libro();

    private static Caja caja(UUID negocio, String nombre, Instant cuando) {
        return new Caja(UuidV7.generar(cuando), negocio, nombre, true);
    }

    private static Turno turno(UUID negocio, UUID caja, Instant cuando) {
        return new Turno(UuidV7.generar(cuando), negocio, caja, "cajero-a", EstadoTurno.ABIERTO, new BigDecimal("500.00"),
                cuando, null, null, 1);
    }

    /** Una caja y un turno abierto en ella, ya guardados. */
    protected Turno turnoAbierto(UUID negocio) {
        var caja = caja(negocio, "Mostrador " + UUID.randomUUID(), AHORA);
        cajas().guardarCaja(caja);
        var turno = turno(negocio, caja.id(), AHORA);
        cajas().guardarTurno(turno);
        return turno;
    }

    private static MovimientoCaja movimiento(Turno turno, TipoMovimiento tipo, MetodoPago metodo, String monto, int segundo) {
        var cuando = AHORA.plusSeconds(segundo);
        return new MovimientoCaja(UuidV7.generar(cuando), turno.negocioId(), turno.id(), tipo, metodo, new BigDecimal(monto),
                null, null, null, cuando, "cajero-a");
    }

    // ---- cajas ----

    @Test
    @DisplayName("una caja guardada se lee igual y se lista en orden de alta, solo del negocio")
    void cajaGuardarLeerYListar() {
        var negocio = UUID.randomUUID();
        var primera = caja(negocio, "Mostrador", AHORA);
        var segunda = new Caja(UuidV7.generar(AHORA.plusSeconds(60)), negocio, "Taquilla", false);
        cajas().guardarCaja(primera);
        cajas().guardarCaja(segunda);
        cajas().guardarCaja(caja(UUID.randomUUID(), "Mostrador", AHORA));

        assertThat(cajas().caja(negocio, primera.id())).contains(primera);
        assertThat(cajas().caja(negocio, segunda.id())).contains(segunda);
        assertThat(cajas().cajas(negocio)).containsExactly(primera, segunda);
        assertThat(cajas().caja(UUID.randomUUID(), primera.id())).isEmpty();
        assertThat(cajas().caja(negocio, UUID.randomUUID())).isEmpty();
    }

    // ---- turnos ----

    @Test
    @DisplayName("un turno guardado se lee igual; uno de otro negocio no se ve")
    void turnoGuardarYLeer() {
        var negocio = UUID.randomUUID();
        var original = turnoAbierto(negocio);

        assertThat(cajas().turno(negocio, original.id())).contains(original);
        assertThat(cajas().turno(UUID.randomUUID(), original.id())).isEmpty();
        assertThat(cajas().turno(negocio, UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("CJ-01: el turno no cerrado de una caja es el abierto o el que esta en corte; uno cerrado ya no cuenta")
    void turnoNoCerrado() {
        var negocio = UUID.randomUUID();
        var abierto = turnoAbierto(negocio);
        assertThat(cajas().turnoNoCerradoDeCaja(negocio, abierto.cajaId())).contains(abierto);

        var enCorte = abierto.enCorte(AHORA.plusSeconds(600));
        assertThat(cajas().actualizarTurno(enCorte, 1)).isTrue();
        assertThat(cajas().turnoNoCerradoDeCaja(negocio, abierto.cajaId())).contains(enCorte);
        assertThat(cajas().turno(negocio, abierto.id())).contains(enCorte);

        var cerrado = enCorte.cerrado(AHORA.plusSeconds(900));
        assertThat(cajas().actualizarTurno(cerrado, 2)).isTrue();
        assertThat(cajas().turnoNoCerradoDeCaja(negocio, abierto.cajaId())).isEmpty();
        assertThat(cajas().turno(negocio, abierto.id())).contains(cerrado);
        assertThat(cajas().turnoNoCerradoDeCaja(UUID.randomUUID(), abierto.cajaId())).isEmpty();
    }

    @Test
    @DisplayName("tras cerrar un turno, la misma caja puede abrir otro")
    void otroTurnoTrasCerrar() {
        var negocio = UUID.randomUUID();
        var primero = turnoAbierto(negocio);
        cajas().actualizarTurno(primero.cerrado(AHORA.plusSeconds(60)), 1);

        var segundo = turno(negocio, primero.cajaId(), AHORA.plusSeconds(120));
        cajas().guardarTurno(segundo);

        assertThat(cajas().turnoNoCerradoDeCaja(negocio, primero.cajaId())).contains(segundo);
    }

    @Test
    @DisplayName("actualizarTurno con una version vieja, o de otro negocio, no cambia nada y responde falso")
    void actualizarTurnoVersionVieja() {
        var negocio = UUID.randomUUID();
        var original = turnoAbierto(negocio);
        assertThat(cajas().actualizarTurno(original.enCorte(AHORA.plusSeconds(60)), 1)).isTrue();

        assertThat(cajas().actualizarTurno(original.cerrado(AHORA.plusSeconds(120)), 1)).isFalse();
        var ajeno = new Turno(original.id(), UUID.randomUUID(), original.cajaId(), "x", EstadoTurno.CERRADO,
                original.fondoInicial(), AHORA, AHORA, null, 3);
        assertThat(cajas().actualizarTurno(ajeno, 2)).isFalse();
        assertThat(cajas().turno(negocio, original.id()).orElseThrow().estado()).isEqualTo(EstadoTurno.EN_CORTE);
        assertThat(cajas().actualizarTurno(turno(negocio, original.cajaId(), AHORA).cerrado(AHORA), 1)).isFalse();
    }

    // ---- movimientos ----

    @Test
    @DisplayName("los movimientos se leen en orden de registro, con todos sus datos, y solo del turno y del negocio")
    void movimientos() {
        var negocio = UUID.randomUUID();
        var turno = turnoAbierto(negocio);
        var otroTurno = turnoAbierto(negocio);
        var fondo = movimiento(turno, TipoMovimiento.FONDO, MetodoPago.EFECTIVO, "500.00", 0);
        var venta = new MovimientoCaja(UuidV7.generar(AHORA.plusSeconds(5)), negocio, turno.id(), TipoMovimiento.VENTA,
                MetodoPago.TARJETA, new BigDecimal("150.00"), UUID.randomUUID(), UUID.randomUUID(), null,
                AHORA.plusSeconds(5), null);
        var retiro = new MovimientoCaja(UuidV7.generar(AHORA.plusSeconds(9)), negocio, turno.id(), TipoMovimiento.RETIRO,
                MetodoPago.EFECTIVO, new BigDecimal("-200.00"), null, null, "Pago a proveedor", AHORA.plusSeconds(9),
                "gerente-a");
        libro().agregar(fondo);
        libro().agregar(venta);
        libro().agregar(retiro);
        libro().agregar(movimiento(otroTurno, TipoMovimiento.FONDO, MetodoPago.EFECTIVO, "100.00", 1));

        assertThat(libro().movimientos(negocio, turno.id())).containsExactly(fondo, venta, retiro);
        assertThat(libro().movimientos(UUID.randomUUID(), turno.id())).isEmpty();
    }

    @Test
    @DisplayName("los movimientos se paginan con cursor sin repetir ni saltar")
    void movimientosPaginados() {
        var negocio = UUID.randomUUID();
        var turno = turnoAbierto(negocio);
        var esperados = new ArrayList<UUID>();
        for (int i = 0; i < 7; i++) {
            var m = movimiento(turno, TipoMovimiento.ENTRADA, MetodoPago.EFECTIVO, "10.00", i);
            libro().agregar(m);
            esperados.add(m.id());
        }

        var vistos = new ArrayList<UUID>();
        String cursor = null;
        boolean hayMas;
        do {
            var pagina = libro().movimientos(negocio, turno.id(), 3, cursor);
            pagina.elementos().forEach(m -> vistos.add(m.id()));
            cursor = pagina.cursorFinal();
            hayMas = pagina.hayMas();
        } while (hayMas);

        assertThat(vistos).containsExactlyElementsOf(esperados);
        assertThat(cursor).isNull();
    }

    // ---- ventas aplicadas y eventos ----

    @Test
    @DisplayName("venta_aplicada: se guarda, se reemplaza y se cuenta por turno y estado, solo del negocio")
    void ventasAplicadas() {
        var negocio = UUID.randomUUID();
        var turno = turnoAbierto(negocio);
        var venta1 = UUID.randomUUID();
        var venta2 = UUID.randomUUID();
        libro().guardarVentaAplicada(new VentaAplicada(negocio, venta1, turno.id(), EstadoVentaAplicada.APLICADA));
        libro().guardarVentaAplicada(new VentaAplicada(negocio, venta2, turno.id(), EstadoVentaAplicada.APLICADA));
        libro().guardarVentaAplicada(new VentaAplicada(UUID.randomUUID(), venta1, turno.id(), EstadoVentaAplicada.APLICADA));

        assertThat(libro().contarVentas(negocio, turno.id(), EstadoVentaAplicada.APLICADA)).isEqualTo(2);
        assertThat(libro().ventaAplicada(negocio, venta1)).contains(
                new VentaAplicada(negocio, venta1, turno.id(), EstadoVentaAplicada.APLICADA));

        libro().guardarVentaAplicada(new VentaAplicada(negocio, venta1, turno.id(), EstadoVentaAplicada.REVERTIDA));

        assertThat(libro().ventaAplicada(negocio, venta1).orElseThrow().estado()).isEqualTo(EstadoVentaAplicada.REVERTIDA);
        assertThat(libro().contarVentas(negocio, turno.id(), EstadoVentaAplicada.APLICADA)).isEqualTo(1);
        assertThat(libro().contarVentas(negocio, turno.id(), EstadoVentaAplicada.REVERTIDA)).isEqualTo(1);
        assertThat(libro().ventaAplicada(UUID.randomUUID(), venta2)).isEmpty();
        assertThat(libro().ventaAplicada(negocio, UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("una marca de anulacion previa puede no tener turno")
    void anuladoPrevioSinTurno() {
        var negocio = UUID.randomUUID();
        var venta = UUID.randomUUID();

        libro().guardarVentaAplicada(new VentaAplicada(negocio, venta, null, EstadoVentaAplicada.ANULADO_PREVIO));

        assertThat(libro().ventaAplicada(negocio, venta)).contains(
                new VentaAplicada(negocio, venta, null, EstadoVentaAplicada.ANULADO_PREVIO));
    }

    @Test
    @DisplayName("R3: un evento se procesa una sola vez por consumidor")
    void eventosProcesados() {
        var evento = UuidV7.generar(AHORA);

        assertThat(libro().registrarEvento("caja.ventas", evento)).isTrue();
        assertThat(libro().registrarEvento("caja.ventas", evento)).isFalse();
        assertThat(libro().registrarEvento("otro.consumidor", evento)).isTrue();
        assertThat(libro().registrarEvento("caja.ventas", UuidV7.generar(AHORA.plusSeconds(1)))).isTrue();
    }

    // ---- cortes ----

    private Corte corte(Turno turno, Instant confirmado, String diferencia) {
        Map<MetodoPago, BigDecimal> esperado = new EnumMap<>(MetodoPago.class);
        esperado.put(MetodoPago.EFECTIVO, new BigDecimal("620.50"));
        esperado.put(MetodoPago.TARJETA, new BigDecimal("150.00"));
        esperado.put(MetodoPago.TRANSFERENCIA, new BigDecimal("0.00"));
        Map<MetodoPago, BigDecimal> contado = new EnumMap<>(MetodoPago.class);
        contado.put(MetodoPago.EFECTIVO, new BigDecimal("620.00"));
        contado.put(MetodoPago.TARJETA, new BigDecimal("150.00"));
        contado.put(MetodoPago.TRANSFERENCIA, new BigDecimal("0.00"));
        return new Corte(UuidV7.generar(confirmado), turno.negocioId(), turno.id(), turno.cajaId(), esperado, contado,
                new BigDecimal(diferencia), "gerente-a", turno.abiertoEn(), confirmado);
    }

    @Test
    @DisplayName("un corte guardado se lee igual, con sus montos por metodo y la diferencia")
    void corteGuardarYLeer() {
        var negocio = UUID.randomUUID();
        var turno = turnoAbierto(negocio);
        var original = corte(turno, AHORA.plusSeconds(3600), "-0.50");

        libro().guardarCorte(original);

        var pagina = libro().cortes(negocio, FiltroCortes.TODOS, 10, null);
        assertThat(pagina.elementos()).containsExactly(original);
        assertThat(libro().cortes(UUID.randomUUID(), FiltroCortes.TODOS, 10, null).elementos()).isEmpty();
    }

    @Test
    @DisplayName("los cortes se filtran por caja y fechas, del mas reciente al mas antiguo, y se paginan")
    void cortesFiltradosYPaginados() {
        var negocio = UUID.randomUUID();
        var turnos = new ArrayList<Turno>();
        for (int i = 0; i < 5; i++) {
            var turno = turnoAbierto(negocio);
            turnos.add(turno);
            libro().guardarCorte(corte(turno, AHORA.plusSeconds(3600L * (i + 1)), "0.00"));
        }

        var todos = libro().cortes(negocio, FiltroCortes.TODOS, 100, null).elementos();
        assertThat(todos).extracting(Corte::turnoId).containsExactly(turnos.get(4).id(), turnos.get(3).id(),
                turnos.get(2).id(), turnos.get(1).id(), turnos.get(0).id());
        assertThat(libro().cortes(negocio, new FiltroCortes(turnos.get(2).cajaId(), null, null), 100, null).elementos())
                .extracting(Corte::turnoId).containsExactly(turnos.get(2).id());
        assertThat(libro().cortes(negocio, new FiltroCortes(null, AHORA.plusSeconds(7200), AHORA.plusSeconds(10800)), 100,
                null).elementos()).extracting(Corte::turnoId).containsExactly(turnos.get(2).id(), turnos.get(1).id());

        var vistos = new ArrayList<UUID>();
        String cursor = null;
        boolean hayMas;
        do {
            var pagina = libro().cortes(negocio, FiltroCortes.TODOS, 2, cursor);
            pagina.elementos().forEach(c -> vistos.add(c.turnoId()));
            cursor = pagina.cursorFinal();
            hayMas = pagina.hayMas();
        } while (hayMas);
        assertThat(vistos).containsExactlyElementsOf(todos.stream().map(Corte::turnoId).toList());
    }

    /** Lista de los metodos de pago de Caja, para que la prueba falle si se agrega uno sin cubrirlo. */
    @Test
    @DisplayName("los tres metodos de pago de Caja cruzan el almacen")
    void losTresMetodos() {
        var negocio = UUID.randomUUID();
        var turno = turnoAbierto(negocio);
        List<MetodoPago> metodos = List.of(MetodoPago.values());
        for (int i = 0; i < metodos.size(); i++) {
            libro().agregar(movimiento(turno, TipoMovimiento.ENTRADA, metodos.get(i), "1.00", i));
        }

        assertThat(libro().movimientos(negocio, turno.id())).extracting(MovimientoCaja::metodo)
                .containsExactlyElementsOf(metodos);
    }
}
