package mx.negocio360.clientes.aplicacion;

import java.time.Clock;
import java.util.Objects;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioClientes;
import mx.negocio360.clientes.dominio.Cliente;
import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.dominio.DatosCliente;
import mx.negocio360.clientes.dominio.EstadoCliente;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.tiempo.ZonaMexico;

/**
 * EditarCliente (I-01 a I-03, I-06 a I-08). Un campo nulo no cambia y un texto vacio borra los opcionales. La version
 * de la persona debe coincidir con la guardada (bloqueo optimista); si no, CONCURRENCIA. Un cliente anonimizado no
 * se edita. No publica eventos.
 */
public final class ManejadorEditarCliente implements ManejadorComando<EditarClienteComando> {

    public static final String TIPO = "editar_cliente";

    private final RepositorioClientes clientes;
    private final Clock reloj;

    public ManejadorEditarCliente(RepositorioClientes clientes, Clock reloj) {
        this.clientes = Objects.requireNonNull(clientes, "clientes");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<EditarClienteComando> claseComando() {
        return EditarClienteComando.class;
    }

    @Override
    public String preparar(ContextoNegocio contexto, EditarClienteComando comando) {
        var cambio = validar(contexto, comando);
        return "Actualizar los datos de " + cambio.datosNuevos().nombreCompleto() + ".";
    }

    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, EditarClienteComando comando) {
        var cambio = validar(contexto, comando);
        var actualizado = cambio.actual().conDatos(cambio.datosNuevos(), reloj.instant());
        clientes.actualizar(actualizado, comando.version());
        return ResultadoComando.ok(PlantillasClientes.CLIENTE_ACTUALIZADO, actualizado.id());
    }

    private Cambio validar(ContextoNegocio contexto, EditarClienteComando comando) {
        var actual = clientes.porId(contexto.negocioId(), comando.clienteId())
                .orElseThrow(() -> new ClientesException(CodigoError.CLIENTE_NO_ENCONTRADO));
        if (actual.estado() == EstadoCliente.ANONIMIZADO) {
            throw new ClientesException(CodigoError.CLIENTE_ANONIMIZADO);
        }
        if (actual.version() != comando.version()) {
            throw new ClientesException(CodigoError.CONCURRENCIA);
        }
        var previos = actual.datos();
        var nuevos = DatosCliente.de(
                elegir(comando.nombre(), previos.nombre()),
                elegir(comando.apellidos(), previos.apellidos()),
                elegir(comando.telefono(), previos.telefono()),
                elegir(comando.correo(), previos.correo()),
                comando.fechaNacimiento() != null ? comando.fechaNacimiento() : previos.fechaNacimiento(),
                elegir(comando.notas(), previos.notas()),
                ZonaMexico.hoy(reloj));
        if (nuevos.telefono() != null) {
            clientes.activoPorTelefono(contexto.negocioId(), nuevos.telefono())
                    .filter(otro -> !otro.id().equals(actual.id()))
                    .ifPresent(otro -> {
                        throw new ClientesException(CodigoError.TELEFONO_DUPLICADO, otro.id());
                    });
        }
        return new Cambio(actual, nuevos);
    }

    /** Nulo conserva el valor actual; cualquier texto (incluso vacio) lo reemplaza. */
    private static String elegir(String enviado, String actual) {
        return enviado != null ? enviado : actual;
    }

    private record Cambio(Cliente actual, DatosCliente datosNuevos) {
    }
}
