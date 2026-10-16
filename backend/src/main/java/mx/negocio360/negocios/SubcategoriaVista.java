package mx.negocio360.negocios;

/** Una subcategoria del negocio. {@code capacidad} es verdadero si es de otro giro que el principal (CA-03). */
public record SubcategoriaVista(String codigo, String nombre, String giro, boolean capacidad) {
}
