package mx.negocio360.compartido.graphql.comandos;

import java.time.Instant;
import java.util.List;

/**
 * Lo que recibe quien prepara un comando: el token (credencial de confirmacion, no se imprime), el resumen que revisa la
 * persona y cuando vence.
 *
 * @param token        token de un solo uso; el despachador le antepone el prefijo de su modulo
 * @param resumen      texto en espanol de Mexico para confirmar
 * @param expiraEn     vencimiento
 * @param advertencias avisos para la persona; vacia si no hay
 */
public record PreparacionVista(String token, String resumen, Instant expiraEn, List<String> advertencias) {

    /** Copia los avisos para que la vista sea inmutable. */
    public PreparacionVista {
        advertencias = List.copyOf(advertencias);
    }

    /** Sin el token: es una credencial de confirmacion y no debe llegar a un registro. */
    @Override
    public String toString() {
        return "PreparacionVista[expiraEn=" + expiraEn + "]";
    }

    /** La misma vista con otro token (el que lleva el prefijo del modulo). */
    PreparacionVista conToken(String nuevo) {
        return new PreparacionVista(nuevo, resumen, expiraEn, advertencias);
    }
}
