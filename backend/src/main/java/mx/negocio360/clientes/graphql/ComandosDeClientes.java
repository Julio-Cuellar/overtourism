package mx.negocio360.clientes.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import mx.negocio360.clientes.ClientesApi;
import mx.negocio360.clientes.aplicacion.Comando;
import mx.negocio360.clientes.aplicacion.EditarClienteComando;
import mx.negocio360.clientes.aplicacion.ManejadorEditarCliente;
import mx.negocio360.clientes.aplicacion.ManejadorPublicarAviso;
import mx.negocio360.clientes.aplicacion.ManejadorRegistrarCliente;
import mx.negocio360.clientes.aplicacion.MapeadorComandos;
import mx.negocio360.clientes.aplicacion.PublicarAvisoComando;
import mx.negocio360.clientes.aplicacion.RegistrarClienteComando;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import mx.negocio360.compartido.graphql.comandos.ModuloDeComandos;
import mx.negocio360.compartido.graphql.comandos.PreparacionVista;
import mx.negocio360.compartido.graphql.comandos.ResultadoVista;
import org.springframework.stereotype.Component;

/**
 * Las variantes de {@code ComandoInput} de Clientes que ya tienen caso de uso: {@code registrarCliente},
 * {@code editarCliente} y {@code publicarAvisoPrivacidad} (D-125). Las demas (consentimiento, anonimizar, ARCO) siguen
 * declaradas en el esquema y responden {@code COMANDO_NO_DISPONIBLE} hasta que existan sus casos de uso.
 *
 * <p>Clientes conserva su propio patron preparar -&gt; confirmar, asi que el permiso del comando se exige aqui, en las dos
 * puntas: al preparar, el de la variante; al confirmar, el del tipo de comando que guarda el token, <b>antes</b> de gastarlo
 * (D-126). La puerta de la plataforma ya comprobo el de la variante.
 */
@Component
public final class ComandosDeClientes implements ModuloDeComandos {

    /** Prefijo de los tokens de Clientes. */
    public static final String PREFIJO = "clientes.";

    /** Permiso de cada tipo de comando guardado en la preparacion. */
    private static final Map<String, String> PERMISO_POR_TIPO = Map.of(
            ManejadorRegistrarCliente.TIPO, "clientes.cliente.registrar",
            ManejadorEditarCliente.TIPO, "clientes.cliente.editar",
            ManejadorPublicarAviso.TIPO, "clientes.aviso.publicar");

    private final ClientesApi clientes;
    private final ObjectMapper json = MapeadorComandos.crear();

    /**
     * Arma el modulo de comandos.
     *
     * @param clientes API de Clientes
     */
    public ComandosDeClientes(ClientesApi clientes) {
        this.clientes = Objects.requireNonNull(clientes, "clientes");
    }

    @Override
    public String prefijo() {
        return PREFIJO;
    }

    @Override
    public Set<String> variantes() {
        return Set.of("registrarCliente", "editarCliente", "publicarAvisoPrivacidad");
    }

    @Override
    public boolean confirmaSinSesion() {
        return false;
    }

    @Override
    public PreparacionVista preparar(EntradaGraphQl entrada, String variante, Map<String, Object> campos) {
        Comando comando;
        String permiso;
        switch (variante) {
            case "registrarCliente" -> {
                comando = convertir(campos, RegistrarClienteComando.class);
                permiso = PERMISO_POR_TIPO.get(ManejadorRegistrarCliente.TIPO);
            }
            case "editarCliente" -> {
                comando = convertir(campos, EditarClienteComando.class);
                permiso = PERMISO_POR_TIPO.get(ManejadorEditarCliente.TIPO);
            }
            case "publicarAvisoPrivacidad" -> {
                comando = convertir(campos, PublicarAvisoComando.class);
                permiso = PERMISO_POR_TIPO.get(ManejadorPublicarAviso.TIPO);
            }
            default -> throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE);
        }
        var contexto = entrada.exigirContexto();
        entrada.autorizacion().exigir(permiso);
        var creada = clientes.preparar(contexto, comando);
        return new PreparacionVista(creada.token(), creada.resumen(), creada.expiraEn(), creada.advertencias());
    }

    @Override
    public ResultadoVista confirmar(EntradaGraphQl entrada, String tokenSinPrefijo) {
        var contexto = entrada.exigirContexto();
        // Si el token es vigente, se exige el permiso de su comando antes de gastarlo. Si no lo es, confirmar da el error.
        clientes.tipoDeComando(contexto, tokenSinPrefijo)
                .ifPresent(tipo -> entrada.autorizacion().exigir(PERMISO_POR_TIPO.getOrDefault(tipo, "")));
        var resultado = clientes.confirmar(contexto, tokenSinPrefijo);
        return new ResultadoVista(true, resultado.codigo(), resultado.mensajePlantilla(), resultado.recursoId(),
                List.of());
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
