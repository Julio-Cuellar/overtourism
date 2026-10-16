package mx.negocio360.compartido.graphql.comandos;

/**
 * Un dato con nombre de un resultado, por ejemplo {@code folio} o {@code cambio}. El valor siempre es texto: el dinero
 * como decimal exacto.
 *
 * @param clave nombre del dato
 * @param valor valor como texto
 */
public record DatoVista(String clave, String valor) {
}
