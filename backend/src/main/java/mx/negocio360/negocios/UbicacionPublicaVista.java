package mx.negocio360.negocios;

import java.math.BigDecimal;

/** Donde esta el negocio, para el turista. */
public record UbicacionPublicaVista(String calle, String numero, String colonia, String municipio,
                                    String entidadFederativa, String codigoPostal, String referencias,
                                    BigDecimal latitud, BigDecimal longitud) {
}
