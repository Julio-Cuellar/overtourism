package mx.negocio360.contabilidad.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Resuelve un pendiente: lo contabiliza con las cuentas que elige la persona o lo descarta con un motivo (E-04).
 *
 * @param pendienteId pendiente
 * @param accion      CONTABILIZAR o DESCARTAR
 * @param motivo      motivo del descarte; obligatorio al descartar
 * @param renglones   renglones de la poliza; obligatorios al contabilizar
 */
public record ResolverPendienteComando(UUID pendienteId, Accion accion, String motivo,
                                       List<RenglonManual> renglones) implements Comando {

    /** Que hacer con el pendiente. */
    public enum Accion {
        /** Registrar una poliza con las cuentas elegidas. */
        CONTABILIZAR,
        /** Cerrar el pendiente sin poliza, con motivo. */
        DESCARTAR
    }

    /**
     * Comando para contabilizar el pendiente.
     *
     * @param pendienteId pendiente
     * @param renglones   renglones de la poliza
     * @return el comando
     */
    public static ResolverPendienteComando contabilizar(UUID pendienteId, List<RenglonManual> renglones) {
        return new ResolverPendienteComando(pendienteId, Accion.CONTABILIZAR, null, renglones);
    }

    /**
     * Comando para descartar el pendiente.
     *
     * @param pendienteId pendiente
     * @param motivo      por que se descarta
     * @return el comando
     */
    public static ResolverPendienteComando descartar(UUID pendienteId, String motivo) {
        return new ResolverPendienteComando(pendienteId, Accion.DESCARTAR, motivo, null);
    }
}
