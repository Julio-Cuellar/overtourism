package mx.negocio360.clientes.aplicacion;

import java.util.List;

/**
 * Una pagina de clientes. {@code cursorFinal} es el id del ultimo cliente de la pagina (nulo si esta vacia); no
 * lleva datos personales.
 */
public record PaginaClientes(List<ClienteListado> nodos, boolean hayMas, String cursorFinal) {

    public PaginaClientes {
        nodos = List.copyOf(nodos);
    }

    @Override
    public String toString() {
        return "PaginaClientes[nodos=" + nodos.size() + ", hayMas=" + hayMas + "]";
    }
}
