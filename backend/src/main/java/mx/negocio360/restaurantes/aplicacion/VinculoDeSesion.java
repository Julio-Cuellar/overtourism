package mx.negocio360.restaurantes.aplicacion;

import java.util.function.Supplier;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;

/**
 * Pone la sesion de la peticion al alcance de {@link AutorizacionDeRestaurantes} mientras corre una accion. Los manejadores de
 * comandos revisan su permiso por dentro como segunda defensa (D-126), pero el hilo que ejecuta un comando de GraphQL no
 * conserva el contexto de seguridad de la peticion, asi que quien despacha el comando entrega aqui la sesion que ya tiene
 * (la de {@code EntradaGraphQl}) en vez de que el adaptador la busque en el hilo.
 */
public interface VinculoDeSesion {

    /**
     * Ejecuta la accion con esa sesion vinculada y la libera al terminar, haya o no excepcion.
     *
     * @param contexto     negocio y actor de la peticion
     * @param autorizacion permisos de la persona de la peticion
     * @param accion       lo que se ejecuta con la sesion vinculada
     * @return lo que devuelve la accion
     */
    <T> T vincular(ContextoNegocio contexto, Autorizacion autorizacion, Supplier<T> accion);
}
