package mx.negocio360.negocios.web;

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
import mx.negocio360.negocios.aplicacion.PermisosDeNegocios;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SG-21, SG-03 y SG-04 para Negocios: el inventario de sus operaciones de GraphQL y lo que exige cada una. El panel pide el
 * permiso que ya exige su servicio; la superficie publica del turista y la resena que manda sin cuenta son las unicas
 * operaciones sin sesion, y esta prueba las lista una por una para que ninguna se vuelva publica sin que alguien lo decida.
 */
class OperacionesDeNegociosTest {

    private final OperacionesDeNegocios declaraciones = new OperacionesDeNegocios();

    private Map<String, OperacionDeclarada> porNombre() {
        var mapa = new LinkedHashMap<String, OperacionDeclarada>();
        declaraciones.operaciones().forEach(o -> mapa.put(o.nombre(), o));
        return mapa;
    }

    private static String regla(OperacionDeclarada o) {
        return o.acceso() + (o.permiso() == null ? "" : ":" + o.permiso());
    }

    @Test
    @DisplayName("el panel: cada operacion pide el mismo permiso que ya exige su servicio")
    void panel_permisos() {
        var esperado = new LinkedHashMap<String, String>();
        esperado.put("Query.miNegocio", "NEGOCIO:" + PermisosDeNegocios.PERFIL_VER);
        esperado.put("Query.sucursales", "NEGOCIO:" + PermisosDeNegocios.PERFIL_VER);
        esperado.put("Query.horarioDeSucursal", "NEGOCIO:" + PermisosDeNegocios.PERFIL_VER);
        esperado.put("Query.resenas", "NEGOCIO:" + PermisosDeNegocios.RESENA_VER);
        esperado.put("Mutation.actualizarNegocio", "NEGOCIO:" + PermisosDeNegocios.PERFIL_EDITAR);
        esperado.put("Mutation.definirSubcategorias", "NEGOCIO:" + PermisosDeNegocios.PERFIL_EDITAR);
        esperado.put("Mutation.definirAtributos", "NEGOCIO:" + PermisosDeNegocios.PERFIL_EDITAR);
        esperado.put("Mutation.definirDisponibilidad", "NEGOCIO:" + PermisosDeNegocios.DISPONIBILIDAD_EDITAR);
        esperado.put("Mutation.publicarNegocio", "NEGOCIO:" + PermisosDeNegocios.NEGOCIO_PUBLICAR);
        esperado.put("Mutation.actualizarSucursal", "NEGOCIO:" + PermisosDeNegocios.SUCURSAL_GESTIONAR);
        esperado.put("Mutation.definirHorario", "NEGOCIO:" + PermisosDeNegocios.HORARIO_EDITAR);
        esperado.put("Mutation.agregarExcepcionHorario", "NEGOCIO:" + PermisosDeNegocios.HORARIO_EDITAR);
        esperado.put("Mutation.quitarExcepcionHorario", "NEGOCIO:" + PermisosDeNegocios.HORARIO_EDITAR);
        esperado.put("Mutation.responderResena", "NEGOCIO:" + PermisosDeNegocios.RESENA_RESPONDER);

        var real = porNombre();
        esperado.forEach((nombre, regla) -> assertThat(real).as(nombre).containsKey(nombre));
        esperado.forEach((nombre, regla) -> assertThat(regla(real.get(nombre))).as(nombre).isEqualTo(regla));
    }

    @Test
    @DisplayName("lo que se ve sin negocio es solo del dueno de la cuenta: sus altas y el aviso de alta piden sesion, no negocio")
    void deCuenta() {
        var real = porNombre();

        assertThat(regla(real.get("Query.misAltasDeNegocio"))).isEqualTo("CUENTA");
        assertThat(regla(real.get("Subscription.altaDeNegocio"))).isEqualTo("CUENTA");
    }

    @Test
    @DisplayName("SG-01: las unicas operaciones publicas de Negocios son las tres consultas del turista")
    void publicas_son_exactamente_estas() {
        var publicas = porNombre().values().stream().filter(o -> o.acceso() == Acceso.PUBLICA)
                .map(OperacionDeclarada::nombre).sorted().toList();

        assertThat(publicas).containsExactly("Query.negocioPublico", "Query.negociosPublicos",
                "Query.resenasDeNegocio");
    }

    @Test
    @DisplayName("publicar una resena es la unica variante de comando publica de Negocios, y no hay otras")
    void variantePublica() {
        assertThat(declaraciones.comandos()).containsExactly(ComandoDeclarado.publico("publicarResena"));
    }

    @Test
    @DisplayName("Negocios declara todas las operaciones de su esquema y ninguna otra")
    void inventarioCompleto() {
        assertThat(porNombre().keySet()).containsExactlyInAnyOrder(
                "Query.miNegocio", "Query.sucursales", "Query.horarioDeSucursal", "Query.misAltasDeNegocio",
                "Query.resenas", "Query.negociosPublicos", "Query.negocioPublico", "Query.resenasDeNegocio",
                "Mutation.actualizarNegocio", "Mutation.definirSubcategorias", "Mutation.definirAtributos",
                "Mutation.definirDisponibilidad", "Mutation.publicarNegocio", "Mutation.actualizarSucursal",
                "Mutation.definirHorario", "Mutation.agregarExcepcionHorario", "Mutation.quitarExcepcionHorario",
                "Mutation.responderResena", "Subscription.altaDeNegocio");
    }

    @Test
    @DisplayName("todo permiso que pide una operacion de Negocios esta en su catalogo de permisos")
    void permisosDelCatalogo() {
        var catalogo = new PermisosDeNegocios().permisos().stream().map(PermisoDeclarado::codigo).toList();

        porNombre().values().stream().map(OperacionDeclarada::permiso).filter(p -> p != null)
                .forEach(permiso -> assertThat(catalogo).contains(permiso));
    }

    @Test
    @DisplayName("los tipos de raiz de las declaraciones son los del esquema")
    void tipos() {
        assertThat(porNombre().values().stream().map(OperacionDeclarada::tipo).distinct().toList())
                .containsExactlyInAnyOrder(QUERY, MUTATION, SUBSCRIPTION);
    }
}
