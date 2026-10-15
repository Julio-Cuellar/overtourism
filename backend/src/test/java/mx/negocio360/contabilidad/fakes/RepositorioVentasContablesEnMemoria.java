package mx.negocio360.contabilidad.fakes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioVentasContables;
import mx.negocio360.contabilidad.dominio.VentaContable;

/** Doble en memoria de venta_contable, con llave (negocio, venta). */
public final class RepositorioVentasContablesEnMemoria implements RepositorioVentasContables {

    private final Map<String, VentaContable> filas = new LinkedHashMap<>();

    @Override
    public synchronized void guardar(VentaContable venta) {
        filas.put(venta.negocioId() + "/" + venta.ventaId(), venta);
    }

    @Override
    public synchronized Optional<VentaContable> porVenta(UUID negocioId, UUID ventaId) {
        return Optional.ofNullable(filas.get(negocioId + "/" + ventaId));
    }

    @Override
    public synchronized List<VentaContable> deSesion(UUID negocioId, UUID cajaSesionId) {
        return filas.values().stream()
                .filter(v -> v.negocioId().equals(negocioId) && cajaSesionId.equals(v.cajaSesionId())).toList();
    }
}
