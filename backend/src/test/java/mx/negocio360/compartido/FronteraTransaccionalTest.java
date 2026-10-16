package mx.negocio360.compartido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import mx.negocio360.compartido.transaccion.FronteraTransaccional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * La frontera transaccional hace que cada llamada a un servicio de aplicacion sea una sola transaccion, sin poner
 * anotaciones de Spring en la capa de aplicacion (sus clases son final y no se pueden proxificar con CGLIB).
 */
class FronteraTransaccionalTest {

    /** Error de negocio esperado: no debe deshacer lo que ya se guardo (por ejemplo, un intento fallido que se cuenta). */
    static final class ErrorDeNegocio extends RuntimeException {
        ErrorDeNegocio() {
            super("negocio");
        }
    }

    public interface Servicio {
        String hacer(String entrada);

        void fallarDeNegocio();

        void fallarInesperado();

        void fallarVerificado() throws IOException;
    }

    /** Anota si cada llamada corrio dentro de una transaccion activa. */
    static final class ServicioReal implements Servicio {
        final List<Boolean> enTransaccion = new ArrayList<>();

        @Override
        public String hacer(String entrada) {
            enTransaccion.add(TransactionSynchronizationManager.isActualTransactionActive());
            return entrada.toUpperCase();
        }

        @Override
        public void fallarDeNegocio() {
            enTransaccion.add(TransactionSynchronizationManager.isActualTransactionActive());
            throw new ErrorDeNegocio();
        }

        @Override
        public void fallarInesperado() {
            throw new IllegalStateException("inesperado");
        }

        @Override
        public void fallarVerificado() throws IOException {
            throw new IOException("verificado");
        }
    }

    private final GestorTransaccionalFalso gestor = new GestorTransaccionalFalso();
    private final ServicioReal real = new ServicioReal();
    private final Servicio servicio = FronteraTransaccional.envolver(Servicio.class, real, gestor.plantilla(),
            ErrorDeNegocio.class);

    @Test
    @DisplayName("cada llamada corre dentro de una transaccion y devuelve el resultado del servicio")
    void llamada_correEnUnaTransaccionYDevuelveElResultado() {
        var resultado = servicio.hacer("hola");

        assertThat(resultado).isEqualTo("HOLA");
        assertThat(real.enTransaccion).containsExactly(true);
        assertThat(gestor.historia()).containsExactly("inicio", "commit");
    }

    @Test
    @DisplayName("un error inesperado deshace la transaccion y llega tal cual al llamador")
    void errorInesperado_deshaceYSePropagaSinEnvolver() {
        assertThatThrownBy(servicio::fallarInesperado)
                .isInstanceOf(IllegalStateException.class).hasMessage("inesperado");

        assertThat(gestor.historia()).containsExactly("inicio", "rollback");
    }

    @Test
    @DisplayName("un error de negocio declarado no deshace lo ya hecho y llega tal cual al llamador")
    void errorDeNegocio_confirmaYSePropagaSinEnvolver() {
        assertThatThrownBy(servicio::fallarDeNegocio).isInstanceOf(ErrorDeNegocio.class);

        assertThat(real.enTransaccion).containsExactly(true);
        assertThat(gestor.historia()).containsExactly("inicio", "commit");
    }

    @Test
    @DisplayName("una excepcion verificada del servicio llega tal cual, sin UndeclaredThrowableException")
    void excepcionVerificada_sePropagaSinEnvolver() {
        assertThatThrownBy(servicio::fallarVerificado).isInstanceOf(IOException.class).hasMessage("verificado");

        assertThat(gestor.historia()).containsExactly("inicio", "rollback");
    }

    @Test
    @DisplayName("equals, hashCode y toString no abren transaccion")
    void metodosDeObject_noAbrenTransaccion() {
        assertThat(servicio.equals(servicio)).isTrue();
        assertThat(servicio.hashCode()).isEqualTo(servicio.hashCode());
        assertThat(servicio.toString()).isNotBlank();

        assertThat(gestor.historia()).isEmpty();
    }

    @Test
    @DisplayName("un objetivo que no implementa la interfaz se rechaza al armar la frontera, no al llamarla")
    void objetivoQueNoImplementaLaInterfaz_fallaAlArmar() {
        assertThatThrownBy(() -> FronteraTransaccional.envolver(Servicio.class, new Object(), gestor.plantilla()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
