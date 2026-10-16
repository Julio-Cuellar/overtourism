package mx.negocio360.compartido.seguridad;

import java.util.List;

/**
 * Rutas HTTP que un modulo declara abiertas a la cadena de filtros porque validan su propio token (por ejemplo, las de
 * acceso de Identidad). Todo lo que ningun modulo declara exige autenticacion y falla cerrado (SG-01). Igual que
 * {@link CatalogoDePermisos}, cada modulo da su parte y la plataforma no importa a ninguno (R1 y R4).
 */
@FunctionalInterface
public interface RutasPublicas {

    /** Patrones de ruta al estilo de Spring, por ejemplo {@code /auth/**}. */
    List<String> patrones();
}
