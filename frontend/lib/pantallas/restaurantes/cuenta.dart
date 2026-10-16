import 'package:flutter/material.dart';

import '../../estado/restaurante_demo.dart';
import '../../modelos/dinero.dart';
import '../../modelos/formato.dart';
import '../../modelos/permisos_restaurante.dart';
import '../../modelos/restaurantes.dart';
import '../../tema.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/componentes.dart';
import 'alcance.dart';

/// Cuenta de una mesa como pantalla completa (teléfono).
class PantallaCuenta extends StatelessWidget {
  const PantallaCuenta({super.key, required this.cuentaId});

  final String cuentaId;

  @override
  Widget build(BuildContext context) {
    final cuenta = context.restaurante.cuenta(cuentaId);
    final mesa = cuenta?.mesaId == null ? null : context.restaurante.mesa(cuenta!.mesaId!);
    return Scaffold(
      appBar: AppBar(title: Text(mesa == null ? 'Para llevar' : 'Mesa ${mesa.etiqueta}')),
      body: PanelCuenta(cuentaId: cuentaId),
    );
  }
}

/// La cuenta de una mesa: líneas con su estado, envío a cocina, pedir la cuenta y cobro (CT-01 a CT-15).
class PanelCuenta extends StatelessWidget {
  const PanelCuenta({super.key, required this.cuentaId, this.enPanel = false});

  final String cuentaId;
  final bool enPanel;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final cuenta = r.cuenta(cuentaId);
    if (cuenta == null) {
      return const EstadoVacio(icono: Icons.receipt_long_outlined, titulo: 'No se encontró la cuenta');
    }
    final tema = Theme.of(context);
    final mesa = cuenta.mesaId == null ? null : r.mesa(cuenta.mesaId!);
    final pendientes = cuenta.lineas.where((l) => l.estado == EstadoLinea.pendiente).toList();
    final enviadas = cuenta.lineas.where((l) => l.estado == EstadoLinea.enviada).toList();
    final anuladas = cuenta.lineas.where((l) => l.estado == EstadoLinea.anulada).toList();
    final editable = cuenta.estado == EstadoCuenta.abierta;

    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(mesa == null ? 'Para llevar' : 'Mesa ${mesa.etiqueta}', style: tema.textTheme.headlineSmall),
                        Text(
                          'Cuenta ${cuenta.numero}'
                          '${cuenta.comensales == null ? '' : ' · ${cuenta.comensales} comensales'}'
                          ' · abierta ${horaTexto(cuenta.abiertaEn)}',
                          style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                        ),
                      ],
                    ),
                  ),
                  Insignia(
                    cuenta.estado.etiqueta,
                    tono: switch (cuenta.estado) {
                      EstadoCuenta.abierta => TonoInsignia.aviso,
                      EstadoCuenta.porCobrar => TonoInsignia.primario,
                      EstadoCuenta.pagada => TonoInsignia.exito,
                      EstadoCuenta.cancelada => TonoInsignia.neutro,
                    },
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Wrap(
                spacing: 8,
                runSpacing: 6,
                children: [
                  if (cuenta.origen == OrigenPedido.qr)
                    const Insignia('Abierta por QR', tono: TonoInsignia.info, icono: Icons.qr_code_2),
                  if (cuenta.origen == OrigenPedido.qr && !cuenta.presenciaConfirmada)
                    const Insignia('Sin atender', tono: TonoInsignia.aviso, icono: Icons.notifications_active_outlined, solida: true),
                  if (cuenta.ventaFolio != null) Insignia('Venta ${cuenta.ventaFolio}', tono: TonoInsignia.exito),
                ],
              ),
              if (cuenta.origen == OrigenPedido.qr && !cuenta.presenciaConfirmada && r.puede(Permiso.cuentaAbrir) && cuenta.estado.noPagada)
                Padding(
                  padding: const EdgeInsets.only(top: 10),
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: OutlinedButton.icon(
                      key: const Key('confirmar-presencia'),
                      onPressed: () => intentar(context, () => r.confirmarPresencia(cuenta.id)),
                      icon: const Icon(Icons.how_to_reg_outlined),
                      label: const Text('Ya estoy con la mesa'),
                    ),
                  ),
                ),
              const SizedBox(height: 16),
              if (cuenta.lineas.isEmpty)
                const Card(child: Padding(padding: EdgeInsets.all(20), child: Text('La cuenta todavía no tiene líneas. Agrega un platillo.'))),
              if (pendientes.isNotEmpty)
                _GrupoLineas(titulo: 'Por enviar a cocina', cuenta: cuenta, lineas: pendientes),
              if (enviadas.isNotEmpty) _GrupoLineas(titulo: 'En servicio', cuenta: cuenta, lineas: enviadas),
              if (anuladas.isNotEmpty) _GrupoLineas(titulo: 'Anuladas', cuenta: cuenta, lineas: anuladas),
              const SizedBox(height: 8),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    children: [
                      Row(
                        children: [
                          Expanded(child: Text('Total', style: tema.textTheme.titleMedium)),
                          Text(pesos(cuenta.total), key: const Key('total-cuenta'), style: tema.textTheme.headlineSmall),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Con los precios de referencia. Ventas cobra con el precio vigente al confirmar (CT-13).',
                        style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
        if (cuenta.estado.noPagada) _BarraAcciones(cuenta: cuenta, pendientes: pendientes.length, editable: editable),
      ],
    );
  }
}

class _GrupoLineas extends StatelessWidget {
  const _GrupoLineas({required this.titulo, required this.cuenta, required this.lineas});

  final String titulo;
  final Cuenta cuenta;
  final List<LineaCuenta> lineas;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(titulo.toUpperCase(),
              style: Theme.of(context)
                  .textTheme
                  .labelMedium
                  ?.copyWith(letterSpacing: 0.8, color: Theme.of(context).colorScheme.onSurfaceVariant)),
          const SizedBox(height: 8),
          for (final l in lineas)
            Padding(padding: const EdgeInsets.only(bottom: 8), child: _FilaLinea(cuenta: cuenta, linea: l)),
        ],
      ),
    );
  }
}

class _FilaLinea extends StatelessWidget {
  const _FilaLinea({required this.cuenta, required this.linea});

  final Cuenta cuenta;
  final LineaCuenta linea;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final c = context.colores;
    final anulada = linea.estado == EstadoLinea.anulada;
    final comanda = linea.comandaId == null ? null : r.comanda(linea.comandaId!);
    final color = switch (linea.estado) {
      EstadoLinea.pendiente => c.neutro,
      EstadoLinea.enviada => switch (comanda?.estado) {
          EstadoComanda.lista || EstadoComanda.entregada => c.exito,
          EstadoComanda.enPreparacion => c.aviso,
          _ => c.info,
        },
      EstadoLinea.anulada => c.peligro,
    };
    final estacion = comanda?.estacion.etiqueta;
    return TarjetaConBarra(
      color: color,
      relleno: anulada ? c.peligroSuave : null,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(color: tema.colorScheme.surfaceContainerHighest, borderRadius: Formas.radioAfilado),
            child: Text('${linea.cantidad}×', style: tema.textTheme.labelLarge),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(linea.nombre,
                    style: tema.textTheme.titleSmall?.copyWith(decoration: anulada ? TextDecoration.lineThrough : null)),
                if (linea.nota != null)
                  Text('Nota: ${linea.nota}', style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
                const SizedBox(height: 4),
                Wrap(
                  spacing: 6,
                  runSpacing: 4,
                  children: [
                    if (linea.marcaAlergia) const Insignia('Alergia', tono: TonoInsignia.peligro, icono: Icons.warning_amber_rounded, solida: true),
                    if (linea.origen == OrigenPedido.qr) const Insignia('QR', tono: TonoInsignia.info),
                    if (!anulada && comanda != null) Insignia('$estacion · ${comanda.estado.etiqueta}', tono: TonoInsignia.neutro),
                    if (linea.estado == EstadoLinea.pendiente) const Insignia('Pendiente', tono: TonoInsignia.neutro),
                    if (anulada)
                      Insignia(
                          linea.motivo == MotivoAnulacion.otro
                              ? 'Anulada: ${linea.motivoTexto}'
                              : 'Anulada: ${linea.motivo?.etiqueta.toLowerCase()}',
                          tono: TonoInsignia.peligro),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(pesos(linea.importe),
                  style: tema.textTheme.titleSmall?.copyWith(decoration: anulada ? TextDecoration.lineThrough : null)),
              if (linea.estado == EstadoLinea.pendiente && cuenta.estado == EstadoCuenta.abierta)
                IconButton(
                  key: Key('quitar-${linea.nombre}'),
                  tooltip: 'Quitar',
                  visualDensity: VisualDensity.compact,
                  onPressed: () => intentar(context, () => r.quitarLineaPendiente(cuenta.id, linea.id)),
                  icon: const Icon(Icons.delete_outline, size: 20),
                ),
              if (linea.estado == EstadoLinea.enviada && cuenta.estado.noPagada)
                IconButton(
                  key: Key('anular-${linea.nombre}'),
                  tooltip: 'Anular con motivo',
                  visualDensity: VisualDensity.compact,
                  onPressed: () => _anular(context, r, cuenta, linea, comanda),
                  icon: const Icon(Icons.cancel_outlined, size: 20),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _anular(BuildContext context, RestauranteDemo r, Cuenta cuenta, LineaCuenta linea, Comanda? comanda) async {
    final yaPreparada = comanda != null && (comanda.estado == EstadoComanda.lista || comanda.estado == EstadoComanda.entregada);
    final motivo = await showDialog<(MotivoAnulacion, String?)>(
      context: context,
      builder: (_) => _DialogoMotivoAnulacion(
        titulo: 'Anular ${linea.cantidad}× ${linea.nombre}',
        aviso: yaPreparada ? 'Ya está preparado: hay merma. Anular exige permiso de gerente en adelante (CT-03).' : null,
        textoConfirmar: 'Anular línea',
      ),
    );
    if (motivo != null && context.mounted) {
      intentar(context, () => r.anularLinea(cuenta.id, linea.id, motivo.$1, texto: motivo.$2));
    }
  }
}

/// Pide el motivo de anulación entre los códigos del contrato (QR-10). El texto libre solo acompaña a «Otro».
class _DialogoMotivoAnulacion extends StatefulWidget {
  const _DialogoMotivoAnulacion({required this.titulo, required this.textoConfirmar, this.aviso});

  final String titulo;
  final String textoConfirmar;
  final String? aviso;

  @override
  State<_DialogoMotivoAnulacion> createState() => _DialogoMotivoAnulacionState();
}

class _DialogoMotivoAnulacionState extends State<_DialogoMotivoAnulacion> {
  MotivoAnulacion? _motivo;
  final _texto = TextEditingController();
  String? _error;

  @override
  void dispose() {
    _texto.dispose();
    super.dispose();
  }

  void _aceptar() {
    if (_motivo == null) {
      setState(() => _error = 'Elige un motivo.');
      return;
    }
    if (_motivo == MotivoAnulacion.otro && _texto.text.trim().isEmpty) {
      setState(() => _error = 'Escribe el motivo.');
      return;
    }
    Navigator.of(context).pop((_motivo!, _motivo == MotivoAnulacion.otro ? _texto.text.trim() : null));
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.titulo),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (widget.aviso != null) ...[
              Text(widget.aviso!, style: TextStyle(color: context.colores.aviso)),
              const SizedBox(height: 8),
            ],
            const Text('El comensal verá el motivo en su pantalla.'),
            RadioGroup<MotivoAnulacion>(
              groupValue: _motivo,
              onChanged: (v) => setState(() {
                _motivo = v;
                _error = null;
              }),
              child: Column(
                children: [
                  for (final m in MotivoAnulacion.values)
                    RadioListTile<MotivoAnulacion>(
                      key: Key('motivo-${m.codigo}'),
                      value: m,
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                      title: Text(m.etiqueta),
                    ),
                ],
              ),
            ),
            if (_motivo == MotivoAnulacion.otro)
              TextField(
                key: const Key('motivo-texto'),
                controller: _texto,
                maxLength: 80,
                decoration: const InputDecoration(labelText: 'Motivo (hasta 80 caracteres)'),
              ),
            if (_error != null) Text(_error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ],
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Volver')),
        FilledButton(
          key: const Key('confirmar-anulacion'),
          style: FilledButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.error),
          onPressed: _aceptar,
          child: Text(widget.textoConfirmar),
        ),
      ],
    );
  }
}

class _BarraAcciones extends StatelessWidget {
  const _BarraAcciones({required this.cuenta, required this.pendientes, required this.editable});

  final Cuenta cuenta;
  final int pendientes;
  final bool editable;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final puedeOperar = r.puede(Permiso.cuentaAbrir);
    final puedeCobrar = r.puede(Permiso.cobrar);
    final botones = <Widget>[
      if (editable && puedeOperar)
        OutlinedButton.icon(
          key: const Key('agregar-platillo'),
          onPressed: () => _agregar(context, cuenta.id),
          icon: const Icon(Icons.add),
          label: const Text('Agregar'),
        ),
      if (editable && puedeOperar)
        FilledButton.icon(
          key: const Key('enviar-cocina'),
          onPressed: pendientes == 0 ? null : () => intentar(context, () => r.enviarACocina(cuenta.id)),
          icon: const Icon(Icons.send_outlined),
          label: Text('Enviar a cocina${pendientes == 0 ? '' : ' ($pendientes)'}'),
        ),
      if (editable && puedeOperar)
        FilledButton.tonalIcon(
          key: const Key('pedir-cuenta'),
          onPressed: () => intentar(context, () => r.pedirCuenta(cuenta.id)),
          icon: const Icon(Icons.receipt_long_outlined),
          label: const Text('Pedir la cuenta'),
        ),
      if (cuenta.estado == EstadoCuenta.porCobrar && puedeOperar)
        OutlinedButton.icon(
          key: const Key('reabrir-cuenta'),
          onPressed: () => intentar(context, () => r.reabrirCuenta(cuenta.id)),
          icon: const Icon(Icons.undo),
          label: const Text('Reabrir'),
        ),
      if (cuenta.estado == EstadoCuenta.porCobrar && puedeCobrar)
        FilledButton.icon(
          key: const Key('cobrar'),
          style: FilledButton.styleFrom(backgroundColor: context.colores.exito),
          onPressed: () => mostrarCobro(context, cuenta.id),
          icon: const Icon(Icons.payments_outlined),
          label: Text('Cobrar ${pesos(cuenta.total)}'),
        ),
      if (cuenta.estado == EstadoCuenta.porCobrar && !puedeCobrar)
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Text('Esperando a caja para cobrar.', style: tema.textTheme.bodyMedium),
        ),
      if (r.puede(Permiso.cuentaCancelar))
        TextButton.icon(
          key: const Key('cancelar-cuenta'),
          style: TextButton.styleFrom(foregroundColor: tema.colorScheme.error),
          onPressed: () => _cancelar(context, r, cuenta),
          icon: const Icon(Icons.close),
          label: const Text('Cancelar cuenta'),
        ),
    ];
    return DecoratedBox(
      decoration: BoxDecoration(
        color: tema.colorScheme.surface,
        border: Border(top: BorderSide(color: tema.colorScheme.outlineVariant)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Wrap(spacing: 8, runSpacing: 8, alignment: WrapAlignment.end, children: botones),
        ),
      ),
    );
  }

  Future<void> _agregar(BuildContext context, String cuentaId) async {
    final r = context.restauranteSinEscuchar;
    final contenido = AlcanceRestaurante(restaurante: r, child: _SelectorPlatillo(cuentaId: cuentaId));
    if (context.disposicion.esAncha) {
      await showDialog<void>(
        context: context,
        builder: (_) => Dialog(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 560, maxHeight: 640), child: contenido)),
      );
    } else {
      await showModalBottomSheet<void>(context: context, isScrollControlled: true, useSafeArea: true, builder: (_) => contenido);
    }
  }

  Future<void> _cancelar(BuildContext context, RestauranteDemo r, Cuenta cuenta) async {
    final motivo = await showDialog<(MotivoAnulacion, String?)>(
      context: context,
      builder: (_) => _DialogoMotivoAnulacion(
        titulo: 'Cancelar la cuenta ${cuenta.numero}',
        aviso: 'Se anulan todas las líneas y la mesa queda libre.',
        textoConfirmar: 'Cancelar cuenta',
      ),
    );
    if (motivo != null && context.mounted) {
      intentar(context, () => r.cancelarCuenta(cuenta.id, motivo.$1, texto: motivo.$2));
    }
  }
}

/// Elegir un platillo para la cuenta. Los que no se pueden pedir hoy salen apagados con su motivo (MN-05, MN-06).
class _SelectorPlatillo extends StatefulWidget {
  const _SelectorPlatillo({required this.cuentaId});

  final String cuentaId;

  @override
  State<_SelectorPlatillo> createState() => _SelectorPlatilloState();
}

class _SelectorPlatilloState extends State<_SelectorPlatillo> {
  String _texto = '';
  Platillo? _elegido;
  int _cantidad = 1;
  bool _alergia = false;
  final _nota = TextEditingController();

  @override
  void dispose() {
    _nota.dispose();
    super.dispose();
  }

  void _agregar() {
    final r = context.restauranteSinEscuchar;
    final ok = intentar(context, () => r.agregarLinea(widget.cuentaId, _elegido!.id, _cantidad,
        nota: _nota.text, marcaAlergia: _alergia));
    if (ok) {
      Navigator.of(context).pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final platillos = r.platillos.where((p) => p.nombre.toLowerCase().contains(_texto.toLowerCase())).toList();
    if (_elegido != null) {
      final p = _elegido!;
      return SafeArea(
        child: Padding(
          padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(children: [
                  IconButton(onPressed: () => setState(() => _elegido = null), icon: const Icon(Icons.arrow_back)),
                  Expanded(child: Text(p.nombre, style: tema.textTheme.titleLarge)),
                  Text(pesos(p.precio), style: tema.textTheme.titleMedium),
                ]),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    IconButton.outlined(
                      key: const Key('menos-cantidad'),
                      onPressed: _cantidad > 1 ? () => setState(() => _cantidad--) : null,
                      icon: const Icon(Icons.remove),
                    ),
                    SizedBox(width: 80, child: Text('$_cantidad', textAlign: TextAlign.center, style: tema.textTheme.headlineMedium)),
                    IconButton.outlined(
                      key: const Key('mas-cantidad'),
                      onPressed: _cantidad < 99 ? () => setState(() => _cantidad++) : null,
                      icon: const Icon(Icons.add),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                TextField(
                  key: const Key('nota-linea'),
                  controller: _nota,
                  maxLength: 80,
                  decoration: const InputDecoration(labelText: 'Nota para cocina (opcional)'),
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Marcar alergia'),
                  subtitle: const Text('Se resalta en el tablero de cocina. No se guarda detalle médico (CD-06).'),
                  value: _alergia,
                  onChanged: (v) => setState(() => _alergia = v),
                ),
                const SizedBox(height: 8),
                FilledButton(
                  key: const Key('confirmar-linea'),
                  onPressed: _agregar,
                  child: Text('Agregar · ${pesos(p.precio * _cantidad)}'),
                ),
              ],
            ),
          ),
        ),
      );
    }
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
          child: TextField(
            key: const Key('buscar-en-cuenta'),
            onChanged: (t) => setState(() => _texto = t),
            decoration: const InputDecoration(hintText: 'Buscar platillo', prefixIcon: Icon(Icons.search), isDense: true),
          ),
        ),
        Flexible(
          child: ListView(
            shrinkWrap: true,
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            children: [
              for (final p in platillos)
                Builder(builder: (context) {
                  final motivo = r.motivoNoOrdenable(p);
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Card(
                      child: ListTile(
                        key: Key('elegir-${p.nombre}'),
                        enabled: motivo == null,
                        leading: FotoPlatillo(platillo: p, estacion: r.estacionDe(p), lado: 44),
                        title: Text(p.nombre),
                        subtitle: motivo == null
                            ? Text(pesos(p.precio))
                            : Align(
                                alignment: Alignment.centerLeft,
                                child: Insignia(
                                  motivo == 'PLATILLO_AGOTADO' ? 'Agotado' : 'Fuera de temporada',
                                  tono: motivo == 'PLATILLO_AGOTADO' ? TonoInsignia.peligro : TonoInsignia.neutro,
                                ),
                              ),
                        trailing: motivo == null ? const Icon(Icons.add_circle_outline) : null,
                        onTap: motivo == null ? () => setState(() => _elegido = p) : null,
                      ),
                    ),
                  );
                }),
            ],
          ),
        ),
      ],
    );
  }
}

/// Cobro de una cuenta por cobrar: preparar → confirmar. La maqueta simula a Ventas, que registra la venta; después
/// Restaurantes marca la cuenta pagada, libera la mesa y descuenta los insumos por receta.
Future<void> mostrarCobro(BuildContext context, String cuentaId) {
  final r = context.restauranteSinEscuchar;
  return showDialog<void>(
    context: context,
    barrierDismissible: false,
    builder: (_) => AlcanceRestaurante(restaurante: r, child: _DialogoCobro(cuentaId: cuentaId)),
  );
}

class _DialogoCobro extends StatefulWidget {
  const _DialogoCobro({required this.cuentaId});

  final String cuentaId;

  @override
  State<_DialogoCobro> createState() => _DialogoCobroState();
}

class _DialogoCobroState extends State<_DialogoCobro> {
  String _metodo = 'Efectivo';
  ResultadoCobro? _resultado;

  void _confirmar() {
    final r = context.restauranteSinEscuchar;
    ResultadoCobro? resultado;
    if (intentar(context, () => resultado = r.cobrar(widget.cuentaId, _metodo))) {
      setState(() => _resultado = resultado);
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = context.restauranteSinEscuchar;
    final cuenta = r.cuenta(widget.cuentaId);
    final tema = Theme.of(context);
    final resultado = _resultado;
    if (resultado != null) {
      return AlertDialog(
        icon: Icon(Icons.check_circle_outline, color: context.colores.exito, size: 36),
        title: Text('Venta ${resultado.venta.folio} registrada'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Total ${pesos(resultado.venta.total)} · ${resultado.venta.metodo}', style: tema.textTheme.titleMedium),
              const SizedBox(height: 4),
              const Text('La cuenta quedó pagada y la mesa está libre.'),
              const SizedBox(height: 14),
              if (resultado.consumoFallido != null)
                TarjetaConBarra(
                  color: context.colores.peligro,
                  child: Text('No se pudieron descontar los insumos: ${resultado.consumoFallido!.detalle} '
                      'La venta no se afecta; queda en «Insumos» para revisarla.'),
                )
              else if (resultado.descontados.isEmpty)
                const Text('Ningún platillo de esta venta descuenta por receta.')
              else ...[
                Text('Insumos descontados', style: tema.textTheme.titleSmall),
                const SizedBox(height: 6),
                for (final i in resultado.descontados)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    child: Row(children: [
                      Expanded(child: Text(i.nombre)),
                      Text(cantidadTexto(i.cantidad, i.unidad), style: tema.textTheme.labelLarge),
                    ]),
                  ),
              ],
            ],
          ),
        ),
        actions: [
          FilledButton(key: const Key('cerrar-cobro'), onPressed: () => Navigator.of(context).pop(), child: const Text('Listo')),
        ],
      );
    }
    return AlertDialog(
      title: Text('Cobrar la cuenta ${cuenta?.numero ?? ''}'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(pesos(cuenta?.total ?? 0), style: tema.textTheme.displaySmall?.copyWith(color: tema.colorScheme.primary)),
            const SizedBox(height: 12),
            Text('Forma de pago', style: tema.textTheme.labelLarge),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: [
                for (final m in const ['Efectivo', 'Tarjeta', 'Transferencia'])
                  ChoiceChip(
                    key: Key('pago-$m'),
                    label: Text(m),
                    selected: _metodo == m,
                    onSelected: (_) => setState(() => _metodo = m),
                  ),
              ],
            ),
            const SizedBox(height: 14),
            Text(
              'Se registrará la venta en Ventas con la referencia de la cuenta. Después se marca pagada, se libera la mesa y se descuentan los insumos de las recetas.',
              style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Volver')),
        FilledButton(key: const Key('confirmar-cobro-cuenta'), onPressed: _confirmar, child: const Text('Confirmar cobro')),
      ],
    );
  }
}
