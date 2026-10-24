package mx.negocio360.restaurantes.infraestructura;

import java.time.Clock;
import java.util.List;
import mx.negocio360.compartido.comandos.PreparacionesEnMemoria;
import mx.negocio360.compartido.comandos.ServicioPreparacion;
import mx.negocio360.compartido.eventos.PublicadorEnBus;
import mx.negocio360.restaurantes.dominio.CodigoError;
import mx.negocio360.restaurantes.dominio.RestaurantesException;
import mx.negocio360.restaurantes.web.ComandosDeRestaurantes;
import mx.negocio360.restaurantes.aplicacion.AutorizacionDeRestaurantes;
import mx.negocio360.restaurantes.aplicacion.ConsumidorDeVentas;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.restaurantes.aplicacion.ConsumosServicio;
import mx.negocio360.restaurantes.aplicacion.PublicadorEventos;
import mx.negocio360.restaurantes.aplicacion.RepositorioConsumos;
import mx.negocio360.restaurantes.aplicacion.InventarioPuerto;
import mx.negocio360.restaurantes.aplicacion.ManejadorDarDeBajaPlatillo;
import mx.negocio360.restaurantes.aplicacion.ManejadorGuardarReceta;
import mx.negocio360.restaurantes.aplicacion.RecetasServicio;
import mx.negocio360.restaurantes.aplicacion.ConsultaDeRecetas;
import mx.negocio360.restaurantes.aplicacion.MenuPublicoServicio;
import mx.negocio360.restaurantes.aplicacion.MenuServicio;
import mx.negocio360.restaurantes.aplicacion.PlatillosParaVenta;
import mx.negocio360.restaurantes.aplicacion.RepositorioMenu;
import mx.negocio360.restaurantes.aplicacion.RepositorioRestaurantes;
import mx.negocio360.restaurantes.aplicacion.SeguimientoDeNegocios;
import mx.negocio360.restaurantes.aplicacion.UsoDeLosPlatillos;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enciende Restaurantes con repositorios en memoria solo con {@code restaurantes.almacen=memoria} (el perfil de desarrollo lo
 * fija). Sin base de datos el menu se pierde al reiniciar, asi que no se enciende por accidente; la version con PostgreSQL es
 * R7, y entonces un entorno de produccion sin almacen no arrancara (H-15).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "restaurantes.almacen", havingValue = "memoria")
public class RestaurantesConfiguracion {

    @Bean
    RepositorioMenu repositorioMenu() {
        return new MenuEnMemoria();
    }

    @Bean
    RepositorioRestaurantes repositorioRestaurantes() {
        return new RestaurantesEnMemoria();
    }

    /** El almacen de recetas es tambien quien responde al menu cuales platillos tienen receta activa (un solo bean de ambos tipos). */
    @Bean
    RecetasEnMemoria recetasEnMemoria() {
        return new RecetasEnMemoria();
    }

    /**
     * Con {@code InventarioApi} en el contexto (R-I1 a R-I4) se usa el adaptador real. Sin el, Inventario no esta conectado
     * y se falla con un error claro en lugar de inventar articulos.
     */
    @Bean
    @ConditionalOnMissingBean(InventarioPuerto.class)
    InventarioPuerto inventarioPuerto(ObjectProvider<InventarioApi> inventario) {
        var api = inventario.getIfAvailable();
        return api != null ? new InventarioApiComoPuerto(api) : new InventarioNoDisponible();
    }

    @Bean
    RecetasServicio recetasServicio(RecetasEnMemoria recetas, RepositorioMenu menu, InventarioPuerto inventario, Clock reloj) {
        return new RecetasServicio(recetas, menu, inventario, reloj);
    }

    @Bean
    ManejadorGuardarReceta manejadorGuardarReceta(RepositorioMenu menu, RecetasEnMemoria recetas, InventarioPuerto inventario,
                                                  AutorizacionDeRestaurantes autorizacion, Clock reloj) {
        return new ManejadorGuardarReceta(menu, recetas, inventario, autorizacion, reloj);
    }

    @Bean
    ManejadorDarDeBajaPlatillo manejadorDarDeBajaPlatillo(RepositorioMenu menu, UsoDeLosPlatillos uso,
                                                          ConsultaDeRecetas recetas, AutorizacionDeRestaurantes autorizacion,
                                                          Clock reloj) {
        return new ManejadorDarDeBajaPlatillo(menu, uso, recetas, autorizacion, reloj);
    }

    /**
     * Las variantes de comando de Restaurantes para el despachador de la plataforma (D-125, D-209). El servicio de preparacion
     * queda dentro y no se expone como bean (Clientes ya expone el suyo); las preparaciones viven en memoria mientras
     * Restaurantes no tenga Postgres (R7).
     */
    @Bean
    ComandosDeRestaurantes comandosDeRestaurantes(ManejadorGuardarReceta guardarReceta,
                                                   ManejadorDarDeBajaPlatillo darDeBaja, Clock reloj) {
        return new ComandosDeRestaurantes(new ServicioPreparacion(new PreparacionesEnMemoria(),
                List.of(guardarReceta, darDeBaja), reloj,
                fallo -> new RestaurantesException(CodigoError.valueOf(fallo.name()))));
    }

    @Bean
    UsoDeLosPlatillos usoDeLosPlatillos() {
        return new SinCuentas();
    }

    @Bean
    AutorizacionDeRestaurantes autorizacionDeRestaurantes() {
        return new SesionDeRestaurantes();
    }

    @Bean
    MenuServicio menuServicio(RepositorioMenu menu, ConsultaDeRecetas recetas, Clock reloj) {
        return new MenuServicio(menu, recetas, reloj);
    }

    @Bean
    MenuPublicoServicio menuPublicoServicio(RepositorioMenu menu, RepositorioRestaurantes restaurantes, Clock reloj) {
        return new MenuPublicoServicio(menu, restaurantes, reloj);
    }

    @Bean
    PlatillosParaVenta platillosParaVenta(RepositorioMenu menu, Clock reloj) {
        return new PlatillosParaVenta(menu, reloj);
    }

    @Bean
    RepositorioConsumos repositorioConsumos() {
        return new ConsumosEnMemoria();
    }

    /** Los eventos salen por el outbox de Modulith: {@code PublicadorEnBus} exige una transaccion activa (D-103). */
    @Bean
    PublicadorEventos publicadorEventos(ApplicationEventPublisher bus) {
        var publicador = new PublicadorEnBus(bus);
        return publicador::publicar;
    }

    @Bean
    ConsumosServicio consumosServicio(RepositorioConsumos consumos) {
        return new ConsumosServicio(consumos);
    }

    @Bean
    ConsumidorDeVentas consumidorDeVentas(RepositorioMenu menu, RecetasEnMemoria recetas, InventarioPuerto inventario,
                                          RepositorioConsumos consumos, PublicadorEventos eventos, Clock reloj) {
        return new ConsumidorDeVentas(menu, recetas, inventario, consumos, eventos, reloj);
    }

    @Bean
    EscuchaDeVentas escuchaDeVentasRestaurantes(ConsumidorDeVentas consumidor) {
        return new EscuchaDeVentas(consumidor);
    }

    @Bean
    SeguimientoDeNegocios seguimientoDeNegocios(RepositorioRestaurantes restaurantes) {
        return new SeguimientoDeNegocios(restaurantes);
    }

    @Bean
    EscuchaDeNegocios escuchaDeNegociosRestaurantes(SeguimientoDeNegocios seguimiento) {
        return new EscuchaDeNegocios(seguimiento);
    }
}
