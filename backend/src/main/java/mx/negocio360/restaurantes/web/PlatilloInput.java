package mx.negocio360.restaurantes.web;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.restaurantes.aplicacion.DatosPlatillo;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.EtiquetaPlatillo;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.dominio.Temporada;

/**
 * La entrada GraphQL {@code RestPlatilloInput}. La temporada llega como dos textos {@code MM-dd} que van juntos o no va
 * ninguno (MN-06); aqui se arma el objeto del servicio.
 */
public record PlatilloInput(String nombre, String descripcion, BigDecimal precio, UUID categoriaId,
                            Set<EtiquetaPlatillo> etiquetas, String temporadaDesde, String temporadaHasta) {

    /**
     * Lo que recibe el servicio.
     *
     * @throws RestaurantesException TEMPORADA_INVALIDA si solo viene un extremo o alguno no es mes y dia
     */
    public DatosPlatillo aDatos() {
        var sinDesde = temporadaDesde == null || temporadaDesde.isBlank();
        var sinHasta = temporadaHasta == null || temporadaHasta.isBlank();
        if (sinDesde != sinHasta) {
            throw new RestaurantesException(CodigoError.TEMPORADA_INVALIDA);
        }
        var temporada = sinDesde ? null : Temporada.de(temporadaDesde.strip(), temporadaHasta.strip());
        return new DatosPlatillo(nombre, descripcion, precio, categoriaId, etiquetas == null ? Set.of() : etiquetas, temporada);
    }
}
