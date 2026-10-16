/**
 * Modulo vertical de Restaurantes (docs/diseno/restaurantes.md). Menu, recetas con costo teorico, descuento de insumos
 * al vender, servicio en salon y menu publico.
 *
 * <p>Depende de {@code compartido} (la plataforma) y de {@code inventario} (recetas contra {@code InventarioApi}, D-09).
 * Nadie depende de el (R4, D-160): ni Ventas ni Inventario lo conocen; le avisan por eventos. Sus «submodulos» son
 * paquetes internos y no modulos de Spring Modulith (D-161).
 *
 * <p>Esta primera fase (R0) solo declara sus permisos y su lugar en la estructura de modulos.
 */
@ApplicationModule(displayName = "Restaurantes", allowedDependencies = {"compartido", "inventario"})
package mx.negocio360.restaurantes;

import org.springframework.modulith.ApplicationModule;
