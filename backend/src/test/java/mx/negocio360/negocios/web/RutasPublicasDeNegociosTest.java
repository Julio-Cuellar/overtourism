package mx.negocio360.negocios.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SG-01: la plataforma falla cerrado, asi que cada ruta REST que Negocios deja sin sesion la declara aqui, una por una. Esta
 * prueba fija la lista para que no crezca sin que alguien la vea: el catalogo publico de solo lectura, la moderacion (que
 * valida su propio token de servicio y no existe sin el) y los comandos de la demo (que no existen fuera del perfil demo).
 */
class RutasPublicasDeNegociosTest {

    @Test
    @DisplayName("las rutas REST sin sesion de Negocios son exactamente estas tres")
    void rutas_sonExactamenteEstas() {
        assertThat(new RutasPublicasDeNegocios().patrones()).containsExactly("/catalogos/**", "/admin/**", "/demo/**");
    }
}
