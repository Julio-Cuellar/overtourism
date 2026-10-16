package mx.negocio360.caja.infraestructura;

import java.math.BigDecimal;
import java.time.Clock;
import mx.negocio360.caja.CajaApi;
import mx.negocio360.caja.aplicacion.CajaServicio;
import mx.negocio360.caja.aplicacion.ConsumidorVentas;
import mx.negocio360.caja.aplicacion.ResumenVentasDesdeVentasApi;
import mx.negocio360.caja.aplicacion.VerificadorDeTurnoDesdeCaja;
import mx.negocio360.caja.aplicacion.puertos.ReglasCaja;
import mx.negocio360.compartido.comandos.PreparacionesEnMemoria;
import mx.negocio360.ventas.VentasApi;
import mx.negocio360.ventas.VerificadorDeTurno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enciende Caja al arrancar con sus tablas en memoria, solo con {@code caja.almacen=memoria} (el perfil
 * {@code desarrollo} lo fija): su API, el verificador de turno que pide Ventas (D-145) y la escucha de
 * {@code venta.registrada} y {@code venta.anulada}. Los permisos ya no se inyectan: cada llamada recibe la
 * {@code Autorizacion} de la sesion (D-126). La version persistente es F6.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "caja.almacen", havingValue = "memoria")
public class CajaConfiguracion {

    /** Umbral de diferencia del corte por omision: el valor inicial propuesto en CJ-08 (luego vendra de reglas). */
    static final String UMBRAL_POR_OMISION = "50.00";

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
     * Respuesta de Caja a «¿el turno sigue abierto?» (VT-01, D-145).
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
     * @param bus          bus de eventos de Spring
     * @param reloj        reloj de la plataforma
     * @param umbral       diferencia de efectivo que pide autorizacion (CJ-08), de {@code caja.umbral-diferencia}
     * @return Caja listo para usarse
     */
    @Bean
    public CajaApi cajaApi(CajaEnMemoria almacen, VentasApi ventas, ApplicationEventPublisher bus, Clock reloj,
                           @Value("${caja.umbral-diferencia:" + UMBRAL_POR_OMISION + "}") BigDecimal umbral) {
        ReglasCaja reglas = negocioId -> umbral;
        return CajaServicio.armar(almacen, almacen, new PreparacionesEnMemoria(), new PublicadorEventosSpring(bus),
                new ResumenVentasDesdeVentasApi(ventas), reglas, reloj);
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
