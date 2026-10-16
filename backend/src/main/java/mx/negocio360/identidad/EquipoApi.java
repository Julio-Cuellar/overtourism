package mx.negocio360.identidad;

import java.util.List;
import java.util.UUID;

/**
 * Administracion del equipo de un negocio (IDN-2.5 y IDN-2.6). Todas las operaciones actuan sobre el negocio activo de
 * la sesion (AZ-05) y piden su permiso. Quien actua es el token; el destino es otra cuenta del mismo negocio.
 * Por ahora no tienen REST: las acciones sensibles necesitan antes la confirmacion reforzada (IDN-3.2, D-73).
 */
public interface EquipoApi {

    /** Personas del negocio activo, activas y suspendidas. Exige {@code identidad.equipo.ver}. */
    List<MiembroVista> equipo(String token);

    /** Cambia el rol de un miembro. Exige {@code identidad.miembro.cambiar_rol}. */
    MiembroVista cambiarRol(String token, UUID cuentaId, String rolCodigo);

    /** Suspende a un miembro y revoca sus sesiones en este negocio. Exige {@code identidad.miembro.revocar}. */
    MiembroVista suspender(String token, UUID cuentaId);

    /** Devuelve a ACTIVA a un miembro suspendido. Exige {@code identidad.miembro.revocar}. */
    MiembroVista reactivar(String token, UUID cuentaId);

    /** Quita a un miembro del negocio y revoca sus sesiones aqui. Exige {@code identidad.miembro.revocar}. */
    void revocar(String token, UUID cuentaId);

    /** La persona sale del negocio. No puede salir el ultimo dueño activo. */
    void salir(String token);
}
