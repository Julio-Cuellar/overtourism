package mx.negocio360.negocios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HO-01 y HO-02: tramos, medianoche, traslapes y topes. */
class SemanaHorarioTest {

    private static DiaHorario dia(int dia, String... tramos) {
        var lista = new java.util.ArrayList<Tramo>();
        for (var i = 0; i < tramos.length; i += 2) {
            lista.add(Tramo.de(tramos[i], tramos[i + 1]));
        }
        return new DiaHorario(dia, lista);
    }

    private static CodigoError error(Runnable accion) {
        var e = org.assertj.core.api.Assertions.catchThrowableOfType(NegociosException.class, accion::run);
        assertThat(e).isNotNull();
        return e.codigo();
    }

    @Test
    @DisplayName("una semana valida: un tramo normal, dos con descanso y uno que cruza la medianoche")
    void semanaValida() {
        var semana = new SemanaHorario(List.of(dia(1, "08:00", "17:00"), dia(2, "08:00", "12:00", "14:00", "18:00"),
                dia(6, "20:00", "02:00")));

        assertThat(semana.dias()).hasSize(3);
        assertThat(semana.dia(2).tramos()).hasSize(2);
    }

    @Test
    @DisplayName("un tramo que cierra antes de abrir cruza la medianoche")
    void tramo_cruzaLaMedianoche() {
        assertThat(Tramo.de("20:00", "02:00").cruzaMedianoche()).isTrue();
        assertThat(Tramo.de("08:00", "17:00").cruzaMedianoche()).isFalse();
        assertThat(Tramo.de("18:00", "00:00").cruzaMedianoche()).isTrue();
    }

    @Test
    @DisplayName("un tramo de cero minutos o con horas mal escritas es invalido")
    void tramo_invalido() {
        assertThat(error(() -> Tramo.de("08:00", "08:00"))).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(error(() -> Tramo.de("8", "17:00"))).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(error(() -> Tramo.de("25:00", "17:00"))).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(error(() -> Tramo.de("08:00", "17:60"))).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(error(() -> Tramo.de(null, "17:00"))).isEqualTo(CodigoError.HORARIO_INVALIDO);
    }

    @Test
    @DisplayName("HO-02: hasta tres tramos por dia; el cuarto falla con TRAMOS_EXCEDIDOS")
    void maximoTresTramos() {
        assertThat(error(() -> dia(1, "06:00", "08:00", "09:00", "11:00", "12:00", "14:00", "15:00", "17:00")))
                .isEqualTo(CodigoError.TRAMOS_EXCEDIDOS);
        assertThat(dia(1, "06:00", "08:00", "09:00", "11:00", "12:00", "14:00").tramos()).hasSize(3);
    }

    @Test
    @DisplayName("el dia va de 1 (lunes) a 7 (domingo) y no se repite")
    void dia_enRango_ySinRepetir() {
        assertThat(error(() -> dia(0, "08:00", "09:00"))).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(error(() -> dia(8, "08:00", "09:00"))).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(error(() -> new SemanaHorario(List.of(dia(3, "08:00", "09:00"), dia(3, "10:00", "11:00")))))
                .isEqualTo(CodigoError.HORARIO_INVALIDO);
    }

    @Test
    @DisplayName("HO-01: dos tramos del mismo dia que se traslapan son invalidos; si se tocan, no")
    void traslapeEnElMismoDia() {
        assertThat(error(() -> dia(1, "08:00", "12:00", "11:00", "15:00"))).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(dia(1, "08:00", "12:00", "12:00", "15:00").tramos()).hasSize(2);
    }

    @Test
    @DisplayName("HO-01: el tramo que viene del dia anterior tambien cuenta (sabado 20:00-02:00 y domingo 01:00-03:00)")
    void traslapeConElDiaAnterior() {
        var dias = List.of(dia(6, "20:00", "02:00"), dia(7, "01:00", "03:00"));

        assertThat(error(() -> new SemanaHorario(dias))).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(new SemanaHorario(List.of(dia(6, "20:00", "02:00"), dia(7, "02:00", "03:00"))).dias()).hasSize(2);
    }

    @Test
    @DisplayName("HO-01: el domingo que cruza la medianoche choca con el lunes de la semana siguiente")
    void traslapeAlDarLaVuelta() {
        assertThat(error(() -> new SemanaHorario(List.of(dia(7, "22:00", "02:00"), dia(1, "01:00", "05:00")))))
                .isEqualTo(CodigoError.HORARIO_INVALIDO);
    }

    @Test
    @DisplayName("un dia sin tramos es un dia cerrado")
    void diaSinTramos_esCerrado() {
        var semana = new SemanaHorario(List.of(dia(1), dia(2, "08:00", "17:00")));

        assertThat(semana.dia(1).tramos()).isEmpty();
        assertThat(semana.dia(5).tramos()).isEmpty();
    }

    @Test
    @DisplayName("los tramos de un dia se ordenan por hora de apertura")
    void tramos_seOrdenan() {
        var d = dia(1, "14:00", "18:00", "08:00", "12:00");

        assertThat(d.tramos().get(0).abre().toString()).isEqualTo("08:00");
    }

    @Test
    @DisplayName("la lista que se devuelve no se puede modificar")
    void listas_sonInmutables() {
        var d = dia(1, "08:00", "09:00");

        assertThatThrownBy(() -> d.tramos().add(Tramo.de("10:00", "11:00")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
