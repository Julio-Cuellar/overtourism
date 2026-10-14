package mx.negocio360.clientes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** I-01, I-02, I-07, I-08 y P-01: validacion y normalizacion de los datos del cliente. */
class DatosClienteTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 14);

    private static DatosCliente de(String nombre, String apellidos, String telefono, String correo,
                                   LocalDate nacimiento, String notas) {
        return DatosCliente.de(nombre, apellidos, telefono, correo, nacimiento, notas, HOY);
    }

    private static void assertCodigo(Runnable accion, CodigoError esperado) {
        assertThatThrownBy(accion::run)
                .isInstanceOfSatisfying(ClientesException.class, e -> assertThat(e.codigo()).isEqualTo(esperado));
    }

    @Test
    @DisplayName("normaliza todos los campos")
    void de_datosConFormatosLibres_losNormaliza() {
        var datos = de("  Ana  ", " Pérez ", "222 123 4567", "ANA@Correo.com", LocalDate.of(1990, 5, 1), " hola ");

        assertThat(datos.nombre()).isEqualTo("Ana");
        assertThat(datos.apellidos()).isEqualTo("Pérez");
        assertThat(datos.telefono()).isEqualTo("+522221234567");
        assertThat(datos.correo()).isEqualTo("ana@correo.com");
        assertThat(datos.fechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 1));
        assertThat(datos.notas()).isEqualTo("hola");
    }

    @Test
    @DisplayName("los opcionales vacios quedan nulos")
    void de_opcionalesEnBlanco_quedanNulos() {
        var datos = de("Ana", "  ", "", "   ", null, "");

        assertThat(datos.apellidos()).isNull();
        assertThat(datos.telefono()).isNull();
        assertThat(datos.correo()).isNull();
        assertThat(datos.notas()).isNull();
    }

    @Test
    @DisplayName("nombre vacio o de mas de 80 caracteres falla con NOMBRE_INVALIDO")
    void de_nombreInvalido_fallaConNombreInvalido() {
        assertCodigo(() -> de("   ", null, null, null, null, null), CodigoError.NOMBRE_INVALIDO);
        assertCodigo(() -> de(null, null, null, null, null, null), CodigoError.NOMBRE_INVALIDO);
        assertCodigo(() -> de("a".repeat(81), null, null, null, null, null), CodigoError.NOMBRE_INVALIDO);
        assertThat(de("a".repeat(80), null, null, null, null, null).nombre()).hasSize(80);
    }

    @Test
    @DisplayName("apellidos de mas de 80 caracteres fallan con NOMBRE_INVALIDO")
    void de_apellidosLargos_fallanConNombreInvalido() {
        assertCodigo(() -> de("Ana", "b".repeat(81), null, null, null, null), CodigoError.NOMBRE_INVALIDO);
        assertThat(de("Ana", "b".repeat(80), null, null, null, null).apellidos()).hasSize(80);
    }

    @Test
    @DisplayName("telefono invalido falla con TELEFONO_INVALIDO")
    void de_telefonoInvalido_fallaConTelefonoInvalido() {
        assertCodigo(() -> de("Ana", null, "123", null, null, null), CodigoError.TELEFONO_INVALIDO);
    }

    @Test
    @DisplayName("correo invalido falla con CORREO_INVALIDO")
    void de_correoInvalido_fallaConCorreoInvalido() {
        assertCodigo(() -> de("Ana", null, null, "ana@", null, null), CodigoError.CORREO_INVALIDO);
    }

    @Test
    @DisplayName("la fecha de nacimiento de hoy es valida y la de manana no")
    void de_fechaDeNacimiento_noPuedeSerFutura() {
        assertThat(de("Ana", null, null, null, HOY, null).fechaNacimiento()).isEqualTo(HOY);
        assertCodigo(() -> de("Ana", null, null, null, HOY.plusDays(1), null),
                CodigoError.FECHA_NACIMIENTO_INVALIDA);
    }

    @Test
    @DisplayName("500 caracteres de notas pasan y 501 fallan con NOTAS_MUY_LARGAS")
    void de_notas_tienenTopeDe500() {
        assertThat(de("Ana", null, null, null, null, "n".repeat(500)).notas()).hasSize(500);
        assertCodigo(() -> de("Ana", null, null, null, null, "n".repeat(501)), CodigoError.NOTAS_MUY_LARGAS);
    }

    @Test
    @DisplayName("nombre completo y texto de busqueda salen del nombre y los apellidos")
    void nombreCompleto_yNombreBusqueda_combinanNombreYApellidos() {
        var datos = de("José", "Pérez López", null, null, null, null);

        assertThat(datos.nombreCompleto()).isEqualTo("José Pérez López");
        assertThat(datos.nombreBusqueda()).isEqualTo("jose perez lopez");
    }

    @Test
    @DisplayName("sin apellidos el nombre completo es solo el nombre")
    void nombreCompleto_sinApellidos_esElNombre() {
        assertThat(de("Ana", null, null, null, null, null).nombreCompleto()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("el constructor tambien normaliza, para que nadie evite las reglas")
    void constructor_datosSinNormalizar_losNormaliza() {
        var datos = new DatosCliente("  Ana ", null, "222 123 4567", "ANA@x.com", null, null);

        assertThat(datos.nombre()).isEqualTo("Ana");
        assertThat(datos.telefono()).isEqualTo("+522221234567");
        assertThat(datos.correo()).isEqualTo("ana@x.com");
    }
}
