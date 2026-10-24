package mx.negocio360.caja.infraestructura;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.caja.FiltroCortes;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.EstadoVentaAplicada;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.caja.dominio.VentaAplicada;
import mx.negocio360.compartido.paginacion.Cursores;
import mx.negocio360.compartido.paginacion.Pagina;

/**
 * Tablas de Caja en memoria para desarrollo y demo (F4): {@code caja}, {@code turno}, {@code movimiento_caja},
 * {@code venta_aplicada}, {@code evento_procesado} y {@code corte}. Cada operacion esta sincronizada, pero una
 * secuencia de operaciones no: la carrera entre cerrar un turno y aplicar una venta (hallazgo A2 de la revision) sigue
 * pendiente con bloqueos por fila en PostgreSQL. Los datos se pierden al reiniciar; la version persistente es
 * {@code RepositorioCajaJdbc} y {@code LibroDeCajaJdbc}.
 */
public final class CajaEnMemoria implements RepositorioCaja, LibroDeCaja {

    private final Map<UUID, Caja> cajas = new LinkedHashMap<>();
    private final Map<UUID, Turno> turnos = new LinkedHashMap<>();
    private final List<MovimientoCaja> movimientos = new ArrayList<>();
    private final Map<String, VentaAplicada> aplicadas = new HashMap<>();
    private final Set<String> procesados = new HashSet<>();
    private final List<Corte> cortes = new ArrayList<>();

    @Override
    public synchronized void guardarCaja(Caja caja) {
        cajas.put(caja.id(), caja);
    }

    @Override
    public synchronized Optional<Caja> caja(UUID negocioId, UUID cajaId) {
        return Optional.ofNullable(cajas.get(cajaId)).filter(c -> c.negocioId().equals(negocioId));
    }

    @Override
    public synchronized List<Caja> cajas(UUID negocioId) {
        return cajas.values().stream().filter(c -> c.negocioId().equals(negocioId)).toList();
    }

    @Override
    public synchronized Optional<Turno> turno(UUID negocioId, UUID turnoId) {
        return Optional.ofNullable(turnos.get(turnoId)).filter(t -> t.negocioId().equals(negocioId));
    }

    @Override
    public synchronized Optional<Turno> turnoNoCerradoDeCaja(UUID negocioId, UUID cajaId) {
        return turnos.values().stream()
                .filter(t -> t.negocioId().equals(negocioId) && t.cajaId().equals(cajaId))
                .filter(t -> t.estado() != EstadoTurno.CERRADO)
                .findFirst();
    }

    @Override
    public synchronized void guardarTurno(Turno turno) {
        turnos.put(turno.id(), turno);
    }

    @Override
    public synchronized boolean actualizarTurno(Turno turno, int versionEsperada) {
        var actual = turnos.get(turno.id());
        if (actual == null || !actual.negocioId().equals(turno.negocioId()) || actual.version() != versionEsperada) {
            return false;
        }
        turnos.put(turno.id(), turno);
        return true;
    }

    @Override
    public synchronized void agregar(MovimientoCaja movimiento) {
        movimientos.add(movimiento);
    }

    @Override
    public synchronized Pagina<MovimientoCaja> movimientos(UUID negocioId, UUID turnoId, int primeros,
                                                           String despues) {
        return Cursores.pagina(movimientos(negocioId, turnoId), primeros, despues);
    }

    @Override
    public synchronized List<MovimientoCaja> movimientos(UUID negocioId, UUID turnoId) {
        return movimientos.stream().filter(m -> m.negocioId().equals(negocioId) && m.turnoId().equals(turnoId))
                .toList();
    }

    @Override
    public synchronized Optional<VentaAplicada> ventaAplicada(UUID negocioId, UUID ventaId) {
        return Optional.ofNullable(aplicadas.get(negocioId + "/" + ventaId));
    }

    @Override
    public synchronized void guardarVentaAplicada(VentaAplicada venta) {
        aplicadas.put(venta.negocioId() + "/" + venta.ventaId(), venta);
    }

    @Override
    public synchronized long contarVentas(UUID negocioId, UUID turnoId, EstadoVentaAplicada estado) {
        return aplicadas.values().stream()
                .filter(v -> v.negocioId().equals(negocioId) && turnoId.equals(v.turnoId()) && v.estado() == estado)
                .count();
    }

    @Override
    public synchronized boolean registrarEvento(String consumidor, UUID eventoId) {
        return procesados.add(consumidor + "/" + eventoId);
    }

    @Override
    public synchronized void guardarCorte(Corte corte) {
        cortes.add(corte);
    }

    @Override
    public synchronized Pagina<Corte> cortes(UUID negocioId, FiltroCortes filtro, int primeros, String despues) {
        var coinciden = cortes.stream()
                .filter(c -> c.negocioId().equals(negocioId))
                .filter(c -> filtro.cajaId() == null || filtro.cajaId().equals(c.cajaId()))
                .filter(c -> filtro.desde() == null || !c.confirmadoEn().isBefore(filtro.desde()))
                .filter(c -> filtro.hasta() == null || !c.confirmadoEn().isAfter(filtro.hasta()))
                .sorted(java.util.Comparator.comparing(Corte::confirmadoEn).reversed())
                .toList();
        return Cursores.pagina(coinciden, primeros, despues);
    }
}
