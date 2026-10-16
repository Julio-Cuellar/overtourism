package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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
 * Manejadores de los comandos de movimiento: entrada, salida, merma y ajuste por conteo. Todos valuan igual: la
 * entrada al costo capturado (C-02, C-03, C-08) y las demas al CPP vigente sin cambiarlo (C-04, C-06).
 */
final class ManejadoresDeMovimiento {

    private ManejadoresDeMovimiento() {
    }

    /** Todo lo que calcula un movimiento antes de escribirlo; preparar y confirmar usan el mismo calculo. */
    record Calculo(Articulo articulo, Saldo antes, Saldo despues, Movimiento movimiento, String resumen) {
    }

    abstract static class Base<C extends Comando> implements ManejadorComando<C> {

        private final RepositorioArticulos articulos;
        private final RepositorioSaldos saldos;
        private final RepositorioMovimientos movimientos;
        private final PublicadorEventos eventos;
        private final Clock reloj;
        private final SecuenciaDeIds ids;

        Base(RepositorioArticulos articulos, RepositorioSaldos saldos, RepositorioMovimientos movimientos,
             PublicadorEventos eventos, Clock reloj, SecuenciaDeIds ids) {
            this.articulos = Objects.requireNonNull(articulos, "articulos");
            this.saldos = Objects.requireNonNull(saldos, "saldos");
            this.movimientos = Objects.requireNonNull(movimientos, "movimientos");
            this.eventos = Objects.requireNonNull(eventos, "eventos");
            this.reloj = Objects.requireNonNull(reloj, "reloj");
            this.ids = Objects.requireNonNull(ids, "ids");
        }

        abstract Calculo calcular(ContextoNegocio contexto, C comando);

        @Override
        public final String preparar(ContextoNegocio contexto, C comando) {
            return calcular(contexto, comando).resumen();
        }

        @Override
        public final ResultadoComando confirmar(ContextoNegocio contexto, C comando, String preparado) {
            // Un solo calculo: el saldo que se compara con el resumen es el mismo que respalda la escritura (M2).
            var calculo = calcular(contexto, comando);
            Operaciones.exigirMismoResumen(preparado, calculo.resumen());
            var movimiento = calculo.movimiento();
            // Primero el saldo: si otra peticion lo movio, falla con CONCURRENCIA sin dejar el movimiento a medias.
            saldos.actualizar(calculo.despues(), calculo.antes().version());
            movimientos.insertar(movimiento);
            publicar(calculo);
            return ResultadoComando.ok(PlantillasInventario.MOVIMIENTO_REGISTRADO, movimiento.id());
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
                    despues.cantidadBase(), causa, null, motivo, ocurridoEn, ahora, actor);
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

        private void publicar(Calculo calculo) {
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
            }
        }
    }

    /** RegistrarEntrada: suma al saldo y promedia el costo. El costo se captura por unidad capturada (C-08). */
    static final class Entrada extends Base<RegistrarEntradaComando> {

        Entrada(RepositorioArticulos articulos, RepositorioSaldos saldos, RepositorioMovimientos movimientos,
                PublicadorEventos eventos, Clock reloj, SecuenciaDeIds ids) {
            super(articulos, saldos, movimientos, eventos, reloj, ids);
        }

        @Override
        public String tipo() {
            return "registrar_entrada";
        }

        @Override
        public Class<RegistrarEntradaComando> claseComando() {
            return RegistrarEntradaComando.class;
        }

        @Override
        Calculo calcular(ContextoNegocio contexto, RegistrarEntradaComando comando) {
            var articulo = articuloActivo(contexto, comando.articuloId());
            var ocurridoEn = fechaDelHecho(comando.ocurridoEn());
            var conversion = convertir(articulo, comando.cantidad(), comando.unidad());
            var costoBase = costoPorUnidadBase(comando.costoUnitario(), conversion);
            // Lo pagado se calcula de lo capturado (25 kg x $18.55), no del costo por gramo ya redondeado (A1).
            var pagado = Redondeo.dinero(conversion.cantidadCapturada().multiply(comando.costoUnitario()));
            return valuar(articulo, saldoDe(contexto, articulo), TipoMovimiento.ENTRADA, CausaTipo.COMPRA, null,
                    conversion, conversion.cantidadBase(), costoBase, pagado, ocurridoEn, contexto.actor(),
                    "Registrar entrada de " + descripcion(articulo, conversion));
        }

        private static BigDecimal costoPorUnidadBase(BigDecimal costo, Conversion conversion) {
            if (costo == null || !Limites.cantidadEnRango(costo) || costo.signum() < 0
                    || costo.stripTrailingZeros().scale() > Limites.MAX_DECIMALES_CAPTURA) {
                throw new InventarioException(CodigoError.COSTO_INVALIDO);
            }
            var porBase = Redondeo.dividir(costo, conversion.factorAplicado(), Redondeo.ESCALA_COSTO);
            if (!Limites.cantidadEnRango(porBase)) {
                throw new InventarioException(CodigoError.COSTO_INVALIDO);
            }
            return porBase;
        }
    }

    /** RegistrarSalida: resta al CPP vigente; puede dejar el saldo en negativo (A-01). */
    static final class Salida extends Base<RegistrarSalidaComando> {

        Salida(RepositorioArticulos articulos, RepositorioSaldos saldos, RepositorioMovimientos movimientos,
               PublicadorEventos eventos, Clock reloj, SecuenciaDeIds ids) {
            super(articulos, saldos, movimientos, eventos, reloj, ids);
        }

        @Override
        public String tipo() {
            return "registrar_salida";
        }

        @Override
        public Class<RegistrarSalidaComando> claseComando() {
            return RegistrarSalidaComando.class;
        }

        @Override
        Calculo calcular(ContextoNegocio contexto, RegistrarSalidaComando comando) {
            var articulo = articuloActivo(contexto, comando.articuloId());
            var conversion = convertir(articulo, comando.cantidad(), comando.unidad());
            return valuar(articulo, saldoDe(contexto, articulo), TipoMovimiento.SALIDA, CausaTipo.MANUAL, null,
                    conversion, conversion.cantidadBase(), null, null, ahora(), contexto.actor(),
                    "Registrar salida de " + descripcion(articulo, conversion));
        }
    }

    /** RegistrarMerma: como una salida, pero con motivo. */
    static final class Merma extends Base<RegistrarMermaComando> {

        Merma(RepositorioArticulos articulos, RepositorioSaldos saldos, RepositorioMovimientos movimientos,
              PublicadorEventos eventos, Clock reloj, SecuenciaDeIds ids) {
            super(articulos, saldos, movimientos, eventos, reloj, ids);
        }

        @Override
        public String tipo() {
            return "registrar_merma";
        }

        @Override
        public Class<RegistrarMermaComando> claseComando() {
            return RegistrarMermaComando.class;
        }

        @Override
        Calculo calcular(ContextoNegocio contexto, RegistrarMermaComando comando) {
            var articulo = articuloActivo(contexto, comando.articuloId());
            if (comando.motivo() == null) {
                throw new InventarioException(CodigoError.MOTIVO_REQUERIDO);
            }
            var conversion = convertir(articulo, comando.cantidad(), comando.unidad());
            return valuar(articulo, saldoDe(contexto, articulo), TipoMovimiento.MERMA, CausaTipo.MERMA,
                    comando.motivo(), conversion, conversion.cantidadBase(), null, null, ahora(), contexto.actor(),
                    "Registrar merma (" + comando.motivo().name().toLowerCase(java.util.Locale.ROOT) + ") de "
                            + descripcion(articulo, conversion));
        }
    }

    /** AjustePorConteo (A-03, C-06): la persona cuenta y el sistema calcula la diferencia contra el saldo. */
    static final class Conteo extends Base<AjustePorConteoComando> {

        Conteo(RepositorioArticulos articulos, RepositorioSaldos saldos, RepositorioMovimientos movimientos,
               PublicadorEventos eventos, Clock reloj, SecuenciaDeIds ids) {
            super(articulos, saldos, movimientos, eventos, reloj, ids);
        }

        @Override
        public String tipo() {
            return "ajuste_por_conteo";
        }

        @Override
        public Class<AjustePorConteoComando> claseComando() {
            return AjustePorConteoComando.class;
        }

        @Override
        Calculo calcular(ContextoNegocio contexto, AjustePorConteoComando comando) {
            var articulo = articuloActivo(contexto, comando.articuloId());
            Operaciones.exigirCantidadCapturada(comando.cantidadContada(), true);
            var contado = articulo.aBase(comando.cantidadContada(), comando.unidad());
            exigirBaseValida(contado.cantidadBase(), true);
            var antes = saldoDe(contexto, articulo);
            var diferencia = contado.cantidadBase().subtract(antes.cantidadBase());
            if (diferencia.signum() == 0) {
                throw new InventarioException(CodigoError.CONTEO_SIN_DIFERENCIA, articulo.id());
            }
            var tipo = diferencia.signum() > 0 ? TipoMovimiento.AJUSTE_POS : TipoMovimiento.AJUSTE_NEG;
            var base = articulo.unidadBase().codigo();
            var cabecera = "Ajustar por conteo: contado " + descripcion(articulo, contado) + ", diferencia "
                    + Operaciones.plano(diferencia) + " " + base;
            return valuar(articulo, antes, tipo, CausaTipo.CONTEO, null, contado, diferencia.abs(), null, null,
                    ahora(), contexto.actor(), cabecera);
        }
    }
}
