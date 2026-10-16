package mx.negocio360.compartido.seguridad;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de la puerta HTTP de /graphql (SG-09 y SG-14), bajo {@code seguridad.http}. Falla cerrada: un valor
 * mal escrito impide el arranque en lugar de abrir de mas.
 *
 * @param cuerpoMaximoBytes  tope del cuerpo de una peticion; por omision 64 KB, como mucho 1 MiB
 * @param origenesPermitidos origenes de navegador que pueden llamar a la API (por ejemplo el del panel web). Sin
 *                           ninguno, toda peticion que traiga {@code Origin} se rechaza; las apps nativas no lo envian.
 *                           Sin comodines ni rutas: {@code https://panel.ejemplo.test}
 */
@ConfigurationProperties(prefix = "seguridad.http")
public record PropiedadesHttp(Integer cuerpoMaximoBytes, List<String> origenesPermitidos) {

    public static final int CUERPO_POR_OMISION = 64 * 1024;
    static final int CUERPO_MAXIMO_ADMITIDO = 1024 * 1024;
    private static final Pattern ORIGEN = Pattern.compile("https?://[a-z0-9]([a-z0-9.-]*[a-z0-9])?(:[0-9]{1,5})?");

    public PropiedadesHttp {
        cuerpoMaximoBytes = cuerpoMaximoBytes == null ? CUERPO_POR_OMISION : cuerpoMaximoBytes;
        if (cuerpoMaximoBytes < 1 || cuerpoMaximoBytes > CUERPO_MAXIMO_ADMITIDO) {
            throw new IllegalArgumentException("seguridad.http.cuerpo-maximo-bytes debe estar entre 1 y "
                    + CUERPO_MAXIMO_ADMITIDO);
        }
        origenesPermitidos = origenesPermitidos == null ? List.of()
                : origenesPermitidos.stream().map(PropiedadesHttp::normalizar).toList();
    }

    /** Minusculas, sin espacios ni diagonal final. Lo que no sea esquema, servidor y puerto opcional se rechaza. */
    static String normalizar(String origen) {
        var limpio = origen == null ? "" : origen.strip().toLowerCase(Locale.ROOT);
        if (limpio.endsWith("/")) {
            limpio = limpio.substring(0, limpio.length() - 1);
        }
        if (!ORIGEN.matcher(limpio).matches()) {
            throw new IllegalArgumentException("Origen no valido en seguridad.http.origenes-permitidos: sin comodines "
                    + "ni rutas, con la forma https://servidor[:puerto]");
        }
        return limpio;
    }
}
