package mx.negocio360.caja.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.dominio.CajaException;
import mx.negocio360.caja.dominio.CalculoCaja;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.TipoMovimiento;
import mx.negocio360.compartido.comandos.ManejadorComando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.compartido.identidad.UuidV7;

/**
 * CF-11 RegistrarMovimientoCaja: entrada o retiro de efectivo en un turno ABIERTO (CJ-10). Un retiro no puede dejar
 * el efectivo esperado bajo cero (CJ-05). Todo se revisa de nuevo al confirmar.
 */
public final class ManejadorMovimientoCaja implements ManejadorComando<RegistrarMovimientoCajaComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "registrar_movimiento_caja";
    static final int MAX_CARACTERES_NOTA = 200;

    private final RepositorioCaja cajas;
    private final LibroDeCaja libro;
    private final Clock reloj;

    /**
     * Arma el manejador.
     *
     * @param cajas tabla turno
     * @param libro movimientos
     * @param reloj reloj de la plataforma
     */
    public ManejadorMovimientoCaja(RepositorioCaja cajas, LibroDeCaja libro, Clock reloj) {
        this.cajas = Objects.requireNonNull(cajas, "cajas");
        this.libro = Objects.requireNonNull(libro, "libro");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<RegistrarMovimientoCajaComando> claseComando() {
        return RegistrarMovimientoCajaComando.class;
    }

    /**
     * @throws CajaException TURNO_NO_ABIERTO, TURNO_EN_CORTE, MONTO_INVALIDO, NOTA_REQUERIDA o
     *                       RETIRO_MAYOR_AL_EFECTIVO
     */
    @Override
    public String preparar(ContextoNegocio contexto, RegistrarMovimientoCajaComando comando) {
        var valido = validar(contexto, comando);
        return PlantillasCaja.resumenMovimiento(comando.tipo(), valido.monto(), valido.nota());
    }

    /** @throws CajaException los de preparar, revisados de nuevo */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, RegistrarMovimientoCajaComando comando,
                                      String resumenPreparado) {
        var valido = validar(contexto, comando);
        var ahora = reloj.instant();
        var tipo = comando.tipo() == TipoMovimientoManual.ENTRADA ? TipoMovimiento.ENTRADA : TipoMovimiento.RETIRO;
        var monto = tipo == TipoMovimiento.ENTRADA ? valido.monto() : valido.monto().negate();
        var movimiento = new MovimientoCaja(UuidV7.generar(ahora), contexto.negocioId(), comando.turnoId(), tipo,
                MetodoPago.EFECTIVO, monto, null, null, valido.nota(), ahora, contexto.actor());
        libro.agregar(movimiento);
        return ResultadoComando.ok(PlantillasCaja.MOVIMIENTO_REGISTRADO, movimiento.id(),
                Map.of("movimiento_id", movimiento.id().toString()));
    }

    private record Valido(BigDecimal monto, String nota) {
    }

    private Valido validar(ContextoNegocio contexto, RegistrarMovimientoCajaComando comando) {
        Validaciones.turnoAbierto(cajas, contexto.negocioId(), comando.turnoId(), reloj.instant());
        var monto = comando.monto();
        if (comando.tipo() == null || monto == null || monto.signum() <= 0 || !Validaciones.dineroValido(monto)) {
            throw new CajaException(CodigoError.MONTO_INVALIDO);
        }
        var nota = Validaciones.textoObligatorio(comando.nota(), MAX_CARACTERES_NOTA);
        if (nota == null) {
            throw new CajaException(CodigoError.NOTA_REQUERIDA);
        }
        var escalado = Redondeo.dinero(monto);
        if (comando.tipo() == TipoMovimientoManual.RETIRO) {
            var efectivo = CalculoCaja.esperado(libro.movimientos(contexto.negocioId(), comando.turnoId()))
                    .get(MetodoPago.EFECTIVO);
            if (escalado.compareTo(efectivo) > 0) {
                throw new CajaException(CodigoError.RETIRO_MAYOR_AL_EFECTIVO, comando.turnoId());
            }
        }
        return new Valido(escalado, nota);
    }
}
