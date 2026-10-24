package mx.negocio360.compartido.almacen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * H-15: con el perfil {@code produccion} la aplicacion no arranca si Ventas, Caja o Inventario no tienen un almacen
 * persistente: sin esa guarda, la plataforma arrancaria sin ellos y sin avisar, o los dejaria en memoria perdiendo todo al
 * reiniciar.
 */
class AlmacenesObligatoriosEnProduccionTest {

    private static final String[] TODOS_EN_POSTGRES = {"ventas.almacen=postgres", "caja.almacen=postgres",
            "inventario.almacen=postgres"};

    private final ApplicationContextRunner produccion = new ApplicationContextRunner()
            .withUserConfiguration(AlmacenesObligatoriosEnProduccion.class)
            .withInitializer(c -> c.getEnvironment().setActiveProfiles("produccion"));

    @Test
    @DisplayName("con los tres almacenes en postgres la aplicacion arranca")
    void conLosTresEnPostgres_arranca() {
        produccion.withPropertyValues(TODOS_EN_POSTGRES).run(contexto -> assertThat(contexto).hasNotFailed());
    }

    @Test
    @DisplayName("el valor se lee sin importar mayusculas")
    void sinImportarMayusculas() {
        produccion.withPropertyValues("ventas.almacen=Postgres", "caja.almacen=POSTGRES", "inventario.almacen=postgres")
                .run(contexto -> assertThat(contexto).hasNotFailed());
    }

    @Test
    @DisplayName("si falta el almacen de Ventas no arranca, y el mensaje dice cual propiedad definir")
    void sinVentas_noArranca() {
        produccion.withPropertyValues("caja.almacen=postgres", "inventario.almacen=postgres")
                .run(contexto -> assertThat(contexto).hasFailed().getFailure().rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("ventas.almacen=postgres")
                        .hasMessageNotContaining("caja.almacen")
                        .hasMessageNotContaining("inventario.almacen"));
    }

    @Test
    @DisplayName("si falta el de Caja o el de Inventario tampoco arranca")
    void sinCajaOSinInventario_noArranca() {
        produccion.withPropertyValues("ventas.almacen=postgres", "inventario.almacen=postgres")
                .run(contexto -> assertThat(contexto).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("caja.almacen=postgres"));
        produccion.withPropertyValues("ventas.almacen=postgres", "caja.almacen=postgres")
                .run(contexto -> assertThat(contexto).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("inventario.almacen=postgres"));
    }

    @Test
    @DisplayName("sin ninguno, el mensaje enumera los tres")
    void sinNinguno_enumeraLosTres() {
        produccion.run(contexto -> assertThat(contexto).hasFailed().getFailure().rootCause()
                .hasMessageContaining("ventas.almacen=postgres").hasMessageContaining("caja.almacen=postgres")
                .hasMessageContaining("inventario.almacen=postgres"));
    }

    @Test
    @DisplayName("el almacen en memoria no se acepta en produccion: se perderia todo al reiniciar")
    void memoria_noArranca() {
        produccion.withPropertyValues("ventas.almacen=memoria", "caja.almacen=postgres", "inventario.almacen=postgres")
                .run(contexto -> assertThat(contexto).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("ventas.almacen").hasMessageContaining("memoria"));
    }

    @Test
    @DisplayName("un valor vacio o desconocido tampoco vale")
    void valorVacioODesconocido_noArranca() {
        produccion.withPropertyValues("ventas.almacen=", "caja.almacen=postgres", "inventario.almacen=postgres")
                .run(contexto -> assertThat(contexto).hasFailed());
        produccion.withPropertyValues("ventas.almacen=oracle", "caja.almacen=postgres", "inventario.almacen=postgres")
                .run(contexto -> assertThat(contexto).hasFailed());
    }

    @Test
    @DisplayName("fuera de produccion la guarda no existe: desarrollo puede usar memoria o no tener los modulos")
    void fueraDeProduccion_noAplica() {
        new ApplicationContextRunner().withUserConfiguration(AlmacenesObligatoriosEnProduccion.class)
                .run(contexto -> assertThat(contexto).hasNotFailed()
                        .doesNotHaveBean(AlmacenesObligatoriosEnProduccion.class));
        new ApplicationContextRunner().withUserConfiguration(AlmacenesObligatoriosEnProduccion.class)
                .withPropertyValues("ventas.almacen=memoria", "caja.almacen=memoria", "inventario.almacen=memoria")
                .run(contexto -> assertThat(contexto).hasNotFailed());
    }
}
