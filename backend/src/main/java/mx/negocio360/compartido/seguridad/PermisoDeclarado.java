package mx.negocio360.compartido.seguridad;

import java.util.regex.Pattern;

/**
 * Un permiso que declara un modulo (AZ-02, D-70): {@code modulo.recurso.accion} o {@code modulo.accion}, en minusculas.
 * Los roles de Identidad los agrupan; Identidad no importa a los modulos, solo recibe esta descripcion como dato.
 *
 * @param codigo       por ejemplo {@code clientes.cliente.anonimizar}
 * @param descripcion  texto para la pantalla de roles
 * @param sensible     si exige ademas una confirmacion reforzada (D-73)
 */
public record PermisoDeclarado(String codigo, String descripcion, boolean sensible) {

    private static final Pattern FORMA = Pattern.compile("[a-z]+(\\.[a-z_]+){1,2}");

    public PermisoDeclarado {
        if (codigo == null || !FORMA.matcher(codigo).matches()) {
            throw new IllegalArgumentException("El codigo del permiso debe tener la forma modulo.recurso.accion");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("El permiso necesita una descripcion");
        }
    }

    /** El modulo que lo declara: el primer segmento del codigo. */
    public String modulo() {
        return codigo.substring(0, codigo.indexOf('.'));
    }
}
