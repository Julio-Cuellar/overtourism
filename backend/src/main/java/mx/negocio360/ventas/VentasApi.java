package mx.negocio360.ventas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.comandos.PreparacionCreada;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.ventas.dominio.Venta;

/**
 * Funciones publicas de Ventas ({@code docs/diseno/ventas.md}, seccion 5). Los resolvedores GraphQL y los demas
 * modulos llaman a esta interfaz y nunca a los repositorios. El negocio y el actor salen siempre del contexto, nunca
 * de un argumento (SG-02). Los fallos salen como {@link mx.negocio360.ventas.dominio.VentasException} con su codigo.
 */
public interface VentasApi {

    /**
     * Prepara un comando (VF-10 RegistrarVenta o VF-11 AnularVenta) sin escribir nada de negocio.
     *
     * @param contexto negocio y actor
     * @param comando  {@code RegistrarVentaComando} o {@code AnularVentaComando}
     * @return token de un solo uso, resumen para confirmar y vencimiento (5 minutos, D-23)
     */
    PreparacionCreada preparar(ContextoNegocio contexto, Comando comando);

    /**
     * Confirma el comando preparado: revalida, escribe y publica en una sola transaccion.
     *
     * @param contexto el mismo negocio y actor que prepararon
     * @param token    token en claro
     * @return resultado con id de la venta y datos ({@code folio}, {@code total}, {@code cambio} al registrar)
     */
    ResultadoComando confirmar(ContextoNegocio contexto, String token);

    /**
     * VF-01: una venta del negocio, con lineas y pagos.
     *
     * @param contexto negocio
     * @param ventaId  venta
     * @return la venta, o vacio si no existe en ese negocio
     */
    Optional<Venta> venta(ContextoNegocio contexto, UUID ventaId);

    /**
     * VF-02 (forma minima): ventas de un turno, registradas y anuladas.
     *
     * @param contexto negocio
     * @param turnoId  turno de Caja
     * @return ventas en orden de folio
     */
    List<Venta> ventasDelTurno(ContextoNegocio contexto, UUID turnoId);

    /**
     * VF-03: conteos, totales por metodo y preparaciones vigentes de un turno, para el corte de Caja (CJ-06, CJ-07).
     *
     * @param contexto negocio
     * @param turnoId  turno de Caja
     * @return resumen del turno; ceros si no hay ventas
     */
    ResumenTurno resumenTurno(ContextoNegocio contexto, UUID turnoId);
}
