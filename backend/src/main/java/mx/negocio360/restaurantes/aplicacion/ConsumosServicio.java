package mx.negocio360.restaurantes.aplicacion;

import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;

/** Los consumos fallidos que ve quien tiene {@code restaurantes.consumo.ver} (CM-06): del negocio de la sesion, con cursor. */
public final class ConsumosServicio {

    /** Tope de una pagina (D-131). */
    public static final int MAX_PAGINA = 100;

    private final RepositorioConsumos consumos;

    /** @param consumos almacen de consumos fallidos */
    public ConsumosServicio(RepositorioConsumos consumos) {
        this.consumos = Objects.requireNonNull(consumos, "consumos");
    }

    /**
     * Consumos fallidos del mas nuevo al mas viejo. {@code primeros} menor que 1 o un cursor que no es de esta lista es una
     * consulta invalida; mas de 100 se recorta a 100.
     */
    public PaginaDeConsumosFallidos consumosFallidos(ContextoNegocio contexto, Autorizacion autorizacion, int primeros,
                                                     String despues) {
        autorizacion.exigir(PermisosDeRestaurantes.CONSUMO_VER);
        if (primeros < 1) {
            throw new RestaurantesException(CodigoError.CONSULTA_INVALIDA);
        }
        var todos = consumos.fallidos(contexto.negocioId());
        var inicio = 0;
        if (despues != null) {
            UUID cursor;
            try {
                cursor = UUID.fromString(despues);
            } catch (IllegalArgumentException e) {
                throw new RestaurantesException(CodigoError.CONSULTA_INVALIDA);
            }
            var posicion = -1;
            for (int i = 0; i < todos.size(); i++) {
                if (todos.get(i).id().equals(cursor)) {
                    posicion = i;
                    break;
                }
            }
            if (posicion < 0) {
                throw new RestaurantesException(CodigoError.CONSULTA_INVALIDA);
            }
            inicio = posicion + 1;
        }
        var fin = Math.min(inicio + Math.min(primeros, MAX_PAGINA), todos.size());
        return new PaginaDeConsumosFallidos(todos.subList(inicio, fin), fin < todos.size() ? todos.get(fin - 1).id().toString() : null);
    }
}
