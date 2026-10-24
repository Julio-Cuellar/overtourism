package mx.negocio360.restaurantes.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.dominio.EstadoDeComanda;

/**
 * Una comanda del tablero. {@code segundosDePreparacion} lo calcula el codigo (CD-04) y {@code retrasada} es verdadero si
 * no esta lista ni entregada y lleva mas del umbral esperando.
 */
public record ComandaVista(UUID id, UUID envioId, UUID cuentaId, int numeroDeCuenta, String mesa, Estacion estacion,
                           EstadoDeComanda estado, Instant enviadaEn, Instant listaEn, Long segundosDePreparacion,
                           boolean retrasada, List<LineaDeComandaVista> lineas, long version) {
}
