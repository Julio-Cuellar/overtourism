package mx.negocio360.contabilidad.aplicacion;

import java.util.UUID;

/**
 * Revierte una poliza registrada con otra que invierte cada renglon (A-06). Es la unica forma de corregir una poliza,
 * porque las polizas no se editan (A-03).
 *
 * @param polizaId poliza a revertir
 * @param motivo   por que se revierte; obligatorio
 */
public record ReversarPolizaComando(UUID polizaId, String motivo) implements Comando {
}
