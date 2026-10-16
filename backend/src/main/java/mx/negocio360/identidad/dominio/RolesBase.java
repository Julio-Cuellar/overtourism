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
 * agregan cuando ese modulo este autorizado (IDN-4.4); por eso cocinero, taquillero y guia no tienen permisos todavia.
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

    private static final List<Rol> TODOS = List.of(
            rol("DUENO", "Dueño", concatenar(EQUIPO_Y_AUDITORIA, OPERACION_DE_CLIENTES, SOLO_RESPONSABLE)),
            rol("ADMINISTRADOR", "Administrador", concatenar(EQUIPO_Y_AUDITORIA, OPERACION_DE_CLIENTES)),
            rol("GERENTE", "Gerente", List.of("identidad.equipo.ver", "clientes.sugerir", "clientes.lista.ver",
                    "clientes.panorama.ver", "clientes.ficha.ver", "clientes.cliente.registrar", "clientes.cliente.editar",
                    "clientes.consentimiento.registrar")),
            rol("CAJERO", "Cajero", List.of("clientes.sugerir", "clientes.cliente.registrar",
                    "clientes.consentimiento.registrar")),
            rol("MESERO", "Mesero", List.of("clientes.sugerir")),
            rol("COCINERO", "Cocinero", List.of()),
            rol("TAQUILLERO", "Taquillero", List.of()),
            rol("GUIA", "Guía", List.of()));

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
