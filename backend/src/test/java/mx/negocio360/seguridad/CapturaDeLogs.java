package mx.negocio360.seguridad;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.slf4j.LoggerFactory;

/** Solo pruebas: guarda lo que se registra en un logger mientras la prueba corre, para revisarlo despues. */
public final class CapturaDeLogs implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> lista = new ListAppender<>();

    public CapturaDeLogs(String nombreDelLogger) {
        this.logger = (Logger) LoggerFactory.getLogger(nombreDelLogger);
        lista.start();
        logger.addAppender(lista);
    }

    /** Cada evento como una sola cadena: el mensaje ya formateado, mas la excepcion completa si la trae. */
    public List<String> lineas() {
        return lista.list.stream().map(evento -> {
            var texto = new StringBuilder(evento.getFormattedMessage());
            var excepcion = evento.getThrowableProxy();
            while (excepcion != null) {
                texto.append(" | ").append(excepcion.getClassName()).append(": ").append(excepcion.getMessage());
                excepcion = excepcion.getCause();
            }
            return texto.toString();
        }).toList();
    }

    @Override
    public void close() {
        logger.detachAppender(lista);
        lista.stop();
    }
}
