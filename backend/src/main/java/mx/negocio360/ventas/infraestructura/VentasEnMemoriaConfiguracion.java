package mx.negocio360.ventas.infraestructura;

import java.time.Clock;
import mx.negocio360.compartido.comandos.PreparacionesEnMemoria;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.VerificadorDeTurno;
import mx.negocio360.ventas.aplicacion.VentasServicio;
import mx.negocio360.ventas.aplicacion.puertos.CatalogoDeVentas;
import mx.negocio360.ventas.aplicacion.puertos.CuponesDePasaporte;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ventas con repositorios en memoria, solo con {@code ventas.almacen=memoria} (el perfil {@code desarrollo} lo fija).
 * El verificador de turno lo da Caja (D-145). Sin base de datos, una venta se pierde al reiniciar, y los eventos van al
 * bus de Spring de forma sincrona y sin outbox: para eso esta {@link VentasPostgresConfiguracion}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "ventas.almacen", havingValue = "memoria")
public class VentasEnMemoriaConfiguracion {

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
