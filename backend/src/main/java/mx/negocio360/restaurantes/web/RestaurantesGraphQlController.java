package mx.negocio360.restaurantes.web;

import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.restaurantes.aplicacion.CategoriaVista;
import mx.negocio360.restaurantes.aplicacion.ConsumoFallido;
import mx.negocio360.restaurantes.aplicacion.ConsumosServicio;
import mx.negocio360.restaurantes.aplicacion.CostoDeReceta;
import mx.negocio360.restaurantes.aplicacion.FiltroPlatillos;
import mx.negocio360.restaurantes.aplicacion.MenuPublicoServicio;
import mx.negocio360.restaurantes.aplicacion.MenuPublicoVista;
import mx.negocio360.restaurantes.aplicacion.MenuServicio;
import mx.negocio360.restaurantes.aplicacion.PaginaDeConsumosFallidos;
import mx.negocio360.restaurantes.aplicacion.PaginaDePlatillos;
import mx.negocio360.restaurantes.aplicacion.PlatilloVista;
import mx.negocio360.restaurantes.aplicacion.RecetaVista;
import mx.negocio360.restaurantes.aplicacion.RecetasServicio;
import mx.negocio360.restaurantes.aplicacion.ResumenDeMenu;
import mx.negocio360.restaurantes.dominio.ModoInventario;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Los resolvedores de Restaurantes (R1): el menu del panel y el menu publico. Solo traducen GraphQL a los servicios: el
 * negocio, el actor y los permisos salen de la sesion que dejo el filtro de entrada ({@link EntradaGraphQl}), nunca de un
 * argumento (SG-02); sin sesion o sin negocio activo fallan cerrado y cada servicio exige su permiso (SG-04). {@code menuPublico}
 * es la excepcion acotada: no tiene sesion y el negocio es el objeto consultado (D-170).
 */
@Controller
@ConditionalOnProperty(name = "restaurantes.almacen", havingValue = "memoria")
public class RestaurantesGraphQlController {

    private final MenuServicio menu;
    private final MenuPublicoServicio publico;
    private final RecetasServicio recetas;
    private final ConsumosServicio consumos;

    /**
     * Arma el controlador.
     *
     * @param menu    el menu del panel
     * @param publico el menu publico
     * @param recetas las recetas y su costo
     * @param consumos los consumos de insumos que fallaron
     */
    public RestaurantesGraphQlController(MenuServicio menu, MenuPublicoServicio publico, RecetasServicio recetas,
                                         ConsumosServicio consumos) {
        this.menu = menu;
        this.publico = publico;
        this.recetas = recetas;
        this.consumos = consumos;
    }

    // ---- consultas del panel ----

    @QueryMapping
    public List<CategoriaVista> categoriasDeMenu(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return menu.categorias(entrada.exigirContexto(), entrada.autorizacion());
    }

    @QueryMapping
    public PaginaDePlatillos platillos(@Argument FiltroPlatillos filtro, @Argument int primeros,
                                       @Argument String despues, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return menu.platillos(entrada.exigirContexto(), entrada.autorizacion(),
                filtro == null ? FiltroPlatillos.TODOS : filtro, primeros, despues);
    }

    @QueryMapping
    public PlatilloVista platillo(@Argument UUID id, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return menu.platillo(entrada.exigirContexto(), entrada.autorizacion(), id);
    }

    @QueryMapping
    public ResumenDeMenu resumenDeMenu(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return menu.resumenDeMenu(entrada.exigirContexto(), entrada.autorizacion());
    }

    /** La receta de un platillo, sin costos (RC-10). Requiere {@code restaurantes.receta.ver}. */
    @QueryMapping
    public RecetaVista receta(@Argument UUID platilloId, @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return recetas.receta(entrada.exigirContexto(), entrada.autorizacion(), platilloId);
    }

    /** Costo teorico y margen de la version vigente. Requiere {@code restaurantes.costo.ver}. */
    @QueryMapping
    public CostoDeReceta costoDeReceta(@Argument UUID platilloId,
                                       @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return recetas.costoDeReceta(entrada.exigirContexto(), entrada.autorizacion(), platilloId);
    }

    /** Los consumos que Inventario no pudo descontar, del mas nuevo al mas viejo. Requiere {@code restaurantes.consumo.ver}. */
    @QueryMapping
    public PaginaDeConsumosFallidos consumosFallidos(@Argument int primeros, @Argument String despues,
                                                     @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return consumos.consumosFallidos(entrada.exigirContexto(), entrada.autorizacion(), primeros, despues);
    }

    /** El texto fijo de un consumo fallido, segun su codigo. */
    @SchemaMapping(typeName = "RestConsumoFallido", field = "mensaje")
    public String mensaje(ConsumoFallido fallido) {
        return fallido.mensaje();
    }

    // ---- consulta publica ----

    /** Sin sesion (SG-01 lo permite aqui y en nada mas): un negocio que no es un restaurante visible responde nulo. */
    @QueryMapping
    public MenuPublicoVista menuPublico(@Argument UUID negocioId, @Argument String idioma) {
        return publico.menuPublico(negocioId, idioma);
    }

    // ---- mutaciones del panel ----

    @MutationMapping
    public CategoriaVista crearCategoria(@Argument CategoriaInput input,
                                         @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return menu.crearCategoria(entrada.exigirContexto(), entrada.autorizacion(), input.aDatos());
    }

    @MutationMapping
    public CategoriaVista editarCategoria(@Argument UUID id, @Argument int version, @Argument CategoriaInput input,
                                          @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return menu.editarCategoria(entrada.exigirContexto(), entrada.autorizacion(), id, version, input.aDatos());
    }

    @MutationMapping
    public PlatilloVista crearPlatillo(@Argument PlatilloInput input,
                                       @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return menu.crearPlatillo(entrada.exigirContexto(), entrada.autorizacion(), input.aDatos());
    }

    @MutationMapping
    public PlatilloVista editarPlatillo(@Argument UUID id, @Argument int version, @Argument PlatilloInput input,
                                        @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return menu.editarPlatillo(entrada.exigirContexto(), entrada.autorizacion(), id, version, input.aDatos());
    }

    @MutationMapping
    public PlatilloVista marcarAgotado(@Argument UUID id, @Argument boolean agotado,
                                       @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return menu.marcarAgotado(entrada.exigirContexto(), entrada.autorizacion(), id, agotado);
    }

    @MutationMapping
    public PlatilloVista publicarPlatillo(@Argument UUID id, @Argument boolean publicar,
                                          @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return menu.publicarPlatillo(entrada.exigirContexto(), entrada.autorizacion(), id, publicar);
    }

    /** Cierra la version abierta de la receta (RC-09). Requiere {@code restaurantes.receta.editar}. */
    @MutationMapping
    public RecetaVista desactivarReceta(@Argument UUID platilloId,
                                        @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return recetas.desactivarReceta(entrada.exigirContexto(), entrada.autorizacion(), platilloId);
    }

    @MutationMapping
    public PlatilloVista cambiarModoDeInventario(@Argument UUID id, @Argument ModoInventario modo,
                                                 @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        return menu.cambiarModo(entrada.exigirContexto(), entrada.autorizacion(), id, modo);
    }
}
