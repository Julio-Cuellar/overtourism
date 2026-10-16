package mx.negocio360.identidad.aplicacion.puertos;

/**
 * Canal por el que llega el codigo a la persona: hoy su correo (D-85); el servicio de mensajeria real se decide aparte
 * (IDN-DOC.1). En desarrollo y demo lo implementa un canal simulado, que lo escribe en la consola (D-77). Las
 * implementaciones reales nunca registran el codigo (SI-15).
 *
 * <p>LIMITE: hoy {@code enviar} corre dentro del {@code synchronized} del servicio, que tambien autentica cada peticion.
 * Un canal real (IDN-DOC.1) lento paralizaria el acceso, y su latencia delataria que la cuenta existe cuando se pide el
 * codigo de acceso. Debe tener un tiempo maximo corto y, cuando haya Postgres (IDN-1.1), enviarse fuera de la
 * transaccion o por outbox.
 */
public interface CanalDeCodigos {

    /** Envia el codigo en claro al correo (en minusculas). Lanza {@link CanalNoDisponibleException} si no puede enviarlo. */
    void enviar(String destino, String codigo);
}
