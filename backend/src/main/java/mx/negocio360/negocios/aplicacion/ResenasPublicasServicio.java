package mx.negocio360.negocios.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.negocios.PaginaResenasPublicas;
import mx.negocio360.negocios.PreparacionResena;
import mx.negocio360.negocios.ResenaPublicaVista;
import mx.negocio360.negocios.ResenasPublicasApi;
import mx.negocio360.negocios.ResultadoResena;
import mx.negocio360.negocios.aplicacion.puertos.ModoDemostracion;
import mx.negocio360.negocios.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioPreparacionesResena;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioPreparacionesResena.Preparada;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioResenas;
import mx.negocio360.negocios.dominio.CodigoError;
import mx.negocio360.negocios.dominio.EstadoNegocio;
import mx.negocio360.negocios.dominio.NegociosException;
import mx.negocio360.negocios.dominio.Resena;

/**
 * Las reseñas de cara al turista (T3, T4) en modo demostracion (D-104, DM-03): sin cuenta ni sello, con el id de dispositivo
 * como autor (RS-01, RS-11), con un limite de 5 por hora por dispositivo y publicando por preparar y confirmar (RS-09). El
 * token es de un solo uso y de 5 minutos (D-23), y solo lo confirma el dispositivo que lo pidio. Leer las reseñas publicas no
 * depende del modo. Java puro.
 */
public final class ResenasPublicasServicio implements ResenasPublicasApi {

    static final Duration VIGENCIA_DEL_TOKEN = Duration.ofMinutes(5);
    static final Duration VENTANA_DEL_LIMITE = Duration.ofHours(1);
    static final int PREPARACIONES_POR_HORA = 5;
    private static final int POR_PAGINA_MAXIMA = 20;

    private final ModoDemostracion modo;
    private final RepositorioNegocios negocios;
    private final RepositorioResenas resenas;
    private final RepositorioPreparacionesResena preparaciones;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public ResenasPublicasServicio(ModoDemostracion modo, RepositorioNegocios negocios, RepositorioResenas resenas,
                                   RepositorioPreparacionesResena preparaciones, PublicadorEventos eventos, Clock reloj) {
        this.modo = Objects.requireNonNull(modo, "modo");
        this.negocios = Objects.requireNonNull(negocios, "negocios");
        this.resenas = Objects.requireNonNull(resenas, "resenas");
        this.preparaciones = Objects.requireNonNull(preparaciones, "preparaciones");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public PreparacionResena prepararResena(String dispositivo, UUID negocioId, int calificacion, String comentario) {
        modo.exigirActivo();
        var autor = Resena.dispositivo(dispositivo);
        var estrellas = Resena.calificacion(calificacion);
        var texto = Resena.comentario(comentario);
        exigirNegocioPublico(Objects.requireNonNull(negocioId, "negocioId"));
        var ahora = reloj.instant();
        preparaciones.borrarVencidas(ahora.minus(Duration.ofHours(2)));
        if (preparaciones.preparadasDesde(autor, ahora.minus(VENTANA_DEL_LIMITE)) >= PREPARACIONES_POR_HORA) {
            throw new NegociosException(CodigoError.DEMASIADAS_SOLICITUDES);
        }
        if (resenas.existePorAutor(negocioId, autor)) {
            throw new NegociosException(CodigoError.RESENA_YA_EXISTE);
        }
        var token = TokenOpaco.nuevo();
        var expira = ahora.plus(VIGENCIA_DEL_TOKEN);
        preparaciones.guardar(new Preparada(TokenOpaco.hash(token), negocioId, autor, estrellas, texto, ahora, expira,
                null));
        var resumen = "Vas a publicar una reseña de " + estrellas + (estrellas == 1 ? " estrella" : " estrellas")
                + (texto == null ? "" : " con comentario") + ". No se puede deshacer desde la app.";
        return new PreparacionResena(token, resumen, expira,
                texto == null ? List.of() : List.of("No escribas datos personales en el comentario."));
    }

    @Override
    public ResultadoResena confirmarResena(String dispositivo, String token) {
        modo.exigirActivo();
        var autor = Resena.dispositivo(dispositivo);
        if (token == null || token.isBlank() || token.length() > 128) {
            throw new NegociosException(CodigoError.TOKEN_INVALIDO);
        }
        var hash = TokenOpaco.hash(token);
        var preparada = preparaciones.porHash(hash).filter(p -> p.visitanteId().equals(autor))
                .orElseThrow(() -> new NegociosException(CodigoError.TOKEN_INVALIDO));
        var ahora = reloj.instant();
        if (preparada.usadaEn() != null) {
            throw new NegociosException(CodigoError.TOKEN_USADO);
        }
        if (!ahora.isBefore(preparada.expiraEn())) {
            throw new NegociosException(CodigoError.TOKEN_EXPIRADO);
        }
        exigirNegocioPublico(preparada.negocioId());
        if (resenas.existePorAutor(preparada.negocioId(), autor)) {
            throw new NegociosException(CodigoError.RESENA_YA_EXISTE);
        }
        if (!preparaciones.marcarUsada(hash, ahora)) {
            throw new NegociosException(CodigoError.TOKEN_USADO);
        }
        var resena = new Resena(UuidV7.generar(ahora), preparada.negocioId(), autor, preparada.calificacion(),
                preparada.comentario(), Resena.Estado.PUBLICADA, Resena.Origen.DEMOSTRACION, null, null, null, ahora,
                null, 0);
        if (!resenas.insertar(resena)) {
            throw new NegociosException(CodigoError.RESENA_YA_EXISTE);
        }
        resenas.recalcularResumen(resena.negocioId(), ahora);
        eventos.publicar(FabricaEventos.resenaPublicada(resena, "PUBLICADA", ahora));
        return new ResultadoResena(resena.id());
    }

    @Override
    public PaginaResenasPublicas resenasDeNegocio(UUID negocioId, int primeros, String despues) {
        var porPagina = Math.max(1, Math.min(primeros, POR_PAGINA_MAXIMA));
        var desplazamiento = CursorDeDesplazamiento.leer(despues);
        var filas = resenas.publicas(Objects.requireNonNull(negocioId, "negocioId"), porPagina + 1, desplazamiento);
        var hayMas = filas.size() > porPagina;
        var pagina = (hayMas ? filas.subList(0, porPagina) : filas).stream()
                .map(r -> new ResenaPublicaVista(r.id(), r.calificacion(), r.comentario(), r.creadaEn(), r.respuesta(),
                        r.respondidaEn(), r.demostracion())).toList();
        return new PaginaResenasPublicas(pagina, hayMas, hayMas ? CursorDeDesplazamiento.codificar(desplazamiento
                + porPagina) : null);
    }

    /** RS-10 y PU-01: un negocio sin publicar, suspendido, cerrado o inexistente responde igual: no esta disponible. */
    private void exigirNegocioPublico(UUID negocioId) {
        var negocio = negocios.negocio(negocioId);
        if (negocio.isEmpty() || negocio.get().estado() != EstadoNegocio.ACTIVO || !negocio.get().publicado()) {
            throw new NegociosException(CodigoError.NEGOCIO_NO_DISPONIBLE);
        }
    }
}
