import 'package:flutter/material.dart';

import '../../datos/demo.dart';
import '../../modelos/dinero.dart';
import '../../widgets/etiqueta_demo.dart';

/// Pasaporte turistico: los cupones que el turista muestra al pagar (D-34). El negocio los captura en su cobro.
class Pasaporte extends StatelessWidget {
  const Pasaporte({super.key});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return ListView(
      children: [
        const EtiquetaDemo(detalle: 'cupones simulados; en la demo se pueden usar varias veces'),
        Padding(
          padding: const EdgeInsets.all(16),
          child: Text('Muestra el código en la caja del negocio para obtener tu descuento.',
              style: tema.textTheme.bodyLarge),
        ),
        for (final cupon in cuponesDemo)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: Card.outlined(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    Container(
                      width: 72,
                      height: 72,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: tema.colorScheme.primaryContainer,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(pesos(cupon.descuento).replaceAll('.00', ''),
                          style: tema.textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.w800, color: tema.colorScheme.onPrimaryContainer)),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(cupon.descripcion, style: tema.textTheme.titleSmall),
                          const SizedBox(height: 8),
                          SelectableText(cupon.codigo,
                              style: tema.textTheme.titleLarge?.copyWith(
                                  fontFamily: 'monospace', fontWeight: FontWeight.w700, letterSpacing: 1.5)),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}
