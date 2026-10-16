package mx.negocio360.inventario.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.dinero.Redondeo;

/**
 * Saldo de un articulo (tabla {@code saldo}): cantidad en unidad base y costo promedio ponderado. Inmutable; cada
 * movimiento produce otro saldo con la version siguiente. No se edita a mano (A-03): solo cambia con movimientos.
 *
 * @param cantidadBase       puede ser negativa (A-01)
 * @param cpp                costo promedio por unidad base, 4 decimales
 * @param ultimoMovimientoId nulo mientras no haya movimientos
 */
public record Saldo(UUID articuloId, UUID negocioId, BigDecimal cantidadBase, BigDecimal cpp,
                    UUID ultimoMovimientoId, long version) {

    public static final long VERSION_INICIAL = 1L;

    public Saldo {
        Objects.requireNonNull(articuloId, "articuloId");
        Objects.requireNonNull(negocioId, "negocioId");
        cantidadBase = Redondeo.cantidad(Objects.requireNonNull(cantidadBase, "cantidadBase"));
        cpp = Redondeo.costoUnitario(Objects.requireNonNull(cpp, "cpp"));
        if (version < VERSION_INICIAL) {
            throw new IllegalArgumentException("La version del saldo debe ser 1 o mayor: " + version);
        }
    }

    public static Saldo inicial(UUID articuloId, UUID negocioId) {
        return new Saldo(articuloId, negocioId, BigDecimal.ZERO, BigDecimal.ZERO, null, VERSION_INICIAL);
    }

    /**
     * Entrada de {@code cantidad} unidades base a {@code costoUnitario} cada una. Con saldo anterior positivo promedia
     * (C-02); con saldo cero o negativo el CPP pasa a ser el costo de la entrada (C-03).
     */
    public Saldo conEntrada(BigDecimal cantidad, BigDecimal costoUnitario, UUID movimientoId) {
        var nuevaCantidad = cantidadBase.add(cantidad);
        BigDecimal nuevoCpp;
        if (cantidadBase.signum() > 0) {
            var valorAnterior = cantidadBase.multiply(cpp);
            var valorEntrada = cantidad.multiply(costoUnitario);
            nuevoCpp = Redondeo.dividir(valorAnterior.add(valorEntrada), nuevaCantidad, Redondeo.ESCALA_COSTO);
        } else {
            nuevoCpp = costoUnitario;
        }
        return new Saldo(articuloId, negocioId, nuevaCantidad, nuevoCpp, movimientoId, version + 1);
    }

    /**
     * Entrada cuyo valor total se conoce con exactitud (lo pagado, calculado desde lo capturado). El CPP sale de ese
     * valor y no del costo unitario ya redondeado, para no arrastrar el error del redondeo (revision A1).
     */
    public Saldo conEntradaConValor(BigDecimal cantidad, BigDecimal valorEntrada, UUID movimientoId) {
        var nuevaCantidad = cantidadBase.add(cantidad);
        var valorTotal = cantidadBase.signum() > 0 ? cantidadBase.multiply(cpp).add(valorEntrada) : valorEntrada;
        var divisor = cantidadBase.signum() > 0 ? nuevaCantidad : cantidad;
        var nuevoCpp = Redondeo.dividir(valorTotal, divisor, Redondeo.ESCALA_COSTO);
        return new Saldo(articuloId, negocioId, nuevaCantidad, nuevoCpp, movimientoId, version + 1);
    }

    /** Salida de {@code cantidad} unidades base: se valua al CPP vigente y el CPP no cambia (C-04). */
    public Saldo conSalida(BigDecimal cantidad, UUID movimientoId) {
        return new Saldo(articuloId, negocioId, cantidadBase.subtract(cantidad), cpp, movimientoId, version + 1);
    }

    /** Valor del inventario: cantidad por CPP, en dinero (C-01). */
    public BigDecimal valor() {
        return Redondeo.dinero(cantidadBase.multiply(cpp));
    }
}
