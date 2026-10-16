package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.inventario.dominio.Articulo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.dominio.Rastreo;
import mx.negocio360.inventario.dominio.Saldo;
import mx.negocio360.inventario.dominio.UnidadEstandar;

/** Manejadores de los comandos de articulo: crear, editar, definir unidad propia y dar de baja. */
final class ManejadoresDeArticulo {

    private ManejadoresDeArticulo() {
    }

    /** CrearArticulo (U-01, U-03). El articulo nace con saldo cero. */
    static final class Crear implements ManejadorComando<CrearArticuloComando> {

        private final RepositorioArticulos articulos;
        private final RepositorioSaldos saldos;
        private final Clock reloj;

        Crear(RepositorioArticulos articulos, RepositorioSaldos saldos, Clock reloj) {
            this.articulos = Objects.requireNonNull(articulos, "articulos");
            this.saldos = Objects.requireNonNull(saldos, "saldos");
            this.reloj = Objects.requireNonNull(reloj, "reloj");
        }

        @Override
        public String tipo() {
            return "crear_articulo";
        }

        @Override
        public Class<CrearArticuloComando> claseComando() {
            return CrearArticuloComando.class;
        }

        @Override
        public String preparar(ContextoNegocio contexto, CrearArticuloComando comando) {
            return resumen(validar(contexto, comando));
        }

        private static String resumen(Articulo articulo) {
            var base = articulo.unidadBase().codigo();
            return "Crear el artículo " + articulo.nombre() + " con unidad base " + base + " y mínimo "
                    + Operaciones.plano(articulo.minimoBase()) + " " + base + ".";
        }

        @Override
        public ResultadoComando confirmar(ContextoNegocio contexto, CrearArticuloComando comando, String preparado) {
            var articulo = validar(contexto, comando);
            Operaciones.exigirMismoResumen(preparado, resumen(articulo));
            articulos.insertar(articulo);
            saldos.insertar(Saldo.inicial(articulo.id(), articulo.negocioId()));
            return ResultadoComando.ok(PlantillasInventario.ARTICULO_GUARDADO, articulo.id());
        }

        private Articulo validar(ContextoNegocio contexto, CrearArticuloComando comando) {
            var ahora = reloj.instant();
            var nombre = Articulo.nombreValido(comando.nombre());
            var categoria = Articulo.categoriaValida(comando.categoria());
            var base = UnidadEstandar.deCodigo(comando.unidadBase())
                    .orElseThrow(() -> new InventarioException(CodigoError.UNIDAD_NO_CONVERTIBLE));
            var propias = unidadesPropias(comando.unidadesPropias());
            var rastreo = comando.rastreo() == null ? Rastreo.NINGUNO : comando.rastreo();
            if (rastreo != Rastreo.NINGUNO) {
                throw new InventarioException(CodigoError.RASTREO_NO_SOPORTADO);
            }
            var id = UuidV7.generar(ahora);
            var sinMinimo = Articulo.nuevo(id, contexto.negocioId(), nombre, categoria, base, propias,
                    BigDecimal.ZERO, contexto.actor(), ahora, rastreo);
            var minimo = comando.minimo() == null ? BigDecimal.ZERO
                    : Operaciones.minimoEnBase(sinMinimo, comando.minimo(), comando.unidadMinimo());
            Operaciones.exigirNombreLibre(articulos, contexto.negocioId(), nombre, id);
            return Articulo.nuevo(id, contexto.negocioId(), nombre, categoria, base, propias, minimo,
                    contexto.actor(), ahora, rastreo);
        }

        private static Map<String, BigDecimal> unidadesPropias(List<UnidadPropiaPedida> pedidas) {
            var mapa = new LinkedHashMap<String, BigDecimal>();
            if (pedidas == null) {
                return mapa;
            }
            for (var pedida : pedidas) {
                if (pedida == null || pedida.nombre() == null) {
                    throw new InventarioException(CodigoError.UNIDAD_PROPIA_INVALIDA);
                }
                var clave = UnidadEstandar.normalizar(pedida.nombre());
                if (mapa.containsKey(clave)) {
                    throw new InventarioException(CodigoError.UNIDAD_PROPIA_INVALIDA);
                }
                mapa.put(clave, pedida.factor());
            }
            return mapa;
        }
    }

    /** EditarArticulo. No toca la unidad base (U-01). */
    static final class Editar implements ManejadorComando<EditarArticuloComando> {

        private final RepositorioArticulos articulos;
        private final Clock reloj;

        Editar(RepositorioArticulos articulos, Clock reloj) {
            this.articulos = Objects.requireNonNull(articulos, "articulos");
            this.reloj = Objects.requireNonNull(reloj, "reloj");
        }

        @Override
        public String tipo() {
            return "editar_articulo";
        }

        @Override
        public Class<EditarArticuloComando> claseComando() {
            return EditarArticuloComando.class;
        }

        @Override
        public String preparar(ContextoNegocio contexto, EditarArticuloComando comando) {
            var actual = Operaciones.activo(articulos, contexto, comando.articuloId());
            return resumen(actual, validar(contexto, comando, actual));
        }

        private static String resumen(Articulo actual, Articulo nuevo) {
            return "Editar el artículo " + actual.nombre() + ": nombre " + nuevo.nombre() + ", categoría "
                    + (nuevo.categoria() == null ? "ninguna" : nuevo.categoria()) + ", mínimo "
                    + Operaciones.plano(nuevo.minimoBase()) + " " + nuevo.unidadBase().codigo() + ".";
        }

        @Override
        public ResultadoComando confirmar(ContextoNegocio contexto, EditarArticuloComando comando, String preparado) {
            var actual = Operaciones.activo(articulos, contexto, comando.articuloId());
            var nuevo = validar(contexto, comando, actual);
            Operaciones.exigirMismoResumen(preparado, resumen(actual, nuevo));
            articulos.actualizar(nuevo, actual.version());
            return ResultadoComando.ok(PlantillasInventario.ARTICULO_ACTUALIZADO, nuevo.id());
        }

        private Articulo validar(ContextoNegocio contexto, EditarArticuloComando comando, Articulo actual) {
            var nombre = comando.nombre() == null ? actual.nombre() : Articulo.nombreValido(comando.nombre());
            var categoria = comando.categoria() == null ? actual.categoria()
                    : Articulo.categoriaValida(comando.categoria());
            var minimo = comando.minimo() == null ? actual.minimoBase()
                    : Operaciones.minimoEnBase(actual, comando.minimo(), comando.unidadMinimo());
            Operaciones.exigirNombreLibre(articulos, contexto.negocioId(), nombre, actual.id());
            return actual.conDatos(nombre, categoria, minimo, reloj.instant());
        }
    }

    /** DefinirUnidadPropia (U-03). */
    static final class DefinirUnidad implements ManejadorComando<DefinirUnidadPropiaComando> {

        private final RepositorioArticulos articulos;
        private final Clock reloj;

        DefinirUnidad(RepositorioArticulos articulos, Clock reloj) {
            this.articulos = Objects.requireNonNull(articulos, "articulos");
            this.reloj = Objects.requireNonNull(reloj, "reloj");
        }

        @Override
        public String tipo() {
            return "definir_unidad_propia";
        }

        @Override
        public Class<DefinirUnidadPropiaComando> claseComando() {
            return DefinirUnidadPropiaComando.class;
        }

        @Override
        public String preparar(ContextoNegocio contexto, DefinirUnidadPropiaComando comando) {
            var actual = Operaciones.activo(articulos, contexto, comando.articuloId());
            validar(comando, actual);
            return resumen(comando, actual);
        }

        private static String resumen(DefinirUnidadPropiaComando comando, Articulo actual) {
            return "Definir la unidad " + UnidadEstandar.normalizar(comando.nombre()) + " = "
                    + Operaciones.plano(comando.factor()) + " " + actual.unidadBase().codigo() + " para "
                    + actual.nombre() + ".";
        }

        @Override
        public ResultadoComando confirmar(ContextoNegocio contexto, DefinirUnidadPropiaComando comando,
                                          String preparado) {
            var actual = Operaciones.activo(articulos, contexto, comando.articuloId());
            var nuevo = validar(comando, actual);
            Operaciones.exigirMismoResumen(preparado, resumen(comando, actual));
            articulos.actualizar(nuevo, actual.version());
            return ResultadoComando.ok(PlantillasInventario.UNIDAD_DEFINIDA, nuevo.id());
        }

        private Articulo validar(DefinirUnidadPropiaComando comando, Articulo actual) {
            return actual.conUnidadPropia(comando.nombre(), comando.factor(), reloj.instant());
        }
    }

    /** BajaArticulo (A-04): baja logica, solo con saldo cero. */
    static final class Baja implements ManejadorComando<BajaArticuloComando> {

        private final RepositorioArticulos articulos;
        private final RepositorioSaldos saldos;
        private final Clock reloj;

        Baja(RepositorioArticulos articulos, RepositorioSaldos saldos, Clock reloj) {
            this.articulos = Objects.requireNonNull(articulos, "articulos");
            this.saldos = Objects.requireNonNull(saldos, "saldos");
            this.reloj = Objects.requireNonNull(reloj, "reloj");
        }

        @Override
        public String tipo() {
            return "baja_articulo";
        }

        @Override
        public Class<BajaArticuloComando> claseComando() {
            return BajaArticuloComando.class;
        }

        @Override
        public String preparar(ContextoNegocio contexto, BajaArticuloComando comando) {
            return resumen(validar(contexto, comando));
        }

        private static String resumen(Articulo articulo) {
            return "Dar de baja el artículo " + articulo.nombre() + ".";
        }

        @Override
        public ResultadoComando confirmar(ContextoNegocio contexto, BajaArticuloComando comando, String preparado) {
            var actual = validar(contexto, comando);
            Operaciones.exigirMismoResumen(preparado, resumen(actual));
            articulos.actualizar(actual.dadoDeBaja(reloj.instant()), actual.version());
            return ResultadoComando.ok(PlantillasInventario.ARTICULO_DADO_DE_BAJA, actual.id());
        }

        private Articulo validar(ContextoNegocio contexto, BajaArticuloComando comando) {
            var actual = Operaciones.activo(articulos, contexto, comando.articuloId());
            var saldo = saldos.porArticulo(contexto.negocioId(), actual.id())
                    .orElseGet(() -> Saldo.inicial(actual.id(), actual.negocioId()));
            if (saldo.cantidadBase().signum() != 0) {
                throw new InventarioException(CodigoError.BAJA_CON_SALDO, actual.id());
            }
            return actual;
        }
    }
}
