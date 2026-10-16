package mx.negocio360.inventario.dominio;

import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** A-02: las alertas se disparan al cruzar el umbral, no en cada movimiento. */
class CruceDeUmbralTest {

    @ParameterizedTest(name = "{0} -> {1} con minimo {2}: {3}")
    @CsvSource(delimiter = ';', value = {
        "2000; 1500; 1000; ",
        "1500; 900;  1000; BAJO_MINIMO",
        "1500; 1000; 1000; BAJO_MINIMO",
        "900;  800;  1000; ",
        "800;  -100; 1000; SALDO_NEGATIVO",
        "1500; -100; 1000; BAJO_MINIMO,SALDO_NEGATIVO",
        "-100; -50;  1000; ",
        "-100; 1900; 1000; REPUESTO",
        "0;    2000; 1000; REPUESTO",
        "1000; 1001; 1000; REPUESTO",
        "0;    0;    0;    ",
        "5;    0;    0;    BAJO_MINIMO",
        "0;    -1;   0;    SALDO_NEGATIVO"
    })
    @DisplayName("A-02: bajo_minimo al pasar a <= minimo, repuesto al volver arriba, saldo_negativo al pasar a < 0")
    void entre_saldos_alertasDelCruce(String antes, String despues, String minimo, String esperadas) {
        var alertas = CruceDeUmbral.entre(d(antes), d(despues), d(minimo));

        var nombres = esperadas == null ? List.<TipoAlerta>of()
                : List.of(esperadas.split(",")).stream().map(TipoAlerta::valueOf).toList();
        assertThat(alertas).containsExactlyElementsOf(nombres);
    }
}
