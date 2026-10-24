package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.EstadoDeCuenta;
import mx.negocio360.restaurantes.dominio.OrigenDeCuenta;

/** Una cuenta con sus lineas tal como la ve el personal; no lleva datos personales del cliente (CT-12). */
public record CuentaVista(UUID id, int numero, UUID mesaId, String mesa, EstadoDeCuenta estado, OrigenDeCuenta origen,
                          Integer comensales, Instant abiertaEn, Instant solicitadaEn, boolean presenciaConfirmada,
                          BigDecimal total, boolean discrepancia, boolean ventaAnulada, UUID ventaId,
                          List<LineaVista> lineas, long version) {
}
