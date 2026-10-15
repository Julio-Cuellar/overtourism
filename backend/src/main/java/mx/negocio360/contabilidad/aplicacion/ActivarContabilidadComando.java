package mx.negocio360.contabilidad.aplicacion;

import java.math.BigDecimal;

/**
 * Activa la contabilidad del negocio: copia el catalogo base y las reglas por defecto (C-01).
 *
 * @param preciosConIva      si el total de la venta ya incluye IVA; nulo cuenta como verdadero
 * @param tasaIvaDefault     tasa de IVA de las reglas por defecto, o nula si es exento
 * @param fuenteTasa         de donde salio la tasa y cuando se verifico; obligatoria (F-01)
 * @param propinasEnTotal    si el cobro incluye propina; nulo cuenta como falso
 * @param ejercicioIniciaMes mes en que empieza el ejercicio, de 1 a 12; nulo cuenta como enero
 */
public record ActivarContabilidadComando(Boolean preciosConIva, BigDecimal tasaIvaDefault, String fuenteTasa,
                                         Boolean propinasEnTotal, Integer ejercicioIniciaMes) implements Comando {
}
