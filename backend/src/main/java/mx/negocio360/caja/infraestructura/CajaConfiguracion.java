package mx.negocio360.caja.infraestructura;

import java.math.BigDecimal;
import java.time.Clock;
import mx.negocio360.caja.CajaApi;
import mx.negocio360.caja.aplicacion.CajaServicio;
import mx.negocio360.caja.aplicacion.ConsumidorVentas;
import mx.negocio360.caja.aplicacion.ResumenVentasDesdeVentasApi;
import mx.negocio360.caja.aplicacion.VerificadorDeTurnoDesdeCaja;
import mx.negocio360.caja.aplicacion.puertos.AutorizacionCaja;
import mx.negocio360.caja.aplicacion.puertos.ReglasCaja;
import mx.negocio360.compartido.comandos.PreparacionesEnMemoria;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.VerificadorDeTurno;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Enciende Caja al arrancar (F4) con sus tablas en memoria: su API, el verificador de turno que pide Ventas (D-86) y
 * la escucha de {@code venta.registrada} y {@code venta.anulada}. Los permisos llegan como bean de otro modulo (en la
 * demo, simulados por {@code canal.demo}).
 *
 * <p>Solo existe con {@code negocio360.demo.habilitada=true} y nunca con el perfil {@code produccion}. La version
 * persistente es F5.
 */
@Configuration(proxyBeanMethods = false)
@Profile("!produccion")
@ConditionalOnProperty(name = "negocio360.demo.habilitada", havingValue = "true")
public class CajaConfiguracion {

    /** Umbral de diferencia del corte en la demo: el valor inicial propuesto en CJ-08 (se leera de reglas en F5). */
    static final BigDecimal UMBRAL_DE_DEMO = new BigDecimal("50.00");

    /**
     * Tablas de Caja en memoria.
     *
     * @return almacen unico de cajas, turnos, movimientos y cortes
     */
    @Bean
    public CajaEnMemoria cajaEnMemoria() {
        return new CajaEnMemoria();
    }

    /**
     * Respuesta de Caja a «¿el turno sigue abierto?» (VT-01, D-86).
     *
     * @param almacen tablas de Caja
     * @param reloj   reloj de la plataforma
     * @return el verificador que usa Ventas
     */
    @Bean
    public VerificadorDeTurno verificadorDeTurno(CajaEnMemoria almacen, Clock reloj) {
        return new VerificadorDeTurnoDesdeCaja(almacen, reloj);
    }

    /**
     * API publica de Caja.
     *
     * @param almacen      tablas de Caja
     * @param ventas       API de Ventas, para el corte (J1, CJ-07)
     * @param autorizacion quien es encargado (CJ-08)
     * @param bus          bus de eventos de Spring
     * @param reloj        reloj de la plataforma
     * @return Caja listo para usarse
     */
    @Bean
    public CajaApi cajaApi(CajaEnMemoria almacen, VentasApi ventas, AutorizacionCaja autorizacion,
                           ApplicationEventPublisher bus, Clock reloj) {
        ReglasCaja reglas = negocioId -> UMBRAL_DE_DEMO;
        return CajaServicio.armar(almacen, almacen, new PreparacionesEnMemoria(), new PublicadorEventosSpring(bus),
                new ResumenVentasDesdeVentasApi(ventas), reglas, autorizacion, reloj);
    }

    /**
     * Escucha de los eventos de Ventas (CF-20, CF-21).
     *
     * @param almacen tablas de Caja
     * @return la escucha registrada en el bus de Spring
     */
    @Bean
    public EscuchaDeVentas escuchaDeVentas(CajaEnMemoria almacen) {
        return new EscuchaDeVentas(new ConsumidorVentas(almacen, almacen));
    }
}
