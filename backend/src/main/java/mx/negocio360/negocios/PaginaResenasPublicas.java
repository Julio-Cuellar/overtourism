package mx.negocio360.negocios;

import java.util.List;

/** Una pagina de reseñas publicas. El cursor es opaco: la app lo devuelve tal cual. */
public record PaginaResenasPublicas(List<ResenaPublicaVista> resenas, boolean hayMas, String cursorFinal) {

    public PaginaResenasPublicas {
        resenas = List.copyOf(resenas);
    }
}
