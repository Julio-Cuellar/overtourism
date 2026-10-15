package mx.negocio360.contabilidad.fakes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.dominio.Configuracion;

/** Doble en memoria de la tabla configuracion: una fila por negocio. */
public final class RepositorioConfiguracionEnMemoria implements RepositorioConfiguracion {

    private final Map<UUID, Configuracion> filas = new HashMap<>();

    @Override
    public Optional<Configuracion> delNegocio(UUID negocioId) {
        return Optional.ofNullable(filas.get(negocioId));
    }

    @Override
    public void guardar(Configuracion configuracion) {
        filas.put(configuracion.negocioId(), configuracion);
    }
}
