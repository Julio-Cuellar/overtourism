package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * V023 · D-84, D-86: la tabla de pre-registros. Guarda datos personales solo mientras vale el codigo, asi que sus reglas
 * (un pre-registro por correo, formatos, hash del codigo, vigencia coherente, negocio completo) viven en restricciones de
 * la propia base. Cada prueba corre en una transaccion que se deshace.
 */
@SpringBootTest
class EsquemaPreregistroPostgresTest {

    private static final String HASH_DE_64 = "a".repeat(64);
    private static final String SAL_DE_32 = "b".repeat(32);
    private static final String INSERTAR = "insert into identidad.preregistro (id, correo, telefono, nombre, apellidos, "
            + "aviso_version, aviso_aceptado_en, negocio_nombre, negocio_giro, codigo_sal, codigo_hash, intentos, "
            + "expira_en, creado_en) values (?, ?, ?, ?, ?, ?, now(), ?, ?, ?, ?, ?, ";
    private static final String VIGENTE = "now() + interval '15 minutes', now())";

    private static String correo() {
        return "pre" + ThreadLocalRandom.current().nextInt(100_000_000) + "@example.com";
    }

    private static String telefono() {
        return "+52222" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
    }

    private static String insertar(SesionSql sesion, String correo, String telefono, String nombre, String apellidos,
                                   int aviso, String negocioNombre, String negocioGiro, String sal, String hash,
                                   int intentos, String vigencia) {
        try {
            return sesion.intentar(INSERTAR + vigencia, UUID.randomUUID(), correo, telefono, nombre, apellidos, aviso,
                    negocioNombre, negocioGiro, sal, hash, intentos);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String valido(SesionSql sesion, String correo) {
        return insertar(sesion, correo, telefono(), "Ana", "López", 1, null, null, SAL_DE_32, HASH_DE_64, 0, VIGENTE);
    }

    @Test
    @DisplayName("un pre-registro con y sin negocio se guarda y se lee con el rol de la aplicacion")
    void flujoFeliz_conYSinNegocio() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var sin = correo();
            var con = correo();

            assertThat(valido(sesion, sin)).isNull();
            assertThat(insertar(sesion, con, telefono(), "Ana", "López", 1, "Fonda Doña Chela", "RESTAURANTE", SAL_DE_32,
                    HASH_DE_64, 0, VIGENTE)).isNull();

            assertThat(sesion.consultar("select count(*) from identidad.preregistro where correo in (?, ?)", Long.class, sin,
                    con)).isEqualTo(2L);
        }
    }

    @Test
    @DisplayName("hay un solo pre-registro por correo")
    void correo_duplicado_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var correo = correo();
            assertThat(valido(sesion, correo)).isNull();

            assertThat(valido(sesion, correo)).isEqualTo("23505");
        }
    }

    @Test
    @DisplayName("el correo va en minusculas y con forma de correo; el celular en E.164 de Mexico")
    void correoYCelular_conFormatoInvalido_seRechazan() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(valido(sesion, "ANA@EXAMPLE.COM")).as("mayúsculas").isEqualTo("23514");
            assertThat(valido(sesion, "sin-arroba")).as("sin arroba").isEqualTo("23514");
            assertThat(valido(sesion, "con espacio@example.com")).as("con espacio").isEqualTo("23514");
            assertThat(insertar(sesion, correo(), "2221110001", "Ana", "López", 1, null, null, SAL_DE_32, HASH_DE_64, 0,
                    VIGENTE)).as("celular sin +52").isEqualTo("23514");
        }
    }

    @Test
    @DisplayName("del codigo solo cabe un hash con sal valido y los intentos no son negativos")
    void codigo_conFormatoInvalido_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(insertar(sesion, correo(), telefono(), "Ana", "López", 1, null, null, SAL_DE_32, "123456", 0, VIGENTE))
                    .as("un código en claro no cabe como hash").isEqualTo("23514");
            assertThat(insertar(sesion, correo(), telefono(), "Ana", "López", 1, null, null, "xyz", HASH_DE_64, 0, VIGENTE))
                    .as("sal inválida").isEqualTo("23514");
            assertThat(insertar(sesion, correo(), telefono(), "Ana", "López", 1, null, null, SAL_DE_32, HASH_DE_64, -1,
                    VIGENTE)).as("intentos negativos").isEqualTo("23514");
        }
    }

    @Test
    @DisplayName("el nombre y los apellidos no pueden ir vacios y el aviso tiene una version positiva")
    void datosDeLaPersona_vacios_seRechazan() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(insertar(sesion, correo(), telefono(), "   ", "López", 1, null, null, SAL_DE_32, HASH_DE_64, 0, VIGENTE))
                    .as("nombre vacío").isEqualTo("23514");
            assertThat(insertar(sesion, correo(), telefono(), "Ana", "   ", 1, null, null, SAL_DE_32, HASH_DE_64, 0, VIGENTE))
                    .as("apellidos vacíos").isEqualTo("23514");
            assertThat(insertar(sesion, correo(), telefono(), "Ana", "López", 0, null, null, SAL_DE_32, HASH_DE_64, 0, VIGENTE))
                    .as("aviso sin versión").isEqualTo("23514");
        }
    }

    @Test
    @DisplayName("el negocio va completo o no va: su nombre y su giro juntos, y el giro es uno de los conocidos")
    void negocio_incompletoOGiroDesconocido_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(insertar(sesion, correo(), telefono(), "Ana", "López", 1, "Fonda", null, SAL_DE_32, HASH_DE_64, 0,
                    VIGENTE)).as("nombre sin giro").isEqualTo("23514");
            assertThat(insertar(sesion, correo(), telefono(), "Ana", "López", 1, null, "MUSEO", SAL_DE_32, HASH_DE_64, 0,
                    VIGENTE)).as("giro sin nombre").isEqualTo("23514");
            assertThat(insertar(sesion, correo(), telefono(), "Ana", "López", 1, "Fonda", "FARMACIA", SAL_DE_32, HASH_DE_64,
                    0, VIGENTE)).as("giro desconocido").isEqualTo("23514");
        }
    }

    @Test
    @DisplayName("el pre-registro vence despues de crearse")
    void vigencia_queVenceAntesDeCrearse_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(insertar(sesion, correo(), telefono(), "Ana", "López", 1, null, null, SAL_DE_32, HASH_DE_64, 0,
                    "now() - interval '1 minute', now())")).isEqualTo("23514");
        }
    }

    @Test
    @DisplayName("el rol de la aplicacion puede leer, actualizar y borrar pre-registros, y el indice de vencimiento existe")
    void permisos_yIndice() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var correo = correo();
            assertThat(valido(sesion, correo)).isNull();

            assertThat(sesion.intentar("update identidad.preregistro set intentos = 1 where correo = ?", correo)).isNull();
            assertThat(sesion.intentar("delete from identidad.preregistro where correo = ?", correo)).isNull();
            assertThat(sesion.consultar("select count(*) from pg_indexes where schemaname = 'identidad' "
                    + "and indexname = 'ix_preregistro_expira'", Long.class)).isEqualTo(1L);
        }
    }
}
