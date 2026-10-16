package mx.negocio360.identidad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API publica de Identidad (D-66, D-72). Los demas modulos la usan en el mismo proceso: el filtro de entrada llama a
 * {@link #autenticar} y fija el contexto de la peticion. Ninguna operacion recibe el negocio ni el actor como
 * argumento: salen de la sesion (AZ-05).
 *
 * <p>Los errores de negocio son {@code IdentidadException} con un {@code CodigoError}.
 */
public interface IdentidadApi {

    /**
     * Envia un codigo de acceso al correo de la cuenta (D-85). El identificador puede ser el celular, el correo o el id
     * (D-76); el codigo siempre llega al correo. Responde igual exista o no la cuenta (SI-02).
     */
    void solicitarCodigo(String identificador, String ip);

    /**
     * Verifica el codigo que llego al correo y abre una sesion en una cuenta que ya existe. Un codigo incorrecto,
     * vencido, agotado o de una cuenta que no existe fallan igual, con CODIGO_INVALIDO.
     */
    SesionIniciada iniciarSesion(String identificador, String codigo, String dispositivo, String ip);

    /**
     * Primer paso del registro (D-84): guarda el formulario como pre-registro, sin crear la cuenta, y manda un codigo al
     * correo. Valida primero la forma de los campos, sin tocar nada; despues que el correo y el celular esten libres. El
     * pre-registro y su codigo valen 15 minutos; volver a llenar el formulario con el mismo correo lo reemplaza.
     */
    PreregistroCreado registrar(SolicitudDeRegistro solicitud, String ip);

    /**
     * Segundo paso del registro (D-84): con el codigo que llego al correo se crea la cuenta, con el correo ya verificado,
     * se borra el pre-registro y se abre la sesion. Si venia un negocio, se pide al modulo de negocios por evento (D-78).
     * Todo fallo del codigo es CODIGO_INVALIDO.
     */
    SesionIniciada verificarRegistro(String correo, String codigo, String dispositivo, String ip);

    /**
     * Manda otro codigo al correo de un pre-registro y le da otros 15 minutos. Responde igual haya o no pre-registro
     * (SI-02); comparte el tope del formulario.
     */
    void reenviarCodigoDeRegistro(String correo, String ip);

    /** Autentica un token. Un token nulo, desconocido, vencido o revocado devuelve lo mismo: vacio (AS-08). */
    Optional<SesionAutenticada> autenticar(String token);

    /** Perfil de la cuenta de la sesion. Falla con NO_AUTENTICADO si el token no sirve. */
    PerfilVista yo(String token);

    /**
     * Elige el negocio activo (AS-06): emite otro token y revoca el actual (SI-07). Un negocio donde no se es miembro
     * activo da el mismo error que uno que no existe. No alarga la sesion.
     */
    SesionIniciada elegirNegocio(String token, UUID negocioId);

    /**
     * Pide al modulo de negocios que registre un negocio (D-78): publica un evento y devuelve el id que Identidad le
     * asigno. No crea el negocio; la persona queda como DUENO cuando el modulo de negocios avise que lo registro.
     */
    NegocioSolicitado solicitarNegocio(String token, String nombre, String giro);

    /** Roles que ofrece el giro del negocio activo, con sus permisos. Exige {@code identidad.equipo.ver}. */
    List<RolVista> roles(String token);

    /** Catalogo de permisos que declaran los modulos. Exige {@code identidad.equipo.ver}. */
    List<PermisoVista> permisos(String token);

    /** Revoca la sesion de ese token. */
    void cerrarSesion(String token);

    /** Revoca todas las sesiones de la cuenta a la que pertenece el token. */
    void cerrarTodasLasSesiones(String token);

    /** Cambia el nombre visible de la cuenta de la sesion. */
    CuentaVista actualizarNombre(String token, String nombre);
}
