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

    abstract static class Base<C extends Comando> extends Valuador implements ManejadorComando<C> {

        Base(RepositorioArticulos articulos, RepositorioSaldos saldos, RepositorioMovimientos movimientos,
             PublicadorEventos eventos, Clock reloj, SecuenciaDeIds ids) {
            super(articulos, saldos, movimientos, eventos, reloj, ids);
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
