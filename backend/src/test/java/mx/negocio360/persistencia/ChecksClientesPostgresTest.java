package mx.negocio360.persistencia;

import static mx.negocio360.persistencia.FilasClientes.insertarAviso;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Las reglas de dominio que la base impone con CHECK: cada una rechaza el valor inválido (SQLSTATE 23514). Corre como
 * dueño de la base porque lo que se prueba es la restricción, no los permisos. %1$s es el negocio y %2$s un cliente.
 */
@SpringBootTest
class ChecksClientesPostgresTest {

    private static final String ARCO = "insert into clientes.solicitud_arco (id, negocio_id, cliente_id, derecho, recibida_en, "
            + "medio_recepcion, responder_antes_de, estado, resultado, creado_por) values (gen_random_uuid(), '%1$s', '%2$s', "
            + "%3$s, current_date, %4$s, current_date + 20, %5$s, %6$s, 'prueba')";
    private static final String CONSENTIMIENTO = "insert into clientes.consentimiento (id, negocio_id, cliente_id, finalidad, "
            + "estado, forma, medio, aviso_version, registrado_por, registrado_en) values (gen_random_uuid(), '%1$s', '%2$s', "
            + "%3$s, %4$s, %5$s, %6$s, 1, 'prueba', now())";
    private static final String COMPRA = "insert into clientes.compra_aplicada (id, negocio_id, venta_id, estado, motivo) "
            + "values (gen_random_uuid(), '%1$s', gen_random_uuid(), %3$s, %4$s)";
    private static final String RESUMEN = "insert into clientes.resumen_cliente (cliente_id, negocio_id, compras_n, total_comprado) "
            + "values ('%2$s', '%1$s', %3$s, %4$s)";
    private static final String AVISO = "insert into clientes.aviso_privacidad (id, negocio_id, version, responsable_nombre, "
            + "responsable_domicilio, finalidades, texto_simplificado, texto_integral, contacto_arco, vigente_desde, "
            + "vigente_hasta, publicado_por) values (gen_random_uuid(), '%1$s', %3$s, %4$s, 'x', %5$s, 'x', 'x', 'x', "
            + "now(), %6$s, 'prueba')";
    private static final String PREPARACION = "insert into clientes.preparacion (token_hash, negocio_id, actor, comando_tipo, "
            + "comando_hash, comando, resumen, expira_en) values (%3$s, '%1$s', 'prueba', 'x', 'x', '{}', '{}', now())";

    private static Arguments caso(String nombre, String plantilla, Object... valores) {
        return Arguments.of(nombre, plantilla, valores);
    }

    static Stream<Arguments> valoresInvalidos() {
        return Stream.of(
                caso("ck_arco_derecho", ARCO, "'BORRAR'", "'PRESENCIAL'", "'RECIBIDA'", "null"),
                caso("ck_arco_medio", ARCO, "'ACCESO'", "'TELEPATIA'", "'RECIBIDA'", "null"),
                caso("ck_arco_estado", ARCO, "'ACCESO'", "'PRESENCIAL'", "'PERDIDA'", "null"),
                caso("ck_arco_resultado", ARCO, "'ACCESO'", "'PRESENCIAL'", "'CERRADA'", "'QUIZAS'"),
                caso("ck_consentimiento_finalidad", CONSENTIMIENTO, "'SPAM'", "'OTORGADO'", "'EXPRESO'", "'PANEL'"),
                caso("ck_consentimiento_estado", CONSENTIMIENTO, "'PROMOCIONES'", "'TAL_VEZ'", "'EXPRESO'", "'PANEL'"),
                caso("ck_consentimiento_forma", CONSENTIMIENTO, "'PROMOCIONES'", "'OTORGADO'", "'IMPLICITO'", "'PANEL'"),
                caso("ck_consentimiento_medio", CONSENTIMIENTO, "'PROMOCIONES'", "'OTORGADO'", "'EXPRESO'", "'PALOMA'"),
                caso("ck_compra_estado", COMPRA, "'PAGADA'", "null"),
                caso("ck_compra_motivo", COMPRA, "'OMITIDA'", "'PORQUE_SI'"),
                caso("ck_resumen_compras", RESUMEN, "-1", "0"),
                caso("ck_resumen_total", RESUMEN, "0", "-0.01"),
                caso("ck_aviso_version", AVISO, "0", "'Fonda'", "'[]'::jsonb", "null"),
                caso("ck_aviso_responsable", AVISO, "1", "'  '", "'[]'::jsonb", "null"),
                caso("ck_aviso_finalidades", AVISO, "1", "'Fonda'", "'{}'::jsonb", "null"),
                caso("ck_aviso_vigencia", AVISO, "1", "'Fonda'", "'[]'::jsonb", "now() - interval '1 day'"),
                caso("ck_preparacion_token", PREPARACION, "'no-es-un-hash'"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("valoresInvalidos")
    @DisplayName("la base rechaza el valor que rompe la regla de dominio")
    void restriccion_rechazaElValorInvalido(String nombre, String plantilla, Object[] valores) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var negocio = UUID.randomUUID();
            insertarAviso(sesion, negocio, 1);
            var cliente = FilasClientes.insertarCliente(sesion, negocio, "Ana", FilasClientes.telefono(), "ACTIVO");
            var argumentos = new Object[valores.length + 2];
            argumentos[0] = negocio;
            argumentos[1] = cliente;
            System.arraycopy(valores, 0, argumentos, 2, valores.length);

            assertThat(sesion.intentar(plantilla.formatted(argumentos))).as(nombre).isEqualTo("23514");
        }
    }
}
