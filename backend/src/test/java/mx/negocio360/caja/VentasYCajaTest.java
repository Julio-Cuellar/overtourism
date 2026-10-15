package mx.negocio360.caja;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.caja.aplicacion.AbrirTurnoComando;
import mx.negocio360.caja.aplicacion.CajaServicio;
import mx.negocio360.caja.aplicacion.CerrarTurnoComando;
import mx.negocio360.caja.aplicacion.ConsumidorVentas;
import mx.negocio360.caja.aplicacion.CrearCajaComando;
import mx.negocio360.caja.aplicacion.ResumenVentasDesdeVentasApi;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.fakes.Dobles;
import mx.negocio360.caja.fakes.LibroDeCajaEnMemoria;
import mx.negocio360.caja.fakes.RepositorioCajaEnMemoria;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.pruebas.RepositorioPreparacionesEnMemoria;
import mx.negocio360.ventas.aplicacion.AnularVentaComando;
import mx.negocio360.ventas.aplicacion.RegistrarVentaComando;
import mx.negocio360.ventas.fakes.Escenario;
import mx.negocio360.ventas.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Camino dorado de punta a punta con los dos modulos reales y un bus en memoria: abrir turno, cobrar con cupon, anular
 * otra venta y hacer el corte. Caja usa la API real de Ventas (D-76) y lee sus eventos reales (D-77).
 */
class VentasYCajaTest {

    private final Escenario ventas = new Escenario();
    private final RepositorioCajaEnMemoria cajas = new RepositorioCajaEnMemoria();
    private final LibroDeCajaEnMemoria libro = new LibroDeCajaEnMemoria();
    private final CajaApi caja = CajaServicio.armar(cajas, libro, new RepositorioPreparacionesEnMemoria(),
            new Dobles.Eventos(), new ResumenVentasDesdeVentasApi(ventas.api), new Dobles.Reglas(),
            new Dobles.Autorizacion(), ventas.reloj);
    private final ConsumidorVentas consumidor = new ConsumidorVentas(cajas, libro);
    private final ContextoNegocio cajero = ventas.ctxA;
    private final ContextoNegocio encargado = new ContextoNegocio(ventas.negocioA, "encargado-a");

    @Test
    @DisplayName("abrir turno, cobrar 135.00 con cupon, anular otra venta y cerrar el corte sin diferencia")
    void caminoDorado() {
        ventas.eventos.alPublicar(consumidor::procesar);
        var cajaId = confirmarCaja(cajero, new CrearCajaComando("Taquilla"));
        var turno = confirmarCaja(cajero, new AbrirTurnoComando(cajaId, new BigDecimal("500.00")));

        ventas.vender(cajero, new RegistrarVentaComando(turno, Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.efectivo("135.00", "200.00")), Fixtures.CODIGO_CUPON, null));
        var anulada = ventas.vender(cajero, new RegistrarVentaComando(turno, Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.tarjeta("150.00")), null, null));
        ventas.confirmar(encargado, new AnularVentaComando(anulada, "Cobro duplicado"));

        var saldo = caja.saldoTurno(cajero, turno);
        assertThat(saldo.esperadoPorMetodo()).containsEntry(MetodoPago.EFECTIVO, new BigDecimal("635.00"))
                .containsEntry(MetodoPago.TARJETA, new BigDecimal("0.00"));

        confirmarCaja(cajero, new CerrarTurnoComando(turno,
                Map.of(MetodoPago.EFECTIVO, new BigDecimal("635.00"), MetodoPago.TARJETA, BigDecimal.ZERO), false));

        assertThat(caja.saldoTurno(cajero, turno).estado()).isEqualTo(EstadoTurno.CERRADO);
        assertThat(caja.cortes(cajero)).singleElement()
                .satisfies(c -> assertThat(c.diferenciaEfectivo()).isEqualTo(new BigDecimal("0.00")));
    }

    @Test
    @DisplayName("si un evento de venta no ha llegado a Caja, el corte no cierra (CJ-07)")
    void eventoPerdido_corteNoCierra() {
        var cajaId = confirmarCaja(cajero, new CrearCajaComando("Taquilla"));
        var turno = confirmarCaja(cajero, new AbrirTurnoComando(cajaId, new BigDecimal("500.00")));
        ventas.vender(cajero, new RegistrarVentaComando(turno, Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.efectivo("150.00", null)), null, null));

        var token = caja.preparar(cajero, new CerrarTurnoComando(turno,
                Map.of(MetodoPago.EFECTIVO, new BigDecimal("500.00")), false)).token();

        mx.negocio360.caja.fakes.Escenario.fallaCon(() -> caja.confirmar(cajero, token),
                CodigoError.CORTE_EVENTOS_PENDIENTES);
    }

    private UUID confirmarCaja(ContextoNegocio contexto, Comando comando) {
        return caja.confirmar(contexto, caja.preparar(contexto, comando).token()).recursoId();
    }
}
