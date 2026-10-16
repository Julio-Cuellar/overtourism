package mx.negocio360.caja.aplicacion;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.caja.CajaApi;
import mx.negocio360.caja.FiltroCortes;
import mx.negocio360.caja.SaldoTurno;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.caja.aplicacion.puertos.ReglasCaja;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.aplicacion.puertos.ResumenVentas;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.CajaException;
import mx.negocio360.caja.dominio.CalculoCaja;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.EstadoVentaAplicada;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.caja.seguridad.PermisosDeCaja;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.comandos.PreparacionCreada;
import mx.negocio360.compartido.comandos.RepositorioPreparaciones;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.comandos.ServicioPreparacion;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.paginacion.Cursores;
import mx.negocio360.compartido.paginacion.Pagina;
import mx.negocio360.compartido.seguridad.Autorizacion;

/** Implementacion de {@link CajaApi}: comandos por preparar -&gt; confirmar y consultas por negocio. */
public final class CajaServicio implements CajaApi {

    private final ServicioPreparacion preparacion;
    private final RepositorioCaja cajas;
    private final LibroDeCaja libro;
    private final Clock reloj;

    private CajaServicio(ServicioPreparacion preparacion, RepositorioCaja cajas, LibroDeCaja libro, Clock reloj) {
        this.preparacion = preparacion;
        this.cajas = cajas;
        this.libro = libro;
        this.reloj = reloj;
    }

    /**
     * Arma el modulo a partir de sus puertos.
     *
     * @param cajas         tablas caja y turno
     * @param libro         movimientos, ventas aplicadas, eventos procesados y cortes
     * @param preparaciones tabla preparacion de Caja
     * @param eventos       outbox
     * @param ventas        conteos de Ventas para el corte
     * @param reglas        umbral de diferencia
     * @param reloj         reloj de la plataforma
     * @return el servicio listo
     */
    public static CajaServicio armar(RepositorioCaja cajas, LibroDeCaja libro, RepositorioPreparaciones preparaciones,
                                     PublicadorEventos eventos, ResumenVentas ventas, ReglasCaja reglas,
                                     Clock reloj) {
        Objects.requireNonNull(cajas, "cajas");
        Objects.requireNonNull(libro, "libro");
        Objects.requireNonNull(reloj, "reloj");
        var manejadores = List.of(
                new ManejadorCrearCaja(cajas, reloj),
                new ManejadorAbrirTurno(cajas, libro, eventos, reloj),
                new ManejadorMovimientoCaja(cajas, libro, reloj),
                new ManejadorCerrarTurno(cajas, libro, ventas, reglas, eventos, reloj),
                new ManejadorDescartarCorte(cajas, reloj));
        var preparacion = new ServicioPreparacion(preparaciones, manejadores, reloj,
                fallo -> new CajaException(CodigoError.valueOf(fallo.name())));
        return new CajaServicio(preparacion, cajas, libro, reloj);
    }

    @Override
    public PreparacionCreada preparar(ContextoNegocio contexto, Autorizacion autorizacion, Comando comando) {
        return preparacion.preparar(contexto, autorizacion, comando);
    }

    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, Autorizacion autorizacion, String token) {
        return preparacion.confirmar(contexto, autorizacion, token);
    }

    @Override
    public Optional<Turno> turnoActivo(ContextoNegocio contexto, Autorizacion autorizacion, UUID cajaId) {
        autorizacion.exigir(PermisosDeCaja.TURNO_VER);
        var ahora = reloj.instant();
        return cajas.turnoNoCerradoDeCaja(contexto.negocioId(), cajaId).map(turno -> turno.vigenteEn(ahora));
    }

    @Override
    public SaldoTurno saldoTurno(ContextoNegocio contexto, Autorizacion autorizacion, UUID turnoId) {
        autorizacion.exigir(PermisosDeCaja.TURNO_VER);
        var turno = cajas.turno(contexto.negocioId(), turnoId)
                .orElseThrow(() -> new CajaException(CodigoError.TURNO_NO_ENCONTRADO, turnoId));
        var movimientos = libro.movimientos(contexto.negocioId(), turnoId);
        return new SaldoTurno(turnoId, turno.cajaId(), turno.estadoEn(reloj.instant()),
                CalculoCaja.esperado(movimientos), movimientos,
                libro.contarVentas(contexto.negocioId(), turnoId, EstadoVentaAplicada.APLICADA));
    }

    @Override
    public Pagina<MovimientoCaja> movimientos(ContextoNegocio contexto, Autorizacion autorizacion, UUID turnoId,
                                              int primeros, String despues) {
        autorizacion.exigir(PermisosDeCaja.TURNO_VER);
        if (!Cursores.valido(primeros, despues)) {
            throw new CajaException(CodigoError.PAGINACION_INVALIDA);
        }
        return libro.movimientos(contexto.negocioId(), turnoId, primeros, despues);
    }

    @Override
    public Pagina<Corte> cortes(ContextoNegocio contexto, Autorizacion autorizacion, FiltroCortes filtro,
                                int primeros, String despues) {
        autorizacion.exigir(PermisosDeCaja.CORTE_VER);
        if (!Cursores.valido(primeros, despues)) {
            throw new CajaException(CodigoError.PAGINACION_INVALIDA);
        }
        var buscado = filtro == null ? FiltroCortes.TODOS : filtro;
        if (!buscado.valido()) {
            throw new CajaException(CodigoError.FILTRO_INVALIDO);
        }
        return libro.cortes(contexto.negocioId(), buscado, primeros, despues);
    }

    @Override
    public List<Caja> cajas(ContextoNegocio contexto, Autorizacion autorizacion) {
        autorizacion.exigir(PermisosDeCaja.TURNO_VER);
        return cajas.cajas(contexto.negocioId());
    }
}
