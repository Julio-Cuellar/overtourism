package mx.negocio360.negocios;

import java.util.List;

/** Una pagina de la busqueda publica. {@code cursorFinal} es opaco: la app lo devuelve tal cual para pedir la siguiente. */
public record PaginaPublica(List<NegocioPublicoVista> negocios, boolean hayMas, String cursorFinal) {

    public PaginaPublica {
        negocios = List.copyOf(negocios);
    }
}
