package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * IDN-1.1: tablas de Identidad. Cuenta, código de acceso y sesión no llevan negocio (SI-11), así que las reglas
 * viven en restricciones de la propia base: unicidad del teléfono, formatos y vigencias coherentes. Cada prueba corre
 * en una transacción que se deshace.
 */
@SpringBootTest
class EsquemaIdentidadPostgresTest {

    private static final String HASH_DE_64 = "a".repeat(64);
    private static final String SAL_DE_32 = "b".repeat(32);

    private static String telefono() {
        return "+52222" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
    }

    private static void insertarCuenta(SesionSql sesion, UUID id, String telefono) throws Exception {
        sesion.ejecutar("insert into identidad.cuenta (id, nombre, telefono, estado, creada_en) "
                + "values (?, 'Ana Prueba', ?, 'ACTIVA', now())", id, telefono);
    }

    @Test
    @DisplayName("una cuenta, su código y su sesión se guardan y se leen con el rol de la aplicación")
    void flujoFeliz_cuentaCodigoYSesion() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var cuenta = UUID.randomUUID();
            var tel = telefono();

            insertarCuenta(sesion, cuenta, tel);
            sesion.ejecutar("insert into identidad.codigo_acceso (id, telefono, sal, hash, intentos, expira_en, creado_en) "
                    + "values (?, ?, ?, ?, 0, now() + interval '5 minutes', now())", UUID.randomUUID(), tel, SAL_DE_32, HASH_DE_64);
            sesion.ejecutar("insert into identidad.sesion (token_hash, cuenta_id, dispositivo, ip_truncada, creada_en, "
                    + "ultimo_uso_en, expira_en, expira_absoluta_en) values (?, ?, 'Chrome en Windows', '189.203.10.0', "
                    + "now(), now(), now() + interval '8 hours', now() + interval '30 days')", HASH_DE_64, cuenta);

            assertThat(sesion.consultar("select count(*) from identidad.sesion where cuenta_id = ?", Long.class, cuenta)).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("AC-01: dos cuentas no pueden tener el mismo teléfono")
    void cuenta_telefonoDuplicado_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var tel = telefono();
            insertarCuenta(sesion, UUID.randomUUID(), tel);

            assertThat(sesion.intentar("insert into identidad.cuenta (id, nombre, telefono, estado, creada_en) "
                    + "values (?, 'Otra Persona', ?, 'ACTIVA', now())", UUID.randomUUID(), tel)).isEqualTo("23505");
        }
    }

    @Test
    @DisplayName("AC-02: el teléfono de una cuenta debe ser E.164 mexicano")
    void cuenta_telefonoFueraDeFormato_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            for (var invalido : new String[] {"2221110001", "+521234", "+1 2221110001", "+52abcdefghij", ""}) {
                assertThat(sesion.intentar("insert into identidad.cuenta (id, nombre, telefono, estado, creada_en) "
                        + "values (?, 'Ana Prueba', ?, 'ACTIVA', now())", UUID.randomUUID(), invalido))
                        .as("teléfono «%s»", invalido).isEqualTo("23514");
            }
        }
    }

    @Test
    @DisplayName("AC-04: el estado de la cuenta es ACTIVA, SUSPENDIDA o ANONIMIZADA, y el nombre no está vacío")
    void cuenta_estadoONombreInvalido_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(sesion.intentar("insert into identidad.cuenta (id, nombre, telefono, estado, creada_en) "
                    + "values (?, 'Ana Prueba', ?, 'BORRADA', now())", UUID.randomUUID(), telefono())).isEqualTo("23514");
            assertThat(sesion.intentar("insert into identidad.cuenta (id, nombre, telefono, estado, creada_en) "
                    + "values (?, '   ', ?, 'ACTIVA', now())", UUID.randomUUID(), telefono())).isEqualTo("23514");
        }
    }

    @Test
    @DisplayName("AS-01: del código solo se guarda el hash con sal, con formato válido, intentos no negativos y vencimiento posterior")
    void codigo_valoresInvalidos_seRechazan() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var insertar = "insert into identidad.codigo_acceso (id, telefono, sal, hash, intentos, expira_en, creado_en) values (?, ?, ?, ?, ?, ";
            assertThat(sesion.intentar(insertar + "now() + interval '5 minutes', now())", UUID.randomUUID(), telefono(), SAL_DE_32, "123456", 0))
                    .as("un código en claro no cabe como hash").isEqualTo("23514");
            assertThat(sesion.intentar(insertar + "now() + interval '5 minutes', now())", UUID.randomUUID(), telefono(), "xyz", HASH_DE_64, 0))
                    .as("sal inválida").isEqualTo("23514");
            assertThat(sesion.intentar(insertar + "now() + interval '5 minutes', now())", UUID.randomUUID(), telefono(), SAL_DE_32, HASH_DE_64, -1))
                    .as("intentos negativos").isEqualTo("23514");
            assertThat(sesion.intentar(insertar + "now() - interval '1 minute', now())", UUID.randomUUID(), telefono(), SAL_DE_32, HASH_DE_64, 0))
                    .as("vence antes de emitirse").isEqualTo("23514");
        }
    }

    @Test
    @DisplayName("AS-05: una sesión necesita su cuenta, un hash de token válido y vigencias coherentes")
    void sesion_valoresInvalidos_seRechazan() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var cuenta = UUID.randomUUID();
            insertarCuenta(sesion, cuenta, telefono());
            var insertar = "insert into identidad.sesion (token_hash, cuenta_id, creada_en, ultimo_uso_en, expira_en, expira_absoluta_en) values (?, ?, now(), now(), ";

            assertThat(sesion.intentar(insertar + "now() + interval '8 hours', now() + interval '30 days')", HASH_DE_64, UUID.randomUUID()))
                    .as("cuenta inexistente").isEqualTo("23503");
            assertThat(sesion.intentar(insertar + "now() + interval '8 hours', now() + interval '30 days')", "no-es-un-hash", cuenta))
                    .as("el token en claro no cabe como hash").isEqualTo("23514");
            assertThat(sesion.intentar(insertar + "now() + interval '31 days', now() + interval '30 days')", "c".repeat(64), cuenta))
                    .as("la inactividad no puede pasar del tope absoluto").isEqualTo("23514");
        }
    }

    @Test
    @DisplayName("AS-05: dos sesiones no pueden compartir el mismo hash de token")
    void sesion_hashDuplicado_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var cuenta = UUID.randomUUID();
            insertarCuenta(sesion, cuenta, telefono());
            var insertar = "insert into identidad.sesion (token_hash, cuenta_id, creada_en, ultimo_uso_en, expira_en, expira_absoluta_en) "
                    + "values (?, ?, now(), now(), now() + interval '8 hours', now() + interval '30 days')";
            sesion.ejecutar(insertar, "d".repeat(64), cuenta);

            assertThat(sesion.intentar(insertar, "d".repeat(64), cuenta)).isEqualTo("23505");
        }
    }
}
