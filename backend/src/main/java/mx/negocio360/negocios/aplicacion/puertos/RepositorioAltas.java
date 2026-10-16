package mx.negocio360.negocios.aplicacion.puertos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import mx.negocio360.negocios.AltaVista;

/**
 * Altas de negocio y lo que se necesita para procesarlas una sola vez (NG-03, EV-02). Las altas se aislan por cuenta,
 * no por negocio (AL-04), porque una alta rechazada no tiene negocio.
 */
public interface RepositorioAltas {

    /** El resultado que se guarda de cada alta. */
    record Registro(UUID negocioId, UUID cuentaId, String nombre, String giro, AltaVista.EstadoAlta estado,
                    String motivo, UUID eventoId, Instant creadaEn) {

        /** Sin el nombre: es lo que escribio una persona y puede ser invalido. */
        @Override
        public String toString() {
            return "Registro[negocioId=" + negocioId + ", estado=" + estado + ", motivo=" + motivo + "]";
        }
    }

    /** Anota que el listener proceso el evento. {@code false} si ya lo habia procesado. */
    boolean marcarEventoProcesado(UUID eventoId, String listener, Instant ahora);

    /** Guarda el resultado de una alta. {@code false} si ese negocio ya tenia una (no cambia nada). */
    boolean insertarAlta(Registro registro);

    /** Las altas que pidio la cuenta, de la mas reciente a la mas antigua. */
    List<AltaVista> altasDeCuenta(UUID cuentaId);

    /** Si el giro existe en el catalogo y esta activo (NG-02). */
    boolean giroActivo(String codigo);
}
