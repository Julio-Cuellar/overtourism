package mx.negocio360.ventas.aplicacion;

import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;
import org.springframework.modulith.NamedInterface;

/**
 * VF-11 AnularVenta (D-141). Lo prepara y confirma un actor con permiso de anular (VT-09).
 *
 * @param ventaId venta a anular
 * @param motivo  por que se anula, de 1 a 200 caracteres
 */
@NamedInterface("comandos")
public record AnularVentaComando(UUID ventaId, String motivo) implements Comando {

    /** El motivo es texto libre y puede traer datos personales: no se imprime. */
    @Override
    public String toString() {
        return "AnularVentaComando[ventaId=" + ventaId + "]";
    }
}
