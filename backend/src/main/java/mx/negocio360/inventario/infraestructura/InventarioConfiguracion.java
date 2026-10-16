package mx.negocio360.inventario.infraestructura;

import java.time.Clock;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.InventarioServicio;
import mx.negocio360.inventario.aplicacion.Manejadores;
import mx.negocio360.inventario.aplicacion.ServicioPreparacion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enciende Inventario al arrancar con su esquema en memoria, solo con {@code inventario.almacen=memoria} (el perfil
 * {@code desarrollo} lo fija). Sin base de datos, los articulos y movimientos se pierden al reiniciar: la version con
 * PostgreSQL esta pendiente. Los permisos no se inyectan: cada llamada del canal recibe la {@code Autorizacion} de la
 * sesion.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "inventario.almacen", havingValue = "memoria")
public class InventarioConfiguracion {

    /**
     * API publica de Inventario.
     *
     * @param bus  bus de eventos de Spring
     * @param reloj reloj de la plataforma
     * @return Inventario listo para usarse
     */
    @Bean
    public InventarioApi inventarioApi(ApplicationEventPublisher bus, Clock reloj) {
        var almacen = new InventarioEnMemoria();
        var manejadores = Manejadores.todos(almacen, almacen, almacen, new PublicadorEventosSpring(bus), reloj);
        return new InventarioServicio(new ServicioPreparacion(new PreparacionesEnMemoria(), manejadores, reloj),
                almacen, almacen, almacen);
    }
}
