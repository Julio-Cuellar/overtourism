package mx.negocio360.compartido.graphql;

import java.util.Map;
import java.util.Objects;
import org.springframework.graphql.execution.ErrorType;

/**
 * Como sale un error de negocio hacia el cliente (SG-13): un codigo estable, un texto fijo (el que ya tiene la plantilla
 * del modulo) y, si ayuda, extensiones con ids o banderas; nunca el mensaje de la excepcion.
 *
 * @param codigo        va en {@code extensions.codigo}: es el contrato con el cliente
 * @param mensaje       texto fijo en espanol de Mexico, sin datos personales
 * @param extensiones   por ejemplo {@code recursoId} o {@code reintentable}
 * @param clasificacion como la entiende el cliente: peticion mala, sin permiso...
 */
public record ErrorTraducido(String codigo, String mensaje, Map<String, Object> extensiones,
                             ErrorType clasificacion) {

    public ErrorTraducido {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(mensaje, "mensaje");
        Objects.requireNonNull(clasificacion, "clasificacion");
        extensiones = Map.copyOf(extensiones);
    }

    /** Un error de peticion (BAD_REQUEST), que es lo normal para un error de negocio. */
    public ErrorTraducido(String codigo, String mensaje, Map<String, Object> extensiones) {
        this(codigo, mensaje, extensiones, ErrorType.BAD_REQUEST);
    }
}
