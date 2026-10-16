package mx.negocio360.clientes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mx.negocio360.clientes.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** L-02: el aviso lleva lo que pide el art. 15; un cambio crea otra version y el contenido no se edita. */
class AvisoPrivacidadTest {

    private static final Instant AHORA = Fixtures.AHORA;
    private static final List<FinalidadAviso> FINALIDADES = List.of(new FinalidadAviso("VENTA", false));

    private static AvisoPrivacidad nuevo(String responsable, String domicilio, List<FinalidadAviso> finalidades,
                                         String simplificado, String integral, String contacto) {
        return AvisoPrivacidad.nuevo(UUID.randomUUID(), UUID.randomUUID(), 1, responsable, domicilio, finalidades,
                simplificado, integral, contacto, "locatario", AHORA);
    }

    private static AvisoPrivacidad valido() {
        return nuevo("Taller Mixcoatl", "Calle 5 de Mayo 10", FINALIDADES, "simple", "integral", "privacidad@taller.test");
    }

    @Test
    @DisplayName("un aviso nuevo queda vigente desde ahora y sin fecha de cierre")
    void nuevo_avisoCompleto_quedaVigente() {
        var aviso = valido();

        assertThat(aviso.vigente()).isTrue();
        assertThat(aviso.vigenteDesde()).isEqualTo(AHORA);
        assertThat(aviso.vigenteHasta()).isNull();
        assertThat(aviso.publicadoPor()).isEqualTo("locatario");
    }

    @Test
    @DisplayName("cerrarlo conserva el contenido y fija la fecha de cierre")
    void cerradoEn_avisoVigente_soloCambiaLaFechaDeCierre() {
        var aviso = valido();
        var cierre = AHORA.plusSeconds(3_600);

        var cerrado = aviso.cerradoEn(cierre);

        assertThat(cerrado.vigente()).isFalse();
        assertThat(cerrado.vigenteHasta()).isEqualTo(cierre);
        assertThat(cerrado).usingRecursiveComparison().ignoringFields("vigenteHasta").isEqualTo(aviso);
    }

    @Test
    @DisplayName("los textos se guardan sin espacios sobrantes")
    void nuevo_textosConEspacios_losRecorta() {
        var aviso = nuevo("  Taller  ", " Calle ", FINALIDADES, " simple ", " integral ", " contacto@taller.test ");

        assertThat(aviso.responsableNombre()).isEqualTo("Taller");
        assertThat(aviso.responsableDomicilio()).isEqualTo("Calle");
        assertThat(aviso.textoSimplificado()).isEqualTo("simple");
        assertThat(aviso.textoIntegral()).isEqualTo("integral");
        assertThat(aviso.contactoArco()).isEqualTo("contacto@taller.test");
    }

    @Test
    @DisplayName("la lista de finalidades se copia y no se puede modificar despues")
    void nuevo_listaDeFinalidades_seCopiaDeFormaInmutable() {
        var original = new ArrayList<>(FINALIDADES);
        var aviso = nuevo("Taller", "Calle", original, "simple", "integral", "contacto");

        original.add(new FinalidadAviso("OTRA", true));

        assertThat(aviso.finalidades()).hasSize(1);
        assertThatThrownBy(() -> aviso.finalidades().add(new FinalidadAviso("X", false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("la version debe ser 1 o mayor")
    void nuevo_versionCero_lanzaIllegalArgument() {
        assertThatThrownBy(() -> AvisoPrivacidad.nuevo(UUID.randomUUID(), UUID.randomUUID(), 0, "T", "C", FINALIDADES,
                "s", "i", "c", "a", AHORA)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("cualquier parte faltante o fuera de tope falla con AVISO_INCOMPLETO")
    void nuevo_contenidoIncompleto_fallaConAvisoIncompleto() {
        var casos = List.<Runnable>of(
                () -> nuevo(" ", "c", FINALIDADES, "s", "i", "k"),
                () -> nuevo("r".repeat(121), "c", FINALIDADES, "s", "i", "k"),
                () -> nuevo("r", "", FINALIDADES, "s", "i", "k"),
                () -> nuevo("r", "d".repeat(201), FINALIDADES, "s", "i", "k"),
                () -> nuevo("r", "c", List.of(), "s", "i", "k"),
                () -> nuevo("r", "c", null, "s", "i", "k"),
                () -> nuevo("r", "c", List.of(new FinalidadAviso(" ", false)), "s", "i", "k"),
                () -> nuevo("r", "c", List.of(new FinalidadAviso("f".repeat(61), false)), "s", "i", "k"),
                () -> nuevo("r", "c",
                        List.of(new FinalidadAviso("VENTA", false), new FinalidadAviso("venta", true)), "s", "i", "k"),
                () -> nuevo("r", "c", FINALIDADES, null, "i", "k"),
                () -> nuevo("r", "c", FINALIDADES, "s".repeat(2_001), "i", "k"),
                () -> nuevo("r", "c", FINALIDADES, "s", "  ", "k"),
                () -> nuevo("r", "c", FINALIDADES, "s", "i".repeat(20_001), "k"),
                () -> nuevo("r", "c", FINALIDADES, "s", "i", ""),
                () -> nuevo("r", "c", FINALIDADES, "s", "i", "k".repeat(201)));

        for (var caso : casos) {
            assertThatThrownBy(caso::run).isInstanceOfSatisfying(ClientesException.class,
                    e -> assertThat(e.codigo()).isEqualTo(CodigoError.AVISO_INCOMPLETO));
        }
    }

    @Test
    @DisplayName("el codigo de una finalidad se guarda recortado")
    void finalidadAviso_codigoConEspacios_seRecorta() {
        assertThat(new FinalidadAviso("  VENTA ", true).codigo()).isEqualTo("VENTA");
    }
}
