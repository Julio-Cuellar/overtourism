package mx.negocio360.identidad.infraestructura;

import java.time.Clock;
import java.util.Objects;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioPreregistros;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Borra cada minuto los pre-registros vencidos (D-84): son datos personales que no hacen falta pasados sus 15 minutos. Es
 * solo limpieza: un pre-registro vencido ya no sirve aunque siga en la tabla, porque la verificacion lo revisa. Un fallo se
 * registra sin datos y no detiene las siguientes pasadas.
 */
public final class LimpiezaDePreregistros {

    private static final Logger REGISTRO = LoggerFactory.getLogger(LimpiezaDePreregistros.class);

    private final RepositorioPreregistros preregistros;
    private final Clock reloj;

    public LimpiezaDePreregistros(RepositorioPreregistros preregistros, Clock reloj) {
        this.preregistros = Objects.requireNonNull(preregistros, "preregistros");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Scheduled(fixedDelayString = "${identidad.preregistro.limpieza-cada:PT1M}")
    public void borrarVencidos() {
        try {
            var borrados = preregistros.borrarVencidos(reloj.instant());
            if (borrados > 0) {
                REGISTRO.info("Se borraron {} pre-registros vencidos.", borrados);
            }
        } catch (RuntimeException e) {
            // Solo la clase: el mensaje de una excepcion de SQL podria llevar datos de la fila.
            REGISTRO.warn("No se pudieron borrar los pre-registros vencidos: {}", e.getClass().getSimpleName());
        }
    }
}
