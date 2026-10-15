package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Configuracion contable de un negocio (tabla {@code configuracion}, una fila por negocio). Su existencia significa
 * que la contabilidad esta activa.
 *
 * @param negocioId          negocio
 * @param activadaEn         cuando se copio el catalogo base (C-01)
 * @param preciosConIva      si el total de la venta ya incluye IVA (F-01)
 * @param tasaIvaDefault     tasa para canjes que llegan antes que su venta, o nula si es exento
 * @param fuenteTasa         de donde salio la tasa y cuando se verifico
 * @param propinasEnTotal    si el cobro incluye propina (K-6)
 * @param ejercicioIniciaMes mes en que empieza el ejercicio, de 1 a 12
 * @param version            version para bloqueo optimista
 */
public record Configuracion(UUID negocioId, Instant activadaEn, boolean preciosConIva, BigDecimal tasaIvaDefault,
                            String fuenteTasa, boolean propinasEnTotal, int ejercicioIniciaMes, long version) {
}
