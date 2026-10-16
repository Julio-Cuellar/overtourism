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
    @DisplayName("la plataforma incluye los modulos clientes, compartido, identidad, inventario, negocios, ventas y caja")
    void modulos_laPlataforma_incluyenLosModulos() {
        var nombres = MODULOS.stream().map(ApplicationModule::getName).collect(Collectors.toSet());

        assertThat(nombres).contains("clientes", "compartido", "identidad", "inventario", "negocios", "ventas", "caja");
    }
}
