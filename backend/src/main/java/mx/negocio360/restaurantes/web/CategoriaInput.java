package mx.negocio360.restaurantes.web;

import mx.negocio360.restaurantes.aplicacion.DatosCategoria;
import mx.negocio360.restaurantes.dominio.Estacion;

/** La entrada GraphQL {@code RestCategoriaInput}. */
public record CategoriaInput(String nombre, int orden, Estacion estacion) {

    /** Lo que recibe el servicio. */
    public DatosCategoria aDatos() {
        return new DatosCategoria(nombre, orden, estacion);
    }
}
