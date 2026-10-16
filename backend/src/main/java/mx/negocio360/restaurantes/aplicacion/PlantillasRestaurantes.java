package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import mx.negocio360.restaurantes.dominio.RecetaVersion;

/** Textos fijos de Restaurantes en español de Mexico. Las cifras que muestran las calcula el codigo, nunca un modelo. */
final class PlantillasRestaurantes {

    /** Mensaje al confirmar una receta. */
    static final String RECETA_GUARDADA = "Receta guardada.";
    /** Mensaje al desactivar una receta. */
    static final String RECETA_DESACTIVADA = "Receta desactivada.";

    private PlantillasRestaurantes() {
    }

    /** Una cifra sin ceros de mas: 120, 0.12, 3.5. */
    static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private static String dinero(BigDecimal valor) {
        return "$" + valor.toPlainString();
    }

    /**
     * El resumen de guardar una receta. Sin costos si la persona no tiene permiso de verlos (RC-10).
     *
     * @param nombres nombre de cada articulo, en el orden de los insumos de la version
     * @param costo   costo de la version, o nulo si la persona no puede verlo
     */
    static String resumenReceta(String platillo, RecetaVersion version, List<String> nombres, CostoDeReceta costo) {
        var texto = new StringBuilder("Receta de «").append(platillo).append("» (versión ").append(version.numero())
                .append(", rinde ").append(version.rendimiento()).append(version.rendimiento() == 1 ? " porción" : " porciones")
                .append(").\nInsumos:");
        for (int i = 0; i < version.insumos().size(); i++) {
            var insumo = version.insumos().get(i);
            texto.append("\n- ").append(nombres.get(i)).append(": ").append(numero(insumo.cantidad())).append(' ')
                    .append(insumo.unidad());
            if (insumo.mermaPct().signum() > 0) {
                texto.append(" (merma ").append(numero(insumo.mermaPct())).append(" %)");
            }
        }
        if (costo == null) {
            return texto.toString();
        }
        if (costo.incompleto()) {
            var faltan = costo.insumos().stream().filter(c -> costo.insumosSinCosto().contains(c.articuloId()))
                    .map(c -> c.nombre() == null ? "un artículo" : c.nombre()).collect(Collectors.joining(", "));
            return texto.append("\nCosto teórico incompleto: falta el costo de ").append(faltan).append('.').toString();
        }
        return texto.append("\nCosto teórico por porción: ").append(dinero(costo.costoPorPorcion())).append(". Precio: ")
                .append(dinero(costo.precio())).append(". Margen: ").append(dinero(costo.margen())).append(" (")
                .append(costo.margenPct().toPlainString()).append(" %).").toString();
    }
}
