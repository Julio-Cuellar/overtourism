package mx.negocio360.contabilidad.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPreparaciones;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Patron preparar -> confirmar con token de un solo uso (D-23, D-55).
 *
 * <p>Preparar valida sin escribir y guarda la preparacion: solo el hash del token, ligado al negocio, al actor y al
 * hash del comando. Confirmar rechaza el token desconocido, ajeno, usado, vencido o cuyo comando cambio; consume el
 * token antes de ejecutar y, pase lo que pase, vacia el comando guardado. El comando tambien se vacia cuando el
 * token vencio o el comando cambio. Es una copia del servicio de Clientes porque un modulo no puede depender de otro
 * (R1); unificarlo en {@code compartido} es la historia HTC-89.
 *
 * <p><b>Contrato transaccional (propuesto):</b> {@code marcarUsada} y {@code vaciar} deben
 * confirmarse en su propia transaccion, aparte de la del manejador, para que un fallo del comando no devuelva el
 * token a la vida ni deje el comando vivo; y cada {@code confirmar} de un manejador debe ser una sola transaccion
 * que incluya sus eventos (outbox). Si se unen en una sola transaccion, el comportamiento que prueban
 * {@code PreparacionTest} cambia y hay que decidirlo de nuevo.
 *
 * <p>El mapeador de JSON es siempre el estricto de {@link MapeadorComandos}: no se acepta uno de fuera.
 */
public final class ServicioPreparacion {

    public static final Duration VIGENCIA_TOKEN = Duration.ofMinutes(5);
    private static final int BYTES_DEL_TOKEN = 32;
    private static final int MAX_CARACTERES_DEL_TOKEN = 64;
    private static final Logger LOG = LoggerFactory.getLogger(ServicioPreparacion.class);

    private final RepositorioPreparaciones repositorio;
    private final Map<Class<?>, ManejadorComando<?>> porClase = new HashMap<>();
    private final Map<String, ManejadorComando<?>> porTipo = new HashMap<>();
    private final Clock reloj;
    private final ObjectMapper mapeador;
    private final SecureRandom aleatorio = new SecureRandom();

    /**
     * Crea el servicio con los manejadores de todos los comandos del modulo.
     *
     * @param repositorio  tabla de preparaciones
     * @param manejadores  un manejador por comando; no se admiten repetidos
     * @param reloj        reloj de la plataforma
     * @throws IllegalArgumentException si dos manejadores atienden el mismo comando o tipo
     */
    public ServicioPreparacion(RepositorioPreparaciones repositorio, List<ManejadorComando<?>> manejadores,
                               Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.mapeador = MapeadorComandos.crear();
        for (var manejador : manejadores) {
            var repetido = porClase.putIfAbsent(manejador.claseComando(), manejador) != null
                    || porTipo.putIfAbsent(manejador.tipo(), manejador) != null;
            if (repetido) {
                throw new IllegalArgumentException("Manejador repetido: " + manejador.tipo());
            }
        }
    }

    /**
     * Valida el comando sin escribir y guarda la preparacion con el hash del token.
     *
     * @param contexto negocio y actor de la peticion
     * @param comando  comando a preparar
     * @return el token en claro, el resumen y el vencimiento
     * @throws ContabilidadException con el codigo de la regla que el comando incumple
     * @throws IllegalArgumentException si no hay manejador para el comando
     */
    public PreparacionCreada preparar(ContextoNegocio contexto, Comando comando) {
        Objects.requireNonNull(contexto, "contexto");
        Objects.requireNonNull(comando, "comando");
        var manejador = porClase.get(comando.getClass());
        if (manejador == null) {
            throw new IllegalArgumentException("No hay manejador para " + comando.getClass().getSimpleName());
        }
        var resumen = resumenDe(manejador, contexto, comando);
        var json = serializar(comando);
        var token = nuevoToken();
        var expiraEn = reloj.instant().plus(VIGENCIA_TOKEN);
        repositorio.guardar(new Preparacion(hash(token), contexto.negocioId(), contexto.actor(), manejador.tipo(),
                hash(json), json, resumen, expiraEn, null));
        return new PreparacionCreada(token, resumen, expiraEn, List.of());
    }

    /**
     * Ejecuta el comando preparado con ese token.
     *
     * @param contexto negocio y actor de la peticion; deben ser los mismos que al preparar
     * @param token    token en claro devuelto al preparar
     * @return el resultado del comando
     * @throws ContabilidadException {@code TOKEN_INVALIDO}, {@code TOKEN_USADO}, {@code TOKEN_EXPIRADO} o
     *                               {@code COMANDO_CAMBIO}; o el codigo de la regla que el comando ya no cumple
     */
    public ResultadoComando confirmar(ContextoNegocio contexto, String token) {
        Objects.requireNonNull(contexto, "contexto");
        if (token == null || token.isBlank() || token.length() > MAX_CARACTERES_DEL_TOKEN) {
            throw new ContabilidadException(CodigoError.TOKEN_INVALIDO);
        }
        var tokenHash = hash(token);
        var fila = repositorio.porHash(tokenHash)
                .filter(f -> f.negocioId().equals(contexto.negocioId()) && f.actor().equals(contexto.actor()))
                .orElseThrow(() -> new ContabilidadException(CodigoError.TOKEN_INVALIDO));
        if (fila.usadoEn() != null) {
            throw new ContabilidadException(CodigoError.TOKEN_USADO);
        }
        var ahora = reloj.instant();
        if (!ahora.isBefore(fila.expiraEn())) {
            throw vaciandoAntesDeFallar(tokenHash, new ContabilidadException(CodigoError.TOKEN_EXPIRADO));
        }
        var json = fila.comando();
        var manejador = porTipo.get(fila.comandoTipo());
        if (json == null || manejador == null || !hash(json).equals(fila.comandoHash())) {
            throw vaciandoAntesDeFallar(tokenHash, new ContabilidadException(CodigoError.COMANDO_CAMBIO));
        }
        if (!repositorio.marcarUsada(tokenHash, ahora)) {
            throw new ContabilidadException(CodigoError.TOKEN_USADO);
        }
        ResultadoComando resultado;
        try {
            resultado = ejecutar(manejador, contexto, json);
        } catch (RuntimeException error) {
            throw vaciandoAntesDeFallar(tokenHash, error);
        }
        vaciarTrasExito(tokenHash);
        return resultado;
    }

    /**
     * Vacia el comando guardado y devuelve el error original. Si vaciar tambien falla,
     * ese fallo queda como suprimido y no tapa el error que la persona debe ver.
     */
    private <E extends RuntimeException> E vaciandoAntesDeFallar(String tokenHash, E error) {
        try {
            repositorio.vaciar(tokenHash);
        } catch (RuntimeException fallo) {
            error.addSuppressed(fallo);
        }
        return error;
    }

    /** Tras un exito, un fallo al vaciar no debe convertir un alta que si ocurrio en un error. */
    private void vaciarTrasExito(String tokenHash) {
        try {
            repositorio.vaciar(tokenHash);
        } catch (RuntimeException fallo) {
            // Solo la clase: el mensaje de la base podria citar el contenido de la fila.
            LOG.error("No se pudo vaciar una preparacion ya confirmada ({}); la purga de 24 horas la limpiara",
                    fallo.getClass().getSimpleName());
        }
    }

    private <C extends Comando> String resumenDe(ManejadorComando<C> manejador, ContextoNegocio contexto,
                                                 Comando comando) {
        return manejador.preparar(contexto, manejador.claseComando().cast(comando));
    }

    private <C extends Comando> ResultadoComando ejecutar(ManejadorComando<C> manejador, ContextoNegocio contexto,
                                                          String json) {
        C comando;
        try {
            comando = mapeador.readValue(json, manejador.claseComando());
        } catch (JsonProcessingException e) {
            // No se propaga la causa: el mensaje de Jackson puede citar el contenido del comando.
            throw new ContabilidadException(CodigoError.COMANDO_CAMBIO);
        }
        return manejador.confirmar(contexto, comando);
    }

    private String serializar(Comando comando) {
        try {
            return mapeador.writeValueAsString(comando);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("El comando no se puede serializar: " + comando.getClass().getName());
        }
    }

    private String nuevoToken() {
        var bytes = new byte[BYTES_DEL_TOKEN];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String texto) {
        try {
            var resumen = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible", e);
        }
    }
}
