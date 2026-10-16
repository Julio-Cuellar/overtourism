package mx.negocio360.caja.aplicacion;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.CajaException;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.TipoMovimiento;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.caja.eventos.TurnoAbierto;
import mx.negocio360.compartido.comandos.ManejadorComando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.compartido.identidad.UuidV7;

/**
 * CF-10 AbrirTurno: CJ-01 (un turno no cerrado por caja) y CJ-02 (fondo ≥ 0; si es mayor que 0 crea un movimiento
 * FONDO en efectivo). Publica {@code caja.turno_abierto} v1.
 */
public final class ManejadorAbrirTurno implements ManejadorComando<AbrirTurnoComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "abrir_turno";

    private final RepositorioCaja cajas;
    private final LibroDeCaja libro;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    /**
     * Arma el manejador.
     *
     * @param cajas   tablas caja y turno
     * @param libro   movimientos
     * @param eventos outbox
     * @param reloj   reloj de la plataforma
     */
    public ManejadorAbrirTurno(RepositorioCaja cajas, LibroDeCaja libro, PublicadorEventos eventos, Clock reloj) {
        this.cajas = Objects.requireNonNull(cajas, "cajas");
        this.libro = Objects.requireNonNull(libro, "libro");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<AbrirTurnoComando> claseComando() {
        return AbrirTurnoComando.class;
    }

    /** @throws CajaException CAJA_NO_ENCONTRADA, CAJA_INACTIVA, FONDO_INVALIDO o TURNO_YA_ABIERTO */
    @Override
    public String preparar(ContextoNegocio contexto, AbrirTurnoComando comando) {
        var caja = cajaDisponible(contexto, comando);
        return PlantillasCaja.resumenAbrirTurno(caja.nombre(), fondoValido(comando));
    }

    /** @throws CajaException los de preparar; TURNO_YA_ABIERTO si otro turno se abrio entre los dos pasos */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, AbrirTurnoComando comando, String resumenPreparado) {
        var caja = cajaDisponible(contexto, comando);
        var fondo = Redondeo.dinero(fondoValido(comando));
        var ahora = reloj.instant();
        var turno = new Turno(UuidV7.generar(ahora), contexto.negocioId(), caja.id(), contexto.actor(),
                EstadoTurno.ABIERTO, fondo, ahora, null, null, 1);
        cajas.guardarTurno(turno);
        if (fondo.signum() > 0) {
            libro.agregar(new MovimientoCaja(UuidV7.generar(ahora), contexto.negocioId(), turno.id(),
                    TipoMovimiento.FONDO, MetodoPago.EFECTIVO, fondo, null, null, null, ahora, contexto.actor()));
        }
        eventos.publicar(FabricaSobres.de(contexto.negocioId(), ahora, turno.id(), TurnoAbierto.TIPO,
                TurnoAbierto.VERSION, new TurnoAbierto(turno).datos()));
        return ResultadoComando.ok(PlantillasCaja.TURNO_ABIERTO, turno.id(), Map.of("turno_id", turno.id().toString()));
    }

    private Caja cajaDisponible(ContextoNegocio contexto, AbrirTurnoComando comando) {
        if (comando.cajaId() == null) {
            throw new CajaException(CodigoError.CAJA_NO_ENCONTRADA);
        }
        var caja = cajas.caja(contexto.negocioId(), comando.cajaId())
                .orElseThrow(() -> new CajaException(CodigoError.CAJA_NO_ENCONTRADA, comando.cajaId()));
        if (!caja.activa()) {
            throw new CajaException(CodigoError.CAJA_INACTIVA, caja.id());
        }
        cajas.turnoNoCerradoDeCaja(contexto.negocioId(), caja.id()).ifPresent(t -> {
            throw new CajaException(CodigoError.TURNO_YA_ABIERTO, t.id());
        });
        return caja;
    }

    private static java.math.BigDecimal fondoValido(AbrirTurnoComando comando) {
        var fondo = comando.fondoInicial();
        if (fondo == null || fondo.signum() < 0 || !Validaciones.dineroValido(fondo)) {
            throw new CajaException(CodigoError.FONDO_INVALIDO);
        }
        return fondo;
    }
}
