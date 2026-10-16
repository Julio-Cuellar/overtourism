package mx.negocio360.negocios;

import java.util.UUID;

/**
 * Las reseñas de cara al turista (T3, T4). En P1 funcionan en modo demostracion (DM-03): el autor es el id de dispositivo
 * que manda la app, sin cuenta ni sello (RS-01, RS-11), con un limite de 5 por hora por dispositivo. Publicar es preparar
 * y confirmar (RS-09, D-23): el token vence a los 5 minutos, se usa una vez y solo lo confirma el mismo dispositivo.
 */
public interface ResenasPublicasApi {

    /** Valida la reseña sin guardarla y devuelve el token. Con el modo apagado falla con MODO_DEMOSTRACION_APAGADO. */
    PreparacionResena prepararResena(String dispositivo, UUID negocioId, int calificacion, String comentario);

    /** Publica la reseña preparada. Falla con TOKEN_INVALIDO, TOKEN_EXPIRADO o TOKEN_USADO si el token no sirve. */
    ResultadoResena confirmarResena(String dispositivo, String token);

    /** Las reseñas publicadas de un negocio publico, de la mas nueva a la mas antigua. No depende del modo. */
    PaginaResenasPublicas resenasDeNegocio(UUID negocioId, int primeros, String despues);
}
