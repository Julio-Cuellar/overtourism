package mx.negocio360.negocios;

import java.util.List;

/** Cuanto le falta al perfil (NG-11): porcentaje y lo que falta. */
public record CompletitudVista(int porcentaje, List<String> faltantes) {

    public CompletitudVista {
        faltantes = List.copyOf(faltantes);
    }
}
