package mx.negocio360.compartido.seguridad;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Limitador de tasa por clave (SG-10): una cubeta de fichas por cuenta o por IP. La cubeta empieza llena, cada
 * peticion gasta una ficha y se rellena a razon de {@code capacidad} fichas por ventana, sin pasar de la capacidad.
 *
 * <p>La cuenta es entera: una ficha vale {@code ventana} milisegundos-unidad y cada milisegundo aporta
 * {@code capacidad} unidades. Con decimales, {@code 0.999 + 0.001} puede dar 0.9999999999999999 y negar justo en el
 * borde. La memoria esta acotada: al pasar el maximo de claves se descarta la menos usada (esa clave empieza de
 * nuevo, que es el lado benigno). Vive en la memoria de esta instancia; con varias instancias cada una cuenta aparte.
 */
public final class LimitadorDeTasa {

    /** Lo que decide el limitador; si no se permite, cuantos segundos esperar para tener una ficha. */
    public record Decision(boolean permitido, long reintentarEnSegundos) {
        static final Decision PERMITIDA = new Decision(true, 0);
    }

    private static final class Cubeta {
        long unidades;
        long marcaMs;

        Cubeta(long unidades, long marcaMs) {
            this.unidades = unidades;
            this.marcaMs = marcaMs;
        }
    }

    private final long capacidad;
    private final long ventanaMs;
    private final Clock reloj;
    private final Map<String, Cubeta> cubetas;

    public LimitadorDeTasa(int capacidad, Duration ventana, int maximoDeClaves, Clock reloj) {
        if (capacidad < 1 || maximoDeClaves < 1 || ventana.toMillis() < 1) {
            throw new IllegalArgumentException("La capacidad, la ventana y el maximo de claves deben ser positivos");
        }
        this.capacidad = capacidad;
        this.ventanaMs = ventana.toMillis();
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.cubetas = new LinkedHashMap<>(16, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Cubeta> masAntigua) {
                return size() > maximoDeClaves;
            }
        };
    }

    /** Gasta una ficha de esa clave si hay; si no, dice cuanto esperar. */
    public synchronized Decision consumir(String clave) {
        var ahora = reloj.millis();
        var cubeta = cubetas.get(clave);
        if (cubeta == null) {
            cubeta = new Cubeta(capacidad * ventanaMs, ahora);
            cubetas.put(clave, cubeta);
        } else {
            recargar(cubeta, ahora);
        }
        if (cubeta.unidades >= ventanaMs) {
            cubeta.unidades -= ventanaMs;
            return Decision.PERMITIDA;
        }
        return new Decision(false, espera(cubeta));
    }

    /** Dice si hay ficha sin gastarla ni crear la clave. */
    public synchronized Decision probar(String clave) {
        var cubeta = cubetas.get(clave);
        if (cubeta == null) {
            return Decision.PERMITIDA;
        }
        recargar(cubeta, reloj.millis());
        return cubeta.unidades >= ventanaMs ? Decision.PERMITIDA : new Decision(false, espera(cubeta));
    }

    /** Cuantas claves recuerda ahora. */
    public synchronized int claves() {
        return cubetas.size();
    }

    private void recargar(Cubeta cubeta, long ahora) {
        var transcurrido = Math.max(0, ahora - cubeta.marcaMs);
        cubeta.unidades = Math.min(capacidad * ventanaMs, cubeta.unidades + transcurrido * capacidad);
        cubeta.marcaMs = ahora;
    }

    /** Segundos, redondeados hacia arriba y nunca menos de uno, hasta tener una ficha entera. */
    private long espera(Cubeta cubeta) {
        var faltante = ventanaMs - cubeta.unidades;
        var milisegundos = (faltante + capacidad - 1) / capacidad;
        return Math.max(1, (milisegundos + 999) / 1000);
    }
}
