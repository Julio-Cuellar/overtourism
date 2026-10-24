package mx.negocio360.compartido.graphql.comandos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * El unico dueno de {@code prepararComando} y {@code confirmarComando} (D-55, D-125): GraphQL admite un resolvedor por
 * campo, asi que los modulos se registran como {@link ModuloDeComandos} y este controlador reparte.
 *
 * <ul>
 *   <li>{@code prepararComando}: el tipo {@code @oneOf} deja pasar una variante; el despachador busca su modulo y le delega.
 *       La puerta de la plataforma ya comprobo el permiso de esa variante. Al token que devuelve el modulo se le antepone
 *       su prefijo.
 *   <li>{@code confirmarComando}: el prefijo del token dice a quien entregarlo. Exige sesion con negocio activo, salvo
 *       que el modulo declare {@link ModuloDeComandos#confirmaSinSesion()} (solo Negocios, por la resena del turista).
 *       Un prefijo desconocido responde {@code TOKEN_INVALIDO} sin revelar que modulos existen.
 * </ul>
 */
@Controller
public class ComandosGraphQlController {

    private static final int MAX_CARACTERES_DEL_TOKEN = 128;

    private final Map<String, ModuloDeComandos> porVariante = new HashMap<>();
    private final Map<String, ModuloDeComandos> porPrefijo = new HashMap<>();

    /**
     * Arma el despachador con los modulos que haya.
     *
     * @param modulos modulos registrados; ninguna variante ni prefijo puede repetirse
     * @throws IllegalStateException si dos modulos comparten variante o prefijo, o un prefijo mal formado
     */
    public ComandosGraphQlController(List<ModuloDeComandos> modulos) {
        var repetidos = new TreeSet<String>();
        for (var modulo : modulos) {
            var prefijo = modulo.prefijo();
            if (!prefijo.isEmpty() && (!prefijo.endsWith(".") || prefijo.indexOf('.') != prefijo.length() - 1)) {
                throw new IllegalStateException("El prefijo de un modulo de comandos termina en un solo punto: " + prefijo);
            }
            if (porPrefijo.putIfAbsent(prefijo, modulo) != null) {
                repetidos.add("el prefijo '" + prefijo + "'");
            }
            for (var variante : modulo.variantes()) {
                if (porVariante.putIfAbsent(variante, modulo) != null) {
                    repetidos.add("la variante " + variante);
                }
            }
        }
        if (!repetidos.isEmpty()) {
            throw new IllegalStateException("Modulos de comandos con " + String.join(", ", repetidos) + " repetido");
        }
    }

    /**
     * Prepara un comando con el modulo dueno de su variante.
     *
     * @param input   {@code ComandoInput} con una sola variante
     * @param entrada sesion de la peticion
     * @return token con el prefijo del modulo, resumen y vencimiento
     * @throws ComandoGraphQlException COMANDO_INVALIDO o COMANDO_NO_DISPONIBLE
     */
    @MutationMapping
    public PreparacionVista prepararComando(@Argument Map<String, Object> input,
                                            @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var presentes = input == null ? List.<Map.Entry<String, Object>>of()
                : input.entrySet().stream().filter(e -> e.getValue() != null).toList();
        if (presentes.size() != 1) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        }
        var variante = presentes.get(0).getKey();
        var modulo = porVariante.get(variante);
        if (modulo == null) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE);
        }
        if (!(presentes.get(0).getValue() instanceof Map<?, ?> crudo)) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.COMANDO_INVALIDO);
        }
        var campos = new HashMap<String, Object>();
        crudo.forEach((clave, valor) -> campos.put(String.valueOf(clave), valor));
        var preparada = modulo.preparar(entrada, variante, campos);
        return preparada.conToken(modulo.prefijo() + preparada.token());
    }

    /**
     * Confirma un comando con el modulo que indica el prefijo del token.
     *
     * @param token   token que devolvio {@link #prepararComando}
     * @param entrada sesion de la peticion
     * @return resultado del modulo, con {@code datos} siempre presente
     * @throws ComandoGraphQlException TOKEN_INVALIDO
     * @throws mx.negocio360.compartido.seguridad.NoAutenticadoException sin sesion o sin negocio activo, si el modulo la exige
     */
    @MutationMapping
    public ResultadoVista confirmarComando(@Argument String token,
                                           @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        if (token == null || token.isBlank() || token.length() > MAX_CARACTERES_DEL_TOKEN) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.TOKEN_INVALIDO);
        }
        var punto = token.indexOf('.');
        var prefijo = punto < 0 ? "" : token.substring(0, punto + 1);
        var modulo = porPrefijo.get(prefijo);
        if (modulo == null) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.TOKEN_INVALIDO);
        }
        if (!modulo.confirmaSinSesion()) {
            entrada.exigirContexto();
        }
        return modulo.confirmar(entrada, token.substring(prefijo.length()));
    }
}
