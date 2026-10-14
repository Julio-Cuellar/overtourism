package mx.negocio360.identidad;

import java.util.Optional;

/**
 * API publica de Identidad (D-66, D-72). Los demas modulos la usan en el mismo proceso: el filtro de entrada llama a
 * {@link #autenticar} y fija el contexto de la peticion. Ninguna operacion recibe el negocio ni el actor como
 * argumento: salen de la sesion (AZ-05).
 *
 * <p>Los errores de negocio son {@code IdentidadException} con un {@code CodigoError}.
 */
public interface IdentidadApi {

    /** Envia un codigo de acceso al telefono. Responde igual exista o no la cuenta (SI-02). */
    void solicitarCodigo(String telefono, String ip);

    /** Verifica el codigo y abre una sesion; si el telefono es nuevo, crea la cuenta con el nombre dado. */
    SesionIniciada iniciarSesion(String telefono, String codigo, String nombre, String dispositivo, String ip);

    /** Autentica un token. Un token nulo, desconocido, vencido o revocado devuelve lo mismo: vacio (AS-08). */
    Optional<SesionAutenticada> autenticar(String token);

    /** Perfil de la cuenta de la sesion. Falla con NO_AUTENTICADO si el token no sirve. */
    PerfilVista yo(String token);

    /** Revoca la sesion de ese token. */
    void cerrarSesion(String token);

    /** Revoca todas las sesiones de la cuenta a la que pertenece el token. */
    void cerrarTodasLasSesiones(String token);

    /** Cambia el nombre visible de la cuenta de la sesion. */
    CuentaVista actualizarNombre(String token, String nombre);
}
