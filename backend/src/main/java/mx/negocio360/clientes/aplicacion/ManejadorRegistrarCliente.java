package mx.negocio360.clientes.aplicacion;

import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import mx.negocio360.clientes.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioAvisos;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioClientes;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioConsentimientos;
import mx.negocio360.clientes.dominio.Cliente;
import mx.negocio360.clientes.dominio.ClientesException;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.dominio.Consentimiento;
import mx.negocio360.clientes.dominio.DatosCliente;
import mx.negocio360.clientes.dominio.FormaConsentimiento;
import mx.negocio360.clientes.dominio.Telefono;
import mx.negocio360.clientes.eventos.ClienteConsentimientoCambiado;
import mx.negocio360.clientes.eventos.ClienteRegistrado;
import mx.negocio360.clientes.eventos.EstadoConsentimiento;
import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.OrigenCliente;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.tiempo.ZonaMexico;

/**
 * RegistrarCliente (I-01 a I-03, I-07, I-08, P-01, P-04, L-01, L-03). Preparar y confirmar ejecutan la misma
 * validacion; solo confirmar escribe el cliente, sus consentimientos y los eventos.
 */
public final class ManejadorRegistrarCliente implements ManejadorComando<RegistrarClienteComando> {

    public static final String TIPO = "registrar_cliente";

    private final RepositorioClientes clientes;
    private final RepositorioAvisos avisos;
    private final RepositorioConsentimientos consentimientos;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public ManejadorRegistrarCliente(RepositorioClientes clientes, RepositorioAvisos avisos,
                                     RepositorioConsentimientos consentimientos, PublicadorEventos eventos,
                                     Clock reloj) {
        this.clientes = Objects.requireNonNull(clientes, "clientes");
        this.avisos = Objects.requireNonNull(avisos, "avisos");
        this.consentimientos = Objects.requireNonNull(consentimientos, "consentimientos");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<RegistrarClienteComando> claseComando() {
        return RegistrarClienteComando.class;
    }

    @Override
    public String preparar(ContextoNegocio contexto, RegistrarClienteComando comando) {
        var plan = validar(contexto, comando);
        var telefono = plan.datos().telefono();
        return "Registrar al cliente " + plan.datos().nombreCompleto()
                + (telefono == null ? "" : ", teléfono " + Telefono.enmascarar(telefono)) + ".";
    }

    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, RegistrarClienteComando comando) {
        var plan = validar(contexto, comando);
        var ahora = reloj.instant();
        var negocioId = contexto.negocioId();
        var cliente = Cliente.nuevo(UuidV7.generar(ahora), negocioId, plan.datos(), OrigenCliente.PANEL,
                plan.avisoVersion(), contexto.actor(), ahora);
        clientes.insertar(cliente);
        eventos.publicar(FabricaSobres.de(negocioId, ahora, cliente.id(), ClienteRegistrado.TIPO,
                ClienteRegistrado.VERSION, new ClienteRegistrado(cliente.id(), OrigenCliente.PANEL).datos()));
        for (var solicitado : plan.consentimientos()) {
            consentimientos.insertar(new Consentimiento(UuidV7.generar(ahora), negocioId, cliente.id(),
                    solicitado.finalidad(), EstadoConsentimiento.OTORGADO, FormaConsentimiento.EXPRESO,
                    solicitado.medio(), plan.avisoVersion(), contexto.actor(), ahora));
            var cambio = new ClienteConsentimientoCambiado(cliente.id(), solicitado.finalidad(),
                    EstadoConsentimiento.OTORGADO, solicitado.medio());
            eventos.publicar(FabricaSobres.de(negocioId, ahora, cliente.id(), ClienteConsentimientoCambiado.TIPO,
                    ClienteConsentimientoCambiado.VERSION, cambio.datos()));
        }
        return ResultadoComando.ok(PlantillasClientes.CLIENTE_GUARDADO, cliente.id());
    }

    private Plan validar(ContextoNegocio contexto, RegistrarClienteComando comando) {
        var datos = DatosCliente.de(comando.nombre(), comando.apellidos(), comando.telefono(), comando.correo(),
                comando.fechaNacimiento(), comando.notas(), ZonaMexico.hoy(reloj));
        var avisoVersion = versionDelAviso(contexto, comando.avisoVersion());
        var solicitados = consentimientosValidos(comando.consentimientos());
        if (datos.telefono() != null) {
            clientes.activoPorTelefono(contexto.negocioId(), datos.telefono()).ifPresent(existente -> {
                throw new ClientesException(CodigoError.TELEFONO_DUPLICADO, existente.id());
            });
        }
        return new Plan(datos, avisoVersion, solicitados);
    }

    /** L-01 y L-03: hay que decir que version del aviso se entrego, y debe ser la vigente del negocio. */
    private int versionDelAviso(ContextoNegocio contexto, Integer pedida) {
        if (pedida == null || pedida < 1) {
            throw new ClientesException(CodigoError.AVISO_VERSION_REQUERIDA);
        }
        var vigente = avisos.vigente(contexto.negocioId())
                .orElseThrow(() -> new ClientesException(CodigoError.AVISO_NO_VIGENTE));
        if (vigente.version() != pedida) {
            throw new ClientesException(CodigoError.AVISO_NO_VIGENTE);
        }
        return vigente.version();
    }

    private static List<ConsentimientoSolicitado> consentimientosValidos(List<ConsentimientoSolicitado> pedidos) {
        if (pedidos == null) {
            return List.of();
        }
        var vistas = EnumSet.noneOf(Finalidad.class);
        for (var pedido : pedidos) {
            if (pedido == null || pedido.finalidad() == null) {
                throw new ClientesException(CodigoError.FINALIDAD_INVALIDA);
            }
            if (pedido.medio() == null) {
                throw new ClientesException(CodigoError.CONSENTIMIENTO_INCOMPLETO);
            }
            if (!vistas.add(pedido.finalidad())) {
                throw new ClientesException(CodigoError.FINALIDAD_INVALIDA);
            }
        }
        return List.copyOf(pedidos);
    }

    private record Plan(DatosCliente datos, int avisoVersion, List<ConsentimientoSolicitado> consentimientos) {
    }
}
