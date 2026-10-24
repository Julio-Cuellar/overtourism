package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import mx.negocio360.compartido.comandos.ContratoRepositorioPreparacionesTest;
import mx.negocio360.compartido.comandos.Preparacion;
import mx.negocio360.compartido.comandos.RepositorioPreparaciones;
import mx.negocio360.compartido.comandos.RepositorioPreparacionesJdbc;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La tabla {@code preparacion} de Ventas y de Caja contra PostgreSQL de verdad, con el rol de la aplicacion: la misma
 * bateria que el almacen en memoria ({@link ContratoRepositorioPreparacionesTest}) y lo propio de la base.
 */
@SpringBootTest
abstract class PreparacionesPostgresTest extends ContratoRepositorioPreparacionesTest {

    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private PlatformTransactionManager transacciones;

    /** Esquema dueño de la tabla bajo prueba. */
    protected abstract String esquema();

    private RepositorioPreparacionesJdbc repositorio;

    @Override
    protected RepositorioPreparaciones almacen() {
        if (repositorio == null) {
            repositorio = new RepositorioPreparacionesJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones), esquema());
        }
        return repositorio;
    }

    private Preparacion nueva(UUID negocio, String hash) {
        return new Preparacion(hash, negocio, "actor-a", "registrar_venta", "hash-del-comando", "{\"a\":1}",
                "Resumen.", "turno-1", AHORA.plusSeconds(300), null);
    }

    @Test
    @DisplayName("marcarUsada se confirma aunque la transaccion de fuera se deshaga: es su propia transaccion")
    void marcarUsadaSobreviveAlRollbackDeFuera() {
        var negocio = UUID.randomUUID();
        var hash = TokenOpaco.hash(TokenOpaco.nuevo());
        almacen().guardar(nueva(negocio, hash));

        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            assertThat(almacen().marcarUsada(negocio, hash, AHORA)).isTrue();
            estado.setRollbackOnly();
        });

        assertThat(almacen().porHash(negocio, hash).orElseThrow().usadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("la base impone el vaciado: una preparacion usada no puede conservar el comando (SC-10)")
    void laBaseImponeElVaciado() {
        var negocio = UUID.randomUUID();
        var hash = TokenOpaco.hash(TokenOpaco.nuevo());
        almacen().guardar(nueva(negocio, hash));

        assertThatThrownBy(() -> almacen().guardar(new Preparacion(TokenOpaco.hash(TokenOpaco.nuevo()), negocio, "a",
                "t", "h", "{\"a\":1}", "r", null, AHORA.plusSeconds(60), AHORA)))
                .hasMessageContaining("ck_preparacion_usada_sin_comando");
    }

    @Test
    @DisplayName("el comando se guarda con el texto exacto: el hash del comando se calcula sobre el")
    void guardaElTextoExacto() {
        var negocio = UUID.randomUUID();
        var hash = TokenOpaco.hash(TokenOpaco.nuevo());
        var exacto = "{ \"z\" : 1,\n \"a\":[1, 2] }";
        almacen().guardar(new Preparacion(hash, negocio, "a", "t", "h", exacto, "r", null, AHORA.plusSeconds(60), null));

        assertThat(almacen().porHash(negocio, hash).orElseThrow().comando()).isEqualTo(exacto);
    }

    @Test
    @DisplayName("un esquema que no es un identificador se rechaza al armar el repositorio")
    void esquemaInvalido() {
        assertThatThrownBy(() -> new RepositorioPreparacionesJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones),
                "ventas; drop table x")).isInstanceOf(IllegalArgumentException.class);
    }
}
