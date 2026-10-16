import 'package:flutter/material.dart';

import '../datos/demo.dart';

/// Franja que rotula los datos simulados (regla del proyecto: todo dato simulado va marcado como tal).
class EtiquetaDemo extends StatelessWidget {
  const EtiquetaDemo({super.key, this.detalle});

  /// Texto adicional, por ejemplo «negocios ficticios».
  final String? detalle;

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      color: colores.tertiaryContainer,
      child: Row(
        children: [
          Icon(Icons.science_outlined, size: 18, color: colores.onTertiaryContainer),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              detalle == null ? rotuloDemo : '$rotuloDemo · $detalle',
              style: TextStyle(color: colores.onTertiaryContainer, fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}
