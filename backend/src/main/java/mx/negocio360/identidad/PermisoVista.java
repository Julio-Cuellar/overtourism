package mx.negocio360.identidad;

/** Un permiso del catalogo, con el modulo que lo declara y si exige confirmacion reforzada. */
public record PermisoVista(String codigo, String modulo, String descripcion, boolean sensible) {
}
