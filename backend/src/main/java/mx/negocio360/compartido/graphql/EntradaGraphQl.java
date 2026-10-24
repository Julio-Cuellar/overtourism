package mx.negocio360.compartido.graphql;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;
import mx.negocio360.compartido.seguridad.SesionDePeticion;

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
    private final String llave;

    private EntradaGraphQl(UUID cuentaId, ContextoNegocio contexto, Autorizacion autorizacion, String dispositivo,
                           String llave) {
        this.cuentaId = cuentaId;
        this.contexto = contexto;
        this.autorizacion = Objects.requireNonNull(autorizacion, "autorizacion");
        this.dispositivo = dispositivo;
        this.llave = llave;
    }

    /** Una sesion autenticada; {@code contexto} es nulo si todavia no eligio negocio. */
    public static EntradaGraphQl de(UUID cuentaId, ContextoNegocio contexto, Autorizacion autorizacion,
                                    String dispositivo) {
        return new EntradaGraphQl(Objects.requireNonNull(cuentaId, "cuentaId"), contexto, autorizacion, dispositivo, null);
    }

    /**
     * La entrada de una sesion que entrega el autenticador de la plataforma (la conexion WebSocket). El actor de la
     * sesion es el id de la cuenta; si no lo es, es un error de programacion y falla, sin repetir el actor.
     */
    public static EntradaGraphQl deSesion(SesionDePeticion sesion, String dispositivo) {
        UUID cuenta;
        try {
            cuenta = UUID.fromString(sesion.actor());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El actor de la sesion no es el id de una cuenta");
        }
        return new EntradaGraphQl(cuenta, sesion.contexto().orElse(null), sesion.autorizacion(), dispositivo, null);
    }

    /** Sin sesion: sin cuenta, sin contexto y sin ningun permiso. El dispositivo, si vino, se conserva. */
    public static EntradaGraphQl anonima(String dispositivo) {
        return new EntradaGraphQl(null, null, NADA, dispositivo, null);
    }

    /** La misma entrada con la llave de idempotencia que mando la peticion (el filtro ya comprobo su forma). */
    public EntradaGraphQl conLlave(String llave) {
        return new EntradaGraphQl(cuentaId, contexto, autorizacion, dispositivo, llave);
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

    /** La cuenta, si hay sesion; para el registro, que no debe fallar por no tenerla. */
    public Optional<UUID> cuenta() {
        return Optional.ofNullable(cuentaId);
    }

    /** El negocio activo, si lo hay; para el registro, que no debe fallar por no tenerlo. */
    public Optional<UUID> negocio() {
        return Optional.ofNullable(contexto).map(ContextoNegocio::negocioId);
    }

    /** Los permisos de la sesion; vacios si no hay sesion o negocio activo. Todo esta negado por omision. */
    public Autorizacion autorizacion() {
        return autorizacion;
    }

    /** El id de dispositivo que mando la app del turista (el filtro de entrada ya comprobo que es un UUID), o nulo. */
    public String dispositivo() {
        return dispositivo;
    }

    /**
     * La llave del encabezado {@code Idempotency-Key} (CT-14, D-208), o nula si no vino. Con la misma llave una operacion que
     * la exige no se repite; el servicio que la usa decide cuanto la recuerda.
     */
    public String llave() {
        return llave;
    }

    /** Sin la cuenta ni el dispositivo en el texto: no deben acabar en un registro. */
    @Override
    public String toString() {
        return "EntradaGraphQl[autenticada=" + autenticada() + "]";
    }
}
