package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.util.UUID;
import mx.negocio360.compartido.comandos.RepositorioPreparacionesJdbc;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.pruebas.Permisos;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import mx.negocio360.compartido.transaccion.FronteraTransaccional;
import mx.negocio360.ventas.FiltroVentas;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.aplicacion.VentasServicio;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.VentasException;
import mx.negocio360.ventas.fakes.CatalogoEnMemoria;
import mx.negocio360.ventas.fakes.CuponesEnMemoria;
import mx.negocio360.ventas.fakes.Fixtures;
import mx.negocio360.ventas.fakes.PublicadorEventosEnMemoria;
import mx.negocio360.ventas.fakes.TurnosEnMemoria;
import mx.negocio360.ventas.infraestructura.RepositorioVentasJdbc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;

/**
 * D-103: cada llamada a la API de Ventas es una sola transaccion, que incluye la venta, sus lineas, sus pagos, el folio y
 * sus eventos. Se prueba con los repositorios JDBC reales y un publicador que puede fallar: si el evento no se puede
 * publicar, ni la venta ni el folio quedan; y el token se gasta aunque la venta falle.
 */
@SpringBootTest
class VentasTransaccionPostgresTest {

    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private PlatformTransactionManager transacciones;
    @Autowired
    private TransactionOperations operaciones;

    private final UUID negocio = UUID.randomUUID();
    private final ContextoNegocio contexto = new ContextoNegocio(negocio, "cajero-a");
    private final PublicadorEventosEnMemoria eventos = new PublicadorEventosEnMemoria();
    private boolean fallarAlPublicar;

    private RepositorioVentasJdbc repositorio() {
        return new RepositorioVentasJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones));
    }

    private VentasApi api() {
        var catalogo = new CatalogoEnMemoria().con(negocio, Fixtures.boleto()).con(negocio, Fixtures.camote());
        var turnos = new TurnosEnMemoria().abrir(negocio, Fixtures.TURNO);
        var servicio = VentasServicio.armar(catalogo, new CuponesEnMemoria(), turnos, repositorio(),
                new RepositorioPreparacionesJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones), "ventas"),
                evento -> {
                    if (fallarAlPublicar) {
                        throw new IllegalStateException("el outbox no responde");
                    }
                    eventos.publicar(evento);
                }, Clock.systemUTC());
        return FronteraTransaccional.envolver(VentasApi.class, servicio, operaciones);
    }

    @Test
    @DisplayName("una venta confirmada deja la venta, el folio 1 y el evento, todo en una transaccion")
    void ventaConfirmada_dejaTodo() {
        var api = api();

        var token = api.preparar(contexto, Permisos.TODOS, Fixtures.ventaSinCupon()).token();
        var resultado = api.confirmar(contexto, Permisos.TODOS, token);

        assertThat(resultado.recursoId()).isNotNull();
        assertThat(repositorio().porId(negocio, resultado.recursoId())).isPresent();
        assertThat(eventos.todos()).hasSize(1);
        assertThat(repositorio().siguienteFolio(negocio, "A")).as("el folio 1 ya se uso").isEqualTo(2);
    }

    @Test
    @DisplayName("si el evento no se puede publicar no queda la venta, ni el folio: el siguiente cobro recibe el folio 1")
    void falloAlPublicar_noDejaNada() {
        var api = api();
        var token = api.preparar(contexto, Permisos.TODOS, Fixtures.ventaSinCupon()).token();
        fallarAlPublicar = true;

        assertThatThrownBy(() -> api.confirmar(contexto, Permisos.TODOS, token))
                .isInstanceOf(IllegalStateException.class);

        assertThat(repositorio().buscar(negocio, FiltroVentas.TODAS, 10, null).elementos()).isEmpty();
        fallarAlPublicar = false;
        var segunda = api.confirmar(contexto, Permisos.TODOS,
                api.preparar(contexto, Permisos.TODOS, Fixtures.ventaSinCupon()).token());
        assertThat(repositorio().porId(negocio, segunda.recursoId()).orElseThrow().folio()).isEqualTo(1);
    }

    @Test
    @DisplayName("el token se gasta aunque la venta falle: una segunda confirmacion es TOKEN_USADO, no un reintento")
    void tokenGastadoAunqueFalle() {
        var api = api();
        var token = api.preparar(contexto, Permisos.TODOS, Fixtures.ventaSinCupon()).token();
        fallarAlPublicar = true;
        assertThatThrownBy(() -> api.confirmar(contexto, Permisos.TODOS, token)).isInstanceOf(IllegalStateException.class);
        fallarAlPublicar = false;

        assertThatThrownBy(() -> api.confirmar(contexto, Permisos.TODOS, token))
                .isInstanceOfSatisfying(VentasException.class, e -> assertThat(e.codigo()).isEqualTo(CodigoError.TOKEN_USADO));
    }

    @Test
    @DisplayName("las consultas de la API tambien corren con el negocio fijado: otro negocio no ve la venta")
    void consultaAislada() {
        var api = api();
        var resultado = api.confirmar(contexto, Permisos.TODOS,
                api.preparar(contexto, Permisos.TODOS, Fixtures.ventaSinCupon()).token());

        assertThat(api.venta(contexto, Permisos.TODOS, resultado.recursoId())).isPresent();
        assertThat(api.venta(new ContextoNegocio(UUID.randomUUID(), "cajero-b"), Permisos.TODOS, resultado.recursoId()))
                .isEmpty();
    }
}
