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
 * la memoria y se olvidan las claves sin actividad, cada una segun SU propia ventana: la misma instancia atiende claves de
 * ventana de un minuto (la entrada de /graphql) y de horas (las solicitudes de codigo), y el barrido que dispara una no
 * puede borrar los intentos de la otra. Solo para desarrollo, demo y una sola instancia.
 */
public final class LimiteDeTasaEnMemoria implements LimiteDeTasa {

    /** Los intentos de una clave y la ventana con la que se cuentan. */
    private record Registro(Deque<Instant> intentos, Duration ventana) {
    }

    private final Map<String, Registro> registros = new HashMap<>();
    private Instant ultimoBarrido;

    @Override
    public synchronized boolean intentar(String clave, int maximo, Duration ventana, Instant ahora) {
        Objects.requireNonNull(clave, "clave");
        if (maximo <= 0 || ventana.isZero() || ventana.isNegative()) {
            throw new IllegalArgumentException("El maximo y la ventana deben ser positivos");
        }
        barrerSiToca(ventana, ahora);
        var registro = registros.compute(clave, (k, actual) -> new Registro(
                actual == null ? new ArrayDeque<>() : actual.intentos(), ventana));
        olvidarVencidos(registro.intentos(), ventana, ahora);
        if (registro.intentos().size() >= maximo) {
            return false;
        }
        registro.intentos().addLast(ahora);
        return true;
    }

    /** Claves con intentos guardados, para las pruebas. */
    public synchronized int clavesActivas() {
        return registros.size();
    }

    private void barrerSiToca(Duration ventana, Instant ahora) {
        if (ultimoBarrido == null) {
            ultimoBarrido = ahora;
            return;
        }
        if (ahora.isBefore(ultimoBarrido.plus(ventana))) {
            return;
        }
        registros.values().forEach(registro -> olvidarVencidos(registro.intentos(), registro.ventana(), ahora));
        registros.values().removeIf(registro -> registro.intentos().isEmpty());
        ultimoBarrido = ahora;
    }

    private static void olvidarVencidos(Deque<Instant> intentos, Duration ventana, Instant ahora) {
        var limite = ahora.minus(ventana);
        while (!intentos.isEmpty() && !intentos.peekFirst().isAfter(limite)) {
            intentos.removeFirst();
        }
    }
}
