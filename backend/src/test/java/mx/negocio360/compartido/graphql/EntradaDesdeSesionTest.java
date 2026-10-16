package mx.negocio360.compartido.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;
import mx.negocio360.compartido.seguridad.SesionDePeticion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las dos entradas autenticadas de {@code /graphql} (el filtro HTTP y la conexion WebSocket) terminan en una
 * {@link EntradaGraphQl}. La del WebSocket sale de la {@link SesionDePeticion} que entrega el autenticador de la
 * plataforma; esta prueba fija esa conversion.
 */
class EntradaDesdeSesionTest {

    private static final UUID CUENTA = UUID.fromString("0192f39f-2222-7000-8000-00000000b001");
    private static final UUID NEGOCIO = UUID.fromString("0192f39f-1111-7000-8000-00000000a001");
    private static final String DISPOSITIVO = "0192f39f-3333-7000-8000-00000000c001";

    @Test
    @DisplayName("una sesion con negocio da cuenta, contexto con el actor, permisos y el dispositivo")
    void sesionConNegocio() {
        var entrada = EntradaGraphQl.deSesion(new SesionDePeticion(CUENTA.toString(), NEGOCIO, Set.of("a.b")),
                DISPOSITIVO);

        assertThat(entrada.autenticada()).isTrue();
        assertThat(entrada.exigirCuenta()).isEqualTo(CUENTA);
        assertThat(entrada.exigirContexto().negocioId()).isEqualTo(NEGOCIO);
        assertThat(entrada.exigirContexto().actor()).isEqualTo(CUENTA.toString());
        assertThat(entrada.autorizacion().puede("a.b")).isTrue();
        assertThat(entrada.autorizacion().puede("a.c")).isFalse();
        assertThat(entrada.dispositivo()).isEqualTo(DISPOSITIVO);
    }

    @Test
    @DisplayName("una sesion sin negocio activo esta autenticada, sin contexto y sin ningun permiso")
    void sesionSinNegocio() {
        var entrada = EntradaGraphQl.deSesion(new SesionDePeticion(CUENTA.toString(), null, Set.of("a.b")), null);

        assertThat(entrada.autenticada()).isTrue();
        assertThat(entrada.autorizacion().puede("a.b")).isFalse();
        assertThatThrownBy(entrada::exigirContexto).isInstanceOfSatisfying(NoAutenticadoException.class,
                e -> assertThat(e.codigo()).isEqualTo(NoAutenticadoException.SIN_NEGOCIO_ACTIVO));
    }

    @Test
    @DisplayName("un actor que no es el id de una cuenta falla en vez de inventar una sesion, sin repetir el actor")
    void actorQueNoEsUnaCuenta() {
        var sesion = new SesionDePeticion("no-es-uuid-secreto", NEGOCIO, Set.of());

        assertThatThrownBy(() -> EntradaGraphQl.deSesion(sesion, null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("no-es-uuid-secreto");
    }

    @Test
    @DisplayName("para el registro: la cuenta y el negocio se pueden pedir sin que falle si no hay")
    void cuentaYNegocioOpcionales() {
        var conNegocio = EntradaGraphQl.deSesion(new SesionDePeticion(CUENTA.toString(), NEGOCIO, Set.of()), null);
        var sinNegocio = EntradaGraphQl.deSesion(new SesionDePeticion(CUENTA.toString(), null, Set.of()), null);
        var anonima = EntradaGraphQl.anonima(DISPOSITIVO);

        assertThat(conNegocio.cuenta()).contains(CUENTA);
        assertThat(conNegocio.negocio()).contains(NEGOCIO);
        assertThat(sinNegocio.cuenta()).contains(CUENTA);
        assertThat(sinNegocio.negocio()).isEmpty();
        assertThat(anonima.cuenta()).isEmpty();
        assertThat(anonima.negocio()).isEmpty();
    }
}
