package mx.negocio360.ventas.infraestructura;

import java.time.Clock;
import mx.negocio360.compartido.comandos.RepositorioPreparacionesJdbc;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import mx.negocio360.compartido.transaccion.FronteraTransaccional;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.VerificadorDeTurno;
import mx.negocio360.ventas.aplicacion.VentasServicio;
import mx.negocio360.ventas.aplicacion.puertos.CatalogoDeVentas;
import mx.negocio360.ventas.aplicacion.puertos.CuponesDePasaporte;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Ventas en PostgreSQL, solo con {@code ventas.almacen=postgres} (F6): repositorios JDBC con seguridad por fila, eventos
 * por el outbox y cada llamada a la API en una sola transaccion ({@link FronteraTransaccional}, D-103), que incluye la
 * venta, su folio y sus eventos. El token de preparar se marca como usado en su propia transaccion (lo hace el
 * repositorio), aparte de la de la venta.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "ventas.almacen", havingValue = "postgres")
public class VentasPostgresConfiguracion {

    /**
     * API publica de Ventas, con cada llamada en una transaccion.
     *
     * @param catalogo      productos y precios (VT-02)
     * @param cupones       cupones de Pasaporte (VT-07)
     * @param turnos        Caja dice si el turno esta abierto (VT-01)
     * @param jdbc          cliente de la base
     * @param transacciones gestor de transacciones
     * @param operaciones   plantilla de transacciones
     * @param bus           bus de eventos de Spring, que con Spring Modulith escribe el outbox
     * @param reloj         reloj de la plataforma
     * @return Ventas listo para usarse
     */
    @Bean
    public VentasApi ventasApi(CatalogoDeVentas catalogo, CuponesDePasaporte cupones, VerificadorDeTurno turnos,
                               JdbcClient jdbc, PlatformTransactionManager transacciones,
                               TransactionOperations operaciones, ApplicationEventPublisher bus, Clock reloj) {
        var negocio = new EjecutorEnNegocio(jdbc, transacciones);
        var servicio = VentasServicio.armar(catalogo, cupones, turnos, new RepositorioVentasJdbc(jdbc, negocio),
                new RepositorioPreparacionesJdbc(jdbc, negocio, "ventas"), new PublicadorEventosEnBus(bus), reloj);
        return FronteraTransaccional.envolver(VentasApi.class, servicio, operaciones);
    }
}
