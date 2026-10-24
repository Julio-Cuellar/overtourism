import 'package:flutter/material.dart';

import '../datos/demo_restaurantes.dart';
import '../modelos/dinero.dart';
import '../modelos/restaurantes.dart';
import 'componentes.dart';

/// Carta que ve quien no es del personal: platillos por categoría con precio y etiquetas declaradas, sin costos ni
/// recetas (PB-03). Con [alAgregar] se puede pedir; los platillos agotados salen deshabilitados (QR-09).
class CartaPublica extends StatefulWidget {
  const CartaPublica({
    super.key,
    required this.platillos,
    this.categorias = categoriasDemo,
    this.cantidades = const {},
    this.alAgregar,
    this.alQuitar,
    this.maxPorLinea = 10,
    this.padding = const EdgeInsets.fromLTRB(16, 8, 16, 24),
    this.encabezado,
    this.enLinea = false,
  });

  final List<Platillo> platillos;
  final List<CategoriaMenu> categorias;
  final Map<String, int> cantidades;
  final ValueChanged<Platillo>? alAgregar;
  final ValueChanged<Platillo>? alQuitar;
  final int maxPorLinea;
  final EdgeInsetsGeometry padding;
  final Widget? encabezado;

  /// Sin desplazamiento propio, para ponerla dentro de otra lista (teléfono).
  final bool enLinea;

  @override
  State<CartaPublica> createState() => _CartaPublicaState();
}

class _CartaPublicaState extends State<CartaPublica> {
  String? _categoriaId;

  @override
  Widget build(BuildContext context) {
    final categoriasConPlatillos = [
      for (final c in widget.categorias)
        if (widget.platillos.any((p) => p.categoriaId == c.id)) c,
    ];
    final visibles = _categoriaId == null
        ? categoriasConPlatillos
        : categoriasConPlatillos.where((c) => c.id == _categoriaId).toList();
    final tema = Theme.of(context);
    final hijos = <Widget>[
        ?widget.encabezado,
        if (categoriasConPlatillos.length > 1)
          SizedBox(
            height: 44,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: [
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(label: const Text('Todo'), selected: _categoriaId == null, onSelected: (_) => setState(() => _categoriaId = null)),
                ),
                for (final c in categoriasConPlatillos)
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(c.nombre),
                      selected: _categoriaId == c.id,
                      onSelected: (_) => setState(() => _categoriaId = _categoriaId == c.id ? null : c.id),
                    ),
                  ),
              ],
            ),
          ),
        for (final c in visibles) ...[
          Padding(
            padding: const EdgeInsets.only(top: 16, bottom: 8),
            child: Text(c.nombre, style: tema.textTheme.titleMedium),
          ),
          for (final p in widget.platillos.where((p) => p.categoriaId == c.id))
            Padding(padding: const EdgeInsets.only(bottom: 10), child: _FilaCarta(platillo: p, categoria: c, carta: widget)),
        ],
        const SizedBox(height: 8),
        Text(leyendaPrecios, style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
        Text('Las etiquetas las declara el negocio; no están verificadas.',
            style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
    ];
    return widget.enLinea
        ? Padding(padding: widget.padding, child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: hijos))
        : ListView(padding: widget.padding, children: hijos);
  }
}

class _FilaCarta extends StatelessWidget {
  const _FilaCarta({required this.platillo, required this.categoria, required this.carta});

  final Platillo platillo;
  final CategoriaMenu categoria;
  final CartaPublica carta;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final cantidad = carta.cantidades[platillo.id] ?? 0;
    final puedePedir = carta.alAgregar != null;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Opacity(opacity: platillo.agotado ? 0.45 : 1, child: FotoPlatillo(platillo: platillo, estacion: categoria.estacion, lado: 72)),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(platillo.nombre,
                            style: tema.textTheme.titleSmall?.copyWith(decoration: platillo.agotado ? TextDecoration.lineThrough : null)),
                      ),
                      Text(pesos(platillo.precio), style: tema.textTheme.titleSmall),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(platillo.descripcion, style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
                  if (platillo.etiquetas.isNotEmpty || platillo.agotado) ...[
                    const SizedBox(height: 6),
                    Wrap(
                      spacing: 6,
                      runSpacing: 4,
                      children: [
                        if (platillo.agotado) const Insignia('Agotado', tono: TonoInsignia.peligro, solida: true),
                        for (final e in platillo.etiquetas)
                          Insignia(e.etiqueta,
                              tono: switch (e) {
                                EtiquetaDieta.picante || EtiquetaDieta.contieneAlcohol => TonoInsignia.aviso,
                                EtiquetaDieta.tipicoPoblano || EtiquetaDieta.deTemporada => TonoInsignia.info,
                                _ => TonoInsignia.exito,
                              }),
                      ],
                    ),
                  ],
                  if (puedePedir && !platillo.agotado) ...[
                    const SizedBox(height: 8),
                    cantidad == 0
                        ? OutlinedButton.icon(
                            key: Key('agregar-${platillo.nombre}'),
                            onPressed: () => carta.alAgregar!(platillo),
                            icon: const Icon(Icons.add, size: 18),
                            label: const Text('Agregar'),
                            style: OutlinedButton.styleFrom(minimumSize: const Size(0, 40)),
                          )
                        : Row(
                            children: [
                              IconButton.outlined(
                                key: Key('quitar-${platillo.nombre}'),
                                onPressed: () => carta.alQuitar?.call(platillo),
                                icon: const Icon(Icons.remove, size: 18),
                              ),
                              SizedBox(width: 44, child: Text('$cantidad', textAlign: TextAlign.center, style: tema.textTheme.titleMedium)),
                              IconButton.outlined(
                                key: Key('sumar-${platillo.nombre}'),
                                onPressed: cantidad >= carta.maxPorLinea ? null : () => carta.alAgregar!(platillo),
                                icon: const Icon(Icons.add, size: 18),
                              ),
                            ],
                          ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
