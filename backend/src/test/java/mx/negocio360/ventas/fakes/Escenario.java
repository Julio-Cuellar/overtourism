package mx.negocio360.ventas.fakes;

import mx.negocio360.compartido.pruebas.Permisos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.pruebas.RelojMutable;
import mx.negocio360.compartido.pruebas.RepositorioPreparacionesEnMemoria;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.aplicacion.VentasServicio;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.VentasException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Arma el modulo de Ventas con dobles en memoria, un reloj controlado y los productos de ejemplo cargados. */
public final class Escenario {

    public final RelojMutable reloj = new RelojMutable();
    public final UUID negocioA = UUID.randomUUID();
    public final UUID negocioB = UUID.randomUUID();
    public final ContextoNegocio ctxA = new ContextoNegocio(negocioA, "cajero-a");
    public final ContextoNegocio ctxB = new ContextoNegocio(negocioB, "cajero-b");

    public final CatalogoEnMemoria catalogo = new CatalogoEnMemoria();
    public final CuponesEnMemoria cupones = new CuponesEnMemoria();
    public final RepositorioVentasEnMemoria ventas = new RepositorioVentasEnMemoria();
    public final RepositorioPreparacionesEnMemoria preparaciones = new RepositorioPreparacionesEnMemoria();
    public final PublicadorEventosEnMemoria eventos = new PublicadorEventosEnMemoria();
    public final TurnosEnMemoria turnos = new TurnosEnMemoria();

    public final VentasApi api;

    public Escenario() {
        for (var negocio : List.of(negocioA, negocioB)) {
            turnos.abrir(negocio, Fixtures.TURNO);
            catalogo.con(negocio, Fixtures.boleto()).con(negocio, Fixtures.camote()).con(negocio, Fixtures.inactivo());
        }
        cupones.porcentaje(Fixtures.CODIGO_CUPON, Fixtures.CUPON, "10");
        api = VentasServicio.armar(catalogo, cupones, turnos, ventas, preparaciones, eventos, reloj);
    }

    /** Prepara y confirma un comando, como lo haria el panel en dos pasos. */
    public ResultadoComando confirmar(ContextoNegocio contexto, Comando comando) {
        return api.confirmar(contexto, Permisos.de(contexto), api.preparar(contexto, Permisos.de(contexto), comando).token());
    }

    /** Registra una venta y devuelve su id. */
    public UUID vender(ContextoNegocio contexto, Comando comando) {
        return confirmar(contexto, comando).recursoId();
    }

    /** Exige que la accion falle con VentasException y el codigo esperado, y devuelve la excepcion. */
    public static VentasException fallaCon(ThrowingCallable accion, CodigoError esperado) {
        var capturada = catchThrowable(accion);
        assertThat(capturada).isInstanceOf(VentasException.class);
        var excepcion = (VentasException) capturada;
        assertThat(excepcion.codigo()).isEqualTo(esperado);
        return excepcion;
    }
}
