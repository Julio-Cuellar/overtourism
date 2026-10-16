/**
 * Puertos de Caja hacia su almacen, reglas y permisos. Se exponen como interfaz con nombre {@code puertos} para que un
 * adaptador de otro modulo los implemente (en la demo, {@code canal.demo}).
 */
@NamedInterface("puertos")
package mx.negocio360.caja.aplicacion.puertos;

import org.springframework.modulith.NamedInterface;
