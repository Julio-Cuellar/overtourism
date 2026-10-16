package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.restaurantes.dominio.RecetaVersion;

/**
 * El costo teorico de una version de receta (RC-05, RC-06, D-167, D-168): suma, por insumo, la cantidad en unidad base por
 * {@code (1 + merma/100)} por el CPP, la reparte entre las porciones del rendimiento y la redondea una sola vez a dos
 * decimales (HALF_UP, D-20). Si a un insumo le falta el costo (articulo que ya no esta, cantidad que no se pudo convertir o
 * CPP nulo o cero), el resultado es incompleto y no se inventa nada.
 */
final class CalculadoraDeCosto {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private CalculadoraDeCosto() {
    }

    /**
     * Calcula el costo de una version.
     *
     * @param platilloId platillo de la receta
     * @param version    version a costear
     * @param precio     precio vigente del platillo
     * @param articulos  datos de Inventario de los articulos que existen
     * @param bases      cantidades ya convertidas a la unidad base; faltan las que no se pudieron convertir
     */
    static CostoDeReceta calcular(UUID platilloId, RecetaVersion version, BigDecimal precio,
                                  Map<UUID, ArticuloDeInventario> articulos, Map<UUID, CantidadEnBase> bases) {
        var lineas = new ArrayList<CostoDeInsumo>();
        var sinCosto = new ArrayList<UUID>();
        var total = BigDecimal.ZERO;
        for (var insumo : version.insumos()) {
            var articulo = articulos.get(insumo.articuloId());
            var base = bases.get(insumo.articuloId());
            var cpp = articulo == null ? null : articulo.cpp();
            BigDecimal costo = null;
            if (base != null && cpp != null && cpp.signum() > 0) {
                var factor = BigDecimal.ONE.add(insumo.mermaPct().divide(CIEN, 4, Redondeo.MODO));
                var exacto = base.cantidadBase().multiply(factor).multiply(cpp);
                total = total.add(exacto);
                costo = exacto.setScale(4, Redondeo.MODO);
            } else {
                sinCosto.add(insumo.articuloId());
            }
            lineas.add(new CostoDeInsumo(insumo.articuloId(), articulo == null ? null : articulo.nombre(),
                    base == null ? null : base.cantidadBase(), base == null ? null : base.unidadBase(), insumo.mermaPct(),
                    cpp, costo));
        }
        var precioFinal = Redondeo.dinero(precio);
        if (!sinCosto.isEmpty()) {
            return new CostoDeReceta(platilloId, version.numero(), version.rendimiento(), precioFinal, null, null, null, true,
                    sinCosto, lineas);
        }
        var porPorcion = Redondeo.dinero(Redondeo.dividir(total, BigDecimal.valueOf(version.rendimiento()), 8));
        var margen = precioFinal.subtract(porPorcion);
        var margenPct = Redondeo.dinero(Redondeo.dividir(margen.multiply(CIEN), precioFinal, 8));
        return new CostoDeReceta(platilloId, version.numero(), version.rendimiento(), precioFinal, porPorcion, margen,
                margenPct, false, sinCosto, lineas);
    }
}
