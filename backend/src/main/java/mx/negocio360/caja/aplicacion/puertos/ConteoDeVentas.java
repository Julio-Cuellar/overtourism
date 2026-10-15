package mx.negocio360.caja.aplicacion.puertos;

/**
 * Lo que Caja necesita de Ventas para cerrar un turno (CJ-07).
 *
 * @param registradas           ventas que siguen registradas en ese turno
 * @param anuladas              ventas anuladas de ese turno
 * @param preparacionesVigentes ventas preparadas sin confirmar ni vencer en ese turno
 */
public record ConteoDeVentas(int registradas, int anuladas, long preparacionesVigentes) {
}
