package mx.negocio360.canal.web;

import graphql.schema.DataFetchingEnvironment;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Donde vive el {@link ContextoNegocio} durante una peticion GraphQL. Lo pone {@link InterceptorDeSesion} a partir de
 * la sesion; ningun argumento de GraphQL lo puede fijar (AZ-05, SG-02).
 */
public final class ContextoGraphQl {

    /** Clave del contexto en el {@code GraphQLContext} de la peticion. */
    public static final String CLAVE = "negocio360.contexto";

    private ContextoGraphQl() {
    }

    /**
     * Contexto de la peticion.
     *
     * @param entorno entorno del resolvedor
     * @return negocio y actor de la sesion
     * @throws CanalException NO_AUTENTICADO si la peticion no trae una sesion con negocio
     */
    public static ContextoNegocio requerido(DataFetchingEnvironment entorno) {
        ContextoNegocio contexto = entorno.getGraphQlContext().get(CLAVE);
        if (contexto == null) {
            throw new CanalException(CanalException.Codigo.NO_AUTENTICADO);
        }
        return contexto;
    }
}
