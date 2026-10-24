package mx.negocio360.restaurantes.infraestructura;

import java.util.Objects;
import java.util.function.Supplier;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.restaurantes.aplicacion.AutorizacionDeRestaurantes;
import mx.negocio360.restaurantes.aplicacion.VinculoDeSesion;

/**
 * Los permisos de la sesion de la peticion para los manejadores de comandos (AZ-04). La sesion no se busca en el contexto de
 * seguridad del hilo, que en el hilo de un comando de GraphQL esta vacio: quien despacha el comando la vincula con
 * {@link #vincular} mientras corre el manejador. Falla cerrado: fuera de un vinculo, o con el contexto de otro negocio, todo
 * permiso esta negado (SG-02).
 */
public final class SesionDeRestaurantes implements AutorizacionDeRestaurantes, VinculoDeSesion {

    private record Vinculada(ContextoNegocio contexto, Autorizacion autorizacion) {
    }

    private final ThreadLocal<Vinculada> enCurso = new ThreadLocal<>();

    @Override
    public boolean puede(ContextoNegocio contexto, String permiso) {
        var vinculada = enCurso.get();
        return vinculada != null && vinculada.contexto().negocioId().equals(contexto.negocioId())
                && vinculada.autorizacion().puede(permiso);
    }

    @Override
    public <T> T vincular(ContextoNegocio contexto, Autorizacion autorizacion, Supplier<T> accion) {
        Objects.requireNonNull(contexto, "contexto");
        Objects.requireNonNull(autorizacion, "autorizacion");
        var previa = enCurso.get();
        enCurso.set(new Vinculada(contexto, autorizacion));
        try {
            return accion.get();
        } finally {
            if (previa == null) {
                enCurso.remove();
            } else {
                enCurso.set(previa);
            }
        }
    }
}
