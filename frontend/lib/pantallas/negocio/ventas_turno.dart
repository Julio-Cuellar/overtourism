import 'package:flutter/material.dart';

import '../../estado/caja_demo.dart';
import '../../modelos/dinero.dart';

/// Ventas del turno, la mas reciente arriba.
class VentasTurno extends StatelessWidget {
  const VentasTurno({super.key, required this.caja});

  final CajaDemo caja;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: caja,
      builder: (context, _) {
        final ventas = caja.ventas.reversed.toList();
        if (ventas.isEmpty) {
          return const Center(child: Text('Aún no hay ventas en este turno.'));
        }
        return ListView.separated(
          padding: const EdgeInsets.all(16),
          itemCount: ventas.length,
          separatorBuilder: (_, _) => const Divider(height: 1),
          itemBuilder: (context, i) {
            final venta = ventas[i];
            return ListTile(
              leading: CircleAvatar(child: Text('${venta.folio}')),
              title: Text('Folio A-${venta.folio}'),
              subtitle: Text('${venta.lineas} ${venta.lineas == 1 ? 'producto' : 'productos'} · ${venta.metodo.etiqueta}'),
              trailing: Text(pesos(venta.total), style: const TextStyle(fontWeight: FontWeight.w700)),
            );
          },
        );
      },
    );
  }
}
