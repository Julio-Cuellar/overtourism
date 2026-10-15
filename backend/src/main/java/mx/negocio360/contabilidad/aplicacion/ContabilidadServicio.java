package mx.negocio360.contabilidad.aplicacion;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.contabilidad.ContabilidadApi;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCortesContables;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPendientes;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPeriodos;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPolizas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioReglas;
import mx.negocio360.contabilidad.dominio.Balanza;
import mx.negocio360.contabilidad.dominio.CorteContable;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.EstadoPendiente;
import mx.negocio360.contabilidad.dominio.Pendiente;
import mx.negocio360.contabilidad.dominio.Periodo;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.ReglaContable;
import mx.negocio360.contabilidad.dominio.ResultadoEvento;

/** Implementacion de {@link ContabilidadApi}: reparte las escrituras a preparar -> confirmar y resuelve consultas. */
public final class ContabilidadServicio implements ContabilidadApi {

    private final ServicioPreparacion preparacion;
    private final RepositorioCuentas cuentas;
    private final RepositorioReglas reglas;
    private final RepositorioPolizas polizas;
    private final LibroDiario libro;
    private final Contabilizador contabilizador;
    private final RepositorioPendientes pendientes;
    private final RepositorioConfiguracion configuracion;
    private final RepositorioPeriodos periodos;
    private final RepositorioCortesContables cortes;

    /**
     * Crea el servicio.
     *
     * @param preparacion servicio de preparar y confirmar
     * @param cuentas     tabla de cuentas
     * @param reglas      tabla de reglas
     * @param polizas     tablas de polizas y movimientos
     * @param libro          libro diario, que tambien lleva los saldos
     * @param contabilizador escrituras del sistema
     * @param pendientes     tabla de pendientes
     * @param configuracion  tabla de configuracion
     * @param periodos       tabla de periodos
     * @param cortes         tabla de cortes de caja
     */
    public ContabilidadServicio(ServicioPreparacion preparacion, RepositorioCuentas cuentas,
                                RepositorioReglas reglas, RepositorioPolizas polizas, LibroDiario libro,
                                Contabilizador contabilizador, RepositorioPendientes pendientes,
                                RepositorioConfiguracion configuracion, RepositorioPeriodos periodos,
                                RepositorioCortesContables cortes) {
        this.cortes = Objects.requireNonNull(cortes, "cortes");
        this.preparacion = Objects.requireNonNull(preparacion, "preparacion");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.polizas = Objects.requireNonNull(polizas, "polizas");
        this.libro = Objects.requireNonNull(libro, "libro");
        this.contabilizador = Objects.requireNonNull(contabilizador, "contabilizador");
        this.pendientes = Objects.requireNonNull(pendientes, "pendientes");
        this.configuracion = Objects.requireNonNull(configuracion, "configuracion");
        this.periodos = Objects.requireNonNull(periodos, "periodos");
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
    public List<Cuenta> catalogo(ContextoNegocio contexto) {
        return cuentas.delNegocio(contexto.negocioId()).stream().sorted(Comparator.comparing(Cuenta::codigo))
                .toList();
    }

    @Override
    public List<ReglaContable> reglas(ContextoNegocio contexto) {
        return reglas.delNegocio(contexto.negocioId());
    }

    @Override
    public Optional<Poliza> poliza(ContextoNegocio contexto, UUID polizaId) {
        return polizas.porId(contexto.negocioId(), polizaId);
    }

    @Override
    public Balanza balanza(ContextoNegocio contexto, YearMonth mes) {
        return libro.balanza(contexto.negocioId(), mes);
    }

    @Override
    public ResultadoEvento contabilizarVenta(SobreEvento evento) {
        return contabilizador.contabilizarVenta(evento);
    }

    @Override
    public ResultadoEvento contabilizarCorte(SobreEvento evento) {
        return contabilizador.contabilizarCorte(evento);
    }

    @Override
    public ResultadoEvento contabilizarCanje(SobreEvento evento) {
        return contabilizador.contabilizarCanje(evento);
    }

    @Override
    public List<CorteContable> cortes(ContextoNegocio contexto) {
        return cortes.delNegocio(contexto.negocioId());
    }

    @Override
    public List<Pendiente> pendientes(ContextoNegocio contexto, EstadoPendiente estado) {
        return pendientes.delNegocio(contexto.negocioId(), estado);
    }

    @Override
    public String abrirPeriodo(UUID negocioId, YearMonth mes) {
        if (configuracion.delNegocio(negocioId).isEmpty()) {
            return "CONTABILIDAD_NO_ACTIVA";
        }
        return libro.abrirPeriodo(negocioId, mes) ? "ABIERTO" : "YA_ABIERTO";
    }

    @Override
    public List<Periodo> periodos(ContextoNegocio contexto, int anio) {
        return periodos.delNegocio(contexto.negocioId()).stream().filter(p -> p.mes().getYear() == anio).toList();
    }
}
