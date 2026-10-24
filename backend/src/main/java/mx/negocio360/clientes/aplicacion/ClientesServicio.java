package mx.negocio360.clientes.aplicacion;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.clientes.ClientesApi;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioAvisos;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioClientes;
import mx.negocio360.clientes.consultas.LimitesDeConsulta;
import mx.negocio360.clientes.dominio.AvisoPrivacidad;
import mx.negocio360.clientes.dominio.Cliente;
import mx.negocio360.clientes.dominio.CriterioBusqueda;
import mx.negocio360.clientes.dominio.Telefono;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/** Implementacion de {@link ClientesApi}: reparte las escrituras al patron preparar -> confirmar y resuelve consultas. */
public final class ClientesServicio implements ClientesApi {

    private final ServicioPreparacion preparacion;
    private final RepositorioClientes clientes;
    private final RepositorioAvisos avisos;

    public ClientesServicio(ServicioPreparacion preparacion, RepositorioClientes clientes, RepositorioAvisos avisos) {
        this.preparacion = Objects.requireNonNull(preparacion, "preparacion");
        this.clientes = Objects.requireNonNull(clientes, "clientes");
        this.avisos = Objects.requireNonNull(avisos, "avisos");
    }

    @Override
    public PreparacionCreada preparar(ContextoNegocio contexto, Comando comando) {
        return preparacion.preparar(contexto, comando);
    }

    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, String token) {
        return preparacion.confirmar(contexto, token);
    }

    @Override
    public Optional<String> tipoDeComando(ContextoNegocio contexto, String token) {
        return preparacion.tipoDeComando(contexto, token);
    }

    @Override
    public PaginaClientes buscar(ContextoNegocio contexto, String texto, int primeros, String despues) {
        if (texto != null && texto.strip().length() > LimitesDeConsulta.MAX_TEXTO_BUSQUEDA) {
            throw new IllegalArgumentException("El texto de busqueda excede el maximo permitido");
        }
        var limite = LimitesDeConsulta.paginaEfectiva(primeros);
        var cursor = leerCursor(despues);
        var criterio = CriterioBusqueda.de(texto);
        if (criterio instanceof CriterioBusqueda.Ninguno) {
            return new PaginaClientes(List.of(), false, null);
        }
        var filas = clientes.buscar(contexto.negocioId(), criterio, cursor, limite + 1);
        var nodos = filas.stream().limit(limite).map(ClientesServicio::listado).toList();
        var ultimo = nodos.isEmpty() ? null : nodos.get(nodos.size() - 1).id().toString();
        return new PaginaClientes(nodos, filas.size() > limite, ultimo);
    }

    @Override
    public List<ClienteListado> sugerir(ContextoNegocio contexto, String texto) {
        if (!LimitesDeConsulta.textoDeSugerenciaValido(texto)) {
            throw new IllegalArgumentException("El texto de la sugerencia debe tener entre "
                    + LimitesDeConsulta.MIN_TEXTO_SUGERENCIA + " y " + LimitesDeConsulta.MAX_TEXTO_BUSQUEDA
                    + " caracteres");
        }
        var criterio = CriterioBusqueda.de(texto);
        if (criterio instanceof CriterioBusqueda.Ninguno) {
            return List.of();
        }
        return clientes.buscar(contexto.negocioId(), criterio, null, LimitesDeConsulta.MAX_SUGERENCIAS).stream()
                .map(ClientesServicio::listado).toList();
    }

    @Override
    public Optional<AvisoPrivacidad> avisoVigente(ContextoNegocio contexto) {
        return avisos.vigente(contexto.negocioId());
    }

    private static ClienteListado listado(Cliente cliente) {
        var telefono = cliente.datos().telefono();
        return new ClienteListado(cliente.id(), cliente.datos().nombreCompleto(),
                telefono == null ? null : Telefono.enmascarar(telefono));
    }

    private static UUID leerCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        var texto = cursor.strip();
        try {
            var id = UUID.fromString(texto);
            // UUID.fromString acepta formas abreviadas ("1-1-1-1-1"): solo se admite la canonica.
            if (id.toString().equals(texto.toLowerCase(Locale.ROOT))) {
                return id;
            }
        } catch (IllegalArgumentException e) {
            // Se responde igual que con una forma abreviada.
        }
        throw new IllegalArgumentException("El cursor no es valido");
    }
}
