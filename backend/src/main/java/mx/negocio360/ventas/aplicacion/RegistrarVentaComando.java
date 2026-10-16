package mx.negocio360.ventas.aplicacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;
import org.springframework.modulith.NamedInterface;

/**
 * VF-10 RegistrarVenta. Lo prepara y confirma el cajero desde el panel.
 *
 * <p>Copia sus listas al crearse (quien lo armo no puede cambiarlo despues) y admite listas nulas o con elementos
 * nulos, porque esos casos los reporta {@link CalculadoraVenta} con su codigo de error.
 *
 * @param turnoId     turno de Caja; Ventas pregunta a Caja si esta ABIERTO (VT-01, D-76)
 * @param lineas      productos y cantidades; el precio lo pone Catalogo (VT-02)
 * @param pagos       formas de pago; deben sumar el total (VT-04)
 * @param codigoCupon codigo del cupon del Pasaporte, opcional (VT-07)
 * @param clienteId   cliente elegido con {@code sugerirClientes}, opcional (VT-13)
 */
@NamedInterface("comandos")
public record RegistrarVentaComando(UUID turnoId, List<LineaSolicitada> lineas, List<PagoSolicitado> pagos,
                                    String codigoCupon, UUID clienteId) implements Comando {

    /** Copia defensiva e inmutable de lineas y pagos. */
    public RegistrarVentaComando {
        lineas = copia(lineas);
        pagos = copia(pagos);
    }

    private static <T> List<T> copia(List<T> lista) {
        return lista == null ? null : Collections.unmodifiableList(new ArrayList<>(lista));
    }

    /** El codigo del cupon vale como un vale al portador y el cliente es dato personal: no se imprimen. */
    @Override
    public String toString() {
        return "RegistrarVentaComando[turnoId=" + turnoId + ", lineas=" + (lineas == null ? 0 : lineas.size())
                + ", pagos=" + (pagos == null ? 0 : pagos.size()) + ", conCupon=" + (codigoCupon != null)
                + ", conCliente=" + (clienteId != null) + "]";
    }
}
