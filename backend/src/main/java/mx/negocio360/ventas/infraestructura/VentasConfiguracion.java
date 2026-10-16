package mx.negocio360.ventas.infraestructura;

import java.time.Clock;
import mx.negocio360.compartido.comandos.PreparacionesEnMemoria;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.VerificadorDeTurno;
import mx.negocio360.ventas.aplicacion.VentasServicio;
import mx.negocio360.ventas.aplicacion.puertos.AutorizacionVentas;
import mx.negocio360.ventas.aplicacion.puertos.CatalogoDeVentas;
import mx.negocio360.ventas.aplicacion.puertos.CuponesDePasaporte;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Enciende Ventas al arrancar (F4) con repositorios en memoria. Catalogo, Pasaporte y los permisos llegan como beans de
 * otros modulos (en la demo, simulados por {@code canal.demo}) y el verificador de turno lo da Caja (D-86).
 *
 * <p>Solo existe con {@code negocio360.demo.habilitada=true} y nunca con el perfil {@code produccion}: sin base de
 * datos, una venta se pierde al reiniciar. La version persistente es F5.
 */
@Configuration(proxyBeanMethods = false)
@Profile("!produccion")
@ConditionalOnProperty(name = "negocio360.demo.habilitada", havingValue = "true")
public class VentasConfiguracion {

    /**
     * API publica de Ventas.
     *
     * @param catalogo     precios de Catalogo (VT-02)
     * @param cupones      cupones de Pasaporte (VT-07)
     * @param turnos       Caja dice si el turno esta abierto (VT-01)
     * @param autorizacion permiso de anular (VT-09)
     * @param bus          bus de eventos de Spring
     * @param reloj        reloj de la plataforma
     * @return Ventas listo para usarse
     */
    @Bean
    public VentasApi ventasApi(CatalogoDeVentas catalogo, CuponesDePasaporte cupones, VerificadorDeTurno turnos,
                               AutorizacionVentas autorizacion, ApplicationEventPublisher bus, Clock reloj) {
        return VentasServicio.armar(catalogo, cupones, turnos, new VentasEnMemoria(), new PreparacionesEnMemoria(),
                new PublicadorEventosSpring(bus), autorizacion, reloj);
    }
}
