package mx.negocio360.identidad.infraestructura;

import static mx.negocio360.identidad.aplicacion.Escenario.TEL_A;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.identidad.aplicacion.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El escucha recibe cualquier SobreEvento del bus y solo actua con negocio.registrado v1 (R2, R3 y R5). */
class EscuchaDeNegociosTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("un negocio.registrado v1 deja a quien lo pidio como dueño")
    void alRecibir_negocioRegistrado_creaElDueno() {
        var ana = e.registrar(TEL_A, "Ana");
        var negocio = e.api.solicitarNegocio(ana.token(), "Fonda", "RESTAURANTE").negocioId();

        e.escucha.alRecibir(e.negocioRegistrado(negocio, ana.cuenta().id(), "Fonda", "RESTAURANTE"));

        assertThat(e.membresias.porCuentaYNegocio(ana.cuenta().id(), negocio)).isPresent();
    }

    @Test
    @DisplayName("los eventos de otros modulos o de otro tipo se ignoran sin error")
    void alRecibir_otrosEventos_seIgnoran() {
        var ahora = e.reloj.instant();
        var ajeno = new SobreEvento(UuidV7.generar(ahora), "cliente.registrado", 1, UuidV7.generar(ahora), "clientes",
                ahora, UuidV7.generar(ahora), null, Map.of("cliente_id", UuidV7.generar(ahora).toString()));
        var propio = new SobreEvento(UuidV7.generar(ahora), "negocio.registro_solicitado", 1, UuidV7.generar(ahora),
                "identidad", ahora, UuidV7.generar(ahora), null, Map.of("cuenta_id", "x"));

        assertThatCode(() -> e.escucha.alRecibir(ajeno)).doesNotThrowAnyException();
        assertThatCode(() -> e.escucha.alRecibir(propio)).doesNotThrowAnyException();

        assertThat(e.membresias.deCuenta(UuidV7.generar(ahora))).isEmpty();
        assertThat(e.eventos.eventos()).isEmpty();
    }

    @Test
    @DisplayName("un negocio.registrado mal formado se descarta sin lanzar: reintentarlo no lo arreglaria")
    void alRecibir_malFormado_noLanza() {
        var ahora = e.reloj.instant();
        var roto = new SobreEvento(UuidV7.generar(ahora), "negocio.registrado", 1, UuidV7.generar(ahora), "negocios",
                ahora, UuidV7.generar(ahora), null, Map.of("nombre", "Fonda"));

        assertThatCode(() -> e.escucha.alRecibir(roto)).doesNotThrowAnyException();
    }
}
