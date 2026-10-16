package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AM-01 y AM-04: una membresia es cuenta, negocio, rol y estado; el estado solo avanza por transiciones validas. */
class MembresiaTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T16:00:00Z");
    private static final UUID CUENTA = UuidV7.generar(AHORA);
    private static final UUID NEGOCIO = UuidV7.generar(AHORA);

    private static Membresia nueva() {
        return Membresia.nueva(UuidV7.generar(AHORA), CUENTA, NEGOCIO, "CAJERO", null, AHORA);
    }

    @Test
    @DisplayName("una membresia nueva esta ACTIVA, en la version 0 y sin fecha de baja")
    void nueva_estaActiva() {
        var membresia = nueva();

        assertThat(membresia.estado()).isEqualTo(EstadoMembresia.ACTIVA);
        assertThat(membresia.activa()).isTrue();
        assertThat(membresia.version()).isZero();
        assertThat(membresia.revocadaEn()).isNull();
        assertThat(membresia.rolCodigo()).isEqualTo("CAJERO");
        assertThat(membresia.creadaEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("cambiar el rol deja otra membresia con el rol nuevo y una version mas")
    void conRol_cambiaElRolYSubeLaVersion() {
        var cambiada = nueva().conRol("GERENTE", AHORA.plusSeconds(60));

        assertThat(cambiada.rolCodigo()).isEqualTo("GERENTE");
        assertThat(cambiada.version()).isEqualTo(1);
        assertThat(cambiada.actualizadaEn()).isEqualTo(AHORA.plusSeconds(60));
    }

    @Test
    @DisplayName("suspender y reactivar mueven entre ACTIVA y SUSPENDIDA")
    void suspenderYReactivar() {
        var suspendida = nueva().suspendida(AHORA.plusSeconds(1));
        var reactivada = suspendida.reactivada(AHORA.plusSeconds(2));

        assertThat(suspendida.estado()).isEqualTo(EstadoMembresia.SUSPENDIDA);
        assertThat(suspendida.activa()).isFalse();
        assertThat(reactivada.estado()).isEqualTo(EstadoMembresia.ACTIVA);
        assertThat(reactivada.version()).isEqualTo(2);
    }

    @Test
    @DisplayName("revocar desde ACTIVA o SUSPENDIDA deja la fecha de baja")
    void revocar_desdeActivaOSuspendida() {
        var desdeActiva = nueva().revocada(AHORA.plusSeconds(5));
        var desdeSuspendida = nueva().suspendida(AHORA).revocada(AHORA.plusSeconds(9));

        assertThat(desdeActiva.estado()).isEqualTo(EstadoMembresia.REVOCADA);
        assertThat(desdeActiva.revocadaEn()).isEqualTo(AHORA.plusSeconds(5));
        assertThat(desdeSuspendida.estado()).isEqualTo(EstadoMembresia.REVOCADA);
    }

    @Test
    @DisplayName("una membresia revocada no vuelve: no se reactiva, suspende ni cambia de rol (se invita de nuevo)")
    void revocada_noVuelve() {
        var revocada = nueva().revocada(AHORA);

        assertThatThrownBy(() -> revocada.reactivada(AHORA)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> revocada.suspendida(AHORA)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> revocada.conRol("GERENTE", AHORA)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> revocada.revocada(AHORA)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("solo se reactiva lo que esta suspendido y solo se suspende lo que esta activo")
    void transicionesInvalidas() {
        assertThatThrownBy(() -> nueva().reactivada(AHORA)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> nueva().suspendida(AHORA).suspendida(AHORA)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("el rol de una membresia no puede ser nulo ni estar vacio")
    void nueva_sinRol_seRechaza() {
        assertThatThrownBy(() -> Membresia.nueva(UuidV7.generar(AHORA), CUENTA, NEGOCIO, " ", null, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Membresia.nueva(UuidV7.generar(AHORA), CUENTA, NEGOCIO, null, null, AHORA))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("el dueño inicial no tiene quien lo agrego")
    void dueno_noTieneAgregadoPor() {
        var dueno = Membresia.nueva(UuidV7.generar(AHORA), CUENTA, NEGOCIO, "DUENO", null, AHORA);
        var agregado = Membresia.nueva(UuidV7.generar(AHORA), CUENTA, NEGOCIO, "CAJERO", UuidV7.generar(AHORA), AHORA);

        assertThat(dueno.agregadaPor()).isNull();
        assertThat(agregado.agregadaPor()).isNotNull();
    }
}
