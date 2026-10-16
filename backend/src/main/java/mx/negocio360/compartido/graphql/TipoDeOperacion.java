package mx.negocio360.compartido.graphql;

/** Los tres tipos de raiz de GraphQL. */
public enum TipoDeOperacion {
    QUERY("Query"),
    MUTATION("Mutation"),
    SUBSCRIPTION("Subscription");

    private final String etiqueta;

    TipoDeOperacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** El nombre habitual del tipo de raiz, para los mensajes. */
    public String etiqueta() {
        return etiqueta;
    }
}
