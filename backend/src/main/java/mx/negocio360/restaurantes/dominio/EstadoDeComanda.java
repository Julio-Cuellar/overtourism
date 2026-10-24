package mx.negocio360.restaurantes.dominio;

/**
 * Los estados de una comanda (CD-01). Solo se avanza de uno en uno; lo unico que se devuelve es {@code LISTA} a
 * {@code EN_PREPARACION}.
 */
public enum EstadoDeComanda {
    NUEVA,
    EN_PREPARACION,
    LISTA,
    ENTREGADA;

    /** Verdadero si desde este estado se puede pasar a {@code destino} avanzando (CD-01). */
    public boolean puedeAvanzarA(EstadoDeComanda destino) {
        return destino.ordinal() == ordinal() + 1;
    }

    /** Verdadero si la comanda ya estaba lista o entregada: ya hay merma si se anula una de sus lineas (CT-03). */
    public boolean preparada() {
        return this == LISTA || this == ENTREGADA;
    }
}
