package mx.negocio360.restaurantes.aplicacion;

/** Lo que responde Inventario a un consumo o a su reversa (K-01 a K-04 de inventario.md). */
public enum ResultadoDeConsumo {
    REGISTRADO, YA_REGISTRADO, OMITIDO_ANULADO_PREVIO, REVERTIDO, YA_REVERTIDO, MARCADO_ANULADO_PREVIO
}
