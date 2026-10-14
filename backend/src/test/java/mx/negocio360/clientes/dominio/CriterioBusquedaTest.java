package mx.negocio360.clientes.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import mx.negocio360.clientes.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** I-04: la busqueda ignora mayusculas y acentos; por telefono solo coincide el numero completo. */
class CriterioBusquedaTest {

    private static final UUID NEGOCIO = UUID.randomUUID();
    private static final Cliente JOSE = Fixtures.cliente(NEGOCIO, "José", "Pérez", "222 123 4567");

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("sin texto se listan todos")
    void de_textoVacio_esTodos(String texto) {
        assertThat(CriterioBusqueda.de(texto)).isEqualTo(new CriterioBusqueda.Todos());
    }

    @Test
    @DisplayName("el texto nulo tambien lista todos")
    void de_textoNulo_esTodos() {
        assertThat(CriterioBusqueda.de(null)).isEqualTo(new CriterioBusqueda.Todos());
    }

    @Test
    @DisplayName("un nombre se normaliza sin acentos ni mayusculas")
    void de_nombre_esPorNombreNormalizado() {
        assertThat(CriterioBusqueda.de("  JOSÉ   pérez ")).isEqualTo(new CriterioBusqueda.PorNombre("jose perez"));
    }

    @Test
    @DisplayName("un telefono completo se normaliza a E.164")
    void de_telefonoCompleto_esPorTelefono() {
        assertThat(CriterioBusqueda.de("222 123 4567")).isEqualTo(new CriterioBusqueda.PorTelefono("+522221234567"));
        assertThat(CriterioBusqueda.de("+52 (222) 123-4567"))
                .isEqualTo(new CriterioBusqueda.PorTelefono("+522221234567"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"5678", "222 123", "+52 222", "(222) 123-45", "4567 ", "12"})
    @DisplayName("un telefono parcial no busca nada")
    void de_telefonoParcial_esNinguno(String texto) {
        assertThat(CriterioBusqueda.de(texto)).isEqualTo(new CriterioBusqueda.Ninguno());
    }

    @Test
    @DisplayName("un telefono con espacios no separables sigue siendo un telefono")
    void de_telefonoConEspaciosUnicode_esPorTelefono() {
        assertThat(CriterioBusqueda.de("222 123 4567"))
                .isEqualTo(new CriterioBusqueda.PorTelefono("+522221234567"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"́́", "́", " ́ "})
    @DisplayName("un texto que queda vacio al normalizarlo no busca nada, en lugar de coincidir con todos")
    void de_textoQueNormalizaAVacio_esNinguno(String texto) {
        assertThat(CriterioBusqueda.de(texto)).isEqualTo(new CriterioBusqueda.Ninguno());
    }

    @Test
    @DisplayName("un texto con letras y numeros se busca como nombre")
    void de_letrasYNumeros_esPorNombre() {
        assertThat(CriterioBusqueda.de("Taller 2")).isEqualTo(new CriterioBusqueda.PorNombre("taller 2"));
    }

    @Test
    @DisplayName("por nombre coincide con parte del nombre o de los apellidos")
    void coincide_porNombre_buscaEnNombreYApellidos() {
        assertThat(CriterioBusqueda.de("jose").coincide(JOSE)).isTrue();
        assertThat(CriterioBusqueda.de("PEREZ").coincide(JOSE)).isTrue();
        assertThat(CriterioBusqueda.de("jos").coincide(JOSE)).isTrue();
        assertThat(CriterioBusqueda.de("maria").coincide(JOSE)).isFalse();
    }

    @Test
    @DisplayName("por telefono coincide solo con el numero exacto")
    void coincide_porTelefono_exigeElNumeroCompleto() {
        assertThat(CriterioBusqueda.de("2221234567").coincide(JOSE)).isTrue();
        assertThat(CriterioBusqueda.de("2221234568").coincide(JOSE)).isFalse();
    }

    @Test
    @DisplayName("Ninguno no coincide con nadie y Todos coincide con todos los activos")
    void coincide_ningunoYTodos() {
        assertThat(new CriterioBusqueda.Ninguno().coincide(JOSE)).isFalse();
        assertThat(new CriterioBusqueda.Todos().coincide(JOSE)).isTrue();
    }

    @Test
    @DisplayName("un cliente anonimizado nunca coincide")
    void coincide_clienteAnonimizado_nuncaCoincide() {
        var anonimizado = Fixtures.anonimizado(NEGOCIO);

        assertThat(new CriterioBusqueda.Todos().coincide(anonimizado)).isFalse();
        assertThat(CriterioBusqueda.de("anonimizado").coincide(anonimizado)).isFalse();
    }

    @Test
    @DisplayName("un cliente sin telefono no coincide por telefono")
    void coincide_sinTelefono_noCoincidePorTelefono() {
        var sinTelefono = Fixtures.cliente(NEGOCIO, "Ana", null, null);

        assertThat(CriterioBusqueda.de("2221234567").coincide(sinTelefono)).isFalse();
    }
}
