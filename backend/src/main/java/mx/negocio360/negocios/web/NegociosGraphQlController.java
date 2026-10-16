package mx.negocio360.negocios.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.negocios.ActualizarNegocio;
import mx.negocio360.negocios.ActualizarSucursal;
import mx.negocio360.negocios.AltaVista;
import mx.negocio360.negocios.DefinirAtributos;
import mx.negocio360.negocios.DiaHorarioComando;
import mx.negocio360.negocios.DisponibilidadVista;
import mx.negocio360.negocios.ExcepcionComando;
import mx.negocio360.negocios.FiltroResenas;
import mx.negocio360.negocios.HorarioVista;
import mx.negocio360.negocios.NegocioVista;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.PaginaResenas;
import mx.negocio360.negocios.ResenaVista;
import mx.negocio360.negocios.SucursalVista;
import mx.negocio360.negocios.infraestructura.AvisosDeAlta;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

/**
 * Los resolvedores del panel del negocio (seccion 9.3). Solo traducen GraphQL a {@link NegociosApi}: el negocio, el actor y
 * los permisos salen de la sesion que dejo el filtro de entrada ({@link EntradaGraphQl}), nunca de un argumento (SG-02);
 * sin sesion o sin negocio activo fallan cerrado, y cada funcion exige su permiso (SG-04). Los errores de negocio salen con
 * su codigo en {@link ResolvedorDeErroresDeNegocios}.
 */
@Controller
public class NegociosGraphQlController {

    private final NegociosApi panel;
    private final AvisosDeAlta avisos;

    public NegociosGraphQlController(NegociosApi panel, AvisosDeAlta avisos) {
        this.panel = panel;
        this.avisos = avisos;
    }

    // ---- consultas ----

    @QueryMapping
    public NegocioVista miNegocio(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.miNegocio(entrada.exigirContexto(), entrada.autorizacion());
    }

    @QueryMapping
    public List<SucursalVista> sucursales(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.sucursales(entrada.exigirContexto(), entrada.autorizacion());
    }

    @QueryMapping
    public HorarioVista horarioDeSucursal(@Argument UUID sucursalId,
                                          @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.horarioDeSucursal(entrada.exigirContexto(), entrada.autorizacion(), sucursalId);
    }

    /** Se permite sin negocio activo: justo despues de registrarse todavia no hay uno (excepcion acotada a SG-02). */
    @QueryMapping
    public List<AltaVista> misAltasDeNegocio(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.misAltas(entrada.exigirCuenta());
    }

    @QueryMapping
    public PaginaResenas resenas(@Argument FiltroResenas filtro, @Argument int primeros, @Argument String despues,
                                 @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.resenas(entrada.exigirContexto(), entrada.autorizacion(),
                filtro == null ? FiltroResenas.TODAS : filtro, primeros, despues);
    }

    // ---- mutaciones ----

    @MutationMapping
    public NegocioVista actualizarNegocio(@Argument ActualizarNegocio input,
                                          @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.actualizarNegocio(entrada.exigirContexto(), entrada.autorizacion(), input);
    }

    @MutationMapping
    public NegocioVista definirSubcategorias(@Argument long version, @Argument List<String> subcategorias,
                                             @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.definirSubcategorias(entrada.exigirContexto(), entrada.autorizacion(), version, subcategorias);
    }

    @MutationMapping
    public NegocioVista definirAtributos(@Argument DefinirAtributos input,
                                         @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.definirAtributos(entrada.exigirContexto(), entrada.autorizacion(), input);
    }

    @MutationMapping
    public DisponibilidadVista definirDisponibilidad(@Argument String nivel, @Argument String nota,
                                                     @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.definirDisponibilidad(entrada.exigirContexto(), entrada.autorizacion(), nivel, nota);
    }

    @MutationMapping
    public NegocioVista publicarNegocio(@Argument boolean publicar,
                                        @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.publicarNegocio(entrada.exigirContexto(), entrada.autorizacion(), publicar);
    }

    @MutationMapping
    public SucursalVista actualizarSucursal(@Argument UUID id, @Argument ActualizarSucursal input,
                                            @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.actualizarSucursal(entrada.exigirContexto(), entrada.autorizacion(), id, input);
    }

    @MutationMapping
    public HorarioVista definirHorario(@Argument UUID sucursalId, @Argument long version,
                                       @Argument List<DiaHorarioComando> semana,
                                       @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.definirHorario(entrada.exigirContexto(), entrada.autorizacion(), sucursalId, version, semana);
    }

    @MutationMapping
    public HorarioVista agregarExcepcionHorario(@Argument UUID sucursalId, @Argument ExcepcionComando input,
                                                @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.agregarExcepcionHorario(entrada.exigirContexto(), entrada.autorizacion(), sucursalId, input);
    }

    @MutationMapping
    public HorarioVista quitarExcepcionHorario(@Argument UUID id,
                                               @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.quitarExcepcionHorario(entrada.exigirContexto(), entrada.autorizacion(), id);
    }

    @MutationMapping
    public ResenaVista responderResena(@Argument UUID id, @Argument String texto,
                                       @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return panel.responderResena(entrada.exigirContexto(), entrada.autorizacion(), id, texto);
    }

    // ---- suscripcion (W1) ----

    /**
     * Avisa a quien pidio el alta cuando el negocio quedo listo o se rechazo, y se cierra sola al avisar. Es un atajo: la
     * verdad esta en {@code misAltasDeNegocio}, que la app consulta al volver de segundo plano. Una suscripcion por cuenta.
     * Se permite sin negocio activo.
     */
    @SubscriptionMapping
    public Flux<AvisosDeAlta.Aviso> altaDeNegocio(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var cuenta = entrada.exigirCuenta();
        return Flux.create(sumidero -> {
            var suscripcion = avisos.suscribir(cuenta, new AvisosDeAlta.Suscriptor() {
                @Override
                public void aviso(AvisosDeAlta.Aviso aviso) {
                    sumidero.next(aviso);
                }

                @Override
                public void completo() {
                    sumidero.complete();
                }
            });
            sumidero.onDispose(suscripcion::close);
        });
    }

    // ---- formas que el esquema pide distintas de las vistas ----

    @SchemaMapping(typeName = "Negocio", field = "giro")
    public Map<String, String> giro(NegocioVista negocio) {
        return Map.of("codigo", negocio.giro(), "nombre", negocio.giroNombre());
    }

    @SchemaMapping(typeName = "Resena", field = "respuesta")
    public Map<String, Object> respuesta(ResenaVista resena) {
        return resena.respuesta() == null ? null : Map.of("texto", resena.respuesta(), "respondidaEn",
                resena.respondidaEn());
    }
}
