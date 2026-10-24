package mx.negocio360.persistencia;

import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioPreparaciones;
import mx.negocio360.inventario.infraestructura.ContratoPreparacionesDeInventarioTest;
import mx.negocio360.inventario.infraestructura.PreparacionesJdbc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.inventario.aplicacion.Preparacion;
import static org.assertj.core.api.Assertions.assertThat;

/** {@code inventario.preparacion} cumple el contrato de las preparaciones y confirma el uso aparte de la transaccion de fuera. */
@SpringBootTest
class PreparacionesDeInventarioPostgresTest extends ContratoPreparacionesDeInventarioTest {

    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private PlatformTransactionManager transacciones;

    private PreparacionesJdbc almacen;

    @Override
    protected RepositorioPreparaciones almacen() {
        if (almacen == null) {
            almacen = new PreparacionesJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones));
        }
        return almacen;
    }

    @Test
    @DisplayName("marcarUsada se confirma aunque la transaccion de fuera se deshaga: es su propia transaccion")
    void marcarUsadaSobreviveAlRollbackDeFuera() {
        var negocio = UUID.randomUUID();
        var hash = TokenOpaco.hash(TokenOpaco.nuevo());
        almacen().guardar(new Preparacion(hash, negocio, "actor-a", "registrar_entrada", "h", "{}", "r",
                AHORA.plusSeconds(300), null));

        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            assertThat(almacen().marcarUsada(negocio, hash, AHORA)).isTrue();
            estado.setRollbackOnly();
        });

        assertThat(almacen().porHash(negocio, hash).orElseThrow().usadoEn()).isEqualTo(AHORA);
    }
}
