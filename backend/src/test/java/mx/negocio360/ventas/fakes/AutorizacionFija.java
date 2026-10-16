package mx.negocio360.ventas.fakes;

import java.util.HashSet;
import java.util.Set;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.ventas.aplicacion.puertos.AutorizacionVentas;

/** Doble de los permisos de Identidad: solo los actores de la lista pueden anular. */
public final class AutorizacionFija implements AutorizacionVentas {

    private final Set<String> encargados = new HashSet<>(Set.of("encargado-a", "encargado-b"));

    @Override
    public boolean puedeAnular(ContextoNegocio contexto) {
        return encargados.contains(contexto.actor());
    }
}
