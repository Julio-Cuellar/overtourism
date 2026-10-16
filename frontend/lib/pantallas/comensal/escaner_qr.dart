import 'package:flutter/material.dart';

import '../../tema.dart';
import '../../widgets/etiqueta_demo.dart';
import '../restaurantes/alcance.dart';
import 'comensal_qr.dart';

/// Escáner de QR simulado. La maqueta no usa la cámara: «escanear» es tocar una de las mesas. El QR real abre la app
/// si está instalada y, si no, la web pública (QR-21).
class EscanearQr extends StatelessWidget {
  const EscanearQr({super.key});

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Escanear QR de la mesa')),
      body: Column(
        children: [
          const EtiquetaDemo(detalle: 'la cámara se simula con los botones de abajo'),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 560),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        AspectRatio(
                          aspectRatio: 1.4,
                          child: Container(
                            decoration: BoxDecoration(color: Paleta.azulNoche, borderRadius: Formas.radioTarjeta),
                            child: const Center(child: _Visor()),
                          ),
                        ),
                        const SizedBox(height: 16),
                        Text('Apunta al QR de tu mesa', style: tema.textTheme.titleMedium, textAlign: TextAlign.center),
                        const SizedBox(height: 4),
                        Text('No necesitas cuenta. Verás la carta, el restaurante y las reseñas, y podrás pedir.',
                            style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                            textAlign: TextAlign.center),
                        const SizedBox(height: 20),
                        Text('Simular escaneo', style: tema.textTheme.labelLarge),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: [
                            for (final m in r.mesas)
                              OutlinedButton(
                                key: Key('escanear-${m.etiqueta}'),
                                onPressed: () => abrirPantalla<void>(context, (_) => PaginaComensal(token: m.qrToken)),
                                child: Text('Mesa ${m.etiqueta}'),
                              ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        TextButton.icon(
                          key: const Key('escanear-invalido'),
                          onPressed: () => abrirPantalla<void>(context, (_) => const PaginaComensal(token: 'qr-que-ya-no-existe')),
                          icon: const Icon(Icons.qr_code_2),
                          label: const Text('QR dañado o ya regenerado'),
                        ),
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(color: context.colores.infoSuave, borderRadius: Formas.radioControl),
                          child: Text(
                            'Tip para la demo: abre el servicio en el panel del restaurante, pide desde una mesa libre y '
                            'cambia el puesto a Cocinero para ver llegar la comanda.',
                            style: tema.textTheme.bodySmall,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Marco del visor con esquinas rectas.
class _Visor extends StatelessWidget {
  const _Visor();

  @override
  Widget build(BuildContext context) {
    Widget esquina(Alignment a) => Align(
          alignment: a,
          child: SizedBox(
            width: 34,
            height: 34,
            child: DecoratedBox(
              decoration: BoxDecoration(
                border: Border(
                  top: a.y < 0 ? const BorderSide(color: Colors.white, width: 4) : BorderSide.none,
                  bottom: a.y > 0 ? const BorderSide(color: Colors.white, width: 4) : BorderSide.none,
                  left: a.x < 0 ? const BorderSide(color: Colors.white, width: 4) : BorderSide.none,
                  right: a.x > 0 ? const BorderSide(color: Colors.white, width: 4) : BorderSide.none,
                ),
              ),
            ),
          ),
        );
    return SizedBox(
      width: 190,
      height: 190,
      child: Stack(
        children: [
          esquina(Alignment.topLeft),
          esquina(Alignment.topRight),
          esquina(Alignment.bottomLeft),
          esquina(Alignment.bottomRight),
          const Center(child: Icon(Icons.qr_code_scanner, color: Colors.white54, size: 64)),
        ],
      ),
    );
  }
}
