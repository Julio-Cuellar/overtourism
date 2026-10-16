package mx.negocio360.inventario.fakes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.InventarioException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Afirmaciones compartidas por las pruebas de Inventario. */
public final class Afirmaciones {

    private Afirmaciones() {
    }

    /** Exige que la accion falle con InventarioException y el codigo esperado, y devuelve la excepcion. */
    public static InventarioException fallaCon(ThrowingCallable accion, CodigoError esperado) {
        var capturada = catchThrowable(accion);
        assertThat(capturada).isInstanceOf(InventarioException.class);
        var excepcion = (InventarioException) capturada;
        assertThat(excepcion.codigo()).isEqualTo(esperado);
        return excepcion;
    }
}
