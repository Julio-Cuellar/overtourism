package mx.negocio360.identidad.aplicacion;

import java.time.Duration;
import java.util.Objects;

/**
 * Valores que gobiernan el acceso (AS-01 a AS-09). Son iniciales y se pueden ajustar por configuracion sin tocar
 * el codigo. El tope absoluto de la sesion nunca puede ser menor que su inactividad.
 *
 * @param codigoVigencia          vida del codigo de acceso
 * @param intentosMaximos         intentos fallidos antes de agotar un codigo
 * @param solicitudesPorCorreo    solicitudes por correo (o por identificador escrito) dentro de la ventana
 * @param solicitudesPorIp        codigos por IP dentro de la ventana
 * @param ventanaDeSolicitudes    ventana deslizante de los topes de solicitud
 * @param inactividad             inactividad que vence una sesion
 * @param topeAbsoluto            vida maxima de una sesion
 * @param sesionesMaximas         sesiones activas por cuenta
 * @param preregistroVigencia     vida del pre-registro y de su codigo (D-84)
 */
public record PoliticaDeAcceso(Duration codigoVigencia, int intentosMaximos, int solicitudesPorCorreo,
                               int solicitudesPorIp, Duration ventanaDeSolicitudes, Duration inactividad,
                               Duration topeAbsoluto, int sesionesMaximas, Duration preregistroVigencia) {

    /** Cotas máximas: un error de configuración no puede dejar el acceso más débil de lo que el diseño tolera. */
    private static final Duration MAXIMO_VIGENCIA_DEL_CODIGO = Duration.ofMinutes(15);
    private static final Duration MAXIMO_VIGENCIA_DEL_PREREGISTRO = Duration.ofMinutes(30);
    static final Duration VIGENCIA_DEL_PREREGISTRO = Duration.ofMinutes(15);
    private static final Duration MAXIMO_VENTANA = Duration.ofHours(24);
    private static final Duration MAXIMA_INACTIVIDAD = Duration.ofDays(30);
    private static final Duration MAXIMO_TOPE_ABSOLUTO = Duration.ofDays(90);
    private static final int MAXIMO_DE_INTENTOS = 10;
    private static final int MAXIMO_POR_CORREO = 10;
    private static final int MAXIMO_POR_IP = 10_000;
    private static final int MAXIMO_DE_SESIONES = 20;

    public PoliticaDeAcceso {
        acotada(codigoVigencia, MAXIMO_VIGENCIA_DEL_CODIGO, "codigoVigencia");
        acotada(preregistroVigencia, MAXIMO_VIGENCIA_DEL_PREREGISTRO, "preregistroVigencia");
        acotada(ventanaDeSolicitudes, MAXIMO_VENTANA, "ventanaDeSolicitudes");
        acotada(inactividad, MAXIMA_INACTIVIDAD, "inactividad");
        acotada(topeAbsoluto, MAXIMO_TOPE_ABSOLUTO, "topeAbsoluto");
        acotado(intentosMaximos, MAXIMO_DE_INTENTOS, "intentosMaximos");
        acotado(solicitudesPorCorreo, MAXIMO_POR_CORREO, "solicitudesPorCorreo");
        acotado(solicitudesPorIp, MAXIMO_POR_IP, "solicitudesPorIp");
        acotado(sesionesMaximas, MAXIMO_DE_SESIONES, "sesionesMaximas");
        if (topeAbsoluto.compareTo(inactividad) < 0) {
            throw new IllegalArgumentException("El tope absoluto no puede ser menor que la inactividad");
        }
    }

    /** Con el pre-registro en su vigencia por omision (15 minutos): lo que tenia la politica antes de D-84. */
    public PoliticaDeAcceso(Duration codigoVigencia, int intentosMaximos, int solicitudesPorCorreo, int solicitudesPorIp,
                            Duration ventanaDeSolicitudes, Duration inactividad, Duration topeAbsoluto,
                            int sesionesMaximas) {
        this(codigoVigencia, intentosMaximos, solicitudesPorCorreo, solicitudesPorIp, ventanaDeSolicitudes, inactividad,
                topeAbsoluto, sesionesMaximas, VIGENCIA_DEL_PREREGISTRO);
    }

    public static PoliticaDeAcceso porDefecto() {
        return new PoliticaDeAcceso(Duration.ofMinutes(5), 5, 3, 20, Duration.ofMinutes(15), Duration.ofHours(8),
                Duration.ofDays(30), 5);
    }

    private static void acotada(Duration duracion, Duration maximo, String nombre) {
        Objects.requireNonNull(duracion, nombre);
        if (duracion.isZero() || duracion.isNegative()) {
            throw new IllegalArgumentException(nombre + " debe ser positiva");
        }
        if (duracion.compareTo(maximo) > 0) {
            throw new IllegalArgumentException(nombre + " no puede pasar de " + maximo);
        }
    }

    private static void acotado(int valor, int maximo, String nombre) {
        if (valor <= 0) {
            throw new IllegalArgumentException(nombre + " debe ser positivo");
        }
        if (valor > maximo) {
            throw new IllegalArgumentException(nombre + " no puede pasar de " + maximo);
        }
    }
}
