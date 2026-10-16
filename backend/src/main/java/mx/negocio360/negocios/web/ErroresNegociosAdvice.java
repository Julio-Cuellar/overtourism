package mx.negocio360.negocios.web;

import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte los errores de negocio de los controladores REST de Negocios en respuestas con su codigo fijo y su texto fijo
 * (seccion 7.8). Nunca repite lo que escribio la persona (SG-13). Solo aplica a los controladores de este modulo.
 */
@RestControllerAdvice(basePackageClasses = ErroresNegociosAdvice.class)
public class ErroresNegociosAdvice {

    /** El cuerpo de todo error: el codigo y su texto. */
    public record ErrorRespuesta(String codigo, String mensaje) {
    }

    @ExceptionHandler(NegociosException.class)
    public ResponseEntity<ErrorRespuesta> deNegocio(NegociosException e) {
        return ResponseEntity.status(estadoDe(e.codigo())).body(new ErrorRespuesta(e.codigo().name(),
                e.codigo().mensaje()));
    }

    static HttpStatus estadoDe(CodigoError codigo) {
        return switch (codigo) {
            case NEGOCIO_NO_DISPONIBLE, SUCURSAL_NO_ENCONTRADA, EXCEPCION_NO_ENCONTRADA, RESENA_NO_ENCONTRADA ->
                    HttpStatus.NOT_FOUND;
            case CONCURRENCIA, RESENA_YA_EXISTE, RESENA_YA_RESPONDIDA, TOKEN_USADO -> HttpStatus.CONFLICT;
            case DEMASIADAS_SOLICITUDES -> HttpStatus.TOO_MANY_REQUESTS;
            case MODO_DEMOSTRACION_APAGADO -> HttpStatus.FORBIDDEN;
            case ERROR_TEMPORAL -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
