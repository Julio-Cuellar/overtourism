package mx.negocio360.restaurantes.web;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.comandos.ServicioPreparacion;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import mx.negocio360.compartido.graphql.comandos.ModuloDeComandos;
import mx.negocio360.compartido.graphql.comandos.PreparacionVista;
import mx.negocio360.compartido.graphql.comandos.ResultadoVista;
import mx.negocio360.restaurantes.aplicacion.DarDeBajaPlatilloComando;
import mx.negocio360.restaurantes.aplicacion.GuardarRecetaComando;

/**
 * Las variantes de {@code ComandoInput} de Restaurantes para el despachador de la plataforma (D-125, D-209):
 * {@code guardarReceta} y {@code darDeBajaPlatillo}. Convierte los campos de GraphQL al comando del modulo, sin campos de
 * mas, y delega en el patron preparar -&gt; confirmar de la plataforma.
 *
 * <p>El permiso de cada comando lo declara su manejador y {@link ServicioPreparacion} lo exige en las dos puntas: al
 * preparar y otra vez al confirmar, <b>antes</b> de gastar el token (D-126). La puerta de la plataforma ya comprobo el de la
 * variante (SG-04). Los errores de dominio salen como {@code RestaurantesException}, que traduce
 * {@link TraductorDeErroresDeRestaurantes}.
 */
public final class ComandosDeRestaurantes implements ModuloDeComandos {

    /** Prefijo de los tokens de Restaurantes. */
    public static final String PREFIJO = "restaurantes.";

    private static final Map<String, Class<? extends Comando>> VARIANTES = Map.of(
            "guardarReceta", GuardarRecetaComando.class,
            "darDeBajaPlatillo", DarDeBajaPlatilloComando.class);

    private final ServicioPreparacion preparacion;
    private final JsonMapper json = JsonMapper.builder().build();

    /**
     * Arma el modulo de comandos.
     *
     * @param preparacion servicio de preparar y confirmar con los manejadores de Restaurantes
     */
    public ComandosDeRestaurantes(ServicioPreparacion preparacion) {
        this.preparacion = Objects.requireNonNull(preparacion, "preparacion");
    }

    @Override
    public String prefijo() {
        return PREFIJO;
    }

    @Override
    public Set<String> variantes() {
        return VARIANTES.keySet();
    }

    @Override
    public boolean confirmaSinSesion() {
        return false;
    }

    @Override
    public PreparacionVista preparar(EntradaGraphQl entrada, String variante, Map<String, Object> campos) {
        var clase = VARIANTES.get(variante);
        if (clase == null) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE);
        }
        var comando = convertir(campos, clase);
        var creada = preparacion.preparar(entrada.exigirContexto(), entrada.autorizacion(), comando);
        return new PreparacionVista(creada.token(), creada.resumen(), creada.expiraEn(), List.of());
    }

    @Override
    public ResultadoVista confirmar(EntradaGraphQl entrada, String tokenSinPrefijo) {
        return ResultadoVista.de(preparacion.confirmar(entrada.exigirContexto(), entrada.autorizacion(), tokenSinPrefijo));
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
