package mx.negocio360.inventario.dominio;

import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** C-01 a C-04 y 4.3: costo promedio ponderado del saldo, con cifras calculadas a mano. */
class SaldoTest {

    private static final UUID ARTICULO = UUID.randomUUID();
    private static final UUID NEGOCIO = UUID.randomUUID();

    private static Saldo saldo(String cantidad, String cpp) {
        return new Saldo(ARTICULO, NEGOCIO, d(cantidad), d(cpp), null, 3L);
    }

    @Test
    @DisplayName("un saldo nuevo empieza en cero, con costo cero y version 1")
    void inicial_articuloNuevo_ceroYVersionUno() {
        var inicial = Saldo.inicial(ARTICULO, NEGOCIO);

        assertThat(inicial.cantidadBase()).isEqualTo(d("0.0000"));
        assertThat(inicial.cpp()).isEqualTo(d("0.0000"));
        assertThat(inicial.version()).isEqualTo(1L);
        assertThat(inicial.valor()).isEqualTo(d("0.00"));
    }

    @Test
    @DisplayName("C-02: entrada con saldo positivo promedia (2000 g a 0.018 + 1000 g a 0.021 = 0.019)")
    void entrada_saldoPositivo_promedia() {
        var movimiento = UUID.randomUUID();

        var nuevo = saldo("2000.0000", "0.0180").conEntrada(d("1000.0000"), d("0.0210"), movimiento);

        assertThat(nuevo.cantidadBase()).isEqualTo(d("3000.0000"));
        assertThat(nuevo.cpp()).isEqualTo(d("0.0190"));
        assertThat(nuevo.ultimoMovimientoId()).isEqualTo(movimiento);
        assertThat(nuevo.version()).isEqualTo(4L);
    }

    @Test
    @DisplayName("C-02 y 4.3: media diezmilesima del CPP redondea hacia arriba (4.0002 / 4 = 1.00005)")
    void entrada_mediaDiezmilesima_redondeaHaciaArriba() {
        var nuevo = saldo("3.0000", "1.0000").conEntrada(d("1.0000"), d("1.0002"), UUID.randomUUID());

        assertThat(nuevo.cpp()).isEqualTo(d("1.0001"));
    }

    @Test
    @DisplayName("C-03: entrada con saldo cero toma el costo de la entrada")
    void entrada_saldoCero_tomaCostoDeEntrada() {
        var nuevo = saldo("0.0000", "0.0500").conEntrada(d("10.0000"), d("0.0300"), UUID.randomUUID());

        assertThat(nuevo.cpp()).isEqualTo(d("0.0300"));
        assertThat(nuevo.cantidadBase()).isEqualTo(d("10.0000"));
    }

    @Test
    @DisplayName("C-03: entrada con saldo negativo toma el costo de la entrada, aunque el saldo siga negativo")
    void entrada_saldoNegativo_tomaCostoDeEntrada() {
        var nuevo = saldo("-500.0000", "0.0190").conEntrada(d("100.0000"), d("0.0300"), UUID.randomUUID());

        assertThat(nuevo.cpp()).isEqualTo(d("0.0300"));
        assertThat(nuevo.cantidadBase()).isEqualTo(d("-400.0000"));
    }

    @Test
    @DisplayName("C-04 y A-01: la salida no cambia el CPP y puede dejar el saldo en negativo")
    void salida_cualquierSaldo_noCambiaCppYPermiteNegativo() {
        var nuevo = saldo("2500.0000", "0.0190").conSalida(d("3000.0000"), UUID.randomUUID());

        assertThat(nuevo.cantidadBase()).isEqualTo(d("-500.0000"));
        assertThat(nuevo.cpp()).isEqualTo(d("0.0190"));
    }

    @Test
    @DisplayName("C-01: el valor del inventario es cantidad por CPP, redondeado a centavos")
    void valor_cantidadPorCpp_aCentavos() {
        assertThat(saldo("3.0000", "0.1250").valor()).isEqualTo(d("0.38"));
        assertThat(saldo("-3.0000", "0.1250").valor()).isEqualTo(d("-0.38"));
    }
}
