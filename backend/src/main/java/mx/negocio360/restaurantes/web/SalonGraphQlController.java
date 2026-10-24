package mx.negocio360.restaurantes.web;

import java.util.UUID;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.restaurantes.aplicacion.DisponibilidadVista;
import mx.negocio360.restaurantes.aplicacion.MesaVista;
import mx.negocio360.restaurantes.aplicacion.SalonServicio;
import mx.negocio360.restaurantes.aplicacion.SalonVista;
import mx.negocio360.restaurantes.aplicacion.ZonaVista;
import mx.negocio360.restaurantes.dominio.ModoServicio;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Los resolvedores del salon (R4): zonas, mesas, servicio, disponibilidad y QR de mesa. Solo traducen GraphQL al servicio:
 * el negocio, el actor y los permisos salen de la sesion que dejo el filtro de entrada (SG-02) y cada servicio exige su
 * permiso (SG-04). Sin sesion o sin negocio activo fallan cerrado.
 */
@Controller
@ConditionalOnProperty(name = "restaurantes.almacen", havingValue = "memoria")
public class SalonGraphQlController {

    private final SalonServicio salon;
    private final EnTransaccion tx;

    /**
     * Arma el controlador.
     *
     * @param salon         el salon del restaurante
     * @param transacciones donde cada mutacion es una sola transaccion (SR-04)
     */
    public SalonGraphQlController(SalonServicio salon, TransactionOperations transacciones) {
        this.salon = salon;
        this.tx = new EnTransaccion(transacciones);
    }

    @QueryMapping
    public SalonVista salon(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return salon.salon(entrada.exigirContexto(), entrada.autorizacion());
    }

    @QueryMapping
    public String qrDeMesa(@Argument UUID mesaId, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return salon.qrDeMesa(entrada.exigirContexto(), entrada.autorizacion(), mesaId);
    }

    @MutationMapping
    public ZonaVista crearZona(@Argument String nombre, @Argument int orden,
                               @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> salon.crearZona(entrada.exigirContexto(), entrada.autorizacion(), nombre, orden));
    }

    @MutationMapping
    public boolean darDeBajaZona(@Argument UUID id, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        tx.ejecutar(() -> salon.darDeBajaZona(entrada.exigirContexto(), entrada.autorizacion(), id));
        return true;
    }

    @MutationMapping
    public MesaVista crearMesa(@Argument MesaInput input, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> salon.crearMesa(entrada.exigirContexto(), entrada.autorizacion(), input.zonaId(), input.etiqueta(),
                input.capacidad()));
    }

    @MutationMapping
    public MesaVista editarMesa(@Argument UUID id, @Argument long version, @Argument MesaInput input,
                                @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> salon.editarMesa(entrada.exigirContexto(), entrada.autorizacion(), id, version, input.zonaId(),
                input.etiqueta(), input.capacidad()));
    }

    @MutationMapping
    public boolean darDeBajaMesa(@Argument UUID id, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        tx.ejecutar(() -> salon.darDeBajaMesa(entrada.exigirContexto(), entrada.autorizacion(), id));
        return true;
    }

    @MutationMapping
    public MesaVista bloquearMesa(@Argument UUID id, @Argument boolean bloquear,
                                  @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> salon.bloquearMesa(entrada.exigirContexto(), entrada.autorizacion(), id, bloquear));
    }

    @MutationMapping
    public DisponibilidadVista abrirServicio(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> salon.abrirServicio(entrada.exigirContexto(), entrada.autorizacion()));
    }

    @MutationMapping
    public DisponibilidadVista cerrarServicio(@Argument String motivo,
                                              @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> salon.cerrarServicio(entrada.exigirContexto(), entrada.autorizacion(), motivo));
    }

    @MutationMapping
    public ModoServicio configurarModoDeServicio(@Argument ModoServicio modo,
                                                 @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        tx.ejecutar(() -> salon.configurarModo(entrada.exigirContexto(), entrada.autorizacion(), modo));
        return modo;
    }

    @MutationMapping
    public String regenerarQrDeMesa(@Argument UUID mesaId, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> salon.regenerarQrDeMesa(entrada.exigirContexto(), entrada.autorizacion(), mesaId));
    }

    @MutationMapping
    public MesaVista pausarQrDeMesa(@Argument UUID mesaId, @Argument boolean pausar,
                                    @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return tx.ejecutar(() -> salon.pausarQrDeMesa(entrada.exigirContexto(), entrada.autorizacion(), mesaId, pausar));
    }

    @MutationMapping
    public boolean habilitarQr(@Argument boolean habilitado, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        tx.ejecutar(() -> salon.habilitarQr(entrada.exigirContexto(), entrada.autorizacion(), habilitado));
        return habilitado;
    }
}
