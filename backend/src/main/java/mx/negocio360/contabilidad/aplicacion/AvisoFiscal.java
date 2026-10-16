package mx.negocio360.contabilidad.aplicacion;

/**
 * Aviso que acompana a todo reporte de Contabilidad (F-03, D-104). La exportacion CSV lo llevara en su encabezado
 * cuando se autorice (pregunta 8 del diseno).
 */
public final class AvisoFiscal {

    /** Texto fijo en espanol de Mexico. */
    public static final String TEXTO = "Este reporte es de apoyo para tu persona contadora. Negocio 360 no emite CFDI "
            + "y no calcula declaraciones.";

    private AvisoFiscal() {
    }
}
