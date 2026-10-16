/// Modelos de la maqueta de Restaurantes. Siguen el diseño `docs/diseno/restaurantes.md` (v0.5): los códigos de los
/// enums son los del contrato GraphQL propuesto y el dinero va en centavos enteros (D-20). Las cantidades de insumos
/// van en diezmilésimas de la unidad base (NUMERIC(14,4) del servidor) para no usar `double`.
library;

/// Etiquetas de dieta declaradas por el negocio, no verificadas (MN-08, D-106).
enum EtiquetaDieta {
  vegetariano('VEGETARIANO', 'Vegetariano'),
  vegano('VEGANO', 'Vegano'),
  sinGluten('SIN_GLUTEN', 'Sin gluten'),
  sinLactosa('SIN_LACTOSA', 'Sin lactosa'),
  sinFrutosSecos('SIN_FRUTOS_SECOS', 'Sin frutos secos'),
  picante('PICANTE', 'Picante'),
  tipicoPoblano('TIPICO_POBLANO', 'Típico poblano'),
  deTemporada('DE_TEMPORADA', 'De temporada'),
  contieneAlcohol('CONTIENE_ALCOHOL', 'Contiene alcohol');

  const EtiquetaDieta(this.codigo, this.etiqueta);

  final String codigo;
  final String etiqueta;
}

/// Estación de la cocina que prepara una categoría (MN-07, CD-03).
enum Estacion {
  cocina('COCINA', 'Cocina'),
  barra('BARRA', 'Barra');

  const Estacion(this.codigo, this.etiqueta);

  final String codigo;
  final String etiqueta;
}

/// Cómo se descuenta el inventario de un platillo (MN-04).
enum ModoInventario {
  ninguno('NINGUNO', 'Sin descuento'),
  receta('RECETA', 'Por receta'),
  vinculoDirecto('VINCULO_DIRECTO', 'Vínculo directo');

  const ModoInventario(this.codigo, this.etiqueta);

  final String codigo;
  final String etiqueta;
}

class CategoriaMenu {
  const CategoriaMenu({required this.id, required this.nombre, required this.orden, this.estacion = Estacion.cocina});

  final String id;
  final String nombre;
  final int orden;
  final Estacion estacion;
}

/// Rango de temporada por mes y día; puede cruzar el fin de año (MN-06).
class Temporada {
  const Temporada({required this.mesDesde, required this.diaDesde, required this.mesHasta, required this.diaHasta});

  final int mesDesde;
  final int diaDesde;
  final int mesHasta;
  final int diaHasta;

  bool contiene(DateTime fecha) {
    final valor = fecha.month * 100 + fecha.day;
    final desde = mesDesde * 100 + diaDesde;
    final hasta = mesHasta * 100 + diaHasta;
    return desde <= hasta ? valor >= desde && valor <= hasta : valor >= desde || valor <= hasta;
  }

  static const _meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

  String get texto => '$diaDesde ${_meses[mesDesde - 1]} a $diaHasta ${_meses[mesHasta - 1]}';
}

/// Platillo del menú. Su `id` es el `producto_id` que conoce Ventas (MN-02). `precio` en centavos.
class Platillo {
  const Platillo({
    required this.id,
    required this.nombre,
    required this.descripcion,
    required this.precio,
    this.categoriaId,
    this.etiquetas = const {},
    this.agotado = false,
    this.temporada,
    this.publicado = true,
    this.activo = true,
    this.modoInventario = ModoInventario.ninguno,
    this.foto,
  });

  final String id;
  final String nombre;
  final String descripcion;
  final int precio;
  final String? categoriaId;
  final Set<EtiquetaDieta> etiquetas;
  final bool agotado;
  final Temporada? temporada;
  final bool publicado;
  final bool activo;
  final ModoInventario modoInventario;

  /// Clave de la foto en la maqueta: el tono de la ilustración (no hay imágenes reales).
  final int? foto;

  Platillo copiar({
    String? nombre,
    String? descripcion,
    int? precio,
    Object? categoriaId = _igual,
    Set<EtiquetaDieta>? etiquetas,
    bool? agotado,
    Object? temporada = _igual,
    bool? publicado,
    bool? activo,
    ModoInventario? modoInventario,
  }) =>
      Platillo(
        id: id,
        nombre: nombre ?? this.nombre,
        descripcion: descripcion ?? this.descripcion,
        precio: precio ?? this.precio,
        categoriaId: identical(categoriaId, _igual) ? this.categoriaId : categoriaId as String?,
        etiquetas: etiquetas ?? this.etiquetas,
        agotado: agotado ?? this.agotado,
        temporada: identical(temporada, _igual) ? this.temporada : temporada as Temporada?,
        publicado: publicado ?? this.publicado,
        activo: activo ?? this.activo,
        modoInventario: modoInventario ?? this.modoInventario,
        foto: foto,
      );
}

const Object _igual = Object();

/// Artículo de Inventario que se usa como insumo. Cantidades en diezmilésimas de la unidad base; `cpp` en milésimas
/// de peso por unidad base (costo promedio ponderado), nulo si Inventario aún no lo tiene.
class Articulo {
  Articulo({
    required this.id,
    required this.nombre,
    required this.unidad,
    required this.existencia,
    required this.minimo,
    this.cpp,
    this.activo = true,
  });

  final String id;
  final String nombre;

  /// Unidad base: `g`, `ml` o `pza`.
  final String unidad;
  int existencia;
  final int minimo;
  final int? cpp;
  final bool activo;

  bool get bajoMinimo => existencia <= minimo;
}

/// Un insumo dentro de una versión de receta. `cantidad` es neta, en unidades base enteras.
class InsumoReceta {
  const InsumoReceta({required this.articuloId, required this.cantidad, this.mermaPct = 0});

  final String articuloId;
  final int cantidad;
  final int mermaPct;
}

/// Versión inmutable de una receta (RC-01). El rendimiento vive en cada versión (RC-04).
class VersionReceta {
  const VersionReceta({
    required this.numero,
    required this.rendimiento,
    required this.insumos,
    required this.vigenteDesde,
    this.vigenteHasta,
  });

  final int numero;
  final int rendimiento;
  final List<InsumoReceta> insumos;
  final DateTime vigenteDesde;
  final DateTime? vigenteHasta;

  bool get vigente => vigenteHasta == null;

  /// Copia de esta versión cerrada en [fecha]; las versiones nunca se editan, solo se cierran (RC-01).
  VersionReceta cerrada(DateTime fecha) => VersionReceta(
        numero: numero,
        rendimiento: rendimiento,
        insumos: insumos,
        vigenteDesde: vigenteDesde,
        vigenteHasta: fecha,
      );
}

/// Costo teórico y margen de una versión (RC-05, RC-06). Todo en centavos; el porcentaje en puntos base (4912 = 49.12 %).
class CostoReceta {
  const CostoReceta({required this.costo, required this.sinCosto, this.margen, this.margenPuntosBase});

  /// Nulo cuando algún insumo no tiene CPP: el costo queda INCOMPLETO y nunca se estima.
  final int? costo;
  final List<String> sinCosto;
  final int? margen;
  final int? margenPuntosBase;

  bool get incompleto => costo == null;
}

/// Cómo atiende el restaurante (MS-08).
enum ModoServicio {
  mesas('MESAS', 'Mesas'),
  mostrador('MOSTRADOR', 'Mostrador'),
  ambos('AMBOS', 'Mesas y mostrador');

  const ModoServicio(this.codigo, this.etiqueta);

  final String codigo;
  final String etiqueta;
}

class Zona {
  const Zona({required this.id, required this.nombre});

  final String id;
  final String nombre;
}

class Mesa {
  Mesa({
    required this.id,
    required this.etiqueta,
    required this.zonaId,
    required this.capacidad,
    required this.qrToken,
    this.bloqueada = false,
    this.qrPausado = false,
  });

  final String id;
  final String etiqueta;
  final String zonaId;
  final int capacidad;
  String qrToken;
  bool bloqueada;
  bool qrPausado;
}

/// Estado derivado de la mesa; no se guarda (MS-02). `porCobrar` es una variante visual de ocupada.
enum EstadoMesa { libre, ocupada, porCobrar, bloqueada }

enum EstadoCuenta {
  abierta('ABIERTA', 'Abierta'),
  porCobrar('POR_COBRAR', 'Por cobrar'),
  pagada('PAGADA', 'Pagada'),
  cancelada('CANCELADA', 'Cancelada');

  const EstadoCuenta(this.codigo, this.etiqueta);

  final String codigo;
  final String etiqueta;

  bool get noPagada => this == abierta || this == porCobrar;
}

enum OrigenPedido { personal, qr }

enum EstadoLinea {
  pendiente('Pendiente'),
  enviada('Enviada'),
  anulada('Anulada');

  const EstadoLinea(this.etiqueta);

  final String etiqueta;
}

/// Motivos de anulación de una línea o cuenta (QR-10). El servidor compone el mensaje que ve el comensal.
enum MotivoAnulacion {
  agotado('AGOTADO', 'Agotado', 'Cancelado: agotado'),
  errorDelPedido('ERROR_DEL_PEDIDO', 'Error del pedido', 'Cancelado: hubo un error en el pedido'),
  sinComensales('SIN_COMENSALES', 'Sin comensales', 'Cancelado: no había nadie en la mesa'),
  cocinaNoPuede('COCINA_NO_PUEDE', 'La cocina no puede', 'Cancelado: la cocina no puede prepararlo'),
  otro('OTRO', 'Otro', 'Cancelado');

  const MotivoAnulacion(this.codigo, this.etiqueta, this.mensajeComensal);

  final String codigo;
  final String etiqueta;
  final String mensajeComensal;
}

class LineaCuenta {
  LineaCuenta({
    required this.id,
    required this.platilloId,
    required this.nombre,
    required this.cantidad,
    required this.precioReferencia,
    required this.origen,
    this.nota,
    this.marcaAlergia = false,
    this.estado = EstadoLinea.pendiente,
    this.dispositivoId,
    this.comandaId,
    this.motivo,
    this.motivoTexto,
  });

  final String id;
  final String platilloId;
  final String nombre;
  final int cantidad;

  /// Precio informativo al agregar; cobra Ventas con el vigente (CT-13).
  final int precioReferencia;
  final OrigenPedido origen;
  final String? nota;
  final bool marcaAlergia;
  EstadoLinea estado;
  final String? dispositivoId;
  String? comandaId;
  MotivoAnulacion? motivo;
  String? motivoTexto;

  int get importe => cantidad * precioReferencia;
}

class Cuenta {
  Cuenta({
    required this.id,
    required this.numero,
    required this.origen,
    required this.abiertaEn,
    this.mesaId,
    this.comensales,
    this.abiertaPor,
    this.estado = EstadoCuenta.abierta,
    this.presenciaConfirmada = false,
  });

  final String id;
  final int numero;
  final String? mesaId;
  final OrigenPedido origen;
  final int? comensales;
  final String? abiertaPor;
  final DateTime abiertaEn;
  EstadoCuenta estado;
  bool presenciaConfirmada;
  DateTime? pedidaEn;
  int? totalCalculado;
  int? ventaFolio;
  final List<LineaCuenta> lineas = [];

  Iterable<LineaCuenta> get lineasActivas => lineas.where((l) => l.estado != EstadoLinea.anulada);

  /// Total con los precios de referencia; el definitivo lo calcula Ventas al cobrar.
  int get total => lineasActivas.fold(0, (suma, l) => suma + l.importe);
}

enum EstadoComanda {
  nueva('NUEVA', 'Nueva'),
  enPreparacion('EN_PREPARACION', 'En preparación'),
  lista('LISTA', 'Lista'),
  entregada('ENTREGADA', 'Entregada');

  const EstadoComanda(this.codigo, this.etiqueta);

  final String codigo;
  final String etiqueta;
}

/// Una comanda por estación en cada envío (CT-04, CD-03). Las comandas de un envío comparten `envioId` (CD-08).
class Comanda {
  Comanda({
    required this.id,
    required this.cuentaId,
    required this.envioId,
    required this.estacion,
    required this.numero,
    required this.enviadaEn,
    required this.lineaIds,
    this.estado = EstadoComanda.nueva,
  });

  final String id;
  final String cuentaId;
  final String envioId;
  final Estacion estacion;
  final int numero;
  final DateTime enviadaEn;
  final List<String> lineaIds;
  EstadoComanda estado;
  DateTime? listaEn;
  DateTime? entregadaEn;
}

/// Consumo de insumos que no se pudo aplicar (CM-06). La venta nunca se bloquea.
class ConsumoFallido {
  const ConsumoFallido({required this.ventaFolio, required this.codigo, required this.detalle, required this.creadoEn});

  final int ventaFolio;
  final String codigo;
  final String detalle;
  final DateTime creadoEn;
}

/// Venta de la cuenta, registrada por Ventas al cobrar. En la maqueta la guarda el estado local.
class VentaRestaurante {
  const VentaRestaurante({
    required this.folio,
    required this.cuentaNumero,
    required this.total,
    required this.metodo,
    required this.hora,
    required this.lineas,
  });

  final int folio;
  final int cuentaNumero;
  final int total;
  final String metodo;
  final DateTime hora;

  /// Platillo → cantidad vendida.
  final Map<String, int> lineas;
}

/// Reseña de un restaurante (viene de Negocios; aquí solo se muestra). Estrellas de 1 a 5.
class ResenaRestaurante {
  const ResenaRestaurante({required this.autor, required this.estrellas, required this.texto, required this.fecha, this.respuesta});

  final String autor;
  final int estrellas;
  final String texto;
  final String fecha;
  final String? respuesta;
}

/// Subcategorías de restaurante (tipo de cocina o carta), del diseño de la plataforma.
enum TipoCocina {
  sinGluten('Gluten free'),
  verano('Verano'),
  mixto('Mixto'),
  tipica('Gastronomía típica'),
  comidaCorrida('Comida corrida'),
  extranjera('Extranjera');

  const TipoCocina(this.etiqueta);

  final String etiqueta;
}

/// Ficha pública de un restaurante en el directorio del turista.
class FichaRestaurante {
  const FichaRestaurante({
    required this.id,
    required this.nombre,
    required this.zona,
    required this.descripcion,
    required this.horario,
    required this.tipos,
    required this.precioMedio,
    required this.resenas,
    this.mesasLibres,
    this.mesasTotal,
    this.abierto = true,
    this.aceptaPasaporte = false,
    this.esElDeLaDemo = false,
    this.tono = 0,
  });

  final String id;
  final String nombre;
  final String zona;
  final String descripcion;
  final String horario;
  final Set<TipoCocina> tipos;

  /// Gasto medio por persona, en centavos.
  final int precioMedio;
  final List<ResenaRestaurante> resenas;

  /// Mesas libres publicadas; nulo si el restaurante no publica disponibilidad.
  final int? mesasLibres;
  final int? mesasTotal;
  final bool abierto;
  final bool aceptaPasaporte;

  /// Verdadero para el restaurante cuyo estado vive en `RestauranteDemo` (mesas libres en vivo).
  final bool esElDeLaDemo;
  final int tono;

  double get promedio => resenas.isEmpty ? 0 : resenas.fold(0, (s, r) => s + r.estrellas) / resenas.length;
}
