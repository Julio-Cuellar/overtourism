package mx.negocio360.identidad.dominio;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lo que Identidad hace con la IP de quien llama (AP-03, SI-07): guardarla truncada y decidir con qué clave se le
 * cuentan los topes. Solo entiende IP literales: un nombre de host se descarta sin consultar el DNS. La IPv4
 * mapeada en IPv6 ({@code ::ffff:a.b.c.d}) se trata como la IPv4 que es, y una IPv6 se agrupa por su prefijo /64
 * porque quien tiene una red IPv6 puede cambiar de dirección dentro de ella sin costo.
 */
public final class DireccionIp {

    /** Clave de los topes cuando la IP falta o no es válida. */
    public static final String DESCONOCIDA = "desconocida";

    private static final Pattern IPV4 = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");
    private static final Pattern IPV6_LITERAL = Pattern.compile("[0-9a-fA-F:.]{2,45}");
    private static final int OCTETOS_IPV4 = 4;
    private static final int OCTETOS_IPV6 = 16;
    private static final int MAXIMO_OCTETO = 255;
    private static final int GRUPOS_DEL_PREFIJO = 4;

    private DireccionIp() {
    }

    /** IPv4 sin su último octeto, o IPv6 reducida a su prefijo /64. Vacío si no es una IP literal. */
    public static Optional<String> truncada(String ip) {
        return octetos(ip).map(DireccionIp::truncar);
    }

    /** Clave con la que se cuentan los topes de solicitud: la IPv4 completa o el /64 de la IPv6. */
    public static String claveDeLimite(String ip) {
        return octetos(ip).map(DireccionIp::clave).orElse(DESCONOCIDA);
    }

    private static String truncar(byte[] octetos) {
        return octetos.length == OCTETOS_IPV4 ? prefijoIpv4(octetos) + ".0" : prefijoIpv6(octetos);
    }

    private static String clave(byte[] octetos) {
        return octetos.length == OCTETOS_IPV4 ? prefijoIpv4(octetos) + "." + sinSigno(octetos[3]) : prefijoIpv6(octetos);
    }

    private static String prefijoIpv4(byte[] octetos) {
        return sinSigno(octetos[0]) + "." + sinSigno(octetos[1]) + "." + sinSigno(octetos[2]);
    }

    private static String prefijoIpv6(byte[] octetos) {
        var grupos = new StringBuilder();
        for (var i = 0; i < GRUPOS_DEL_PREFIJO; i++) {
            var grupo = sinSigno(octetos[2 * i]) << 8 | sinSigno(octetos[2 * i + 1]);
            grupos.append(Integer.toHexString(grupo)).append(i < GRUPOS_DEL_PREFIJO - 1 ? ":" : "::");
        }
        return grupos.toString();
    }

    private static int sinSigno(byte valor) {
        return valor & 0xff;
    }

    private static Optional<byte[]> octetos(String ip) {
        if (ip == null) {
            return Optional.empty();
        }
        var v4 = IPV4.matcher(ip);
        if (v4.matches()) {
            return octetosIpv4(v4);
        }
        if (ip.indexOf(':') < 0 || !IPV6_LITERAL.matcher(ip).matches()) {
            return Optional.empty();
        }
        return octetosIpv6(ip);
    }

    private static Optional<byte[]> octetosIpv4(Matcher coincidencia) {
        var octetos = new byte[OCTETOS_IPV4];
        for (var i = 0; i < OCTETOS_IPV4; i++) {
            var valor = Integer.parseInt(coincidencia.group(i + 1));
            if (valor > MAXIMO_OCTETO) {
                return Optional.empty();
            }
            octetos[i] = (byte) valor;
        }
        return Optional.of(octetos);
    }

    /** Con ':' y solo caracteres de IP, {@code getByName} lee el literal y nunca consulta el DNS. */
    private static Optional<byte[]> octetosIpv6(String ip) {
        try {
            var direccion = InetAddress.getByName(ip).getAddress();
            var esperado = direccion.length == OCTETOS_IPV4 || direccion.length == OCTETOS_IPV6;
            return esperado ? Optional.of(direccion) : Optional.empty();
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }
}
