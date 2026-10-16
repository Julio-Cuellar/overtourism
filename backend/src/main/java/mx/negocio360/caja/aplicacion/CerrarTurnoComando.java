package mx.negocio360.caja.aplicacion;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.compartido.comandos.Comando;
import org.springframework.modulith.NamedInterface;

/**
 * CF-12 CerrarTurno: el corte (CJ-06 a CJ-08, CJ-12). Preparar pone el turno EN_CORTE (D-81).
 *
 * <p>Copia su mapa al crearse (quien lo armo no puede cambiarlo despues). Admite un mapa nulo o con valores nulos,
 * porque esos casos los reporta el manejador con {@code CONTEO_INVALIDO}.
 *
 * @param turnoId          turno ABIERTO o EN_CORTE
 * @param contadoPorMetodo lo que conto la persona; un metodo que falta cuenta como 0.00
 * @param autorizar        verdadero si quien confirma es encargado y autoriza una diferencia sobre el umbral
 */
@NamedInterface("comandos")
public record CerrarTurnoComando(UUID turnoId, Map<MetodoPago, BigDecimal> contadoPorMetodo, boolean autorizar)
        implements Comando {

    /** Copia defensiva e inmutable del conteo, en el orden en que llego. */
    public CerrarTurnoComando {
        contadoPorMetodo = contadoPorMetodo == null ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(contadoPorMetodo));
    }
}
