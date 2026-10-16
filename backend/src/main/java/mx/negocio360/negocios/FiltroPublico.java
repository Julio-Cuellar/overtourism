package mx.negocio360.negocios;

import java.util.List;

/**
 * Lo que se puede pedir a la busqueda publica (T1, PU-05). Todo es opcional. Las listas piden todos los elementos;
 * un negocio que no declaro el atributo no pasa el filtro (AT-06).
 */
public record FiltroPublico(String texto, String giro, List<String> subcategorias, List<String> dieta,
                            List<String> accesibilidad, Boolean abiertoAhora, CercaDe cerca, OrdenPublico orden) {

    public static FiltroPublico todos() {
        return new FiltroPublico(null, null, null, null, null, null, null, null);
    }
}
