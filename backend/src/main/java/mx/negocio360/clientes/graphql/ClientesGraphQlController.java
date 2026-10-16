package mx.negocio360.clientes.graphql;

import java.util.List;
import java.util.Objects;
import mx.negocio360.clientes.ClientesApi;
import mx.negocio360.clientes.aplicacion.ClienteListado;
import mx.negocio360.clientes.consultas.LimitesDeConsulta;
import mx.negocio360.clientes.dominio.AvisoPrivacidad;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.comandos.ComandoGraphQlException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Los resolvedores de consulta de Clientes que ya tienen caso de uso: {@code sugerirClientes} y
 * {@code avisoPrivacidadVigente}. El negocio, el actor y los permisos salen de la sesion (SG-02); cada consulta vuelve a
 * exigir su permiso, ademas de la puerta de la plataforma (dos barreras).
 *
 * <p>Las demas consultas del esquema ({@code clientes}, {@code cliente}, {@code historialCliente},
 * {@code consentimientosCliente}, {@code panoramaClientes}, {@code solicitudesArco}) dependen del historial de compras, del
 * consentimiento y de ARCO, que todavia no estan programados: responden {@code OPERACION_NO_DISPONIBLE} en lugar de
 * inventar cifras.
 */
@Controller
public class ClientesGraphQlController {

    private final ClientesApi clientes;

    /**
     * Arma el controlador.
     *
     * @param clientes API de Clientes
     */
    public ClientesGraphQlController(ClientesApi clientes) {
        this.clientes = Objects.requireNonNull(clientes, "clientes");
    }

    /** SG-07: entre 2 y 80 caracteres; devuelve a lo mas 5, con el telefono enmascarado. */
    @QueryMapping
    public List<ClienteListado> sugerirClientes(@Argument String texto,
                                                @ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var contexto = entrada.exigirContexto();
        entrada.autorizacion().exigir("clientes.sugerir");
        if (!LimitesDeConsulta.textoDeSugerenciaValido(texto)) {
            throw new ComandoGraphQlException(ComandoGraphQlException.Codigo.CONSULTA_INVALIDA);
        }
        return clientes.sugerir(contexto, texto);
    }

    /** L-01: el aviso de privacidad vigente del negocio, o nulo si aun no publica uno. */
    @QueryMapping
    public AvisoPrivacidad avisoPrivacidadVigente(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        var contexto = entrada.exigirContexto();
        entrada.autorizacion().exigir("clientes.cliente.registrar");
        return clientes.avisoVigente(contexto).orElse(null);
    }

    /** Pendiente de CLI-I3: el listado completo trae compras y totales que aun no existen. */
    @QueryMapping
    public Object clientes(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        throw noDisponible(entrada);
    }

    /** Pendiente: la ficha completa y su auditoria de acceso (P-05). */
    @QueryMapping
    public Object cliente(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        throw noDisponible(entrada);
    }

    /** Pendiente de CLI-I3. */
    @QueryMapping
    public Object historialCliente(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        throw noDisponible(entrada);
    }

    /** Pendiente de CLI-I4. */
    @QueryMapping
    public Object consentimientosCliente(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        throw noDisponible(entrada);
    }

    /** Pendiente de CLI-I3. */
    @QueryMapping
    public Object panoramaClientes(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        throw noDisponible(entrada);
    }

    /** Pendiente de CLI-I5. */
    @QueryMapping
    public Object solicitudesArco(@ContextValue(EntradaGraphQl.CLAVE) EntradaGraphQl entrada) {
        throw noDisponible(entrada);
    }

    private static ComandoGraphQlException noDisponible(EntradaGraphQl entrada) {
        entrada.exigirContexto();
        return new ComandoGraphQlException(ComandoGraphQlException.Codigo.OPERACION_NO_DISPONIBLE);
    }
}
