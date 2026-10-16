package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * Respuesta de {@code registrarConsumo} y {@code revertirConsumo} (6.4). Las cifras las calcula el servidor.
 *
 * @param estado      que paso con la llamada
 * @param movimientos movimientos que creo la llamada, o los guardados si es una repeticion; vacio si solo hubo marca
 * @param costoTotal  costo de lo descontado o repuesto, positivo; cero si no se movio nada
 * @param alertas     alertas de saldo que disparo esta llamada; vacio en una repeticion
 */
@NamedInterface("api")
public record ConsumoResultado(EstadoDeConsumo estado, List<UUID> movimientos, BigDecimal costoTotal,
                               List<AlertaDeConsumo> alertas) {

    public ConsumoResultado {
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(costoTotal, "costoTotal");
        movimientos = List.copyOf(Objects.requireNonNull(movimientos, "movimientos"));
        alertas = List.copyOf(Objects.requireNonNull(alertas, "alertas"));
    }

    /** Resultado sin movimientos ni alertas: repeticiones y marcas. */
    static ConsumoResultado sinMovimientos(EstadoDeConsumo estado) {
        return new ConsumoResultado(estado, List.of(), BigDecimal.ZERO, List.of());
    }
}
