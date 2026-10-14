package mx.negocio360.clientes.dominio;

/** Estado del cliente. Anonimizar es irreversible y no existe borrado fisico (D-50). */
public enum EstadoCliente {
    ACTIVO,
    ANONIMIZADO
}
