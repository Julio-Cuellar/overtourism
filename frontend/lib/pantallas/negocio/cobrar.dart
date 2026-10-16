import 'package:flutter/material.dart';

import '../../datos/demo.dart';
import '../../estado/caja_demo.dart';
import '../../estado/carrito.dart';
import '../../modelos/dinero.dart';
import '../../modelos/modelos.dart';
import '../../widgets/etiqueta_demo.dart';
import 'confirmar_cobro.dart';

/// Cobrar: se tocan los productos para armar la venta y se pasa a confirmar (preparar → confirmar, D-23).
class Cobrar extends StatelessWidget {
  const Cobrar({super.key, required this.caja, required this.carrito});

  final CajaDemo caja;
  final Carrito carrito;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: carrito,
      builder: (context, _) => Column(
        children: [
          const EtiquetaDemo(detalle: 'catálogo simulado'),
          Expanded(
            child: ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: productosDemo.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, i) => _FilaProducto(producto: productosDemo[i], carrito: carrito),
            ),
          ),
          _BarraTotal(caja: caja, carrito: carrito),
        ],
      ),
    );
  }
}

class _FilaProducto extends StatelessWidget {
  const _FilaProducto({required this.producto, required this.carrito});

  final Producto producto;
  final Carrito carrito;

  @override
  Widget build(BuildContext context) {
    final cantidad = carrito.cantidadDe(producto);
    final porUnidad = producto.admiteFraccion ? 'por ${producto.unidad} · se vende por medio kilo' : 'por pieza';
    return Card.outlined(
      child: ListTile(
        title: Text(producto.descripcion),
        subtitle: Text('${pesos(producto.precio)} $porUnidad'),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (cantidad > 0) ...[
              IconButton(
                tooltip: 'Quitar',
                onPressed: () => carrito.quitar(producto),
                icon: const Icon(Icons.remove_circle_outline),
              ),
              Text(_cantidad(cantidad, producto), style: const TextStyle(fontWeight: FontWeight.w700)),
            ],
            IconButton.filledTonal(
              key: Key('agregar-${producto.descripcion}'),
              tooltip: 'Agregar',
              onPressed: () => carrito.agregar(producto),
              icon: const Icon(Icons.add),
            ),
          ],
        ),
      ),
    );
  }

  static String _cantidad(int milesimas, Producto producto) {
    final entero = milesimas ~/ 1000;
    final resto = milesimas % 1000;
    final texto = resto == 0 ? '$entero' : '$entero.${(resto ~/ 100)}';
    return producto.admiteFraccion ? '$texto kg' : texto;
  }
}

class _BarraTotal extends StatelessWidget {
  const _BarraTotal({required this.caja, required this.carrito});

  final CajaDemo caja;
  final Carrito carrito;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Material(
      elevation: 3,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text('Total', style: tema.textTheme.labelLarge),
                    Text(pesos(carrito.total),
                        style: tema.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
                  ],
                ),
              ),
              FilledButton.icon(
                key: const Key('ir-a-cobrar'),
                onPressed: carrito.estaVacio
                    ? null
                    : () => Navigator.of(context).push(MaterialPageRoute<void>(
                        builder: (_) => ConfirmarCobro(caja: caja, carrito: carrito))),
                icon: const Icon(Icons.payments_outlined),
                label: const Text('Cobrar'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
