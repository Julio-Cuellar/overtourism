import 'package:flutter/material.dart';

import '../../estado/restaurante_demo.dart';
import '../../modelos/dinero.dart';
import '../../modelos/formato.dart';
import '../../modelos/restaurantes.dart';
import '../../widgets/adaptativo.dart';
import 'alcance.dart';

/// Abre el editor de un platillo (nuevo si [platillo] es nulo): hoja inferior en teléfono, diálogo en pantallas
/// anchas. Devuelve verdadero si se guardó.
Future<bool?> mostrarEditorPlatillo(BuildContext context, {Platillo? platillo}) {
  final restaurante = context.restauranteSinEscuchar;
  final contenido = AlcanceRestaurante(restaurante: restaurante, child: _EditorPlatillo(platillo: platillo));
  if (context.disposicion.esAncha) {
    return showDialog<bool>(
      context: context,
      builder: (_) => Dialog(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 560), child: contenido)),
    );
  }
  return showModalBottomSheet<bool>(context: context, isScrollControlled: true, builder: (_) => contenido);
}

class _EditorPlatillo extends StatefulWidget {
  const _EditorPlatillo({this.platillo});

  final Platillo? platillo;

  @override
  State<_EditorPlatillo> createState() => _EditorPlatilloState();
}

class _EditorPlatilloState extends State<_EditorPlatillo> {
  late final TextEditingController _nombre;
  late final TextEditingController _descripcion;
  late final TextEditingController _precio;
  String? _categoriaId;
  late Set<EtiquetaDieta> _etiquetas;
  late bool _publicado;
  late bool _conTemporada;
  int _mesDesde = 1;
  int _diaDesde = 1;
  int _mesHasta = 12;
  int _diaHasta = 31;
  String? _error;

  @override
  void initState() {
    super.initState();
    final p = widget.platillo;
    _nombre = TextEditingController(text: p?.nombre ?? '');
    _descripcion = TextEditingController(text: p?.descripcion ?? '');
    _precio = TextEditingController(text: p == null ? '' : decimalDe(p.precio));
    _categoriaId = p?.categoriaId;
    _etiquetas = {...?p?.etiquetas};
    _publicado = p?.publicado ?? true;
    final t = p?.temporada;
    _conTemporada = t != null;
    if (t != null) {
      _mesDesde = t.mesDesde;
      _diaDesde = t.diaDesde;
      _mesHasta = t.mesHasta;
      _diaHasta = t.diaHasta;
    }
  }

  @override
  void dispose() {
    _nombre.dispose();
    _descripcion.dispose();
    _precio.dispose();
    super.dispose();
  }

  static const _meses = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

  void _guardar() {
    final centavos = centavosDeTexto(_precio.text);
    if (centavos == null) {
      setState(() => _error = 'El precio debe ser un monto con hasta 2 decimales.');
      return;
    }
    final r = context.restauranteSinEscuchar;
    final temporada = _conTemporada
        ? Temporada(mesDesde: _mesDesde, diaDesde: _diaDesde, mesHasta: _mesHasta, diaHasta: _diaHasta)
        : null;
    try {
      final p = widget.platillo;
      if (p == null) {
        r.crearPlatillo(
          nombre: _nombre.text,
          descripcion: _descripcion.text,
          precio: centavos,
          categoriaId: _categoriaId,
          etiquetas: _etiquetas,
          temporada: temporada,
          publicado: _publicado,
        );
      } else {
        r.editarPlatillo(
          p.id,
          nombre: _nombre.text,
          descripcion: _descripcion.text,
          precio: centavos,
          categoriaId: _categoriaId,
          etiquetas: _etiquetas,
          temporada: temporada,
          publicado: _publicado,
        );
      }
      Navigator.of(context).pop(true);
    } on ReglaRestaurante catch (error) {
      setState(() => _error = error.mensaje);
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final esNuevo = widget.platillo == null;
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(esNuevo ? 'Nuevo platillo' : 'Editar platillo', style: tema.textTheme.titleLarge),
              const SizedBox(height: 16),
              TextField(
                key: const Key('campo-nombre'),
                controller: _nombre,
                textCapitalization: TextCapitalization.sentences,
                decoration: const InputDecoration(labelText: 'Nombre', helperText: 'De 2 a 80 caracteres; único entre los activos.'),
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('campo-descripcion'),
                controller: _descripcion,
                maxLines: 2,
                decoration: const InputDecoration(labelText: 'Descripción'),
              ),
              const SizedBox(height: 12),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: TextField(
                      key: const Key('campo-precio'),
                      controller: _precio,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      decoration: const InputDecoration(labelText: 'Precio', prefixText: r'$ ', helperText: 'Precio final al público'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: DropdownButtonFormField<String?>(
                      key: const Key('campo-categoria'),
                      initialValue: _categoriaId,
                      isExpanded: true,
                      decoration: const InputDecoration(labelText: 'Categoría'),
                      items: [
                        const DropdownMenuItem<String?>(value: null, child: Text('Sin categoría')),
                        for (final c in r.categorias) DropdownMenuItem<String?>(value: c.id, child: Text(c.nombre)),
                      ],
                      onChanged: (v) => setState(() => _categoriaId = v),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Text('Etiquetas (hasta 8)', style: tema.textTheme.labelLarge),
              const SizedBox(height: 4),
              Text('Las declara el negocio y la app las rotula así.',
                  style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final e in EtiquetaDieta.values)
                    FilterChip(
                      label: Text(e.etiqueta),
                      selected: _etiquetas.contains(e),
                      onSelected: (v) => setState(() => v ? _etiquetas.add(e) : _etiquetas.remove(e)),
                    ),
                ],
              ),
              const SizedBox(height: 12),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Solo de temporada'),
                subtitle: const Text('Fuera del rango no se puede pedir ni sale en el menú público.'),
                value: _conTemporada,
                onChanged: (v) => setState(() => _conTemporada = v),
              ),
              if (_conTemporada) ...[
                Row(
                  children: [
                    Expanded(child: _selectorFecha('Desde', _mesDesde, _diaDesde, (m, d) => setState(() {
                          _mesDesde = m;
                          _diaDesde = d;
                        }))),
                    const SizedBox(width: 12),
                    Expanded(child: _selectorFecha('Hasta', _mesHasta, _diaHasta, (m, d) => setState(() {
                          _mesHasta = m;
                          _diaHasta = d;
                        }))),
                  ],
                ),
                const SizedBox(height: 8),
              ],
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Publicado en el menú del turista'),
                value: _publicado,
                onChanged: (v) => setState(() => _publicado = v),
              ),
              if (_error != null) ...[
                const SizedBox(height: 8),
                Container(
                  key: const Key('error-platillo'),
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: tema.colorScheme.errorContainer,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.error_outline, color: tema.colorScheme.error, size: 20),
                      const SizedBox(width: 8),
                      Expanded(child: Text(_error!, style: TextStyle(color: tema.colorScheme.onErrorContainer))),
                    ],
                  ),
                ),
              ],
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Cancelar')),
                  const SizedBox(width: 8),
                  FilledButton(key: const Key('guardar-platillo'), onPressed: _guardar, child: const Text('Guardar')),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _selectorFecha(String etiqueta, int mes, int dia, void Function(int, int) alCambiar) {
    return Row(
      children: [
        Expanded(
          flex: 3,
          child: DropdownButtonFormField<int>(
            initialValue: mes,
            isExpanded: true,
            decoration: InputDecoration(labelText: '$etiqueta · mes'),
            items: [for (var i = 1; i <= 12; i++) DropdownMenuItem(value: i, child: Text(_meses[i - 1]))],
            onChanged: (v) => alCambiar(v ?? mes, dia),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          flex: 2,
          child: DropdownButtonFormField<int>(
            initialValue: dia,
            isExpanded: true,
            decoration: const InputDecoration(labelText: 'Día'),
            items: [for (var i = 1; i <= 31; i++) DropdownMenuItem(value: i, child: Text('$i'))],
            onChanged: (v) => alCambiar(mes, v ?? dia),
          ),
        ),
      ],
    );
  }
}
