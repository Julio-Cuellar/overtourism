import 'package:flutter/material.dart';

import '../../datos/demo.dart';
import '../../modelos/modelos.dart';
import '../../widgets/etiqueta_demo.dart';

/// Explorar: negocios filtrados por intereses (cuestionario de intereses del diseno).
class Explorar extends StatefulWidget {
  const Explorar({super.key});

  @override
  State<Explorar> createState() => _ExplorarState();
}

class _ExplorarState extends State<Explorar> {
  final Set<Interes> _filtros = {};

  List<Negocio> get _visibles => _filtros.isEmpty
      ? negociosDemo
      : negociosDemo.where((n) => n.intereses.any(_filtros.contains)).toList();

  @override
  Widget build(BuildContext context) {
    final visibles = _visibles;
    return Column(
      children: [
        const EtiquetaDemo(detalle: 'negocios ficticios; después, negocios reales del DENUE'),
        SizedBox(
          height: 56,
          child: ListView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            children: [
              for (final interes in Interes.values)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: FilterChip(
                    label: Text(interes.etiqueta),
                    selected: _filtros.contains(interes),
                    onSelected: (activo) => setState(() => activo ? _filtros.add(interes) : _filtros.remove(interes)),
                  ),
                ),
            ],
          ),
        ),
        Expanded(
          child: visibles.isEmpty
              ? const Center(child: Text('No hay negocios con esos intereses.'))
              : ListView.separated(
                  padding: const EdgeInsets.all(16),
                  itemCount: visibles.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 12),
                  itemBuilder: (context, i) => _TarjetaNegocio(negocio: visibles[i]),
                ),
        ),
      ],
    );
  }
}

class _TarjetaNegocio extends StatelessWidget {
  const _TarjetaNegocio({required this.negocio});

  final Negocio negocio;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Card.outlined(
      child: InkWell(
        onTap: () => Navigator.of(context)
            .push(MaterialPageRoute<void>(builder: (_) => DetalleNegocio(negocio: negocio))),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(negocio.nombre, style: tema.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
              const SizedBox(height: 4),
              Text('${negocio.giro} · ${negocio.zona}', style: tema.textTheme.bodyMedium),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 4,
                children: [
                  for (final interes in negocio.intereses) Chip(label: Text(interes.etiqueta)),
                  if (negocio.cupon != null)
                    Chip(
                      avatar: const Icon(Icons.local_activity, size: 18),
                      label: const Text('Acepta Pasaporte'),
                      backgroundColor: tema.colorScheme.secondaryContainer,
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Ficha de un negocio con su cupon del Pasaporte, si tiene.
class DetalleNegocio extends StatelessWidget {
  const DetalleNegocio({super.key, required this.negocio});

  final Negocio negocio;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final cupon = negocio.cupon;
    return Scaffold(
      appBar: AppBar(title: Text(negocio.giro)),
      body: ListView(
        children: [
          const EtiquetaDemo(detalle: 'negocio ficticio'),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(negocio.nombre, style: tema.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700)),
                const SizedBox(height: 8),
                Text(negocio.descripcion, style: tema.textTheme.bodyLarge),
                const SizedBox(height: 16),
                ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.place_outlined),
                    title: Text(negocio.zona)),
                ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.schedule),
                    title: Text(negocio.horario)),
                if (cupon != null) ...[
                  const SizedBox(height: 8),
                  Card.filled(
                    color: tema.colorScheme.secondaryContainer,
                    child: ListTile(
                      leading: const Icon(Icons.local_activity),
                      title: Text(cupon.descripcion),
                      subtitle: Text('Muestra el código ${cupon.codigo} al pagar'),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
