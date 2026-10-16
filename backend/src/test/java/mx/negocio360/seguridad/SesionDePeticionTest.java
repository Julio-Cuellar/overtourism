package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.SesionActual;
import mx.negocio360.compartido.seguridad.SesionAuthentication;
import mx.negocio360.compartido.seguridad.SesionDePeticion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;

/** SG-02 y AZ-04: el negocio y el actor salen de la sesion; sin negocio activo no hay contexto ni permisos. */
class SesionDePeticionTest {

    private static final UUID NEGOCIO = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static void entraComo(SesionDePeticion sesion) {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new SesionAuthentication(sesion));
        SecurityContextHolder.setContext(contexto);
    }

    @Test
    @DisplayName("con negocio activo hay contexto con el actor de la cuenta y la autorizacion de sus permisos")
    void sesion_conNegocio_dacontextoYAutorizacion() {
        var sesion = new SesionDePeticion("cuenta-1", NEGOCIO, Set.of("clientes.sugerir"));

        assertThat(sesion.contexto()).hasValueSatisfying(c -> {
            assertThat(c.negocioId()).isEqualTo(NEGOCIO);
            assertThat(c.actor()).isEqualTo("cuenta-1");
        });
        assertThat(sesion.autorizacion().puede("clientes.sugerir")).isTrue();
        assertThat(sesion.autorizacion().puede("clientes.cliente.anonimizar")).isFalse();
    }

    @Test
    @DisplayName("AZ-04: sin negocio activo no hay contexto y todo permiso esta negado, aunque la sesion traiga permisos")
    void sesion_sinNegocio_noTieneContextoNiPermisos() {
        var sesion = new SesionDePeticion("cuenta-1", null, Set.of("clientes.sugerir"));

        assertThat(sesion.contexto()).isEmpty();
        assertThat(sesion.permisos()).isEmpty();
        assertThat(sesion.autorizacion().puede("clientes.sugerir")).isFalse();
    }

    @Test
    @DisplayName("la autenticacion expone los permisos como autoridades y no guarda credenciales")
    void autenticacion_exponePermisosComoAutoridades() {
        var autenticacion = new SesionAuthentication(new SesionDePeticion("c", NEGOCIO, Set.of("a.b", "a.c")));

        assertThat(autenticacion.isAuthenticated()).isTrue();
        assertThat(autenticacion.getCredentials()).isNull();
        assertThat(autenticacion.getAuthorities()).extracting(Object::toString)
                .containsExactlyInAnyOrder("a.b", "a.c");
        assertThat(autenticacion.toString()).doesNotContain("a.b");
    }

    @Test
    @DisplayName("SesionActual entrega el contexto de la sesion en curso")
    void sesionActual_conSesion_entregaContextoYAutorizacion() {
        entraComo(new SesionDePeticion("cuenta-9", NEGOCIO, Set.of("clientes.sugerir")));

        assertThat(SesionActual.contexto().negocioId()).isEqualTo(NEGOCIO);
        assertThat(SesionActual.contexto().actor()).isEqualTo("cuenta-9");
        assertThat(SesionActual.autorizacion().puede("clientes.sugerir")).isTrue();
    }

    @Test
    @DisplayName("SesionActual sin sesion falla como no autenticada, y sin negocio como acceso denegado")
    void sesionActual_sinSesionOSinNegocio_falla() {
        assertThatThrownBy(SesionActual::contexto).isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        entraComo(new SesionDePeticion("cuenta-9", null, Set.of()));
        assertThatThrownBy(SesionActual::contexto).isInstanceOf(AccessDeniedException.class);
        assertThat(SesionActual.autorizacion().puede("clientes.sugerir")).isFalse();
    }
}
