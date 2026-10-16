package mx.negocio360.negocios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.negocios.DiaHorarioComando;
import mx.negocio360.negocios.ExcepcionComando;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.TramoComando;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** N2: horario semanal y excepciones (HO-01 a HO-05) contra Postgres. */
@SpringBootTest
@RecordApplicationEvents
class HorarioPanelTest {

    @Autowired
    private NegociosApi negocios;
    @Autowired
    private AltasApi altas;
    @Autowired
    private ApplicationEvents eventos;

    private static DiaHorarioComando dia(int dia, String... horas) {
        var tramos = new java.util.ArrayList<TramoComando>();
        for (var i = 0; i < horas.length; i += 2) {
            tramos.add(new TramoComando(horas[i], horas[i + 1]));
        }
        return new DiaHorarioComando(dia, tramos);
    }

    private static CodigoError codigo(Throwable error) {
        assertThat(error).isInstanceOf(NegociosException.class);
        return ((NegociosException) error).codigo();
    }

    private UUID principal(Escenario e) {
        return negocios.sucursales(e.contexto, e.dueno).get(0).id();
    }

    @Test
    @DisplayName("un negocio nuevo trae un horario vacio: ningun dia abre")
    void horarioNuevo_estaVacio() {
        var e = Escenario.nuevo(altas);

        var horario = negocios.horarioDeSucursal(e.contexto, e.dueno, principal(e));

        assertThat(horario.semana()).isEmpty();
        assertThat(horario.excepciones()).isEmpty();
        assertThat(horario.abiertoAhora()).isFalse();
        assertThat(horario.version()).isZero();
    }

    @Test
    @DisplayName("definirHorario guarda la semana, la devuelve ordenada, sube la version y publica el evento")
    void definirHorario_guardaLaSemana() {
        var e = Escenario.nuevo(altas);
        var id = principal(e);

        var horario = negocios.definirHorario(e.contexto, e.dueno, id, 0,
                List.of(dia(3, "14:00", "18:00", "08:00", "12:00"), dia(1, "08:00", "17:00"), dia(6, "20:00", "02:00")));

        assertThat(horario.version()).isEqualTo(1);
        assertThat(horario.semana()).extracting("dia").containsExactly(1, 3, 6);
        assertThat(horario.semana().get(1).tramos()).extracting("abre").containsExactly("08:00", "14:00");
        assertThat(horario.semana().get(2).tramos().get(0).cierra()).isEqualTo("02:00");
        assertThat(negocios.miNegocio(e.contexto, e.dueno).completitud().faltantes()).doesNotContain("HORARIO");
        var sobre = eventos.stream(SobreEvento.class).filter(x -> x.tipo().equals("negocio.horario_cambiado")
                && x.negocioId().equals(e.negocioId)).findFirst().orElseThrow();
        assertThat(sobre.datos()).containsEntry("sucursal_id", id.toString())
                .containsEntry("zona_horaria", "America/Mexico_City");
        assertThat((List<Object>) sobre.datos().get("semana")).hasSize(3);
    }

    @Test
    @DisplayName("HO-05: guardar reemplaza la semana completa; si un tramo es invalido no cambia nada")
    void definirHorario_reemplazaOFallaSinCambios() {
        var e = Escenario.nuevo(altas);
        var id = principal(e);
        negocios.definirHorario(e.contexto, e.dueno, id, 0, List.of(dia(1, "08:00", "17:00"), dia(2, "08:00", "17:00")));

        var error = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirHorario(e.contexto, e.dueno,
                id, 1, List.of(dia(5, "08:00", "12:00", "11:00", "15:00"))));
        var reemplazo = negocios.definirHorario(e.contexto, e.dueno, id, 1, List.of(dia(5, "09:00", "14:00")));

        assertThat(codigo(error)).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(reemplazo.semana()).extracting("dia").containsExactly(5);
        assertThat(reemplazo.version()).isEqualTo(2);
    }

    @Test
    @DisplayName("HO-01 y HO-02: traslapes con el dia anterior y mas de tres tramos fallan")
    void definirHorario_reglasDeTramos() {
        var e = Escenario.nuevo(altas);
        var id = principal(e);

        var traslape = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirHorario(e.contexto,
                e.dueno, id, 0, List.of(dia(6, "20:00", "02:00"), dia(7, "01:00", "03:00"))));
        var demasiados = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirHorario(e.contexto,
                e.dueno, id, 0, List.of(dia(1, "06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00"))));
        var malFormado = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirHorario(e.contexto,
                e.dueno, id, 0, List.of(dia(1, "8:00", "17:00"))));

        assertThat(codigo(traslape)).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(codigo(demasiados)).isEqualTo(CodigoError.TRAMOS_EXCEDIDOS);
        assertThat(codigo(malFormado)).isEqualTo(CodigoError.HORARIO_INVALIDO);
    }

    @Test
    @DisplayName("NG-10: una version vieja del horario falla con CONCURRENCIA y no pisa nada")
    void definirHorario_versionVieja() {
        var e = Escenario.nuevo(altas);
        var id = principal(e);
        negocios.definirHorario(e.contexto, e.dueno, id, 0, List.of(dia(1, "08:00", "17:00")));

        var error = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.definirHorario(e.contexto, e.dueno,
                id, 0, List.of(dia(2, "08:00", "17:00"))));

        assertThat(codigo(error)).isEqualTo(CodigoError.CONCURRENCIA);
        assertThat(negocios.horarioDeSucursal(e.contexto, e.dueno, id).semana()).extracting("dia").containsExactly(1);
    }

    @Test
    @DisplayName("el horario de la sucursal de otro negocio responde como si no existiera")
    void horario_deOtroNegocio() {
        var a = Escenario.nuevo(altas);
        var b = Escenario.nuevo(altas);

        var error = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.horarioDeSucursal(a.contexto,
                a.dueno, principal(b)));

        assertThat(codigo(error)).isEqualTo(CodigoError.SUCURSAL_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("SG-04: editar el horario pide negocios.horario.editar y verlo, negocios.perfil.ver")
    void horario_permisos() {
        var e = Escenario.nuevo(altas);
        var id = principal(e);

        assertThatThrownBy(() -> negocios.definirHorario(e.contexto, e.con("negocios.perfil.ver"), id, 0,
                List.of(dia(1, "08:00", "17:00")))).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> negocios.horarioDeSucursal(e.contexto, e.nadie, id))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    // ---- excepciones ----

    @Test
    @DisplayName("HO-03: se agrega una excepcion cerrada y otra con horario, y se quitan")
    void excepciones_seAgreganYSeQuitan() {
        var e = Escenario.nuevo(altas);
        var id = principal(e);
        var festivo = LocalDate.of(2026, 11, 2);
        var feria = LocalDate.of(2026, 12, 12);

        negocios.agregarExcepcionHorario(e.contexto, e.dueno, id, new ExcepcionComando(festivo, true, null, null,
                "Día de Muertos"));
        var horario = negocios.agregarExcepcionHorario(e.contexto, e.dueno, id,
                new ExcepcionComando(feria, false, "10:00", "22:00", "Feria"));

        assertThat(horario.excepciones()).extracting("fecha").containsExactly(festivo, feria);
        assertThat(horario.excepciones().get(0).cerrado()).isTrue();
        assertThat(horario.excepciones().get(0).abre()).isNull();
        assertThat(horario.excepciones().get(1).abre()).isEqualTo("10:00");
        var quitada = negocios.quitarExcepcionHorario(e.contexto, e.dueno, horario.excepciones().get(0).id());
        assertThat(quitada.excepciones()).extracting("fecha").containsExactly(feria);
    }

    @Test
    @DisplayName("HO-03: una sola excepcion por fecha; una cerrada no lleva horas y una abierta las exige")
    void excepciones_reglas() {
        var e = Escenario.nuevo(altas);
        var id = principal(e);
        var fecha = LocalDate.of(2026, 11, 2);
        negocios.agregarExcepcionHorario(e.contexto, e.dueno, id, new ExcepcionComando(fecha, true, null, null, null));

        var duplicada = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.agregarExcepcionHorario(
                e.contexto, e.dueno, id, new ExcepcionComando(fecha, true, null, null, null)));
        var cerradaConHoras = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.agregarExcepcionHorario(
                e.contexto, e.dueno, id, new ExcepcionComando(fecha.plusDays(1), true, "08:00", "09:00", null)));
        var abiertaSinHoras = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.agregarExcepcionHorario(
                e.contexto, e.dueno, id, new ExcepcionComando(fecha.plusDays(2), false, null, null, null)));

        assertThat(codigo(duplicada)).isEqualTo(CodigoError.EXCEPCION_DUPLICADA);
        assertThat(codigo(cerradaConHoras)).isEqualTo(CodigoError.HORARIO_INVALIDO);
        assertThat(codigo(abiertaSinHoras)).isEqualTo(CodigoError.HORARIO_INVALIDO);
    }

    @Test
    @DisplayName("quitar una excepcion inexistente o de otro negocio da EXCEPCION_NO_ENCONTRADA")
    void quitarExcepcion_ajena() {
        var a = Escenario.nuevo(altas);
        var b = Escenario.nuevo(altas);
        var deB = negocios.agregarExcepcionHorario(b.contexto, b.dueno, principal(b),
                new ExcepcionComando(LocalDate.of(2026, 11, 2), true, null, null, null)).excepciones().get(0).id();

        var ajena = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.quitarExcepcionHorario(a.contexto,
                a.dueno, deB));
        var inexistente = org.assertj.core.api.Assertions.catchThrowable(() -> negocios.quitarExcepcionHorario(
                a.contexto, a.dueno, Datos.id()));

        assertThat(codigo(ajena)).isEqualTo(CodigoError.EXCEPCION_NO_ENCONTRADA);
        assertThat(codigo(inexistente)).isEqualTo(CodigoError.EXCEPCION_NO_ENCONTRADA);
        assertThat(negocios.horarioDeSucursal(b.contexto, b.dueno, principal(b)).excepciones()).hasSize(1);
    }

    @Test
    @DisplayName("cada cambio de excepcion publica negocio.horario_cambiado con las excepciones proximas")
    void excepciones_publicanElEvento() {
        var e = Escenario.nuevo(altas);
        var fecha = java.time.LocalDate.now().plusDays(10);

        negocios.agregarExcepcionHorario(e.contexto, e.dueno, principal(e),
                new ExcepcionComando(fecha, true, null, null, "Cierre"));

        var publicados = eventos.stream(SobreEvento.class).filter(x -> x.tipo().equals("negocio.horario_cambiado")
                && x.negocioId().equals(e.negocioId)).toList();
        assertThat(publicados).hasSize(1);
        assertThat((List<Object>) publicados.get(0).datos().get("excepciones")).hasSize(1);
    }
}
