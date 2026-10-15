package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * E-03 y SK-03: seguridad por fila en las 13 tablas de Contabilidad, con dos negocios y el rol de la aplicacion.
 *
 * <p><b>NO EJECUTADA:</b> se escribio en una maquina sin Docker. Pendiente de correr con Testcontainers.
 */
@SpringBootTest
class AislamientoContabilidadPostgresTest {

    private static final String[] TABLAS = {"configuracion", "cuenta", "regla_contable", "periodo", "poliza",
            "movimiento", "saldo_cuenta", "venta_contable", "canje_contable", "corte_contable", "evento_procesado",
            "pendiente_contable", "preparacion"};

    static Stream<String> tablas() {
        return Stream.of(TABLAS);
    }

    /** Siembra una fila de cada tabla para el negocio fijado en la sesion. */
    private static void sembrarTodo(SesionSql sesion, UUID negocio) throws SQLException {
        var filas = FilasContabilidad.sembrar(sesion, negocio);
        sesion.ejecutar("insert into contabilidad.configuracion (negocio_id, activada_en, precios_con_iva, "
                + "tasa_iva_default, fuente_tasa, propinas_en_total, ejercicio_inicia_mes) values (?, now(), true, "
                + "0.16, 'Dato de ejemplo de pruebas', false, 1)", negocio);
        sesion.ejecutar("insert into contabilidad.regla_contable (id, negocio_id, disparador, condicion, cuenta_cargo_id, "
                + "cuenta_abono_id, fuente, vigente_desde) values (gen_random_uuid(), ?, 'CORTE_SOBRANTE', null, ?, ?, "
                + "'Catalogo base', now())", negocio, filas.caja(), filas.ventas());
        sesion.ejecutar("insert into contabilidad.saldo_cuenta (negocio_id, cuenta_id, periodo_id, saldo_inicial, cargos, "
                + "abonos, saldo_final) values (?, ?, ?, 0, 10, 0, 10)", negocio, filas.caja(), filas.periodo());
        sesion.ejecutar("insert into contabilidad.venta_contable (id, negocio_id, venta_id, medio_pago, total, base, iva, "
                + "ocurrido_en, poliza_id) values (gen_random_uuid(), ?, gen_random_uuid(), 'EFECTIVO', 10, 10, 0, now(), ?)",
                negocio, filas.poliza());
        sesion.ejecutar("insert into contabilidad.canje_contable (id, negocio_id, cupon_id, monto_descuento, base_descuento, "
                + "financiador, estado, motivo) values (gen_random_uuid(), ?, gen_random_uuid(), 5, 5, 'NEGOCIO', "
                + "'SIN_EFECTO', 'SIN_VENTA')", negocio);
        sesion.ejecutar("insert into contabilidad.corte_contable (id, negocio_id, corte_id, caja_sesion_id, "
                + "efectivo_esperado, efectivo_contado, diferencia, ventas_efectivo_contables, conciliacion, cerrada_en) "
                + "values (gen_random_uuid(), ?, gen_random_uuid(), gen_random_uuid(), 10, 10, 0, 10, 'CUADRA', now())",
                negocio);
        sesion.ejecutar("insert into contabilidad.evento_procesado (negocio_id, evento_id, tipo, version, recibido_en, "
                + "resultado) values (?, gen_random_uuid(), 'venta.registrada', 1, now(), 'APLICADO')", negocio);
        sesion.ejecutar("insert into contabilidad.pendiente_contable (id, negocio_id, evento_id, tipo_evento, datos, "
                + "motivo, fecha_evento, estado, creado_en) values (gen_random_uuid(), ?, gen_random_uuid(), "
                + "'venta.registrada', '{}'::jsonb, 'REGLA_FALTANTE', current_date, 'ABIERTO', now())", negocio);
        sesion.ejecutar("insert into contabilidad.preparacion (token_hash, negocio_id, actor, comando_tipo, comando_hash, "
                + "comando, resumen, expira_en) values (encode(sha256(gen_random_uuid()::text::bytea), 'hex'), ?, "
                + "'prueba', 'CerrarPeriodo', 'x', '{}'::jsonb, 'resumen', now() + interval '5 minutes')", negocio);
    }

    private static long contar(SesionSql sesion, String tabla) throws SQLException {
        return sesion.consultar("select count(*) from contabilidad." + tabla, Long.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablas")
    @DisplayName("E-03: cada negocio ve solo sus filas, y sin negocio en el contexto no ve ninguna")
    void lectura_cadaNegocioVeSoloLoSuyo(String tabla) throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var a = UUID.randomUUID();
            var b = UUID.randomUUID();
            sembrarTodo(sesion.enNegocio(a), a);
            sembrarTodo(sesion.enNegocio(b), b);

            var esperado = tabla.equals("cuenta") || tabla.equals("movimiento") ? 2L : 1L;
            assertThat(contar(sesion.enNegocio(a), tabla)).as("negocio A").isEqualTo(esperado);
            assertThat(contar(sesion.enNegocio(b), tabla)).as("negocio B").isEqualTo(esperado);
            assertThat(contar(sesion.sinNegocio(), tabla)).as("sin negocio").isZero();
        }
    }

    @Test
    @DisplayName("E-03: un listener con el negocio A no puede marcar un evento del negocio B")
    void escritura_noEntraAOtroNegocio() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var a = UUID.randomUUID();
            var b = UUID.randomUUID();
            sesion.enNegocio(a);

            var estado = sesion.intentar("insert into contabilidad.evento_procesado (negocio_id, evento_id, tipo, "
                    + "version, recibido_en, resultado) values (?, gen_random_uuid(), 'x', 1, now(), 'APLICADO')", b);

            assertThat(estado).isEqualTo("42501");
        }
    }
}
