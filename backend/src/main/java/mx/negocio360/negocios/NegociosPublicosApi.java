package mx.negocio360.negocios;

import java.util.Optional;
import java.util.UUID;

/**
 * Superficie publica de lectura para la app del turista y para Rutas (seccion 9.7, D-98, D-99). No exige sesion y solo
 * lee la vista publica con una lista permitida de campos (PU-03, SN-14). Un negocio que no es publico responde igual que
 * uno inexistente. Los topes (PU-05) se aplican aqui, aunque el cliente pida mas.
 */
public interface NegociosPublicosApi {

    Optional<NegocioPublicoVista> negocioPublico(UUID negocioId);

    /**
     * Hasta {@code LimitesPublicos.POR_PAGINA_MAXIMA} negocios por pagina (un {@code primeros} menor a uno cuenta uno).
     * El texto, el radio y el cursor se validan: lo que excede da {@code CONSULTA_INVALIDA}.
     */
    PaginaPublica negociosPublicos(FiltroPublico filtro, int primeros, String despues);

    /** Para otros horizontales (J2, R6): quien es el negocio, en que estado y donde. */
    Optional<VistaDeNegocio> vistaDeNegocio(UUID negocioId);
}
