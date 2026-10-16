package mx.negocio360;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Estructura de modulos con Spring Modulith: dependencias permitidas entre modulos y ausencia de ciclos. No
 * verifica por si sola la idempotencia (R3) ni el versionado de eventos (R5): eso lo cubren las pruebas de contrato.
 */
class EstructuraModulosTest {

    private static final ApplicationModules MODULOS = ApplicationModules.of(Negocio360Application.class);

    @Test
    @DisplayName("verify: la estructura de modulos no tiene violaciones")
    void verify_estructuraDeModulos_noTieneViolaciones() {
        MODULOS.verify();
    }

    @Test
    @DisplayName("la plataforma incluye los modulos clientes, compartido, identidad, inventario, negocios, ventas, caja, canal y restaurantes")
    void modulos_laPlataforma_incluyenLosModulos() {
        var nombres = MODULOS.stream().map(ApplicationModule::getName).collect(Collectors.toSet());

        assertThat(nombres).contains("clientes", "compartido", "identidad", "inventario", "negocios", "ventas", "caja",
                "canal", "restaurantes");
    }

    /** D-160 y R4: Restaurantes es vertical, asi que ningun otro modulo puede depender de el. */
    @Test
    @DisplayName("R4: ningun modulo depende de restaurantes")
    void restaurantes_nadieDependeDeEl() {
        var restaurantes = MODULOS.getModuleByName("restaurantes").orElseThrow();

        var dependientes = MODULOS.stream().filter(m -> !m.getName().equals("restaurantes"))
                .filter(m -> m.getDirectDependencies(MODULOS).uniqueModules().anyMatch(restaurantes::equals))
                .map(ApplicationModule::getName)
                .toList();

        assertThat(dependientes).isEmpty();
    }

    /** D-160: Restaurantes solo puede depender de la plataforma y de Inventario (D-09, recetas contra InventarioApi). */
    @Test
    @DisplayName("D-160: restaurantes solo depende de compartido e inventario")
    void restaurantes_soloDependeDeCompartidoEInventario() {
        var restaurantes = MODULOS.getModuleByName("restaurantes").orElseThrow();

        var dependencias = restaurantes.getDirectDependencies(MODULOS).uniqueModules().map(ApplicationModule::getName)
                .collect(Collectors.toSet());

        assertThat(dependencias).isSubsetOf("compartido", "inventario");
    }
}
