import 'package:flutter/material.dart';

import '../../estado/restaurante_demo.dart';
import '../../modelos/permisos_restaurante.dart';
import '../../tema.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/componentes.dart';
import '../comensal/comensal_qr.dart';
import 'alcance.dart';

/// Dominio de la web pública: sigue sin elegirse (D-190), por eso se muestra como marcador.
const dominioPendiente = 'dominio-por-definir';

/// Opciones de una mesa: su QR, regenerarlo, pausarlo y bloquear la mesa (QR-01, QR-02, QR-17, MS-02).
Future<void> mostrarOpcionesMesa(BuildContext context, String mesaId) {
  final restaurante = context.restauranteSinEscuchar;
  final contenido = AlcanceRestaurante(restaurante: restaurante, child: _OpcionesMesa(mesaId: mesaId));
  if (context.disposicion.esAncha) {
    return showDialog<void>(
      context: context,
      builder: (_) => Dialog(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 480), child: contenido)),
    );
  }
  return showModalBottomSheet<void>(context: context, isScrollControlled: true, useSafeArea: true, builder: (_) => contenido);
}

class _OpcionesMesa extends StatelessWidget {
  const _OpcionesMesa({required this.mesaId});

  final String mesaId;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final mesa = r.mesa(mesaId);
    if (mesa == null) {
      return const EstadoVacio(icono: Icons.qr_code_2, titulo: 'La mesa ya no existe');
    }
    final tema = Theme.of(context);
    final configurar = r.puede(Permiso.salonConfigurar);
    final operar = r.puede(Permiso.servicioOperar);
    final url = 'https://$dominioPendiente/m/${mesa.qrToken}';
    final vista = r.mesaPorQr(mesa.qrToken);
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Mesa ${mesa.etiqueta}', style: tema.textTheme.titleLarge),
            const SizedBox(height: 4),
            Text('QR de la mesa: el comensal ve la carta, el restaurante y las reseñas, y puede pedir sin cuenta.',
                style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
            const SizedBox(height: 16),
            Center(child: QrDibujo(token: mesa.qrToken)),
            const SizedBox(height: 8),
            Center(
              child: Text('QR de ejemplo; la app real lo dibuja con la URL del servidor',
                  style: tema.textTheme.labelSmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(color: context.colores.neutroSuave, borderRadius: Formas.radioControl),
              child: SelectableText(url, key: const Key('url-qr'), style: tema.textTheme.bodySmall),
            ),
            const SizedBox(height: 4),
            Text('El token es aleatorio y no contiene el id de la mesa ni del negocio. El dominio real está por definir.',
                style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
            const SizedBox(height: 12),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                Insignia(vista?.aceptaPedidos == true ? 'Acepta pedidos' : 'No acepta pedidos',
                    tono: vista?.aceptaPedidos == true ? TonoInsignia.exito : TonoInsignia.peligro),
                if (mesa.qrPausado) const Insignia('QR en pausa', tono: TonoInsignia.peligro),
                if (mesa.bloqueada) const Insignia('Mesa bloqueada', tono: TonoInsignia.neutro),
                if (vista?.motivoNoAcepta != null) Insignia(plantillasDeError[vista!.motivoNoAcepta] ?? '', tono: TonoInsignia.neutro),
              ],
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                FilledButton.icon(
                  key: const Key('probar-como-comensal'),
                  onPressed: () {
                    final token = mesa.qrToken;
                    Navigator.of(context).pop();
                    abrirPantalla<void>(context, (_) => PaginaComensal(token: token));
                  },
                  icon: const Icon(Icons.smartphone),
                  label: const Text('Probar como comensal'),
                ),
                OutlinedButton.icon(
                  key: const Key('pausar-qr'),
                  onPressed: operar ? () => intentar(context, () => r.pausarQr(mesa.id, !mesa.qrPausado)) : null,
                  icon: Icon(mesa.qrPausado ? Icons.play_arrow : Icons.pause),
                  label: Text(mesa.qrPausado ? 'Reanudar QR' : 'Pausar QR'),
                ),
                OutlinedButton.icon(
                  key: const Key('regenerar-qr'),
                  onPressed: configurar
                      ? () async {
                          final ok = await confirmarDialogo(
                            context,
                            titulo: 'Regenerar el QR',
                            mensaje: 'El QR impreso de la mesa ${mesa.etiqueta} dejará de funcionar de inmediato y habrá que imprimir el nuevo.',
                            confirmar: 'Regenerar',
                          );
                          if (ok && context.mounted) {
                            intentar(context, () => r.regenerarQr(mesa.id));
                          }
                        }
                      : null,
                  icon: const Icon(Icons.refresh),
                  label: const Text('Regenerar QR'),
                ),
                OutlinedButton.icon(
                  key: const Key('bloquear-mesa'),
                  onPressed: configurar ? () => intentar(context, () => r.bloquearMesa(mesa.id, !mesa.bloqueada)) : null,
                  icon: Icon(mesa.bloqueada ? Icons.lock_open : Icons.block),
                  label: Text(mesa.bloqueada ? 'Desbloquear mesa' : 'Bloquear mesa'),
                ),
              ],
            ),
            if (!configurar || !operar) ...[
              const SizedBox(height: 10),
              Text('Algunas opciones requieren permisos que tu puesto no tiene.',
                  style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
            ],
          ],
        ),
      ),
    );
  }
}
