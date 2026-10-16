package mx.negocio360.persistencia;

import static mx.negocio360.persistencia.FilasClientes.ACTOR;
import static mx.negocio360.persistencia.FilasClientes.insertarAviso;
import static mx.negocio360.persistencia.FilasClientes.insertarCliente;
import static mx.negocio360.persistencia.FilasClientes.insertarConsentimiento;
import static mx.negocio360.persistencia.FilasClientes.insertarPreparacion;
import static mx.negocio360.persistencia.FilasClientes.telefono;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * HTC-21, HTC-22 y HTC-23: tablas de Clientes con seguridad por fila (app.negocio_id), historial que no se edita y
 * reglas de integridad. Las pruebas de aislamiento usan el rol de la aplicación, que no es superusuario; sin eso la
 * seguridad por fila no se aplicaría. Cada prueba corre en una transacción que se deshace.
 */
@SpringBootTest
class EsquemaClientesPostgresTest {

    @Test
    @DisplayName("un negocio ve a sus clientes y a los de otro negocio no")
    void aislamiento_unNegocioNoVeLosClientesDeOtro() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var a = UUID.randomUUID();
            var b = UUID.randomUUID();
            sesion.enNegocio(a);
            insertarAviso(sesion, a, 1);
            insertarCliente(sesion, a, "Cliente de A", telefono(), "ACTIVO");
            sesion.enNegocio(b);
            insertarAviso(sesion, b, 1);
            insertarCliente(sesion, b, "Cliente de B", telefono(), "ACTIVO");

            assertThat(sesion.enNegocio(a).columna("select nombre from clientes.cliente", String.class)).containsExactly("Cliente de A");
            assertThat(sesion.enNegocio(b).columna("select nombre from clientes.cliente", String.class)).containsExactly("Cliente de B");
        }
    }

    @Test
    @DisplayName("SC-01: sin negocio en el contexto no se ve ni se inserta nada (falla cerrado)")
    void aislamiento_sinNegocio_noSeVeNada() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var a = UUID.randomUUID();
            sesion.enNegocio(a);
            insertarAviso(sesion, a, 1);
            insertarCliente(sesion, a, "Cliente de A", telefono(), "ACTIVO");

            sesion.sinNegocio();

            assertThat(sesion.consultar("select count(*) from clientes.cliente", Long.class)).isZero();
            assertThat(sesion.consultar("select count(*) from clientes.aviso_privacidad", Long.class)).isZero();
        }
    }

    @Test
    @DisplayName("SC-01: desde el contexto de un negocio no se puede escribir un cliente de otro negocio")
    void aislamiento_noSePuedeEscribirEnOtroNegocio() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var a = UUID.randomUUID();
            var b = UUID.randomUUID();
            sesion.enNegocio(b);
            insertarAviso(sesion, b, 1);
            sesion.enNegocio(a);
            insertarAviso(sesion, a, 1);

            assertThat(sesion.intentar("insert into clientes.cliente (id, negocio_id, nombre, origen, aviso_version, "
                    + "aviso_entregado_en, creado_por) values (?, ?, 'Intruso', 'PANEL', 1, now(), ?)",
                    UUID.randomUUID(), b, ACTOR)).isEqualTo("42501");
            assertThat(sesion.consultar("select count(*) from clientes.cliente", Long.class)).isZero();
        }
    }

    @Test
    @DisplayName("HTC-22: todas las tablas con negocio_id tienen la seguridad por fila activada y forzada")
    void aislamiento_todasLasTablasConNegocioLaTienenForzada() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            // Cualquier esquema propio, no solo clientes: un módulo futuro con negocio_id también queda vigilado.
            // Excepciones de Identidad: sesion guarda el negocio activo de la sesión y negocio_ref es la copia global del
            // negocio (su negocio_id es la llave, no el dueño de la fila); ninguna pertenece a un negocio, se aíslan por rol.
            List<String> sinProteger = sesion.columna(
                    "select n.nspname || '.' || c.relname from pg_class c join pg_namespace n on n.oid = c.relnamespace "
                            + "where n.nspname not in ('pg_catalog', 'information_schema', 'public') "
                            + "and n.nspname not like 'pg\\_%' and c.relkind = 'r' "
                            + "and not (n.nspname = 'identidad' and c.relname in ('sesion', 'negocio_ref')) "
                            + "and exists (select 1 from information_schema.columns k where k.table_schema = n.nspname "
                            + "and k.table_name = c.relname and k.column_name = 'negocio_id') "
                            + "and not (c.relrowsecurity and c.relforcerowsecurity) order by 1", String.class);

            assertThat(sinProteger).isEmpty();
            assertThat(sesion.consultar("select count(*) from pg_class c join pg_namespace n on n.oid = c.relnamespace "
                    + "where n.nspname = 'clientes' and c.relkind = 'r' and c.relrowsecurity", Long.class)).isEqualTo(8L);
        }
    }

    @Test
    @DisplayName("P-03: el consentimiento solo se inserta; la aplicación no puede editarlo ni borrarlo")
    void consentimiento_soloInsercion_paraLaAplicacion() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            sesion.enNegocio(negocio);
            insertarAviso(sesion, negocio, 1);
            var cliente = insertarCliente(sesion, negocio, "Ana", telefono(), "ACTIVO");
            insertarConsentimiento(sesion, negocio, cliente);

            assertThat(sesion.intentar("update clientes.consentimiento set estado = 'REVOCADO'")).isEqualTo("42501");
            assertThat(sesion.intentar("delete from clientes.consentimiento")).isEqualTo("42501");
            assertThat(sesion.consultar("select count(*) from clientes.consentimiento", Long.class)).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("P-03: ni siquiera el dueño de la base puede editar, borrar ni vaciar el consentimiento o la bitácora de accesos")
    void historiales_sinModificacion_ni_paraElDueno() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var negocio = UUID.randomUUID();
            insertarAviso(sesion, negocio, 1);
            var cliente = insertarCliente(sesion, negocio, "Ana", telefono(), "ACTIVO");
            insertarConsentimiento(sesion, negocio, cliente);
            sesion.ejecutar("insert into clientes.acceso_cliente (id, negocio_id, cliente_id, actor, operacion, ocurrido_en) "
                    + "values (?, ?, ?, ?, 'DETALLE', now())", UUID.randomUUID(), negocio, cliente, ACTOR);

            for (var tabla : new String[] {"consentimiento", "acceso_cliente"}) {
                assertThat(sesion.intentar("update clientes." + tabla + " set negocio_id = negocio_id")).as(tabla).isEqualTo("23001");
                assertThat(sesion.intentar("delete from clientes." + tabla)).as(tabla).isEqualTo("23001");
                assertThat(sesion.intentar("truncate clientes." + tabla)).as(tabla).isEqualTo("23001");
            }
        }
    }

    @Test
    @DisplayName("I-03: el teléfono de un cliente activo es único por negocio; uno anonimizado deja libre el número")
    void cliente_telefonoUnicoPorNegocioSoloEntreActivos() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var a = UUID.randomUUID();
            var b = UUID.randomUUID();
            var tel = telefono();
            sesion.enNegocio(a);
            insertarAviso(sesion, a, 1);
            var primero = insertarCliente(sesion, a, "Primero", tel, "ACTIVO");
            sesion.enNegocio(b);
            insertarAviso(sesion, b, 1);
            insertarCliente(sesion, b, "De otro negocio", tel, "ACTIVO");
            sesion.enNegocio(a);
            var insertarOtro = "insert into clientes.cliente (id, negocio_id, nombre, telefono, origen, aviso_version, "
                    + "aviso_entregado_en, creado_por) values (?, ?, 'Repetido', ?, 'PANEL', 1, now(), ?)";

            assertThat(sesion.intentar(insertarOtro, UUID.randomUUID(), a, tel, ACTOR)).as("mientras sigue activo").isEqualTo("23505");

            sesion.ejecutar(ANONIMIZAR, primero);

            assertThat(sesion.intentar(insertarOtro, UUID.randomUUID(), a, tel, ACTOR)).as("ya anonimizado").isNull();
        }
    }

    private static final String ANONIMIZAR = "update clientes.cliente set estado = 'ANONIMIZADO', anonimizado_en = now(), "
            + "apellidos = null, telefono = null, correo = null, fecha_nacimiento = null, notas = null where id = ?";

    @Test
    @DisplayName("B-01: un cliente anonimizado no conserva teléfono, correo, apellidos, fecha de nacimiento ni notas")
    void cliente_anonimizadoConDatosPersonales_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            sesion.enNegocio(negocio);
            insertarAviso(sesion, negocio, 1);
            var id = insertarCliente(sesion, negocio, "Ana", telefono(), "ACTIVO");
            sesion.ejecutar("update clientes.cliente set correo = 'a@ejemplo.invalid', fecha_nacimiento = date '1990-01-01', "
                    + "notas = 'algo' where id = ?", id);
            var datosPersonales = List.of("apellidos", "telefono", "correo", "fecha_nacimiento", "notas");

            for (var conservado : datosPersonales) {
                var vaciados = datosPersonales.stream().filter(c -> !c.equals(conservado)).map(c -> c + " = null")
                        .collect(java.util.stream.Collectors.joining(", "));

                assertThat(sesion.intentar("update clientes.cliente set estado = 'ANONIMIZADO', anonimizado_en = now(), "
                        + vaciados + " where id = ?", id)).as("conserva " + conservado).isEqualTo("23514");
            }
            assertThat(sesion.intentar(ANONIMIZAR, id)).as("anonimizar bien").isNull();
        }
    }

    @Test
    @DisplayName("B-02: la anonimización es irreversible, ni siquiera para el dueño de la base")
    void cliente_anonimizado_noVuelveAActivo() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var negocio = UUID.randomUUID();
            insertarAviso(sesion, negocio, 1);
            var id = insertarCliente(sesion, negocio, "Ana", telefono(), "ACTIVO");
            sesion.ejecutar(ANONIMIZAR, id);

            assertThat(sesion.intentar("update clientes.cliente set estado = 'ACTIVO', anonimizado_en = null where id = ?", id))
                    .isEqualTo("23001");
        }
    }

    @Test
    @DisplayName("SC-10: una preparación usada no puede conservar el comando con datos personales")
    void preparacion_usadaSinComando() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            sesion.enNegocio(negocio);
            var token = insertarPreparacion(sesion, negocio);

            assertThat(sesion.intentar("update clientes.preparacion set usado_en = now() where token_hash = ?", token))
                    .as("usada con el comando puesto").isEqualTo("23514");
            assertThat(sesion.intentar("update clientes.preparacion set usado_en = now(), comando = null where token_hash = ?", token))
                    .as("usada y vaciada").isNull();
        }
    }

    @Test
    @DisplayName("L-03: el contenido de un aviso publicado no se edita; solo se cierra su vigencia")
    void aviso_soloSePuedeCerrarLaVigencia() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            sesion.enNegocio(negocio);
            insertarAviso(sesion, negocio, 1);

            for (var columna : new String[] {"texto_integral = 'otro'", "finalidades = '[]'::jsonb", "version = 9",
                    "vigente_desde = now() - interval '1 year'", "responsable_nombre = 'otro'"}) {
                assertThat(sesion.intentar("update clientes.aviso_privacidad set " + columna)).as(columna).isEqualTo("42501");
            }
            assertThat(sesion.intentar("update clientes.aviso_privacidad set vigente_hasta = now() + interval '1 day'"))
                    .as("cerrar la vigencia").isNull();
        }
    }

    @Test
    @DisplayName("H-03: la aplicación solo borra marcas ANULADO_PREVIO; el historial de compras aplicadas no se borra")
    void compraAplicada_soloSeBorranLasMarcasPrevias() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            sesion.enNegocio(negocio);
            insertarAviso(sesion, negocio, 1);
            var cliente = insertarCliente(sesion, negocio, "Ana", telefono(), "ACTIVO");
            FilasClientes.insertarCompra(sesion, negocio, cliente, "APLICADA");
            sesion.ejecutar("insert into clientes.compra_aplicada (id, negocio_id, venta_id, estado) "
                    + "values (?, ?, ?, 'ANULADO_PREVIO')", UUID.randomUUID(), negocio, UUID.randomUUID());

            assertThat(sesion.ejecutar("delete from clientes.compra_aplicada where estado = 'APLICADA'")).isZero();
            assertThat(sesion.ejecutar("delete from clientes.compra_aplicada where estado = 'ANULADO_PREVIO'")).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("I-01 e I-02: nombre y teléfono tienen formato válido; origen y estado son los del diseño")
    void cliente_valoresInvalidos_seRechazan() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            sesion.enNegocio(negocio);
            insertarAviso(sesion, negocio, 1);
            var base = "insert into clientes.cliente (id, negocio_id, nombre, telefono, origen, estado, aviso_version, aviso_entregado_en, creado_por) "
                    + "values (?, ?, ?, ?, ?, ?, 1, now(), ?)";

            assertThat(sesion.intentar(base, UUID.randomUUID(), negocio, "  ", null, "PANEL", "ACTIVO", ACTOR)).as("nombre vacío").isEqualTo("23514");
            assertThat(sesion.intentar(base, UUID.randomUUID(), negocio, "Ana", "2221110001", "PANEL", "ACTIVO", ACTOR)).as("teléfono sin E.164").isEqualTo("23514");
            assertThat(sesion.intentar(base, UUID.randomUUID(), negocio, "Ana", null, "OTRO", "ACTIVO", ACTOR)).as("origen").isEqualTo("23514");
            assertThat(sesion.intentar(base, UUID.randomUUID(), negocio, "Ana", null, "PANEL", "BORRADO", ACTOR)).as("estado").isEqualTo("23514");
        }
    }

    @Test
    @DisplayName("la búsqueda ignora acentos y mayúsculas: nombre_busqueda se calcula en la base")
    void cliente_nombreBusqueda_sinAcentosNiMayusculas() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            sesion.enNegocio(negocio);
            insertarAviso(sesion, negocio, 1);
            insertarCliente(sesion, negocio, "José Ángel", telefono(), "ACTIVO");

            assertThat(sesion.consultar("select nombre_busqueda from clientes.cliente", String.class)).isEqualTo("jose angel perez");
            assertThat(sesion.consultar("select count(*) from clientes.cliente where nombre_busqueda like '%jose%'", Long.class)).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("L-03: un cliente solo puede apuntar a un aviso de privacidad de su mismo negocio")
    void cliente_avisoDeOtroNegocio_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var a = UUID.randomUUID();
            var b = UUID.randomUUID();
            insertarAviso(sesion, a, 1);

            assertThat(sesion.intentar("insert into clientes.cliente (id, negocio_id, nombre, origen, aviso_version, "
                    + "aviso_entregado_en, creado_por) values (?, ?, 'Ana', 'PANEL', 1, now(), ?)",
                    UUID.randomUUID(), b, ACTOR)).isEqualTo("23503");
        }
    }

    @Test
    @DisplayName("el consentimiento de un negocio no puede colgarse de un cliente de otro negocio")
    void consentimiento_clienteDeOtroNegocio_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var a = UUID.randomUUID();
            var b = UUID.randomUUID();
            insertarAviso(sesion, a, 1);
            insertarAviso(sesion, b, 1);
            var clienteDeA = insertarCliente(sesion, a, "Ana", telefono(), "ACTIVO");

            assertThat(sesion.intentar("insert into clientes.consentimiento (id, negocio_id, cliente_id, finalidad, estado, forma, medio, "
                    + "aviso_version, registrado_por, registrado_en) values (?, ?, ?, 'PROMOCIONES', 'OTORGADO', 'EXPRESO', 'PANEL', 1, ?, now())",
                    UUID.randomUUID(), b, clienteDeA, ACTOR)).isEqualTo("23503");
        }
    }

    @Test
    @DisplayName("H-02: una venta se aplica una sola vez por negocio")
    void compraAplicada_ventaUnicaPorNegocio() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            var venta = UUID.randomUUID();
            sesion.enNegocio(negocio);
            var insertar = "insert into clientes.compra_aplicada (id, negocio_id, venta_id, estado, creado_en) values (?, ?, ?, 'ANULADO_PREVIO', now())";
            sesion.ejecutar(insertar, UUID.randomUUID(), negocio, venta);

            assertThat(sesion.intentar(insertar, UUID.randomUUID(), negocio, venta)).isEqualTo("23505");
        }
    }

    @Test
    @DisplayName("solo hay un aviso de privacidad vigente por negocio, y su versión no se repite")
    void aviso_unVigenteYVersionUnica() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var negocio = UUID.randomUUID();
            sesion.enNegocio(negocio);
            insertarAviso(sesion, negocio, 1);

            assertThat(sesion.intentar("insert into clientes.aviso_privacidad (id, negocio_id, version, responsable_nombre, "
                    + "responsable_domicilio, finalidades, texto_simplificado, texto_integral, contacto_arco, vigente_desde, "
                    + "publicado_por) values (?, ?, 1, 'x', 'x', '[]'::jsonb, 'x', 'x', 'x', now(), 'x')", UUID.randomUUID(), negocio))
                    .as("versión repetida").isEqualTo("23505");
            assertThat(sesion.intentar("insert into clientes.aviso_privacidad (id, negocio_id, version, responsable_nombre, "
                    + "responsable_domicilio, finalidades, texto_simplificado, texto_integral, contacto_arco, vigente_desde, "
                    + "publicado_por) values (?, ?, 2, 'x', 'x', '[]'::jsonb, 'x', 'x', 'x', now(), 'x')", UUID.randomUUID(), negocio))
                    .as("dos vigentes a la vez").isEqualTo("23505");
        }
    }
}
