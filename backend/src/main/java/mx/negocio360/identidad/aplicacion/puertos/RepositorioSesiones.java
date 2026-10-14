package mx.negocio360.identidad.aplicacion.puertos;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.dominio.Sesion;

/** Almacen de sesiones, por el hash del token. {@code guardar} inserta o reemplaza. */
public interface RepositorioSesiones {

    void guardar(Sesion sesion);

    Optional<Sesion> porHash(String tokenHash);

    /** Sesiones vigentes de una cuenta, de la mas antigua a la mas reciente. */
    List<Sesion> vigentesDe(UUID cuentaId, Instant ahora);
}
