package mx.negocio360.clientes;

import java.util.List;
import java.util.Optional;
import mx.negocio360.clientes.aplicacion.ClienteListado;
import mx.negocio360.clientes.aplicacion.Comando;
import mx.negocio360.clientes.aplicacion.PaginaClientes;
import mx.negocio360.clientes.aplicacion.PreparacionCreada;
import mx.negocio360.clientes.aplicacion.ResultadoComando;
import mx.negocio360.clientes.dominio.AvisoPrivacidad;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Funciones publicas del modulo (seccion 7). Los resolvedores GraphQL y el controlador REST llaman a esta interfaz y
 * nunca a los repositorios. El negocio y el actor salen siempre del contexto, no de un argumento (SG-02).
 *
 * <p>Las busquedas devuelven listas: ninguna operacion elige a un cliente a partir de un texto (I-05).
 */
public interface ClientesApi {

    /** Valida un comando sin escribir y devuelve el token de un solo uso con el resumen para confirmar. */
    PreparacionCreada preparar(ContextoNegocio contexto, Comando comando);

    /** Ejecuta el comando preparado. Falla con TOKEN_* si el token no sirve (D-23). */
    ResultadoComando confirmar(ContextoNegocio contexto, String token);

    /**
     * Clientes activos por parte del nombre o por telefono completo, de {@code primeros} en {@code primeros} (maximo
     * 50). {@code despues} es el cursor de la pagina anterior.
     */
    PaginaClientes buscar(ContextoNegocio contexto, String texto, int primeros, String despues);

    /** Hasta 5 candidatos para la pantalla de venta. El texto lleva entre 2 y 80 caracteres. */
    List<ClienteListado> sugerir(ContextoNegocio contexto, String texto);

    /** El aviso de privacidad vigente del negocio, si publico alguno. */
    Optional<AvisoPrivacidad> avisoVigente(ContextoNegocio contexto);

    /**
     * El tipo del comando que espera confirmacion con ese token (por ejemplo {@code registrar_cliente}), para que el canal
     * exija el permiso de ese comando antes de gastar el token (D-126).
     *
     * @param contexto negocio y actor que prepararon
     * @param token    token en claro
     * @return el tipo si el token es vigente y de esa persona; vacio en cualquier otro caso
     */
    Optional<String> tipoDeComando(ContextoNegocio contexto, String token);
}
