/**
 * Modulo horizontal de Negocios (D-87): la fuente de todo lo que se sabe de un negocio (categoria, subcategorias,
 * atributos, ubicacion, horario, disponibilidad, estado y, mas adelante, reseñas y personal). Solo puede depender de
 * {@code compartido}: no llama a la API de ningun otro modulo, escucha los eventos de Identidad y publica los suyos
 * (R1, R2, R4 y R6). Los verticales se cuelgan de su {@code negocio_id} y este modulo no los conoce.
 */
@ApplicationModule(displayName = "Negocios", allowedDependencies = "compartido")
package mx.negocio360.negocios;

import org.springframework.modulith.ApplicationModule;
