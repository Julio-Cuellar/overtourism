package mx.negocio360.negocios;

import java.util.UUID;

/**
 * La moderacion de la plataforma (R6, RS-05, D-110): solo la plataforma oculta una reseña o la restituye. Quien la llama es
 * el servicio de administracion, que es otro proyecto (D-108). Por ahora nadie modera de forma activa.
 */
public interface AdministracionApi {

    /** Oculta una reseña: deja de verse y de contar en el resumen. {@code motivo} es un codigo de {@code A-Z0-9_}. */
    ResenaVista ocultarResena(UUID negocioId, UUID resenaId, String motivo, String operador);

    ResenaVista restituirResena(UUID negocioId, UUID resenaId, String operador);
}
