package mx.negocio360.restaurantes.dominio;

/**
 * Las mesas libres de un restaurante (MS-04): {@code libres} son las activas, no bloqueadas y sin cuenta; {@code total}, las
 * activas no bloqueadas; {@code abierto}, si el servicio esta abierto. Es lo que se publica para el turista (MS-06).
 */
public record Disponibilidad(int libres, int total, boolean abierto) {
}
