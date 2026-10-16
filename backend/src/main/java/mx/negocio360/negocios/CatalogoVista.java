package mx.negocio360.negocios;

import java.util.List;

/** Los catalogos que sirve {@code GET /catalogos/categorias} (R1): giros, subcategorias y atributos activos. */
public record CatalogoVista(List<Giro> giros, List<Subcategoria> subcategorias, List<Atributo> atributos) {

    public record Giro(String codigo, String nombre) {
    }

    public record Subcategoria(String codigo, String giro, String nombre) {
    }

    public record Atributo(String tipo, String codigo, String nombre) {
    }

    public CatalogoVista {
        giros = List.copyOf(giros);
        subcategorias = List.copyOf(subcategorias);
        atributos = List.copyOf(atributos);
    }
}
