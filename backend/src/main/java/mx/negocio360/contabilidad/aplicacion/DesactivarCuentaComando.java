package mx.negocio360.contabilidad.aplicacion;

import java.util.UUID;

/**
 * Desactiva una cuenta. Una cuenta nunca se borra (C-03); desactivada deja de recibir movimientos (A-04).
 *
 * @param cuentaId cuenta a desactivar
 * @param version  version que la persona tenia a la vista
 */
public record DesactivarCuentaComando(UUID cuentaId, long version) implements Comando {
}
