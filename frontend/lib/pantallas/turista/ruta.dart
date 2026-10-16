import 'package:flutter/material.dart';

import '../../datos/demo.dart';
import '../../widgets/etiqueta_demo.dart';

/// Ruta del dia: maqueta de la ruta que propondra la IA con los negocios de la plataforma. Aqui el orden y los
/// tiempos son fijos y se rotulan como simulacion; la IA de rutas aun no esta conectada.
class RutaSugerida extends StatelessWidget {
  const RutaSugerida({super.key});

  static const _paradas = [
    ('n3', '10:00', 'Desayuno dulce y camotes para llevar'),
    ('n2', '11:30', 'Recorrido por el taller de talavera'),
    ('n1', '14:00', 'Comida: mole poblano'),
    ('n4', '16:30', 'Cemitas antes de subir a la pirámide de Cholula'),
  ];

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return ListView(
      children: [
        const EtiquetaDemo(detalle: 'simulación; la IA de rutas aún no está conectada'),
        Padding(
          padding: const EdgeInsets.all(16),
          child: Text('Gastronomía y artesanía, un día', style: tema.textTheme.titleLarge),
        ),
        for (final (indice, parada) in _paradas.indexed)
          _Parada(
            numero: indice + 1,
            hora: parada.$2,
            nombre: negociosDemo.firstWhere((n) => n.id == parada.$1).nombre,
            motivo: parada.$3,
            esUltima: indice == _paradas.length - 1,
          ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: Text(
            'Cómo decidirá la IA: elige solo entre negocios de la plataforma; el servidor revisa horarios y '
            'distancias antes de mostrar la ruta.',
            style: tema.textTheme.bodySmall,
          ),
        ),
      ],
    );
  }
}

class _Parada extends StatelessWidget {
  const _Parada({required this.numero, required this.hora, required this.nombre, required this.motivo,
      required this.esUltima});

  final int numero;
  final String hora;
  final String nombre;
  final String motivo;
  final bool esUltima;

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SizedBox(
            width: 64,
            child: Column(
              children: [
                CircleAvatar(radius: 14, backgroundColor: colores.primary,
                    child: Text('$numero', style: TextStyle(color: colores.onPrimary, fontSize: 13))),
                if (!esUltima) Expanded(child: VerticalDivider(color: colores.outlineVariant, thickness: 2)),
              ],
            ),
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(right: 16, bottom: 20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(hora, style: TextStyle(color: colores.primary, fontWeight: FontWeight.w700)),
                  Text(nombre, style: const TextStyle(fontWeight: FontWeight.w600)),
                  Text(motivo),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
