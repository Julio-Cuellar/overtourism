package mx.negocio360.ventas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.comandos.PreparacionCreada;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.paginacion.Pagina;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.ventas.dominio.Venta;

/**
 * Funciones publicas de Ventas ({@code docs/diseno/ventas.md}, seccion 5). Los resolvedores GraphQL y los demas
 * modulos llaman a esta interfaz y nunca a los repositorios. El negocio y el actor salen siempre del contexto, nunca
 * de un argumento (SG-02). Cada funcion de usuario recibe tambien la {@link Autorizacion} de la sesion y exige su
 * permiso: es la segunda barrera despues de la puerta de GraphQL (D-126). Los fallos salen como
 * {@link mx.negocio360.ventas.dominio.VentasException} con su codigo, o como
 * {@link mx.negocio360.compartido.seguridad.PermisoDenegadoException} si falta un permiso.
 */
public interface VentasApi {

    /**
     * Prepara un comando (VF-10 RegistrarVenta o VF-11 AnularVenta) sin escribir nada de negocio.
     *
     * @param contexto     negocio y actor
     * @param autorizacion permisos de la persona; cobrar pide {@code ventas.venta.registrar} y anular
     *                     {@code ventas.venta.anular}
     * @param comando      {@code RegistrarVentaComando} o {@code AnularVentaComando}
     * @return token de un solo uso, resumen para confirmar y vencimiento (5 minutos, D-23)
     */
    PreparacionCreada preparar(ContextoNegocio contexto, Autorizacion autorizacion, Comando comando);

    /**
     * Confirma el comando preparado: vuelve a exigir el permiso <b>antes de gastar el token</b> (D-126), revalida,
     * escribe y publica en una sola transaccion.
     *
     * @param contexto     el mismo negocio y actor que prepararon
     * @param autorizacion permisos de la persona ahora
     * @param token        token en claro
     * @return resultado con id de la venta y datos ({@code folio}, {@code total}, {@code cambio} al registrar)
     */
    ResultadoComando confirmar(ContextoNegocio contexto, Autorizacion autorizacion, String token);

    /**
     * VF-01: una venta del negocio, con lineas y pagos. Exige {@code ventas.venta.ver}.
     *
     * @param contexto     negocio
     * @param autorizacion permisos de la persona
     * @param ventaId      venta
     * @return la venta, o vacio si no existe en ese negocio
     */
    Optional<Venta> venta(ContextoNegocio contexto, Autorizacion autorizacion, UUID ventaId);

    /**
     * VF-02: ventas del negocio de la mas reciente a la mas antigua, con filtros y cursor (D-131). Exige
     * {@code ventas.venta.ver}.
     *
     * @param contexto     negocio
     * @param autorizacion permisos de la persona
     * @param filtro       filtros opcionales; nulo es sin filtros
     * @param primeros     tamano de pagina, de 1 a 100
     * @param despues      cursor de la pagina anterior, o nulo
     * @return la pagina pedida
     * @throws mx.negocio360.ventas.dominio.VentasException PAGINACION_INVALIDA o FILTRO_INVALIDO
     */
    Pagina<Venta> ventas(ContextoNegocio contexto, Autorizacion autorizacion, FiltroVentas filtro, int primeros,
                         String despues);

    /**
     * VF-03: conteos, totales por metodo y preparaciones vigentes de un turno. Es la consulta interna que usa Caja para
     * el corte (CJ-06, CJ-07): no pide permiso de usuario porque la llama un modulo, no una persona.
     *
     * @param contexto negocio
     * @param turnoId  turno de Caja
     * @return resumen del turno; ceros si no hay ventas
     */
    ResumenTurno resumenTurno(ContextoNegocio contexto, UUID turnoId);

    /**
     * VF-03 para una persona (el panel): lo mismo que {@link #resumenTurno(ContextoNegocio, UUID)} pero exige
     * {@code ventas.venta.ver}.
     *
     * @param contexto     negocio
     * @param autorizacion permisos de la persona
     * @param turnoId      turno de Caja
     * @return resumen del turno; ceros si no hay ventas o si el turno es de otro negocio
     */
    ResumenTurno resumenTurno(ContextoNegocio contexto, Autorizacion autorizacion, UUID turnoId);

    /**
     * G1: productos que se pueden vender, de las fuentes del negocio. Exige {@code ventas.venta.registrar}.
     *
     * @param contexto     negocio
     * @param autorizacion permisos de la persona
     * @param texto        texto a buscar, de hasta 80 caracteres, o nulo
     * @param primeros     cuantos, de 1 a 50
     * @return productos activos que coinciden
     */
    List<mx.negocio360.compartido.catalogo.ProductoParaVenta> productosParaVenta(ContextoNegocio contexto,
                                                                                 Autorizacion autorizacion,
                                                                                 String texto, int primeros);
}
