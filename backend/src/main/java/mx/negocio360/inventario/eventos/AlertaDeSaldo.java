package mx.negocio360.inventario.eventos;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.dinero.DecimalPlano;

/**
 * Alertas de saldo v1 (A-02): {@code inventario.bajo_minimo}, {@code inventario.repuesto} y
 * {@code inventario.saldo_negativo}. Se publican al cruzar el umbral, no en cada movimiento. La de saldo negativo no
 * lleva minimo.
 */
public record AlertaDeSaldo(String tipo, UUID articuloId, String nombre, BigDecimal saldoBase, BigDecimal minimoBase,
                            String unidadBase) {

    public static final String BAJO_MINIMO = "inventario.bajo_minimo";
    public static final String REPUESTO = "inventario.repuesto";
    public static final String SALDO_NEGATIVO = "inventario.saldo_negativo";
    public static final int VERSION = 1;
    private static final Set<String> TIPOS = Set.of(BAJO_MINIMO, REPUESTO, SALDO_NEGATIVO);

    public AlertaDeSaldo {
        Objects.requireNonNull(tipo, "tipo");
        if (!TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de alerta desconocido: " + tipo);
        }
        Objects.requireNonNull(articuloId, "articuloId");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(saldoBase, "saldoBase");
        Objects.requireNonNull(minimoBase, "minimoBase");
        Objects.requireNonNull(unidadBase, "unidadBase");
    }

    /** Carga del sobre, con las claves del contrato. */
    public Map<String, Object> datos() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("articulo_id", articuloId.toString());
        datos.put("nombre", nombre);
        datos.put("saldo_base", DecimalPlano.escribir(saldoBase));
        if (!SALDO_NEGATIVO.equals(tipo)) {
            datos.put("minimo_base", DecimalPlano.escribir(minimoBase));
        }
        datos.put("unidad_base", unidadBase);
        return datos;
    }
}
