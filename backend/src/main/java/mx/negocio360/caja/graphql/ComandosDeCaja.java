package mx.negocio360.caja.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.caja.CajaApi;
import mx.negocio360.caja.aplicacion.AbrirTurnoComando;
import mx.negocio360.caja.aplicacion.CerrarTurnoComando;
import mx.negocio360.caja.aplicacion.CrearCajaComando;
import mx.negocio360.caja.aplicacion.DescartarCorteComando;
import mx.negocio360.caja.aplicacion.RegistrarMovimientoCajaComando;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import mx.negocio360.compartido.graphql.comandos.ModuloDeComandos;
import mx.negocio360.compartido.graphql.comandos.PreparacionVista;
import mx.negocio360.compartido.graphql.comandos.ResultadoVista;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las variantes de {@code ComandoInput} de Caja (CF-10 a CF-14) para el despachador de la plataforma (D-125). Convierte
 * los campos de GraphQL al comando del modulo, sin campos de mas, y entrega la sesion de la peticion a {@link CajaApi}.
 * El conteo del corte llega como lista de montos por metodo; un metodo repetido invalida el comando.
 */
@Component
@ConditionalOnProperty(name = "caja.almacen")
public final class ComandosDeCaja implements ModuloDeComandos {

    /** Prefijo de los tokens de Caja. */
    public static final String PREFIJO = "caja.";

    /** Un monto por metodo de pago, como llega en {@code CerrarTurnoInput.contado}. */
    private record MontoInput(MetodoPago metodo, BigDecimal monto) {
    }

    private final CajaApi caja;
    private final ObjectMapper json = JsonMapper.builder().build();

    /**
     * Arma el modulo de comandos.
     *
     * @param caja API de Caja
     */
    public ComandosDeCaja(CajaApi caja) {
        this.caja = Objects.requireNonNull(caja, "caja");
    }

    @Override
    public String prefijo() {
        return PREFIJO;
    }

    @Override
    public Set<String> variantes() {
        return Set.of("crearCaja", "abrirTurno", "registrarMovimientoCaja", "cerrarTurno", "descartarCorte");
    }

    @Override
    public boolean confirmaSinSesion() {
        return false;
    }

    @Override
    public PreparacionVista preparar(EntradaGraphQl entrada, String variante, Map<String, Object> campos) {
        Comando comando = switch (variante) {
            case "crearCaja" -> convertir(campos, CrearCajaComando.class);
            case "abrirTurno" -> convertir(campos, AbrirTurnoComando.class);
            case "registrarMovimientoCaja" -> convertir(campos, RegistrarMovimientoCajaComando.class);
            case "cerrarTurno" -> cerrarTurno(campos);
            case "descartarCorte" -> convertir(campos, DescartarCorteComando.class);
            default -> throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE);
        };
        var creada = caja.preparar(entrada.exigirContexto(), entrada.autorizacion(), comando);
        return new PreparacionVista(creada.token(), creada.resumen(), creada.expiraEn(), List.of());
    }

    @Override
    public ResultadoVista confirmar(EntradaGraphQl entrada, String tokenSinPrefijo) {
        return ResultadoVista.de(caja.confirmar(entrada.exigirContexto(), entrada.autorizacion(), tokenSinPrefijo));
    }

    /** El conteo llega como lista de montos por metodo; un metodo repetido es un comando invalido. */
    private CerrarTurnoComando cerrarTurno(Map<String, Object> campos) {
        var contado = new EnumMap<MetodoPago, BigDecimal>(MetodoPago.class);
        if (campos.get("contado") instanceof List<?> montos) {
            for (var monto : montos) {
                var leido = convertir(monto, MontoInput.class);
                if (leido.metodo() == null || contado.put(leido.metodo(), leido.monto()) != null) {
                    throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
                }
            }
        }
        var turnoId = convertir(campos.get("turnoId"), UUID.class);
        return new CerrarTurnoComando(turnoId, contado, Boolean.TRUE.equals(campos.get("autorizar")));
    }

    /** Un campo que el comando no conoce o un valor que no se puede leer lo invalida. */
    private <T> T convertir(Object campos, Class<T> tipo) {
        try {
            return json.convertValue(campos, tipo);
        } catch (IllegalArgumentException e) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        }
    }
}
