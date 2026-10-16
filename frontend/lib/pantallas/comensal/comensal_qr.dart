import 'package:flutter/material.dart';

import '../../datos/demo_restaurantes.dart';
import '../../estado/restaurante_demo.dart';
import '../../modelos/dinero.dart';
import '../../modelos/restaurantes.dart';
import '../../tema.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/carta_publica.dart';
import '../../widgets/componentes.dart';
import '../../widgets/etiqueta_demo.dart';
import '../restaurantes/alcance.dart';
import '../turista/resenas_restaurante.dart';

/// Página que se abre al escanear el QR de una mesa. Funciona igual con la app o en la web, sin cuenta ni registro
/// (QR-04, QR-21): carta, restaurante y reseñas, y pedido directo a cocina cuando el servicio está abierto.
class PaginaComensal extends StatefulWidget {
  const PaginaComensal({super.key, required this.token});

  final String token;

  @override
  State<PaginaComensal> createState() => _PaginaComensalState();
}

class _PaginaComensalState extends State<PaginaComensal> {
  final Map<String, int> _carrito = {};
  final _nota = TextEditingController();
  bool _alergia = false;
  String? _llave;
  int _intentos = 0;
  int _pestana = 0;

  @override
  void dispose() {
    _nota.dispose();
    super.dispose();
  }

  void _sumar(Platillo p) {
    final actual = _carrito[p.id] ?? 0;
    if (actual >= RestauranteDemo.maxCantidadPorLineaQr) {
      mostrarAviso(context, 'Máximo ${RestauranteDemo.maxCantidadPorLineaQr} por platillo.');
      return;
    }
    setState(() {
      _carrito[p.id] = actual + 1;
      _llave = null;
    });
  }

  void _restar(Platillo p) {
    final actual = _carrito[p.id] ?? 0;
    setState(() {
      if (actual <= 1) {
        _carrito.remove(p.id);
      } else {
        _carrito[p.id] = actual - 1;
      }
      _llave = null;
    });
  }

  int _total(RestauranteDemo r) =>
      _carrito.entries.fold(0, (suma, e) => suma + e.value * (r.platillo(e.key)?.precio ?? 0));

  void _pedir() {
    final r = context.restauranteSinEscuchar;
    // La misma llave en un reintento no duplica el pedido (Idempotency-Key).
    final llave = _llave ??= '${widget.token}-${_intentos++}';
    final nota = _nota.text.trim();
    try {
      final resultado = r.pedirPorQr(
        widget.token,
        RestauranteDemo.dispositivoDemo,
        [
          for (final e in _carrito.entries)
            LineaPedidoQr(
              platilloId: e.key,
              cantidad: e.value,
              nota: nota.isEmpty ? null : nota,
              marcaAlergia: _alergia,
            ),
        ],
        llave: llave,
      );
      setState(() {
        _carrito.clear();
        _nota.clear();
        _alergia = false;
        _llave = null;
        _pestana = 1;
      });
      mostrarAviso(
        context,
        resultado.abrioCuenta
            ? 'Pedido enviado a cocina. Se abrió la cuenta de tu mesa.'
            : 'Pedido enviado a cocina.',
      );
    } on ReglaRestaurante catch (error) {
      mostrarError(context, error);
    }
  }

  Future<void> _pedirCuenta() async {
    final r = context.restauranteSinEscuchar;
    final ok = await confirmarDialogo(
      context,
      titulo: 'Pedir la cuenta',
      mensaje: 'Avisaremos al mesero y a caja. Después ya no podrás pedir desde el QR.',
      confirmar: 'Pedir la cuenta',
    );
    if (ok && mounted) {
      intentar(context, () => r.pedirCuentaPorQr(widget.token));
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final vista = r.mesaPorQr(widget.token);
    if (vista == null) {
      return const _QrInvalido();
    }
    final platillos = r.menuPublico() ?? const <Platillo>[];
    final pedido = r.estadoDePedidoQr(widget.token, RestauranteDemo.dispositivoDemo);
    final ancha = context.disposicion.esAncha;
    final tema = Theme.of(context);
    final total = _total(r);
    final cantidad = _carrito.values.fold(0, (a, b) => a + b);

    final cabecera = _CabeceraMesa(vista: vista);
    final carta = CartaPublica(
      platillos: platillos,
      cantidades: _carrito,
      alAgregar: vista.aceptaPedidos ? _sumar : null,
      alQuitar: _restar,
      encabezado: Padding(padding: const EdgeInsets.only(bottom: 12), child: _AvisoPedidos(vista: vista)),
    );
    final carrito = _Carrito(
      restaurante: r,
      carrito: _carrito,
      total: total,
      habilitado: vista.aceptaPedidos,
      nota: _nota,
      alergia: _alergia,
      alCambiarAlergia: (v) => setState(() => _alergia = v),
      alSumar: (p) => _sumar(p),
      alRestar: (p) => _restar(p),
      alPedir: _pedir,
    );
    final estado = _MiPedido(pedido: pedido, vista: vista, alPedirCuenta: _pedirCuenta);
    const infoRestaurante = _Restaurante();

    final cuerpoAncho = Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(flex: 6, child: _Pestanas(pestana: _pestana, alCambiar: (i) => setState(() => _pestana = i), ancha: true, hijos: [carta, estado, infoRestaurante])),
        VerticalDivider(width: 1, color: tema.colorScheme.outlineVariant),
        SizedBox(width: 380, child: carrito),
      ],
    );

    return Scaffold(
      appBar: AppBar(
        title: Text('Mesa ${vista.mesa}'),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: Center(child: Insignia(vista.aceptaPedidos ? 'Pedidos abiertos' : 'Solo carta', tono: vista.aceptaPedidos ? TonoInsignia.exito : TonoInsignia.neutro)),
          ),
        ],
      ),
      body: Column(
        children: [
          const EtiquetaDemo(detalle: 'así se ve al escanear el QR, con o sin la app'),
          cabecera,
          Expanded(
            child: ancha
                ? cuerpoAncho
                : _Pestanas(pestana: _pestana, alCambiar: (i) => setState(() => _pestana = i), ancha: false, hijos: [carta, estado, infoRestaurante]),
          ),
        ],
      ),
      bottomNavigationBar: ancha
          ? null
          : _pestana == 0 && cantidad > 0
              ? SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                    child: FilledButton.icon(
                      key: const Key('ver-pedido'),
                      onPressed: () => _abrirCarrito(carrito),
                      icon: Badge.count(count: cantidad, child: const Icon(Icons.shopping_basket_outlined)),
                      label: Text('Ver mi pedido · ${pesos(total)}'),
                    ),
                  ),
                )
              : null,
    );
  }

  void _abrirCarrito(Widget carrito) {
    final r = context.restauranteSinEscuchar;
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => StatefulBuilder(
        builder: (contexto, actualizar) => AlcanceRestaurante(
          restaurante: r,
          child: _Carrito(
            restaurante: r,
            carrito: _carrito,
            total: _total(r),
            habilitado: r.mesaPorQr(widget.token)?.aceptaPedidos ?? false,
            nota: _nota,
            alergia: _alergia,
            alCambiarAlergia: (v) {
              setState(() => _alergia = v);
              actualizar(() {});
            },
            alSumar: (p) {
              _sumar(p);
              actualizar(() {});
            },
            alRestar: (p) {
              _restar(p);
              actualizar(() {});
              if (_carrito.isEmpty) {
                Navigator.of(contexto).pop();
              }
            },
            alPedir: () {
              _pedir();
              if (_carrito.isEmpty) {
                Navigator.of(contexto).pop();
              }
            },
            enHoja: true,
          ),
        ),
      ),
    );
  }
}

class _CabeceraMesa extends StatelessWidget {
  const _CabeceraMesa({required this.vista});

  final VistaMesaQr vista;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
      decoration: BoxDecoration(
        color: tema.colorScheme.primary,
      ),
      child: ContenidoCentrado(
        anchoMaximo: double.infinity,
        padding: EdgeInsets.zero,
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.14), borderRadius: Formas.radioControl),
              child: Icon(Icons.restaurant, color: tema.colorScheme.onPrimary),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(vista.restaurante,
                      style: tema.textTheme.titleMedium?.copyWith(color: tema.colorScheme.onPrimary)),
                  Text('Mesa ${vista.mesa} · no necesitas cuenta ni la app',
                      style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onPrimary.withValues(alpha: 0.85))),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AvisoPedidos extends StatelessWidget {
  const _AvisoPedidos({required this.vista});

  final VistaMesaQr vista;

  @override
  Widget build(BuildContext context) {
    if (vista.aceptaPedidos) {
      return TarjetaConBarra(
        color: context.colores.exito,
        child: const Row(children: [
          Icon(Icons.bolt, size: 20),
          SizedBox(width: 10),
          Expanded(child: Text('Pide desde aquí: tu pedido llega directo a cocina y avisamos al mesero.')),
        ]),
      );
    }
    return TarjetaConBarra(
      color: context.colores.aviso,
      child: Row(children: [
        const Icon(Icons.info_outline, size: 20),
        const SizedBox(width: 10),
        Expanded(child: Text(plantillasDeError[vista.motivoNoAcepta] ?? 'Por ahora solo puedes ver la carta.')),
      ]),
    );
  }
}

/// Pestañas simples (Carta, Mi pedido, Restaurante) que conservan el estado de cada una.
class _Pestanas extends StatelessWidget {
  const _Pestanas({required this.pestana, required this.alCambiar, required this.hijos, required this.ancha});

  final int pestana;
  final ValueChanged<int> alCambiar;
  final List<Widget> hijos;
  final bool ancha;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
          child: SegmentedButton<int>(
            showSelectedIcon: false,
            segments: const [
              ButtonSegment(value: 0, label: Text('Carta'), icon: Icon(Icons.menu_book_outlined, size: 18)),
              ButtonSegment(value: 1, label: Text('Mi pedido'), icon: Icon(Icons.receipt_long_outlined, size: 18)),
              ButtonSegment(value: 2, label: Text('Info'), icon: Icon(Icons.storefront_outlined, size: 18)),
            ],
            selected: {pestana},
            onSelectionChanged: (s) => alCambiar(s.first),
          ),
        ),
        Expanded(child: IndexedStack(index: pestana, children: hijos)),
      ],
    );
  }
}

class _Carrito extends StatelessWidget {
  const _Carrito({
    required this.restaurante,
    required this.carrito,
    required this.total,
    required this.habilitado,
    required this.nota,
    required this.alergia,
    required this.alCambiarAlergia,
    required this.alSumar,
    required this.alRestar,
    required this.alPedir,
    this.enHoja = false,
  });

  final RestauranteDemo restaurante;
  final Map<String, int> carrito;
  final int total;
  final bool habilitado;
  final TextEditingController nota;
  final bool alergia;
  final ValueChanged<bool> alCambiarAlergia;
  final ValueChanged<Platillo> alSumar;
  final ValueChanged<Platillo> alRestar;
  final VoidCallback alPedir;
  final bool enHoja;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final lineas = carrito.entries.length;
    final cuerpo = Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
          child: Text('Tu pedido', style: tema.textTheme.titleLarge),
        ),
        if (carrito.isEmpty)
          const Padding(
            padding: EdgeInsets.all(24),
            child: EstadoVacio(icono: Icons.shopping_basket_outlined, titulo: 'Aún no agregas nada', mensaje: 'Elige platillos de la carta.'),
          )
        else
          Flexible(
            child: ListView(
              shrinkWrap: true,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              children: [
                for (final e in carrito.entries)
                  if (restaurante.platillo(e.key) case final p?)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 6),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(p.nombre),
                                Text(pesos(p.precio * e.value), style: tema.textTheme.bodySmall),
                              ],
                            ),
                          ),
                          IconButton.outlined(onPressed: () => alRestar(p), icon: const Icon(Icons.remove, size: 18)),
                          SizedBox(width: 36, child: Text('${e.value}', textAlign: TextAlign.center, style: tema.textTheme.titleSmall)),
                          IconButton.outlined(onPressed: () => alSumar(p), icon: const Icon(Icons.add, size: 18)),
                        ],
                      ),
                    ),
                const SizedBox(height: 8),
                TextField(
                  key: const Key('nota-pedido'),
                  controller: nota,
                  maxLength: 80,
                  decoration: const InputDecoration(labelText: 'Nota para cocina (opcional)', isDense: true),
                ),
                SwitchListTile(
                  key: const Key('alergia-pedido'),
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Tengo una alergia'),
                  subtitle: const Text('Cocina verá una marca de alergia. Escribe cuál en la nota.'),
                  value: alergia,
                  onChanged: alCambiarAlergia,
                ),
              ],
            ),
          ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(children: [
                Expanded(child: Text('Total estimado', style: tema.textTheme.titleMedium)),
                Text(pesos(total), style: tema.textTheme.titleLarge),
              ]),
              const SizedBox(height: 4),
              Text('Pagas en caja o con el mesero. $leyendaPrecios',
                  style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
              const SizedBox(height: 12),
              FilledButton.icon(
                key: const Key('enviar-pedido'),
                onPressed: habilitado && lineas > 0 ? alPedir : null,
                icon: const Icon(Icons.send_outlined),
                label: Text(habilitado ? 'Enviar a cocina' : 'Pedidos no disponibles'),
              ),
            ],
          ),
        ),
      ],
    );
    return enHoja
        ? SafeArea(child: Padding(padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom), child: cuerpo))
        : cuerpo;
  }
}

class _MiPedido extends StatelessWidget {
  const _MiPedido({required this.pedido, required this.vista, required this.alPedirCuenta});

  final PedidoQrVista? pedido;
  final VistaMesaQr vista;
  final VoidCallback alPedirCuenta;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final c = context.colores;
    final lineas = pedido?.lineas ?? const <LineaVistaQr>[];
    final solicitada = pedido?.cuenta == EstadoCuentaQr.solicitada;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (solicitada)
          TarjetaConBarra(
            color: c.info,
            child: const Row(children: [
              Icon(Icons.receipt_long, size: 20),
              SizedBox(width: 10),
              Expanded(child: Text('Pediste la cuenta. Un mesero va en camino para cobrar.')),
            ]),
          ),
        if (lineas.isEmpty)
          const Padding(
            padding: EdgeInsets.only(top: 40),
            child: EstadoVacio(icono: Icons.receipt_long_outlined, titulo: 'Todavía no has pedido', mensaje: 'Cuando envíes un pedido, aquí verás cómo va.'),
          ),
        for (final l in lineas)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: TarjetaConBarra(
              color: switch (l.estado) {
                EstadoVistaLinea.enCocina => c.info,
                EstadoVistaLinea.preparando => c.aviso,
                EstadoVistaLinea.listo || EstadoVistaLinea.entregado => c.exito,
                EstadoVistaLinea.cancelado => c.peligro,
              },
              relleno: l.estado == EstadoVistaLinea.cancelado ? c.peligroSuave : null,
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('${l.cantidad}× ${l.nombre}',
                            style: tema.textTheme.titleSmall?.copyWith(decoration: l.estado == EstadoVistaLinea.cancelado ? TextDecoration.lineThrough : null)),
                        if (l.mensaje != null) Text(l.mensaje!, style: tema.textTheme.bodySmall?.copyWith(color: c.peligro)),
                      ],
                    ),
                  ),
                  Insignia(
                    l.estado.etiqueta,
                    tono: switch (l.estado) {
                      EstadoVistaLinea.enCocina => TonoInsignia.info,
                      EstadoVistaLinea.preparando => TonoInsignia.aviso,
                      EstadoVistaLinea.listo || EstadoVistaLinea.entregado => TonoInsignia.exito,
                      EstadoVistaLinea.cancelado => TonoInsignia.peligro,
                    },
                  ),
                ],
              ),
            ),
          ),
        const SizedBox(height: 16),
        Text('Se actualiza solo cada pocos segundos. En la maqueta cambia al instante.',
            style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
        const SizedBox(height: 16),
        if (lineas.any((l) => l.estado != EstadoVistaLinea.cancelado) && !solicitada)
          FilledButton.tonalIcon(
            key: const Key('pedir-cuenta-qr'),
            onPressed: alPedirCuenta,
            icon: const Icon(Icons.receipt_long_outlined),
            label: const Text('Pedir la cuenta'),
          ),
      ],
    );
  }
}

class _Restaurante extends StatelessWidget {
  const _Restaurante();

  @override
  Widget build(BuildContext context) {
    final ficha = directorioDemo.first;
    final tema = Theme.of(context);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(ficha.nombre, style: tema.textTheme.headlineSmall),
        const SizedBox(height: 4),
        Text(ficha.descripcion, style: tema.textTheme.bodyLarge),
        const SizedBox(height: 12),
        ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.place_outlined), title: Text(ficha.zona)),
        ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.schedule), title: Text(ficha.horario)),
        const SizedBox(height: 12),
        ResenasRestaurante(resenas: ficha.resenas),
      ],
    );
  }
}

class _QrInvalido extends StatelessWidget {
  const _QrInvalido();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Código QR')),
      body: const EstadoVacio(
        icono: Icons.qr_code_scanner,
        titulo: 'Este código QR no es válido',
        mensaje: 'Puede que haya cambiado o que el restaurante no esté disponible. Pide ayuda a quien te atiende.',
      ),
    );
  }
}
