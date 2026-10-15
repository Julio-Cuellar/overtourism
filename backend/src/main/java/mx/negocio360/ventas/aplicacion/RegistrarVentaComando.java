package mx.negocio360.ventas.aplicacion;

import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;

/**
 * VF-10 RegistrarVenta. Lo prepara y confirma el cajero desde el panel.
 *
 * @param turnoId     turno de Caja que el canal ya verifico ABIERTO (VT-01, D-76)
 * @param lineas      productos y cantidades; el precio lo pone Catalogo (VT-02)
 * @param pagos       formas de pago; deben sumar el total (VT-04)
 * @param codigoCupon codigo del cupon del Pasaporte, opcional (VT-07)
 * @param clienteId   cliente elegido con {@code sugerirClientes}, opcional (VT-13)
 */
public record RegistrarVentaComando(UUID turnoId, List<LineaSolicitada> lineas, List<PagoSolicitado> pagos,
                                    String codigoCupon, UUID clienteId) implements Comando {
}
