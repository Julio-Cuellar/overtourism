package mx.negocio360.restaurantes.aplicacion;

import java.util.List;
import mx.negocio360.restaurantes.dominio.ModoServicio;

/**
 * Todo el salon de un restaurante en una sola consulta: el servicio, la disponibilidad, las zonas activas y las mesas
 * activas con su estado derivado.
 */
public record SalonVista(boolean servicioAbierto, boolean qrHabilitado, ModoServicio modo, DisponibilidadVista disponibilidad,
                         List<ZonaVista> zonas, List<MesaVista> mesas) {
}
