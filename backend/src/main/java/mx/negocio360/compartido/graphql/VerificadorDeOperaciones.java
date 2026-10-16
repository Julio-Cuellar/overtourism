package mx.negocio360.compartido.graphql;

import java.util.Objects;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.graphql.execution.GraphQlSource;

/** Al arrancar, comprueba que la tabla de operaciones y el esquema coincidan; si no, la aplicacion no arranca (SG-21). */
public final class VerificadorDeOperaciones implements SmartInitializingSingleton {

    private final TablaDeOperaciones tabla;
    private final GraphQlSource fuente;

    public VerificadorDeOperaciones(TablaDeOperaciones tabla, GraphQlSource fuente) {
        this.tabla = Objects.requireNonNull(tabla, "tabla");
        this.fuente = Objects.requireNonNull(fuente, "fuente");
    }

    @Override
    public void afterSingletonsInstantiated() {
        tabla.verificar(fuente.schema());
    }
}
