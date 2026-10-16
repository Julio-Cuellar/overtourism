package mx.negocio360.compartido.graphql;

import java.util.List;

/**
 * Cada modulo declara las operaciones de GraphQL que aporta y lo que exigen (SG-21), igual que declara sus permisos con
 * {@code CatalogoDePermisos} (R1 y R4). La plataforma junta todo en una {@link TablaDeOperaciones} y, al arrancar,
 * comprueba que el esquema y las declaraciones coincidan exactamente.
 */
public interface OperacionesGraphql {

    /** Los campos de la raiz que declara este modulo. */
    List<OperacionDeclarada> operaciones();

    /** Las variantes del tipo de comando que aporta este modulo, con el permiso de cada una. */
    default List<ComandoDeclarado> comandos() {
        return List.of();
    }
}
