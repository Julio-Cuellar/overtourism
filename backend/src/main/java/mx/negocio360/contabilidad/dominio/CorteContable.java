package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Corte de caja visto por Contabilidad (tabla {@code corte_contable}).
 *
 * @param id                      UUIDv7
 * @param negocioId               negocio
 * @param corteId                 corte; referencia logica a Caja, sin llave foranea
 * @param cajaSesionId            sesion de caja
 * @param efectivoEsperado        efectivo que Caja esperaba
 * @param efectivoContado         efectivo contado
 * @param diferencia              contado menos esperado; negativa es faltante (J-01)
 * @param ventasEfectivoCaja      ventas en efectivo segun Caja, o nulo
 * @param ventasEfectivoContables ventas en efectivo contabilizadas de la sesion (J-03)
 * @param conciliacion            resultado de la conciliacion
 * @param descuadrePublicadoEn    cuando se publico el aviso de descuadre, o nulo (J-04)
 * @param polizaId                poliza de la diferencia de conteo, o nula
 * @param cerradaEn               momento del cierre
 */
public record CorteContable(UUID id, UUID negocioId, UUID corteId, UUID cajaSesionId, BigDecimal efectivoEsperado,
                            BigDecimal efectivoContado, BigDecimal diferencia, BigDecimal ventasEfectivoCaja,
                            BigDecimal ventasEfectivoContables, Conciliacion conciliacion,
                            Instant descuadrePublicadoEn, UUID polizaId, Instant cerradaEn) {
}
