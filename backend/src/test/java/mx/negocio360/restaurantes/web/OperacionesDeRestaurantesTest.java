package mx.negocio360.restaurantes.web;

import static mx.negocio360.compartido.graphql.TipoDeOperacion.MUTATION;
import static mx.negocio360.compartido.graphql.TipoDeOperacion.QUERY;
import static mx.negocio360.compartido.graphql.TipoDeOperacion.SUBSCRIPTION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import mx.negocio360.compartido.graphql.Acceso;
import mx.negocio360.compartido.graphql.ComandoDeclarado;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SG-21, SG-03 y SG-04 para Restaurantes: el inventario de sus operaciones de GraphQL y lo que exige cada una. El panel pide
 * el mismo permiso que ya exige su servicio; la unica operacion sin sesion es el menu publico del turista.
 */
class OperacionesDeRestaurantesTest {

    private final OperacionesDeRestaurantes declaraciones = new OperacionesDeRestaurantes();

    private Map<String, OperacionDeclarada> porNombre() {
        var mapa = new LinkedHashMap<String, OperacionDeclarada>();
        declaraciones.operaciones().forEach(o -> mapa.put(o.nombre(), o));
        return mapa;
    }

    private static String regla(OperacionDeclarada o) {
        return o.acceso() + (o.permiso() == null ? "" : ":" + o.permiso());
    }

    @Test
    @DisplayName("cada operacion del panel pide el mismo permiso que su servicio")
    void panel_permisos() {
        var esperado = new LinkedHashMap<String, String>();
        esperado.put("Query.categoriasDeMenu", "NEGOCIO:" + PermisosDeRestaurantes.MENU_VER);
        esperado.put("Query.platillos", "NEGOCIO:" + PermisosDeRestaurantes.MENU_VER);
        esperado.put("Query.platillo", "NEGOCIO:" + PermisosDeRestaurantes.MENU_VER);
        esperado.put("Query.resumenDeMenu", "NEGOCIO:" + PermisosDeRestaurantes.MENU_VER);
        esperado.put("Mutation.crearCategoria", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.editarCategoria", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.crearPlatillo", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.editarPlatillo", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.publicarPlatillo", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.cambiarModoDeInventario", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.marcarAgotado", "NEGOCIO:" + PermisosDeRestaurantes.MENU_AGOTAR);
        esperado.put("Query.consumosFallidos", "NEGOCIO:" + PermisosDeRestaurantes.CONSUMO_VER);
        esperado.put("Query.receta", "NEGOCIO:" + PermisosDeRestaurantes.RECETA_VER);
        esperado.put("Query.costoDeReceta", "NEGOCIO:" + PermisosDeRestaurantes.COSTO_VER);
        esperado.put("Mutation.desactivarReceta", "NEGOCIO:" + PermisosDeRestaurantes.RECETA_EDITAR);
        esperado.put("Query.salon", "NEGOCIO:" + PermisosDeRestaurantes.SALON_VER);
        esperado.put("Query.qrDeMesa", "NEGOCIO:" + PermisosDeRestaurantes.SALON_CONFIGURAR);
        for (var configurar : new String[] {"crearZona", "darDeBajaZona", "crearMesa", "editarMesa", "darDeBajaMesa",
                "configurarModoDeServicio", "regenerarQrDeMesa"}) {
            esperado.put("Mutation." + configurar, "NEGOCIO:" + PermisosDeRestaurantes.SALON_CONFIGURAR);
        }
        for (var operar : new String[] {"bloquearMesa", "abrirServicio", "cerrarServicio", "pausarQrDeMesa", "habilitarQr",
                "liberarMesa"}) {
            esperado.put("Mutation." + operar, "NEGOCIO:" + PermisosDeRestaurantes.SERVICIO_OPERAR);
        }
        for (var verCuentas : new String[] {"cuenta", "cuentasAbiertas", "cuentaParaCobro"}) {
            esperado.put("Query." + verCuentas, "NEGOCIO:" + PermisosDeRestaurantes.CUENTA_VER);
        }
        esperado.put("Query.comandas", "NEGOCIO:" + PermisosDeRestaurantes.COMANDA_VER);
        for (var abrir : new String[] {"abrirCuenta", "agregarLinea", "quitarLineaPendiente", "enviarACocina", "pedirCuenta",
                "reabrirCuenta", "confirmarPresencia"}) {
            esperado.put("Mutation." + abrir, "NEGOCIO:" + PermisosDeRestaurantes.CUENTA_ABRIR);
        }
        esperado.put("Mutation.anularLinea", "NEGOCIO:" + PermisosDeRestaurantes.LINEA_ANULAR);
        esperado.put("Mutation.cancelarCuenta", "NEGOCIO:" + PermisosDeRestaurantes.CUENTA_CANCELAR);
        esperado.put("Mutation.avanzarComanda", "NEGOCIO:" + PermisosDeRestaurantes.COMANDA_VER);
        esperado.put("Mutation.devolverComanda", "NEGOCIO:" + PermisosDeRestaurantes.COMANDA_PREPARAR);
        esperado.put("Subscription.comandasDeCocina", "NEGOCIO:" + PermisosDeRestaurantes.COMANDA_VER);
        esperado.put("Subscription.salonActualizado", "NEGOCIO:" + PermisosDeRestaurantes.SALON_VER);

        var real = porNombre();
        esperado.forEach((nombre, regla) -> assertThat(real).as(nombre).containsKey(nombre));
        esperado.forEach((nombre, regla) -> assertThat(regla(real.get(nombre))).as(nombre).isEqualTo(regla));
    }

    @Test
    @DisplayName("SG-01: Restaurantes publica el menu del turista y las cuatro operaciones del comensal por QR, y nada mas")
    void publicas() {
        var publicas = porNombre().values().stream().filter(o -> o.acceso() == Acceso.PUBLICA)
                .map(OperacionDeclarada::nombre).sorted().toList();

        assertThat(publicas).containsExactly("Mutation.pedirCuentaPorQr", "Mutation.pedirPorQr", "Query.estadoDePedidoQr",
                "Query.menuPublico", "Query.mesaPorQr");
    }

    @Test
    @DisplayName("Restaurantes declara todas las operaciones de su esquema y ninguna otra")
    void inventarioCompleto() {
        assertThat(porNombre().keySet()).containsExactlyInAnyOrder("Query.categoriasDeMenu", "Query.platillos",
                "Query.platillo", "Query.resumenDeMenu", "Query.menuPublico", "Mutation.crearCategoria",
                "Mutation.editarCategoria", "Mutation.crearPlatillo", "Mutation.editarPlatillo",
                "Mutation.publicarPlatillo", "Mutation.cambiarModoDeInventario", "Mutation.marcarAgotado", "Query.receta",
                "Query.costoDeReceta", "Mutation.desactivarReceta", "Query.consumosFallidos", "Query.salon", "Query.qrDeMesa",
                "Mutation.crearZona", "Mutation.darDeBajaZona", "Mutation.crearMesa", "Mutation.editarMesa",
                "Mutation.darDeBajaMesa", "Mutation.configurarModoDeServicio", "Mutation.regenerarQrDeMesa",
                "Mutation.bloquearMesa", "Mutation.abrirServicio", "Mutation.cerrarServicio", "Mutation.pausarQrDeMesa",
                "Mutation.habilitarQr", "Query.cuenta", "Query.cuentasAbiertas", "Query.cuentaParaCobro", "Query.comandas",
                "Mutation.abrirCuenta", "Mutation.agregarLinea", "Mutation.quitarLineaPendiente", "Mutation.enviarACocina",
                "Mutation.pedirCuenta", "Mutation.reabrirCuenta", "Mutation.confirmarPresencia", "Mutation.anularLinea",
                "Mutation.cancelarCuenta", "Mutation.liberarMesa", "Mutation.avanzarComanda", "Mutation.devolverComanda",
                "Query.mesaPorQr", "Query.estadoDePedidoQr", "Mutation.pedirPorQr", "Mutation.pedirCuentaPorQr",
                "Subscription.comandasDeCocina", "Subscription.salonActualizado");
    }

    @Test
    @DisplayName("todo permiso que pide una operacion esta en el catalogo de permisos de Restaurantes")
    void permisosDelCatalogo() {
        var catalogo = new PermisosDeRestaurantes().permisos().stream().map(PermisoDeclarado::codigo).toList();

        porNombre().values().stream().map(OperacionDeclarada::permiso).filter(p -> p != null)
                .forEach(permiso -> assertThat(catalogo).contains(permiso));
    }

    @Test
    @DisplayName("solo hay consultas, mutaciones directas y las dos suscripciones del personal, mas las cuatro variantes de comando del despachador (D-209)")
    void tipos() {
        assertThat(porNombre().values().stream().map(OperacionDeclarada::tipo).distinct().toList())
                .containsExactlyInAnyOrder(QUERY, MUTATION, SUBSCRIPTION);
        assertThat(declaraciones.comandos()).extracting(ComandoDeclarado::variante)
                .containsExactlyInAnyOrder("guardarReceta", "darDeBajaPlatillo", "anularLineaPreparada", "cancelarCuentaPreparada");
    }

    @Test
    @DisplayName("D-209: guardarReceta pide receta.editar y darDeBajaPlatillo pide menu.editar, y ambos estan en el catalogo")
    void permisosDeLosComandos() {
        var catalogo = new PermisosDeRestaurantes().permisos().stream().map(PermisoDeclarado::codigo).toList();

        assertThat(declaraciones.comandos()).filteredOn(c -> c.variante().equals("guardarReceta"))
                .extracting(ComandoDeclarado::permiso).containsExactly(PermisosDeRestaurantes.RECETA_EDITAR);
        assertThat(declaraciones.comandos()).filteredOn(c -> c.variante().equals("darDeBajaPlatillo"))
                .extracting(ComandoDeclarado::permiso).containsExactly(PermisosDeRestaurantes.MENU_EDITAR);
        declaraciones.comandos().forEach(c -> assertThat(catalogo).contains(c.permiso()));
    }
}
