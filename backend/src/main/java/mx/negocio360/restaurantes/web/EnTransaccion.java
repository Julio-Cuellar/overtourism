package mx.negocio360.restaurantes.web;

import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Hace que cada caso de uso de los resolvedores sea una sola transaccion (SR-04, D-103). El publicador de eventos de
 * Restaurantes exige una transaccion activa para que el evento quede en el outbox junto con el cambio y llegue a quien
 * lo escucha despues de confirmar; una falla a la mitad no deja evento. Los almacenes en memoria no se deshacen, pero el
 * evento si se descarta.
 */
final class EnTransaccion {

    private final TransactionOperations transacciones;

    EnTransaccion(TransactionOperations transacciones) {
        this.transacciones = Objects.requireNonNull(transacciones, "transacciones");
    }

    /** Ejecuta la accion dentro de una transaccion y devuelve su resultado. */
    <T> T ejecutar(Supplier<T> accion) {
        return transacciones.execute(estado -> accion.get());
    }

    /** Ejecuta la accion dentro de una transaccion. */
    void ejecutar(Runnable accion) {
        transacciones.executeWithoutResult(estado -> accion.run());
    }
}
