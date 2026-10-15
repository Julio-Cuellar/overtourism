package mx.negocio360.compartido.comandos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.TokenOpaco;

/**
 * Patron preparar -&gt; confirmar con token de un solo uso (D-23, D-55), generico para cualquier modulo. Es la misma
 * logica que {@code clientes.aplicacion.ServicioPreparacion}, sin atarse a los codigos de error de Clientes: cada
 * modulo pasa un traductor de {@link FalloPreparacion} a su propia excepcion.
 *
 * <p>Preparar valida con el manejador y guarda solo el hash del token, ligado al negocio, al actor, al hash del
 * comando y a su ambito. Confirmar rechaza el token desconocido, ajeno, usado, vencido o cuyo comando cambio; consume
 * el token antes de ejecutar y despues vacia el comando guardado, pase lo que pase.
 *
 * <p><b>Transacciones (pendiente con los adaptadores reales):</b> {@code marcarUsada} y {@code vaciar} deben
 * confirmarse aparte de la transaccion del manejador, para que un fallo del comando no devuelva el token a la vida; y
 * cada {@code confirmar} de un manejador debe ser una sola transaccion que incluya sus eventos (outbox).
 */
public final class ServicioPreparacion {

    /** Vigencia del token (D-23). */
    public static final Duration VIGENCIA_TOKEN = Duration.ofMinutes(5);
    private static final int MAX_CARACTERES_DEL_TOKEN = 64;

    private final RepositorioPreparaciones repositorio;
    private final Map<Class<?>, ManejadorComando<?>> porClase = new HashMap<>();
    private final Map<String, ManejadorComando<?>> porTipo = new HashMap<>();
    private final Clock reloj;
    private final Function<FalloPreparacion, ? extends RuntimeException> traductor;
    private final ObjectMapper mapeador = mapeadorEstricto();

    /**
     * Arma el servicio con los manejadores del modulo.
     *
     * @param repositorio tabla {@code preparacion} del modulo
     * @param manejadores un manejador por comando; tipo y clase no se pueden repetir
     * @param reloj       reloj de la plataforma
     * @param traductor   convierte un fallo del patron en la excepcion de negocio del modulo
     * @throws IllegalArgumentException si dos manejadores comparten tipo o clase de comando
     */
    public ServicioPreparacion(RepositorioPreparaciones repositorio, List<? extends ManejadorComando<?>> manejadores,
                               Clock reloj, Function<FalloPreparacion, ? extends RuntimeException> traductor) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.traductor = Objects.requireNonNull(traductor, "traductor");
        for (var manejador : manejadores) {
            var repetido = porClase.putIfAbsent(manejador.claseComando(), manejador) != null
                    || porTipo.putIfAbsent(manejador.tipo(), manejador) != null;
            if (repetido) {
                throw new IllegalArgumentException("Manejador repetido: " + manejador.tipo());
            }
        }
    }

    /**
     * Valida el comando sin ejecutarlo y crea su token.
     *
     * @param contexto negocio y actor
     * @param comando  comando a preparar
     * @return token en claro, resumen y vencimiento
     * @throws IllegalArgumentException si no hay manejador para ese comando (error de programacion)
     */
    public PreparacionCreada preparar(ContextoNegocio contexto, Comando comando) {
        Objects.requireNonNull(contexto, "contexto");
        Objects.requireNonNull(comando, "comando");
        var manejador = porClase.get(comando.getClass());
        if (manejador == null) {
            throw new IllegalArgumentException("No hay manejador para " + comando.getClass().getName());
        }
        var resumen = resumenDe(manejador, contexto, comando);
        var json = serializar(comando);
        var token = TokenOpaco.nuevo();
        var expiraEn = reloj.instant().plus(VIGENCIA_TOKEN);
        repositorio.guardar(new Preparacion(TokenOpaco.hash(token), contexto.negocioId(), contexto.actor(),
                manejador.tipo(), TokenOpaco.hash(json), json, resumen, ambitoDe(manejador, comando), expiraEn, null));
        return new PreparacionCreada(token, resumen, expiraEn);
    }

    /**
     * Ejecuta el comando preparado con ese token.
     *
     * @param contexto negocio y actor; deben ser los mismos que prepararon
     * @param token    token en claro
     * @return resultado del manejador
     * @throws RuntimeException la excepcion del modulo con TOKEN_INVALIDO, TOKEN_USADO, TOKEN_EXPIRADO o
     *                          COMANDO_CAMBIO, o la que lance el manejador
     */
    public ResultadoComando confirmar(ContextoNegocio contexto, String token) {
        Objects.requireNonNull(contexto, "contexto");
        if (token == null || token.isBlank() || token.length() > MAX_CARACTERES_DEL_TOKEN) {
            throw traductor.apply(FalloPreparacion.TOKEN_INVALIDO);
        }
        var tokenHash = TokenOpaco.hash(token);
        var fila = repositorio.porHash(tokenHash)
                .filter(f -> f.negocioId().equals(contexto.negocioId()) && f.actor().equals(contexto.actor()))
                .orElseThrow(() -> traductor.apply(FalloPreparacion.TOKEN_INVALIDO));
        if (fila.usadoEn() != null) {
            throw traductor.apply(FalloPreparacion.TOKEN_USADO);
        }
        var ahora = reloj.instant();
        if (!ahora.isBefore(fila.expiraEn())) {
            repositorio.vaciar(tokenHash);
            throw traductor.apply(FalloPreparacion.TOKEN_EXPIRADO);
        }
        var manejador = porTipo.get(fila.comandoTipo());
        if (fila.comando() == null || manejador == null || !TokenOpaco.hash(fila.comando()).equals(fila.comandoHash())) {
            repositorio.vaciar(tokenHash);
            throw traductor.apply(FalloPreparacion.COMANDO_CAMBIO);
        }
        if (!repositorio.marcarUsada(tokenHash, ahora)) {
            throw traductor.apply(FalloPreparacion.TOKEN_USADO);
        }
        try {
            return ejecutar(manejador, contexto, fila.comando(), fila.resumen());
        } finally {
            repositorio.vaciar(tokenHash);
        }
    }

    /**
     * Preparaciones sin usar ni vencer de un tipo y ambito, en el negocio del contexto (CJ-07).
     *
     * @param contexto    negocio
     * @param comandoTipo tipo del comando
     * @param ambito      ambito, por ejemplo el id del turno
     * @return cuantas siguen vigentes
     */
    public long vigentes(ContextoNegocio contexto, String comandoTipo, String ambito) {
        return repositorio.vigentes(contexto.negocioId(), comandoTipo, ambito, reloj.instant());
    }

    private <C extends Comando> String resumenDe(ManejadorComando<C> manejador, ContextoNegocio contexto,
                                                 Comando comando) {
        return manejador.preparar(contexto, manejador.claseComando().cast(comando));
    }

    private <C extends Comando> String ambitoDe(ManejadorComando<C> manejador, Comando comando) {
        return manejador.ambito(manejador.claseComando().cast(comando));
    }

    private <C extends Comando> ResultadoComando ejecutar(ManejadorComando<C> manejador, ContextoNegocio contexto,
                                                          String json, String resumen) {
        C comando;
        try {
            comando = mapeador.readValue(json, manejador.claseComando());
        } catch (JsonProcessingException e) {
            // Sin la causa: el mensaje de Jackson puede citar el contenido del comando.
            throw traductor.apply(FalloPreparacion.COMANDO_CAMBIO);
        }
        return manejador.confirmar(contexto, comando, resumen);
    }

    private String serializar(Comando comando) {
        try {
            return mapeador.writeValueAsString(comando);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("El comando no se puede serializar: " + comando.getClass().getName());
        }
    }

    /**
     * JSON estricto: un campo desconocido es una alteracion, y las propiedades van en orden alfabetico para que el
     * mismo comando produzca siempre el mismo texto y el mismo hash.
     */
    private static ObjectMapper mapeadorEstricto() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
    }
}
