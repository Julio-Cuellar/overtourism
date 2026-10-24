package mx.negocio360.compartido.comandos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato de {@link RepositorioPreparaciones}: lo que debe cumplir cualquier almacen de la tabla {@code preparacion},
 * en memoria o en PostgreSQL. Cada subclase aporta el almacen; la bateria es la misma (F6). Todas las operaciones llevan
 * el negocio, porque en la base la seguridad por fila no muestra una fila sin el.
 */
public abstract class ContratoRepositorioPreparacionesTest {

    protected static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");

    /** El almacen bajo prueba; cada llamada puede devolver el mismo. */
    protected abstract RepositorioPreparaciones almacen();

    private static String hashNuevo() {
        return TokenOpaco.hash(TokenOpaco.nuevo());
    }

    private static Preparacion preparacion(UUID negocio, String hash, String ambito) {
        return new Preparacion(hash, negocio, "actor-a", "registrar_venta", "hash-del-comando",
                "{\"turnoId\":\"t1\"}", "Cobrar 10.00.", ambito, AHORA.plusSeconds(300), null);
    }

    @Test
    @DisplayName("una preparacion guardada se lee tal cual: comando, resumen, ambito y vencimiento")
    void guardarYLeer() {
        var negocio = UUID.randomUUID();
        var hash = hashNuevo();

        almacen().guardar(preparacion(negocio, hash, "turno-1"));

        var leida = almacen().porHash(negocio, hash).orElseThrow();
        assertThat(leida.actor()).isEqualTo("actor-a");
        assertThat(leida.comandoTipo()).isEqualTo("registrar_venta");
        assertThat(leida.comandoHash()).isEqualTo("hash-del-comando");
        assertThat(leida.comando()).isEqualTo("{\"turnoId\":\"t1\"}");
        assertThat(leida.resumen()).isEqualTo("Cobrar 10.00.");
        assertThat(leida.ambito()).isEqualTo("turno-1");
        assertThat(leida.expiraEn()).isEqualTo(AHORA.plusSeconds(300));
        assertThat(leida.usadoEn()).isNull();
    }

    @Test
    @DisplayName("el ambito puede faltar")
    void ambitoNulo() {
        var negocio = UUID.randomUUID();
        var hash = hashNuevo();
        almacen().guardar(preparacion(negocio, hash, null));

        assertThat(almacen().porHash(negocio, hash).orElseThrow().ambito()).isNull();
    }

    @Test
    @DisplayName("un token desconocido no existe")
    void desconocido() {
        assertThat(almacen().porHash(UUID.randomUUID(), hashNuevo())).isEmpty();
    }

    @Test
    @DisplayName("la preparacion de un negocio no la ve ni la gasta otro negocio")
    void otroNegocioNoLaVe() {
        var negocio = UUID.randomUUID();
        var hash = hashNuevo();
        almacen().guardar(preparacion(negocio, hash, null));
        var otro = UUID.randomUUID();

        assertThat(almacen().porHash(otro, hash)).isEmpty();
        assertThat(almacen().marcarUsada(otro, hash, AHORA)).isFalse();
        almacen().vaciar(otro, hash);

        var intacta = almacen().porHash(negocio, hash).orElseThrow();
        assertThat(intacta.comando()).isNotNull();
        assertThat(intacta.usadoEn()).isNull();
    }

    @Test
    @DisplayName("marcarUsada: la primera gana, la segunda recibe falso, y la fila ya no conserva el comando (SC-10)")
    void marcarUsadaEsDeUnSoloUso() {
        var negocio = UUID.randomUUID();
        var hash = hashNuevo();
        almacen().guardar(preparacion(negocio, hash, null));

        var primera = almacen().marcarUsada(negocio, hash, AHORA);
        var segunda = almacen().marcarUsada(negocio, hash, AHORA.plusSeconds(1));

        assertThat(primera).isTrue();
        assertThat(segunda).isFalse();
        var leida = almacen().porHash(negocio, hash).orElseThrow();
        assertThat(leida.usadoEn()).isEqualTo(AHORA);
        assertThat(leida.comando()).isNull();
        assertThat(leida.resumen()).isNull();
    }

    @Test
    @DisplayName("vaciar quita el comando y el resumen y conserva el resto de la fila")
    void vaciar() {
        var negocio = UUID.randomUUID();
        var hash = hashNuevo();
        almacen().guardar(preparacion(negocio, hash, "turno-1"));

        almacen().vaciar(negocio, hash);

        var leida = almacen().porHash(negocio, hash).orElseThrow();
        assertThat(leida.comando()).isNull();
        assertThat(leida.resumen()).isNull();
        assertThat(leida.comandoHash()).isEqualTo("hash-del-comando");
        assertThat(leida.ambito()).isEqualTo("turno-1");
    }

    @Test
    @DisplayName("vigentes cuenta las sin usar y sin vencer de un tipo y ambito, solo del negocio")
    void vigentes() {
        var negocio = UUID.randomUUID();
        var usada = hashNuevo();
        almacen().guardar(preparacion(negocio, hashNuevo(), "turno-1"));
        almacen().guardar(preparacion(negocio, hashNuevo(), "turno-1"));
        almacen().guardar(preparacion(negocio, usada, "turno-1"));
        almacen().guardar(preparacion(negocio, hashNuevo(), "turno-2"));
        almacen().guardar(preparacion(UUID.randomUUID(), hashNuevo(), "turno-1"));
        almacen().marcarUsada(negocio, usada, AHORA);

        assertThat(almacen().vigentes(negocio, "registrar_venta", "turno-1", AHORA)).isEqualTo(2);
        assertThat(almacen().vigentes(negocio, "registrar_venta", "turno-2", AHORA)).isEqualTo(1);
        assertThat(almacen().vigentes(negocio, "otro_tipo", "turno-1", AHORA)).isZero();
        assertThat(almacen().vigentes(negocio, "registrar_venta", "turno-1", AHORA.plusSeconds(300))).isZero();
    }
}
