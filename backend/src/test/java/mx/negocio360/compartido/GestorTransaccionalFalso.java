package mx.negocio360.compartido;

import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gestor de transacciones de mentira para pruebas unitarias: no toca ninguna base, pero marca la transaccion como
 * activa mientras dura (lo que mira {@code TransactionSynchronizationManager}) y anota como termino cada una.
 */
public final class GestorTransaccionalFalso implements PlatformTransactionManager {

    private final List<String> historia = new ArrayList<>();

    public TransactionTemplate plantilla() {
        return new TransactionTemplate(this);
    }

    /** {@code inicio}, {@code commit} o {@code rollback}, en orden. */
    public List<String> historia() {
        return List.copyOf(historia);
    }

    public int commits() {
        return (int) historia.stream().filter("commit"::equals).count();
    }

    public int rollbacks() {
        return (int) historia.stream().filter("rollback"::equals).count();
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definicion) {
        historia.add("inicio");
        TransactionSynchronizationManager.setActualTransactionActive(true);
        return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus estado) {
        historia.add("commit");
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Override
    public void rollback(TransactionStatus estado) {
        historia.add("rollback");
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }
}
