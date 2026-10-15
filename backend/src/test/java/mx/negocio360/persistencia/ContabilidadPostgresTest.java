package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * A-03 y SK-01 (polizas inmutables) y la parte de base de A-02 (cargo o abono), contra PostgreSQL real.
 *
 * <p><b>NO EJECUTADA:</b> se escribio en una maquina sin Docker. Pendiente de correr con Testcontainers.
 */
@SpringBootTest
class ContabilidadPostgresTest {

    /** SQLSTATE de permiso denegado. */
    private static final String SIN_PERMISO = "42501";
    /** SQLSTATE que levanta plataforma.rechazar_modificacion(). */
    private static final String SOLO_INSERCION = "23001";
    /** SQLSTATE de un CHECK violado. */
    private static final String CHECK_VIOLADO = "23514";

    @Test
    @DisplayName("A-03: la aplicacion no puede cambiar ni borrar polizas ni movimientos: no tiene el permiso")
    void aplicacion_noModificaPolizas() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            var filas = FilasContabilidad.sembrar(sesion.enNegocio(negocio), negocio);

            assertThat(sesion.intentar("update contabilidad.poliza set total = 1 where id = ?", filas.poliza()))
                    .isEqualTo(SIN_PERMISO);
            assertThat(sesion.intentar("delete from contabilidad.poliza where id = ?", filas.poliza()))
                    .isEqualTo(SIN_PERMISO);
            assertThat(sesion.intentar("update contabilidad.movimiento set cargo = 1 where poliza_id = ?",
                    filas.poliza())).isEqualTo(SIN_PERMISO);
            assertThat(sesion.intentar("delete from contabilidad.movimiento where poliza_id = ?", filas.poliza()))
                    .isEqualTo(SIN_PERMISO);
        }
    }

    @Test
    @DisplayName("SK-01: aun con permisos de dueno, el disparador rechaza cambiar, borrar o vaciar polizas")
    void dueno_tampocoModificaPolizas() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var negocio = UUID.randomUUID();
            var filas = FilasContabilidad.sembrar(sesion.enNegocio(negocio), negocio);

            assertThat(sesion.intentar("update contabilidad.poliza set concepto = 'otro' where id = ?",
                    filas.poliza())).isEqualTo(SOLO_INSERCION);
            assertThat(sesion.intentar("delete from contabilidad.movimiento where poliza_id = ?", filas.poliza()))
                    .isEqualTo(SOLO_INSERCION);
            assertThat(sesion.intentar("truncate contabilidad.movimiento cascade")).isEqualTo(SOLO_INSERCION);
        }
    }

    @Test
    @DisplayName("A-02: un movimiento con cargo y abono a la vez, o sin ninguno, viola el CHECK")
    void movimiento_cargoYAbono_violaElCheck() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            var filas = FilasContabilidad.sembrar(sesion.enNegocio(negocio), negocio);

            assertThat(sesion.intentar("insert into contabilidad.movimiento (id, negocio_id, poliza_id, renglon, "
                    + "cuenta_id, cargo, abono) values (gen_random_uuid(), ?, ?, 3, ?, 10, 10)", negocio,
                    filas.poliza(), filas.caja())).isEqualTo(CHECK_VIOLADO);
            assertThat(sesion.intentar("insert into contabilidad.movimiento (id, negocio_id, poliza_id, renglon, "
                    + "cuenta_id, cargo, abono) values (gen_random_uuid(), ?, ?, 3, ?, 0, 0)", negocio,
                    filas.poliza(), filas.caja())).isEqualTo(CHECK_VIOLADO);
        }
    }

    @Test
    @DisplayName("A-06: una poliza solo se revierte una vez: revierte_poliza_id es unico")
    void reversa_unica() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            var filas = FilasContabilidad.sembrar(sesion.enNegocio(negocio), negocio);
            var reversa = "insert into contabilidad.poliza (id, negocio_id, periodo_id, tipo, folio, fecha, concepto, "
                    + "origen, extemporanea, revierte_poliza_id, total, creado_por, creado_en) values (gen_random_uuid(), "
                    + "?, ?, 'INGRESO', ?, date '2026-10-16', 'Reversa', 'REVERSA', false, ?, 10.00, 'prueba', now())";

            sesion.ejecutar(reversa, negocio, filas.periodo(), 2, filas.poliza());

            assertThat(sesion.intentar(reversa, negocio, filas.periodo(), 3, filas.poliza())).isEqualTo("23505");
        }
    }
}
