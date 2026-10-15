package mx.negocio360.contabilidad.aplicacion;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPeriodos;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.contabilidad.dominio.Balanza;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.Montos;
import mx.negocio360.contabilidad.dominio.Periodo;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.Saldo;

/**
 * Saldos por cuenta y periodo (A-08). Los actualiza al registrar cada poliza y los lee para la balanza. Una cuenta sin
 * movimientos en un mes arrastra el saldo final del mes anterior.
 */
final class Mayor {

    private final RepositorioCuentas cuentas;
    private final RepositorioPeriodos periodos;
    private final RepositorioSaldos saldos;

    Mayor(RepositorioCuentas cuentas, RepositorioPeriodos periodos, RepositorioSaldos saldos) {
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.periodos = Objects.requireNonNull(periodos, "periodos");
        this.saldos = Objects.requireNonNull(saldos, "saldos");
    }

    /** Suma los renglones de la poliza a los saldos de su periodo y desplaza los saldos de los periodos siguientes. */
    void aplicar(Poliza poliza, Periodo periodo) {
        var negocioId = poliza.negocioId();
        var meses = mesesPorPeriodo(negocioId);
        meses.put(periodo.id(), periodo.mes());
        for (var renglon : poliza.renglones()) {
            var cuenta = cuentas.porId(negocioId, renglon.cuentaId()).orElseThrow();
            var actual = saldos.de(negocioId, cuenta.id(), periodo.id())
                    .orElseGet(() -> Saldo.inicial(negocioId, cuenta.id(), periodo.id(),
                            arrastre(negocioId, cuenta.id(), periodo.mes(), meses)));
            saldos.guardar(actual.con(renglon.cargo(), renglon.abono(), cuenta.naturaleza()));
            var delta = Saldo.neto(renglon.cargo(), renglon.abono(), cuenta.naturaleza());
            for (var posterior : saldos.deCuenta(negocioId, cuenta.id())) {
                var mes = meses.get(posterior.periodoId());
                if (mes != null && mes.isAfter(periodo.mes())) {
                    saldos.guardar(posterior.desplazado(delta));
                }
            }
        }
    }

    /** Balanza del mes con todas las cuentas afectables del negocio. */
    Balanza balanza(UUID negocioId, YearMonth mes) {
        var meses = mesesPorPeriodo(negocioId);
        var periodo = periodos.delMes(negocioId, mes);
        var cero = Montos.exacto(BigDecimal.ZERO);
        var filas = cuentas.delNegocio(negocioId).stream().filter(Cuenta::afectable)
                .sorted(Comparator.comparing(Cuenta::codigo))
                .map(cuenta -> {
                    var saldo = periodo.flatMap(p -> saldos.de(negocioId, cuenta.id(), p.id()));
                    if (saldo.isPresent()) {
                        var s = saldo.get();
                        return new Balanza.Fila(cuenta.id(), cuenta.codigo(), cuenta.nombre(), cuenta.naturaleza(),
                                s.saldoInicial(), s.cargos(), s.abonos(), s.saldoFinal());
                    }
                    var arrastre = arrastre(negocioId, cuenta.id(), mes, meses);
                    return new Balanza.Fila(cuenta.id(), cuenta.codigo(), cuenta.nombre(), cuenta.naturaleza(),
                            arrastre, cero, cero, arrastre);
                })
                .toList();
        var cargos = filas.stream().map(Balanza.Fila::cargos).reduce(cero, BigDecimal::add);
        var abonos = filas.stream().map(Balanza.Fila::abonos).reduce(cero, BigDecimal::add);
        return new Balanza(mes, filas, cargos, abonos);
    }

    /** Saldo final del ultimo periodo anterior a {@code mes} en que la cuenta tuvo saldo, o cero. */
    private BigDecimal arrastre(UUID negocioId, UUID cuentaId, YearMonth mes, Map<UUID, YearMonth> meses) {
        return saldos.deCuenta(negocioId, cuentaId).stream()
                .filter(s -> meses.containsKey(s.periodoId()) && meses.get(s.periodoId()).isBefore(mes))
                .max(Comparator.comparing(s -> meses.get(s.periodoId())))
                .map(Saldo::saldoFinal)
                .orElse(Montos.exacto(BigDecimal.ZERO));
    }

    private Map<UUID, YearMonth> mesesPorPeriodo(UUID negocioId) {
        var meses = new HashMap<UUID, YearMonth>();
        periodos.delNegocio(negocioId).forEach(p -> meses.put(p.id(), p.mes()));
        return meses;
    }
}
