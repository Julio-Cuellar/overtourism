package mx.negocio360.clientes.infraestructura;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Corre trabajo de base de datos con el negocio fijado en la transaccion ({@code app.negocio_id}), que es lo que usa la
 * seguridad por fila de las tablas de Clientes: sin negocio la base no muestra ni deja escribir nada. El negocio sale
 * siempre del contexto autenticado que llega al repositorio, nunca de un dato del usuario.
 *
 * <p>Falla cerrado: una transaccion que ya tiene un negocio no puede cambiar a otro a la mitad (un descuido asi
 * mezclaria datos de dos negocios en una misma operacion). Repetir el mismo negocio si es valido, porque cada
 * repositorio lo fija al entrar y un caso de uso llama a varios.
 *
 * <p>{@link #en} se une a la transaccion en curso o abre una; {@link #enTransaccionPropia} abre siempre una aparte, que se
 * confirma aunque la de fuera se deshaga (el token usado, por ejemplo).
 */
public final class EjecutorEnNegocio {

    private final JdbcClient jdbc;
    private final TransactionTemplate unida;
    private final TransactionTemplate propia;

    public EjecutorEnNegocio(JdbcClient jdbc, PlatformTransactionManager transacciones) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        Objects.requireNonNull(transacciones, "transacciones");
        this.unida = new TransactionTemplate(transacciones);
        this.propia = new TransactionTemplate(transacciones);
        this.propia.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T en(UUID negocioId, Supplier<T> trabajo) {
        return unida.execute(estado -> fijarYCorrer(negocioId, trabajo));
    }

    public <T> T enTransaccionPropia(UUID negocioId, Supplier<T> trabajo) {
        return propia.execute(estado -> fijarYCorrer(negocioId, trabajo));
    }

    private <T> T fijarYCorrer(UUID negocioId, Supplier<T> trabajo) {
        Objects.requireNonNull(negocioId, "negocioId");
        var actual = jdbc.sql("SELECT coalesce(plataforma.negocio_actual()::text, '')").query(String.class).single();
        if (!actual.isEmpty() && !actual.equals(negocioId.toString())) {
            throw new IllegalStateException("La transaccion ya trabaja con otro negocio: no puede cambiar a mitad de camino");
        }
        jdbc.sql("SELECT set_config('app.negocio_id', :negocio, true)").param("negocio", negocioId.toString())
                .query(String.class).single();
        return trabajo.get();
    }
}
