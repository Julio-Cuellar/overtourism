package mx.negocio360.inventario.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import mx.negocio360.compartido.graphql.comandos.ModuloDeComandos;
import mx.negocio360.compartido.graphql.comandos.PreparacionVista;
import mx.negocio360.compartido.graphql.comandos.ResultadoVista;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.AjustePorConteoComando;
import mx.negocio360.inventario.aplicacion.BajaArticuloComando;
import mx.negocio360.inventario.aplicacion.Comando;
import mx.negocio360.inventario.aplicacion.CrearArticuloComando;
import mx.negocio360.inventario.aplicacion.DefinirUnidadPropiaComando;
import mx.negocio360.inventario.aplicacion.EditarArticuloComando;
import mx.negocio360.inventario.aplicacion.RegistrarEntradaComando;
import mx.negocio360.inventario.aplicacion.RegistrarMermaComando;
import mx.negocio360.inventario.aplicacion.RegistrarSalidaComando;
import mx.negocio360.inventario.seguridad.PermisosDeInventario;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Las variantes de {@code ComandoInput} de Inventario para el despachador de la plataforma (D-125): los ocho comandos del
 * nucleo. Convierte los campos de GraphQL al comando del modulo, sin campos de mas.
 *
 * <p>Inventario conserva su propio patron preparar -&gt; confirmar, asi que el permiso del comando se exige aqui, en las dos
 * puntas: al preparar, el de la variante; al confirmar, el del tipo de comando que guarda el token, <b>antes</b> de gastarlo
 * (D-126). La puerta de la plataforma ya comprobo el de la variante.
 */
@Component
@ConditionalOnProperty(name = "inventario.almacen")
public final class ComandosDeInventario implements ModuloDeComandos {

    /** Prefijo de los tokens de Inventario. */
    public static final String PREFIJO = "inventario.";

    /** Variante de GraphQL → comando y permiso. */
    private record Variante(Class<? extends Comando> comando, String permiso) {
    }

    private static final Map<String, Variante> VARIANTES = Map.of(
            "crearArticulo", new Variante(CrearArticuloComando.class, PermisosDeInventario.ARTICULO_GESTIONAR),
            "editarArticulo", new Variante(EditarArticuloComando.class, PermisosDeInventario.ARTICULO_GESTIONAR),
            "definirUnidadPropia",
            new Variante(DefinirUnidadPropiaComando.class, PermisosDeInventario.ARTICULO_GESTIONAR),
            "bajaArticulo", new Variante(BajaArticuloComando.class, PermisosDeInventario.ARTICULO_GESTIONAR),
            "registrarEntradaInventario",
            new Variante(RegistrarEntradaComando.class, PermisosDeInventario.MOVIMIENTO_REGISTRAR),
            "registrarSalidaInventario",
            new Variante(RegistrarSalidaComando.class, PermisosDeInventario.MOVIMIENTO_REGISTRAR),
            "registrarMermaInventario",
            new Variante(RegistrarMermaComando.class, PermisosDeInventario.MOVIMIENTO_REGISTRAR),
            "ajustarPorConteo", new Variante(AjustePorConteoComando.class, PermisosDeInventario.CONTEO_AJUSTAR));

    /** Permiso de cada tipo de comando guardado en la preparacion. */
    private static final Map<String, String> PERMISO_POR_TIPO = Map.of(
            "crear_articulo", PermisosDeInventario.ARTICULO_GESTIONAR,
            "editar_articulo", PermisosDeInventario.ARTICULO_GESTIONAR,
            "definir_unidad_propia", PermisosDeInventario.ARTICULO_GESTIONAR,
            "baja_articulo", PermisosDeInventario.ARTICULO_GESTIONAR,
            "registrar_entrada", PermisosDeInventario.MOVIMIENTO_REGISTRAR,
            "registrar_salida", PermisosDeInventario.MOVIMIENTO_REGISTRAR,
            "registrar_merma", PermisosDeInventario.MOVIMIENTO_REGISTRAR,
            "ajuste_por_conteo", PermisosDeInventario.CONTEO_AJUSTAR);

    private final InventarioApi inventario;
    private final ObjectMapper json = com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .addModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .build();

    /**
     * Arma el modulo de comandos.
     *
     * @param inventario API de Inventario
     */
    public ComandosDeInventario(InventarioApi inventario) {
        this.inventario = Objects.requireNonNull(inventario, "inventario");
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
        var definicion = VARIANTES.get(variante);
        if (definicion == null) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.COMANDO_NO_DISPONIBLE);
        }
        var comando = convertir(campos, definicion.comando());
        var contexto = entrada.exigirContexto();
        entrada.autorizacion().exigir(definicion.permiso());
        var creada = inventario.preparar(contexto, comando);
        return new PreparacionVista(creada.token(), creada.resumen(), creada.expiraEn(), creada.advertencias());
    }

    @Override
    public ResultadoVista confirmar(EntradaGraphQl entrada, String tokenSinPrefijo) {
        var contexto = entrada.exigirContexto();
        // Si el token es vigente, se exige el permiso de su comando antes de gastarlo. Si no lo es, confirmar da el error.
        inventario.tipoDeComando(contexto, tokenSinPrefijo)
                .ifPresent(tipo -> entrada.autorizacion().exigir(PERMISO_POR_TIPO.getOrDefault(tipo, "")));
        var resultado = inventario.confirmar(contexto, tokenSinPrefijo);
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
