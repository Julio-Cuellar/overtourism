package mx.negocio360.inventario.aplicacion;

import static mx.negocio360.inventario.fakes.Afirmaciones.fallaCon;
import static mx.negocio360.inventario.fakes.Escenario.d;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import mx.negocio360.inventario.dominio.Articulo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.Rastreo;
import mx.negocio360.inventario.dominio.UnidadEstandar;
import mx.negocio360.inventario.fakes.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * R-I1 y R-I3 de Restaurantes: {@code articulosPorId} trae de una vez nombre, unidad base, CPP, {@code activo} y
 * {@code rastreo} (D-27) de varios articulos, y el rastreo existe en el dominio aunque hoy solo se cree {@code NINGUNO}.
 */
class ArticulosPorIdTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("articulosPorId trae varios articulos en una sola llamada, con unidad base, CPP, activo y rastreo")
    void variosEnUnaLlamada() {
        var harina = e.harina(e.ctxA);
        var azucar = e.crear(e.ctxA, "Azúcar", "g", null, null, null);
        e.entrada(e.ctxA, harina, "2", "kg", "20");

        var respuesta = e.api.articulosPorId(e.ctxA, Set.of(harina, azucar));

        assertThat(respuesta).containsOnlyKeys(harina, azucar);
        var h = respuesta.get(harina);
        assertThat(h.id()).isEqualTo(harina);
        assertThat(h.nombre()).isEqualTo("Harina");
        assertThat(h.unidadBase()).isEqualTo("g");
        assertThat(h.cpp()).isEqualByComparingTo("0.02");
        assertThat(h.activo()).isTrue();
        assertThat(h.rastreo()).isEqualTo(Rastreo.NINGUNO);
        assertThat(respuesta.get(azucar).cpp()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("los ids que no existen en el negocio no aparecen, ni los de otro negocio")
    void inexistentesYAjenosNoAparecen() {
        var propio = e.harina(e.ctxA);
        var ajeno = e.crear(e.ctxB, "Harina", "g", null, null, null);

        var respuesta = e.api.articulosPorId(e.ctxA, Set.of(propio, ajeno, UUID.randomUUID()));

        assertThat(respuesta).containsOnlyKeys(propio);
    }

    @Test
    @DisplayName("un articulo dado de baja se devuelve con activo=false (Restaurantes lo rechaza)")
    void inactivoSeDevuelveInactivo() {
        var sal = e.crear(e.ctxA, "Sal", "g", null, null, null);
        e.confirmar(e.ctxA, new BajaArticuloComando(sal));

        var respuesta = e.api.articulosPorId(e.ctxA, Set.of(sal));

        assertThat(respuesta.get(sal).activo()).isFalse();
    }

    @Test
    @DisplayName("sin ids responde vacio; con mas de 100 falla como aBase")
    void limites() {
        assertThat(e.api.articulosPorId(e.ctxA, Set.of())).isEmpty();
        var demasiados = new HashSet<UUID>();
        IntStream.range(0, 101).forEach(i -> demasiados.add(UUID.randomUUID()));

        assertThatThrownBy(() -> e.api.articulosPorId(e.ctxA, demasiados))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("crear sin rastreo o con NINGUNO deja el articulo sin rastreo")
    void crearSinRastreoEsNinguno() {
        var sinDato = e.crear(e.ctxA, "Sal", "g", null, null, null);
        var explicito = e.confirmar(e.ctxA, new CrearArticuloComando("Pimienta", null, "g", null, null, null,
                Rastreo.NINGUNO)).recursoId();

        var respuesta = e.api.articulosPorId(e.ctxA, Set.of(sinDato, explicito));

        assertThat(respuesta.values()).extracting(ArticuloBase::rastreo).containsOnly(Rastreo.NINGUNO);
    }

    @Test
    @DisplayName("crear con LOTE o SERIE se rechaza hasta que se programen lotes y series")
    void crearConLoteOSerieSeRechaza() {
        for (var rastreo : new Rastreo[] {Rastreo.LOTE, Rastreo.SERIE}) {
            var comando = new CrearArticuloComando("Queso " + rastreo, null, "g", null, null, null, rastreo);

            fallaCon(() -> e.api.preparar(e.ctxA, comando), CodigoError.RASTREO_NO_SOPORTADO);
        }
        assertThat(e.almacen.cuantosArticulos()).isZero();
    }

    @Test
    @DisplayName("el articulo del dominio nace sin rastreo y lo conserva al editarse o darse de baja")
    void dominioConservaRastreo() {
        var ahora = Escenario.AHORA;
        var articulo = Articulo.nuevo(UUID.randomUUID(), UUID.randomUUID(), "Sal", null, UnidadEstandar.G,
                java.util.Map.of(), d("0"), "yo", ahora);

        assertThat(articulo.rastreo()).isEqualTo(Rastreo.NINGUNO);
        assertThat(articulo.conDatos("Sal fina", null, d("0"), ahora).rastreo()).isEqualTo(Rastreo.NINGUNO);
        assertThat(articulo.dadoDeBaja(ahora).rastreo()).isEqualTo(Rastreo.NINGUNO);
        var conSerie = Articulo.nuevo(UUID.randomUUID(), UUID.randomUUID(), "Licuadora", null, UnidadEstandar.PIEZA,
                java.util.Map.of(), d("0"), "yo", ahora, Rastreo.SERIE);
        assertThat(conSerie.conUnidadPropia("caja", d("12"), ahora).rastreo()).isEqualTo(Rastreo.SERIE);
    }
}
