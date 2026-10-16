package mx.negocio360.restaurantes.dominio;

/**
 * Etiquetas que un negocio puede declarar de un platillo (MN-08, D-176). Son declaradas y no verificadas: la app las
 * rotula «declarado por el negocio». Los codigos de dieta coinciden con los de Negocios, sin llave foranea (R1).
 */
public enum EtiquetaPlatillo {
    VEGETARIANO, VEGANO, SIN_GLUTEN, SIN_LACTOSA, SIN_FRUTOS_SECOS, PICANTE, TIPICO_POBLANO, DE_TEMPORADA, CONTIENE_ALCOHOL
}
