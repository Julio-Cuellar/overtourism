package mx.negocio360.negocios;

import java.util.List;

/**
 * El conjunto completo de atributos opcionales (G25, AT-02, AT-03): reemplaza lo declarado. Listas vacias y numeros
 * nulos borran lo que habia.
 */
public record DefinirAtributos(long version, List<String> dieta, List<String> accesibilidad, Integer duracionVisitaMin,
                               Integer costoMedioMxn) {
}
