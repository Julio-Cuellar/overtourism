package mx.negocio360.restaurantes.aplicacion;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Un limite de tasa de ventana deslizante por clave (QR-16): a lo mas {@code maximo} sucesos dentro de {@code ventana}.
 * Primero se pregunta con {@link #excede} y, solo si lo que se intento salio bien, se anota con {@link #registrar}; asi un
 * pedido que falla por validacion no gasta el cupo. Vive en memoria de este proceso.
 */
final class LimiteDeTasa {

    /** Cuantas claves se guardan antes de barrer las vencidas. */
    private static final int BARRIDO_DESDE = 10_000;

    private final int maximo;
    private final Duration ventana;
    private final Map<String, ArrayDeque<Instant>> sucesos = new HashMap<>();

    LimiteDeTasa(int maximo, Duration ventana) {
        this.maximo = maximo;
        this.ventana = ventana;
    }

    /** Verdadero si la clave ya uso todo su cupo en la ventana que termina {@code ahora}. */
    synchronized boolean excede(String clave, Instant ahora) {
        var cola = sucesos.get(clave);
        if (cola == null) {
            return false;
        }
        descartarVencidos(cola, ahora);
        return cola.size() >= maximo;
    }

    /** Anota un suceso de la clave. */
    synchronized void registrar(String clave, Instant ahora) {
        if (sucesos.size() >= BARRIDO_DESDE) {
            sucesos.values().forEach(c -> descartarVencidos(c, ahora));
            sucesos.values().removeIf(ArrayDeque::isEmpty);
        }
        var cola = sucesos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        descartarVencidos(cola, ahora);
        cola.addLast(ahora);
    }

    private void descartarVencidos(ArrayDeque<Instant> cola, Instant ahora) {
        var limite = ahora.minus(ventana);
        while (!cola.isEmpty() && !cola.peekFirst().isAfter(limite)) {
            cola.removeFirst();
        }
    }
}
