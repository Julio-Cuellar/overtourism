package mx.negocio360.identidad.aplicacion.puertos;

import java.time.Duration;
import java.time.Instant;

/** Tope de solicitudes en una ventana deslizante (AS-03). Las claves llegan ya convertidas en hash. */
public interface LimiteDeTasa {

    /**
     * Cuenta un intento si todavia hay cupo y devuelve verdadero; si no, devuelve falso sin contarlo. La operacion es
     * atomica.
     */
    boolean intentar(String clave, int maximo, Duration ventana, Instant ahora);
}
