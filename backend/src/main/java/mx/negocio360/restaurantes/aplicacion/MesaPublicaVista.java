package mx.negocio360.restaurantes.aplicacion;

import java.util.UUID;

/**
 * Lo que la pagina publica sabe de una mesa al escanear su QR (QR-03). Nunca lleva ids de cuenta, comensales, totales ni
 * lineas. El nombre del negocio no viaja aqui: lo da {@code negocioPublico} de Negocios con el {@code negocioId}.
 */
public record MesaPublicaVista(UUID negocioId, String mesa, boolean servicioAbierto, boolean aceptaPedidos,
                               EstadoPublicoDeCuenta estadoCuenta) {
}
