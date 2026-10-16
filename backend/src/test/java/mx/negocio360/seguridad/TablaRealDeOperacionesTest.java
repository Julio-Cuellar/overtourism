package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Collectors;
import mx.negocio360.clientes.graphql.OperacionesDeClientes;
import mx.negocio360.compartido.graphql.Acceso;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesDePlataforma;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.compartido.graphql.TablaDeOperaciones;
import mx.negocio360.compartido.graphql.TipoDeOperacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * SG-21: el inventario de operaciones de la plataforma, tal como arranca. Fija las decisiones de permisos por operacion
 * para que cambiarlas sea una decision a la vista y no un descuido. (Que no falte ninguna lo garantiza el arranque.)
 */
@SpringBootTest
class TablaRealDeOperacionesTest {

    @Autowired
    private TablaDeOperaciones tabla;

    /** Los permisos de lo que declara una fuente, tal como quedo en la tabla real. */
    private Map<String, String> permisosDe(TipoDeOperacion tipo, OperacionesGraphql fuente) {
        return fuente.operaciones().stream().filter(o -> o.tipo() == tipo)
                .map(o -> tabla.operacion(o.tipo(), o.campo()).orElseThrow())
                .collect(Collectors.toMap(OperacionDeclarada::campo, o -> String.valueOf(o.permiso())));
    }

    /** Las operaciones de la tabla que no son de pruebas, con su acceso. */
    private Map<String, Acceso> accesos() {
        return tabla.operaciones().stream().filter(o -> !o.campo().endsWith("DePrueba"))
                .collect(Collectors.toMap(OperacionDeclarada::nombre, OperacionDeclarada::acceso));
    }

    @Test
    @DisplayName("las 8 consultas de Clientes y el permiso de cada una")
    void consultasDeClientes() {
        assertThat(permisosDe(TipoDeOperacion.QUERY, new OperacionesDeClientes())).isEqualTo(Map.of(
                "clientes", "clientes.lista.ver",
                "cliente", "clientes.ficha.ver",
                "historialCliente", "clientes.ficha.ver",
                "consentimientosCliente", "clientes.ficha.ver",
                "sugerirClientes", "clientes.sugerir",
                "panoramaClientes", "clientes.panorama.ver",
                "avisoPrivacidadVigente", "clientes.cliente.registrar",
                "solicitudesArco", "clientes.arco.gestionar"));
    }

    @Test
    @DisplayName("las mutaciones de la plataforma no llevan permiso propio; prepararComando decide por la variante del comando")
    void mutacionesDeLaPlataforma() {
        assertThat(permisosDe(TipoDeOperacion.MUTATION, new OperacionesDePlataforma())).isEqualTo(Map.of(
                "prepararComando", "null", "confirmarComando", "null"));
        assertThat(tabla.operacion(TipoDeOperacion.MUTATION, "prepararComando")).hasValueSatisfying(
                o -> assertThat(o.argumentoDeComando()).isEqualTo("input"));
        assertThat(tabla.operacion(TipoDeOperacion.MUTATION, "confirmarComando")).hasValueSatisfying(
                o -> assertThat(o.argumentoDeComando()).isNull());
    }

    @Test
    @DisplayName("las 8 variantes de comando de Clientes y su permiso")
    void variantesDeClientes() {
        assertThat(delModulo("clientes.")).isEqualTo(Map.of(
                "registrarCliente", "clientes.cliente.registrar",
                "editarCliente", "clientes.cliente.editar",
                "registrarConsentimiento", "clientes.consentimiento.registrar",
                "anonimizarCliente", "clientes.cliente.anonimizar",
                "publicarAvisoPrivacidad", "clientes.aviso.publicar",
                "registrarSolicitudArco", "clientes.arco.gestionar",
                "ampliarSolicitudArco", "clientes.arco.gestionar",
                "resolverSolicitudArco", "clientes.arco.gestionar"));
    }

    @Test
    @DisplayName("las 7 variantes de comando de Ventas y Caja y su permiso (seccion 7.3 del plan de integracion)")
    void variantesDeVentasYCaja() {
        assertThat(delModulo("ventas.")).isEqualTo(Map.of(
                "registrarVenta", "ventas.venta.registrar",
                "anularVenta", "ventas.venta.anular"));
        assertThat(delModulo("caja.")).isEqualTo(Map.of(
                "crearCaja", "caja.caja.gestionar",
                "abrirTurno", "caja.turno.abrir",
                "registrarMovimientoCaja", "caja.movimiento.registrar",
                "cerrarTurno", "caja.corte.cerrar",
                "descartarCorte", "caja.corte.cerrar"));
    }

    @Test
    @DisplayName("las consultas de Ventas y Caja piden su permiso y ninguna es publica ni de cuenta")
    void consultasDeVentasYCaja() {
        var esperado = Map.of("venta", "ventas.venta.ver", "ventas", "ventas.venta.ver",
                "resumenTurno", "ventas.venta.ver", "productosParaVenta", "ventas.venta.registrar",
                "cajas", "caja.turno.ver", "turnoActivo", "caja.turno.ver", "saldoTurno", "caja.turno.ver",
                "cortes", "caja.corte.ver");
        esperado.forEach((campo, permiso) -> assertThat(tabla.operacion(TipoDeOperacion.QUERY, campo))
                .as(campo).hasValueSatisfying(o -> {
                    assertThat(o.permiso()).isEqualTo(permiso);
                    assertThat(o.acceso()).isEqualTo(Acceso.NEGOCIO);
                }));
    }

    /** Las variantes cuyo permiso empieza con ese prefijo de modulo. */
    private Map<String, String> delModulo(String prefijo) {
        return tabla.variantes().entrySet().stream().filter(e -> e.getValue().startsWith(prefijo))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    @DisplayName("los permisos sensibles de Clientes son los que la tabla marca como tales")
    void sensibles() {
        assertThat(tabla.sensible("clientes.cliente.anonimizar")).isTrue();
        assertThat(tabla.sensible("clientes.aviso.publicar")).isTrue();
        assertThat(tabla.sensible("clientes.arco.gestionar")).isTrue();
        assertThat(tabla.sensible("clientes.datos.exportar")).isTrue();
        assertThat(tabla.sensible("clientes.sugerir")).isFalse();
        assertThat(tabla.sensible("clientes.cliente.registrar")).isFalse();
    }

    @Test
    @DisplayName("SG-01: las unicas operaciones sin sesion de toda la plataforma son estas; agregar una es una decision a la vista")
    void operacionesPublicas() {
        var publicas = accesos().entrySet().stream().filter(e -> e.getValue() == Acceso.PUBLICA)
                .map(Map.Entry::getKey).sorted().toList();

        assertThat(publicas).containsExactly("Mutation.confirmarComando", "Query.negocioPublico",
                "Query.negociosPublicos", "Query.resenasDeNegocio");
        assertThat(tabla.variantesPublicas()).containsExactly("publicarResena");
    }

    @Test
    @DisplayName("las operaciones que piden solo una cuenta, sin negocio activo, son estas")
    void operacionesDeCuenta() {
        var deCuenta = accesos().entrySet().stream().filter(e -> e.getValue() == Acceso.CUENTA)
                .map(Map.Entry::getKey).sorted().toList();

        assertThat(deCuenta).containsExactly("Query.misAltasDeNegocio", "Subscription.altaDeNegocio");
    }

    @Test
    @DisplayName("todo lo demas pide una sesion con negocio activo y, casi siempre, un permiso")
    void lasDemasPidenNegocio() {
        accesos().forEach((nombre, acceso) -> {
            if (acceso == Acceso.NEGOCIO) {
                assertThat(tabla.operaciones().stream().filter(o -> o.nombre().equals(nombre)).findFirst())
                        .as(nombre).isPresent();
            }
        });
        assertThat(accesos().values().stream().filter(a -> a == Acceso.NEGOCIO).count()).isGreaterThan(20);
    }
}
