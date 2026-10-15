package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Contenido de una poliza antes de registrarse: tipo, fecha contable, concepto y renglones. Garantiza la partida
 * doble (A-01): al menos dos renglones y suma de cargos igual a suma de abonos.
 *
 * @param tipo      tipo de poliza
 * @param fecha     fecha contable, en la zona de Mexico
 * @param concepto  descripcion de la poliza
 * @param renglones renglones; se copian a una lista inmutable
 * @throws ContabilidadException {@code POLIZA_SIN_RENGLONES} si hay menos de dos renglones;
 *                               {@code POLIZA_DESCUADRADA} si los cargos no suman lo mismo que los abonos
 */
public record Asiento(TipoPoliza tipo, LocalDate fecha, String concepto, List<Renglon> renglones) {

    private static final int MINIMO_DE_RENGLONES = 2;

    public Asiento {
        if (renglones == null || renglones.size() < MINIMO_DE_RENGLONES) {
            throw new ContabilidadException(CodigoError.POLIZA_SIN_RENGLONES);
        }
        renglones = List.copyOf(renglones);
        var cargos = renglones.stream().map(Renglon::cargo).reduce(BigDecimal.ZERO, BigDecimal::add);
        var abonos = renglones.stream().map(Renglon::abono).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (cargos.compareTo(abonos) != 0) {
            throw new ContabilidadException(CodigoError.POLIZA_DESCUADRADA);
        }
    }

    /**
     * Total de la poliza: la suma de los cargos, que es igual a la de los abonos.
     *
     * @return el total
     */
    public BigDecimal total() {
        return renglones.stream().map(Renglon::cargo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
