package mx.negocio360.compartido.graphql;

import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import mx.negocio360.compartido.seguridad.ConfirmacionRequeridaException;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.core.convert.ConversionException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;

/**
 * La regla de SG-13 en un solo lugar, para los resolvedores de consultas y para los de suscripciones: que sale hacia el
 * cliente cuando una excepcion llega a GraphQL. Sin permiso, sin sesion, sin confirmacion reforzada, un error de negocio
 * (lo traduce su modulo) o un error interno con referencia; nunca el mensaje de la excepcion.
 */
final class ClasificadorDeErrores {

    /** Lo que sale hacia el cliente. */
    record Clasificado(ErrorType tipo, String codigo, String mensaje, Map<String, Object> extensiones) {
    }

    private static final Logger REGISTRO = LoggerFactory.getLogger(ResolvedorDeErroresGraphql.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final List<TraductorDeErrores> traductores;

    ClasificadorDeErrores(List<TraductorDeErrores> traductores) {
        this.traductores = List.copyOf(traductores);
    }

    /** @param origen el campo o la operacion donde fallo, solo para el registro */
    Clasificado clasificar(Throwable excepcion, String origen) {
        var causa = desenvolver(excepcion);
        if (causa instanceof PermisoDenegadoException || causa instanceof AccessDeniedException) {
            return fijo(ErrorType.FORBIDDEN, ResolvedorDeErroresGraphql.PERMISO_DENEGADO);
        }
        if (causa instanceof ConfirmacionRequeridaException) {
            return fijo(ErrorType.FORBIDDEN, ResolvedorDeErroresGraphql.CONFIRMACION_REQUERIDA);
        }
        if (causa instanceof NoAutenticadoException e) {
            return fijo(ErrorType.UNAUTHORIZED, e.codigo());
        }
        if (causa instanceof BindException || causa instanceof ConversionException) {
            // Un argumento que no se puede leer (un id que no es UUID, un enumerado que no existe): es una consulta mala. Deja
            // rastro (la clase y el campo, nunca el valor) para que un fallo de conversion dentro de un servicio no pase por culpa
            // del cliente sin que nadie lo vea.
            REGISTRO.warn("CONSULTA_INVALIDA campo={} causa={}", origen, SinDatos.clases(causa));
            return fijo(ErrorType.BAD_REQUEST, EnmascaradoDeErroresInterceptor.CONSULTA_INVALIDA);
        }
        if (causa instanceof AuthenticationException) {
            return fijo(ErrorType.UNAUTHORIZED, ResolvedorDeErroresGraphql.NO_AUTENTICADO);
        }
        for (var traductor : traductores) {
            var traducido = traductor.traducir(causa);
            if (traducido.isPresent()) {
                var t = traducido.get();
                var extensiones = new LinkedHashMap<String, Object>(t.extensiones());
                extensiones.put("codigo", t.codigo());
                return new Clasificado(t.clasificacion(), t.codigo(), t.mensaje(), extensiones);
            }
        }
        var referencia = nuevaReferencia();
        REGISTRO.error("ERROR_INTERNO referencia={} campo={} causa={} marcos={}", referencia, origen,
                SinDatos.clases(causa), SinDatos.marcos(causa));
        return new Clasificado(ErrorType.INTERNAL_ERROR, ResolvedorDeErroresGraphql.ERROR_INTERNO,
                ResolvedorDeErroresGraphql.ERROR_INTERNO, Map.of("codigo", ResolvedorDeErroresGraphql.ERROR_INTERNO,
                        "referencia", referencia));
    }

    private static Clasificado fijo(ErrorType tipo, String codigo) {
        return new Clasificado(tipo, codigo, codigo, Map.of("codigo", codigo));
    }

    static String nuevaReferencia() {
        var bytes = new byte[6];
        ALEATORIO.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Las excepciones de los resolvedores asincronos llegan envueltas; lo que importa es la causa. */
    private static Throwable desenvolver(Throwable excepcion) {
        var actual = excepcion;
        while (actual.getCause() != null && actual.getCause() != actual && (actual instanceof CompletionException
                || actual instanceof ExecutionException || actual instanceof InvocationTargetException)) {
            actual = actual.getCause();
        }
        return actual;
    }
}
