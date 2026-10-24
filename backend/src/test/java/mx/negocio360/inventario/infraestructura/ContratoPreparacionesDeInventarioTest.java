package mx.negocio360.inventario.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.inventario.aplicacion.Preparacion;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioPreparaciones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato de {@link RepositorioPreparaciones} de Inventario, en memoria o en PostgreSQL. Todas las operaciones llevan el
 * negocio, porque en la base la seguridad por fila no muestra una fila sin el. El comando de Inventario no lleva datos
 * personales, asi que se conserva despues de usarse.
 */
public abstract class ContratoPreparacionesDeInventarioTest {

    protected static final Instant AHORA = Instant.parse("2026-10-16T18:00:00Z");

    /** El almacen bajo prueba. */
    protected abstract RepositorioPreparaciones almacen();

    private static Preparacion preparacion(UUID negocio, String hash) {
        return new Preparacion(hash, negocio, "actor-a", "registrar_entrada", "hash-del-comando",
                "{\"articuloId\":\"a1\"}", "Entrada de 2 kg.", AHORA.plusSeconds(300), null);
    }

    private static String hashNuevo() {
        return TokenOpaco.hash(TokenOpaco.nuevo());
    }

    @Test
    @DisplayName("una preparacion guardada se lee tal cual")
    void guardarYLeer() {
        var negocio = UUID.randomUUID();
        var hash = hashNuevo();

        almacen().guardar(preparacion(negocio, hash));

        assertThat(almacen().porHash(negocio, hash)).contains(preparacion(negocio, hash));
    }

    @Test
    @DisplayName("la preparacion de un negocio no la ve ni la gasta otro negocio")
    void otroNegocioNoLaVe() {
        var negocio = UUID.randomUUID();
        var hash = hashNuevo();
        almacen().guardar(preparacion(negocio, hash));

        assertThat(almacen().porHash(UUID.randomUUID(), hash)).isEmpty();
        assertThat(almacen().marcarUsada(UUID.randomUUID(), hash, AHORA)).isFalse();
        assertThat(almacen().porHash(negocio, hash).orElseThrow().usadoEn()).isNull();
    }

    @Test
    @DisplayName("marcarUsada: la primera gana y la segunda recibe falso")
    void marcarUsadaEsDeUnSoloUso() {
        var negocio = UUID.randomUUID();
        var hash = hashNuevo();
        almacen().guardar(preparacion(negocio, hash));

        assertThat(almacen().marcarUsada(negocio, hash, AHORA)).isTrue();
        assertThat(almacen().marcarUsada(negocio, hash, AHORA.plusSeconds(1))).isFalse();
        assertThat(almacen().porHash(negocio, hash).orElseThrow().usadoEn()).isEqualTo(AHORA);
    }
}
