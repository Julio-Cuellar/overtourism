package mx.negocio360.compartido.graphql;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Un campo de la raiz del esquema (consulta, mutacion o suscripcion) y lo que exige para ejecutarse (SG-03, SG-04 y
 * SG-21). Cada modulo declara los suyos, como declara sus permisos. Hay cinco formas, excluyentes:
 * <ul>
 *   <li>un {@code permiso}: la persona debe tenerlo;
 *   <li>un {@code argumentoDeComando}: el permiso depende de cual variante del comando trae ese argumento (el tipo de
 *       entrada {@code @oneOf}), segun la tabla de {@link ComandoDeclarado};
 *   <li>ninguno: basta una sesion con negocio activo. Es lo que piden los campos genericos de la plataforma, cuyo
 *       permiso decide el modulo dueño de cada comando;
 *   <li>{@link Acceso#CUENTA}: basta una sesion, aunque todavia no haya negocio activo;
 *   <li>{@link Acceso#PUBLICA}: cualquiera, sin sesion. Solo para lo que se publica a proposito.
 * </ul>
 * Si el permiso es sensible (lo dice su declaracion), ademas exige la confirmacion reforzada (SG-19).
 */
public record OperacionDeclarada(TipoDeOperacion tipo, String campo, String permiso, String argumentoDeComando,
                                 Acceso acceso) {

    private static final Pattern NOMBRE = Pattern.compile("[_A-Za-z][_0-9A-Za-z]*");

    public OperacionDeclarada {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(acceso, "acceso");
        if (campo == null || !NOMBRE.matcher(campo).matches()) {
            throw new IllegalArgumentException("El campo no es un nombre de GraphQL valido");
        }
        if (permiso != null && argumentoDeComando != null) {
            throw new IllegalArgumentException("Una operacion pide un permiso o decide por la variante del comando, no ambos");
        }
        if (acceso != Acceso.NEGOCIO && (permiso != null || argumentoDeComando != null)) {
            throw new IllegalArgumentException("Una operacion publica o de cuenta no pide permiso ni comando");
        }
        if ((permiso != null && permiso.isBlank()) || (argumentoDeComando != null && argumentoDeComando.isBlank())) {
            throw new IllegalArgumentException("El permiso y el argumento de comando no pueden estar en blanco");
        }
    }

    /** Pide una sesion con negocio activo (y, segun la forma, un permiso o el de la variante del comando). */
    public OperacionDeclarada(TipoDeOperacion tipo, String campo, String permiso, String argumentoDeComando) {
        this(tipo, campo, permiso, argumentoDeComando, Acceso.NEGOCIO);
    }

    /** Pide ese permiso. */
    public static OperacionDeclarada de(TipoDeOperacion tipo, String campo, String permiso) {
        return new OperacionDeclarada(tipo, campo, Objects.requireNonNull(permiso, "permiso"), null);
    }

    /** Basta una sesion con negocio activo. */
    public static OperacionDeclarada deSesion(TipoDeOperacion tipo, String campo) {
        return new OperacionDeclarada(tipo, campo, null, null);
    }

    /** El permiso lo decide la variante del comando que viaja en ese argumento. */
    public static OperacionDeclarada porComando(TipoDeOperacion tipo, String campo, String argumento) {
        return new OperacionDeclarada(tipo, campo, null, Objects.requireNonNull(argumento, "argumento"));
    }

    /** Cualquiera puede ejecutarla, sin sesion: solo para lo que se publica a proposito (SG-01 lo permite aqui y en nada mas). */
    public static OperacionDeclarada publica(TipoDeOperacion tipo, String campo) {
        return new OperacionDeclarada(tipo, campo, null, null, Acceso.PUBLICA);
    }

    /** Basta una sesion, aunque no haya negocio activo. */
    public static OperacionDeclarada deCuenta(TipoDeOperacion tipo, String campo) {
        return new OperacionDeclarada(tipo, campo, null, null, Acceso.CUENTA);
    }

    /** {@code Query.clientes}, para los mensajes. */
    public String nombre() {
        return tipo.etiqueta() + "." + campo;
    }
}
