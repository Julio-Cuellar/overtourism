package mx.negocio360.identidad.aplicacion;

import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A;
import static mx.negocio360.identidad.aplicacion.Escenario.TEL_B;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D-79: el negocio que se pidio aparece como pendiente en el perfil y en la respuesta de iniciar sesion, para que la
 * app sepa «pendiente» o «listo» con una sola llamada, sin sondear otra ruta.
 */
class NegociosPendientesTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("un negocio pedido aparece como pendiente en el perfil hasta que el modulo de negocios responde")
    void yo_negocioPedido_apareceComoPendienteYDespuesComoNegocio() {
        var ana = e.registrar(TEL_A, "Ana");
        var negocioId = e.api.solicitarNegocio(ana.token(), "Fonda Doña Chela", "RESTAURANTE").negocioId();

        var antes = e.api.yo(ana.token());

        assertThat(antes.negocios()).isEmpty();
        assertThat(antes.negociosPendientes()).singleElement().satisfies(p -> {
            assertThat(p.id()).isEqualTo(negocioId);
            assertThat(p.nombre()).isEqualTo("Fonda Doña Chela");
            assertThat(p.giro()).isEqualTo("RESTAURANTE");
            assertThat(p.estado()).isEqualTo("PENDIENTE");
        });

        e.escucha.alRecibir(e.negocioRegistrado(negocioId, ana.cuenta().id(), "Fonda Doña Chela", "RESTAURANTE"));

        var despues = e.api.yo(ana.token());
        assertThat(despues.negociosPendientes()).isEmpty();
        assertThat(despues.negocios()).extracting(n -> n.id()).containsExactly(negocioId);
    }

    @Test
    @DisplayName("cada persona ve solo sus pendientes, por orden de pedido")
    void yo_pendientes_soloLosPropiosEnOrden() {
        var ana = e.registrar(TEL_A, "Ana");
        var beto = e.registrar(TEL_B, "Beto");
        var primero = e.api.solicitarNegocio(ana.token(), "Fonda Uno", "RESTAURANTE").negocioId();
        e.reloj.avanzar(Duration.ofSeconds(1));
        var segundo = e.api.solicitarNegocio(ana.token(), "Museo Dos", "MUSEO").negocioId();
        e.api.solicitarNegocio(beto.token(), "Tienda de Beto", "MISCELANEA");

        assertThat(e.api.yo(ana.token()).negociosPendientes()).extracting(p -> p.id()).containsExactly(primero, segundo);
        assertThat(e.api.yo(beto.token()).negociosPendientes()).singleElement()
                .satisfies(p -> assertThat(p.nombre()).isEqualTo("Tienda de Beto"));
    }

    @Test
    @DisplayName("al volver a iniciar sesion, la respuesta trae los pendientes")
    void iniciarSesion_traeLosPendientes() {
        var ana = e.registrar(TEL_A, "Ana");
        e.api.solicitarNegocio(ana.token(), "Fonda", "RESTAURANTE");
        e.reloj.avanzar(Duration.ofMinutes(20));

        var sesion = e.iniciar(TEL_A);

        assertThat(sesion.negociosPendientes()).singleElement().satisfies(p -> assertThat(p.nombre()).isEqualTo("Fonda"));
        assertThat(sesion.negocios()).isEmpty();
    }

    @Test
    @DisplayName("el aviso que no se puede aplicar deja el pedido pendiente; el aplicado lo consume")
    void pendiente_seConsumeSoloAlAplicarse() {
        var ana = e.registrar(TEL_A, "Ana");
        var negocioId = e.api.solicitarNegocio(ana.token(), "Fonda", "RESTAURANTE").negocioId();

        e.escucha.alRecibir(e.negocioRegistrado(negocioId, ana.cuenta().id(), "Fonda", "FARMACIA"));

        assertThat(e.api.yo(ana.token()).negociosPendientes()).hasSize(1);
        e.escucha.alRecibir(e.negocioRegistrado(negocioId, ana.cuenta().id(), "Fonda", "RESTAURANTE"));
        assertThat(e.api.yo(ana.token()).negociosPendientes()).isEmpty();
    }
}
