package mx.negocio360.inventario.aplicacion;

import java.util.Objects;
import mx.negocio360.inventario.dominio.Articulo;
import mx.negocio360.inventario.dominio.Saldo;

/** Un articulo junto con su saldo; el saldo es nulo solo si el articulo nunca lo tuvo. */
public record ArticuloConSaldo(Articulo articulo, Saldo saldo) {

    public ArticuloConSaldo {
        Objects.requireNonNull(articulo, "articulo");
    }

    public Saldo saldoOInicial() {
        return saldo != null ? saldo : Saldo.inicial(articulo.id(), articulo.negocioId());
    }

    /**
     * Esta en o bajo su minimo. Un minimo de cero significa "sin minimo configurado" y no avisa (decision D-77 a
     * confirmar con el equipo).
     */
    public boolean bajoMinimo() {
        var minimo = articulo.minimoBase();
        return minimo.signum() > 0 && saldoOInicial().cantidadBase().compareTo(minimo) <= 0;
    }

    /** Hay algo que atender: bajo minimo o saldo negativo (A-01, A-02). */
    public boolean enAlerta() {
        return bajoMinimo() || saldoOInicial().cantidadBase().signum() < 0;
    }
}
