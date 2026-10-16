package mx.negocio360.caja.fakes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.EstadoVentaAplicada;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.VentaAplicada;

/** Doble de {@code movimiento_caja}, {@code venta_aplicada}, {@code evento_procesado} y {@code corte}. */
public final class LibroDeCajaEnMemoria implements LibroDeCaja {

    private final List<MovimientoCaja> movimientos = new ArrayList<>();
    private final Map<String, VentaAplicada> aplicadas = new HashMap<>();
    private final Set<String> procesados = new HashSet<>();
    private final List<Corte> cortes = new ArrayList<>();

    @Override
    public void agregar(MovimientoCaja movimiento) {
        movimientos.add(movimiento);
    }

    @Override
    public List<MovimientoCaja> movimientos(UUID negocioId, UUID turnoId) {
        return movimientos.stream().filter(m -> m.negocioId().equals(negocioId) && m.turnoId().equals(turnoId))
                .toList();
    }

    @Override
    public Optional<VentaAplicada> ventaAplicada(UUID negocioId, UUID ventaId) {
        return Optional.ofNullable(aplicadas.get(negocioId + "/" + ventaId));
    }

    @Override
    public void guardarVentaAplicada(VentaAplicada venta) {
        aplicadas.put(venta.negocioId() + "/" + venta.ventaId(), venta);
    }

    @Override
    public long contarVentas(UUID negocioId, UUID turnoId, EstadoVentaAplicada estado) {
        return aplicadas.values().stream()
                .filter(v -> v.negocioId().equals(negocioId) && turnoId.equals(v.turnoId()) && v.estado() == estado)
                .count();
    }

    @Override
    public boolean registrarEvento(String consumidor, UUID eventoId) {
        return procesados.add(consumidor + "/" + eventoId);
    }

    @Override
    public void guardarCorte(Corte corte) {
        cortes.add(corte);
    }

    @Override
    public List<Corte> cortes(UUID negocioId) {
        return cortes.stream().filter(c -> c.negocioId().equals(negocioId)).toList();
    }

    /** Dice si el consumidor ya marco el evento como procesado. */
    public boolean procesado(String consumidor, UUID eventoId) {
        return procesados.contains(consumidor + "/" + eventoId);
    }

    public List<MovimientoCaja> todos() {
        return List.copyOf(movimientos);
    }
}
