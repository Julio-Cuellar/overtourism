package mx.negocio360.identidad.aplicacion.puertos;

/**
 * Canal por el que llega el codigo de acceso a la persona (SMS, WhatsApp o Telegram: se decide aparte, IDN-DOC.1).
 * En desarrollo y demo lo implementa un canal simulado. Las implementaciones nunca registran el codigo (SI-15).
 */
public interface CanalDeCodigos {

    /** Envia el codigo en claro al telefono E.164. Lanza {@link CanalNoDisponibleException} si no puede enviarlo. */
    void enviar(String telefono, String codigo);
}
