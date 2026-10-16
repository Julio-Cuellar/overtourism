import 'package:flutter/material.dart';

import '../../modelos/dinero.dart';
import '../../modelos/permisos_restaurante.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/componentes.dart';
import 'alcance.dart';
import 'editor_platillo.dart';
import 'receta.dart';

/// Detalle de un platillo como pantalla completa (teléfono).
class PantallaDetallePlatillo extends StatelessWidget {
  const PantallaDetallePlatillo({super.key, required this.platilloId});

  final String platilloId;

  @override
  Widget build(BuildContext context) {
    final platillo = context.restaurante.platillo(platilloId);
    return Scaffold(
      appBar: AppBar(title: Text(platillo?.nombre ?? 'Platillo')),
      body: DetallePlatillo(platilloId: platilloId),
    );
  }
}

/// Detalle de un platillo: datos del menú, estado (agotado, publicado) y receta con costo y margen.
class DetallePlatillo extends StatelessWidget {
  const DetallePlatillo({super.key, required this.platilloId, this.enPanel = false});

  final String platilloId;
  final bool enPanel;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final platillo = r.platillo(platilloId);
    if (platillo == null || !platillo.activo) {
      return const EstadoVacio(icono: Icons.delete_outline, titulo: 'Este platillo ya no está en el menú');
    }
    final tema = Theme.of(context);
    final categoria = r.categoria(platillo.categoriaId);
    final fuera = platillo.temporada != null && !platillo.temporada!.contiene(r.ahora);
    final puedeEditar = r.puede(Permiso.menuEditar);
    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 16),
      children: [
        ContenidoCentrado(
          anchoMaximo: 820,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  FotoPlatillo(platillo: platillo, estacion: r.estacionDe(platillo), lado: 84),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (enPanel) Text(platillo.nombre, style: tema.textTheme.headlineSmall),
                        if (!enPanel) Text(pesos(platillo.precio), style: tema.textTheme.headlineSmall),
                        if (enPanel) Text(pesos(platillo.precio), style: tema.textTheme.titleLarge?.copyWith(color: tema.colorScheme.primary)),
                        const SizedBox(height: 4),
                        Text(
                          '${categoria?.nombre ?? 'Sin categoría'} · ${r.estacionDe(platillo).etiqueta}',
                          style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                        ),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 6,
                          runSpacing: 4,
                          children: [
                            if (platillo.agotado) const Insignia('Agotado', tono: TonoInsignia.peligro, solida: true),
                            if (fuera) const Insignia('Fuera de temporada', tono: TonoInsignia.neutro),
                            Insignia(platillo.publicado ? 'Publicado' : 'No publicado',
                                tono: platillo.publicado ? TonoInsignia.exito : TonoInsignia.neutro),
                            Insignia(platillo.modoInventario.etiqueta, tono: TonoInsignia.info),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Text(platillo.descripcion, style: tema.textTheme.bodyLarge),
              if (platillo.etiquetas.isNotEmpty) ...[
                const SizedBox(height: 12),
                EtiquetasDieta(platillo.etiquetas),
                const SizedBox(height: 4),
                Text('Declarado por el negocio; no está verificado.',
                    style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
              ],
              if (platillo.temporada != null) ...[
                const SizedBox(height: 12),
                Row(
                  children: [
                    Icon(Icons.event_outlined, size: 18, color: tema.colorScheme.onSurfaceVariant),
                    const SizedBox(width: 6),
                    Text('Temporada: ${platillo.temporada!.texto}'),
                  ],
                ),
              ],
              const SizedBox(height: 20),
              Card(
                child: Column(
                  children: [
                    SwitchListTile(
                      key: const Key('agotado'),
                      title: const Text('Agotado'),
                      subtitle: const Text('No se podrá pedir en el salón ni por QR. Las líneas ya pedidas siguen (MN-05).'),
                      value: platillo.agotado,
                      onChanged: r.puede(Permiso.menuAgotar)
                          ? (v) => intentar(context, () => r.marcarAgotado(platillo.id, v))
                          : null,
                    ),
                    const Divider(),
                    SwitchListTile(
                      key: const Key('publicado'),
                      title: const Text('Publicado en el menú del turista'),
                      subtitle: const Text('Un platillo sin publicar se sigue vendiendo en el salón (MN-09).'),
                      value: platillo.publicado,
                      onChanged: puedeEditar
                          ? (v) => intentar(context, () => r.editarPlatillo(
                                platillo.id,
                                nombre: platillo.nombre,
                                descripcion: platillo.descripcion,
                                precio: platillo.precio,
                                categoriaId: platillo.categoriaId,
                                etiquetas: platillo.etiquetas,
                                temporada: platillo.temporada,
                                publicado: v,
                              ))
                          : null,
                    ),
                  ],
                ),
              ),
              if (puedeEditar) ...[
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    OutlinedButton.icon(
                      key: const Key('editar-platillo'),
                      onPressed: () => mostrarEditorPlatillo(context, platillo: platillo),
                      icon: const Icon(Icons.edit_outlined),
                      label: const Text('Editar'),
                    ),
                    TextButton.icon(
                      key: const Key('baja-platillo'),
                      style: TextButton.styleFrom(foregroundColor: tema.colorScheme.error),
                      onPressed: () async {
                        final ok = await confirmarDialogo(
                          context,
                          titulo: 'Dar de baja el platillo',
                          mensaje: '«${platillo.nombre}» saldrá del menú y ya no se podrá vender. Las ventas anteriores no cambian.',
                          confirmar: 'Dar de baja',
                          destructivo: true,
                        );
                        if (ok && context.mounted) {
                          final exito = intentar(context, () => r.darDeBajaPlatillo(platillo.id));
                          if (exito && !enPanel && context.mounted) {
                            Navigator.of(context).maybePop();
                          }
                        }
                      },
                      icon: const Icon(Icons.delete_outline),
                      label: const Text('Dar de baja'),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 24),
              SeccionReceta(platilloId: platillo.id),
            ],
          ),
        ),
      ],
    );
  }
}
