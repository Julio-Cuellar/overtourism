package mx.negocio360.identidad.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import mx.negocio360.identidad.aplicacion.PlantillasIdentidad;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.IdentidadException;

/**
 * Traduce los errores de Identidad a HTTP con un cuerpo fijo {@code {codigo, mensaje}} (SI-14). Nunca devuelve
 * trazas, nombres de clase ni lo que envio la persona. Solo aplica a los controladores de Identidad, para no cambiar
 * las respuestas de otros modulos (por ejemplo el 404 de las rutas cerradas).
 */
@RestControllerAdvice(assignableTypes = {AuthController.class, DevCanalController.class})
public class ErroresIdentidadAdvice {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ErroresIdentidadAdvice.class);

    public record ErrorRespuesta(String codigo, String mensaje) {
    }

    @ExceptionHandler(IdentidadException.class)
    public ResponseEntity<ErrorRespuesta> deNegocio(IdentidadException error) {
        return respuesta(estadoDe(error.codigo()), error.codigo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> cuerpoIlegible(HttpMessageNotReadableException error) {
        return respuesta(HttpStatus.BAD_REQUEST, CodigoError.SOLICITUD_INVALIDA);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> parametroFaltante(MissingServletRequestParameterException error) {
        return respuesta(HttpStatus.BAD_REQUEST, CodigoError.SOLICITUD_INVALIDA);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> tipoNoSoportado(HttpMediaTypeNotSupportedException error) {
        return respuesta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, CodigoError.SOLICITUD_INVALIDA);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception error) {
        // Solo la clase: el mensaje de una excepcion ajena podria llevar datos de la peticion.
        REGISTRO.error("Error inesperado en Identidad: {}", error.getClass().getSimpleName());
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO);
    }

    private static ResponseEntity<ErrorRespuesta> respuesta(HttpStatus estado, CodigoError codigo) {
        var cuerpo = new ErrorRespuesta(codigo.name(), PlantillasIdentidad.texto(codigo));
        var constructor = ResponseEntity.status(estado);
        if (codigo == CodigoError.NO_AUTENTICADO) {
            constructor.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return constructor.body(cuerpo);
    }

    private static HttpStatus estadoDe(CodigoError codigo) {
        return switch (codigo) {
            case TELEFONO_INVALIDO, NOMBRE_INVALIDO, SOLICITUD_INVALIDA -> HttpStatus.BAD_REQUEST;
            case NOMBRE_REQUERIDO -> HttpStatus.UNPROCESSABLE_ENTITY;
            case CODIGO_INVALIDO, CODIGO_EXPIRADO, CODIGO_AGOTADO, NO_AUTENTICADO -> HttpStatus.UNAUTHORIZED;
            case TASA_EXCEDIDA -> HttpStatus.TOO_MANY_REQUESTS;
            case ERROR_TEMPORAL -> HttpStatus.SERVICE_UNAVAILABLE;
            case ERROR_INTERNO -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
