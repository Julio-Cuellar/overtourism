/**
 * Modulo horizontal de Identidad (D-66): autenticacion, sesiones, equipo y permisos del personal de los negocios.
 * Solo puede depender de {@code compartido}: no importa a ningun modulo de negocio. Los demas modulos reciben el
 * contexto y los permisos por interfaces de {@code compartido} (R1, R4 y R6). Los turistas no se autentican aqui.
 */
@ApplicationModule(displayName = "Identidad", allowedDependencies = "compartido")
package mx.negocio360.identidad;

import org.springframework.modulith.ApplicationModule;
