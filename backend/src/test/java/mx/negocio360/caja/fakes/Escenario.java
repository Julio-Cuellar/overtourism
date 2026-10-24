package mx.negocio360.caja.fakes;

import mx.negocio360.compartido.pruebas.Permisos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.caja.CajaApi;
import mx.negocio360.caja.aplicacion.AbrirTurnoComando;
import mx.negocio360.caja.aplicacion.CajaServicio;
import mx.negocio360.caja.aplicacion.ConsumidorVentas;
import mx.negocio360.caja.aplicacion.CrearCajaComando;
import mx.negocio360.caja.dominio.CajaException;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.pruebas.RelojMutable;
import mx.negocio360.compartido.pruebas.RepositorioPreparacionesEnMemoria;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Arma el modulo de Caja con dobles en memoria y un reloj controlado. Datos de ejemplo inventados. */
public final class Escenario {

    public final RelojMutable reloj = new RelojMutable();
    public final UUID negocioA = UUID.randomUUID();
    public final UUID negocioB = UUID.randomUUID();
    public final ContextoNegocio ctxA = new ContextoNegocio(negocioA, "cajero-a");
    public final ContextoNegocio encargadoA = new ContextoNegocio(negocioA, "encargado-a");
    public final ContextoNegocio ctxB = new ContextoNegocio(negocioB, "cajero-b");

    public final RepositorioCajaEnMemoria cajas = new RepositorioCajaEnMemoria();
    public final LibroDeCajaEnMemoria libro = new LibroDeCajaEnMemoria();
    public final RepositorioPreparacionesEnMemoria preparaciones = new RepositorioPreparacionesEnMemoria();
    public final Dobles.Eventos eventos = new Dobles.Eventos();
    public final Dobles.Ventas ventas = new Dobles.Ventas();
    public final Dobles.Reglas reglas = new Dobles.Reglas();

    public final CajaApi api;
    public final ConsumidorVentas consumidor;

    public Escenario() {
        api = CajaServicio.armar(cajas, libro, preparaciones, eventos, ventas, reglas, reloj);
        consumidor = new ConsumidorVentas(cajas, libro);
    }

    /** Prepara y confirma un comando, como lo haria el panel en dos pasos. */
    public ResultadoComando confirmar(ContextoNegocio contexto, Comando comando) {
        return api.confirmar(contexto, Permisos.de(contexto), api.preparar(contexto, Permisos.de(contexto), comando).token());
    }

    /** Prepara y confirma un comando con los permisos que se le den, para probar quien puede y quien no. */
    public ResultadoComando confirmar(ContextoNegocio contexto, mx.negocio360.compartido.seguridad.Autorizacion permisos,
                                      Comando comando) {
        return api.confirmar(contexto, permisos, api.preparar(contexto, permisos, comando).token());
    }

    /** Crear cajas es del administrador y el dueno (D-127): el montaje de las pruebas lo hace con todos los permisos. */
    public UUID crearCaja(ContextoNegocio contexto, String nombre) {
        return confirmar(contexto, Permisos.TODOS, new CrearCajaComando(nombre)).recursoId();
    }

    /** Crea la caja «Taquilla» y abre un turno con ese fondo. Devuelve el turno. */
    public UUID abrirTurno(ContextoNegocio contexto, String fondo) {
        var caja = crearCaja(contexto, "Taquilla");
        return confirmar(contexto, new AbrirTurnoComando(caja, new BigDecimal(fondo))).recursoId();
    }

    /** {@code venta.registrada} v1 como la publica Ventas (solo los campos que lee Caja y uno extra). */
    public SobreEvento ventaRegistrada(UUID negocioId, UUID turnoId, UUID ventaId, Map<String, String> pagos) {
        return sobre(negocioId, "venta.registrada", ventaId, Map.of("venta_id", ventaId.toString(),
                "turno_id", turnoId.toString(), "total", "999.00", "pagos", pagosDe(pagos)));
    }

    /** {@code venta.anulada} v1 como la publica Ventas. */
    public SobreEvento ventaAnulada(UUID negocioId, UUID turnoId, UUID ventaId, Map<String, String> pagos) {
        return sobre(negocioId, "venta.anulada", ventaId, Map.of("venta_id", ventaId.toString(),
                "turno_id", turnoId.toString(), "motivo", "Error", "pagos", pagosDe(pagos)));
    }

    private SobreEvento sobre(UUID negocioId, String tipo, UUID ventaId, Map<String, Object> datos) {
        Instant ahora = reloj.instant();
        return new SobreEvento(UuidV7.generar(ahora), tipo, 1, negocioId, "ventas", ahora, ventaId, null, datos);
    }

    private static List<Map<String, Object>> pagosDe(Map<String, String> pagos) {
        return pagos.entrySet().stream()
                .map(p -> Map.<String, Object>of("metodo", p.getKey(), "monto", p.getValue())).toList();
    }

    /** Exige que la accion falle con CajaException y el codigo esperado. */
    public static CajaException fallaCon(ThrowingCallable accion, CodigoError esperado) {
        var capturada = catchThrowable(accion);
        assertThat(capturada).isInstanceOf(CajaException.class);
        var excepcion = (CajaException) capturada;
        assertThat(excepcion.codigo()).isEqualTo(esperado);
        return excepcion;
    }
}
