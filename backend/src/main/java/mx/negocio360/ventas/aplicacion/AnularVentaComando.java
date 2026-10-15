package mx.negocio360.ventas.aplicacion;

import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;

/**
 * VF-11 AnularVenta (D-82). Lo prepara y confirma un actor con permiso de anular (VT-09).
 *
 * @param ventaId venta a anular
 * @param motivo  por que se anula, de 1 a 200 caracteres
 */
public record AnularVentaComando(UUID ventaId, String motivo) implements Comando {
}
