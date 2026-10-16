package mx.negocio360.canal.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import graphql.schema.DataFetchingEnvironment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.caja.CajaApi;
import mx.negocio360.caja.aplicacion.AbrirTurnoComando;
import mx.negocio360.caja.aplicacion.CerrarTurnoComando;
import mx.negocio360.caja.aplicacion.CrearCajaComando;
import mx.negocio360.caja.aplicacion.DescartarCorteComando;
import mx.negocio360.caja.aplicacion.RegistrarMovimientoCajaComando;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.comandos.PreparacionCreada;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.aplicacion.AnularVentaComando;
import mx.negocio360.ventas.aplicacion.RegistrarVentaComando;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * {@code prepararComando} y {@code confirmarComando} de la plataforma (D-55) para Ventas y Caja. El negocio y el actor
 * salen del contexto de la sesion; el {@code ComandoInput} trae exactamente una variante ({@code @oneOf}, SG-17) y se
 * convierte al comando del modulo dueno sin campos de mas.
 *
 * <p>Cada modulo guarda sus propias preparaciones, asi que el token que recibe la persona lleva al frente el modulo
 * ({@code ventas.} o {@code caja.}) y {@code confirmarComando} lo usa para saber a quien entregarlo. El resto del
 * token sigue siendo opaco y de un solo uso (D-23).
 */
@Controller
@Profile("!produccion")
@ConditionalOnProperty(name = "negocio360.demo.habilitada", havingValue = "true")
public class ComandosController {

    static final String PREFIJO_VENTAS = "ventas.";
    static final String PREFIJO_CAJA = "caja.";

    /**
     * Lo que recibe quien prepara. El token no se imprime: es una credencial de confirmacion.
     *
     * @param token        token con el prefijo del modulo
     * @param resumen      texto que la persona revisa
     * @param expiraEn     vencimiento
     * @param advertencias avisos para la persona; hoy siempre vacia
     */
    public record PreparacionVista(String token, String resumen, Instant expiraEn, List<String> advertencias) {
        @Override
        public String toString() {
            return "PreparacionVista[expiraEn=" + expiraEn + "]";
        }
    }

    /**
     * Un dato del resultado, por ejemplo {@code folio} o {@code cambio}.
     *
     * @param clave nombre del dato
     * @param valor valor como texto
     */
    public record DatoVista(String clave, String valor) {
    }

    /**
     * Resultado de confirmar.
     *
     * @param ok               verdadero si el codigo es OK
     * @param codigo           codigo fijo
     * @param mensajePlantilla texto fijo
     * @param recursoId        id de lo creado o cambiado
     * @param datos            datos del resultado, ordenados por clave
     */
    public record ResultadoVista(boolean ok, String codigo, String mensajePlantilla, UUID recursoId,
                                 List<DatoVista> datos) {
    }

    /** Un monto por metodo de pago, como llega en {@code CerrarTurnoInput.contado}. */
    private record MontoInput(MetodoPago metodo, BigDecimal monto) {
    }

    private final VentasApi ventas;
    private final CajaApi caja;
    private final ObjectMapper json = JsonMapper.builder().build();

    /**
     * Arma el controlador.
     *
     * @param ventas API de Ventas
     * @param caja   API de Caja
     */
    public ComandosController(VentasApi ventas, CajaApi caja) {
        this.ventas = Objects.requireNonNull(ventas, "ventas");
        this.caja = Objects.requireNonNull(caja, "caja");
    }

    /**
     * Prepara un comando de Ventas o de Caja.
     *
     * @param input   {@code ComandoInput} con una sola variante
     * @param entorno entorno con el contexto de la sesion
     * @return token, resumen y vencimiento
     */
    @MutationMapping
    public PreparacionVista prepararComando(@Argument Map<String, Object> input, DataFetchingEnvironment entorno) {
        var contexto = ContextoGraphQl.requerido(entorno);
        var variantes = input.entrySet().stream().filter(e -> e.getValue() != null).toList();
        if (variantes.size() != 1) {
            throw new CanalException(CanalException.Codigo.COMANDO_INVALIDO);
        }
        var campos = variantes.get(0).getValue();
        return switch (variantes.get(0).getKey()) {
            case "registrarVenta" -> deVentas(contexto, convertir(campos, RegistrarVentaComando.class));
            case "anularVenta" -> deVentas(contexto, convertir(campos, AnularVentaComando.class));
            case "crearCaja" -> deCaja(contexto, convertir(campos, CrearCajaComando.class));
            case "abrirTurno" -> deCaja(contexto, convertir(campos, AbrirTurnoComando.class));
            case "registrarMovimientoCaja" -> deCaja(contexto,
                    convertir(campos, RegistrarMovimientoCajaComando.class));
            case "cerrarTurno" -> deCaja(contexto, cerrarTurno(campos));
            case "descartarCorte" -> deCaja(contexto, convertir(campos, DescartarCorteComando.class));
            default -> throw new CanalException(CanalException.Codigo.NO_DISPONIBLE);
        };
    }

    /**
     * Confirma un comando preparado con el modulo que indica el prefijo del token.
     *
     * @param token   token que devolvio {@link #prepararComando}
     * @param entorno entorno con el contexto de la sesion
     * @return resultado del modulo
     */
    @MutationMapping
    public ResultadoVista confirmarComando(@Argument String token, DataFetchingEnvironment entorno) {
        var contexto = ContextoGraphQl.requerido(entorno);
        if (token != null && token.startsWith(PREFIJO_VENTAS)) {
            return vista(ventas.confirmar(contexto, token.substring(PREFIJO_VENTAS.length())));
        }
        if (token != null && token.startsWith(PREFIJO_CAJA)) {
            return vista(caja.confirmar(contexto, token.substring(PREFIJO_CAJA.length())));
        }
        throw new CanalException(CanalException.Codigo.TOKEN_INVALIDO);
    }

    private PreparacionVista deVentas(ContextoNegocio contexto, Comando comando) {
        return vista(PREFIJO_VENTAS, ventas.preparar(contexto, comando));
    }

    private PreparacionVista deCaja(ContextoNegocio contexto, Comando comando) {
        return vista(PREFIJO_CAJA, caja.preparar(contexto, comando));
    }

    /** El conteo llega como lista de montos por metodo; un metodo repetido es un comando invalido. */
    private CerrarTurnoComando cerrarTurno(Object campos) {
        if (!(campos instanceof Map<?, ?> mapa)) {
            throw new CanalException(CanalException.Codigo.COMANDO_INVALIDO);
        }
        var contado = new LinkedHashMap<MetodoPago, BigDecimal>();
        if (mapa.get("contado") instanceof List<?> montos) {
            for (var monto : montos) {
                var leido = convertir(monto, MontoInput.class);
                if (contado.put(leido.metodo(), leido.monto()) != null) {
                    throw new CanalException(CanalException.Codigo.COMANDO_INVALIDO);
                }
            }
        }
        var turnoId = convertir(mapa.get("turnoId"), UUID.class);
        return new CerrarTurnoComando(turnoId, contado, Boolean.TRUE.equals(mapa.get("autorizar")));
    }

    /** Convierte los campos de la variante al comando; un campo que el comando no conoce lo invalida. */
    private <T> T convertir(Object campos, Class<T> tipo) {
        try {
            return json.convertValue(campos, tipo);
        } catch (IllegalArgumentException campoDesconocido) {
            throw new CanalException(CanalException.Codigo.COMANDO_INVALIDO);
        }
    }

    private static PreparacionVista vista(String prefijo, PreparacionCreada creada) {
        return new PreparacionVista(prefijo + creada.token(), creada.resumen(), creada.expiraEn(), List.of());
    }

    private static ResultadoVista vista(ResultadoComando resultado) {
        var datos = resultado.datos().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> new DatoVista(e.getKey(), e.getValue())).toList();
        return new ResultadoVista(ResultadoComando.OK.equals(resultado.codigo()), resultado.codigo(),
                resultado.mensajePlantilla(), resultado.recursoId(), datos);
    }
}
