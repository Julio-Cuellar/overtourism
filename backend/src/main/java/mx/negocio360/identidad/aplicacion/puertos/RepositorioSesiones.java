package mx.negocio360.identidad.aplicacion.puertos;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.dominio.Sesion;

/**
 * Almacen de sesiones, por el hash del token. {@code guardar} inserta o reemplaza.
 *
 * <p>LIMITE QUE EXIGE LA PERSISTENCIA (IDN-1.1): renovar una sesion en cada peticion no debe poder pisar una revocacion
 * concurrente. Con Postgres la renovacion tiene que ser {@code UPDATE ... SET ultimo_uso = ?, expira_en = ? WHERE
 * token_hash = ? AND revocada_en IS NULL}, no guardar la sesion completa que se leyo antes: si no, una peticion con un
 * token recien revocado podria dejar la sesion viva otra vez. En memoria lo evita el {@code synchronized} del servicio.
 */
public interface RepositorioSesiones {

    void guardar(Sesion sesion);

    Optional<Sesion> porHash(String tokenHash);

    /** Sesiones vigentes de una cuenta, de la mas antigua a la mas reciente. */
    List<Sesion> vigentesDe(UUID cuentaId, Instant ahora);
}
