package mx.negocio360.seguridad;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.seguridad.Autorizacion;

/**
 * Solo pruebas: guarda la entrada de GraphQL ({@link EntradaGraphQl}) de quien "hace" la operacion, igual que la deja el
 * filtro de entrada de {@code /graphql} en una peticion real. Las pruebas que llaman al manejador sin pasar por HTTP la
 * entregan como atributo de la peticion ({@link #atributos()}) o en el contexto de GraphQL ({@link #contextoGraphql()}).
 * Sin llamar a {@code entrar}, la entrada es anonima.
 */
public final class SesionDePrueba {

    public static final UUID NEGOCIO = UUID.fromString("0192f39f-1111-7000-8000-00000000a001");
    public static final UUID CUENTA = UUID.fromString("0192f39f-2222-7000-8000-00000000b001");

    private static final ThreadLocal<EntradaGraphQl> ACTUAL = new ThreadLocal<>();

    private SesionDePrueba() {
    }

    /** Una sesion con negocio activo y exactamente esos permisos. */
    public static void entrar(String... permisos) {
        entrar(NEGOCIO, permisos);
    }

    public static void entrar(UUID negocio, String... permisos) {
        ACTUAL.set(entrada(CUENTA, negocio, permisos));
    }

    /** Una sesion autenticada que todavia no eligio negocio. */
    public static void entrarSinNegocio() {
        ACTUAL.set(EntradaGraphQl.de(CUENTA, null, Autorizacion.de(Set.of()), null));
    }

    /** Una entrada sin sesion, con el id de dispositivo del turista. */
    public static void entrarAnonimo(String dispositivo) {
        ACTUAL.set(EntradaGraphQl.anonima(dispositivo));
    }

    public static void salir() {
        ACTUAL.remove();
    }

    /** Una entrada autenticada cualquiera, sin tocar la actual. */
    public static EntradaGraphQl entrada(UUID cuenta, UUID negocio, String... permisos) {
        var contexto = negocio == null ? null : new ContextoNegocio(negocio, cuenta.toString());
        return EntradaGraphQl.de(cuenta, contexto, Autorizacion.de(Set.of(permisos)), null);
    }

    /** La entrada actual, o una anonima si nadie ha entrado. */
    public static EntradaGraphQl entrada() {
        var actual = ACTUAL.get();
        return actual != null ? actual : EntradaGraphQl.anonima(null);
    }

    /** Los atributos de una {@code WebGraphQlRequest}: donde el filtro de entrada deja la entrada. */
    public static Map<String, Object> atributos() {
        return Map.of(EntradaGraphQl.ATRIBUTO, entrada());
    }

    /** El contexto de GraphQL de un motor ejecutado sin interceptores. */
    public static Map<Object, Object> contextoGraphql() {
        return Map.of(EntradaGraphQl.CLAVE, entrada());
    }
}
