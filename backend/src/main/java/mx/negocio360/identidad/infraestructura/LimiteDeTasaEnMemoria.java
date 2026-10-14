package mx.negocio360.identidad.infraestructura;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.identidad.aplicacion.puertos.LimiteDeTasa;

/**
 * Ventana deslizante en memoria (AS-03): guarda el momento de cada intento permitido y cuenta los que caen dentro de
 * la ventana. Los rechazados no se guardan, asi que un bloqueo no se alarga por insistir. Una vez por ventana se barre
 * la memoria y se olvidan las claves sin actividad. Solo para desarrollo, demo y una sola instancia.
 */
public final class LimiteDeTasaEnMemoria implements LimiteDeTasa {

    private final Map<String, Deque<Instant>> intentosPorClave = new HashMap<>();
    private Instant ultimoBarrido;

    @Override
    public synchronized boolean intentar(String clave, int maximo, Duration ventana, Instant ahora) {
        Objects.requireNonNull(clave, "clave");
        if (maximo <= 0 || ventana.isZero() || ventana.isNegative()) {
            throw new IllegalArgumentException("El maximo y la ventana deben ser positivos");
        }
        barrerSiToca(ventana, ahora);
        var intentos = intentosPorClave.computeIfAbsent(clave, k -> new ArrayDeque<>());
        olvidarVencidos(intentos, ventana, ahora);
        if (intentos.size() >= maximo) {
            return false;
        }
        intentos.addLast(ahora);
        return true;
    }

    /** Claves con intentos guardados, para las pruebas. */
    public synchronized int clavesActivas() {
        return intentosPorClave.size();
    }

    private void barrerSiToca(Duration ventana, Instant ahora) {
        if (ultimoBarrido == null) {
            ultimoBarrido = ahora;
            return;
        }
        if (ahora.isBefore(ultimoBarrido.plus(ventana))) {
            return;
        }
        intentosPorClave.values().forEach(intentos -> olvidarVencidos(intentos, ventana, ahora));
        intentosPorClave.values().removeIf(Deque::isEmpty);
        ultimoBarrido = ahora;
    }

    private static void olvidarVencidos(Deque<Instant> intentos, Duration ventana, Instant ahora) {
        var limite = ahora.minus(ventana);
        while (!intentos.isEmpty() && !intentos.peekFirst().isAfter(limite)) {
            intentos.removeFirst();
        }
    }
}
