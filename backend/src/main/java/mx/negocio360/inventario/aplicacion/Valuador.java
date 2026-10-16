package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.inventario.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioMovimientos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.inventario.dominio.Articulo;
import mx.negocio360.inventario.dominio.CausaTipo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.Conversion;
import mx.negocio360.inventario.dominio.CruceDeUmbral;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.dominio.Limites;
import mx.negocio360.inventario.dominio.MotivoMerma;
import mx.negocio360.inventario.dominio.Movimiento;
import mx.negocio360.inventario.dominio.Saldo;
import mx.negocio360.inventario.dominio.TipoAlerta;
import mx.negocio360.inventario.dominio.TipoMovimiento;
import mx.negocio360.inventario.eventos.AlertaDeSaldo;
import mx.negocio360.inventario.eventos.MovimientoRegistrado;

/**
 * Lo que comparten todo lo que mueve el saldo: los cuatro comandos de movimiento (con confirmacion humana) y el consumo
 * por venta (sin ella). Valua igual en ambos: la entrada al costo capturado (C-02, C-03, C-08) y las demas al CPP
 * vigente sin cambiarlo (C-04, C-06), y publica el movimiento y sus alertas por cruce de umbral (A-02).
 */
abstract class Valuador {

    /** Todo lo que calcula un movimiento antes de escribirlo; preparar y confirmar usan el mismo calculo. */
    record Calculo(Articulo articulo, Saldo antes, Saldo despues, Movimiento movimiento, String resumen) {
    }

    final RepositorioArticulos articulos;
    final RepositorioSaldos saldos;
    final RepositorioMovimientos movimientos;
    final PublicadorEventos eventos;
    final Clock reloj;
    final SecuenciaDeIds ids;

    Valuador(RepositorioArticulos articulos, RepositorioSaldos saldos, RepositorioMovimientos movimientos,
         PublicadorEventos eventos, Clock reloj, SecuenciaDeIds ids) {
        this.articulos = Objects.requireNonNull(articulos, "articulos");
        this.saldos = Objects.requireNonNull(saldos, "saldos");
        this.movimientos = Objects.requireNonNull(movimientos, "movimientos");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.ids = Objects.requireNonNull(ids, "ids");
    }

    // --- Pasos compartidos por los cuatro comandos ---

    final Articulo articuloActivo(ContextoNegocio contexto, UUID articuloId) {
        return Operaciones.activo(articulos, contexto, articuloId);
    }

    final Saldo saldoDe(ContextoNegocio contexto, Articulo articulo) {
        return saldos.porArticulo(contexto.negocioId(), articulo.id())
                .orElseGet(() -> Saldo.inicial(articulo.id(), articulo.negocioId()));
    }

    final Instant ahora() {
        return reloj.instant();
    }

    /** Una cantidad positiva en una unidad, llevada a la base: valida signo, decimales, unidad y rango. */
    final Conversion convertir(Articulo articulo, BigDecimal cantidad, String unidad) {
        Operaciones.exigirCantidadCapturada(cantidad, false);
        var conversion = articulo.aBase(cantidad, unidad);
        exigirBaseValida(conversion.cantidadBase(), false);
        return conversion;
    }

    static void exigirBaseValida(BigDecimal cantidadBase, boolean permiteCero) {
        var sinSigno = cantidadBase.signum() < 0 || (!permiteCero && cantidadBase.signum() == 0);
        if (sinSigno || !Limites.cantidadEnRango(cantidadBase)) {
            throw new InventarioException(CodigoError.CANTIDAD_INVALIDA);
        }
    }

    final Instant fechaDelHecho(Instant pedida) {
        var ahora = reloj.instant();
        if (pedida != null && pedida.isAfter(ahora)) {
            throw new InventarioException(CodigoError.FECHA_FUTURA);
        }
        return pedida == null ? ahora : pedida;
    }

    /**
     * Valua un movimiento de {@code cantidadBase} (siempre positiva) unidades base. Con {@code costoBase} nulo se
     * usa el CPP vigente. En una entrada, {@code valorExacto} es lo pagado calculado desde lo capturado (A1); en
     * las demas es nulo y el valor sale de cantidad por costo. Valida que todo quepa en las columnas (A3).
     */
    final Calculo valuar(Articulo articulo, Saldo antes, TipoMovimiento tipo, CausaTipo causa, MotivoMerma motivo,
                         Conversion conversion, BigDecimal cantidadBase, BigDecimal costoBase,
                         BigDecimal valorExacto, Instant ocurridoEn, String actor, String cabecera) {
        return valuar(articulo, antes, tipo, causa, null, motivo, conversion, cantidadBase, costoBase, valorExacto,
                ocurridoEn, actor, cabecera);
    }

    /** Igual que la anterior, con el id del origen externo (por ejemplo la venta) en {@code causaId}. */
    final Calculo valuar(Articulo articulo, Saldo antes, TipoMovimiento tipo, CausaTipo causa, UUID causaId,
                         MotivoMerma motivo, Conversion conversion, BigDecimal cantidadBase, BigDecimal costoBase,
                         BigDecimal valorExacto, Instant ocurridoEn, String actor, String cabecera) {
        var ahora = reloj.instant();
        var costoUnitario = costoBase != null ? costoBase : antes.cpp();
        var id = ids.siguiente(ahora);
        var firmada = tipo.entra() ? cantidadBase : cantidadBase.negate();
        var costoTotal = valorExacto != null ? valorExacto : Redondeo.dinero(firmada.multiply(costoUnitario));
        var despues = valorExacto != null ? antes.conEntradaConValor(cantidadBase, valorExacto, id)
                : tipo.entra() ? antes.conEntrada(cantidadBase, costoUnitario, id)
                : antes.conSalida(cantidadBase, id);
        if (!Limites.cantidadEnRango(despues.cantidadBase())) {
            throw new InventarioException(CodigoError.CANTIDAD_INVALIDA);
        }
        if (!Limites.dineroEnRango(costoTotal) || !Limites.dineroEnRango(despues.valor())) {
            throw new InventarioException(CodigoError.COSTO_INVALIDO);
        }
        var movimiento = new Movimiento(id, articulo.negocioId(), articulo.id(), tipo,
                conversion.cantidadCapturada(), conversion.unidadCapturada(), conversion.factorAplicado(),
                Redondeo.cantidad(firmada), Redondeo.costoUnitario(costoUnitario), costoTotal, despues.cpp(),
                despues.cantidadBase(), causa, causaId, motivo, ocurridoEn, ahora, actor);
        var base = articulo.unidadBase().codigo();
        var resumen = cabecera + ". Valor: $" + costoTotal.abs().toPlainString() + ". Saldo: "
                + Operaciones.plano(antes.cantidadBase()) + " " + base + " → "
                + Operaciones.plano(despues.cantidadBase()) + " " + base + ". Costo promedio por " + base + ": "
                + antes.cpp().toPlainString() + " → " + despues.cpp().toPlainString() + ".";
        return new Calculo(articulo, antes, despues, movimiento, resumen);
    }

    /** "0.5 kg (500 g) de Harina": lo capturado, su equivalente en base si la unidad es otra, y el articulo. */
    static String descripcion(Articulo articulo, Conversion conversion) {
        var base = articulo.unidadBase().codigo();
        var equivalente = conversion.unidadCapturada().equals(base) ? ""
                : " (" + Operaciones.plano(conversion.cantidadBase()) + " " + base + ")";
        return Operaciones.plano(conversion.cantidadCapturada()) + " " + conversion.unidadCapturada()
                + equivalente + " de " + articulo.nombre();
    }

    /** Publica el movimiento y las alertas por cruce de umbral; devuelve las alertas que se publicaron. */
    final List<AlertaDeConsumo> publicar(Calculo calculo) {
        var avisos = new ArrayList<AlertaDeConsumo>();
        var m = calculo.movimiento();
        var articulo = calculo.articulo();
        var base = articulo.unidadBase().codigo();
        var registrado = new MovimientoRegistrado(m.id(), m.articuloId(), m.tipo().name(), m.cantidadBase(), base,
                m.costoUnitario(), m.costoTotal(), m.cppResultante(), m.saldoResultante(), m.causaTipo().name(),
                m.causaId());
        var sobre = FabricaSobres.de(m.negocioId(), m.ocurridoEn(), m.registradoEn(), m.id(), null,
                MovimientoRegistrado.TIPO, MovimientoRegistrado.VERSION, registrado.datos());
        eventos.publicar(sobre);
        var minimo = articulo.minimoBase();
        var antes = calculo.antes().cantidadBase();
        var despues = calculo.despues().cantidadBase();
        for (var alerta : CruceDeUmbral.entre(antes, despues, minimo)) {
            // Un minimo de cero es "sin minimo": solo avisa el saldo negativo (D-77).
            if (minimo.signum() == 0 && alerta != TipoAlerta.SALDO_NEGATIVO) {
                continue;
            }
            var tipo = switch (alerta) {
                case BAJO_MINIMO -> AlertaDeSaldo.BAJO_MINIMO;
                case REPUESTO -> AlertaDeSaldo.REPUESTO;
                case SALDO_NEGATIVO -> AlertaDeSaldo.SALDO_NEGATIVO;
            };
            var datos = new AlertaDeSaldo(tipo, articulo.id(), articulo.nombre(), despues, minimo, base).datos();
            eventos.publicar(FabricaSobres.de(m.negocioId(), m.registradoEn(), m.registradoEn(), m.id(),
                    sobre.id(), tipo, AlertaDeSaldo.VERSION, datos));
            avisos.add(new AlertaDeConsumo(articulo.id(), tipo));
        }
        return avisos;
    }
}
