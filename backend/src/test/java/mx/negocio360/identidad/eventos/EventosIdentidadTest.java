package mx.negocio360.identidad.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
import mx.negocio360.identidad.dominio.EstadoMembresia;
import mx.negocio360.identidad.dominio.Giro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** R5 y AP-02: los eventos de Identidad son contratos versionados con ids y valores, nunca nombre ni telefono. */
class EventosIdentidadTest {

    private static final UUID CUENTA = UUID.fromString("0192f39f-3333-7000-8000-00000000f006");

    @Test
    @DisplayName("negocio.registro_solicitado v1 lleva la cuenta que lo pide, el nombre del negocio y su giro")
    void negocioRegistroSolicitado_contrato() {
        var evento = new NegocioRegistroSolicitado(CUENTA, "Fonda Doña Chela", Giro.RESTAURANTE);

        assertThat(NegocioRegistroSolicitado.TIPO).isEqualTo("negocio.registro_solicitado");
        assertThat(NegocioRegistroSolicitado.VERSION).isEqualTo(1);
        assertThat(evento.datos()).isEqualTo(Map.of("cuenta_id", CUENTA.toString(), "nombre", "Fonda Doña Chela",
                "giro", "RESTAURANTE"));
    }

    @Test
    @DisplayName("identidad.membresia_agregada v1 lleva la cuenta, el rol y de donde vino")
    void membresiaAgregada_contrato() {
        var evento = new MembresiaAgregada(CUENTA, "DUENO", MembresiaAgregada.Origen.DUENO_INICIAL);

        assertThat(MembresiaAgregada.TIPO).isEqualTo("identidad.membresia_agregada");
        assertThat(MembresiaAgregada.VERSION).isEqualTo(1);
        assertThat(evento.datos()).isEqualTo(Map.of("cuenta_id", CUENTA.toString(), "rol", "DUENO",
                "origen", "DUENO_INICIAL"));
    }

    @Test
    @DisplayName("identidad.membresia_cambiada v1 lleva el rol anterior, el nuevo y el estado")
    void membresiaCambiada_contrato() {
        var evento = new MembresiaCambiada(CUENTA, "CAJERO", "GERENTE", EstadoMembresia.ACTIVA);

        assertThat(MembresiaCambiada.TIPO).isEqualTo("identidad.membresia_cambiada");
        assertThat(MembresiaCambiada.VERSION).isEqualTo(1);
        assertThat(evento.datos()).isEqualTo(Map.of("cuenta_id", CUENTA.toString(), "rol_anterior", "CAJERO",
                "rol_nuevo", "GERENTE", "estado", "ACTIVA"));
    }

    @Test
    @DisplayName("identidad.membresia_revocada v1 lleva la cuenta y el motivo")
    void membresiaRevocada_contrato() {
        var evento = new MembresiaRevocada(CUENTA, MembresiaRevocada.Motivo.SALIO);

        assertThat(MembresiaRevocada.TIPO).isEqualTo("identidad.membresia_revocada");
        assertThat(MembresiaRevocada.VERSION).isEqualTo(1);
        assertThat(evento.datos()).isEqualTo(Map.of("cuenta_id", CUENTA.toString(), "motivo", "SALIO"));
    }

    @Test
    @DisplayName("ningun evento acepta datos nulos")
    void eventos_rechazanNulos() {
        assertThatThrownBy(() -> new NegocioRegistroSolicitado(null, "x", Giro.MUSEO)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MembresiaAgregada(CUENTA, null, MembresiaAgregada.Origen.INVITACION))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MembresiaCambiada(CUENTA, "A", "B", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MembresiaRevocada(CUENTA, null)).isInstanceOf(NullPointerException.class);
    }
}
