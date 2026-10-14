/**
 * Modulo horizontal de Clientes (D-47). Solo puede depender de {@code compartido}: no llama a la API
 * de ningun otro modulo y escucha eventos de Ventas (R2, R4 y R6).
 */
@ApplicationModule(displayName = "Clientes", allowedDependencies = "compartido")
package mx.negocio360.clientes;

import org.springframework.modulith.ApplicationModule;
