package mx.negocio360.compartido.seguridad;

import java.util.UUID;

/**
 * La comprobacion de la confirmacion reforzada de las operaciones sensibles (SG-19, D-73): un permiso marcado
 * {@code sensible} no basta, hay que haber confirmado la accion con un codigo nuevo ligado a ella. Identidad la
 * implementara con IDN-3.2; esta interfaz fija el contrato para que la puerta de /graphql ya lo respete.
 *
 * <p>Mientras no haya una implementacion, la plataforma usa una que no confirma nada: toda operacion sensible se niega por
 * omision. Falla cerrado, y abrirla es agregar el bean, no quitar una proteccion. La forma de entregar la prueba (un
 * encabezado, un argumento) la define IDN-3.2; puede ampliar esta interfaz con metodos nuevos con valor por omision.
 */
@FunctionalInterface
public interface ConfirmacionReforzada {

    /**
     * Si esta persona confirmo, de forma reforzada y vigente, una accion que exige este permiso sensible.
     *
     * @param cuentaId  la cuenta que actua
     * @param negocioId su negocio activo
     */
    boolean confirmada(UUID cuentaId, UUID negocioId, String permiso);
}
