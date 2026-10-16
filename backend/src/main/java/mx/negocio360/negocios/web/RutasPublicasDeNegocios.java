package mx.negocio360.negocios.web;

import java.util.List;
import mx.negocio360.compartido.seguridad.RutasPublicas;
import org.springframework.stereotype.Component;

/**
 * Las rutas REST de Negocios que no piden una sesion de Identidad (SG-01): la plataforma falla cerrado, asi que cada una se
 * declara. {@code /catalogos} es el catalogo publico de solo lectura; {@code /admin} es la moderacion, que valida su propio
 * token de servicio y solo existe si hay uno configurado; {@code /demo} son los comandos del stand, que solo existen con el
 * perfil {@code demo} y nunca con {@code produccion}. Se declaran siempre, incluso donde la ruta no existe, para que ahi
 * responda 404 y no 401: no existe y no hay nada que proteger. Todo lo demas de Negocios entra por GraphQL, con su filtro.
 */
@Component
public final class RutasPublicasDeNegocios implements RutasPublicas {

    @Override
    public List<String> patrones() {
        return List.of("/catalogos/**", "/admin/**", "/demo/**");
    }
}
