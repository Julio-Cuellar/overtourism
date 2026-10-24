package mx.negocio360.inventario.infraestructura;

import java.time.Clock;
import mx.negocio360.compartido.transaccion.EjecutorEnNegocio;
import mx.negocio360.compartido.transaccion.FronteraTransaccional;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.ConsumosServicio;
import mx.negocio360.inventario.aplicacion.InventarioServicio;
import mx.negocio360.inventario.aplicacion.Manejadores;
import mx.negocio360.inventario.aplicacion.SecuenciaDeIds;
import mx.negocio360.inventario.aplicacion.ServicioPreparacion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Inventario en PostgreSQL, solo con {@code inventario.almacen=postgres} (F6): repositorios JDBC con seguridad por fila,
 * eventos por el outbox y cada llamada a la API en una sola transaccion ({@link FronteraTransaccional}, D-103), que
 * incluye el movimiento, el saldo, la fila del consumo y sus eventos: registrar o revertir un consumo es todo o nada
 * (K-02). El token de preparar se marca como usado en su propia transaccion (lo
 * hace el repositorio), aparte de la del movimiento.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "inventario.almacen", havingValue = "postgres")
public class InventarioPostgresConfiguracion {

    /**
     * API publica de Inventario, con cada llamada en una transaccion.
     *
     * @param jdbc          cliente de la base
     * @param transacciones gestor de transacciones
     * @param operaciones   plantilla de transacciones
     * @param bus           bus de eventos de Spring, que con Spring Modulith escribe el outbox
     * @param reloj         reloj de la plataforma
     * @return Inventario listo para usarse
     */
    @Bean
    public InventarioApi inventarioApi(JdbcClient jdbc, PlatformTransactionManager transacciones,
                                       TransactionOperations operaciones, ApplicationEventPublisher bus, Clock reloj) {
        var negocio = new EjecutorEnNegocio(jdbc, transacciones);
        var almacen = new InventarioJdbc(jdbc, negocio);
        var eventos = new PublicadorEventosEnBus(bus);
        // Los movimientos manuales y el consumo por venta comparten la secuencia: el kardex se ordena por id.
        var ids = new SecuenciaDeIds();
        var manejadores = Manejadores.todos(almacen, almacen, almacen, eventos, reloj, ids);
        var consumo = new ConsumosServicio(almacen, almacen, almacen, almacen, eventos, reloj, ids);
        var servicio = new InventarioServicio(new ServicioPreparacion(new PreparacionesJdbc(jdbc, negocio), manejadores,
                reloj), almacen, almacen, almacen, consumo);
        return FronteraTransaccional.envolver(InventarioApi.class, servicio, operaciones);
    }
}
