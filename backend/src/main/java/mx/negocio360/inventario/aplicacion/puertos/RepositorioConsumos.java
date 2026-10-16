package mx.negocio360.inventario.aplicacion.puertos;

import java.util.Optional;
import java.util.UUID;
import mx.negocio360.inventario.dominio.Consumo;

/**
 * Puerto de la tabla {@code consumo}: una fila por (negocio, causa, consumidor). La implementacion real escribe en la
 * misma transaccion que los movimientos y los saldos, que es lo que hace atomico a {@code registrarConsumo} (K-02).
 */
public interface RepositorioConsumos {

    /** El consumo de esa llave, si existe. */
    Optional<Consumo> porLlave(UUID negocioId, UUID causaId, String consumidor);

    /** Guarda un consumo nuevo; si ya hay uno con esa llave, falla con CONCURRENCIA. */
    void insertar(Consumo consumo);

    /** Reemplaza el consumo de esa llave; si no existe, falla con CONCURRENCIA. */
    void actualizar(Consumo consumo);
}
