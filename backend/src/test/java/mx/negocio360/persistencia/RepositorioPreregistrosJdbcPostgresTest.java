package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import mx.negocio360.identidad.dominio.Preregistro;
import mx.negocio360.identidad.infraestructura.RepositorioPreregistrosJdbc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * D-86 · el repositorio JDBC de pre-registros contra el Postgres real de pruebas, con el rol de la aplicacion. Cumple el
 * mismo contrato que el de memoria. Cada operacion se confirma sola (no hay transaccion que deshacer), asi que cada
 * prueba usa sus propios correos. El contexto de Spring solo hace que corran las migraciones.
 */
@SpringBootTest
class RepositorioPreregistrosJdbcPostgresTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T16:00:00Z");
    private static final Duration VIGENCIA = Duration.ofMinutes(15);

    private RepositorioPreregistrosJdbc repositorio;

    @BeforeEach
    void conectar() {
        var origen = new DriverManagerDataSource(PostgresDePruebas.url(), PostgresDePruebas.usuarioApp(),
                PostgresDePruebas.contrasenaApp());
        repositorio = new RepositorioPreregistrosJdbc(JdbcClient.create(origen));
    }

    private static String correo() {
        return "jdbc" + ThreadLocalRandom.current().nextInt(100_000_000) + "@example.com";
    }

    private static Preregistro de(String correo, String negocioNombre, String negocioGiro, Instant creado) {
        return Preregistro.nuevo(UUID.randomUUID(), correo, "+522221110001", "Ana María", "López Pérez", 1, negocioNombre,
                negocioGiro, "123456", creado, VIGENCIA);
    }

    @Test
    @DisplayName("lo guardado se lee igual, con y sin negocio, y el codigo sigue coincidiendo")
    void guardar_yLeer_devuelveLoMismo() {
        var sin = de(correo(), null, null, AHORA);
        var con = de(correo(), "Fonda Doña Chela", "RESTAURANTE", AHORA.plusSeconds(5));

        repositorio.guardar(sin);
        repositorio.guardar(con);

        assertThat(repositorio.porCorreo(sin.correo())).contains(sin);
        assertThat(repositorio.porCorreo(con.correo())).contains(con);
        assertThat(repositorio.porCorreo(con.correo()).orElseThrow().coincide("123456")).isTrue();
        assertThat(repositorio.porCorreo(sin.correo()).orElseThrow().pideNegocio()).isFalse();
    }

    @Test
    @DisplayName("un correo desconocido no devuelve nada")
    void porCorreo_desconocido_vacio() {
        assertThat(repositorio.porCorreo(correo())).isEmpty();
    }

    @Test
    @DisplayName("guardar con el mismo correo reemplaza al anterior, incluidos sus intentos y su codigo")
    void guardar_mismoCorreo_reemplaza() {
        var correo = correo();
        var primero = de(correo, null, null, AHORA).conIntentoFallido().conIntentoFallido();
        var segundo = de(correo, "Museo", "MUSEO", AHORA.plusSeconds(60));
        repositorio.guardar(primero);

        repositorio.guardar(segundo);

        assertThat(repositorio.porCorreo(correo)).contains(segundo);
        assertThat(repositorio.porCorreo(correo).orElseThrow().intentos()).isZero();
        assertThat(repositorio.eliminar(primero.id())).as("el viejo ya no existe").isFalse();
    }

    @Test
    @DisplayName("un intento fallido guardado se conserva")
    void guardar_conIntentos_seConservan() {
        var pre = de(correo(), null, null, AHORA).conIntentoFallido();

        repositorio.guardar(pre);

        assertThat(repositorio.porCorreo(pre.correo()).orElseThrow().intentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("eliminar dice si borro algo: solo la primera de dos eliminaciones gana")
    void eliminar_soloUnaVezGana() {
        var pre = de(correo(), null, null, AHORA);
        repositorio.guardar(pre);

        assertThat(repositorio.eliminar(pre.id())).isTrue();
        assertThat(repositorio.eliminar(pre.id())).isFalse();
        assertThat(repositorio.porCorreo(pre.correo())).isEmpty();
    }

    @Test
    @DisplayName("borrarVencidos quita los vencidos (el que vence justo ahora, tambien) y deja los vigentes")
    void borrarVencidos_quitaSoloLosVencidos() {
        var lejano = Instant.parse("2031-06-01T00:00:00Z");
        var vencido = de(correo(), null, null, lejano.minus(Duration.ofHours(1)));
        var justo = de(correo(), null, null, lejano.minus(VIGENCIA));
        var vigente = de(correo(), null, null, lejano.minusSeconds(60));
        repositorio.guardar(vencido);
        repositorio.guardar(justo);
        repositorio.guardar(vigente);

        var borrados = repositorio.borrarVencidos(lejano);

        assertThat(borrados).isGreaterThanOrEqualTo(2);
        assertThat(repositorio.porCorreo(vencido.correo())).isEmpty();
        assertThat(repositorio.porCorreo(justo.correo())).isEmpty();
        assertThat(repositorio.porCorreo(vigente.correo())).isPresent();
        repositorio.eliminar(vigente.id());
    }
}
