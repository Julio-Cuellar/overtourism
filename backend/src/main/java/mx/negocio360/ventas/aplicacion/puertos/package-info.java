/**
 * Puertos de Ventas hacia modulos que aun no existen (Catalogo, Pasaporte, permisos) y hacia su almacen. Se exponen
 * como interfaz con nombre {@code puertos} para que un adaptador de otro modulo los implemente (en la demo,
 * {@code canal.demo}) sin tocar el resto de {@code ventas.aplicacion}.
 */
@NamedInterface("puertos")
package mx.negocio360.ventas.aplicacion.puertos;

import org.springframework.modulith.NamedInterface;
