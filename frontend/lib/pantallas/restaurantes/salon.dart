import 'package:flutter/material.dart';

import '../../modelos/dinero.dart';
import '../../modelos/formato.dart';
import '../../modelos/permisos_restaurante.dart';
import '../../modelos/restaurantes.dart';
import '../../tema.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/componentes.dart';
import 'alcance.dart';
import 'cuenta.dart';
import 'mesa_qr.dart';

/// Salón: las mesas con su estado, la disponibilidad que se publica y la cuenta de la mesa elegida. En pantallas
/// anchas la cuenta se ve al lado; en teléfono se abre en otra pantalla.
class Salon extends StatefulWidget {
  const Salon({super.key});

  @override
  State<Salon> createState() => _SalonState();
}

class _SalonState extends State<Salon> {
  String? _zonaId;
  String? _mesaElegidaId;
  bool _mostradorElegido = false;
  String? _cuentaMostradorId;

  Future<void> _tocarMesa(Mesa mesa) async {
    final r = context.restauranteSinEscuchar;
    final estado = r.estadoDe(mesa);
    final ancha = context.disposicion.esAncha;
    if (estado == EstadoMesa.bloqueada) {
      mostrarAviso(context, 'La mesa ${mesa.etiqueta} está bloqueada.');
      return;
    }
    var cuenta = r.cuentaActivaDe(mesa.id);
    if (cuenta == null) {
      if (!r.puede(Permiso.cuentaAbrir)) {
        mostrarAviso(context, 'Tu puesto no abre cuentas. Pide al mesero.');
        return;
      }
      final comensales = await _pedirComensales(mesa);
      if (comensales == null || !mounted) {
        return;
      }
      Cuenta? nueva;
      intentar(context, () => nueva = r.abrirCuenta(mesaId: mesa.id, comensales: comensales));
      cuenta = nueva;
      if (cuenta == null) {
        return;
      }
    }
    if (ancha) {
      setState(() {
        _mesaElegidaId = mesa.id;
        _mostradorElegido = false;
      });
    } else {
      final id = cuenta.id;
      await abrirPantalla<void>(context, (_) => PantallaCuenta(cuentaId: id));
    }
  }

  Future<int?> _pedirComensales(Mesa mesa) {
    return showDialog<int>(
      context: context,
      builder: (_) => _DialogoComensales(mesa: mesa),
    );
  }

  Future<void> _abrirMostrador() async {
    final r = context.restauranteSinEscuchar;
    Cuenta? nueva;
    if (!intentar(context, () => nueva = r.abrirCuenta(comensales: 1)) || nueva == null) {
      return;
    }
    final id = nueva!.id;
    if (context.disposicion.esAncha) {
      setState(() {
        _mostradorElegido = true;
        _cuentaMostradorId = id;
        _mesaElegidaId = null;
      });
    } else {
      await abrirPantalla<void>(context, (_) => PantallaCuenta(cuentaId: id));
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final ancha = context.disposicion.esAncha;
    final zonas = r.zonas;
    final mesas = r.mesas.where((m) => _zonaId == null || m.zonaId == _zonaId).toList();
    final disp = r.disponibilidad;
    final tema = Theme.of(context);
    final avisos = r.avisos.where((a) => !a.atendido).take(2).toList();
    final admiteMostrador = r.modoServicio != ModoServicio.mesas && r.puede(Permiso.cuentaAbrir);

    final mesaElegida = _mesaElegidaId == null ? null : r.mesa(_mesaElegidaId!);
    final cuentaPanelId = _mostradorElegido
        ? _cuentaMostradorId
        : (mesaElegida == null ? null : r.cuentaActivaDe(mesaElegida.id)?.id);

    final contenidoMesas = ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Mesas libres: ${disp.libres} de ${disp.total}', style: tema.textTheme.titleLarge, key: const Key('mesas-libres')),
                  Text(
                    disp.abierto ? 'Esto es lo que ve el turista en la ficha.' : 'Servicio cerrado: el turista ve que no hay atención.',
                    style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            if (admiteMostrador)
              OutlinedButton.icon(
                key: const Key('abrir-mostrador'),
                onPressed: _abrirMostrador,
                icon: const Icon(Icons.takeout_dining_outlined),
                label: const Text('Para llevar'),
              ),
          ],
        ),
        const SizedBox(height: 12),
        for (final aviso in avisos)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: TarjetaConBarra(
              color: context.colores.aviso,
              child: Row(
                children: [
                  const Icon(Icons.notifications_active_outlined, size: 20),
                  const SizedBox(width: 10),
                  Expanded(child: Text(aviso.texto)),
                  TextButton(onPressed: () => r.atenderAviso(aviso), child: const Text('Atendido')),
                ],
              ),
            ),
          ),
        const SizedBox(height: 4),
        SizedBox(
          height: 44,
          child: ListView(
            scrollDirection: Axis.horizontal,
            children: [
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(label: const Text('Todas'), selected: _zonaId == null, onSelected: (_) => setState(() => _zonaId = null)),
              ),
              for (final z in zonas)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: Text(z.nombre),
                    selected: _zonaId == z.id,
                    onSelected: (_) => setState(() => _zonaId = z.id),
                  ),
                ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        const _Leyenda(),
        const SizedBox(height: 12),
        LayoutBuilder(
          builder: (context, restricciones) {
            const separacion = 12.0;
            final minimo = ancha ? 170.0 : 150.0;
            final columnas = (restricciones.maxWidth / (minimo + separacion)).floor().clamp(2, 6);
            final anchoTarjeta = (restricciones.maxWidth - separacion * (columnas - 1)) / columnas;
            return Wrap(
              spacing: separacion,
              runSpacing: separacion,
              children: [
                for (final m in mesas)
                  SizedBox(
                    width: anchoTarjeta,
                    child: _TarjetaMesa(
                      mesa: m,
                      elegida: _mesaElegidaId == m.id && !_mostradorElegido,
                      alTocar: () => _tocarMesa(m),
                    ),
                  ),
              ],
            );
          },
        ),
      ],
    );

    if (!ancha) {
      return contenidoMesas;
    }
    return Row(
      children: [
        Expanded(flex: 5, child: contenidoMesas),
        VerticalDivider(width: 1, color: tema.colorScheme.outlineVariant),
        Expanded(
          flex: 4,
          child: cuentaPanelId == null
              ? const EstadoVacio(
                  icono: Icons.table_restaurant_outlined,
                  titulo: 'Elige una mesa',
                  mensaje: 'Toca una mesa libre para abrir su cuenta o una ocupada para verla.',
                )
              : PanelCuenta(key: ValueKey(cuentaPanelId), cuentaId: cuentaPanelId, enPanel: true),
        ),
      ],
    );
  }
}

class _Leyenda extends StatelessWidget {
  const _Leyenda();

  @override
  Widget build(BuildContext context) {
    return const Wrap(
      spacing: 8,
      runSpacing: 6,
      children: [
        Insignia('Libre', tono: TonoInsignia.exito),
        Insignia('Ocupada', tono: TonoInsignia.aviso),
        Insignia('Por cobrar', tono: TonoInsignia.primario),
        Insignia('Bloqueada', tono: TonoInsignia.neutro),
      ],
    );
  }
}

class _TarjetaMesa extends StatelessWidget {
  const _TarjetaMesa({required this.mesa, required this.elegida, required this.alTocar});

  final Mesa mesa;
  final bool elegida;
  final VoidCallback alTocar;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final c = context.colores;
    final estado = r.estadoDe(mesa);
    final cuenta = r.cuentaActivaDe(mesa.id);
    final (color, texto, tono) = switch (estado) {
      EstadoMesa.libre => (c.exito, 'Libre', TonoInsignia.exito),
      EstadoMesa.ocupada => (c.aviso, 'Ocupada', TonoInsignia.aviso),
      EstadoMesa.porCobrar => (tema.colorScheme.primary, 'Por cobrar', TonoInsignia.primario),
      EstadoMesa.bloqueada => (c.neutro, 'Bloqueada', TonoInsignia.neutro),
    };
    final sinAtender = cuenta != null && cuenta.origen == OrigenPedido.qr && !cuenta.presenciaConfirmada;
    return Card(
      key: Key('mesa-${mesa.etiqueta}'),
      color: estado == EstadoMesa.bloqueada ? c.neutroSuave : null,
      shape: RoundedRectangleBorder(
        borderRadius: Formas.radioTarjeta,
        side: BorderSide(color: elegida ? tema.colorScheme.primary : c.borde, width: elegida ? 2 : 1),
      ),
      child: InkWell(
        onTap: alTocar,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(height: 4, color: color),
            Container(
              constraints: const BoxConstraints(minHeight: 164),
              padding: const EdgeInsets.fromLTRB(12, 10, 4, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(child: Text('Mesa ${mesa.etiqueta}', style: tema.textTheme.titleMedium)),
                      IconButton(
                        key: Key('opciones-${mesa.etiqueta}'),
                        tooltip: 'QR y opciones de la mesa',
                        visualDensity: VisualDensity.compact,
                        onPressed: () => mostrarOpcionesMesa(context, mesa.id),
                        icon: const Icon(Icons.qr_code_2, size: 22),
                      ),
                    ],
                  ),
                  Text('${mesa.capacidad} personas', style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
                  const SizedBox(height: 8),
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: Wrap(
                      spacing: 6,
                      runSpacing: 4,
                      children: [
                        Insignia(texto, tono: tono),
                        if (mesa.qrPausado) const Insignia('QR en pausa', tono: TonoInsignia.peligro),
                        if (sinAtender) const Insignia('Sin atender', tono: TonoInsignia.aviso, icono: Icons.notifications_active_outlined, solida: true),
                      ],
                    ),
                  ),
                  if (cuenta != null) ...[
                    const SizedBox(height: 8),
                    Text('Cuenta ${cuenta.numero}', style: tema.textTheme.bodySmall),
                    Text(pesos(cuenta.total), style: tema.textTheme.titleSmall),
                    Text('Abierta ${minutosTexto(r.ahora.difference(cuenta.abiertaEn).inMinutes)}',
                        style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DialogoComensales extends StatefulWidget {
  const _DialogoComensales({required this.mesa});

  final Mesa mesa;

  @override
  State<_DialogoComensales> createState() => _DialogoComensalesState();
}

class _DialogoComensalesState extends State<_DialogoComensales> {
  late int _comensales = widget.mesa.capacidad.clamp(1, 4);

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return AlertDialog(
      title: Text('Abrir cuenta · Mesa ${widget.mesa.etiqueta}'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('¿Cuántos comensales son?'),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              IconButton.outlined(
                key: const Key('menos-comensales'),
                onPressed: _comensales > 1 ? () => setState(() => _comensales--) : null,
                icon: const Icon(Icons.remove),
              ),
              SizedBox(
                width: 72,
                child: Text('$_comensales', textAlign: TextAlign.center, style: tema.textTheme.headlineMedium),
              ),
              IconButton.outlined(
                key: const Key('mas-comensales'),
                onPressed: _comensales < 50 ? () => setState(() => _comensales++) : null,
                icon: const Icon(Icons.add),
              ),
            ],
          ),
          if (_comensales > widget.mesa.capacidad)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text('La mesa es para ${widget.mesa.capacidad}. Solo es un aviso (CT-01).',
                  style: tema.textTheme.bodySmall?.copyWith(color: context.colores.aviso)),
            ),
        ],
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Cancelar')),
        FilledButton(
          key: const Key('abrir-cuenta'),
          onPressed: () => Navigator.of(context).pop(_comensales),
          child: const Text('Abrir cuenta'),
        ),
      ],
    );
  }
}
