package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Las migraciones de Flyway corrieron contra un PostgreSQL real al arrancar la aplicación: esquemas por módulo, sus
 * tablas, roles y permisos. Corre con la aplicación completa y el rol sin privilegios que usa de verdad.
 */
@SpringBootTest
class EsquemaPostgresTest {

    static Stream<Arguments> tablas() {
        return Stream.of(
                Arguments.of("plataforma", "event_publication"),
                Arguments.of("identidad", "cuenta"), Arguments.of("identidad", "codigo_acceso"),
                Arguments.of("identidad", "sesion"),
                Arguments.of("clientes", "aviso_privacidad"), Arguments.of("clientes", "cliente"),
                Arguments.of("clientes", "consentimiento"), Arguments.of("clientes", "solicitud_arco"),
                Arguments.of("clientes", "resumen_cliente"), Arguments.of("clientes", "compra_aplicada"),
                Arguments.of("clientes", "acceso_cliente"), Arguments.of("clientes", "preparacion"),
                Arguments.of("clientes", "evento_procesado"));
    }

    @Test
    @DisplayName("Flyway aplicó todas las migraciones y ninguna falló")
    void flyway_aplicoTodasLasMigracionesSinErrores() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            assertThat(sesion.consultar("select count(*) from plataforma.flyway_schema_history where success", Long.class))
                    .isGreaterThanOrEqualTo(4L);
            assertThat(sesion.consultar("select count(*) from plataforma.flyway_schema_history where not success", Long.class))
                    .isZero();
        }
    }

    @Test
    @DisplayName("cada módulo tiene su propio esquema")
    void esquemas_existenUnoPorModulo() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var esquemas = sesion.columna("select schema_name from information_schema.schemata", String.class);

            assertThat(esquemas).contains("plataforma", "identidad", "clientes");
        }
    }

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("tablas")
    @DisplayName("cada tabla del diseño existe en el esquema de su módulo")
    void tablas_existen(String esquema, String tabla) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            assertThat(sesion.consultar(
                    "select count(*) from information_schema.tables where table_schema = ? and table_name = ?",
                    Long.class, esquema, tabla)).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("la aplicación usa un rol que no es superusuario ni puede saltarse la seguridad por fila")
    void rolDeLaAplicacion_noEsSuperusuarioNiSaltaLaSeguridadPorFila() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(sesion.consultar("select rolsuper from pg_roles where rolname = current_user", Boolean.class)).isFalse();
            assertThat(sesion.consultar("select rolbypassrls from pg_roles where rolname = current_user", Boolean.class)).isFalse();
        }
    }

    @Test
    @DisplayName("SI-11: Identidad y Clientes tienen roles distintos y ninguno ve las tablas del otro")
    void roles_cadaModuloSoloVeSusTablas() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            assertThat(sesion.consultar("select has_table_privilege('rol_identidad', 'identidad.cuenta', 'SELECT')", Boolean.class)).isTrue();
            assertThat(sesion.consultar("select has_table_privilege('rol_identidad', 'clientes.cliente', 'SELECT')", Boolean.class)).isFalse();
            assertThat(sesion.consultar("select has_table_privilege('rol_clientes', 'clientes.cliente', 'SELECT')", Boolean.class)).isTrue();
            assertThat(sesion.consultar("select has_table_privilege('rol_clientes', 'identidad.cuenta', 'SELECT')", Boolean.class)).isFalse();
            assertThat(sesion.consultar("select pg_has_role('negocio360_app', 'rol_identidad', 'member')", Boolean.class)).isTrue();
            assertThat(sesion.consultar("select pg_has_role('negocio360_app', 'rol_clientes', 'member')", Boolean.class)).isTrue();
        }
    }

    @Test
    @DisplayName("la aplicación no puede crear ni borrar tablas, ni tocar el historial de migraciones")
    void rolDeLaAplicacion_noPuedeCambiarElEsquema() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(sesion.intentar("create table clientes.intruso (id int)")).isEqualTo("42501");
            assertThat(sesion.intentar("drop table clientes.cliente")).isEqualTo("42501");
            assertThat(sesion.intentar("select * from plataforma.flyway_schema_history")).isEqualTo("42501");
        }
    }

    @Test
    @DisplayName("el rol de la aplicación tiene tiempos máximos: una consulta o una transacción colgada no retiene bloqueos")
    void rolDeLaAplicacion_tieneTiemposMaximos() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(sesion.consultar("show statement_timeout", String.class)).isEqualTo("30s");
            assertThat(sesion.consultar("show lock_timeout", String.class)).isEqualTo("10s");
            assertThat(sesion.consultar("show idle_in_transaction_session_timeout", String.class)).isEqualTo("1min");
        }
    }

    @Test
    @DisplayName("negocio_actual() es segura para planes paralelos; si no, desactiva el paralelismo en toda consulta con política")
    void negocioActual_esParallelSafe() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            assertThat(sesion.consultar("select proparallel from pg_proc where proname = 'negocio_actual'", String.class))
                    .isEqualTo("s");
        }
    }

    static Stream<String> indices() {
        return Stream.of("clientes.ix_cliente_creado", "clientes.ix_cliente_nombre_cursor", "clientes.ix_consentimiento_vigente",
                "clientes.ix_arco_cliente", "clientes.ix_resumen_ultima_compra", "clientes.ix_compra_negocio_fecha",
                "clientes.ix_compra_cliente", "clientes.ix_acceso_cliente", "identidad.ix_codigo_expira",
                "identidad.ix_sesion_expira");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("indices")
    @DisplayName("existe el índice que usan las consultas del diseño y las purgas de caducados")
    void indices_existen(String indice) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            assertThat(sesion.consultar("select to_regclass(?) is not null", Boolean.class, indice)).isTrue();
        }
    }

    @Test
    @DisplayName("no hay un índice de trigramas que la seguridad por fila volvería inútil (los operadores no son leakproof)")
    void indices_sinTrigramasInutiles() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            assertThat(sesion.consultar("select to_regclass('clientes.ix_cliente_nombre_busqueda') is null", Boolean.class)).isTrue();
            assertThat(sesion.consultar("select count(*) from pg_extension where extname = 'pg_trgm'", Long.class)).isZero();
        }
    }

    @Test
    @DisplayName("el script que crea el rol de la aplicación en Docker lo deja sin privilegios y con finales de línea de Unix")
    void scriptDeRoles_creaUnRolSinPrivilegios() throws Exception {
        var script = java.nio.file.Files.readString(java.nio.file.Path.of("docker/postgres/01-roles.sh"));

        assertThat(script).contains("NOSUPERUSER", "NOCREATEDB", "NOCREATEROLE", "NOBYPASSRLS").doesNotContain("\r");
    }

    @Test
    @DisplayName("la lista de esquemas de la aplicación no incluye ninguno por descuido")
    void esquemas_soloLosDelDiseno() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            List<String> propios = sesion.columna(
                    "select schema_name from information_schema.schemata where schema_name not like 'pg\\_%' "
                            + "and schema_name not in ('information_schema', 'public')", String.class);

            assertThat(propios).containsExactlyInAnyOrder("plataforma", "identidad", "clientes", "contabilidad");
        }
    }
}
