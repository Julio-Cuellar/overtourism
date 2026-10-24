package mx.negocio360.caja.graphql;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.caja.CajaApi;
import mx.negocio360.caja.FiltroCortes;
import mx.negocio360.caja.SaldoTurno;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Los resolvedores de consulta de Caja (CF-01 a CF-04). Solo traducen GraphQL a {@link CajaApi}: el negocio, el actor y los
 * permisos salen de la sesion que dejo el filtro de entrada ({@link EntradaGraphQl}), nunca de un argumento (SG-02); sin
 * sesion o sin negocio activo fallan cerrado. Un id de otro negocio responde vacio, igual que uno que no existe.
 */
@Controller
@ConditionalOnProperty(name = "caja.almacen")
public class CajaGraphQlController {

    /** Una pagina de cortes con la forma de {@code CorteConexion}. */
    public record CorteConexion(List<Corte> cortes, Map<String, Object> pageInfo) {
    }

    /** Una pagina de movimientos con la forma de {@code MovimientoCajaConexion}. */
    public record MovimientoCajaConexion(List<MovimientoCaja> movimientos, Map<String, Object> pageInfo) {
    }

    private final CajaApi caja;

    /**
     * Arma el controlador.
     *
     * @param caja API de Caja
     */
    public CajaGraphQlController(CajaApi caja) {
        this.caja = Objects.requireNonNull(caja, "caja");
    }

    /** CF-04: cajas del negocio. */
    @QueryMapping
    public List<Caja> cajas(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return caja.cajas(entrada.exigirContexto(), entrada.autorizacion());
    }

    /** CF-01: turno abierto o en corte de una caja, con su estado efectivo. */
    @QueryMapping
    public Turno turnoActivo(@Argument UUID cajaId, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return caja.turnoActivo(entrada.exigirContexto(), entrada.autorizacion(), cajaId).orElse(null);
    }

    /** CF-02: esperado por metodo de un turno. */
    @QueryMapping
    public SaldoTurno saldoTurno(@Argument UUID turnoId, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return caja.saldoTurno(entrada.exigirContexto(), entrada.autorizacion(), turnoId);
    }

    /** CF-03: cortes del negocio, del mas reciente al mas antiguo, con cursor. */
    @QueryMapping
    public CorteConexion cortes(@Argument FiltroCortes filtro, @Argument int primeros, @Argument String despues,
                                @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var pagina = caja.cortes(entrada.exigirContexto(), entrada.autorizacion(), filtro, primeros, despues);
        return new CorteConexion(pagina.elementos(), info(pagina.hayMas(), pagina.cursorFinal()));
    }

    /** CF-02: libro del turno, paginado. */
    @SchemaMapping(typeName = "SaldoTurno", field = "movimientos")
    public MovimientoCajaConexion movimientos(SaldoTurno saldo, @Argument int primeros, @Argument String despues,
                                              @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var pagina = caja.movimientos(entrada.exigirContexto(), entrada.autorizacion(), saldo.turnoId(), primeros,
                despues);
        return new MovimientoCajaConexion(pagina.elementos(), info(pagina.hayMas(), pagina.cursorFinal()));
    }

    /** La cuenta que abrio el turno, con el nombre que usa el esquema. */
    @SchemaMapping(typeName = "Turno", field = "abiertoPor")
    public String abiertoPor(Turno turno) {
        return turno.cajero();
    }

    /** El esperado del saldo como lista de {@code MontoPorMetodo}. */
    @SchemaMapping(typeName = "SaldoTurno", field = "esperadoPorMetodo")
    public List<Map<String, Object>> esperadoDelSaldo(SaldoTurno saldo) {
        return montos(saldo.esperadoPorMetodo());
    }

    /** El esperado del corte como lista. */
    @SchemaMapping(typeName = "CorteDeCaja", field = "esperadoPorMetodo")
    public List<Map<String, Object>> esperadoDelCorte(Corte corte) {
        return montos(corte.esperadoPorMetodo());
    }

    /** Lo contado en el corte como lista. */
    @SchemaMapping(typeName = "CorteDeCaja", field = "contadoPorMetodo")
    public List<Map<String, Object>> contadoDelCorte(Corte corte) {
        return montos(corte.contadoPorMetodo());
    }

    private static List<Map<String, Object>> montos(Map<MetodoPago, BigDecimal> porMetodo) {
        return porMetodo.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> Map.<String, Object>of("metodo", e.getKey(), "monto", e.getValue())).toList();
    }

    private static Map<String, Object> info(boolean hayMas, String cursorFinal) {
        var info = new LinkedHashMap<String, Object>();
        info.put("hayMas", hayMas);
        info.put("cursorFinal", cursorFinal);
        return info;
    }
}
