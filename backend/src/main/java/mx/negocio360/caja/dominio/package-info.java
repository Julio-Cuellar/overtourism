/**
 * Dominio de Caja: cajas, turnos, movimientos, cortes y codigos de error. Se expone como interfaz con nombre
 * {@code dominio} porque {@code CajaApi} devuelve estos tipos y los fallos llegan como
 * {@link mx.negocio360.caja.dominio.CajaException}.
 */
@NamedInterface("dominio")
package mx.negocio360.caja.dominio;

import org.springframework.modulith.NamedInterface;
