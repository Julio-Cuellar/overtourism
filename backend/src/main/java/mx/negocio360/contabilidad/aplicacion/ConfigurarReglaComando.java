package mx.negocio360.contabilidad.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.Disparador;

/**
 * Configura una regla de contabilizacion nueva. Si ya hay una regla abierta para el mismo disparador y condicion, la
 * nueva la cierra en su fecha de inicio (F-01, C-04).
 *
 * @param disparador       hecho que la activa
 * @param condicion        medio de pago o financiador, o nulo
 * @param cuentaCargoId    cuenta que se carga
 * @param cuentaAbonoId    cuenta que se abona
 * @param cuentaImpuestoId cuenta de IVA; obligatoria si hay tasa
 * @param tasaIva          tasa de IVA, o nula si es exento
 * @param fuente           de donde salio la regla y cuando se verifico; obligatoria
 * @param vigenteDesde     inicio de vigencia; nulo cuenta como ahora
 */
public record ConfigurarReglaComando(Disparador disparador, String condicion, UUID cuentaCargoId,
                                     UUID cuentaAbonoId, UUID cuentaImpuestoId, BigDecimal tasaIva, String fuente,
                                     Instant vigenteDesde) implements Comando {
}
