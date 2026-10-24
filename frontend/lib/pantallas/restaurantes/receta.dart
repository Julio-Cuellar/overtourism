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

/// Receta de un platillo con sus versiones, insumos, costo teórico y margen (RC-01 a RC-10). El costo y el margen solo
/// los ve quien tiene `restaurantes.costo.ver`; el cocinero ve insumos y cantidades.
class SeccionReceta extends StatefulWidget {
  const SeccionReceta({super.key, required this.platilloId});

  final String platilloId;

  @override
  State<SeccionReceta> createState() => _SeccionRecetaState();
}

class _SeccionRecetaState extends State<SeccionReceta> {
  int? _versionElegida;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final platillo = r.platillo(widget.platilloId)!;
    final versiones = r.versionesDe(widget.platilloId);
    final tema = Theme.of(context);
    final puedeVer = r.puede(Permiso.recetaVer) || r.puede(Permiso.recetaEditar);
    final puedeEditar = r.puede(Permiso.recetaEditar);
    final verCosto = r.puede(Permiso.costoVer);

    if (!puedeVer) {
      return const Seccion(
        titulo: 'Receta',
        child: Card(child: Padding(padding: EdgeInsets.all(16), child: Text('Tu puesto no tiene acceso a las recetas.'))),
      );
    }
    final vigente = r.recetaVigente(widget.platilloId);
    final elegida = versiones.where((v) => v.numero == _versionElegida).firstOrNull ?? vigente;

    final accion = puedeEditar
        ? FilledButton.tonalIcon(
            key: const Key('editar-receta'),
            onPressed: () => abrirEditorReceta(context, platillo.id),
            icon: Icon(vigente == null ? Icons.add : Icons.edit_outlined),
            label: Text(vigente == null ? 'Crear receta' : 'Nueva versión'),
          )
        : null;

    if (elegida == null) {
      return Seccion(
        titulo: 'Receta',
        accion: accion,
        child: const Card(
          child: Padding(
            padding: EdgeInsets.all(16),
            child: Text('Este platillo no tiene receta. Sin receta no se descuentan insumos ni se calcula el costo.'),
          ),
        ),
      );
    }
    final costo = r.costoDe(elegida, platillo.precio);
    return Seccion(
      titulo: 'Receta y costo teórico',
      subtitulo: 'Para ${elegida.rendimiento} ${elegida.rendimiento == 1 ? 'porción' : 'porciones'}',
      accion: accion,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (versiones.length > 1)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Wrap(
                spacing: 8,
                children: [
                  for (final v in versiones.reversed)
                    ChoiceChip(
                      label: Text('Versión ${v.numero}${v.vigente ? ' · vigente' : ''}'),
                      selected: v.numero == elegida.numero,
                      onSelected: (_) => setState(() => _versionElegida = v.numero),
                    ),
                ],
              ),
            ),
          Card(
            child: Column(
              children: [
                for (var i = 0; i < elegida.insumos.length; i++) ...[
                  if (i > 0) const Divider(),
                  _FilaInsumo(insumo: elegida.insumos[i], rendimiento: elegida.rendimiento, verCosto: verCosto),
                ],
              ],
            ),
          ),
          if (verCosto && elegida.rendimiento > 1)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Text('Los importes de los insumos son del lote completo; el costo teórico es por porción.',
                  style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
            ),
          const SizedBox(height: 12),
          if (verCosto)
            _ResumenCosto(costo: costo, precio: platillo.precio)
          else
            Text('El costo y el margen solo los ve quien tiene el permiso de costos.',
                style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
          if (!elegida.vigente)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text('Versión cerrada: se conserva para las ventas que se hicieron con ella (RC-07).',
                  style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
            ),
        ],
      ),
    );
  }
}

class _FilaInsumo extends StatelessWidget {
  const _FilaInsumo({required this.insumo, required this.rendimiento, required this.verCosto});

  final InsumoReceta insumo;
  final int rendimiento;
  final bool verCosto;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final articulo = r.articulo(insumo.articuloId);
    final unidad = articulo?.unidad ?? '';
    // Bruto = neto × (1 + merma/100), en diezmilésimas para mostrar decimales exactos.
    final bruto = insumo.cantidad * (100 + insumo.mermaPct) * 100;
    final cpp = articulo?.cpp;
    final importe = cpp == null ? null : ReglaCosto.centavosDeInsumo(insumo, cpp);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Flexible(child: Text(articulo?.nombre ?? insumo.articuloId, overflow: TextOverflow.ellipsis)),
                    if (articulo != null && !articulo.activo) ...[
                      const SizedBox(width: 6),
                      const Insignia('Dado de baja', tono: TonoInsignia.peligro),
                    ],
                  ],
                ),
                Text(
                  'Neto ${baseTexto(insumo.cantidad, unidad)} · merma ${insumo.mermaPct} % · bruto ${cantidadTexto(bruto, unidad)}',
                  style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
          ),
          if (verCosto)
            importe == null
                ? const Insignia('Sin costo', tono: TonoInsignia.aviso)
                : Text(pesos(importe), style: tema.textTheme.titleSmall),
        ],
      ),
    );
  }
}

/// Costo de un insumo en centavos sin dividir entre el rendimiento: solo para mostrar la fila (el total se calcula con
/// `RestauranteDemo.costoDe`, que redondea una vez).
class ReglaCosto {
  ReglaCosto._();

  static int centavosDeInsumo(InsumoReceta insumo, int cpp) =>
      RestauranteDemo.dividirRedondeado(insumo.cantidad * (100 + insumo.mermaPct) * cpp, 1000);
}

class _ResumenCosto extends StatelessWidget {
  const _ResumenCosto({required this.costo, required this.precio});

  final CostoReceta costo;
  final int precio;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final c = context.colores;
    if (costo.incompleto) {
      return TarjetaConBarra(
        color: c.aviso,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Text('Costo incompleto', style: tema.textTheme.titleSmall),
              const SizedBox(width: 8),
              const Insignia('INCOMPLETO', tono: TonoInsignia.aviso, solida: true),
            ]),
            const SizedBox(height: 4),
            Text('Falta el costo de: ${costo.sinCosto.join(', ')}. No se estima: Inventario lo calcula con las compras (RC-05).',
                style: tema.textTheme.bodySmall),
          ],
        ),
      );
    }
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Expanded(child: _Dato(etiqueta: 'Costo teórico', valor: pesos(costo.costo!))),
          const SizedBox(width: 10),
          Expanded(child: _Dato(etiqueta: 'Precio', valor: pesos(precio))),
          const SizedBox(width: 10),
          Expanded(
            child: _Dato(
              etiqueta: 'Margen',
              valor: pesos(costo.margen!),
              detalle: porcentajeTexto(costo.margenPuntosBase!),
              color: costo.margen! >= 0 ? c.exito : c.peligro,
            ),
          ),
        ],
      ),
    );
  }
}

class _Dato extends StatelessWidget {
  const _Dato({required this.etiqueta, required this.valor, this.detalle, this.color});

  final String etiqueta;
  final String valor;
  final String? detalle;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(etiqueta, style: tema.textTheme.labelMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
            const SizedBox(height: 4),
            FittedBox(
              fit: BoxFit.scaleDown,
              child: Text(valor, style: tema.textTheme.titleLarge?.copyWith(color: color)),
            ),
            if (detalle != null) Text(detalle!, style: tema.textTheme.bodySmall?.copyWith(color: color)),
          ],
        ),
      ),
    );
  }
}

/// Abre el editor de receta (preparar → confirmar). Hoja en teléfono, diálogo en pantallas anchas.
Future<bool?> abrirEditorReceta(BuildContext context, String platilloId) {
  final restaurante = context.restauranteSinEscuchar;
  final contenido = AlcanceRestaurante(restaurante: restaurante, child: _EditorReceta(platilloId: platilloId));
  if (context.disposicion.esAncha) {
    return showDialog<bool>(
      context: context,
      builder: (_) => Dialog(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 640), child: contenido)),
    );
  }
  return showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (_) => contenido,
  );
}

class _FilaEditable {
  _FilaEditable({required this.articuloId, int cantidad = 0, int merma = 0})
      : cantidad = TextEditingController(text: cantidad == 0 ? '' : '$cantidad'),
        merma = TextEditingController(text: '$merma');

  String articuloId;
  final TextEditingController cantidad;
  final TextEditingController merma;

  void liberar() {
    cantidad.dispose();
    merma.dispose();
  }
}

class _EditorReceta extends StatefulWidget {
  const _EditorReceta({required this.platilloId});

  final String platilloId;

  @override
  State<_EditorReceta> createState() => _EditorRecetaState();
}

class _EditorRecetaState extends State<_EditorReceta> {
  final List<_FilaEditable> _filas = [];
  final _rendimiento = TextEditingController(text: '1');
  BorradorReceta? _borrador;
  String? _error;

  @override
  void initState() {
    super.initState();
    final r = context.restauranteSinEscuchar;
    final vigente = r.recetaVigente(widget.platilloId);
    if (vigente != null) {
      _rendimiento.text = '${vigente.rendimiento}';
      for (final i in vigente.insumos) {
        _filas.add(_FilaEditable(articuloId: i.articuloId, cantidad: i.cantidad, merma: i.mermaPct));
      }
    }
  }

  @override
  void dispose() {
    for (final f in _filas) {
      f.liberar();
    }
    _rendimiento.dispose();
    super.dispose();
  }

  List<InsumoReceta> _insumos() => [
        for (final f in _filas)
          InsumoReceta(
            articuloId: f.articuloId,
            cantidad: int.tryParse(f.cantidad.text.trim()) ?? 0,
            mermaPct: int.tryParse(f.merma.text.trim()) ?? 0,
          ),
      ];

  void _revisar() {
    final r = context.restauranteSinEscuchar;
    final rendimiento = int.tryParse(_rendimiento.text.trim()) ?? 0;
    try {
      final borrador = r.prepararReceta(widget.platilloId, rendimiento: rendimiento, insumos: _insumos());
      setState(() {
        _borrador = borrador;
        _error = null;
      });
    } on ReglaRestaurante catch (error) {
      setState(() {
        _borrador = null;
        _error = error.mensaje;
      });
    }
  }

  void _confirmar() {
    final r = context.restauranteSinEscuchar;
    try {
      r.confirmarReceta(_borrador!);
      Navigator.of(context).pop(true);
    } on ReglaRestaurante catch (error) {
      setState(() {
        _borrador = null;
        _error = error.mensaje;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final platillo = r.platillo(widget.platilloId)!;
    final usados = _filas.map((f) => f.articuloId).toSet();
    final disponibles = r.articulos.where((a) => a.activo && !usados.contains(a.id)).toList();
    final verCosto = r.puede(Permiso.costoVer);
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('Receta de ${platillo.nombre}', style: tema.textTheme.titleLarge),
              const SizedBox(height: 4),
              Text('Guardar crea una versión nueva; las anteriores se conservan. Primero se revisa el costo y luego se confirma.',
                  style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
              const SizedBox(height: 16),
              SizedBox(
                width: 220,
                child: TextField(
                  key: const Key('campo-rendimiento'),
                  controller: _rendimiento,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: 'Porciones que rinde', helperText: 'De 1 a 500'),
                  onChanged: (_) => setState(() => _borrador = null),
                ),
              ),
              const SizedBox(height: 16),
              Text('Insumos (cantidad neta en la unidad base)', style: tema.textTheme.labelLarge),
              const SizedBox(height: 8),
              for (var i = 0; i < _filas.length; i++)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        flex: 5,
                        child: Padding(
                          padding: const EdgeInsets.only(top: 14),
                          child: Text(r.articulo(_filas[i].articuloId)?.nombre ?? _filas[i].articuloId,
                              overflow: TextOverflow.ellipsis),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        flex: 3,
                        child: TextField(
                          key: Key('cantidad-$i'),
                          controller: _filas[i].cantidad,
                          keyboardType: TextInputType.number,
                          decoration: InputDecoration(
                            labelText: 'Cantidad',
                            suffixText: r.articulo(_filas[i].articuloId)?.unidad,
                            isDense: true,
                          ),
                          onChanged: (_) => setState(() => _borrador = null),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        flex: 3,
                        child: TextField(
                          key: Key('merma-$i'),
                          controller: _filas[i].merma,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(labelText: 'Merma', suffixText: '%', isDense: true),
                          onChanged: (_) => setState(() => _borrador = null),
                        ),
                      ),
                      IconButton(
                        tooltip: 'Quitar insumo',
                        onPressed: () => setState(() {
                          _filas.removeAt(i).liberar();
                          _borrador = null;
                        }),
                        icon: const Icon(Icons.close),
                      ),
                    ],
                  ),
                ),
              if (disponibles.isNotEmpty)
                PopupMenuButton<Articulo>(
                  key: const Key('agregar-insumo'),
                  tooltip: 'Agregar insumo',
                  onSelected: (a) => setState(() {
                    _filas.add(_FilaEditable(articuloId: a.id));
                    _borrador = null;
                  }),
                  itemBuilder: (_) => [
                    for (final a in disponibles) PopupMenuItem(value: a, child: Text('${a.nombre} (${a.unidad})')),
                  ],
                  child: IgnorePointer(
                    child: OutlinedButton.icon(onPressed: () {}, icon: const Icon(Icons.add), label: const Text('Agregar insumo')),
                  ),
                ),
              if (_error != null) ...[
                const SizedBox(height: 12),
                Container(
                  key: const Key('error-receta'),
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(color: tema.colorScheme.errorContainer, borderRadius: BorderRadius.circular(10)),
                  child: Row(children: [
                    Icon(Icons.error_outline, color: tema.colorScheme.error, size: 20),
                    const SizedBox(width: 8),
                    Expanded(child: Text(_error!, style: TextStyle(color: tema.colorScheme.onErrorContainer))),
                  ]),
                ),
              ],
              if (_borrador != null) ...[
                const SizedBox(height: 16),
                _ResumenDeBorrador(borrador: _borrador!, verCosto: verCosto),
              ],
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Cancelar')),
                  const SizedBox(width: 8),
                  if (_borrador == null)
                    FilledButton(key: const Key('revisar-receta'), onPressed: _revisar, child: const Text('Revisar costo'))
                  else
                    FilledButton(key: const Key('confirmar-receta'), onPressed: _confirmar, child: const Text('Confirmar y guardar')),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ResumenDeBorrador extends StatelessWidget {
  const _ResumenDeBorrador({required this.borrador, required this.verCosto});

  final BorradorReceta borrador;
  final bool verCosto;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final c = context.colores;
    return Card(
      key: const Key('resumen-receta'),
      color: c.infoSuave,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Así quedará la receta', style: tema.textTheme.titleSmall),
            const SizedBox(height: 8),
            if (verCosto && !borrador.costo.incompleto)
              Text('Costo teórico ${pesos(borrador.costo.costo!)} · margen ${pesos(borrador.costo.margen!)} '
                  '(${porcentajeTexto(borrador.costo.margenPuntosBase!)} del precio)')
            else if (verCosto)
              const Text('Costo incompleto: no se calcula el margen.')
            else
              const Text('Se guardará una versión nueva.'),
            for (final aviso in borrador.avisos) ...[
              const SizedBox(height: 6),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.info_outline, size: 16, color: c.aviso),
                  const SizedBox(width: 6),
                  Expanded(child: Text(aviso, style: tema.textTheme.bodySmall)),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}
