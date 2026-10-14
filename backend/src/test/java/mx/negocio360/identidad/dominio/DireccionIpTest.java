package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** AP-03 y SI-07: la IP se guarda truncada y la clave del tope agrupa lo que una sola persona puede cambiar a voluntad. */
class DireccionIpTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "189.203.10.57|189.203.10.0",
        "2001:db8:85a3:8d3:1319:8a2e:370:7348|2001:db8:85a3:8d3::",
        "2001:db8::1|2001:db8:0:0::",
        "::1|0:0:0:0::",
        "::ffff:189.203.10.57|189.203.10.0"})
    @DisplayName("la IP se trunca: IPv4 sin el ultimo octeto, IPv6 a su prefijo /64, y la IPv4 mapeada cuenta como IPv4")
    void truncada_ipValida_seTruncaSinImportarLaForma(String ip, String esperada) {
        assertThat(DireccionIp.truncada(ip)).contains(esperada);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"no-es-una-ip", "999.999.999.999", "256.1.1.1", "1.2.3", "1.2.3.4.5", "evil.example.com",
        "fe80::1%eth0", "2001:db8:::1", "gggg::1", "189.203.10.57 "})
    @DisplayName("lo que no es una IP literal no se guarda ni se resuelve")
    void truncada_noEsIp_noSeGuarda(String ip) {
        assertThat(DireccionIp.truncada(ip)).isEmpty();
    }

    @Test
    @DisplayName("un nombre de host nunca dispara una consulta de DNS")
    void truncada_nombreDeHost_respondeAlMomento() {
        var inicio = System.nanoTime();

        DireccionIp.truncada("host-que-no-existe.invalid");

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(500));
    }

    @Test
    @DisplayName("SI-07: la clave del tope de una IPv4 es la IP completa, y la IPv4 mapeada coincide con la normal")
    void claveDeLimite_ipv4_esLaIpCompleta() {
        assertThat(DireccionIp.claveDeLimite("189.203.10.57")).isEqualTo("189.203.10.57");
        assertThat(DireccionIp.claveDeLimite("::ffff:189.203.10.57")).isEqualTo("189.203.10.57");
    }

    @Test
    @DisplayName("SI-07: dos direcciones del mismo /64 comparten tope y las de otro /64 no")
    void claveDeLimite_ipv6_agrupaPorPrefijo64() {
        var a = DireccionIp.claveDeLimite("2001:db8:85a3:8d3:1319:8a2e:370:7348");
        var mismoPrefijo = DireccionIp.claveDeLimite("2001:db8:85a3:8d3:ffff:ffff:ffff:ffff");
        var otroPrefijo = DireccionIp.claveDeLimite("2001:db8:85a3:8d4:1319:8a2e:370:7348");

        assertThat(mismoPrefijo).isEqualTo(a);
        assertThat(otroPrefijo).isNotEqualTo(a);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"no-es-una-ip", "999.999.999.999"})
    @DisplayName("una IP ausente o invalida se agrupa bajo una sola clave, sin fallar")
    void claveDeLimite_ipInvalida_esDesconocida(String ip) {
        assertThat(DireccionIp.claveDeLimite(ip)).isEqualTo("desconocida");
    }
}
