package mx.negocio360.restaurantes.aplicacion;

import java.util.UUID;

/** Una linea de una comanda en el tablero: tachada si se anulo (CD-05) y resaltada si lleva marca de alergia (CD-06). */
public record LineaDeComandaVista(UUID lineaId, String platillo, int cantidad, String nota, boolean marcaAlergia,
                                  boolean anulada) {
}
