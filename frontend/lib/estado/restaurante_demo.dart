import 'dart:math';

import 'package:flutter/foundation.dart';

import '../datos/demo_restaurantes.dart';
import '../modelos/permisos_restaurante.dart';
import '../modelos/restaurantes.dart';

/// Error de una regla de negocio con el código del contrato y su texto en español de México (SR-07). La app muestra
/// `mensaje`; nunca improvisa.
class ReglaRestaurante implements Exception {
  const ReglaRestaurante(this.codigo, [String? mensaje]) : _mensaje = mensaje;

  final String codigo;
  final String? _mensaje;

  String get mensaje => _mensaje ?? plantillasDeError[codigo] ?? 'No se pudo completar la operación.';

  @override
  String toString() => '$codigo: $mensaje';
}

/// Plantillas de los códigos de error (sección 7 del diseño).
const plantillasDeError = <String, String>{
  'PRECIO_INVALIDO': 'El precio debe ser mayor que cero y de hasta 99,999.99.',
  'NOMBRE_INVALIDO': 'El nombre debe tener entre 2 y 80 caracteres.',
  'NOMBRE_DUPLICADO': 'Ya existe un platillo activo con ese nombre.',
  'RECETA_REQUERIDA': 'Este platillo descuenta por receta, pero no tiene una receta activa.',
  'PLATILLO_AGOTADO': 'Ese platillo está agotado.',
  'FUERA_DE_TEMPORADA': 'Ese platillo está fuera de temporada.',
  'PLATILLO_NO_ORDENABLE': 'Ese platillo ya no se puede pedir.',
  'PLATILLO_EN_CUENTA_ABIERTA': 'No se puede dar de baja: el platillo está en una cuenta abierta.',
  'ETIQUETA_INVALIDA': 'Cada platillo admite hasta 8 etiquetas.',
  'LIMITE_EXCEDIDO': 'Se alcanzó el límite permitido.',
  'INSUMO_REPETIDO': 'Un insumo no puede repetirse en la misma receta.',
  'UNIDAD_NO_CONVERTIBLE': 'La cantidad de un insumo debe ser mayor que cero.',
  'ARTICULO_INACTIVO': 'Ese artículo de inventario está dado de baja.',
  'MESA_OCUPADA': 'La mesa ya tiene una cuenta o está bloqueada.',
  'SERVICIO_CERRADO': 'El servicio está cerrado.',
  'CUENTA_NO_ABIERTA': 'La cuenta ya no admite cambios.',
  'SIN_LINEAS_PENDIENTES': 'No hay líneas por enviar a cocina.',
  'LINEAS_SIN_ENVIAR': 'Envía a cocina todas las líneas antes de pedir la cuenta.',
  'MOTIVO_REQUERIDO': 'Indica el motivo.',
  'CUENTA_SOLICITADA': 'La cuenta ya fue solicitada. Pide al mesero.',
  'QR_INVALIDO': 'Este código QR no es válido o ya no está vigente.',
  'QR_PAUSADO': 'Los pedidos desde esta mesa están en pausa. Pide al mesero.',
  'QR_DESHABILITADO': 'Este restaurante no recibe pedidos desde el QR por ahora.',
  'LIMITE_QR_EXCEDIDO': 'El pedido supera el límite permitido. Pide al mesero.',
  'TASA_EXCEDIDA': 'Estás pidiendo demasiado rápido. Espera un momento.',
  'TRANSICION_INVALIDA': 'Ese cambio de estado no está permitido.',
  'PERMISO_DENEGADO': 'Tu puesto no tiene permiso para hacer esto.',
  'COMANDO_CAMBIO': 'Algo cambió desde que lo revisaste. Vuelve a revisar y confirma.',
};

/// Aviso para meseros y caja (QR-07, QR-13).
class AvisoSalon {
  AvisoSalon({required this.texto, required this.hora, required this.mesaEtiqueta, this.atendido = false});

  final String texto;
  final DateTime hora;
  final String mesaEtiqueta;
  bool atendido;
}

/// Borrador de receta: resultado de «preparar» (RC-08). No se guarda hasta «confirmar».
class BorradorReceta {
  const BorradorReceta({
    required this.platilloId,
    required this.rendimiento,
    required this.insumos,
    required this.costo,
    required this.precio,
    required this.avisos,
  });

  final String platilloId;
  final int rendimiento;
  final List<InsumoReceta> insumos;
  final CostoReceta costo;
  final int precio;
  final List<String> avisos;
}

enum EstadoCuentaQr { sinCuenta, abierta, solicitada }

/// Lo único que ve quien escanea el QR de una mesa, sin sesión (QR-03).
class VistaMesaQr {
  const VistaMesaQr({
    required this.restaurante,
    required this.mesa,
    required this.servicioAbierto,
    required this.aceptaPedidos,
    required this.estadoCuenta,
    this.motivoNoAcepta,
  });

  final String restaurante;
  final String mesa;
  final bool servicioAbierto;
  final bool aceptaPedidos;
  final EstadoCuentaQr estadoCuenta;

  /// Código por el que no se aceptan pedidos, si es el caso.
  final String? motivoNoAcepta;
}

class LineaPedidoQr {
  const LineaPedidoQr({required this.platilloId, required this.cantidad, this.nota, this.marcaAlergia = false});

  final String platilloId;
  final int cantidad;
  final String? nota;
  final bool marcaAlergia;
}

enum EstadoVistaLinea {
  enCocina('En cocina'),
  preparando('Preparando'),
  listo('Listo'),
  entregado('Entregado'),
  cancelado('Cancelado');

  const EstadoVistaLinea(this.etiqueta);

  final String etiqueta;
}

class LineaVistaQr {
  const LineaVistaQr({required this.nombre, required this.cantidad, required this.estado, this.mensaje});

  final String nombre;
  final int cantidad;
  final EstadoVistaLinea estado;
  final String? mensaje;
}

class PedidoQrVista {
  const PedidoQrVista({required this.lineas, required this.cuenta});

  final List<LineaVistaQr> lineas;
  final EstadoCuentaQr cuenta;
}

class ResultadoPedidoQr {
  const ResultadoPedidoQr({required this.abrioCuenta, required this.lineas});

  final bool abrioCuenta;
  final int lineas;
}

class InsumoDescontado {
  const InsumoDescontado({required this.nombre, required this.cantidad, required this.unidad});

  final String nombre;

  /// Diezmilésimas de la unidad base.
  final int cantidad;
  final String unidad;
}

class ResultadoCobro {
  const ResultadoCobro({required this.venta, required this.descontados, this.consumoFallido});

  final VentaRestaurante venta;
  final List<InsumoDescontado> descontados;
  final ConsumoFallido? consumoFallido;
}

/// Un platillo con lo vendido en el día, para el panel.
class PlatilloVendido {
  const PlatilloVendido({required this.platillo, required this.cantidad, required this.importe});

  final Platillo platillo;
  final int cantidad;
  final int importe;
}

/// Restaurante SIMULADO en la memoria del dispositivo: menú, recetas, salón, cuentas, cocina y pedidos por QR. Aplica
/// las mismas reglas que el diseño (`restaurantes.md`, MN, RC, CM, MS, CT, CD y QR) para que la maqueta se comporte
/// como el servidor; con el servidor conectado, este estado lo darán las consultas y comandos GraphQL de Restaurantes.
/// El rol activo hace de sesión: cada operación exige su permiso y falla con `PERMISO_DENEGADO`.
class RestauranteDemo extends ChangeNotifier {
  RestauranteDemo({DateTime Function()? reloj, Random? azar})
      : _reloj = reloj ?? DateTime.now,
        _azar = azar ?? Random(2026) {
    _sembrar();
  }

  final DateTime Function() _reloj;
  final Random _azar;

  /// Umbral para resaltar una comanda lenta, en minutos (CD-04; regla versionada, 15 por omisión).
  static const umbralPreparacionMin = 15;

  /// Límites del pedido por QR (QR-15 y QR-16), reglas versionadas en el servidor.
  static const maxLineasPorPedidoQr = 20;
  static const maxCantidadPorLineaQr = 10;
  static const maxLineasPorCuentaQr = 60;
  static const maxMontoPorCuentaQr = 300000;
  static const maxPedidosQrPorMinuto = 5;

  /// Identificador de dispositivo del comensal en esta maqueta (QR-12).
  static const dispositivoDemo = 'dispositivo-demo';

  final String nombre = nombreRestauranteDemo;
  RolRestaurante _rol = RolRestaurante.dueno;
  ModoServicio _modoServicio = ModoServicio.ambos;
  bool _servicioAbierto = true;
  bool _qrHabilitado = true;
  bool _visible = true;

  List<CategoriaMenu> _categorias = [];
  List<Platillo> _platillos = [];
  List<Articulo> _articulos = [];
  final Map<String, List<VersionReceta>> _recetas = {};
  List<Zona> _zonas = [];
  List<Mesa> _mesas = [];
  final List<Cuenta> _cuentas = [];
  final List<Comanda> _comandas = [];
  final List<VentaRestaurante> _ventas = [];
  final List<ConsumoFallido> _consumosFallidos = [];
  final List<AvisoSalon> _avisos = [];
  final Map<String, List<DateTime>> _pedidosQrRecientes = {};
  final Map<String, List<DateTime>> _bromasPorMesa = {};
  final Map<String, ResultadoPedidoQr> _llavesIdempotentes = {};

  int _siguienteCuenta = 1044;
  int _siguienteFolio = 2300;
  int _siguienteComanda = 1;
  int _contadorId = 0;

  // ---------------------------------------------------------------- sesión

  RolRestaurante get rol => _rol;

  set rol(RolRestaurante nuevo) {
    if (nuevo == _rol) {
      return;
    }
    _rol = nuevo;
    notifyListeners();
  }

  bool puede(Permiso permiso) => _rol.puede(permiso);

  void _exigir(Permiso permiso) {
    if (!puede(permiso)) {
      throw const ReglaRestaurante('PERMISO_DENEGADO');
    }
  }

  DateTime get ahora => _reloj();

  String _id(String prefijo) => '$prefijo-${++_contadorId}';

  // --------------------------------------------------------------- lecturas

  ModoServicio get modoServicio => _modoServicio;

  bool get servicioAbierto => _servicioAbierto;

  bool get qrHabilitado => _qrHabilitado;

  bool get visible => _visible;

  List<CategoriaMenu> get categorias => List.unmodifiable(_categorias);

  List<Platillo> get platillos => List.unmodifiable(_platillos.where((p) => p.activo));

  List<Articulo> get articulos => List.unmodifiable(_articulos);

  List<Zona> get zonas => List.unmodifiable(_zonas);

  List<Mesa> get mesas => List.unmodifiable(_mesas);

  List<Cuenta> get cuentas => List.unmodifiable(_cuentas);

  List<Comanda> get comandas => List.unmodifiable(_comandas);

  List<VentaRestaurante> get ventas => List.unmodifiable(_ventas);

  List<ConsumoFallido> get consumosFallidos => List.unmodifiable(_consumosFallidos);

  List<AvisoSalon> get avisos => List.unmodifiable(_avisos);

  int get avisosPendientes => _avisos.where((a) => !a.atendido).length;

  Platillo? platillo(String id) => _platillos.where((p) => p.id == id).firstOrNull;

  CategoriaMenu? categoria(String? id) => _categorias.where((c) => c.id == id).firstOrNull;

  Articulo? articulo(String id) => _articulos.where((a) => a.id == id).firstOrNull;

  Mesa? mesa(String id) => _mesas.where((m) => m.id == id).firstOrNull;

  Cuenta? cuenta(String id) => _cuentas.where((c) => c.id == id).firstOrNull;

  Comanda? comanda(String id) => _comandas.where((c) => c.id == id).firstOrNull;

  Estacion estacionDe(Platillo platillo) => categoria(platillo.categoriaId)?.estacion ?? Estacion.cocina;

  /// Cuenta no pagada de la mesa, si la hay (MS-03: a lo más una).
  Cuenta? cuentaActivaDe(String mesaId) =>
      _cuentas.where((c) => c.mesaId == mesaId && c.estado.noPagada).firstOrNull;

  EstadoMesa estadoDe(Mesa mesa) {
    if (mesa.bloqueada) {
      return EstadoMesa.bloqueada;
    }
    final cuenta = cuentaActivaDe(mesa.id);
    if (cuenta == null) {
      return EstadoMesa.libre;
    }
    return cuenta.estado == EstadoCuenta.porCobrar ? EstadoMesa.porCobrar : EstadoMesa.ocupada;
  }

  /// Lo que se publica a Negocios y se ve en la ficha del turista (MS-04).
  ({int libres, int total, bool abierto}) get disponibilidad {
    final activas = _mesas.where((m) => !m.bloqueada).toList();
    final libres = activas.where((m) => cuentaActivaDe(m.id) == null).length;
    return (libres: libres, total: activas.length, abierto: _servicioAbierto);
  }

  List<Cuenta> get cuentasPorAtender =>
      _cuentas.where((c) => c.estado.noPagada).toList()..sort((a, b) => a.numero.compareTo(b.numero));

  /// Motivo por el que un platillo no se puede pedir hoy, o nulo si se puede (MN-05, MN-06, MN-14).
  String? motivoNoOrdenable(Platillo platillo) {
    if (!platillo.activo) {
      return 'PLATILLO_NO_ORDENABLE';
    }
    final temporada = platillo.temporada;
    if (temporada != null && !temporada.contiene(ahora)) {
      return 'FUERA_DE_TEMPORADA';
    }
    if (platillo.agotado) {
      return 'PLATILLO_AGOTADO';
    }
    return null;
  }

  /// Menú que ve el turista (PB-01 a PB-03): solo activos y publicados, sin costos ni recetas. Nulo si el negocio no
  /// es visible. Fuera de temporada no sale; agotado sale marcado.
  List<Platillo>? menuPublico() {
    if (!_visible) {
      return null;
    }
    return _platillos.where((p) => p.activo && p.publicado && (p.temporada?.contiene(ahora) ?? true)).toList();
  }

  // ----------------------------------------------------------- ventas y panel

  int get totalVendidoHoy => _ventas.fold(0, (suma, v) => suma + v.total);

  int get ticketPromedio => _ventas.isEmpty ? 0 : (totalVendidoHoy * 2 + _ventas.length) ~/ (_ventas.length * 2);

  List<PlatilloVendido> platillosMasVendidos({int limite = 5}) {
    final cantidades = <String, int>{};
    for (final venta in _ventas) {
      venta.lineas.forEach((id, cantidad) => cantidades[id] = (cantidades[id] ?? 0) + cantidad);
    }
    final lista = [
      for (final entrada in cantidades.entries)
        if (platillo(entrada.key) case final p?)
          PlatilloVendido(platillo: p, cantidad: entrada.value, importe: entrada.value * p.precio),
    ]..sort((a, b) => b.cantidad.compareTo(a.cantidad));
    return lista.take(limite).toList();
  }

  // ------------------------------------------------------------------- menú

  void marcarAgotado(String platilloId, bool agotado) {
    _exigir(Permiso.menuAgotar);
    _reemplazar(platilloId, (p) => p.copiar(agotado: agotado));
  }

  void _reemplazar(String id, Platillo Function(Platillo) cambio) {
    final indice = _platillos.indexWhere((p) => p.id == id);
    if (indice < 0) {
      throw const ReglaRestaurante('PLATILLO_NO_ORDENABLE');
    }
    _platillos[indice] = cambio(_platillos[indice]);
    notifyListeners();
  }

  void _validarPlatillo({
    required String nombre,
    required int precio,
    required Set<EtiquetaDieta> etiquetas,
    String? idActual,
  }) {
    final limpio = nombre.trim();
    if (limpio.length < 2 || limpio.length > 80) {
      throw const ReglaRestaurante('NOMBRE_INVALIDO');
    }
    if (precio <= 0 || precio > 9999999) {
      throw const ReglaRestaurante('PRECIO_INVALIDO');
    }
    if (etiquetas.length > 8) {
      throw const ReglaRestaurante('ETIQUETA_INVALIDA');
    }
    final repetido = _platillos.any((p) => p.activo && p.id != idActual && p.nombre.toLowerCase() == limpio.toLowerCase());
    if (repetido) {
      throw const ReglaRestaurante('NOMBRE_DUPLICADO');
    }
  }

  Platillo crearPlatillo({
    required String nombre,
    required String descripcion,
    required int precio,
    String? categoriaId,
    Set<EtiquetaDieta> etiquetas = const {},
    Temporada? temporada,
    bool publicado = true,
  }) {
    _exigir(Permiso.menuEditar);
    _validarPlatillo(nombre: nombre, precio: precio, etiquetas: etiquetas);
    if (_platillos.where((p) => p.activo).length >= 200) {
      throw const ReglaRestaurante('LIMITE_EXCEDIDO');
    }
    final nuevo = Platillo(
      id: _id('p'),
      nombre: nombre.trim(),
      descripcion: descripcion.trim(),
      precio: precio,
      categoriaId: categoriaId,
      etiquetas: etiquetas,
      temporada: temporada,
      publicado: publicado,
    );
    _platillos.add(nuevo);
    notifyListeners();
    return nuevo;
  }

  void editarPlatillo(
    String id, {
    required String nombre,
    required String descripcion,
    required int precio,
    required String? categoriaId,
    required Set<EtiquetaDieta> etiquetas,
    required Temporada? temporada,
    required bool publicado,
    ModoInventario? modoInventario,
  }) {
    _exigir(Permiso.menuEditar);
    _validarPlatillo(nombre: nombre, precio: precio, etiquetas: etiquetas, idActual: id);
    final modo = modoInventario ?? platillo(id)?.modoInventario ?? ModoInventario.ninguno;
    if (modo == ModoInventario.receta && recetaVigente(id) == null) {
      throw const ReglaRestaurante('RECETA_REQUERIDA');
    }
    _reemplazar(
      id,
      (p) => p.copiar(
        nombre: nombre.trim(),
        descripcion: descripcion.trim(),
        precio: precio,
        categoriaId: categoriaId,
        etiquetas: etiquetas,
        temporada: temporada,
        publicado: publicado,
        modoInventario: modo,
      ),
    );
  }

  /// Baja lógica (MN-10). Falla si el platillo está en una cuenta abierta o por cobrar.
  void darDeBajaPlatillo(String id) {
    _exigir(Permiso.menuEditar);
    final enCuenta = _cuentas.any((c) => c.estado.noPagada && c.lineasActivas.any((l) => l.platilloId == id));
    if (enCuenta) {
      throw const ReglaRestaurante('PLATILLO_EN_CUENTA_ABIERTA');
    }
    _reemplazar(id, (p) => p.copiar(activo: false));
  }

  // ---------------------------------------------------------------- recetas

  List<VersionReceta> versionesDe(String platilloId) => List.unmodifiable(_recetas[platilloId] ?? const []);

  VersionReceta? recetaVigente(String platilloId) =>
      (_recetas[platilloId] ?? const []).where((v) => v.vigente).firstOrNull;

  /// División entera con redondeo HALF_UP (D-20), también para negativos.
  static int dividirRedondeado(int numerador, int denominador) {
    final signo = (numerador < 0) != (denominador < 0) ? -1 : 1;
    final n = numerador.abs();
    final d = denominador.abs();
    return signo * ((n * 2 + d) ~/ (d * 2));
  }

  /// Costo teórico y margen de una versión (RC-05, RC-06). Todo en enteros: nunca `double`.
  CostoReceta costoDe(VersionReceta version, int precio) {
    var numerador = 0;
    final sinCosto = <String>[];
    for (final insumo in version.insumos) {
      final articulo = this.articulo(insumo.articuloId);
      if (articulo == null || articulo.cpp == null) {
        sinCosto.add(articulo?.nombre ?? insumo.articuloId);
        continue;
      }
      numerador += insumo.cantidad * (100 + insumo.mermaPct) * articulo.cpp!;
    }
    if (sinCosto.isNotEmpty) {
      return CostoReceta(costo: null, sinCosto: sinCosto);
    }
    final costo = dividirRedondeado(numerador, 1000 * version.rendimiento);
    final margen = precio - costo;
    return CostoReceta(
      costo: costo,
      sinCosto: const [],
      margen: margen,
      margenPuntosBase: dividirRedondeado(margen * 10000, precio),
    );
  }

  /// Preparar → confirmar (RC-08): valida (RC-02 a RC-04), calcula y muestra el resumen sin guardar.
  BorradorReceta prepararReceta(String platilloId, {required int rendimiento, required List<InsumoReceta> insumos}) {
    _exigir(Permiso.recetaEditar);
    final plato = platillo(platilloId);
    if (plato == null) {
      throw const ReglaRestaurante('PLATILLO_NO_ORDENABLE');
    }
    if (insumos.isEmpty || insumos.length > 30) {
      throw const ReglaRestaurante('LIMITE_EXCEDIDO', 'Una receta lleva de 1 a 30 insumos.');
    }
    if (rendimiento < 1 || rendimiento > 500) {
      throw const ReglaRestaurante('LIMITE_EXCEDIDO', 'El rendimiento va de 1 a 500 porciones.');
    }
    final vistos = <String>{};
    for (final insumo in insumos) {
      if (!vistos.add(insumo.articuloId)) {
        throw const ReglaRestaurante('INSUMO_REPETIDO');
      }
      if (insumo.cantidad <= 0) {
        throw const ReglaRestaurante('UNIDAD_NO_CONVERTIBLE');
      }
      if (insumo.mermaPct < 0 || insumo.mermaPct > 100) {
        throw const ReglaRestaurante('LIMITE_EXCEDIDO', 'La merma va de 0 a 100 %.');
      }
      final articulo = this.articulo(insumo.articuloId);
      if (articulo == null || !articulo.activo) {
        throw const ReglaRestaurante('ARTICULO_INACTIVO');
      }
    }
    final borrador = VersionReceta(
        numero: 0, rendimiento: rendimiento, insumos: insumos, vigenteDesde: ahora);
    final costo = costoDe(borrador, plato.precio);
    final avisos = <String>[
      if (costo.incompleto) 'Costo incompleto: falta el costo de ${costo.sinCosto.join(', ')}. No se estima.',
      if (!costo.incompleto && costo.margen! < 0) 'El costo teórico supera el precio del platillo.',
      if (plato.modoInventario != ModoInventario.receta)
        'El platillo no descuenta por receta. Cambia su modo de inventario para que descuente al vender.',
    ];
    return BorradorReceta(
      platilloId: platilloId,
      rendimiento: rendimiento,
      insumos: insumos,
      costo: costo,
      precio: plato.precio,
      avisos: avisos,
    );
  }

  /// Confirma el borrador: vuelve a validar y, si cambió un precio o un costo, falla con `COMANDO_CAMBIO` (RC-08).
  VersionReceta confirmarReceta(BorradorReceta borrador) {
    _exigir(Permiso.recetaEditar);
    final plato = platillo(borrador.platilloId);
    final actual = prepararReceta(borrador.platilloId, rendimiento: borrador.rendimiento, insumos: borrador.insumos);
    if (plato == null || plato.precio != borrador.precio || actual.costo.costo != borrador.costo.costo) {
      throw const ReglaRestaurante('COMANDO_CAMBIO');
    }
    final versiones = [...(_recetas[borrador.platilloId] ?? const <VersionReceta>[])];
    final ahoraMismo = ahora;
    for (var i = 0; i < versiones.length; i++) {
      if (versiones[i].vigente) {
        versiones[i] = versiones[i].cerrada(ahoraMismo);
      }
    }
    final nueva = VersionReceta(
      numero: versiones.length + 1,
      rendimiento: borrador.rendimiento,
      insumos: borrador.insumos,
      vigenteDesde: ahoraMismo,
    );
    versiones.add(nueva);
    _recetas[borrador.platilloId] = versiones;
    notifyListeners();
    return nueva;
  }

  /// Cambia el modo de inventario; `RECETA` exige una receta activa (MN-04).
  void cambiarModoDeInventario(String platilloId, ModoInventario modo) {
    _exigir(Permiso.menuEditar);
    if (modo == ModoInventario.receta && recetaVigente(platilloId) == null) {
      throw const ReglaRestaurante('RECETA_REQUERIDA');
    }
    _reemplazar(platilloId, (p) => p.copiar(modoInventario: modo));
  }

  // ----------------------------------------------------------------- salón

  void abrirServicio() {
    _exigir(Permiso.servicioOperar);
    _servicioAbierto = true;
    notifyListeners();
  }

  /// Cerrar con cuentas abiertas exige permiso de gerente y motivo (MS-05).
  void cerrarServicio({String? motivo}) {
    _exigir(Permiso.servicioOperar);
    if (_cuentas.any((c) => c.estado.noPagada)) {
      _exigir(Permiso.salonConfigurar);
      if (motivo == null || motivo.trim().isEmpty) {
        throw const ReglaRestaurante('MOTIVO_REQUERIDO');
      }
    }
    _servicioAbierto = false;
    notifyListeners();
  }

  /// Pausa o reanuda la publicación del restaurante para el turista (PB-01: sin publicar, el menú público es nulo).
  void cambiarVisibilidad(bool visible) {
    _exigir(Permiso.salonConfigurar);
    _visible = visible;
    notifyListeners();
  }

  void cambiarModoServicio(ModoServicio modo) {
    _exigir(Permiso.salonConfigurar);
    _modoServicio = modo;
    notifyListeners();
  }

  void bloquearMesa(String mesaId, bool bloquear) {
    _exigir(Permiso.salonConfigurar);
    final mesa = _mesaOFalla(mesaId);
    if (bloquear && cuentaActivaDe(mesaId) != null) {
      throw const ReglaRestaurante('MESA_OCUPADA');
    }
    mesa.bloqueada = bloquear;
    notifyListeners();
  }

  /// Genera un token nuevo e invalida el anterior de inmediato (QR-02).
  void regenerarQr(String mesaId) {
    _exigir(Permiso.salonConfigurar);
    _mesaOFalla(mesaId)
      ..qrToken = _tokenNuevo()
      ..qrPausado = false;
    notifyListeners();
  }

  void pausarQr(String mesaId, bool pausar) {
    _exigir(Permiso.servicioOperar);
    _mesaOFalla(mesaId).qrPausado = pausar;
    notifyListeners();
  }

  /// Enciende o apaga los pedidos por QR de todo el local (QR-17).
  void habilitarQr(bool habilitado) {
    _exigir(Permiso.servicioOperar);
    _qrHabilitado = habilitado;
    notifyListeners();
  }

  Mesa _mesaOFalla(String id) {
    final mesa = this.mesa(id);
    if (mesa == null) {
      throw const ReglaRestaurante('QR_INVALIDO');
    }
    return mesa;
  }

  static const _alfabeto = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';

  String _tokenNuevo() => String.fromCharCodes(
      List.generate(22, (_) => _alfabeto.codeUnitAt(_azar.nextInt(_alfabeto.length))));

  // --------------------------------------------------------------- cuentas

  Cuenta abrirCuenta({String? mesaId, required int comensales}) {
    _exigir(Permiso.cuentaAbrir);
    if (!_servicioAbierto) {
      throw const ReglaRestaurante('SERVICIO_CERRADO');
    }
    if (comensales < 1 || comensales > 50) {
      throw const ReglaRestaurante('LIMITE_EXCEDIDO', 'Los comensales van de 1 a 50.');
    }
    if (mesaId != null) {
      final mesa = _mesaOFalla(mesaId);
      if (mesa.bloqueada || cuentaActivaDe(mesaId) != null) {
        throw const ReglaRestaurante('MESA_OCUPADA');
      }
    } else if (_modoServicio == ModoServicio.mesas) {
      throw const ReglaRestaurante('MESA_OCUPADA', 'Este restaurante solo atiende en mesas.');
    }
    final cuenta = Cuenta(
      id: _id('cu'),
      numero: _siguienteCuenta++,
      mesaId: mesaId,
      origen: OrigenPedido.personal,
      comensales: comensales,
      abiertaPor: _rol.etiqueta,
      abiertaEn: ahora,
      presenciaConfirmada: true,
    );
    _cuentas.add(cuenta);
    notifyListeners();
    return cuenta;
  }

  LineaCuenta agregarLinea(
    String cuentaId,
    String platilloId,
    int cantidad, {
    String? nota,
    bool marcaAlergia = false,
  }) {
    _exigir(Permiso.cuentaAbrir);
    final cuenta = _cuentaOFalla(cuentaId);
    if (cuenta.estado != EstadoCuenta.abierta) {
      throw const ReglaRestaurante('CUENTA_NO_ABIERTA');
    }
    final linea = _crearLinea(cuenta, platilloId, cantidad, OrigenPedido.personal,
        nota: nota, marcaAlergia: marcaAlergia, estado: EstadoLinea.pendiente);
    notifyListeners();
    return linea;
  }

  LineaCuenta _crearLinea(
    Cuenta cuenta,
    String platilloId,
    int cantidad,
    OrigenPedido origen, {
    String? nota,
    bool marcaAlergia = false,
    required EstadoLinea estado,
    String? dispositivoId,
    int maxCantidad = 99,
  }) {
    final plato = platillo(platilloId);
    if (plato == null) {
      throw const ReglaRestaurante('PLATILLO_NO_ORDENABLE');
    }
    final motivo = motivoNoOrdenable(plato);
    if (motivo != null) {
      throw ReglaRestaurante(motivo);
    }
    if (cantidad < 1 || cantidad > maxCantidad) {
      throw ReglaRestaurante('LIMITE_EXCEDIDO', 'La cantidad va de 1 a $maxCantidad.');
    }
    final textoNota = nota?.trim();
    if (textoNota != null && textoNota.length > 80) {
      throw const ReglaRestaurante('LIMITE_EXCEDIDO', 'La nota admite hasta 80 caracteres.');
    }
    final linea = LineaCuenta(
      id: _id('li'),
      platilloId: plato.id,
      nombre: plato.nombre,
      cantidad: cantidad,
      precioReferencia: plato.precio,
      origen: origen,
      nota: (textoNota == null || textoNota.isEmpty) ? null : textoNota,
      marcaAlergia: marcaAlergia,
      estado: estado,
      dispositivoId: dispositivoId,
    );
    cuenta.lineas.add(linea);
    return linea;
  }

  Cuenta _cuentaOFalla(String id) {
    final cuenta = this.cuenta(id);
    if (cuenta == null) {
      throw const ReglaRestaurante('CUENTA_NO_ABIERTA');
    }
    return cuenta;
  }

  void quitarLineaPendiente(String cuentaId, String lineaId) {
    _exigir(Permiso.cuentaAbrir);
    final cuenta = _cuentaOFalla(cuentaId);
    cuenta.lineas.removeWhere((l) => l.id == lineaId && l.estado == EstadoLinea.pendiente);
    notifyListeners();
  }

  /// Una comanda por estación con las líneas pendientes (CT-04, CD-03, CD-08).
  List<Comanda> enviarACocina(String cuentaId) {
    _exigir(Permiso.cuentaAbrir);
    final cuenta = _cuentaOFalla(cuentaId);
    if (cuenta.estado != EstadoCuenta.abierta) {
      throw const ReglaRestaurante('CUENTA_NO_ABIERTA');
    }
    return _enviar(cuenta);
  }

  List<Comanda> _enviar(Cuenta cuenta) {
    final pendientes = cuenta.lineas.where((l) => l.estado == EstadoLinea.pendiente).toList();
    if (pendientes.isEmpty) {
      throw const ReglaRestaurante('SIN_LINEAS_PENDIENTES');
    }
    final envioId = _id('en');
    final porEstacion = <Estacion, List<LineaCuenta>>{};
    for (final linea in pendientes) {
      final plato = platillo(linea.platilloId);
      final estacion = plato == null ? Estacion.cocina : estacionDe(plato);
      porEstacion.putIfAbsent(estacion, () => []).add(linea);
    }
    final creadas = <Comanda>[];
    for (final entrada in porEstacion.entries) {
      final comanda = Comanda(
        id: _id('co'),
        cuentaId: cuenta.id,
        envioId: envioId,
        estacion: entrada.key,
        numero: _siguienteComanda++,
        enviadaEn: ahora,
        lineaIds: [for (final l in entrada.value) l.id],
      );
      for (final linea in entrada.value) {
        linea.estado = EstadoLinea.enviada;
        linea.comandaId = comanda.id;
      }
      _comandas.add(comanda);
      creadas.add(comanda);
    }
    notifyListeners();
    return creadas;
  }

  /// Anula una línea enviada con motivo (CT-03, QR-10). Con la comanda lista o entregada ya hay merma y se exige el
  /// permiso de gerente en adelante.
  void anularLinea(String cuentaId, String lineaId, MotivoAnulacion motivo, {String? texto}) {
    final cuenta = _cuentaOFalla(cuentaId);
    final linea = cuenta.lineas.where((l) => l.id == lineaId).firstOrNull;
    if (linea == null || linea.estado != EstadoLinea.enviada) {
      throw const ReglaRestaurante('TRANSICION_INVALIDA');
    }
    if (motivo == MotivoAnulacion.otro && (texto == null || texto.trim().isEmpty)) {
      throw const ReglaRestaurante('MOTIVO_REQUERIDO');
    }
    final comanda = linea.comandaId == null ? null : this.comanda(linea.comandaId!);
    final yaPreparada = comanda != null &&
        (comanda.estado == EstadoComanda.lista || comanda.estado == EstadoComanda.entregada);
    _exigir(yaPreparada ? Permiso.lineaAnularPreparada : Permiso.lineaAnular);
    linea
      ..estado = EstadoLinea.anulada
      ..motivo = motivo
      ..motivoTexto = motivo == MotivoAnulacion.otro ? texto!.trim() : null;
    notifyListeners();
  }

  /// Cancela la cuenta completa con motivo (CT-11). Tres cancelaciones `SIN_COMENSALES` de cuentas por QR en una
  /// mesa dentro de una hora pausan el QR de esa mesa (QR-17).
  void cancelarCuenta(String cuentaId, MotivoAnulacion motivo, {String? texto}) {
    _exigir(Permiso.cuentaCancelar);
    final cuenta = _cuentaOFalla(cuentaId);
    if (!cuenta.estado.noPagada) {
      throw const ReglaRestaurante('CUENTA_NO_ABIERTA');
    }
    if (motivo == MotivoAnulacion.otro && (texto == null || texto.trim().isEmpty)) {
      throw const ReglaRestaurante('MOTIVO_REQUERIDO');
    }
    final hayPreparadas = cuenta.lineas.any((l) {
      final c = l.comandaId == null ? null : comanda(l.comandaId!);
      return l.estado == EstadoLinea.enviada &&
          c != null &&
          (c.estado == EstadoComanda.lista || c.estado == EstadoComanda.entregada);
    });
    if (hayPreparadas) {
      _exigir(Permiso.lineaAnularPreparada);
    }
    for (final linea in cuenta.lineas.where((l) => l.estado != EstadoLinea.anulada)) {
      linea
        ..estado = EstadoLinea.anulada
        ..motivo = motivo
        ..motivoTexto = motivo == MotivoAnulacion.otro ? texto!.trim() : null;
    }
    cuenta.estado = EstadoCuenta.cancelada;
    if (cuenta.origen == OrigenPedido.qr && motivo == MotivoAnulacion.sinComensales && cuenta.mesaId != null) {
      final ahoraMismo = ahora;
      final registro = _bromasPorMesa.putIfAbsent(cuenta.mesaId!, () => []);
      registro
        ..removeWhere((t) => ahoraMismo.difference(t) > const Duration(hours: 1))
        ..add(ahoraMismo);
      if (registro.length >= 3) {
        mesa(cuenta.mesaId!)?.qrPausado = true;
        _avisar('Se pausó el QR de la mesa ${mesa(cuenta.mesaId!)?.etiqueta}: tres cuentas sin comensales en una hora.',
            mesa(cuenta.mesaId!)?.etiqueta ?? '');
      }
    }
    notifyListeners();
  }

  /// ABIERTA → POR_COBRAR (CT-05). Exige al menos una línea y ninguna pendiente.
  void pedirCuenta(String cuentaId) {
    _exigir(Permiso.cuentaAbrir);
    _pedirCuenta(_cuentaOFalla(cuentaId));
    notifyListeners();
  }

  void _pedirCuenta(Cuenta cuenta) {
    if (cuenta.estado != EstadoCuenta.abierta) {
      throw const ReglaRestaurante('CUENTA_NO_ABIERTA');
    }
    if (cuenta.lineas.any((l) => l.estado == EstadoLinea.pendiente)) {
      throw const ReglaRestaurante('LINEAS_SIN_ENVIAR');
    }
    if (cuenta.lineasActivas.isEmpty) {
      throw const ReglaRestaurante('SIN_LINEAS_PENDIENTES', 'La cuenta no tiene líneas que cobrar.');
    }
    cuenta
      ..estado = EstadoCuenta.porCobrar
      ..pedidaEn = ahora
      ..totalCalculado = cuenta.total;
  }

  /// POR_COBRAR → ABIERTA, antes del cobro (CT-06).
  void reabrirCuenta(String cuentaId) {
    _exigir(Permiso.cuentaAbrir);
    final cuenta = _cuentaOFalla(cuentaId);
    if (cuenta.estado != EstadoCuenta.porCobrar) {
      throw const ReglaRestaurante('TRANSICION_INVALIDA');
    }
    cuenta
      ..estado = EstadoCuenta.abierta
      ..totalCalculado = null;
    notifyListeners();
  }

  void confirmarPresencia(String cuentaId) {
    _exigir(Permiso.cuentaAbrir);
    _cuentaOFalla(cuentaId).presenciaConfirmada = true;
    notifyListeners();
  }

  /// Cobra una cuenta por cobrar. En el servidor la app llama a Ventas con `registrarVenta` y la referencia
  /// `restaurantes:cuenta:<id>`; Restaurantes solo escucha `venta.registrada` para marcarla pagada y descontar los
  /// insumos (CT-08, CM-01 a CM-06). Aquí se simula todo ese camino.
  ResultadoCobro cobrar(String cuentaId, String metodo) {
    _exigir(Permiso.cobrar);
    final cuenta = _cuentaOFalla(cuentaId);
    if (cuenta.estado != EstadoCuenta.porCobrar) {
      throw const ReglaRestaurante('TRANSICION_INVALIDA', 'Solo se cobra una cuenta que ya pidió la cuenta.');
    }
    final lineas = <String, int>{};
    for (final linea in cuenta.lineasActivas) {
      lineas[linea.platilloId] = (lineas[linea.platilloId] ?? 0) + linea.cantidad;
    }
    final venta = VentaRestaurante(
      folio: _siguienteFolio++,
      cuentaNumero: cuenta.numero,
      total: cuenta.total,
      metodo: metodo,
      hora: ahora,
      lineas: lineas,
    );
    _ventas.add(venta);
    cuenta
      ..estado = EstadoCuenta.pagada
      ..ventaFolio = venta.folio;
    final consumo = _aplicarConsumo(venta);
    notifyListeners();
    return ResultadoCobro(venta: venta, descontados: consumo.$1, consumoFallido: consumo.$2);
  }

  /// Descuenta los insumos de la venta (CM-01 a CM-06). Atómico: si un artículo no se puede descontar, no se descuenta
  /// ninguno, se guarda el fallo y la venta sigue intacta.
  (List<InsumoDescontado>, ConsumoFallido?) _aplicarConsumo(VentaRestaurante venta) {
    // Escala 8 por platillo, suma por artículo y un solo redondeo a 4 decimales (CM-05).
    final porArticulo = <String, int>{};
    for (final entrada in venta.lineas.entries) {
      final plato = platillo(entrada.key);
      final receta = recetaVigente(entrada.key);
      if (plato == null || plato.modoInventario != ModoInventario.receta || receta == null) {
        continue;
      }
      for (final insumo in receta.insumos) {
        final escala8 = dividirRedondeado(
            insumo.cantidad * (100 + insumo.mermaPct) * entrada.value * 100000000, 100 * receta.rendimiento);
        porArticulo[insumo.articuloId] = (porArticulo[insumo.articuloId] ?? 0) + escala8;
      }
    }
    if (porArticulo.isEmpty) {
      return (const [], null);
    }
    final cantidades = {for (final e in porArticulo.entries) e.key: dividirRedondeado(e.value, 10000)};
    for (final id in cantidades.keys) {
      final articulo = this.articulo(id);
      if (articulo == null || !articulo.activo) {
        final fallo = ConsumoFallido(
          ventaFolio: venta.folio,
          codigo: 'ARTICULO_INACTIVO',
          detalle: '${articulo?.nombre ?? id} está dado de baja en Inventario.',
          creadoEn: ahora,
        );
        _consumosFallidos.insert(0, fallo);
        return (const [], fallo);
      }
    }
    final descontados = <InsumoDescontado>[];
    for (final entrada in cantidades.entries) {
      final articulo = this.articulo(entrada.key)!;
      articulo.existencia -= entrada.value;
      descontados.add(InsumoDescontado(nombre: articulo.nombre, cantidad: entrada.value, unidad: articulo.unidad));
    }
    return (descontados, null);
  }

  // ---------------------------------------------------------------- cocina

  /// Comandas no entregadas de la estación (o de las dos), de la más antigua a la más nueva (CD-03).
  List<Comanda> comandasDe(Estacion? estacion) => _comandas
      .where((c) => c.estado != EstadoComanda.entregada && (estacion == null || c.estacion == estacion))
      .toList()
    ..sort((a, b) => a.enviadaEn.compareTo(b.enviadaEn));

  List<LineaCuenta> lineasDe(Comanda comanda) {
    final cuenta = this.cuenta(comanda.cuentaId);
    if (cuenta == null) {
      return const [];
    }
    return [for (final l in cuenta.lineas) if (comanda.lineaIds.contains(l.id)) l];
  }

  /// Minutos de preparación: hasta ahora, o hasta que quedó lista (CD-04). Los calcula el código.
  int minutosDe(Comanda comanda) {
    final fin = comanda.listaEn ?? ahora;
    return fin.difference(comanda.enviadaEn).inMinutes;
  }

  bool esLenta(Comanda comanda) =>
      comanda.estado != EstadoComanda.lista && comanda.estado != EstadoComanda.entregada && minutosDe(comanda) >= umbralPreparacionMin;

  /// NUEVA → EN_PREPARACION → LISTA (CD-01, CD-02).
  void avanzarComanda(String id) {
    _exigir(Permiso.comandaPreparar);
    final comanda = _comandaOFalla(id);
    switch (comanda.estado) {
      case EstadoComanda.nueva:
        comanda.estado = EstadoComanda.enPreparacion;
      case EstadoComanda.enPreparacion:
        comanda.estado = EstadoComanda.lista;
        comanda.listaEn = ahora;
        _avisarComandaLista(comanda);
      case EstadoComanda.lista:
      case EstadoComanda.entregada:
        throw const ReglaRestaurante('TRANSICION_INVALIDA');
    }
    notifyListeners();
  }

  /// Única vuelta atrás permitida: LISTA → EN_PREPARACION (CD-01).
  void devolverComanda(String id) {
    _exigir(Permiso.comandaPreparar);
    final comanda = _comandaOFalla(id);
    if (comanda.estado != EstadoComanda.lista) {
      throw const ReglaRestaurante('TRANSICION_INVALIDA');
    }
    comanda
      ..estado = EstadoComanda.enPreparacion
      ..listaEn = null;
    notifyListeners();
  }

  void entregarComanda(String id) {
    _exigir(Permiso.comandaEntregar);
    final comanda = _comandaOFalla(id);
    if (comanda.estado != EstadoComanda.lista) {
      throw const ReglaRestaurante('TRANSICION_INVALIDA');
    }
    comanda
      ..estado = EstadoComanda.entregada
      ..entregadaEn = ahora;
    notifyListeners();
  }

  Comanda _comandaOFalla(String id) {
    final comanda = this.comanda(id);
    if (comanda == null) {
      throw const ReglaRestaurante('TRANSICION_INVALIDA');
    }
    return comanda;
  }

  void _avisarComandaLista(Comanda comanda) {
    final cuenta = this.cuenta(comanda.cuentaId);
    final etiqueta = _etiquetaDeMesa(cuenta);
    _avisar('${comanda.estacion.etiqueta} lista en $etiqueta (comanda ${comanda.numero}).', etiqueta);
  }

  String _etiquetaDeMesa(Cuenta? cuenta) =>
      cuenta?.mesaId == null ? 'Mostrador' : (mesa(cuenta!.mesaId!)?.etiqueta ?? '');

  // ------------------------------------------------------------------ avisos

  void _avisar(String texto, String mesaEtiqueta) {
    _avisos.insert(0, AvisoSalon(texto: texto, hora: ahora, mesaEtiqueta: mesaEtiqueta));
  }

  void atenderAviso(AvisoSalon aviso) {
    aviso.atendido = true;
    notifyListeners();
  }

  // ----------------------------------------------------------- mesa por QR

  Mesa? _mesaDeToken(String token) => _mesas.where((m) => m.qrToken == token).firstOrNull;

  /// Sin sesión (QR-03): un token inexistente, rotado o de una mesa dada de baja responde igual, con nulo.
  VistaMesaQr? mesaPorQr(String token) {
    final mesa = _mesaDeToken(token);
    if (mesa == null || !_visible) {
      return null;
    }
    final cuenta = cuentaActivaDe(mesa.id);
    final estadoCuenta = cuenta == null
        ? EstadoCuentaQr.sinCuenta
        : (cuenta.estado == EstadoCuenta.porCobrar ? EstadoCuentaQr.solicitada : EstadoCuentaQr.abierta);
    final motivo = _motivoNoAcepta(mesa, estadoCuenta);
    return VistaMesaQr(
      restaurante: nombre,
      mesa: mesa.etiqueta,
      servicioAbierto: _servicioAbierto,
      aceptaPedidos: motivo == null,
      estadoCuenta: estadoCuenta,
      motivoNoAcepta: motivo,
    );
  }

  /// QR-05: se aceptan pedidos si el negocio es visible, el QR está habilitado, el servicio abierto, la mesa ni
  /// bloqueada ni pausada y la cuenta no fue solicitada.
  String? _motivoNoAcepta(Mesa mesa, EstadoCuentaQr estadoCuenta) {
    if (!_qrHabilitado) {
      return 'QR_DESHABILITADO';
    }
    if (!_servicioAbierto) {
      return 'SERVICIO_CERRADO';
    }
    if (mesa.bloqueada || mesa.qrPausado) {
      return 'QR_PAUSADO';
    }
    if (estadoCuenta == EstadoCuentaQr.solicitada) {
      return 'CUENTA_SOLICITADA';
    }
    return null;
  }

  /// Pedido del comensal: va directo a cocina, sin aceptación del personal (QR-06). Idempotente por `llave`.
  ResultadoPedidoQr pedirPorQr(
    String token,
    String dispositivoId,
    List<LineaPedidoQr> lineas, {
    required String llave,
  }) {
    final previo = _llavesIdempotentes[llave];
    if (previo != null) {
      return previo;
    }
    final mesa = _mesaDeToken(token);
    final vista = mesaPorQr(token);
    if (mesa == null || vista == null) {
      throw const ReglaRestaurante('QR_INVALIDO');
    }
    if (!vista.aceptaPedidos) {
      throw ReglaRestaurante(vista.motivoNoAcepta ?? 'QR_PAUSADO');
    }
    if (lineas.isEmpty) {
      throw const ReglaRestaurante('SIN_LINEAS_PENDIENTES', 'Agrega al menos un platillo.');
    }
    if (lineas.length > maxLineasPorPedidoQr) {
      throw const ReglaRestaurante('LIMITE_QR_EXCEDIDO');
    }
    final ahoraMismo = ahora;
    final recientes = _pedidosQrRecientes.putIfAbsent(token, () => [])
      ..removeWhere((t) => ahoraMismo.difference(t) > const Duration(minutes: 1));
    if (recientes.length >= maxPedidosQrPorMinuto) {
      throw const ReglaRestaurante('TASA_EXCEDIDA');
    }
    // Validación completa antes de escribir: o entra todo el pedido o nada.
    var monto = 0;
    for (final linea in lineas) {
      final plato = platillo(linea.platilloId);
      if (plato == null) {
        throw const ReglaRestaurante('PLATILLO_NO_ORDENABLE');
      }
      final motivo = motivoNoOrdenable(plato);
      if (motivo != null) {
        throw ReglaRestaurante(motivo, '${plato.nombre}: ${plantillasDeError[motivo]}');
      }
      if (linea.cantidad < 1 || linea.cantidad > maxCantidadPorLineaQr) {
        throw const ReglaRestaurante('LIMITE_QR_EXCEDIDO');
      }
      monto += plato.precio * linea.cantidad;
    }
    var cuenta = cuentaActivaDe(mesa.id);
    final existentes = cuenta?.lineas.where((l) => l.origen == OrigenPedido.qr).length ?? 0;
    final montoActual = cuenta?.total ?? 0;
    if (existentes + lineas.length > maxLineasPorCuentaQr || montoActual + monto > maxMontoPorCuentaQr) {
      throw const ReglaRestaurante('LIMITE_QR_EXCEDIDO');
    }
    var abrio = false;
    if (cuenta == null) {
      cuenta = Cuenta(
        id: _id('cu'),
        numero: _siguienteCuenta++,
        mesaId: mesa.id,
        origen: OrigenPedido.qr,
        abiertaEn: ahoraMismo,
      );
      _cuentas.add(cuenta);
      abrio = true;
    }
    for (final linea in lineas) {
      _crearLinea(cuenta, linea.platilloId, linea.cantidad, OrigenPedido.qr,
          nota: linea.nota,
          marcaAlergia: linea.marcaAlergia,
          estado: EstadoLinea.pendiente,
          dispositivoId: dispositivoId,
          maxCantidad: maxCantidadPorLineaQr);
    }
    _enviar(cuenta);
    recientes.add(ahoraMismo);
    _avisar(
      'Pedido por QR en la mesa ${mesa.etiqueta}: ${lineas.length} ${lineas.length == 1 ? 'línea' : 'líneas'}'
      '${abrio ? ' (abrió la cuenta)' : ''}.',
      mesa.etiqueta,
    );
    final resultado = ResultadoPedidoQr(abrioCuenta: abrio, lineas: lineas.length);
    _llavesIdempotentes[llave] = resultado;
    notifyListeners();
    return resultado;
  }

  /// Las líneas pedidas desde ese dispositivo, con su estado (QR-11). Nunca las de otros dispositivos.
  PedidoQrVista? estadoDePedidoQr(String token, String dispositivoId) {
    final mesa = _mesaDeToken(token);
    final vista = mesaPorQr(token);
    if (mesa == null || vista == null) {
      return null;
    }
    final cuenta = cuentaActivaDe(mesa.id);
    if (cuenta == null) {
      return PedidoQrVista(lineas: const [], cuenta: vista.estadoCuenta);
    }
    final lineas = <LineaVistaQr>[];
    for (final linea in cuenta.lineas.where((l) => l.dispositivoId == dispositivoId)) {
      if (linea.estado == EstadoLinea.anulada) {
        lineas.add(LineaVistaQr(
          nombre: linea.nombre,
          cantidad: linea.cantidad,
          estado: EstadoVistaLinea.cancelado,
          mensaje: linea.motivo == MotivoAnulacion.otro
              ? 'Cancelado: ${linea.motivoTexto}'
              : linea.motivo?.mensajeComensal,
        ));
        continue;
      }
      final comanda = linea.comandaId == null ? null : this.comanda(linea.comandaId!);
      final estado = switch (comanda?.estado) {
        EstadoComanda.enPreparacion => EstadoVistaLinea.preparando,
        EstadoComanda.lista => EstadoVistaLinea.listo,
        EstadoComanda.entregada => EstadoVistaLinea.entregado,
        _ => EstadoVistaLinea.enCocina,
      };
      lineas.add(LineaVistaQr(nombre: linea.nombre, cantidad: linea.cantidad, estado: estado));
    }
    return PedidoQrVista(lineas: lineas, cuenta: vista.estadoCuenta);
  }

  /// El comensal pide la cuenta (QR-13): pasa a por cobrar y avisa a meseros y caja.
  void pedirCuentaPorQr(String token) {
    final mesa = _mesaDeToken(token);
    if (mesa == null) {
      throw const ReglaRestaurante('QR_INVALIDO');
    }
    final cuenta = cuentaActivaDe(mesa.id);
    if (cuenta == null || cuenta.estado != EstadoCuenta.abierta) {
      throw ReglaRestaurante(cuenta?.estado == EstadoCuenta.porCobrar ? 'CUENTA_SOLICITADA' : 'CUENTA_NO_ABIERTA');
    }
    _pedirCuenta(cuenta);
    _avisar('La mesa ${mesa.etiqueta} pidió la cuenta por QR. Total ${_pesos(cuenta.total)}.', mesa.etiqueta);
    notifyListeners();
  }

  static String _pesos(int centavos) =>
      '\$${(centavos ~/ 100).toString().replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (_) => ',')}.${(centavos % 100).toString().padLeft(2, '0')}';

  // ---------------------------------------------------------------- siembra

  void _sembrar() {
    _categorias = [...categoriasDemo];
    _platillos = [...platillosDemo()];
    _articulos = articulosDemo();
    _zonas = [...zonasDemo];
    _mesas = mesasDemo();
    final inicio = ahora.subtract(const Duration(hours: 5));
    final insumos = insumosDeRecetasDemo();
    for (final entrada in insumos.entries) {
      _recetas[entrada.key] = [
        VersionReceta(
          numero: 1,
          rendimiento: rendimientosDemo[entrada.key] ?? 1,
          insumos: entrada.value,
          vigenteDesde: inicio.subtract(const Duration(days: 30)),
        ),
      ];
    }
    _sembrarVentas();
    _sembrarServicio();
  }

  void _sembrarVentas() {
    final hoy = ahora;
    void venta(int minutosAtras, String metodo, Map<String, int> lineas) {
      final total = lineas.entries.fold(0, (suma, e) => suma + e.value * (platillo(e.key)?.precio ?? 0));
      _ventas.add(VentaRestaurante(
        folio: _siguienteFolio++,
        cuentaNumero: 1034 + _ventas.length,
        total: total,
        metodo: metodo,
        hora: hoy.subtract(Duration(minutes: minutosAtras)),
        lineas: lineas,
      ));
    }

    _siguienteFolio = 2294;
    venta(260, 'Efectivo', {'p-cemita': 2, 'p-horchata': 2});
    venta(230, 'Tarjeta', {'p-mole': 2, 'p-horchata': 2, 'p-camote': 1});
    venta(190, 'Efectivo', {'p-arabes': 2, 'p-jamaica': 2});
    venta(150, 'Tarjeta', {'p-cemita': 3, 'p-cerveza': 2, 'p-guacamole': 1});
    venta(110, 'Transferencia', {'p-pipian': 2, 'p-cafe': 2});
    venta(70, 'Efectivo', {'p-cemita': 1, 'p-horchata': 1});
    _consumosFallidos.add(ConsumoFallido(
      ventaFolio: 2296,
      codigo: 'ARTICULO_INACTIVO',
      detalle: 'Flor de jamaica está dado de baja en Inventario.',
      creadoEn: hoy.subtract(const Duration(minutes: 190)),
    ));
    _siguienteFolio = 2300;
  }

  void _sembrarServicio() {
    final hoy = ahora;

    Comanda comanda(Cuenta cuenta, Estacion estacion, int minutosAtras, List<LineaCuenta> lineas,
        {EstadoComanda estado = EstadoComanda.nueva, int? listaHace, String? envio}) {
      final c = Comanda(
        id: _id('co'),
        cuentaId: cuenta.id,
        envioId: envio ?? _id('en'),
        estacion: estacion,
        numero: _siguienteComanda++,
        enviadaEn: hoy.subtract(Duration(minutes: minutosAtras)),
        lineaIds: [for (final l in lineas) l.id],
        estado: estado,
      );
      if (listaHace != null) {
        c.listaEn = hoy.subtract(Duration(minutes: listaHace));
      }
      for (final l in lineas) {
        l.estado = EstadoLinea.enviada;
        l.comandaId = c.id;
      }
      _comandas.add(c);
      return c;
    }

    LineaCuenta linea(Cuenta cuenta, String platilloId, int cantidad,
        {String? nota, bool alergia = false, OrigenPedido origen = OrigenPedido.personal, String? dispositivo}) {
      final plato = platillo(platilloId)!;
      final l = LineaCuenta(
        id: _id('li'),
        platilloId: plato.id,
        nombre: plato.nombre,
        cantidad: cantidad,
        precioReferencia: plato.precio,
        origen: origen,
        nota: nota,
        marcaAlergia: alergia,
        dispositivoId: dispositivo,
      );
      cuenta.lineas.add(l);
      return l;
    }

    // Mesa 2: ya pidió la cuenta, lista para cobrar.
    final c1041 = Cuenta(
        id: _id('cu'), numero: 1041, mesaId: 'm2', origen: OrigenPedido.personal, comensales: 3,
        abiertaPor: 'Mesero', abiertaEn: hoy.subtract(const Duration(minutes: 62)), presenciaConfirmada: true);
    _cuentas.add(c1041);
    final mole = linea(c1041, 'p-mole', 2);
    final agua = linea(c1041, 'p-horchata', 2);
    final camote = linea(c1041, 'p-camote', 1);
    comanda(c1041, Estacion.cocina, 55, [mole, camote], estado: EstadoComanda.entregada);
    comanda(c1041, Estacion.barra, 55, [agua], estado: EstadoComanda.entregada);
    c1041
      ..estado = EstadoCuenta.porCobrar
      ..pedidaEn = hoy.subtract(const Duration(minutes: 4))
      ..totalCalculado = c1041.total;

    // Mesa 3: en servicio, con una comanda lenta y otra lista.
    final c1042 = Cuenta(
        id: _id('cu'), numero: 1042, mesaId: 'm3', origen: OrigenPedido.personal, comensales: 4,
        abiertaPor: 'Mesero', abiertaEn: hoy.subtract(const Duration(minutes: 24)), presenciaConfirmada: true);
    _cuentas.add(c1042);
    final envio = _id('en');
    final cemitas = linea(c1042, 'p-cemita', 2, nota: 'Una sin papalo');
    final horchatas = linea(c1042, 'p-horchata', 2);
    comanda(c1042, Estacion.cocina, 18, [cemitas], estado: EstadoComanda.enPreparacion, envio: envio);
    comanda(c1042, Estacion.barra, 18, [horchatas], estado: EstadoComanda.lista, listaHace: 12, envio: envio);
    // Línea pendiente con marca de alergia, todavía sin enviar.
    linea(c1042, 'p-mole', 1, nota: 'Alergia a la nuez', alergia: true).estado = EstadoLinea.pendiente;

    // Mesa 7: abierta por un comensal desde el QR, sin mesero todavía.
    final c1043 = Cuenta(
        id: _id('cu'), numero: 1043, mesaId: 'm7', origen: OrigenPedido.qr,
        abiertaEn: hoy.subtract(const Duration(minutes: 2)));
    _cuentas.add(c1043);
    final envioQr = _id('en');
    final guac = linea(c1043, 'p-guacamole', 1, origen: OrigenPedido.qr, dispositivo: 'disp-otro');
    final jamaica = linea(c1043, 'p-jamaica', 2, origen: OrigenPedido.qr, dispositivo: 'disp-otro');
    comanda(c1043, Estacion.cocina, 2, [guac], envio: envioQr);
    comanda(c1043, Estacion.barra, 2, [jamaica], envio: envioQr);
    _avisos.add(AvisoSalon(
        texto: 'Pedido por QR en la mesa 7: 2 líneas (abrió la cuenta).',
        hora: hoy.subtract(const Duration(minutes: 2)),
        mesaEtiqueta: '7'));
    _avisos.add(AvisoSalon(
        texto: 'La mesa 2 pidió la cuenta.', hora: hoy.subtract(const Duration(minutes: 4)), mesaEtiqueta: '2'));
    _siguienteCuenta = 1044;
  }
}
