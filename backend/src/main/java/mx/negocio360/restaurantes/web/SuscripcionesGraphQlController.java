package mx.negocio360.restaurantes.web;

import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.EventosPorNegocio;
import mx.negocio360.restaurantes.aplicacion.AvisoDeComanda;
import mx.negocio360.restaurantes.aplicacion.AvisoDelSalon;
import mx.negocio360.restaurantes.dominio.Estacion;
import mx.negocio360.restaurantes.infraestructura.AvisosEnVivo;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

/**
 * Los avisos en vivo del personal (8.4 del diseno): el tablero de cocina y barra, y el salon. Solo traducen los eventos de
 * Restaurantes a avisos: el negocio sale de la sesion al abrir la suscripcion y nunca de un argumento (SG-02, SG-16), el
 * permiso se exige al abrirla (SG-04) y no cambian ningun estado. Son un atajo y no la verdad: la pantalla se corrige con las
 * consultas {@code comandas} y {@code salon}.
 */
@Controller
@ConditionalOnProperty(name = "restaurantes.almacen", havingValue = "memoria")
public class SuscripcionesGraphQlController {

    private final AvisosEnVivo avisos;

    /**
     * Arma el controlador.
     *
     * @param avisos el puente con el bus de eventos
     */
    public SuscripcionesGraphQlController(AvisosEnVivo avisos) {
        this.avisos = avisos;
    }

    /**
     * Cada comanda enviada o actualizada del negocio, de las dos estaciones o de la que se pida (CD-09). Los avisos de
     * «platillo listo» para el mesero son las actualizaciones a {@code LISTA}. Exige {@code restaurantes.comanda.ver}.
     */
    @SubscriptionMapping
    public Flux<AvisoDeComanda> comandasDeCocina(@Argument Estacion estacion,
                                                 @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var delNegocio = EventosPorNegocio.deSobres(avisos.eventos(), entrada);
        entrada.autorizacion().exigir(PermisosDeRestaurantes.COMANDA_VER);
        return delNegocio.flatMap(e -> Flux.fromStream(AvisoDeComanda.de(e).stream()))
                .filter(a -> estacion == null || a.estacion() == estacion);
    }

    /**
     * Los cambios del salon: pedidos por QR, cuentas pedidas y mesas libres; la pausa de un QR solo para quien tiene
     * {@code restaurantes.servicio.operar} (QR-17). Exige {@code restaurantes.salon.ver}.
     */
    @SubscriptionMapping
    public Flux<AvisoDelSalon> salonActualizado(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var delNegocio = EventosPorNegocio.deSobres(avisos.eventos(), entrada);
        entrada.autorizacion().exigir(PermisosDeRestaurantes.SALON_VER);
        var opera = entrada.autorizacion().puede(PermisosDeRestaurantes.SERVICIO_OPERAR);
        return delNegocio.flatMap(e -> Flux.fromStream(AvisoDelSalon.de(e).stream()))
                .filter(a -> opera || a.tipo() != AvisoDelSalon.Tipo.QR_PAUSADO);
    }
}
