package mx.negocio360.restaurantes.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.restaurantes.aplicacion.ConsultaDeRecetas;
import mx.negocio360.restaurantes.aplicacion.ConsumidorDeVentas;
import mx.negocio360.restaurantes.aplicacion.ConsumosServicio;
import mx.negocio360.restaurantes.aplicacion.InventarioPuerto;
import mx.negocio360.restaurantes.aplicacion.PublicadorEventos;
import mx.negocio360.restaurantes.aplicacion.RepositorioConsumos;
import mx.negocio360.restaurantes.aplicacion.ManejadorDarDeBajaPlatillo;
import mx.negocio360.restaurantes.aplicacion.ManejadorGuardarReceta;
import mx.negocio360.restaurantes.aplicacion.MenuPublicoServicio;
import mx.negocio360.restaurantes.aplicacion.RecetasServicio;
import mx.negocio360.restaurantes.aplicacion.MenuServicio;
import mx.negocio360.compartido.catalogo.FuenteDeProductos;
import mx.negocio360.restaurantes.aplicacion.PlatillosParaVenta;
import mx.negocio360.restaurantes.aplicacion.RepositorioMenu;
import mx.negocio360.restaurantes.aplicacion.RepositorioRestaurantes;
import mx.negocio360.restaurantes.aplicacion.SeguimientoDeNegocios;
import mx.negocio360.restaurantes.aplicacion.UsoDeLosPlatillos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Restaurantes se enciende solo con {@code restaurantes.almacen=memoria}, igual que Ventas y Caja con su almacen: sin base de
 * datos los datos se pierden al reiniciar, asi que no se enciende por accidente. La version con PostgreSQL es R7.
 */
class RestaurantesConfiguracionTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(RestaurantesConfiguracion.class).withBean(Clock.class, Clock::systemUTC)
            .withBean(org.springframework.transaction.support.TransactionOperations.class,
                    org.springframework.transaction.support.TransactionOperations::withoutTransaction);

    @Test
    @DisplayName("con restaurantes.almacen=memoria se arman los servicios del menu con sus repositorios en memoria")
    void conAlmacenEnMemoria() {
        contexto.withPropertyValues("restaurantes.almacen=memoria").run(c -> {
            assertThat(c).hasSingleBean(RepositorioMenu.class).hasSingleBean(RepositorioRestaurantes.class)
                    .hasSingleBean(ConsultaDeRecetas.class).hasSingleBean(UsoDeLosPlatillos.class)
                    .hasSingleBean(MenuServicio.class).hasSingleBean(MenuPublicoServicio.class)
                    .hasSingleBean(PlatillosParaVenta.class).hasSingleBean(FuenteDeProductos.class)
                    .hasSingleBean(SeguimientoDeNegocios.class)
                    .hasSingleBean(EscuchaDeNegocios.class).hasSingleBean(RecetasServicio.class)
                    .hasSingleBean(ManejadorGuardarReceta.class).hasSingleBean(ManejadorDarDeBajaPlatillo.class)
                    .hasSingleBean(InventarioPuerto.class).hasSingleBean(RepositorioConsumos.class)
                    .hasSingleBean(ConsumosServicio.class).hasSingleBean(ConsumidorDeVentas.class)
                    .hasSingleBean(EscuchaDeVentas.class).hasSingleBean(PublicadorEventos.class)
                    .hasSingleBean(mx.negocio360.restaurantes.aplicacion.SalonServicio.class)
                    .hasSingleBean(mx.negocio360.restaurantes.aplicacion.CuentasServicio.class)
                    .hasSingleBean(mx.negocio360.restaurantes.aplicacion.CocinaServicio.class)
                    .hasSingleBean(mx.negocio360.restaurantes.aplicacion.PedidosQrServicio.class)
                    .hasSingleBean(AvisosEnVivo.class)
                    .hasSingleBean(mx.negocio360.restaurantes.aplicacion.VinculoDeCuentasConVentas.class)
                    .hasSingleBean(EscuchaDeVentasEnCuentas.class)
                    .hasSingleBean(mx.negocio360.restaurantes.aplicacion.HashDeDispositivo.class)
                    .hasSingleBean(mx.negocio360.restaurantes.aplicacion.LimitesDeQr.class)
                    .hasSingleBean(mx.negocio360.restaurantes.aplicacion.OcupacionDeMesas.class)
                    .hasSingleBean(mx.negocio360.restaurantes.aplicacion.ManejadorAnularLineaPreparada.class)
                    .hasSingleBean(mx.negocio360.restaurantes.aplicacion.ManejadorCancelarCuentaPreparada.class);
            assertThat(c.getBean(InventarioPuerto.class)).as("Inventario aun no esta conectado").isInstanceOf(InventarioNoDisponible.class);
            assertThat(c.getBean(ConsultaDeRecetas.class)).isSameAs(c.getBean(RecetasEnMemoria.class));
            assertThat(c.getBean(RepositorioMenu.class)).isInstanceOf(MenuEnMemoria.class);
        });
    }

    @Test
    @DisplayName("sin la propiedad no se enciende nada: no hay servicios ni escucha")
    void sinAlmacen() {
        contexto.run(c -> assertThat(c).doesNotHaveBean(MenuServicio.class).doesNotHaveBean(EscuchaDeNegocios.class)
                .doesNotHaveBean(RepositorioMenu.class));
    }

    @Test
    @DisplayName("un almacen que no es memoria tampoco enciende nada (la base llega con R7)")
    void otroAlmacen() {
        contexto.withPropertyValues("restaurantes.almacen=postgres").run(c -> assertThat(c).doesNotHaveBean(MenuServicio.class));
    }

    @Test
    @DisplayName("con InventarioApi en el contexto, Restaurantes usa el adaptador real en lugar de InventarioNoDisponible")
    void conInventarioApi_usaElAdaptador() {
        contexto.withPropertyValues("restaurantes.almacen=memoria")
                .withBean(InventarioApi.class, () -> org.mockito.Mockito.mock(InventarioApi.class)).run(c -> {
                    assertThat(c.getBean(InventarioPuerto.class)).isInstanceOf(InventarioApiComoPuerto.class);
                    assertThat(c).hasSingleBean(ConsumidorDeVentas.class);
                });
    }
}
