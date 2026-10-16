package mx.negocio360.inventario.aplicacion;

import static mx.negocio360.inventario.fakes.Afirmaciones.fallaCon;
import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.Saldo;
import mx.negocio360.inventario.fakes.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Revision de codigo M2: el saldo que se compara con el resumen es el mismo que respalda la escritura. */
class CarreraEnConfirmarTest {

    /** Saldos que, tras la primera lectura, dejan correr una accion: otra peticion que mueve el saldo. */
    private static final class SaldosConCarrera implements RepositorioSaldos {
        private final RepositorioSaldos real;
        private Runnable trasLaPrimeraLectura;

        SaldosConCarrera(RepositorioSaldos real) {
            this.real = real;
        }

        @Override
        public java.util.Optional<Saldo> porArticulo(UUID negocioId, UUID articuloId) {
            var lectura = real.porArticulo(negocioId, articuloId);
            var accion = trasLaPrimeraLectura;
            trasLaPrimeraLectura = null;
            if (accion != null) {
                accion.run();
            }
            return lectura;
        }

        @Override
        public void insertar(Saldo saldo) {
            real.insertar(saldo);
        }

        @Override
        public void actualizar(Saldo nuevo, long versionEsperada) {
            real.actualizar(nuevo, versionEsperada);
        }
    }

    @Test
    @DisplayName("si el saldo se mueve justo despues de leerlo al confirmar, la escritura falla con CONCURRENCIA")
    void confirmar_saldoSeMueveTrasLeerlo_concurrencia() {
        var e = new Escenario();
        var espia = new SaldosConCarrera(e.almacen);
        var ids = new SecuenciaDeIds();
        var manejadores = Manejadores.todos(e.almacen, espia, e.almacen, e.eventos, e.reloj, ids);
        var consumo = new ConsumosServicio(e.almacen, espia, e.almacen, e.consumos, e.eventos, e.reloj, ids);
        var api = new InventarioServicio(new ServicioPreparacion(e.preparaciones, manejadores, e.reloj), e.almacen,
                espia, e.almacen, consumo);
        var harina = api.confirmar(e.ctxA, api.preparar(e.ctxA, new CrearArticuloComando("Harina", null, "g", null,
                null, null)).token()).recursoId();
        var token = api.preparar(e.ctxA, new RegistrarEntradaComando(harina, d("1"), "kg", d("20"), null)).token();
        espia.trasLaPrimeraLectura = () -> {
            var actual = e.almacen.porArticulo(e.negocioA, harina).orElseThrow();
            e.almacen.actualizar(actual.conSalida(d("100"), UUID.randomUUID()), actual.version());
        };

        fallaCon(() -> api.confirmar(e.ctxA, token), CodigoError.CONCURRENCIA);

        assertThat(e.almacen.movimientos()).isEmpty();
    }
}
