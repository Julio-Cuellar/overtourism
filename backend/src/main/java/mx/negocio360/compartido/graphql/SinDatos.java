package mx.negocio360.compartido.graphql;

import java.util.ArrayList;

/**
 * Resume una excepcion para el registro sin su mensaje (SG-20): el mensaje de una excepcion puede traer SQL, valores de
 * columnas o datos personales. Quedan las clases de la cadena de causas y los primeros marcos de la pila, que bastan para
 * encontrar el codigo que fallo.
 */
final class SinDatos {

    private static final int MAX_MARCOS = 8;
    private static final int MAX_CAUSAS = 5;

    private SinDatos() {
    }

    static String clases(Throwable error) {
        var clases = new ArrayList<String>();
        for (Throwable actual = error; actual != null && clases.size() < MAX_CAUSAS; actual = actual.getCause()) {
            clases.add(actual.getClass().getName());
        }
        return String.join(" <- ", clases);
    }

    static String marcos(Throwable error) {
        var raiz = error;
        for (var i = 0; raiz.getCause() != null && raiz.getCause() != raiz && i < MAX_CAUSAS; i++) {
            raiz = raiz.getCause();
        }
        var marcos = new ArrayList<String>();
        for (var marco : raiz.getStackTrace()) {
            if (marcos.size() >= MAX_MARCOS) {
                break;
            }
            marcos.add(marco.getClassName() + "." + marco.getMethodName() + ":" + marco.getLineNumber());
        }
        return String.join(" <- ", marcos);
    }
}
