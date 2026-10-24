package mx.negocio360.inventario.aplicacion;

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
import java.util.Optional;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioPreparaciones;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.InventarioException;

/**
 * Patron preparar -> confirmar con token de un solo uso (6.2 del diseno).
 *
 * <p>Preparar valida sin escribir y guarda la preparacion: solo el hash del token, ligado al negocio, al actor y al
 * hash del comando. Confirmar rechaza el token desconocido, ajeno, usado, vencido o cuyo comando cambio; consume el
 * token antes de ejecutar y luego <b>recalcula el resumen</b>: si ya no coincide con el que vio la persona (por
 * ejemplo, el saldo cambio) falla con COMANDO_CAMBIO y no escribe.
 *
 * <p>Esta clase repite la de Clientes a proposito: R1 impide compartir codigo entre modulos fuera de
 * {@code compartido}. Moverla alli es el pendiente HTC-89.
 *
 * <p><b>Contrato transaccional:</b> {@code marcarUsada} se confirma en su propia transaccion (el repositorio de
 * PostgreSQL abre una), aparte de la del manejador, para que un fallo del comando no devuelva el token a la vida; y cada
 * llamada a la API del modulo es una sola transaccion que incluye los eventos (outbox), gracias a
 * {@code FronteraTransaccional}.
 */
public final class ServicioPreparacion {

    public static final Duration VIGENCIA_TOKEN = Duration.ofMinutes(5);
    private static final int BYTES_DEL_TOKEN = 32;
    private static final int MAX_CARACTERES_DEL_TOKEN = 64;

    private final RepositorioPreparaciones repositorio;
    private final Map<Class<?>, ManejadorComando<?>> porClase = new HashMap<>();
    private final Map<String, ManejadorComando<?>> porTipo = new HashMap<>();
    private final Clock reloj;
    private final ObjectMapper mapeador;
    private final SecureRandom aleatorio = new SecureRandom();

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
     * El tipo del comando que espera confirmacion con ese token, para que el canal exija su permiso <b>antes</b> de gastar el
     * token (D-126). Solo responde por una preparacion de ese negocio y ese actor, sin usar ni vencer.
     *
     * @param contexto negocio y actor que prepararon
     * @param token    token en claro
     * @return el tipo, por ejemplo {@code registrar_entrada}, si el token es vigente
     */
    public Optional<String> tipoDeComando(ContextoNegocio contexto, String token) {
        if (token == null || token.isBlank() || token.length() > MAX_CARACTERES_DEL_TOKEN) {
            return Optional.empty();
        }
        var ahora = reloj.instant();
        return repositorio.porHash(contexto.negocioId(), hash(token))
                .filter(f -> f.actor().equals(contexto.actor()))
                .filter(f -> f.usadoEn() == null && ahora.isBefore(f.expiraEn()))
                .map(Preparacion::comandoTipo);
    }

    public ResultadoComando confirmar(ContextoNegocio contexto, String token) {
        Objects.requireNonNull(contexto, "contexto");
        if (token == null || token.isBlank() || token.length() > MAX_CARACTERES_DEL_TOKEN) {
            throw new InventarioException(CodigoError.TOKEN_INVALIDO);
        }
        var tokenHash = hash(token);
        var fila = repositorio.porHash(contexto.negocioId(), tokenHash)
                .filter(f -> f.actor().equals(contexto.actor()))
                .orElseThrow(() -> new InventarioException(CodigoError.TOKEN_INVALIDO));
        if (fila.usadoEn() != null) {
            throw new InventarioException(CodigoError.TOKEN_USADO);
        }
        var ahora = reloj.instant();
        if (!ahora.isBefore(fila.expiraEn())) {
            throw new InventarioException(CodigoError.TOKEN_EXPIRADO);
        }
        var json = fila.comando();
        var manejador = porTipo.get(fila.comandoTipo());
        if (json == null || manejador == null || !hash(json).equals(fila.comandoHash())) {
            throw new InventarioException(CodigoError.COMANDO_CAMBIO);
        }
        if (!repositorio.marcarUsada(contexto.negocioId(), tokenHash, ahora)) {
            throw new InventarioException(CodigoError.TOKEN_USADO);
        }
        return ejecutar(manejador, contexto, json, fila.resumen());
    }

    private <C extends Comando> String resumenDe(ManejadorComando<C> manejador, ContextoNegocio contexto,
                                                 Comando comando) {
        return manejador.preparar(contexto, manejador.claseComando().cast(comando));
    }

    private <C extends Comando> ResultadoComando ejecutar(ManejadorComando<C> manejador, ContextoNegocio contexto,
                                                          String json, String resumenPreparado) {
        C comando;
        try {
            comando = mapeador.readValue(json, manejador.claseComando());
        } catch (JsonProcessingException e) {
            // No se propaga la causa: el mensaje de Jackson puede citar el contenido del comando.
            throw new InventarioException(CodigoError.COMANDO_CAMBIO);
        }
        return manejador.confirmar(contexto, comando, resumenPreparado);
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
