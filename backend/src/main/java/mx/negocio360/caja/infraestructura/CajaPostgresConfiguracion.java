package mx.negocio360.caja.infraestructura;

import java.math.BigDecimal;
import java.time.Clock;
import mx.negocio360.caja.CajaApi;
import mx.negocio360.caja.aplicacion.CajaServicio;
import mx.negocio360.caja.aplicacion.ConsumidorVentas;
import mx.negocio360.caja.aplicacion.ResumenVentasDesdeVentasApi;
import mx.negocio360.caja.aplicacion.VerificadorDeTurnoDesdeCaja;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.aplicacion.puertos.ReglasCaja;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.compartido.comandos.RepositorioPreparacionesJdbc;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import mx.negocio360.compartido.transaccion.FronteraTransaccional;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.VerificadorDeTurno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Caja en PostgreSQL, solo con {@code caja.almacen=postgres} (F5 y F6): repositorios JDBC con seguridad por fila, cada
 * llamada a la API en una sola transaccion ({@link FronteraTransaccional}, D-103) con sus eventos en el outbox, y la
 * escucha de {@code venta.registrada} y {@code venta.anulada} con {@code @ApplicationModuleListener}
 * ({@link EscuchaDeVentasEnOutbox}): despues de confirmar la venta, en otro hilo y al menos una vez. El verificador de
 * turno que pide Ventas (D-145) lee las mismas tablas.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "caja.almacen", havingValue = "postgres")
public class CajaPostgresConfiguracion {

    /**
     * Cajas y turnos.
     *
     * @param jdbc          cliente de la base
     * @param transacciones gestor de transacciones
     * @return el repositorio JDBC
     */
    @Bean
    public RepositorioCaja repositorioDeCaja(JdbcClient jdbc, PlatformTransactionManager transacciones) {
        return new RepositorioCajaJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones));
    }

    /**
     * Movimientos, ventas aplicadas, eventos procesados y cortes.
     *
     * @param jdbc          cliente de la base
     * @param transacciones gestor de transacciones
     * @return el libro JDBC
     */
    @Bean
    public LibroDeCaja libroDeCaja(JdbcClient jdbc, PlatformTransactionManager transacciones) {
        return new LibroDeCajaJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones));
    }

    /**
     * Respuesta de Caja a «¿el turno sigue abierto?» (VT-01, D-145).
     *
     * @param cajas cajas y turnos
     * @param reloj reloj de la plataforma
     * @return el verificador que usa Ventas
     */
    @Bean
    public VerificadorDeTurno verificadorDeTurno(RepositorioCaja cajas, Clock reloj) {
        return new VerificadorDeTurnoDesdeCaja(cajas, reloj);
    }

    /**
     * API publica de Caja, con cada llamada en una transaccion.
     *
     * @param cajas         cajas y turnos
     * @param libro         movimientos, ventas aplicadas, eventos y cortes
     * @param ventas        API de Ventas, para el corte (J1, CJ-07)
     * @param jdbc          cliente de la base
     * @param transacciones gestor de transacciones
     * @param operaciones   plantilla de transacciones
     * @param bus           bus de eventos de Spring, que con Spring Modulith escribe el outbox
     * @param reloj         reloj de la plataforma
     * @param umbral        diferencia de efectivo que pide autorizacion (CJ-08), de {@code caja.umbral-diferencia}
     * @return Caja listo para usarse
     */
    @Bean
    public CajaApi cajaApi(RepositorioCaja cajas, LibroDeCaja libro, VentasApi ventas, JdbcClient jdbc,
                           PlatformTransactionManager transacciones, TransactionOperations operaciones,
                           ApplicationEventPublisher bus, Clock reloj,
                           @Value("${caja.umbral-diferencia:" + CajaEnMemoriaConfiguracion.UMBRAL_POR_OMISION
                                   + "}") BigDecimal umbral) {
        ReglasCaja reglas = negocioId -> umbral;
        var preparaciones = new RepositorioPreparacionesJdbc(jdbc, new EjecutorEnNegocio(jdbc, transacciones), "caja");
        var servicio = CajaServicio.armar(cajas, libro, preparaciones, new PublicadorEventosEnBus(bus),
                new ResumenVentasDesdeVentasApi(ventas), reglas, reloj);
        return FronteraTransaccional.envolver(CajaApi.class, servicio, operaciones);
    }

    /**
     * Escucha de los eventos de Ventas por el outbox (CF-20, CF-21).
     *
     * @param cajas cajas y turnos
     * @param libro movimientos, ventas aplicadas, eventos y cortes
     * @return la escucha registrada en el bus de Spring Modulith
     */
    @Bean
    public EscuchaDeVentasEnOutbox escuchaDeVentasEnOutbox(RepositorioCaja cajas, LibroDeCaja libro) {
        return new EscuchaDeVentasEnOutbox(new ConsumidorVentas(cajas, libro));
    }
}
