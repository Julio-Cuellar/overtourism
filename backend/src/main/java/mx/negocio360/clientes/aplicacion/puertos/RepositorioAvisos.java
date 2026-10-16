package mx.negocio360.clientes.aplicacion.puertos;

import java.util.Optional;
import java.util.UUID;
import mx.negocio360.clientes.dominio.AvisoPrivacidad;

/** Puerto de persistencia de los avisos de privacidad de cada negocio, por version. */
public interface RepositorioAvisos {

    /** El aviso vigente del negocio (el que no tiene fecha de cierre), si lo hay. */
    Optional<AvisoPrivacidad> vigente(UUID negocioId);

    /** La version mas alta publicada por el negocio, o 0 si nunca publico uno. */
    int ultimaVersion(UUID negocioId);

    /**
     * Guarda el aviso nuevo y cierra, en la misma operacion, el que estaba vigente, con fecha de cierre igual a la
     * de inicio del nuevo. Falla con CONCURRENCIA si la version ya existe para el negocio.
     */
    void publicar(AvisoPrivacidad nuevo);
}
