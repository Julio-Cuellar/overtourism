package mx.negocio360.identidad.infraestructura;

import mx.negocio360.identidad.EquipoApi;
import mx.negocio360.identidad.IdentidadApi;

/**
 * Las dos APIs de Identidad juntas. El servicio las implementa en un solo objeto y esta interfaz permite envolverlo
 * en una frontera transaccional que conserve ambas; los controladores siguen pidiendo la que usan.
 */
public interface IdentidadFachada extends IdentidadApi, EquipoApi {
}
