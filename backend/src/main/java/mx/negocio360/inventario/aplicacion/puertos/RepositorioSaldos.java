package mx.negocio360.inventario.aplicacion.puertos;

import java.util.Optional;
import java.util.UUID;
import mx.negocio360.inventario.dominio.Saldo;

/** Puerto de la tabla {@code saldo}. Se actualiza en la misma transaccion que el movimiento. */
public interface RepositorioSaldos {

    Optional<Saldo> porArticulo(UUID negocioId, UUID articuloId);

    void insertar(Saldo saldo);

    /** Reemplaza el saldo solo si la version guardada es {@code versionEsperada}; si no, falla con CONCURRENCIA. */
    void actualizar(Saldo nuevo, long versionEsperada);
}
