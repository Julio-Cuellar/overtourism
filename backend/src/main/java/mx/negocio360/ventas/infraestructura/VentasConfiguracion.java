package mx.negocio360.ventas.infraestructura;

import java.time.Clock;
import mx.negocio360.compartido.catalogo.FuenteDeProductos;
import mx.negocio360.compartido.comandos.PreparacionesEnMemoria;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.VerificadorDeTurno;
import mx.negocio360.ventas.aplicacion.VentasServicio;
import mx.negocio360.ventas.aplicacion.puertos.CatalogoDeVentas;
import mx.negocio360.ventas.aplicacion.puertos.CuponesDePasaporte;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enciende Ventas al arrancar con repositorios en memoria, solo con {@code ventas.almacen=memoria} (el perfil
 * {@code desarrollo} lo fija). Los productos salen de las {@link FuenteDeProductos} que ofrezcan otros modulos
 * (D-128); los cupones, de Pasaporte cuando exista y, mientras no, ninguno es valido (D-134). El verificador de turno
 * lo da Caja (D-145). Sin base de datos, una venta se pierde al reiniciar: la version persistente es F6.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "ventas.almacen", havingValue = "memoria")
public class VentasConfiguracion {

    /**
     * Productos que se pueden vender: las fuentes que haya, en orden.
     *
     * @param fuentes fuentes de productos de los modulos que las ofrecen; puede no haber ninguna
     * @return el catalogo compuesto
     */
    @Bean
    public CatalogoDeVentas catalogoDeVentas(ObjectProvider<FuenteDeProductos> fuentes) {
        return new CatalogoCompuesto(fuentes.orderedStream().toList());
    }

    /**
     * Cupones: ninguno es valido hasta que exista Pasaporte (D-134).
     *
     * @return el adaptador que rechaza todo codigo
     */
    @Bean
    @ConditionalOnMissingBean(CuponesDePasaporte.class)
    public CuponesDePasaporte cuponesDePasaporte() {
        return new CuponesNoDisponibles();
    }

    /**
     * API publica de Ventas.
     *
     * @param catalogo productos y precios (VT-02)
     * @param cupones  cupones de Pasaporte (VT-07)
     * @param turnos   Caja dice si el turno esta abierto (VT-01)
     * @param bus      bus de eventos de Spring
     * @param reloj    reloj de la plataforma
     * @return Ventas listo para usarse
     */
    @Bean
    public VentasApi ventasApi(CatalogoDeVentas catalogo, CuponesDePasaporte cupones, VerificadorDeTurno turnos,
                               ApplicationEventPublisher bus, Clock reloj) {
        return VentasServicio.armar(catalogo, cupones, turnos, new VentasEnMemoria(), new PreparacionesEnMemoria(),
                new PublicadorEventosSpring(bus), reloj);
    }
}
