package mx.negocio360.negocios.aplicacion;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.negocios.CalificacionVista;
import mx.negocio360.negocios.FiltroResenas;
import mx.negocio360.negocios.PaginaResenas;
import mx.negocio360.negocios.ResenaVista;
import mx.negocio360.negocios.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioResenas;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.NegociosException;
import mx.negocio360.negocios.dominio.Resena;

/**
 * Las reseñas del lado del negocio y de la plataforma: verlas y responderlas (RS-04, RS-06) y moderarlas (RS-05, R6).
 * Java puro: la transaccion la pone quien lo envuelve, asi que cada cambio guarda la reseña, el resumen, el historial y el
 * evento juntos. Una reseña oculta no existe para el negocio; una de otro negocio, tampoco (SG-03).
 */
public final class ResenasServicio {

    private static final int MAXIMO_POR_PAGINA = 50;
    private static final String PATRON_DE_MOTIVO = "[A-Z0-9_]{1,40}";

    private final RepositorioResenas resenas;
    private final RepositorioNegocios negocios;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public ResenasServicio(RepositorioResenas resenas, RepositorioNegocios negocios, PublicadorEventos eventos,
                           Clock reloj) {
        this.resenas = Objects.requireNonNull(resenas, "resenas");
        this.negocios = Objects.requireNonNull(negocios, "negocios");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ---------------------------------------------------------------------------------------------- el negocio

    public PaginaResenas pagina(ContextoNegocio contexto, Autorizacion autorizacion, FiltroResenas filtro, int primeros,
                                String despues) {
        autorizacion.exigir(PermisosDeNegocios.RESENA_VER);
        var porPagina = Math.max(1, Math.min(primeros, MAXIMO_POR_PAGINA));
        var desplazamiento = CursorDeDesplazamiento.leer(despues);
        var filas = resenas.pagina(contexto.negocioId(), filtro == FiltroResenas.SIN_RESPUESTA, porPagina + 1,
                desplazamiento);
        var hayMas = filas.size() > porPagina;
        var pagina = (hayMas ? filas.subList(0, porPagina) : filas).stream().map(ResenasServicio::vista).toList();
        var resumen = resenas.resumen(contexto.negocioId());
        return new PaginaResenas(pagina, hayMas, hayMas ? CursorDeDesplazamiento.codificar(desplazamiento + porPagina)
                : null, new CalificacionVista(resumen.promedio(), resumen.total(), resumen.distribucion()));
    }

    public ResenaVista responder(ContextoNegocio contexto, Autorizacion autorizacion, UUID resenaId, String texto) {
        autorizacion.exigir(PermisosDeNegocios.RESENA_RESPONDER);
        var negocio = negocios.negocio(contexto.negocioId())
                .orElseThrow(() -> new NegociosException(CodigoError.NEGOCIO_NO_DISPONIBLE));
        negocio.exigirEditable();
        var respuesta = Resena.respuesta(texto);
        var resena = resenas.resena(negocio.id(), Objects.requireNonNull(resenaId, "resenaId"))
                .filter(r -> r.estado() == Resena.Estado.PUBLICADA)
                .orElseThrow(() -> new NegociosException(CodigoError.RESENA_NO_ENCONTRADA));
        if (resena.respuesta() != null) {
            throw new NegociosException(CodigoError.RESENA_YA_RESPONDIDA);
        }
        var ahora = reloj.instant();
        if (!resenas.responder(negocio.id(), resena.id(), respuesta, cuentaDe(contexto), ahora)) {
            throw new NegociosException(CodigoError.RESENA_YA_RESPONDIDA);
        }
        negocios.registrarHistorial(negocio.id(), "RESPONDER_RESENA", List.of("resena"), contexto.actor(), ahora);
        eventos.publicar(FabricaEventos.resenaRespondida(resena, ahora));
        return vista(resenas.resena(negocio.id(), resena.id()).orElseThrow());
    }

    // ------------------------------------------------------------------------------------------ la plataforma

    public ResenaVista ocultarResena(UUID negocioId, UUID resenaId, String motivo, String operador) {
        if (motivo == null || !motivo.matches(PATRON_DE_MOTIVO)) {
            throw new NegociosException(CodigoError.CONSULTA_INVALIDA);
        }
        return moderar(negocioId, resenaId, operador, Resena.Estado.OCULTA, "OCULTAR_RESENA", "OCULTADA");
    }

    public ResenaVista restituirResena(UUID negocioId, UUID resenaId, String operador) {
        return moderar(negocioId, resenaId, operador, Resena.Estado.PUBLICADA, "RESTITUIR_RESENA", "RESTITUIDA");
    }

    private ResenaVista moderar(UUID negocioId, UUID resenaId, String operador, Resena.Estado estado, String accion,
                                String accionDelEvento) {
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(resenaId, "resenaId");
        if (operador == null || operador.isBlank() || operador.length() > 80) {
            throw new NegociosException(CodigoError.CONSULTA_INVALIDA);
        }
        var resena = resenas.resena(negocioId, resenaId)
                .orElseThrow(() -> new NegociosException(CodigoError.RESENA_NO_ENCONTRADA));
        var ahora = reloj.instant();
        if (resena.estado() != estado) {
            resenas.cambiarEstado(negocioId, resenaId, estado, ahora);
            resenas.recalcularResumen(negocioId, ahora);
            negocios.registrarHistorial(negocioId, accion, List.of("resena"), operador, ahora);
            eventos.publicar(FabricaEventos.resenaPublicada(resena, accionDelEvento, ahora));
        }
        return vista(resenas.resena(negocioId, resenaId).orElseThrow());
    }

    // ------------------------------------------------------------------------------------------------- apoyo

    static ResenaVista vista(Resena r) {
        return new ResenaVista(r.id(), r.calificacion(), r.comentario(), r.creadaEn(), r.estado().name(),
                r.respuesta(), r.respondidaEn(), r.origen() == Resena.Origen.DEMOSTRACION, r.version());
    }

    /** La cuenta del actor, si el actor es un id de cuenta; el contexto lleva un texto para la auditoria. */
    private static UUID cuentaDe(ContextoNegocio contexto) {
        var actor = contexto.actor();
        var inicio = actor.length() >= 36 ? actor.substring(actor.length() - 36) : actor;
        try {
            return UUID.fromString(inicio);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
