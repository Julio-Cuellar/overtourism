package mx.negocio360.negocios;

/** Un punto y un radio para buscar por cercania. El radio tiene tope (PU-05). */
public record CercaDe(double latitud, double longitud, double radioKm) {
}
