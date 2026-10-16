package mx.negocio360.compartido.tiempo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** I-08: la fecha "de hoy" es la del calendario local de Mexico, sin importar la zona del reloj inyectado. */
class ZonaMexicoTest {

    @ParameterizedTest
    @CsvSource({
            "2026-10-14T06:00:00Z,2026-10-14",
            "2026-10-14T05:59:59Z,2026-10-13",
            "2026-10-15T01:00:00Z,2026-10-14",
            "2026-10-15T05:59:59Z,2026-10-14",
            "2026-10-15T06:00:00Z,2026-10-15"})
    @DisplayName("hoy es la fecha de Mexico aunque el reloj este en UTC")
    void hoy_relojEnUtc_devuelveLaFechaDeMexico(String instante, String esperada) {
        var reloj = Clock.fixed(Instant.parse(instante), ZoneOffset.UTC);

        assertThat(ZonaMexico.hoy(reloj)).isEqualTo(LocalDate.parse(esperada));
    }

    @Test
    @DisplayName("la zona es America/Mexico_City")
    void zona_esCiudadDeMexico() {
        assertThat(ZonaMexico.ZONA.getId()).isEqualTo("America/Mexico_City");
    }
}
