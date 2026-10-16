package mx.negocio360.restaurantes.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Un consumo que Inventario no pudo descontar por un error permanente (CM-06). Se guarda para que alguien lo revise; no se
 * reintenta solo. No lleva datos personales: ni la venta ni quien cobro.
 *
 * @param id         identificador
 * @param negocioId  negocio
 * @param ventaId    venta que lo causo
 * @param codigo     {@code SIN_RECETA} o el codigo de Inventario, por ejemplo {@code ARTICULO_INACTIVO}
 * @param lineas     platillos afectados
 * @param ocurridoEn cuando ocurrio la venta
 * @param creadoEn   cuando se registro el fallo
 */
public record ConsumoFallido(UUID id, UUID negocioId, UUID ventaId, String codigo, List<LineaFallida> lineas,
                             Instant ocurridoEn, Instant creadoEn) {

    public ConsumoFallido {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(ventaId, "ventaId");
        Objects.requireNonNull(codigo, "codigo");
        lineas = List.copyOf(Objects.requireNonNull(lineas, "lineas"));
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(creadoEn, "creadoEn");
    }

    /** El texto fijo para la persona; un codigo que no se conoce tiene un texto general, nunca el codigo en crudo. */
    public String mensaje() {
        return switch (codigo) {
            case ConsumidorDeVentas.SIN_RECETA -> "El platillo no tenía receta cuando se vendió.";
            case "ARTICULO_INACTIVO" -> "Un artículo de la receta ya no está activo en el inventario.";
            case "UNIDAD_NO_CONVERTIBLE" -> "La unidad de un insumo ya no se puede convertir a la del artículo.";
            case "ARTICULO_NO_ENCONTRADO" -> "Un artículo de la receta ya no existe en el inventario.";
            default -> "Inventario no pudo descontar los insumos de esta venta.";
        };
    }
}
