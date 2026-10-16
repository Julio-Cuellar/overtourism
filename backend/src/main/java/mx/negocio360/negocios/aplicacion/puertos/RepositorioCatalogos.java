package mx.negocio360.negocios.aplicacion.puertos;

import java.util.List;

/** Los catalogos globales, de solo lectura para la aplicacion (CA-05): giros, subcategorias y atributos. */
public interface RepositorioCatalogos {

    record Giro(String codigo, String nombre) {
    }

    record Subcategoria(String codigo, String giro, String nombre) {
    }

    record Atributo(String tipo, String codigo, String nombre) {
    }

    /** Giros activos. */
    List<Giro> giros();

    /** Subcategorias activas, en el orden del catalogo. */
    List<Subcategoria> subcategorias();

    /** Atributos activos, en el orden del catalogo. */
    List<Atributo> atributos();
}
