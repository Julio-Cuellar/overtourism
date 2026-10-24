package mx.negocio360.inventario;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.inventario.aplicacion.ArticuloBase;
import mx.negocio360.inventario.aplicacion.ArticuloVista;
import mx.negocio360.inventario.aplicacion.CantidadEnBase;
import mx.negocio360.inventario.aplicacion.CantidadPedida;
import mx.negocio360.inventario.aplicacion.Comando;
import mx.negocio360.inventario.aplicacion.ConsumoResultado;
import mx.negocio360.inventario.aplicacion.LineaDeConsumo;
import mx.negocio360.inventario.aplicacion.MovimientoVista;
import mx.negocio360.inventario.aplicacion.Pagina;
import mx.negocio360.inventario.aplicacion.PreparacionCreada;
import mx.negocio360.inventario.aplicacion.ResultadoComando;

/**
 * Funciones publicas del modulo (seccion 6 del diseno). Los resolvedores GraphQL y los demas modulos llaman a esta
 * interfaz y nunca a los repositorios. El negocio y el actor salen siempre del contexto, no de un argumento.
 */
public interface InventarioApi {

    /** Valida un comando sin escribir y devuelve el token de un solo uso con el resumen para confirmar. */
    PreparacionCreada preparar(ContextoNegocio contexto, Comando comando);

    /** Ejecuta el comando preparado. Falla con TOKEN_* si el token no sirve, o con COMANDO_CAMBIO si el mundo cambio. */
    ResultadoComando confirmar(ContextoNegocio contexto, String token);

    /** El articulo con su saldo, activo o no. Vacio si no existe en el negocio. */
    Optional<ArticuloVista> articulo(ContextoNegocio contexto, UUID articuloId);

    /**
     * Articulos activos por parte del nombre, de {@code primeros} en {@code primeros} (maximo 100). {@code despues} es
     * el cursor de la pagina anterior.
     */
    Pagina<ArticuloVista> articulos(ContextoNegocio contexto, String texto, boolean soloAlertas, int primeros,
                                    String despues);

    /** Movimientos de un articulo en orden de registro, de {@code primeros} en {@code primeros} (maximo 100). */
    Pagina<MovimientoVista> kardex(ContextoNegocio contexto, UUID articuloId, int primeros, String despues);

    /**
     * Los articulos que existen en el negocio, activos o no, con lo que un vertical necesita para costear: unidad base,
     * CPP, {@code activo} y {@code rastreo} (R-I1 y R-I3). Los ids que no existen o son de otro negocio no aparecen. Hasta
     * 100 ids por llamada.
     */
    Map<UUID, ArticuloBase> articulosPorId(ContextoNegocio contexto, Set<UUID> ids);

    /** Lleva hasta 100 cantidades a la unidad base de su articulo (para validar y calcular recetas). */
    List<CantidadEnBase> aBase(ContextoNegocio contexto, List<CantidadPedida> lineas);

    /**
     * Descuenta el consumo de una causa externa (una venta), sin confirmacion humana porque es consecuencia de una venta
     * ya confirmada (6.4, D-22). Atomico (K-02) e idempotente por (negocio, {@code causaId}, {@code consumidor}) (K-01):
     * repetir la llamada devuelve {@code YA_REGISTRADO} y no vuelve a descontar. Si la anulacion llego antes, responde
     * {@code OMITIDO_ANULADO_PREVIO} (K-04). Cada linea se valua al CPP y puede dejar el saldo en negativo (A-01).
     *
     * @param consumidor {@code vinculo} o {@code restaurantes}; otro valor falla con CONSUMIDOR_INVALIDO
     * @param ocurridoEn cuando paso la venta; nulo es ahora y uno futuro falla con FECHA_FUTURA
     * @param lineas     de 1 a 100 lineas; el mismo articulo puede repetirse
     */
    ConsumoResultado registrarConsumo(ContextoNegocio contexto, String consumidor, UUID causaId, Instant ocurridoEn,
                                      List<LineaDeConsumo> lineas);

    /**
     * Repone al costo original lo consumido por esa causa (K-03), idempotente por la misma llave: repetir devuelve
     * {@code YA_REVERTIDO}. Si el consumo no ha llegado, deja una marca y responde {@code MARCADO_ANULADO_PREVIO} para
     * omitirlo cuando llegue (K-04). Falla con ARTICULO_INACTIVO si algun articulo ya se dio de baja; se puede reintentar.
     */
    ConsumoResultado revertirConsumo(ContextoNegocio contexto, String consumidor, UUID causaId);

    /**
     * El tipo del comando que espera confirmacion con ese token (por ejemplo {@code registrar_entrada}), para que el canal
     * exija el permiso de ese comando antes de gastar el token (D-126).
     *
     * @param contexto negocio y actor que prepararon
     * @param token    token en claro
     * @return el tipo si el token es vigente y de esa persona; vacio en cualquier otro caso
     */
    Optional<String> tipoDeComando(ContextoNegocio contexto, String token);
}
