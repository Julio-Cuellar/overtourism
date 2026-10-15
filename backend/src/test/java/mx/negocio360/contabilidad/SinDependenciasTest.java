package mx.negocio360.contabilidad;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.stream.Collectors;
import mx.negocio360.Negocio360Application;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * E-07: Contabilidad no depende de ningun otro modulo: no llama a sus APIs ni usa sus clases. Solo puede usar
 * {@code compartido}, la biblioteca estandar y el marco (R1, R2, R6). Leer tablas ajenas lo impiden ademas los roles de
 * Postgres (rol_contabilidad no tiene permisos sobre otros esquemas).
 */
class SinDependenciasTest {

    @Test
    @DisplayName("E-07: las clases de Contabilidad solo dependen de compartido, de Java y del marco")
    void contabilidad_soloDependeDeCompartido() {
        var clases = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("mx.negocio360.contabilidad");

        classes().that().resideInAPackage("mx.negocio360.contabilidad..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("mx.negocio360.contabilidad..",
                        "mx.negocio360.compartido..", "java..", "javax..", "jakarta..", "org.springframework..",
                        "com.fasterxml.jackson..", "org.slf4j..")
                .check(clases);
    }

    @Test
    @DisplayName("E-07: para Spring Modulith, Contabilidad no depende de ningun otro modulo")
    void contabilidad_sinModulosVecinos() {
        var modulos = ApplicationModules.of(Negocio360Application.class);
        var contabilidad = modulos.getModuleByName("contabilidad").orElseThrow();

        var dependencias = contabilidad.getBootstrapDependencies(modulos).map(m -> m.getName())
                .collect(Collectors.toSet());

        assertThat(dependencias).isSubsetOf("compartido");
    }
}
