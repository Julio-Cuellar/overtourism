package mx.negocio360.negocios.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HO-04: «abierto ahora» se calcula en el servidor, con la zona del negocio y la excepcion del dia. */
class AbiertoAhoraTest {

    private static final ZoneId CDMX = ZoneId.of("America/Mexico_City");
    private static final ZoneId TIJUANA = ZoneId.of("America/Tijuana");

    private static DiaHorario dia(int dia, String... tramos) {
        var lista = new java.util.ArrayList<Tramo>();
        for (var i = 0; i < tramos.length; i += 2) {
            lista.add(Tramo.de(tramos[i], tramos[i + 1]));
        }
        return new DiaHorario(dia, lista);
    }

    /** Jueves 15 de octubre de 2026. El horario de Mexico no tiene horario de verano desde 2022. */
    private static Instant jueves(int hora, int minuto, ZoneId zona) {
        return LocalDate.of(2026, 10, 15).atTime(hora, minuto).atZone(zona).toInstant();
    }

    private static HorarioSucursal horario(List<DiaHorario> dias, List<ExcepcionHorario> excepciones) {
        return new HorarioSucursal(new SemanaHorario(dias), excepciones);
    }

    @Test
    @DisplayName("dentro del tramo esta abierto; antes de abrir y a la hora de cerrar, cerrado")
    void dentroYFueraDelTramo() {
        var h = horario(List.of(dia(4, "08:00", "17:00")), List.of());

        assertThat(h.abiertoEn(jueves(10, 30, CDMX), CDMX)).isTrue();
        assertThat(h.abiertoEn(jueves(8, 0, CDMX), CDMX)).isTrue();
        assertThat(h.abiertoEn(jueves(7, 59, CDMX), CDMX)).isFalse();
        assertThat(h.abiertoEn(jueves(17, 0, CDMX), CDMX)).isFalse();
    }

    @Test
    @DisplayName("el mismo instante da distinto resultado en dos zonas horarias")
    void dosZonas_dosResultados() {
        var h = horario(List.of(dia(4, "08:00", "17:00")), List.of());
        var instante = jueves(10, 30, CDMX); // 09:30 en Tijuana (una hora menos en octubre)

        assertThat(h.abiertoEn(instante, CDMX)).isTrue();
        assertThat(h.abiertoEn(instante, TIJUANA)).isTrue();
        var tempranoEnTijuana = jueves(7, 30, TIJUANA); // 08:30 en la Ciudad de Mexico
        assertThat(h.abiertoEn(tempranoEnTijuana, TIJUANA)).isFalse();
        assertThat(h.abiertoEn(tempranoEnTijuana, CDMX)).isTrue();
    }

    @Test
    @DisplayName("un dia sin tramos esta cerrado")
    void diaSinTramos_cerrado() {
        assertThat(horario(List.of(dia(1, "08:00", "17:00")), List.of()).abiertoEn(jueves(10, 0, CDMX), CDMX)).isFalse();
    }

    @Test
    @DisplayName("un tramo que cruza la medianoche sigue abierto de madrugada, segun el dia que lo abrio")
    void cruzaLaMedianoche() {
        var h = horario(List.of(dia(4, "20:00", "02:00")), List.of());
        var viernesMadrugada = LocalDate.of(2026, 10, 16).atTime(1, 0).atZone(CDMX).toInstant();
        var viernesTresAm = LocalDate.of(2026, 10, 16).atTime(3, 0).atZone(CDMX).toInstant();

        assertThat(h.abiertoEn(jueves(21, 0, CDMX), CDMX)).isTrue();
        assertThat(h.abiertoEn(viernesMadrugada, CDMX)).isTrue();
        assertThat(h.abiertoEn(viernesTresAm, CDMX)).isFalse();
        assertThat(h.abiertoEn(jueves(1, 0, CDMX), CDMX)).isFalse();
    }

    @Test
    @DisplayName("una excepcion cerrada manda sobre el horario de la semana, tambien sobre el tramo de ayer")
    void excepcionCerrada_mandaSobreLaSemana() {
        var cierre = ExcepcionHorario.cerrada(UUID.randomUUID(), LocalDate.of(2026, 10, 15), "Dia festivo");
        var h = horario(List.of(dia(4, "08:00", "17:00"), dia(3, "20:00", "02:00")), List.of(cierre));

        assertThat(h.abiertoEn(jueves(10, 0, CDMX), CDMX)).isFalse();
        assertThat(h.abiertoEn(jueves(1, 0, CDMX), CDMX)).isFalse();
    }

    @Test
    @DisplayName("una excepcion abierta reemplaza el horario de ese dia")
    void excepcionAbierta_reemplazaElDia() {
        var especial = ExcepcionHorario.abierta(UUID.randomUUID(), LocalDate.of(2026, 10, 15), "Feria",
                Tramo.de("12:00", "15:00"));
        var h = horario(List.of(dia(4, "08:00", "17:00")), List.of(especial));

        assertThat(h.abiertoEn(jueves(9, 0, CDMX), CDMX)).isFalse();
        assertThat(h.abiertoEn(jueves(13, 0, CDMX), CDMX)).isTrue();
    }

    @Test
    @DisplayName("una excepcion de otra fecha no cambia nada")
    void excepcionDeOtraFecha_noCambiaNada() {
        var otra = ExcepcionHorario.cerrada(UUID.randomUUID(), LocalDate.of(2026, 11, 2), "Dia de Muertos");
        var h = horario(List.of(dia(4, "08:00", "17:00")), List.of(otra));

        assertThat(h.abiertoEn(jueves(10, 0, CDMX), CDMX)).isTrue();
    }

    @Test
    @DisplayName("HO-03: una excepcion cerrada no lleva horas y una abierta las exige")
    void excepcion_reglasDeHoras() {
        var e = org.assertj.core.api.Assertions.catchThrowableOfType(NegociosException.class,
                () -> ExcepcionHorario.cerrada(UUID.randomUUID(), LocalDate.of(2026, 11, 2), "x".repeat(81)));
        assertThat(e).isNotNull();
        assertThat(e.codigo()).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(ExcepcionHorario.cerrada(UUID.randomUUID(), LocalDate.of(2026, 11, 2), null).abre()).isNull();
        assertThat(ExcepcionHorario.abierta(UUID.randomUUID(), LocalDate.of(2026, 11, 2), null,
                Tramo.de("09:00", "10:00")).cerrado()).isFalse();
    }
}
