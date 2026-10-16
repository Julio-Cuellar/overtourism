package mx.negocio360.negocios.infraestructura;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Corre trabajo de base de datos con el negocio ({@code app.negocio_id}) y/o la cuenta ({@code app.cuenta_id}) fijados
 * en la transaccion, que es lo que usa la seguridad por fila de las tablas de Negocios (SN-02, AL-04): sin contexto
 * la base no muestra ni deja escribir nada. El negocio y la cuenta salen siempre del contexto autenticado o del
 * evento, nunca de un dato del usuario.
 *
 * <p>Falla cerrado: una transaccion que ya tiene un negocio (o una cuenta) no puede cambiar a otro a la mitad. Repetir
 * el mismo si es valido, porque cada repositorio lo fija al entrar y un caso de uso llama a varios.
 */
public final class EjecutorEnContexto {

    private final JdbcClient jdbc;
    private final TransactionTemplate transaccion;

    public EjecutorEnContexto(JdbcClient jdbc, PlatformTransactionManager transacciones) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.transaccion = new TransactionTemplate(Objects.requireNonNull(transacciones, "transacciones"));
    }

    public <T> T enNegocio(UUID negocioId, Supplier<T> trabajo) {
        return transaccion.execute(estado -> {
            fijar("app.negocio_id", "plataforma.negocio_actual()", Objects.requireNonNull(negocioId, "negocioId"));
            return trabajo.get();
        });
    }

    public <T> T enCuenta(UUID cuentaId, Supplier<T> trabajo) {
        return transaccion.execute(estado -> {
            fijar("app.cuenta_id", "plataforma.cuenta_actual()", Objects.requireNonNull(cuentaId, "cuentaId"));
            return trabajo.get();
        });
    }

    public <T> T enNegocioYCuenta(UUID negocioId, UUID cuentaId, Supplier<T> trabajo) {
        return transaccion.execute(estado -> {
            fijar("app.negocio_id", "plataforma.negocio_actual()", Objects.requireNonNull(negocioId, "negocioId"));
            fijar("app.cuenta_id", "plataforma.cuenta_actual()", Objects.requireNonNull(cuentaId, "cuentaId"));
            return trabajo.get();
        });
    }

    private void fijar(String ajuste, String funcion, UUID valor) {
        var actual = jdbc.sql("SELECT coalesce(" + funcion + "::text, '')").query(String.class).single();
        if (!actual.isEmpty() && !actual.equals(valor.toString())) {
            throw new IllegalStateException("La transaccion ya trabaja con otro contexto: no puede cambiar a mitad de camino");
        }
        jdbc.sql("SELECT set_config(:ajuste, :valor, true)").param("ajuste", ajuste).param("valor", valor.toString())
                .query(String.class).single();
    }
}
