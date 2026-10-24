import 'package:flutter/material.dart';

import '../../modelos/formato.dart';
import '../../modelos/permisos_restaurante.dart';
import '../../modelos/restaurantes.dart';
import '../../tema.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/componentes.dart';
import 'alcance.dart';

/// Insumos de las recetas tal como los ve Restaurantes en Inventario: existencia, mínimo y costo promedio (CPP). La
/// existencia baja sola cuando se cobra un platillo con receta; si Inventario no puede descontar, queda un consumo
/// sin descontar para revisar (CM-06) y la venta nunca se bloquea.
class Insumos extends StatelessWidget {
  const Insumos({super.key});

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final verCosto = r.puede(Permiso.costoVer);
    final articulos = [...r.articulos]..sort((a, b) {
        if (a.bajoMinimo != b.bajoMinimo) {
          return a.bajoMinimo ? -1 : 1;
        }
        return a.nombre.compareTo(b.nombre);
      });
    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 16),
      children: [
        ContenidoCentrado(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Insumos', style: tema.textTheme.headlineSmall),
              const SizedBox(height: 4),
              Text(
                'Existencias que descuentan las recetas al cobrar. Inventario es dueño de estos datos; aquí solo se consultan.',
                style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 20),
              if (r.consumosFallidos.isNotEmpty) ...[
                Seccion(
                  titulo: 'Consumos sin descontar',
                  subtitulo: 'La venta se cobró; Inventario no pudo descontar estos insumos.',
                  child: Column(
                    children: [
                      for (final f in r.consumosFallidos)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: TarjetaConBarra(
                            color: context.colores.peligro,
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Icon(Icons.sync_problem_outlined, color: context.colores.peligro),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text('Venta ${f.ventaFolio}', style: tema.textTheme.titleSmall),
                                      Text(f.detalle, style: tema.textTheme.bodySmall),
                                    ],
                                  ),
                                ),
                                Insignia(f.codigo, tono: TonoInsignia.peligro),
                              ],
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
              ],
              Seccion(
                titulo: 'Existencias',
                child: LayoutBuilder(
                  builder: (context, restricciones) {
                    const separacion = 12.0;
                    final columnas = restricciones.maxWidth >= 900 ? 3 : (restricciones.maxWidth >= 560 ? 2 : 1);
                    final ancho = (restricciones.maxWidth - separacion * (columnas - 1)) / columnas;
                    return Wrap(
                      spacing: separacion,
                      runSpacing: separacion,
                      children: [
                        for (final a in articulos) SizedBox(width: ancho, child: _TarjetaArticulo(articulo: a, verCosto: verCosto)),
                      ],
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _TarjetaArticulo extends StatelessWidget {
  const _TarjetaArticulo({required this.articulo, required this.verCosto});

  final Articulo articulo;
  final bool verCosto;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final c = context.colores;
    final bajo = articulo.activo && articulo.bajoMinimo;
    final proporcion = articulo.minimo == 0 ? 1.0 : (articulo.existencia / (articulo.minimo * 3)).clamp(0.0, 1.0);
    return TarjetaConBarra(
      color: !articulo.activo ? c.neutro : (bajo ? c.aviso : c.exito),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(articulo.nombre, style: tema.textTheme.titleSmall, overflow: TextOverflow.ellipsis)),
              if (!articulo.activo)
                const Insignia('Dado de baja', tono: TonoInsignia.neutro)
              else if (bajo)
                const Insignia('Bajo el mínimo', tono: TonoInsignia.aviso, solida: true),
            ],
          ),
          const SizedBox(height: 8),
          Text(cantidadTexto(articulo.existencia, articulo.unidad), style: tema.textTheme.titleLarge),
          Text('Mínimo ${cantidadTexto(articulo.minimo, articulo.unidad)}',
              style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: Formas.radioAfilado,
            child: LinearProgressIndicator(
              value: proporcion,
              minHeight: 5,
              backgroundColor: c.neutroSuave,
              color: bajo ? c.aviso : c.exito,
            ),
          ),
          if (verCosto) ...[
            const SizedBox(height: 8),
            Text(
              articulo.cpp == null
                  ? 'Sin costo: Inventario aún no lo calcula'
                  : 'CPP \$${_milesimas(articulo.cpp!)} por ${articulo.unidad}',
              style: tema.textTheme.bodySmall?.copyWith(color: articulo.cpp == null ? c.aviso : tema.colorScheme.onSurfaceVariant),
            ),
          ],
        ],
      ),
    );
  }

  /// Milésimas de peso como decimal: `180` → `0.180`, `6000` → `6.000`.
  static String _milesimas(int valor) => '${valor ~/ 1000}.${(valor % 1000).toString().padLeft(3, '0')}';
}
