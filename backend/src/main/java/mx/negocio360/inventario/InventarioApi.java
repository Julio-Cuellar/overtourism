package mx.negocio360.inventario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.inventario.aplicacion.ArticuloVista;
import mx.negocio360.inventario.aplicacion.CantidadEnBase;
import mx.negocio360.inventario.aplicacion.CantidadPedida;
import mx.negocio360.inventario.aplicacion.Comando;
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

    /** Lleva hasta 100 cantidades a la unidad base de su articulo (para validar y calcular recetas). */
    List<CantidadEnBase> aBase(ContextoNegocio contexto, List<CantidadPedida> lineas);
}
