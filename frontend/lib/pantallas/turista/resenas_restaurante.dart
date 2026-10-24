import 'package:flutter/material.dart';

import '../../modelos/restaurantes.dart';
import '../../tema.dart';

/// Estrellas de 1 a 5 como iconos; el promedio lo calcula el código.
class Estrellas extends StatelessWidget {
  const Estrellas(this.cantidad, {super.key, this.tamano = 16});

  final int cantidad;
  final double tamano;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: '$cantidad de 5 estrellas',
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (var i = 1; i <= 5; i++)
            Icon(i <= cantidad ? Icons.star_rounded : Icons.star_outline_rounded, size: tamano, color: context.colores.aviso),
        ],
      ),
    );
  }
}

/// Reseñas de un restaurante con la respuesta del dueño. Vienen de Negocios; en la maqueta son de ejemplo.
class ResenasRestaurante extends StatelessWidget {
  const ResenasRestaurante({super.key, required this.resenas});

  final List<ResenaRestaurante> resenas;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final promedio = resenas.isEmpty ? 0 : resenas.fold(0, (s, r) => s + r.estrellas) / resenas.length;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text('Reseñas', style: tema.textTheme.titleMedium),
            const SizedBox(width: 10),
            if (resenas.isNotEmpty) ...[
              Estrellas(promedio.round()),
              const SizedBox(width: 6),
              Text(promedio.toStringAsFixed(1), style: tema.textTheme.titleSmall),
              Text(' · ${resenas.length}', style: tema.textTheme.bodySmall),
            ],
          ],
        ),
        const SizedBox(height: 4),
        Text('Reseñas de ejemplo (modo demostración).',
            style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
        const SizedBox(height: 10),
        if (resenas.isEmpty) const Text('Todavía no hay reseñas.'),
        for (final r in resenas)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(child: Text(r.autor, style: tema.textTheme.titleSmall)),
                        Text(r.fecha, style: tema.textTheme.bodySmall),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Estrellas(r.estrellas, tamano: 15),
                    const SizedBox(height: 6),
                    Text(r.texto),
                    if (r.respuesta != null) ...[
                      const SizedBox(height: 10),
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: context.colores.neutroSuave,
                          borderRadius: Formas.radioControl,
                          border: Border(left: BorderSide(color: tema.colorScheme.primary, width: 3)),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('Respuesta del restaurante', style: tema.textTheme.labelMedium),
                            const SizedBox(height: 2),
                            Text(r.respuesta!),
                          ],
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}
