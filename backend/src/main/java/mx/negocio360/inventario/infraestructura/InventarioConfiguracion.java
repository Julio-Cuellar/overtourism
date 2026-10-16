package mx.negocio360.inventario.infraestructura;

import java.time.Clock;
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
import org.springframework.context.annotation.Profile;

/**
 * Enciende Inventario con repositorios en memoria solo con {@code inventario.almacen=memoria} (el perfil de desarrollo lo
 * fija) y nunca con el perfil {@code produccion}: sin base de datos los saldos se pierden al reiniciar, asi que no se
 * enciende por accidente. Expone {@link InventarioApi}, que es lo que usan los demas modulos (hoy Restaurantes, R-I1 a
 * R-I4). No expone GraphQL: eso es de otro tramo. La version con PostgreSQL llega despues.
 */
@Configuration(proxyBeanMethods = false)
@Profile("!produccion")
@ConditionalOnProperty(name = "inventario.almacen", havingValue = "memoria")
public class InventarioConfiguracion {

    /**
     * Tablas de Inventario en memoria.
     *
     * @return almacen unico de articulos, saldos, movimientos y consumos
     */
    @Bean
    InventarioEnMemoria inventarioEnMemoria() {
        return new InventarioEnMemoria();
    }

    /**
     * API publica de Inventario.
     *
     * @param almacen tablas de Inventario
     * @param bus     bus de eventos de Spring
     * @param reloj   reloj de la plataforma
     * @return Inventario listo para usarse
     */
    @Bean
    public InventarioApi inventarioApi(InventarioEnMemoria almacen, ApplicationEventPublisher bus, Clock reloj) {
        var eventos = new PublicadorEventosSpring(bus);
        // Los movimientos manuales y el consumo por venta comparten la secuencia: el kardex se ordena por id.
        var ids = new SecuenciaDeIds();
        var manejadores = Manejadores.todos(almacen, almacen, almacen, eventos, reloj, ids);
        var preparacion = new ServicioPreparacion(new PreparacionesDeInventarioEnMemoria(), manejadores, reloj);
        var consumo = new ConsumosServicio(almacen, almacen, almacen, almacen, eventos, reloj, ids);
        return new InventarioServicio(preparacion, almacen, almacen, almacen, consumo);
    }
}
