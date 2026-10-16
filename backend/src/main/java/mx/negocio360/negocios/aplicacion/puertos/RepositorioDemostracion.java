package mx.negocio360.negocios.aplicacion.puertos;

import java.util.List;
import java.util.UUID;

/** Los negocios simulados de la demo (DM-01): se marcan al sembrarlos y solo ellos se restablecen (DM-04). */
public interface RepositorioDemostracion {

    /** Los ids de los negocios simulados. */
    List<UUID> negocios();

    /** Marca un negocio como simulado (`es_simulado`) y lo anota para poder restablecerlo. Idempotente. */
    void marcarSimulado(UUID negocioId);

    boolean esSimulado(UUID negocioId);
}
