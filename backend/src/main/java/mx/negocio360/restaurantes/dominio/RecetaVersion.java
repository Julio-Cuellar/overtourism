package mx.negocio360.restaurantes.dominio;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Una version de una receta (RC-01): inmutable una vez guardada. {@code vigenteHasta} es lo unico que se llena despues, una
 * sola vez, cuando llega la version siguiente o se desactiva la receta. El rendimiento va aqui y no en la receta para que una
 * version vieja conserve las porciones para las que se escribio (RC-07).
 *
 * @param numero        1, 2, 3...
 * @param rendimiento   porciones que salen con estas cantidades, de 1 a 500 (RC-04)
 * @param vigenteDesde  inicio de su vigencia
 * @param vigenteHasta  fin de su vigencia, o nulo si es la vigente
 * @param insumos       de 1 a 30, sin repetir articulo
 * @param creadaPor     actor que la guardo, para la auditoria
 */
public record RecetaVersion(int numero, int rendimiento, Instant vigenteDesde, Instant vigenteHasta,
                            List<InsumoDeReceta> insumos, String creadaPor) {

    public RecetaVersion {
        Objects.requireNonNull(vigenteDesde, "vigenteDesde");
        Objects.requireNonNull(creadaPor, "creadaPor");
        insumos = List.copyOf(Objects.requireNonNull(insumos, "insumos"));
    }

    /** Verdadero si esta version era la vigente en ese instante: desde, incluido, hasta, excluido. */
    public boolean vigenteEn(Instant instante) {
        return !instante.isBefore(vigenteDesde) && (vigenteHasta == null || instante.isBefore(vigenteHasta));
    }

    /** La misma version, cerrada en ese instante. */
    RecetaVersion cerrada(Instant instante) {
        return new RecetaVersion(numero, rendimiento, vigenteDesde, instante, insumos, creadaPor);
    }
}
