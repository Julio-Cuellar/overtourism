package mx.negocio360.caja.aplicacion;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.dominio.CajaException;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.compartido.comandos.ManejadorComando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/** CF-13 DescartarCorte: un turno EN_CORTE vuelve a ABIERTO; si ya estaba ABIERTO no cambia nada. */
public final class ManejadorDescartarCorte implements ManejadorComando<DescartarCorteComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "descartar_corte";

    private final RepositorioCaja cajas;
    private final Clock reloj;

    /**
     * Arma el manejador.
     *
     * @param cajas tabla turno
     * @param reloj reloj de la plataforma
     */
    public ManejadorDescartarCorte(RepositorioCaja cajas, Clock reloj) {
        this.cajas = Objects.requireNonNull(cajas, "cajas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<DescartarCorteComando> claseComando() {
        return DescartarCorteComando.class;
    }

    /** @throws CajaException TURNO_NO_ABIERTO si no existe o ya esta cerrado */
    @Override
    public String preparar(ContextoNegocio contexto, DescartarCorteComando comando) {
        Validaciones.turnoNoCerrado(cajas, contexto.negocioId(), comando.turnoId(), reloj.instant());
        return PlantillasCaja.DESCARTAR_CORTE;
    }

    /** @throws CajaException TURNO_NO_ABIERTO o CONCURRENCIA */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, DescartarCorteComando comando,
                                      String resumenPreparado) {
        var ahora = reloj.instant();
        var turno = Validaciones.turnoNoCerrado(cajas, contexto.negocioId(), comando.turnoId(), ahora);
        if (turno.estadoEn(ahora) == EstadoTurno.EN_CORTE
                && !cajas.actualizarTurno(turno.abierto(), turno.version())) {
            throw new CajaException(CodigoError.CONCURRENCIA, turno.id());
        }
        return ResultadoComando.ok(PlantillasCaja.CORTE_DESCARTADO, turno.id(),
                Map.of("turno_id", turno.id().toString()));
    }
}
