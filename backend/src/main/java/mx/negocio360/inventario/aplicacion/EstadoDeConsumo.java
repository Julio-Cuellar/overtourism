package mx.negocio360.inventario.aplicacion;

import org.springframework.modulith.NamedInterface;

/**
 * Que paso con una llamada a {@code registrarConsumo} o {@code revertirConsumo} (6.4). Repetir una llamada devuelve el
 * estado "YA_" sin tocar el saldo.
 */
@NamedInterface("api")
public enum EstadoDeConsumo {
    /** Se descontaron las lineas. */
    REGISTRADO,
    /** El consumo ya estaba registrado (o registrado y revertido): no se descuenta otra vez (K-01). */
    YA_REGISTRADO,
    /** La anulacion habia llegado antes: no se descuenta nada (K-04). */
    OMITIDO_ANULADO_PREVIO,
    /** Se repusieron las lineas al costo original (K-03). */
    REVERTIDO,
    /** Ya se habia revertido (K-03). */
    YA_REVERTIDO,
    /** No habia consumo: se dejo la marca para omitirlo si llega despues (K-04). */
    MARCADO_ANULADO_PREVIO
}
