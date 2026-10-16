package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import mx.negocio360.clientes.seguridad.PermisosDeClientes;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import mx.negocio360.identidad.aplicacion.PermisosDeIdentidad;
import mx.negocio360.identidad.dominio.Giro;
import mx.negocio360.identidad.dominio.RolesBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * IDN-2 (V022): negocio de referencia, roles, permisos, plantillas por giro y membresias. Los catalogos de la base
 * deben coincidir con los de los modulos y los roles base (AM-09, AZ-03); las membresias llevan seguridad por fila
 * por negocio, y cada persona puede leer las suyas en todos sus negocios.
 */
@SpringBootTest
class EsquemaEquipoPostgresTest {

    private static final String INSERTAR_CUENTA = "insert into identidad.cuenta (id, nombre, apellidos, telefono, correo, "
            + "aviso_version, aviso_aceptado_en, estado, creada_en) values (?, 'Ana', 'Prueba', ?, ?, 1, now(), 'ACTIVA', now())";
    private static final String INSERTAR_NEGOCIO = "insert into identidad.negocio_ref (negocio_id, nombre, giro, estado) "
            + "values (?, 'Fonda de prueba', ?, ?)";
    private static final String INSERTAR_MEMBRESIA = "insert into identidad.membresia (id, cuenta_id, negocio_id, rol_codigo, "
            + "estado, creada_en, actualizada_en, revocada_en) values (?, ?, ?, ?, ?, now(), now(), ?)";

    private static UUID cuenta(SesionSql sesion) throws Exception {
        var id = UUID.randomUUID();
        var numero = ThreadLocalRandom.current().nextInt(100_000_000);
        sesion.ejecutar(INSERTAR_CUENTA, id, "+52222" + String.format("%07d", numero % 10_000_000), "e" + numero + "@example.com");
        return id;
    }

    private static UUID negocio(SesionSql sesion) throws Exception {
        var id = UUID.randomUUID();
        sesion.ejecutar(INSERTAR_NEGOCIO, id, "RESTAURANTE", "ACTIVO");
        return id;
    }

    private static void membresia(SesionSql sesion, UUID cuenta, UUID negocio, String rol) throws Exception {
        sesion.enNegocio(negocio).ejecutar(INSERTAR_MEMBRESIA, UUID.randomUUID(), cuenta, negocio, rol, "ACTIVA", null);
    }

    // ---- los catalogos son datos y coinciden con el codigo ----

    @Test
    @DisplayName("AZ-02: los permisos sembrados en la base son exactamente los que declaran Identidad y Clientes")
    void permisos_coincidenConLosCatalogosDeLosModulos() throws Exception {
        var declarados = new java.util.ArrayList<PermisoDeclarado>(new PermisosDeIdentidad().permisos());
        declarados.addAll(new PermisosDeClientes().permisos());

        try (var sesion = SesionSql.comoAplicacion()) {
            var codigos = sesion.columna("select codigo from identidad.permiso order by codigo", String.class);
            assertThat(codigos).containsExactlyElementsOf(declarados.stream().map(PermisoDeclarado::codigo).sorted().toList());
            for (var permiso : declarados) {
                assertThat(sesion.consultar("select modulo || '|' || descripcion || '|' || sensible from identidad.permiso "
                        + "where codigo = ?", String.class, permiso.codigo()))
                        .as(permiso.codigo()).isEqualTo(permiso.modulo() + "|" + permiso.descripcion() + "|" + permiso.sensible());
            }
        }
    }

    @Test
    @DisplayName("AM-09: los 8 roles base y sus permisos sembrados coinciden con RolesBase")
    void roles_coincidenConRolesBase() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(sesion.columna("select codigo from identidad.rol order by codigo", String.class))
                    .containsExactlyElementsOf(RolesBase.todos().stream().map(r -> r.codigo()).sorted().toList());
            for (var rol : RolesBase.todos()) {
                var enBase = sesion.columna("select permiso_codigo from identidad.rol_permiso where rol_codigo = ? "
                        + "order by permiso_codigo", String.class, rol.codigo());
                assertThat(enBase).as(rol.codigo()).containsExactlyInAnyOrderElementsOf(rol.permisos());
                assertThat(sesion.consultar("select nombre from identidad.rol where codigo = ?", String.class, rol.codigo()))
                        .isEqualTo(rol.nombre());
            }
        }
    }

    @Test
    @DisplayName("D-69: la plantilla de cada giro sembrada en la base coincide con RolesBase y conserva el orden")
    void plantillas_coincidenConRolesBase() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            for (var giro : Giro.values()) {
                assertThat(sesion.columna("select rol_codigo from identidad.plantilla_giro_rol where giro = ? order by orden",
                        String.class, giro.name())).as(giro.name()).containsExactlyElementsOf(RolesBase.plantilla(giro));
            }
        }
    }

    @Test
    @DisplayName("los catalogos son datos de solo lectura para la aplicacion: ni insertar, ni cambiar, ni borrar")
    void catalogos_laAplicacionSoloLee() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            for (var sentencia : List.of(
                    "insert into identidad.rol (codigo, nombre) values ('INTRUSO', 'Intruso')",
                    "update identidad.rol set nombre = 'Otro' where codigo = 'CAJERO'",
                    "delete from identidad.rol_permiso",
                    "update identidad.permiso set sensible = false",
                    "insert into identidad.plantilla_giro_rol (giro, rol_codigo, orden) values ('MUSEO', 'CAJERO', 9)")) {
                assertThat(sesion.intentar(sentencia)).as(sentencia).isEqualTo("42501");
            }
        }
    }

    @Test
    @DisplayName("los codigos de permiso y de rol tienen la forma del contrato y cada permiso es de su modulo")
    void catalogos_formaDeLosCodigos() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            assertThat(sesion.intentar("insert into identidad.permiso (codigo, modulo, descripcion, sensible) "
                    + "values ('Clientes.Mal', 'clientes', 'x', false)")).isEqualTo("23514");
            assertThat(sesion.intentar("insert into identidad.permiso (codigo, modulo, descripcion, sensible) "
                    + "values ('clientes.prueba', 'inventario', 'x', false)")).as("modulo distinto del codigo").isEqualTo("23514");
            assertThat(sesion.intentar("insert into identidad.rol (codigo, nombre) values ('minusculas', 'x')"))
                    .isEqualTo("23514");
            assertThat(sesion.intentar("insert into identidad.rol_permiso (rol_codigo, permiso_codigo) "
                    + "values ('CAJERO', 'clientes.no_existe')")).as("AZ-03 en la base").isEqualTo("23503");
        }
    }

    // ---- negocio de referencia ----

    @Test
    @DisplayName("el negocio de referencia solo admite los giros y estados conocidos y un nombre no vacio")
    void negocioRef_valoresInvalidos_seRechazan() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            assertThat(sesion.intentar(INSERTAR_NEGOCIO, UUID.randomUUID(), "FARMACIA", "ACTIVO")).isEqualTo("23514");
            assertThat(sesion.intentar(INSERTAR_NEGOCIO, UUID.randomUUID(), "MUSEO", "BORRADO")).isEqualTo("23514");
            assertThat(sesion.intentar("insert into identidad.negocio_ref (negocio_id, nombre, giro, estado) "
                    + "values (?, '   ', 'MUSEO', 'ACTIVO')", UUID.randomUUID())).isEqualTo("23514");
            assertThat(sesion.intentar(INSERTAR_NEGOCIO, UUID.randomUUID(), "MUSEO", "ACTIVO")).isNull();
        }
    }

    // ---- membresia ----

    @Test
    @DisplayName("AM-01: una cuenta tiene una sola membresia por negocio, con rol, cuenta y negocio existentes")
    void membresia_restricciones() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var cuenta = cuenta(sesion);
            var negocio = negocio(sesion);
            membresia(sesion, cuenta, negocio, "DUENO");

            sesion.enNegocio(negocio);
            assertThat(sesion.intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), cuenta, negocio, "CAJERO", "ACTIVA", null))
                    .as("segunda membresia en el mismo negocio").isEqualTo("23505");
            var otro = negocio(sesion);
            sesion.enNegocio(otro);
            assertThat(sesion.intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), cuenta, otro, "SUPERUSUARIO", "ACTIVA", null))
                    .as("rol inexistente").isEqualTo("23503");
            assertThat(sesion.intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), UUID.randomUUID(), otro, "CAJERO", "ACTIVA", null))
                    .as("cuenta inexistente").isEqualTo("23503");
            assertThat(sesion.intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), cuenta, otro, "CAJERO", "BORRADA", null))
                    .as("estado desconocido").isEqualTo("23514");
            assertThat(sesion.intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), cuenta, otro, "CAJERO", "REVOCADA", null))
                    .as("revocada sin fecha de baja").isEqualTo("23514");
            assertThat(sesion.intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), cuenta, otro, "CAJERO", "ACTIVA",
                    java.sql.Timestamp.from(java.time.Instant.now()))).as("fecha de baja sin revocar").isEqualTo("23514");
            assertThat(sesion.intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), cuenta, UUID.randomUUID(), "CAJERO", "ACTIVA", null))
                    .as("negocio inexistente (y fuera del negocio de la transaccion)").isNotNull();
        }
    }

    @Test
    @DisplayName("SI-10: con el negocio de la transaccion solo se ven las membresias de ese negocio")
    void membresia_seguridadPorFila_porNegocio() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var ana = cuenta(sesion);
            var beto = cuenta(sesion);
            var fonda = negocio(sesion);
            var tienda = negocio(sesion);
            membresia(sesion, ana, fonda, "DUENO");
            membresia(sesion, beto, tienda, "DUENO");

            assertThat(sesion.enNegocio(fonda).sinCuenta().columna("select cuenta_id from identidad.membresia", UUID.class))
                    .containsExactly(ana);
            assertThat(sesion.enNegocio(tienda).columna("select cuenta_id from identidad.membresia", UUID.class))
                    .containsExactly(beto);
            assertThat(sesion.sinNegocio().sinCuenta().columna("select cuenta_id from identidad.membresia", UUID.class))
                    .as("sin negocio ni cuenta no se ve nada").isEmpty();
        }
    }

    @Test
    @DisplayName("cada persona lee sus propias membresias en todos sus negocios, y solo las suyas")
    void membresia_seguridadPorFila_porCuenta() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var ana = cuenta(sesion);
            var beto = cuenta(sesion);
            var fonda = negocio(sesion);
            var tienda = negocio(sesion);
            membresia(sesion, ana, fonda, "DUENO");
            membresia(sesion, ana, tienda, "CAJERO");
            membresia(sesion, beto, tienda, "DUENO");

            var deAna = sesion.sinNegocio().enCuenta(ana).columna("select negocio_id from identidad.membresia", UUID.class);

            assertThat(deAna).containsExactlyInAnyOrder(fonda, tienda);
            assertThat(sesion.enCuenta(beto).columna("select cuenta_id from identidad.membresia", UUID.class))
                    .containsExactly(beto);
        }
    }

    @Test
    @DisplayName("no se puede escribir una membresia en un negocio distinto del de la transaccion")
    void membresia_escrituraEnOtroNegocio_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var ana = cuenta(sesion);
            var fonda = negocio(sesion);
            var tienda = negocio(sesion);
            sesion.enNegocio(fonda);

            assertThat(sesion.intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), ana, tienda, "DUENO", "ACTIVA", null))
                    .isEqualTo("42501");
            assertThat(sesion.sinNegocio().intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), ana, fonda, "DUENO", "ACTIVA", null))
                    .as("sin negocio no se escribe nada").isEqualTo("42501");
            assertThat(sesion.enNegocio(fonda).intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), ana, fonda, "DUENO", "ACTIVA", null))
                    .isNull();
        }
    }

    @Test
    @DisplayName("la politica de membresia no deja a una persona mover su fila a otro negocio")
    void membresia_cambiarDeNegocio_seRechaza() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var ana = cuenta(sesion);
            var fonda = negocio(sesion);
            var tienda = negocio(sesion);
            membresia(sesion, ana, fonda, "DUENO");

            sesion.enNegocio(fonda);

            assertThat(sesion.intentar("update identidad.membresia set negocio_id = ? where cuenta_id = ?", tienda, ana))
                    .as("lo frena el disparador antes que la politica").isEqualTo("23001");
        }
    }

    @Test
    @DisplayName("la membresia tiene la seguridad por fila forzada; las tablas globales no la llevan")
    void membresia_rlsForzada() throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            assertThat(sesion.consultar("select relrowsecurity and relforcerowsecurity from pg_class "
                    + "where oid = 'identidad.membresia'::regclass", Boolean.class)).isTrue();
            for (var tabla : List.of("rol", "permiso", "rol_permiso", "plantilla_giro_rol", "negocio_ref")) {
                assertThat(sesion.consultar("select relrowsecurity from pg_class where oid = ?::regclass", Boolean.class,
                        "identidad." + tabla)).as(tabla).isFalse();
            }
        }
    }

    // ---- revision de la ronda del 2026-10-15 ----

    @Test
    @DisplayName("AM-01: tras revocar a alguien se le puede volver a invitar (otra fila), pero nunca dos no revocadas a la vez")
    void membresia_invitarDeNuevoTrasRevocar() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var ana = cuenta(sesion);
            var fonda = negocio(sesion);
            membresia(sesion, ana, fonda, "CAJERO");
            sesion.enNegocio(fonda);

            sesion.ejecutar("update identidad.membresia set estado = 'REVOCADA', revocada_en = now() "
                    + "where cuenta_id = ? and negocio_id = ?", ana, fonda);

            assertThat(sesion.intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), ana, fonda, "GERENTE", "ACTIVA", null)).isNull();
            assertThat(sesion.consultar("select count(*) from identidad.membresia where cuenta_id = ?", Long.class, ana))
                    .isEqualTo(2L);
            assertThat(sesion.intentar(INSERTAR_MEMBRESIA, UUID.randomUUID(), ana, fonda, "CAJERO", "ACTIVA", null))
                    .isEqualTo("23505");
        }
    }

    @Test
    @DisplayName("una membresia revocada no vuelve a ACTIVA y ninguna cambia de cuenta (disparador)")
    void membresia_disparador_protegeLasTransiciones() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var ana = cuenta(sesion);
            var beto = cuenta(sesion);
            var fonda = negocio(sesion);
            membresia(sesion, ana, fonda, "CAJERO");
            sesion.enNegocio(fonda);

            assertThat(sesion.intentar("update identidad.membresia set cuenta_id = ? where cuenta_id = ?", beto, ana))
                    .isEqualTo("23001");
            sesion.ejecutar("update identidad.membresia set estado = 'REVOCADA', revocada_en = now() where cuenta_id = ?", ana);
            assertThat(sesion.intentar("update identidad.membresia set estado = 'ACTIVA', revocada_en = null "
                    + "where cuenta_id = ?", ana)).isEqualTo("23001");
            assertThat(sesion.intentar("update identidad.membresia set estado = 'SUSPENDIDA', revocada_en = null "
                    + "where cuenta_id = ?", ana)).isEqualTo("23001");
        }
    }

    @Test
    @DisplayName("quien solo tiene la cuenta de la transaccion lee sus membresias pero no puede cambiarlas")
    void membresia_soloLaCuenta_noPuedeEscribir() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var ana = cuenta(sesion);
            var fonda = negocio(sesion);
            membresia(sesion, ana, fonda, "CAJERO");

            sesion.sinNegocio().enCuenta(ana);
            var cambiadas = sesion.ejecutar("update identidad.membresia set rol_codigo = 'DUENO' where cuenta_id = ?", ana);

            assertThat(cambiadas).as("la fila se ve, pero no se puede actualizar sin el negocio").isZero();
            assertThat(sesion.columna("select rol_codigo from identidad.membresia", String.class)).containsExactly("CAJERO");
            assertThat(sesion.intentar("delete from identidad.membresia")).as("y no hay permiso para borrar").isEqualTo("42501");
        }
    }

    @Test
    @DisplayName("de negocio_ref la aplicacion solo cambia el estado: no renombra ni borra negocios")
    void negocioRef_laAplicacionSoloCambiaElEstado() throws Exception {
        try (var sesion = SesionSql.comoAplicacion()) {
            var fonda = negocio(sesion);

            assertThat(sesion.intentar("update identidad.negocio_ref set nombre = 'Otro' where negocio_id = ?", fonda))
                    .isEqualTo("42501");
            assertThat(sesion.intentar("update identidad.negocio_ref set giro = 'MUSEO' where negocio_id = ?", fonda))
                    .isEqualTo("42501");
            assertThat(sesion.intentar("update identidad.negocio_ref set estado = 'CERRADO' where negocio_id = ?", fonda))
                    .isNull();
            assertThat(sesion.intentar("delete from identidad.negocio_ref where negocio_id = ?", fonda)).isEqualTo("42501");
        }
    }
}
