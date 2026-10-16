package mx.negocio360.compartido.graphql;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;

/**
 * Lo que el filtro de entrada de {@code /graphql} le entrega a los resolvedores (IDN-4): la cuenta y el negocio de la sesion,
 * sus permisos y el id de dispositivo del turista. Sale de la sesion y de los encabezados, nunca de un argumento (SG-02). Una
 * peticion sin token es anonima: no tiene cuenta ni permisos y los resolvedores del panel fallan cerrado; lo publico no pide
 * nada. Va como atributo de la peticion y de ahi al contexto de GraphQL.
 */
public final class EntradaGraphQl {

    /** Atributo de la peticion HTTP donde el filtro deja la entrada. */
    public static final String ATRIBUTO = "mx.negocio360.entrada";
    /** Llave en el contexto de GraphQL. */
    public static final String CLAVE = "mx.negocio360.entrada";

    private static final Autorizacion NADA = Autorizacion.de(Set.of());

    private final UUID cuentaId;
    private final ContextoNegocio contexto;
    private final Autorizacion autorizacion;
    private final String dispositivo;

    private EntradaGraphQl(UUID cuentaId, ContextoNegocio contexto, Autorizacion autorizacion, String dispositivo) {
        this.cuentaId = cuentaId;
        this.contexto = contexto;
        this.autorizacion = Objects.requireNonNull(autorizacion, "autorizacion");
        this.dispositivo = dispositivo;
    }

    /** Una sesion autenticada; {@code contexto} es nulo si todavia no eligio negocio. */
    public static EntradaGraphQl de(UUID cuentaId, ContextoNegocio contexto, Autorizacion autorizacion,
                                    String dispositivo) {
        return new EntradaGraphQl(Objects.requireNonNull(cuentaId, "cuentaId"), contexto, autorizacion, dispositivo);
    }

    /** Sin sesion: sin cuenta, sin contexto y sin ningun permiso. El dispositivo, si vino, se conserva. */
    public static EntradaGraphQl anonima(String dispositivo) {
        return new EntradaGraphQl(null, null, NADA, dispositivo);
    }

    public boolean autenticada() {
        return cuentaId != null;
    }

    /** @throws NoAutenticadoException {@code NO_AUTENTICADO} si no hay sesion */
    public UUID exigirCuenta() {
        if (cuentaId == null) {
            throw new NoAutenticadoException(NoAutenticadoException.NO_AUTENTICADO);
        }
        return cuentaId;
    }

    /** @throws NoAutenticadoException {@code NO_AUTENTICADO} sin sesion y {@code SIN_NEGOCIO_ACTIVO} sin negocio activo */
    public ContextoNegocio exigirContexto() {
        exigirCuenta();
        if (contexto == null) {
            throw new NoAutenticadoException(NoAutenticadoException.SIN_NEGOCIO_ACTIVO);
        }
        return contexto;
    }

    /** Los permisos de la sesion; vacios si no hay sesion o negocio activo. Todo esta negado por omision. */
    public Autorizacion autorizacion() {
        return autorizacion;
    }

    /** El id de dispositivo que mando la app del turista, sin validar, o nulo. */
    public String dispositivo() {
        return dispositivo;
    }

    /** Sin la cuenta ni el dispositivo en el texto: no deben acabar en un registro. */
    @Override
    public String toString() {
        return "EntradaGraphQl[autenticada=" + autenticada() + "]";
    }
}
