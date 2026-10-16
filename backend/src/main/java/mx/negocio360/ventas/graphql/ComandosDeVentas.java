package mx.negocio360.ventas.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import mx.negocio360.compartido.graphql.comandos.ModuloDeComandos;
import mx.negocio360.compartido.graphql.comandos.PreparacionVista;
import mx.negocio360.compartido.graphql.comandos.ResultadoVista;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.aplicacion.AnularVentaComando;
import mx.negocio360.ventas.aplicacion.RegistrarVentaComando;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las variantes de {@code ComandoInput} de Ventas (VF-10 y VF-11) para el despachador de la plataforma (D-125). Convierte
 * los campos de GraphQL al comando del modulo, sin campos de mas, y entrega la sesion de la peticion a {@link VentasApi}:
 * el negocio, el actor y los permisos salen de ella, nunca de un argumento (SG-02).
 */
@Component
@ConditionalOnProperty(name = "ventas.almacen")
public final class ComandosDeVentas implements ModuloDeComandos {

    /** Prefijo de los tokens de Ventas. */
    public static final String PREFIJO = "ventas.";

    private final VentasApi ventas;
    private final ObjectMapper json = JsonMapper.builder().build();

    /**
     * Arma el modulo de comandos.
     *
     * @param ventas API de Ventas
     */
    public ComandosDeVentas(VentasApi ventas) {
        this.ventas = Objects.requireNonNull(ventas, "ventas");
    }

    @Override
    public String prefijo() {
        return PREFIJO;
    }

    @Override
    public Set<String> variantes() {
        return Set.of("registrarVenta", "anularVenta");
    }

    @Override
    public boolean confirmaSinSesion() {
        return false;
    }

    @Override
    public PreparacionVista preparar(EntradaGraphQl entrada, String variante, Map<String, Object> campos) {
        Comando comando = switch (variante) {
            case "registrarVenta" -> convertir(campos, RegistrarVentaComando.class);
            case "anularVenta" -> convertir(campos, AnularVentaComando.class);
            default -> throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE);
        };
        var creada = ventas.preparar(entrada.exigirContexto(), entrada.autorizacion(), comando);
        return new PreparacionVista(creada.token(), creada.resumen(), creada.expiraEn(), java.util.List.of());
    }

    @Override
    public ResultadoVista confirmar(EntradaGraphQl entrada, String tokenSinPrefijo) {
        return ResultadoVista.de(ventas.confirmar(entrada.exigirContexto(), entrada.autorizacion(), tokenSinPrefijo));
    }

    /** Un campo que el comando no conoce o un valor que no se puede leer lo invalida. */
    private <T> T convertir(Map<String, Object> campos, Class<T> tipo) {
        try {
            return json.convertValue(campos, tipo);
        } catch (IllegalArgumentException e) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        }
    }
}
