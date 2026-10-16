package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_B;
import static mx.negocio360.identidad.aplicacion.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import mx.negocio360.identidad.dominio.CodigoError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** IDN-2.1 y IDN-2.2: los roles que ofrece el giro del negocio activo y el catalogo de permisos. */
class RolesYPermisosTest {

    private final Escenario e = new Escenario();

    private String tokenEnNegocio(String giro) {
        var ana = e.registrar(TEL_A, "Ana");
        var negocio = e.crearNegocio(ana.token(), "Negocio de prueba", giro);
        return e.api.elegirNegocio(ana.token(), negocio).token();
    }

    @Test
    @DisplayName("un restaurante ofrece mesero y cocinero; cada rol con sus permisos en orden")
    void roles_restaurante() {
        var roles = e.api.roles(tokenEnNegocio("RESTAURANTE"));

        assertThat(roles).extracting(r -> r.codigo()).containsExactly("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO",
                "MESERO", "COCINERO");
        assertThat(roles).filteredOn(r -> r.codigo().equals("CAJERO")).singleElement().satisfies(r -> {
            assertThat(r.nombre()).isEqualTo("Cajero");
            assertThat(r.permisos()).containsExactly("clientes.cliente.registrar", "clientes.consentimiento.registrar",
                    "clientes.sugerir");
        });
    }

    @Test
    @DisplayName("un museo ofrece taquillero y guia, y una miscelanea solo los cuatro roles comunes")
    void roles_museoYMiscelanea() {
        assertThat(e.api.roles(tokenEnNegocio("MUSEO"))).extracting(r -> r.codigo())
                .containsExactly("DUENO", "ADMINISTRADOR", "GERENTE", "TAQUILLERO", "GUIA");
        var otro = new Escenario();
        var ana = otro.registrar(TEL_B, "Beto");
        var negocio = otro.crearNegocio(ana.token(), "Tienda", "MISCELANEA");
        var token = otro.api.elegirNegocio(ana.token(), negocio).token();
        assertThat(otro.api.roles(token)).extracting(r -> r.codigo())
                .containsExactly("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO");
    }

    @Test
    @DisplayName("el catalogo de permisos lista cada permiso con su modulo y si es sensible")
    void permisos_catalogoCompleto() {
        var permisos = e.api.permisos(tokenEnNegocio("RESTAURANTE"));

        assertThat(permisos).hasSize(16).extracting(p -> p.codigo()).isSorted();
        assertThat(permisos).filteredOn(p -> p.codigo().equals("identidad.miembro.invitar")).singleElement()
                .satisfies(p -> {
                    assertThat(p.modulo()).isEqualTo("identidad");
                    assertThat(p.sensible()).isTrue();
                    assertThat(p.descripcion()).isNotBlank();
                });
    }

    @Test
    @DisplayName("ver roles y permisos exige identidad.equipo.ver: el cajero no puede")
    void rolesYPermisos_exigenElPermiso() {
        var ana = e.registrar(TEL_A, "Ana");
        var cajera = e.registrar(TEL_B, "Carla");
        var negocio = e.crearNegocio(ana.token(), "Fonda", "RESTAURANTE");
        e.agregarMiembro(negocio, cajera.cuenta().id(), "CAJERO");
        e.reloj.avanzar(Duration.ofMinutes(20));
        var token = e.api.elegirNegocio(e.iniciar(TEL_B).token(), negocio).token();

        fallaCon(() -> e.api.roles(token), CodigoError.PERMISO_DENEGADO);
        fallaCon(() -> e.api.permisos(token), CodigoError.PERMISO_DENEGADO);
    }

    @Test
    @DisplayName("sin negocio activo o sin sesion no se ven roles ni permisos")
    void rolesYPermisos_sinNegocioOSinSesion() {
        var sinNegocio = e.registrar(TEL_A, "Ana").token();

        fallaCon(() -> e.api.roles(sinNegocio), CodigoError.NEGOCIO_NO_DISPONIBLE);
        fallaCon(() -> e.api.permisos(sinNegocio), CodigoError.NEGOCIO_NO_DISPONIBLE);
        fallaCon(() -> e.api.roles("token-inventado"), CodigoError.NO_AUTENTICADO);
        fallaCon(() -> e.api.permisos(null), CodigoError.NO_AUTENTICADO);
    }
}
