package mx.negocio360.clientes.fakes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.dominio.CodigoError;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Afirmaciones compartidas por las pruebas de Clientes. */
public final class Afirmaciones {

    private Afirmaciones() {
    }

    /** Exige que la accion falle con ClientesException y el codigo esperado, y devuelve la excepcion. */
    public static ClientesException fallaCon(ThrowingCallable accion, CodigoError esperado) {
        var capturada = catchThrowable(accion);
        assertThat(capturada).isInstanceOf(ClientesException.class);
        var excepcion = (ClientesException) capturada;
        assertThat(excepcion.codigo()).isEqualTo(esperado);
        return excepcion;
    }
}
