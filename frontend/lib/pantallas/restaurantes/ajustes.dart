import 'package:flutter/material.dart';

import '../../modelos/permisos_restaurante.dart';
import '../../modelos/restaurantes.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/componentes.dart';
import 'alcance.dart';

/// Ajustes del restaurante: modo de servicio, pedidos por QR y publicación para el turista.
class AjustesRestaurante extends StatelessWidget {
  const AjustesRestaurante({super.key});

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final configurar = r.puede(Permiso.salonConfigurar);
    final operar = r.puede(Permiso.servicioOperar);
    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 16),
      children: [
        ContenidoCentrado(
          anchoMaximo: 820,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Ajustes', style: tema.textTheme.headlineSmall),
              const SizedBox(height: 20),
              Seccion(
                titulo: 'Cómo atiendes',
                subtitulo: 'En mostrador la cuenta no lleva mesa (para llevar).',
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: SegmentedButton<ModoServicio>(
                      showSelectedIcon: false,
                      segments: [for (final m in ModoServicio.values) ButtonSegment(value: m, label: Text(m.etiqueta))],
                      selected: {r.modoServicio},
                      onSelectionChanged: configurar ? (s) => intentar(context, () => r.cambiarModoServicio(s.first)) : null,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Seccion(
                titulo: 'Pedidos por QR',
                child: Card(
                  child: Column(
                    children: [
                      SwitchListTile(
                        key: const Key('qr-habilitado'),
                        title: const Text('Recibir pedidos desde el QR de las mesas'),
                        subtitle: const Text(
                            'Apagado: la carta se sigue viendo, pero nadie puede pedir. Úsalo si hay bromas o falta personal (QR-17).'),
                        value: r.qrHabilitado,
                        onChanged: operar ? (v) => intentar(context, () => r.habilitarQr(v)) : null,
                      ),
                      const Divider(),
                      const ListTile(
                        leading: Icon(Icons.shield_outlined),
                        title: Text('Defensas activas'),
                        subtitle: Text(
                            'Hasta 20 líneas por pedido y 10 por platillo, 60 líneas y \$3,000.00 por cuenta de QR, 5 pedidos por minuto por mesa. '
                            'Tres cuentas sin comensales en una hora pausan el QR de esa mesa.'),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Seccion(
                titulo: 'Publicación para el turista',
                child: Card(
                  child: SwitchListTile(
                    key: const Key('visible'),
                    title: const Text('Mostrar mi restaurante en el directorio'),
                    subtitle: const Text('Pausado: el menú público y el QR dejan de responder (PB-01).'),
                    value: r.visible,
                    onChanged: configurar ? (v) => intentar(context, () => r.cambiarVisibilidad(v)) : null,
                  ),
                ),
              ),
              if (!configurar) ...[
                const SizedBox(height: 12),
                Text('Algunos ajustes requieren permisos que tu puesto no tiene.', style: tema.textTheme.bodySmall),
              ],
            ],
          ),
        ),
      ],
    );
  }
}
