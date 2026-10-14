package mx.negocio360.persistencia;

import static mx.negocio360.persistencia.FilasClientes.insertarAviso;
import static mx.negocio360.persistencia.FilasClientes.sembrarNegocio;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * SG-03 y SC-04: la seguridad por fila se prueba tabla por tabla con dos negocios, con el rol de la aplicación (que
 * no es superusuario). Cubre lectura, escritura entre negocios, el cambio de negocio de una fila, la fuga del
 * negocio entre transacciones de una misma conexión y los valores que no son UUID. Cada prueba deshace lo que hace.
 */
@SpringBootTest
class AislamientoPorFilaPostgresTest {

    /** Tablas de Clientes con negocio_id. */
    private static final String[] TABLAS = {"aviso_privacidad", "cliente", "consentimiento", "solicitud_arco",
            "resumen_cliente", "compra_aplicada", "acceso_cliente", "preparacion"};

    static Stream<String> tablas() {
        return Stream.of(TABLAS);
    }

    /** Tablas con permiso de UPDATE para la aplicación. */
    static Stream<String> tablasConActualizacion() {
        return Stream.of("aviso_privacidad", "cliente", "solicitud_arco", "resumen_cliente", "compra_aplicada",
                "preparacion");
    }

    static Stream<String> todasLasTablasDeClientes() {
        return Stream.concat(Stream.of(TABLAS), Stream.of("evento_procesado"));
    }

    /** Registros que cuelgan de un cliente; %1$s es el negocio y %2$s el cliente. */
    static Stream<Arguments> registrosDeUnCliente() {
        return Stream.of(
                Arguments.of("solicitud_arco", "insert into clientes.solicitud_arco (id, negocio_id, cliente_id, derecho, "
                        + "recibida_en, medio_recepcion, responder_antes_de, estado, creado_por) values (gen_random_uuid(), "
                        + "'%1$s', '%2$s', 'ACCESO', current_date, 'PRESENCIAL', current_date + 20, 'RECIBIDA', 'prueba')"),
                Arguments.of("compra_aplicada", "insert into clientes.compra_aplicada (id, negocio_id, cliente_id, venta_id, "
                        + "monto, ocurrido_en, estado) values (gen_random_uuid(), '%1$s', '%2$s', gen_random_uuid(), 1, "
                        + "now(), 'APLICADA')"),
                Arguments.of("acceso_cliente", "insert into clientes.acceso_cliente (id, negocio_id, cliente_id, actor, "
                        + "operacion, ocurrido_en) values (gen_random_uuid(), '%1$s', '%2$s', 'prueba', 'DETALLE', now())"),
                Arguments.of("resumen_cliente",
                        "insert into clientes.resumen_cliente (cliente_id, negocio_id) values ('%2$s', '%1$s')"));
    }

    private static long contar(SesionSql sesion, String tabla) throws Exception {
        return sesion.consultar("select count(*) from clientes." + tabla, Long.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablas")
    @DisplayName("SG-03: cada negocio ve solo sus filas, y sin negocio en el contexto no ve ninguna")
    void lectura_cadaNegocioVeSoloLoSuyo(String tabla) throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var a = UUID.randomUUID();
            var b = UUID.randomUUID();
            sembrarNegocio(sesion.enNegocio(a), a);
            sembrarNegocio(sesion.enNegocio(b), b);

            assertThat(contar(sesion.enNegocio(a), tabla)).as("negocio A").isEqualTo(1L);
            assertThat(contar(sesion.enNegocio(b), tabla)).as("negocio B").isEqualTo(1L);
            assertThat(contar(sesion.sinNegocio(), tabla)).as("sin negocio").isZero();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablas")
    @DisplayName("SG-03: desde un negocio no se modifica ni se borra ninguna fila de otro")
    void escritura_noAfectaAlOtroNegocio(String tabla) throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var a = UUID.randomUUID();
            var b = UUID.randomUUID();
            sembrarNegocio(sesion.enNegocio(a), a);
            sembrarNegocio(sesion.enNegocio(b), b);
            sesion.enNegocio(b);

            // Con el permiso o sin él, la sentencia no toca filas de A: falla por permiso o afecta 0 filas.
            var actualizar = sesion.intentar("update clientes." + tabla + " set negocio_id = negocio_id where negocio_id = ?", a);
            var borrar = sesion.intentar("delete from clientes." + tabla + " where negocio_id = ?", a);

            assertThat(actualizar).isIn(null, "42501", "23001");
            assertThat(borrar).isIn(null, "42501", "23001");
            assertThat(contar(sesion.enNegocio(a), tabla)).as("A conserva su fila").isEqualTo(1L);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablasConActualizacion")
    @DisplayName("SC-01: una fila no se puede mover a otro negocio con un UPDATE")
    void escritura_noSePuedeCambiarElNegocioDeUnaFila(String tabla) throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var a = UUID.randomUUID();
            var b = UUID.randomUUID();
            sembrarNegocio(sesion.enNegocio(a), a);
            sesion.enNegocio(a);

            assertThat(sesion.intentar("update clientes." + tabla + " set negocio_id = ?", b)).isEqualTo("42501");
            assertThat(contar(sesion, tabla)).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("SC-04: el negocio de una transacción no se filtra a la siguiente transacción de la misma conexión")
    void negocio_noSeFiltraEntreTransacciones() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            sesion.enNegocio(negocio);
            insertarAviso(sesion, negocio, 1);
            assertThat(contar(sesion, "aviso_privacidad")).as("dentro de la transacción").isEqualTo(1L);

            sesion.confirmar();

            assertThat(sesion.consultar("select nullif(current_setting('app.negocio_id', true), '')", String.class))
                    .as("el valor se restablece al confirmar").isNull();
            assertThat(contar(sesion, "aviso_privacidad")).as("la transacción siguiente no ve nada").isZero();
            assertThat(contar(sesion.enNegocio(negocio), "aviso_privacidad")).as("con su negocio sí").isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("un negocio que no es un UUID válido hace fallar la consulta en vez de devolver filas")
    void negocio_noUuid_fallaCerrado() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            sesion.consultar("select set_config('app.negocio_id', 'no-es-un-uuid', true)", String.class);

            assertThat(sesion.intentar("select count(*) from clientes.cliente")).isEqualTo("22P02");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasLasTablasDeClientes")
    @DisplayName("la aplicación no puede vaciar ninguna tabla de Clientes (TRUNCATE ignora la seguridad por fila)")
    void truncate_estaNegadoParaLaAplicacion(String tabla) throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(sesion.intentar("truncate clientes." + tabla)).isEqualTo("42501");
        }
    }

    @Test
    @DisplayName("P-03: la bitácora de accesos solo se inserta; la aplicación no puede editarla ni borrarla")
    void accesoCliente_soloInsercion_paraLaAplicacion() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            sembrarNegocio(sesion.enNegocio(negocio), negocio);

            assertThat(sesion.intentar("update clientes.acceso_cliente set actor = 'otro'")).isEqualTo("42501");
            assertThat(sesion.intentar("delete from clientes.acceso_cliente")).isEqualTo("42501");
            assertThat(contar(sesion, "acceso_cliente")).isEqualTo(1L);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("registrosDeUnCliente")
    @DisplayName("un registro de un negocio no puede colgarse de un cliente de otro negocio")
    void llavesForaneas_clienteDeOtroNegocio_seRechaza(String tabla, String plantilla) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var a = UUID.randomUUID();
            var b = UUID.randomUUID();
            insertarAviso(sesion, a, 1);
            insertarAviso(sesion, b, 1);
            var clienteDeA = FilasClientes.insertarCliente(sesion, a, "Ana", FilasClientes.telefono(), "ACTIVO");

            // Primero el de otro negocio: resumen_cliente tiene llave primaria por cliente y el repetido fallaría por eso.
            assertThat(sesion.intentar(plantilla.formatted(b, clienteDeA))).as(tabla + " de otro negocio").isEqualTo("23503");
            assertThat(sesion.intentar(plantilla.formatted(a, clienteDeA))).as(tabla + " del mismo negocio").isNull();
        }
    }
}
