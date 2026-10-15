package mx.negocio360.contabilidad.fakes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** Afirmaciones compartidas por las pruebas de Contabilidad. */
public final class Afirmaciones {

    private Afirmaciones() {
    }

    /** Exige que la accion falle con ContabilidadException y el codigo esperado, y devuelve la excepcion. */
    public static ContabilidadException fallaCon(ThrowingCallable accion, CodigoError esperado) {
        var capturada = catchThrowable(accion);
        assertThat(capturada).isInstanceOf(ContabilidadException.class);
        var excepcion = (ContabilidadException) capturada;
        assertThat(excepcion.codigo()).isEqualTo(esperado);
        return excepcion;
    }
}
