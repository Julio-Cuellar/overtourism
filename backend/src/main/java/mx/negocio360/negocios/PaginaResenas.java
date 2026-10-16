package mx.negocio360.negocios;

import java.util.List;

/** Una pagina de reseñas del panel con el resumen de calificaciones del negocio. */
public record PaginaResenas(List<ResenaVista> resenas, boolean hayMas, String cursorFinal, CalificacionVista resumen) {

    public PaginaResenas {
        resenas = List.copyOf(resenas);
    }
}
