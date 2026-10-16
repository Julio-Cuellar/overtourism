package mx.negocio360.negocios.infraestructura;

import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Corre lecturas como {@code rol_negocios_publico}, que no tiene permiso sobre ninguna tabla base y solo lee las vistas
 * publicas (SN-14, PU-03): cambia de rol solo dentro de su transaccion y lo devuelve al terminar, para no dejar el rol
 * publico puesto en una transaccion que siga con otras consultas.
 */
final class EjecutorPublico {

    private final JdbcClient jdbc;
    private final TransactionTemplate transaccion;

    EjecutorPublico(JdbcClient jdbc, PlatformTransactionManager transacciones) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.transaccion = new TransactionTemplate(Objects.requireNonNull(transacciones, "transacciones"));
        this.transaccion.setReadOnly(true);
    }

    <T> T leer(Supplier<T> consulta) {
        return transaccion.execute(estado -> {
            jdbc.sql("SET LOCAL ROLE rol_negocios_publico").update();
            try {
                return consulta.get();
            } finally {
                jdbc.sql("RESET ROLE").update();
            }
        });
    }
}
