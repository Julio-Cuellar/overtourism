package mx.negocio360.negocios.aplicacion;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;

/**
 * El cursor opaco de las listas paginadas: un desplazamiento codificado. La app lo devuelve tal cual; un cursor que no es
 * de aqui, negativo o enorme da {@code CONSULTA_INVALIDA} (PU-05).
 */
final class CursorDeDesplazamiento {

    private static final String PREFIJO = "n1:";
    private static final int MAXIMO = 100_000;

    private CursorDeDesplazamiento() {
    }

    static String codificar(int desplazamiento) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((PREFIJO + desplazamiento).getBytes(StandardCharsets.UTF_8));
    }

    static int leer(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        try {
            var texto = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            if (!texto.startsWith(PREFIJO)) {
                throw new NegociosException(CodigoError.CONSULTA_INVALIDA);
            }
            var valor = Integer.parseInt(texto.substring(PREFIJO.length()));
            if (valor < 0 || valor > MAXIMO) {
                throw new NegociosException(CodigoError.CONSULTA_INVALIDA);
            }
            return valor;
        } catch (IllegalArgumentException e) {
            throw new NegociosException(CodigoError.CONSULTA_INVALIDA);
        }
    }
}
