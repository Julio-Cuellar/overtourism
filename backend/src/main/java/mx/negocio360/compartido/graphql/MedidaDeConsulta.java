package mx.negocio360.compartido.graphql;

/**
 * Lo que cuesta una operacion GraphQL. Cada cifra se satura en {@link MedidorDeConsulta#TOPE}: una consulta hecha para
 * desbordar el contador no lo logra.
 *
 * @param profundidad niveles de campos anidados en la rama mas larga
 * @param complejidad 1 por campo mas la de sus hijos, expandiendo los fragmentos
 * @param alias       campos con alias
 * @param camposRaiz  campos pedidos en la raiz de la operacion
 */
public record MedidaDeConsulta(int profundidad, int complejidad, int alias, int camposRaiz) {
}
