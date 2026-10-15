package mx.negocio360.contabilidad.aplicacion;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.ContabilidadApi;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPolizas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioReglas;
import mx.negocio360.contabilidad.dominio.Balanza;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.ReglaContable;

/** Implementacion de {@link ContabilidadApi}: reparte las escrituras a preparar -> confirmar y resuelve consultas. */
public final class ContabilidadServicio implements ContabilidadApi {

    private final ServicioPreparacion preparacion;
    private final RepositorioCuentas cuentas;
    private final RepositorioReglas reglas;
    private final RepositorioPolizas polizas;
    private final LibroDiario libro;

    /**
     * Crea el servicio.
     *
     * @param preparacion servicio de preparar y confirmar
     * @param cuentas     tabla de cuentas
     * @param reglas      tabla de reglas
     * @param polizas     tablas de polizas y movimientos
     * @param libro       libro diario, que tambien lleva los saldos
     */
    public ContabilidadServicio(ServicioPreparacion preparacion, RepositorioCuentas cuentas,
                                RepositorioReglas reglas, RepositorioPolizas polizas, LibroDiario libro) {
        this.preparacion = Objects.requireNonNull(preparacion, "preparacion");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.polizas = Objects.requireNonNull(polizas, "polizas");
        this.libro = Objects.requireNonNull(libro, "libro");
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
}
