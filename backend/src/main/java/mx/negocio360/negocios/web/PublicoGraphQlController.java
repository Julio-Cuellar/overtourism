package mx.negocio360.negocios.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.negocios.FiltroPublico;
import mx.negocio360.negocios.NegocioPublicoVista;
import mx.negocio360.negocios.NegociosPublicosApi;
import mx.negocio360.negocios.PaginaPublica;
import mx.negocio360.negocios.PaginaResenasPublicas;
import mx.negocio360.negocios.ResenaPublicaVista;
import mx.negocio360.negocios.ResenasPublicasApi;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * La superficie publica para la app del turista (seccion 9.7, D-98, D-99): sin sesion, sobre las vistas publicas, con los
 * topes de PU-05 en el servicio. Un negocio que no es publico responde nulo, igual que uno inexistente. Las reseñas se
 * publican con el par de la plataforma {@code prepararComando} y {@code confirmarComando} (RS-09), con el id de dispositivo
 * del encabezado {@code X-Dispositivo-Id} como autor (RS-11) y solo con el modo demostracion encendido (DM-03).
 *
 * <p>{@code prepararComando} y {@code confirmarComando} ya no se resuelven aqui: son del despachador de la plataforma
 * (D-125) y la variante {@code publicarResena} la atiende {@link ComandosDeNegocios}.
 */
@Controller
public class PublicoGraphQlController {

    private final NegociosPublicosApi publico;
    private final ResenasPublicasApi resenas;

    public PublicoGraphQlController(NegociosPublicosApi publico, ResenasPublicasApi resenas) {
        this.publico = publico;
        this.resenas = resenas;
    }

    @QueryMapping
    public NegocioPublicoVista negocioPublico(@Argument UUID id) {
        return publico.negocioPublico(id).orElse(null);
    }

    @QueryMapping
    public PaginaPublica negociosPublicos(@Argument FiltroPublico filtro, @Argument int primeros,
                                          @Argument String despues) {
        return publico.negociosPublicos(filtro, primeros, despues);
    }

    @QueryMapping
    public PaginaResenasPublicas resenasDeNegocio(@Argument UUID negocioId, @Argument int primeros,
                                                  @Argument String despues) {
        return resenas.resenasDeNegocio(negocioId, primeros, despues);
    }

    // ---- formas que el esquema pide distintas de las vistas ----

    @SchemaMapping(typeName = "NegocioPublico", field = "giro")
    public Map<String, String> giro(NegocioPublicoVista negocio) {
        return Map.of("codigo", negocio.giro(), "nombre", negocio.giroNombre());
    }

    @SchemaMapping(typeName = "NegocioPublicoConexion", field = "pageInfo")
    public Map<String, Object> pageInfo(PaginaPublica pagina) {
        return paginaInfo(pagina.hayMas(), pagina.cursorFinal());
    }

    @SchemaMapping(typeName = "ResenaPublicaConexion", field = "pageInfo")
    public Map<String, Object> pageInfo(PaginaResenasPublicas pagina) {
        return paginaInfo(pagina.hayMas(), pagina.cursorFinal());
    }

    @SchemaMapping(typeName = "ResenaPublica", field = "respuesta")
    public Map<String, Object> respuesta(ResenaPublicaVista resena) {
        return resena.respuesta() == null ? null : Map.of("texto", resena.respuesta(), "respondidaEn",
                resena.respondidaEn());
    }

    private static Map<String, Object> paginaInfo(boolean hayMas, String cursorFinal) {
        var info = new LinkedHashMap<String, Object>();
        info.put("hayMas", hayMas);
        info.put("cursorFinal", cursorFinal);
        return info;
    }
}
