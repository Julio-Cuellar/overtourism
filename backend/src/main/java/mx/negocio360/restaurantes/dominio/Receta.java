package mx.negocio360.restaurantes.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * La receta de un platillo (RC-01): una lista de versiones inmutables. Guardar crea la siguiente y cierra la abierta en ese
 * mismo instante, asi que nunca hay dos vigentes. Una venta usa la version que estaba vigente en su momento, no la de cuando
 * se procesa (RC-07). Desactivar cierra la version abierta; guardar de nuevo la reactiva.
 *
 * @param id          identificador
 * @param negocioId   negocio dueño
 * @param platilloId  platillo (su {@code producto_id})
 * @param versiones   de la mas vieja a la mas nueva
 * @param version     bloqueo optimista de la receta, desde 1
 * @param creadaEn    cuando se creo
 * @param actualizadaEn ultimo cambio
 */
public record Receta(UUID id, UUID negocioId, UUID platilloId, List<RecetaVersion> versiones, long version, Instant creadaEn,
                     Instant actualizadaEn) {

    /** Tope de insumos por version (RC-02). */
    public static final int MAX_INSUMOS = 30;
    /** Tope de porciones por version (RC-04). */
    public static final int MAX_RENDIMIENTO = 500;

    public Receta {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(platilloId, "platilloId");
        versiones = List.copyOf(Objects.requireNonNull(versiones, "versiones"));
        Objects.requireNonNull(creadaEn, "creadaEn");
        Objects.requireNonNull(actualizadaEn, "actualizadaEn");
    }

    /**
     * Una receta nueva con su version 1.
     *
     * @throws RestaurantesException RECETA_INVALIDA o INSUMO_REPETIDO
     */
    public static Receta nueva(UUID id, UUID negocioId, UUID platilloId, int rendimiento, List<InsumoDeReceta> insumos,
                               String creadaPor, Instant ahora) {
        exigirValida(rendimiento, insumos);
        return new Receta(id, negocioId, platilloId,
                List.of(new RecetaVersion(1, rendimiento, ahora, null, insumos, creadaPor)), 1, ahora, ahora);
    }

    /**
     * Comprueba lo que una version tiene que cumplir: de 1 a 30 insumos sin repetir articulo y un rendimiento de 1 a 500.
     *
     * @throws RestaurantesException RECETA_INVALIDA o INSUMO_REPETIDO
     */
    public static void exigirValida(int rendimiento, List<InsumoDeReceta> insumos) {
        if (rendimiento < 1 || rendimiento > MAX_RENDIMIENTO || insumos == null || insumos.isEmpty()
                || insumos.size() > MAX_INSUMOS) {
            throw new RestaurantesException(CodigoError.RECETA_INVALIDA);
        }
        var vistos = new HashSet<UUID>();
        for (var insumo : insumos) {
            if (insumo == null) {
                throw new RestaurantesException(CodigoError.RECETA_INVALIDA);
            }
            if (!vistos.add(insumo.articuloId())) {
                throw new RestaurantesException(CodigoError.INSUMO_REPETIDO);
            }
        }
    }

    /** Guarda la version siguiente: cierra la abierta, si hay, y abre la nueva en el mismo instante. */
    public Receta conNuevaVersion(int rendimiento, List<InsumoDeReceta> insumos, String creadaPor, Instant ahora) {
        exigirValida(rendimiento, insumos);
        var nuevas = new ArrayList<RecetaVersion>();
        for (var v : versiones) {
            nuevas.add(v.vigenteHasta() == null ? v.cerrada(ahora) : v);
        }
        var numero = versiones.isEmpty() ? 1 : versiones.get(versiones.size() - 1).numero() + 1;
        nuevas.add(new RecetaVersion(numero, rendimiento, ahora, null, insumos, creadaPor));
        return new Receta(id, negocioId, platilloId, nuevas, version + 1, creadaEn, ahora);
    }

    /** Cierra la version abierta: la receta deja de estar activa y las ventas de antes aun la encuentran. */
    public Receta desactivada(Instant ahora) {
        var nuevas = versiones.stream().map(v -> v.vigenteHasta() == null ? v.cerrada(ahora) : v).toList();
        return new Receta(id, negocioId, platilloId, nuevas, version + 1, creadaEn, ahora);
    }

    /** La version abierta, si la receta esta activa. */
    public Optional<RecetaVersion> versionVigente() {
        return versiones.stream().filter(v -> v.vigenteHasta() == null).findFirst();
    }

    /** La version que estaba vigente en ese instante (RC-07). */
    public Optional<RecetaVersion> versionEn(Instant instante) {
        return versiones.stream().filter(v -> v.vigenteEn(instante)).findFirst();
    }

    /** Verdadero si tiene una version abierta. */
    public boolean activa() {
        return versionVigente().isPresent();
    }

    /** La ultima version guardada, vigente o no. */
    public Optional<RecetaVersion> ultimaVersion() {
        return versiones.isEmpty() ? Optional.empty() : Optional.of(versiones.get(versiones.size() - 1));
    }
}
