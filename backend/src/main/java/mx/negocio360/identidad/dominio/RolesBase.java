package mx.negocio360.identidad.dominio;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;

/**
 * Los ocho roles base y la plantilla de cada giro (D-69, AM-09). Es la fuente de estos datos para la aplicacion; la
 * migracion V022 los siembra en la base y una prueba comprueba que los dos coinciden. Los permisos de Inventario se
 * agregan cuando ese modulo este autorizado (IDN-4.4). Los de Negocios siguen la matriz de la seccion 7.7 de
 * {@code docs/diseno/negocios.md} (D-96, cambio 5): todos ven el perfil del negocio y cada rol suma lo suyo.
 * Un permiso que ningun modulo declara impide el arranque (AZ-03, {@link #verificar}).
 */
public final class RolesBase {

    private static final List<String> SOLO_RESPONSABLE = List.of("clientes.aviso.publicar", "clientes.cliente.anonimizar",
            "clientes.arco.gestionar", "clientes.datos.exportar");
    private static final List<String> EQUIPO_Y_AUDITORIA = List.of("identidad.equipo.ver", "identidad.miembro.invitar",
            "identidad.miembro.cambiar_rol", "identidad.miembro.revocar", "identidad.auditoria.ver");
    private static final List<String> OPERACION_DE_CLIENTES = List.of("clientes.sugerir", "clientes.lista.ver",
            "clientes.panorama.ver", "clientes.ficha.ver", "clientes.cliente.registrar", "clientes.cliente.editar",
            "clientes.consentimiento.registrar");

    /** Permisos de Negocios: todos ven el perfil; el administrador opera el negocio y su personal; el dueno ademas lo pausa y lo cierra. */
    private static final List<String> NEGOCIOS_VER = List.of("negocios.perfil.ver");
    private static final List<String> NEGOCIOS_ADMINISTRADOR = List.of("negocios.perfil.ver", "negocios.perfil.editar",
            "negocios.sucursal.gestionar", "negocios.horario.editar", "negocios.negocio.publicar",
            "negocios.disponibilidad.editar", "negocios.empleado.ver", "negocios.empleado.registrar",
            "negocios.empleado.editar", "negocios.empleado.baja", "negocios.resena.ver", "negocios.resena.responder");
    private static final List<String> NEGOCIOS_SOLO_DUENO = List.of("negocios.negocio.pausar", "negocios.negocio.cerrar");
    private static final List<String> NEGOCIOS_GERENTE = List.of("negocios.perfil.ver", "negocios.horario.editar",
            "negocios.disponibilidad.editar", "negocios.empleado.ver", "negocios.resena.ver", "negocios.resena.responder");

    private static final List<Rol> TODOS = List.of(
            rol("DUENO", "Dueño", concatenar(EQUIPO_Y_AUDITORIA, OPERACION_DE_CLIENTES, SOLO_RESPONSABLE,
                    NEGOCIOS_ADMINISTRADOR, NEGOCIOS_SOLO_DUENO)),
            rol("ADMINISTRADOR", "Administrador", concatenar(EQUIPO_Y_AUDITORIA, OPERACION_DE_CLIENTES,
                    NEGOCIOS_ADMINISTRADOR)),
            rol("GERENTE", "Gerente", concatenar(List.of("identidad.equipo.ver", "clientes.sugerir", "clientes.lista.ver",
                    "clientes.panorama.ver", "clientes.ficha.ver", "clientes.cliente.registrar", "clientes.cliente.editar",
                    "clientes.consentimiento.registrar"), NEGOCIOS_GERENTE)),
            rol("CAJERO", "Cajero", concatenar(List.of("clientes.sugerir", "clientes.cliente.registrar",
                    "clientes.consentimiento.registrar"), NEGOCIOS_VER)),
            rol("MESERO", "Mesero", concatenar(List.of("clientes.sugerir"), NEGOCIOS_VER)),
            rol("COCINERO", "Cocinero", NEGOCIOS_VER),
            rol("TAQUILLERO", "Taquillero", NEGOCIOS_VER),
            rol("GUIA", "Guía", NEGOCIOS_VER));

    private static final Map<Giro, List<String>> PLANTILLAS = Map.of(
            Giro.RESTAURANTE, List.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "MESERO", "COCINERO"),
            Giro.MISCELANEA, List.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO"),
            Giro.MUSEO, List.of("DUENO", "ADMINISTRADOR", "GERENTE", "TAQUILLERO", "GUIA"));

    private RolesBase() {
    }

    private static Rol rol(String codigo, String nombre, List<String> permisos) {
        return new Rol(codigo, nombre, Set.copyOf(permisos));
    }

    @SafeVarargs
    private static List<String> concatenar(List<String>... listas) {
        return java.util.Arrays.stream(listas).flatMap(List::stream).toList();
    }

    /** Los roles base, en el orden del diseño. */
    public static List<Rol> todos() {
        return TODOS;
    }

    public static Optional<Rol> porCodigo(String codigo) {
        return TODOS.stream().filter(r -> r.codigo().equals(codigo)).findFirst();
    }

    /** Codigos de los roles que ofrece ese giro, en el orden de la plantilla. */
    public static List<String> plantilla(Giro giro) {
        return PLANTILLAS.get(giro);
    }

    /**
     * AZ-03: comprueba que cada permiso de cada rol este declarado por algun modulo.
     *
     * @throws IllegalStateException si falta alguno; el mensaje nombra los permisos, que no son datos personales
     */
    public static void verificar(Collection<PermisoDeclarado> catalogo) {
        var declarados = catalogo.stream().map(PermisoDeclarado::codigo).collect(Collectors.toSet());
        var faltantes = new TreeSet<String>();
        for (var rol : TODOS) {
            for (var permiso : rol.permisos()) {
                if (!declarados.contains(permiso)) {
                    faltantes.add(permiso);
                }
            }
        }
        if (!faltantes.isEmpty()) {
            throw new IllegalStateException("Los roles base citan permisos que ningun modulo declara: " + faltantes);
        }
    }
}
