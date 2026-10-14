package mx.negocio360.clientes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import mx.negocio360.clientes.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * SG-17: los textos tienen largo y caracteres limitados. Un caracter de control rompe el INSERT (NUL), falsifica
 * lineas de registro (salto de linea) o suplanta a otro cliente (controles bidireccionales y de ancho cero).
 */
class CaracteresTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 14);

    private static DatosCliente de(String nombre, String apellidos, String correo, String notas) {
        return DatosCliente.de(nombre, apellidos, null, correo, null, notas, HOY);
    }

    private static void assertCodigo(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run)
                .isInstanceOfSatisfying(ClientesException.class, e -> assertThat(e.codigo()).isEqualTo(esperado));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ana\u0000", "Ana\nPerez", "An\ra", "Ana\tPerez", "An‮a", "Ana​", "An a",
            "An a", "​", "  ", " ", " "})
    @DisplayName("un nombre con controles, saltos o caracteres invisibles, o hecho solo de espacios Unicode, se rechaza")
    void de_nombreConCaracteresProhibidos_fallaConNombreInvalido(String nombre) {
        assertCodigo(() -> de(nombre, null, null, null), CodigoError.NOMBRE_INVALIDO);
    }

    @Test
    @DisplayName("los apellidos con caracteres prohibidos tambien se rechazan")
    void de_apellidosConCaracteresProhibidos_fallanConNombreInvalido() {
        assertCodigo(() -> de("Ana", "Perez\u0000", null, null), CodigoError.NOMBRE_INVALIDO);
        assertCodigo(() -> de("Ana", "Perez‮", null, null), CodigoError.NOMBRE_INVALIDO);
    }

    @Test
    @DisplayName("los espacios Unicode de los extremos se recortan y los de en medio pasan a espacio normal")
    void de_espaciosUnicode_seRecortanYSeNormalizan() {
        var datos = de(" Ana Maria ", " Perez ", null, null);

        assertThat(datos.nombre()).isEqualTo("Ana Maria");
        assertThat(datos.apellidos()).isEqualTo("Perez");
    }

    @Test
    @DisplayName("las letras con acento y la enie siguen siendo validas")
    void de_nombreConAcentosYEnie_esValido() {
        assertThat(de("Muñoz", "Núñez Álvarez", null, null).nombreCompleto())
                .isEqualTo("Muñoz Núñez Álvarez");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a,b<x>@c.co", "ana\u0000@x.com", "an‮a@x.com", "ñandu@x.com", "ana@\nx.com",
            "ana@x​.com", "ana@x..com", "ana@-.com"})
    @DisplayName("un correo con caracteres fuera de la lista permitida se rechaza")
    void de_correoConCaracteresProhibidos_fallaConCorreoInvalido(String correo) {
        assertCodigo(() -> de("Ana", null, correo, null), CodigoError.CORREO_INVALIDO);
    }

    @Test
    @DisplayName("las notas conservan saltos de linea y tabulaciones pero pierden controles y caracteres invisibles")
    void de_notas_quitanLosCaracteresProhibidos() {
        var datos = de("Ana", null, null, "linea 1\nlinea\t2\u0000 fin‮​");

        assertThat(datos.notas()).isEqualTo("linea 1\nlinea\t2 fin");
    }

    @Test
    @DisplayName("unas notas que solo tenian caracteres prohibidos quedan vacias")
    void de_notasSoloConControles_quedanNulas() {
        assertThat(de("Ana", null, null, "\u0000​").notas()).isNull();
    }

    @Test
    @DisplayName("el tope de 500 caracteres de las notas cuenta despues de quitar los prohibidos")
    void de_notasConControles_cuentanSoloLosCaracteresQueQuedan() {
        var notas = "n".repeat(500) + "\u0000\u0000";

        assertThat(de("Ana", null, null, notas).notas()).hasSize(500);
    }

    private static AvisoPrivacidad aviso(String responsable, String simplificado, String integral, String contacto) {
        return AvisoPrivacidad.nuevo(UUID.randomUUID(), UUID.randomUUID(), 1, responsable, "Calle 1",
                List.of(new FinalidadAviso("VENTA", false)), simplificado, integral, contacto, "locatario",
                Fixtures.AHORA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u0000", "‮", "​", " "})
    @DisplayName("el aviso rechaza controles y caracteres invisibles en cualquiera de sus textos")
    void aviso_conCaracteresProhibidos_fallaConAvisoIncompleto(String prohibido) {
        assertCodigo(() -> aviso("Ta" + prohibido + "ller", "s", "i", "c"), CodigoError.AVISO_INCOMPLETO);
        assertCodigo(() -> aviso("Taller", "s" + prohibido + "x", "i", "c"), CodigoError.AVISO_INCOMPLETO);
        assertCodigo(() -> aviso("Taller", "s", "i" + prohibido + "x", "c"), CodigoError.AVISO_INCOMPLETO);
        assertCodigo(() -> aviso("Taller", "s", "i", "c" + prohibido + "x"), CodigoError.AVISO_INCOMPLETO);
    }

    @Test
    @DisplayName("el aviso conserva saltos de linea y tabulaciones en sus textos largos")
    void aviso_conSaltosDeLinea_loConserva() {
        var aviso = aviso("Taller", "linea 1\nlinea 2", "parrafo 1\r\n\tparrafo 2", "c");

        assertThat(aviso.textoSimplificado()).isEqualTo("linea 1\nlinea 2");
        assertThat(aviso.textoIntegral()).isEqualTo("parrafo 1\r\n\tparrafo 2");
    }

    @Test
    @DisplayName("el aviso admite como maximo 20 finalidades")
    void aviso_masDeVeinteFinalidades_fallaConAvisoIncompleto() {
        var veintiuna = new java.util.ArrayList<FinalidadAviso>();
        for (int i = 0; i < 21; i++) {
            veintiuna.add(new FinalidadAviso("F" + i, false));
        }

        assertCodigo(() -> AvisoPrivacidad.nuevo(UUID.randomUUID(), UUID.randomUUID(), 1, "T", "C", veintiuna, "s", "i",
                "c", "a", Fixtures.AHORA), CodigoError.AVISO_INCOMPLETO);
        assertThat(AvisoPrivacidad.nuevo(UUID.randomUUID(), UUID.randomUUID(), 1, "T", "C",
                veintiuna.subList(0, 20), "s", "i", "c", "a", Fixtures.AHORA).finalidades()).hasSize(20);
    }
}
