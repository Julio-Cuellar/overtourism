package mx.negocio360.restaurantes.aplicacion;

import mx.negocio360.restaurantes.dominio.Estacion;

/**
 * Lo que la persona escribe de una categoria del menu.
 *
 * @param nombre   de 2 a 40 caracteres
 * @param orden    de 0 a 999
 * @param estacion cocina o barra; nulo es cocina
 */
public record DatosCategoria(String nombre, int orden, Estacion estacion) {
}
