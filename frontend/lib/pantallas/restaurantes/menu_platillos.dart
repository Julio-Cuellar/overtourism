import 'package:flutter/material.dart';

import '../../estado/restaurante_demo.dart';
import '../../modelos/dinero.dart';
import '../../modelos/formato.dart';
import '../../modelos/permisos_restaurante.dart';
import '../../modelos/restaurantes.dart';
import '../../tema.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/componentes.dart';
import 'alcance.dart';
import 'detalle_platillo.dart';
import 'editor_platillo.dart';

/// Menú del restaurante: platillos con precio, etiquetas, temporada y agotado (MN-01 a MN-14). En pantallas anchas
/// muestra la lista y el detalle lado a lado.
class MenuPlatillos extends StatefulWidget {
  const MenuPlatillos({super.key});

  @override
  State<MenuPlatillos> createState() => _MenuPlatillosState();
}

enum _Filtro { todos, agotados, fueraDeTemporada, noPublicados }

class _MenuPlatillosState extends State<MenuPlatillos> {
  String _busqueda = '';
  String? _categoriaId;
  _Filtro _filtro = _Filtro.todos;
  String? _seleccionado;

  List<Platillo> _visibles(RestauranteDemo r) {
    final texto = _busqueda.trim().toLowerCase();
    return r.platillos.where((p) {
      if (texto.isNotEmpty && !p.nombre.toLowerCase().contains(texto)) {
        return false;
      }
      if (_categoriaId != null && p.categoriaId != _categoriaId) {
        return false;
      }
      return switch (_filtro) {
        _Filtro.todos => true,
        _Filtro.agotados => p.agotado,
        _Filtro.fueraDeTemporada => p.temporada != null && !p.temporada!.contiene(r.ahora),
        _Filtro.noPublicados => !p.publicado,
      };
    }).toList();
  }

  Future<void> _nuevo() async {
    await mostrarEditorPlatillo(context);
  }

  void _abrir(Platillo p) {
    if (context.disposicion.esAncha) {
      setState(() => _seleccionado = p.id);
    } else {
      abrirPantalla<void>(context, (_) => PantallaDetallePlatillo(platilloId: p.id));
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final ancha = context.disposicion.esAncha;
    final visibles = _visibles(r);
    final seleccionado = ancha ? (r.platillo(_seleccionado ?? '') ?? (visibles.isEmpty ? null : visibles.first)) : null;
    final lista = _Lista(
      visibles: visibles,
      seleccionadoId: seleccionado?.id,
      categoriaId: _categoriaId,
      filtro: _filtro,
      alBuscar: (t) => setState(() => _busqueda = t),
      alElegirCategoria: (c) => setState(() => _categoriaId = c),
      alElegirFiltro: (f) => setState(() => _filtro = f),
      alTocar: _abrir,
      alNuevo: r.puede(Permiso.menuEditar) && ancha ? _nuevo : null,
    );
    final nuevo = r.puede(Permiso.menuEditar) && !ancha
        ? FloatingActionButton.extended(
            key: const Key('nuevo-platillo'),
            onPressed: _nuevo,
            icon: const Icon(Icons.add),
            label: const Text('Nuevo platillo'),
          )
        : null;
    final cuerpo = ancha
        ? Row(
            children: [
              SizedBox(width: context.disposicion == Disposicion.escritorio ? 460 : 380, child: lista),
              VerticalDivider(width: 1, color: Theme.of(context).colorScheme.outlineVariant),
              Expanded(
                child: seleccionado == null
                    ? const EstadoVacio(icono: Icons.menu_book_outlined, titulo: 'Elige un platillo', mensaje: 'Aquí verás su receta, costo y margen.')
                    : DetallePlatillo(key: ValueKey(seleccionado.id), platilloId: seleccionado.id, enPanel: true),
              ),
            ],
          )
        : lista;
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: nuevo,
      body: cuerpo,
    );
  }
}

class _Lista extends StatelessWidget {
  const _Lista({
    required this.visibles,
    required this.seleccionadoId,
    required this.categoriaId,
    required this.filtro,
    required this.alBuscar,
    required this.alElegirCategoria,
    required this.alElegirFiltro,
    required this.alTocar,
    this.alNuevo,
  });

  final VoidCallback? alNuevo;
  final List<Platillo> visibles;
  final String? seleccionadoId;
  final String? categoriaId;
  final _Filtro filtro;
  final ValueChanged<String> alBuscar;
  final ValueChanged<String?> alElegirCategoria;
  final ValueChanged<_Filtro> alElegirFiltro;
  final ValueChanged<Platillo> alTocar;

  static const _etiquetasFiltro = {
    _Filtro.todos: 'Todos',
    _Filtro.agotados: 'Agotados',
    _Filtro.fueraDeTemporada: 'Fuera de temporada',
    _Filtro.noPublicados: 'No publicados',
  };

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final verCosto = r.puede(Permiso.costoVer);
    final grupos = <CategoriaMenu?, List<Platillo>>{};
    for (final c in r.categorias) {
      final lista = visibles.where((p) => p.categoriaId == c.id).toList();
      if (lista.isNotEmpty) {
        grupos[c] = lista;
      }
    }
    final sinCategoria = visibles.where((p) => r.categoria(p.categoriaId) == null).toList();
    if (sinCategoria.isNotEmpty) {
      grupos[null] = sinCategoria;
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  key: const Key('buscar-platillo'),
                  onChanged: alBuscar,
                  decoration: const InputDecoration(
                    hintText: 'Buscar platillo',
                    prefixIcon: Icon(Icons.search),
                    isDense: true,
                  ),
                ),
              ),
              if (alNuevo != null) ...[
                const SizedBox(width: 8),
                IconButton.filled(
                  key: const Key('nuevo-platillo'),
                  tooltip: 'Nuevo platillo',
                  onPressed: alNuevo,
                  icon: const Icon(Icons.add),
                ),
              ],
            ],
          ),
        ),
        SizedBox(
          height: 44,
          child: ListView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            children: [
              for (final f in _Filtro.values)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    key: Key('filtro-${f.name}'),
                    label: Text(_etiquetasFiltro[f]!),
                    selected: filtro == f,
                    onSelected: (_) => alElegirFiltro(f),
                  ),
                ),
            ],
          ),
        ),
        SizedBox(
          height: 44,
          child: ListView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            children: [
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: const Text('Todas las categorías'),
                  selected: categoriaId == null,
                  onSelected: (_) => alElegirCategoria(null),
                ),
              ),
              for (final c in r.categorias)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    avatar: c.estacion == Estacion.barra ? const Icon(Icons.local_bar_outlined, size: 16) : null,
                    label: Text(c.nombre),
                    selected: categoriaId == c.id,
                    onSelected: (_) => alElegirCategoria(categoriaId == c.id ? null : c.id),
                  ),
                ),
            ],
          ),
        ),
        Expanded(
          child: visibles.isEmpty
              ? const EstadoVacio(icono: Icons.search_off, titulo: 'Sin resultados', mensaje: 'Prueba con otro filtro o búsqueda.')
              : ListView(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 96),
                  children: [
                    for (final entrada in grupos.entries) ...[
                      Padding(
                        padding: const EdgeInsets.only(top: 12, bottom: 8),
                        child: Row(
                          children: [
                            Text((entrada.key?.nombre ?? 'Sin categoría').toUpperCase(),
                                style: Theme.of(context).textTheme.labelMedium?.copyWith(
                                    letterSpacing: 0.8, color: Theme.of(context).colorScheme.onSurfaceVariant)),
                            const SizedBox(width: 8),
                            if (entrada.key?.estacion == Estacion.barra) const Insignia('Barra', tono: TonoInsignia.info),
                          ],
                        ),
                      ),
                      for (final p in entrada.value)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: _FilaPlatillo(
                            platillo: p,
                            seleccionado: p.id == seleccionadoId,
                            verCosto: verCosto,
                            alTocar: () => alTocar(p),
                          ),
                        ),
                    ],
                  ],
                ),
        ),
      ],
    );
  }
}

class _FilaPlatillo extends StatelessWidget {
  const _FilaPlatillo({required this.platillo, required this.seleccionado, required this.verCosto, required this.alTocar});

  final Platillo platillo;
  final bool seleccionado;
  final bool verCosto;
  final VoidCallback alTocar;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final fuera = platillo.temporada != null && !platillo.temporada!.contiene(r.ahora);
    final receta = r.recetaVigente(platillo.id);
    final costo = receta == null ? null : r.costoDe(receta, platillo.precio);
    final apagado = platillo.agotado || fuera;
    return Card(
      color: seleccionado ? tema.colorScheme.primaryContainer.withValues(alpha: 0.55) : null,
      shape: RoundedRectangleBorder(
        borderRadius: Formas.radioTarjeta,
        side: BorderSide(color: seleccionado ? tema.colorScheme.primary : context.colores.borde, width: seleccionado ? 1.5 : 1),
      ),
      child: InkWell(
        key: Key('platillo-${platillo.nombre}'),
        onTap: alTocar,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              Opacity(opacity: apagado ? 0.5 : 1, child: FotoPlatillo(platillo: platillo, estacion: r.estacionDe(platillo))),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      platillo.nombre,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: tema.textTheme.titleSmall?.copyWith(
                        decoration: platillo.agotado ? TextDecoration.lineThrough : null,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Wrap(
                      spacing: 6,
                      runSpacing: 4,
                      children: [
                        if (platillo.agotado) const Insignia('Agotado', tono: TonoInsignia.peligro, solida: true),
                        if (fuera) Insignia('Fuera de temporada · ${platillo.temporada!.texto}', tono: TonoInsignia.neutro),
                        if (!platillo.publicado) const Insignia('No publicado', tono: TonoInsignia.neutro, icono: Icons.visibility_off_outlined),
                        if (receta != null && verCosto && costo != null)
                          costo.incompleto
                              ? const Insignia('Costo incompleto', tono: TonoInsignia.aviso)
                              : Insignia('Margen ${porcentajeTexto(costo.margenPuntosBase!)}', tono: TonoInsignia.exito),
                        if (receta == null) const Insignia('Sin receta', tono: TonoInsignia.neutro),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(pesos(platillo.precio), style: tema.textTheme.titleMedium),
                  const SizedBox(height: 4),
                  Icon(Icons.chevron_right, color: tema.colorScheme.outline),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
