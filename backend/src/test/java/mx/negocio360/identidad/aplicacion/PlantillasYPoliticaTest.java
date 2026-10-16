package mx.negocio360.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import mx.negocio360.identidad.dominio.Cuenta;
import mx.negocio360.identidad.dominio.CodigoError;
import mx.negocio360.identidad.dominio.EstadoCuenta;
import mx.negocio360.identidad.dominio.IdentidadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Textos fijos de error (SI-14), politica de acceso con sus valores iniciales y la cuenta como valor inmutable. */
class PlantillasYPoliticaTest {

    @ParameterizedTest
    @EnumSource(CodigoError.class)
    @DisplayName("SI-14: todo codigo de error tiene un texto fijo en español, sin datos ni el nombre tecnico")
    void texto_cadaCodigo_esUnaFraseFijaSinDatos(CodigoError codigo) {
        var texto = PlantillasIdentidad.texto(codigo);

        assertThat(texto).isNotBlank().endsWith(".").doesNotContain(codigo.name()).doesNotContain("{")
                .doesNotContain("%").doesNotContainPattern("\\d{4,}");
    }

    @Test
    @DisplayName("los errores de autenticacion no dicen cual de las causas ocurrio")
    void texto_erroresDeAutenticacion_noDistinguenCausas() {
        assertThat(PlantillasIdentidad.texto(CodigoError.NO_AUTENTICADO)).doesNotContainIgnoringCase("vencid")
                .doesNotContainIgnoringCase("revoc").doesNotContainIgnoringCase("desconoc");
    }

    @Test
    @DisplayName("la excepcion lleva solo el codigo como mensaje y rechaza un codigo nulo")
    void excepcion_mensajeEsElCodigo_yNoAceptaNulo() {
        var excepcion = new IdentidadException(CodigoError.TASA_EXCEDIDA);

        assertThat(excepcion.codigo()).isEqualTo(CodigoError.TASA_EXCEDIDA);
        assertThat(excepcion.getMessage()).isEqualTo("TASA_EXCEDIDA");
        assertThatThrownBy(() -> new IdentidadException(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("la politica inicial es la del diseño: codigo 5 min y 5 intentos, 3 y 20 solicitudes, sesion 8 h y 30 d, 5 sesiones")
    void politica_porDefecto_coincideConElDiseno() {
        var p = PoliticaDeAcceso.porDefecto();

        assertThat(p.codigoVigencia()).isEqualTo(Duration.ofMinutes(5));
        assertThat(p.intentosMaximos()).isEqualTo(5);
        assertThat(p.solicitudesPorCorreo()).isEqualTo(3);
        assertThat(p.solicitudesPorIp()).isEqualTo(20);
        assertThat(p.ventanaDeSolicitudes()).isEqualTo(Duration.ofMinutes(15));
        assertThat(p.inactividad()).isEqualTo(Duration.ofHours(8));
        assertThat(p.topeAbsoluto()).isEqualTo(Duration.ofDays(30));
        assertThat(p.sesionesMaximas()).isEqualTo(5);
    }

    @ParameterizedTest
    @MethodSource("politicasInvalidas")
    @DisplayName("una politica con valores en cero, negativos o un tope absoluto menor que la inactividad se rechaza")
    void politica_valoresInvalidos_seRechaza(Runnable construir) {
        assertThatThrownBy(construir::run).isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Runnable> politicasInvalidas() {
        var m = Duration.ofMinutes(1);
        return Stream.of(
                () -> new PoliticaDeAcceso(Duration.ZERO, 5, 3, 20, m, m, m, 5),
                () -> new PoliticaDeAcceso(m, 0, 3, 20, m, m, m, 5),
                () -> new PoliticaDeAcceso(m, 5, 0, 20, m, m, m, 5),
                () -> new PoliticaDeAcceso(m, 5, 3, -1, m, m, m, 5),
                () -> new PoliticaDeAcceso(m, 5, 3, 20, Duration.ZERO, m, m, 5),
                () -> new PoliticaDeAcceso(m, 5, 3, 20, m, Duration.ofDays(2), Duration.ofDays(1), 5),
                () -> new PoliticaDeAcceso(m, 5, 3, 20, m, m, m, 0));
    }

    @Test
    @DisplayName("la cuenta es inmutable: cada cambio devuelve otra cuenta")
    void cuenta_cambios_devuelvenOtraCuenta() {
        var ahora = Instant.parse("2026-10-14T16:00:00Z");
        var original = Cuenta.nueva(UUID.randomUUID(), "Ana", "López Pérez", "+522221110001", "ana@example.com", 1, ahora);

        var renombrada = original.conNombre("Ana Nueva");
        var suspendida = original.conEstado(EstadoCuenta.SUSPENDIDA);
        var conAcceso = original.conUltimoAcceso(ahora.plusSeconds(60));

        assertThat(original.nombre()).isEqualTo("Ana");
        assertThat(original.estado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(original.ultimoAccesoEn()).isEqualTo(ahora);
        assertThat(renombrada.nombre()).isEqualTo("Ana Nueva");
        assertThat(suspendida.estado()).isEqualTo(EstadoCuenta.SUSPENDIDA);
        assertThat(conAcceso.ultimoAccesoEn()).isEqualTo(ahora.plusSeconds(60));
    }

    @Test
    @DisplayName("toString de la cuenta no revela nombre ni telefono")
    void cuenta_toString_noRevelaDatosPersonales() {
        var cuenta = Cuenta.nueva(UUID.randomUUID(), "Ana", "López Pérez", "+522221110001", "ana@example.com", 1, Instant.now());

        assertThat(cuenta.toString()).doesNotContain("Ana").doesNotContain("2221110001");
    }
}
