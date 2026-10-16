import 'package:flutter/material.dart';

import '../../datos/demo.dart';
import '../../estado/caja_demo.dart';
import '../../estado/carrito.dart';
import '../../modelos/dinero.dart';
import '../../modelos/metodo_pago.dart';
import '../../modelos/modelos.dart';

/// Confirmar el cobro: la persona revisa el resumen, captura el cupon y la forma de pago, y confirma. Nada se
/// registra hasta tocar «Confirmar cobro» (D-23). Con el servidor, este resumen es el de `prepararComando`.
class ConfirmarCobro extends StatefulWidget {
  const ConfirmarCobro({super.key, required this.caja, required this.carrito});

  final CajaDemo caja;
  final Carrito carrito;

  @override
  State<ConfirmarCobro> createState() => _ConfirmarCobroState();
}

class _ConfirmarCobroState extends State<ConfirmarCobro> {
  final _cupon = TextEditingController();
  final _recibido = TextEditingController();
  MetodoPago _metodo = MetodoPago.efectivo;
  String? _errorCupon;
  String? _errorRecibido;

  @override
  void dispose() {
    _cupon.dispose();
    _recibido.dispose();
    super.dispose();
  }

  Carrito get _carrito => widget.carrito;

  void _aplicarCupon() {
    final codigo = _cupon.text.trim().toUpperCase();
    if (codigo.isEmpty) {
      setState(() => _errorCupon = null);
      _carrito.aplicarCupon(null);
      return;
    }
    final cupon = cuponesDemo.where((c) => c.codigo == codigo).firstOrNull;
    setState(() => _errorCupon = cupon == null ? 'Ese cupón no es válido.' : null);
    _carrito.aplicarCupon(cupon);
  }

  /// Lo recibido en efectivo; vacio cuenta como pago exacto. Nulo si no es un monto valido.
  int? get _recibidoCentavos {
    final texto = _recibido.text.trim();
    if (texto.isEmpty) {
      return _carrito.total;
    }
    return RegExp(r'^\d{1,7}(\.\d{1,2})?$').hasMatch(texto) ? centavosDe(texto) : null;
  }

  Future<void> _confirmar() async {
    if (_metodo == MetodoPago.efectivo) {
      final recibido = _recibidoCentavos;
      if (recibido == null || recibido < _carrito.total) {
        setState(() => _errorRecibido = 'El efectivo recibido no alcanza para el total.');
        return;
      }
    }
    final cambio = _metodo == MetodoPago.efectivo ? _carrito.cambio(_recibidoCentavos!) : 0;
    final venta = widget.caja.cobrar(_carrito, _metodo);
    _carrito.vaciar();
    await showDialog<void>(
      context: context,
      builder: (contexto) => AlertDialog(
        icon: const Icon(Icons.check_circle_outline),
        title: const Text('Venta registrada'),
        content: Text('Folio ${venta.folio} · Total ${pesos(venta.total)} · Cambio ${pesos(cambio)}'),
        actions: [TextButton(onPressed: () => Navigator.of(contexto).pop(), child: const Text('Listo'))],
      ),
    );
    if (mounted) {
      Navigator.of(context).pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return ListenableBuilder(
      listenable: _carrito,
      builder: (context, _) => Scaffold(
        appBar: AppBar(title: const Text('Confirmar cobro')),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text('Revisa antes de cobrar', style: tema.textTheme.titleMedium),
            const SizedBox(height: 8),
            for (final linea in _carrito.lineas.entries) _FilaResumen(producto: linea.key, milesimas: linea.value),
            const Divider(height: 24),
            _Cifra(etiqueta: 'Subtotal', valor: _carrito.subtotal),
            if (_carrito.descuento > 0) _Cifra(etiqueta: 'Cupón ${_carrito.cupon!.codigo}', valor: -_carrito.descuento),
            _Cifra(etiqueta: 'Total', valor: _carrito.total, destacada: true),
            const SizedBox(height: 16),
            TextField(
              controller: _cupon,
              textCapitalization: TextCapitalization.characters,
              decoration: InputDecoration(
                labelText: 'Cupón del Pasaporte (opcional)',
                errorText: _errorCupon,
                suffixIcon: IconButton(onPressed: _aplicarCupon, icon: const Icon(Icons.check)),
              ),
              onSubmitted: (_) => _aplicarCupon(),
            ),
            const SizedBox(height: 16),
            SegmentedButton<MetodoPago>(
              segments: [
                for (final metodo in MetodoPago.values) ButtonSegment(value: metodo, label: Text(metodo.etiqueta)),
              ],
              selected: {_metodo},
              onSelectionChanged: (seleccion) => setState(() => _metodo = seleccion.first),
            ),
            if (_metodo == MetodoPago.efectivo) ...[
              const SizedBox(height: 16),
              TextField(
                controller: _recibido,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                  labelText: 'Efectivo recibido (vacío = exacto)',
                  prefixText: r'$ ',
                  errorText: _errorRecibido,
                ),
                onChanged: (_) => setState(() => _errorRecibido = null),
              ),
              const SizedBox(height: 8),
              _Cifra(etiqueta: 'Cambio', valor: _carrito.cambio(_recibidoCentavos ?? 0)),
            ],
          ],
        ),
        bottomNavigationBar: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: FilledButton.icon(
              key: const Key('confirmar-cobro'),
              onPressed: _carrito.estaVacio ? null : _confirmar,
              icon: const Icon(Icons.check),
              label: Text('Confirmar cobro de ${pesos(_carrito.total)}'),
            ),
          ),
        ),
      ),
    );
  }
}

class _FilaResumen extends StatelessWidget {
  const _FilaResumen({required this.producto, required this.milesimas});

  final Producto producto;
  final int milesimas;

  @override
  Widget build(BuildContext context) {
    final resto = milesimas % 1000;
    final cantidad = resto == 0 ? '${milesimas ~/ 1000}' : '${milesimas ~/ 1000}.${resto ~/ 100}';
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Expanded(child: Text('$cantidad ${producto.unidad} · ${producto.descripcion}')),
          Text(pesos(Carrito.importe(producto, milesimas))),
        ],
      ),
    );
  }
}

class _Cifra extends StatelessWidget {
  const _Cifra({required this.etiqueta, required this.valor, this.destacada = false});

  final String etiqueta;
  final int valor;
  final bool destacada;

  @override
  Widget build(BuildContext context) {
    final estilo = destacada
        ? Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)
        : Theme.of(context).textTheme.bodyLarge;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          Expanded(child: Text(etiqueta, style: estilo)),
          Text(pesos(valor), style: estilo),
        ],
      ),
    );
  }
}
