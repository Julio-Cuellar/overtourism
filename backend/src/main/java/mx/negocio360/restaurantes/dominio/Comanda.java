package mx.negocio360.restaurantes.dominio;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Una comanda: lo que una estacion (cocina o barra) tiene que preparar de un envio (CT-04, CD-03). Cada estacion tiene su
 * propio estado, asi que una cemita lista en cocina no cambia la comanda de bebidas. Guarda actor y hora de cada cambio
 * (CD-01). Es inmutable.
 *
 * @param id          identificador
 * @param negocioId   negocio dueño
 * @param envioId     las comandas de un mismo envio lo comparten (CD-08)
 * @param cuentaId    la cuenta de la que salio
 * @param estacion    cocina o barra
 * @param estado      nueva, en preparacion, lista o entregada
 * @param lineaIds    las lineas de la cuenta que prepara esta comanda
 * @param enviadaEn   cuando se envio
 * @param listaEn     cuando quedo lista, o nulo (CD-04)
 * @param entregadaEn cuando se entrego, o nulo
 * @param ultimoActor quien hizo el ultimo cambio
 * @param ultimoCambioEn cuando
 * @param version     bloqueo optimista, desde 1
 */
public record Comanda(UUID id, UUID negocioId, UUID envioId, UUID cuentaId, Estacion estacion, EstadoDeComanda estado,
                      List<UUID> lineaIds, Instant enviadaEn, Instant listaEn, Instant entregadaEn, String ultimoActor,
                      Instant ultimoCambioEn, long version) {

    public Comanda {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(envioId, "envioId");
        Objects.requireNonNull(cuentaId, "cuentaId");
        Objects.requireNonNull(estacion, "estacion");
        Objects.requireNonNull(estado, "estado");
        lineaIds = List.copyOf(Objects.requireNonNull(lineaIds, "lineaIds"));
        Objects.requireNonNull(enviadaEn, "enviadaEn");
    }

    /** Una comanda nueva, recien enviada. */
    public static Comanda nueva(UUID id, UUID negocioId, UUID envioId, UUID cuentaId, Estacion estacion, List<UUID> lineaIds,
                                String actor, Instant ahora) {
        return new Comanda(id, negocioId, envioId, cuentaId, estacion, EstadoDeComanda.NUEVA, lineaIds, ahora, null, null,
                actor, ahora, 1);
    }

    /**
     * Pasa a otro estado (CD-01): avanzar de uno en uno, o devolver de lista a en preparacion.
     *
     * @throws RestaurantesException TRANSICION_INVALIDA
     */
    public Comanda a(EstadoDeComanda destino, String actor, Instant ahora) {
        var avanza = estado.puedeAvanzarA(destino);
        var devuelve = estado == EstadoDeComanda.LISTA && destino == EstadoDeComanda.EN_PREPARACION;
        if (!avanza && !devuelve) {
            throw new RestaurantesException(CodigoError.TRANSICION_INVALIDA);
        }
        return new Comanda(id, negocioId, envioId, cuentaId, estacion, destino, lineaIds, enviadaEn,
                destino == EstadoDeComanda.LISTA ? ahora : devuelve ? null : listaEn,
                destino == EstadoDeComanda.ENTREGADA ? ahora : entregadaEn, actor, ahora, version + 1);
    }

    /** Cierra la comanda porque todas sus lineas se anularon: ya no hay nada que preparar ni entregar. */
    public Comanda cerradaPorAnulacion(String actor, Instant ahora) {
        return new Comanda(id, negocioId, envioId, cuentaId, estacion, EstadoDeComanda.ENTREGADA, lineaIds, enviadaEn, listaEn,
                ahora, actor, ahora, version + 1);
    }

    /** Segundos entre el envio y el momento en que quedo lista, calculados por el codigo (CD-04); nulo si aun no esta. */
    public Long segundosDePreparacion() {
        return listaEn == null ? null : java.time.Duration.between(enviadaEn, listaEn).getSeconds();
    }
}
