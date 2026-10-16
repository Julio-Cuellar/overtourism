package mx.negocio360.seguridad;

import java.util.List;

/** Solo pruebas: el nodo que devuelve {@code arbolDePrueba}. Sin hijos, porque las pruebas miden la consulta, no los datos. */
public record NodoDePrueba(String valor, NodoDePrueba hijo, List<NodoDePrueba> hermanos) {

    static NodoDePrueba hoja() {
        return new NodoDePrueba("hoja", null, List.of());
    }
}
