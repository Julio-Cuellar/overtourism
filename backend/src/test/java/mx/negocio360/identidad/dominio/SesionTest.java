package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** IDN-1.5 · AS-05, AS-07: vigencia deslizante, tope absoluto y revocacion de una sesion. */
class SesionTest {

    private static final Instant AHORA = Instant.parse("2026-10-14T16:00:00Z");
    private static final Duration INACTIVIDAD = Duration.ofHours(8);
    private static final Duration TOPE = Duration.ofDays(30);
    private static final String HASH = "a".repeat(64);

    private Sesion nueva() {
        return Sesion.abrir(HASH, UUID.randomUUID(), "Chrome en Windows", "189.203.10.0", AHORA, INACTIVIDAD, TOPE);
    }

    @Test
    @DisplayName("abrir fija 8 horas de inactividad, 30 dias de tope y ningun negocio activo")
    void abrir_sesionNueva_fijaVigenciasYSinNegocio() {
        var sesion = nueva();

        assertThat(sesion.expiraEn()).isEqualTo(AHORA.plus(INACTIVIDAD));
        assertThat(sesion.expiraAbsolutaEn()).isEqualTo(AHORA.plus(TOPE));
        assertThat(sesion.creadaEn()).isEqualTo(AHORA);
        assertThat(sesion.ultimoUsoEn()).isEqualTo(AHORA);
        assertThat(sesion.negocioId()).isNull();
        assertThat(sesion.revocadaEn()).isNull();
    }

    @Test
    @DisplayName("vigente hasta justo antes de cumplirse la inactividad")
    void vigente_antesDeLaInactividad_esVerdadero() {
        assertThat(nueva().vigente(AHORA.plus(INACTIVIDAD).minusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("deja de ser vigente exactamente al cumplirse la inactividad")
    void vigente_alCumplirseLaInactividad_esFalso() {
        assertThat(nueva().vigente(AHORA.plus(INACTIVIDAD))).isFalse();
    }

    @Test
    @DisplayName("renovada corre la inactividad desde el momento de uso")
    void renovada_despuesDeUnRato_correLaInactividad() {
        var uso = AHORA.plus(Duration.ofHours(5));

        var renovada = nueva().renovada(uso, INACTIVIDAD);

        assertThat(renovada.ultimoUsoEn()).isEqualTo(uso);
        assertThat(renovada.expiraEn()).isEqualTo(uso.plus(INACTIVIDAD));
    }

    @Test
    @DisplayName("renovada nunca pasa del tope absoluto de 30 dias")
    void renovada_cercaDelTope_noPasaDelTopeAbsoluto() {
        var uso = AHORA.plus(TOPE).minus(Duration.ofHours(2));

        var renovada = nueva().renovada(uso, INACTIVIDAD);

        assertThat(renovada.expiraEn()).isEqualTo(AHORA.plus(TOPE));
        assertThat(renovada.vigente(AHORA.plus(TOPE))).isFalse();
    }

    @Test
    @DisplayName("revocada no es vigente y no modifica la sesion original")
    void revocada_sesion_dejaDeSerVigenteSinMutarLaOriginal() {
        var original = nueva();

        var revocada = original.revocada(AHORA.plusSeconds(10));

        assertThat(revocada.vigente(AHORA.plusSeconds(11))).isFalse();
        assertThat(revocada.revocadaEn()).isEqualTo(AHORA.plusSeconds(10));
        assertThat(original.revocadaEn()).isNull();
        assertThat(original.vigente(AHORA.plusSeconds(11))).isTrue();
    }

    @Test
    @DisplayName("revocar una sesion ya revocada conserva la primera fecha")
    void revocada_dosVeces_conservaLaPrimeraFecha() {
        var primera = nueva().revocada(AHORA.plusSeconds(10));

        var segunda = primera.revocada(AHORA.plusSeconds(99));

        assertThat(segunda.revocadaEn()).isEqualTo(AHORA.plusSeconds(10));
    }

    @Test
    @DisplayName("toString no revela el hash del token")
    void toString_nuncaRevelaElHashDelToken() {
        assertThat(nueva().toString()).doesNotContain(HASH);
    }

    @Test
    @DisplayName("conNegocio fija el negocio activo y devuelve otra sesion")
    void conNegocio_negocioActivo_devuelveOtraSesion() {
        var negocio = UUID.randomUUID();
        var original = nueva();

        var conNegocio = original.conNegocio(negocio);

        assertThat(conNegocio.negocioId()).isEqualTo(negocio);
        assertThat(original.negocioId()).isNull();
    }
}
