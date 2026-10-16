import 'package:flutter/material.dart';

import '../../datos/demo_restaurantes.dart';
import '../../modelos/dinero.dart';
import '../../modelos/restaurantes.dart';
import '../../tema.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/carta_publica.dart';
import '../../widgets/componentes.dart';
import '../../widgets/etiqueta_demo.dart';
import '../comensal/escaner_qr.dart';
import '../restaurantes/alcance.dart';
import 'resenas_restaurante.dart';

/// Directorio de restaurantes del turista: filtros por tipo de cocina, mesas libres y precio medio.
class DirectorioRestaurantes extends StatefulWidget {
  const DirectorioRestaurantes({super.key});

  @override
  State<DirectorioRestaurantes> createState() => _DirectorioRestaurantesState();
}

class _DirectorioRestaurantesState extends State<DirectorioRestaurantes> {
  final Set<TipoCocina> _tipos = {};
  bool _soloAbiertos = false;
  String _busqueda = '';

  List<FichaRestaurante> get _visibles {
    final texto = _busqueda.trim().toLowerCase();
    return directorioDemo.where((f) {
      if (_soloAbiertos && !f.abierto) {
        return false;
      }
      if (_tipos.isNotEmpty && !f.tipos.any(_tipos.contains)) {
        return false;
      }
      return texto.isEmpty || f.nombre.toLowerCase().contains(texto) || f.zona.toLowerCase().contains(texto);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final visibles = _visibles;
    final ancho = MediaQuery.sizeOf(context).width;
    final columnas = ancho >= 1120 ? 3 : (ancho >= 720 ? 2 : 1);
    return Column(
      children: [
        const EtiquetaDemo(detalle: 'restaurantes ficticios; después, negocios reales del DENUE'),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.symmetric(vertical: 16),
            children: [
              ContenidoCentrado(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Dónde comer', style: Theme.of(context).textTheme.headlineSmall),
                    const SizedBox(height: 12),
                    TextField(
                      key: const Key('buscar-restaurante'),
                      onChanged: (t) => setState(() => _busqueda = t),
                      decoration: const InputDecoration(hintText: 'Buscar por nombre o zona', prefixIcon: Icon(Icons.search), isDense: true),
                    ),
                    const SizedBox(height: 10),
                    SizedBox(
                      height: 44,
                      child: ListView(
                        scrollDirection: Axis.horizontal,
                        children: [
                          Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: FilterChip(
                              avatar: const Icon(Icons.schedule, size: 16),
                              label: const Text('Abiertos'),
                              selected: _soloAbiertos,
                              onSelected: (v) => setState(() => _soloAbiertos = v),
                            ),
                          ),
                          for (final t in TipoCocina.values)
                            Padding(
                              padding: const EdgeInsets.only(right: 8),
                              child: FilterChip(
                                key: Key('tipo-${t.name}'),
                                label: Text(t.etiqueta),
                                selected: _tipos.contains(t),
                                onSelected: (v) => setState(() => v ? _tipos.add(t) : _tipos.remove(t)),
                              ),
                            ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                    if (visibles.isEmpty)
                      const Padding(
                        padding: EdgeInsets.only(top: 40),
                        child: EstadoVacio(icono: Icons.search_off, titulo: 'No hay restaurantes con esos filtros'),
                      )
                    else
                      LayoutBuilder(
                        builder: (context, restricciones) {
                          const separacion = 14.0;
                          final anchoTarjeta = (restricciones.maxWidth - separacion * (columnas - 1)) / columnas;
                          return Wrap(
                            spacing: separacion,
                            runSpacing: separacion,
                            children: [
                              for (final f in visibles) SizedBox(width: anchoTarjeta, child: _TarjetaRestaurante(ficha: f)),
                            ],
                          );
                        },
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// Mesas libres de una ficha: en vivo para el restaurante de la demo, publicadas para los demás.
({int? libres, int? total, bool abierto}) disponibilidadDe(BuildContext context, FichaRestaurante f) {
  if (f.esElDeLaDemo) {
    final d = context.restaurante.disponibilidad;
    return (libres: d.libres, total: d.total, abierto: d.abierto);
  }
  return (libres: f.mesasLibres, total: f.mesasTotal, abierto: f.abierto);
}

class _TarjetaRestaurante extends StatelessWidget {
  const _TarjetaRestaurante({required this.ficha});

  final FichaRestaurante ficha;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final d = disponibilidadDe(context, ficha);
    return Card(
      child: InkWell(
        key: Key('restaurante-${ficha.id}'),
        onTap: () => abrirPantalla<void>(context, (_) => FichaRestauranteTurista(ficha: ficha)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _Portada(ficha: ficha, alto: 96),
            Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(ficha.nombre, style: tema.textTheme.titleMedium),
                  const SizedBox(height: 2),
                  Text('${ficha.zona} · ${ficha.horario}',
                      style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      const Icon(Icons.star_rounded, size: 18, color: Paleta.naranja),
                      const SizedBox(width: 2),
                      Text(ficha.promedio.toStringAsFixed(1), style: tema.textTheme.labelLarge),
                      const SizedBox(width: 10),
                      Text('~${pesos(ficha.precioMedio)} por persona', style: tema.textTheme.bodySmall),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 6,
                    runSpacing: 4,
                    children: [
                      for (final t in ficha.tipos) Insignia(t.etiqueta, tono: TonoInsignia.info),
                      _InsigniaMesas(libres: d.libres, total: d.total, abierto: d.abierto),
                      if (ficha.aceptaPasaporte) const Insignia('Acepta Pasaporte', tono: TonoInsignia.aviso, icono: Icons.local_activity_outlined),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InsigniaMesas extends StatelessWidget {
  const _InsigniaMesas({required this.libres, required this.total, required this.abierto});

  final int? libres;
  final int? total;
  final bool abierto;

  @override
  Widget build(BuildContext context) {
    if (!abierto) {
      return const Insignia('Cerrado', tono: TonoInsignia.neutro, icono: Icons.lock_outline);
    }
    if (libres == null || total == null) {
      return const Insignia('Sin datos de mesas', tono: TonoInsignia.neutro);
    }
    if (libres == 0) {
      return Insignia('Lleno · 0 de $total', tono: TonoInsignia.peligro, icono: Icons.table_restaurant_outlined);
    }
    return Insignia('Mesas libres $libres de $total', tono: TonoInsignia.exito, icono: Icons.table_restaurant_outlined);
  }
}

class _Portada extends StatelessWidget {
  const _Portada({required this.ficha, required this.alto});

  final FichaRestaurante ficha;
  final double alto;

  static const _tonos = [Paleta.azulTalavera, Paleta.naranja, Paleta.verde, Paleta.cafe, Paleta.rojo, Paleta.azulCielo];

  @override
  Widget build(BuildContext context) {
    final tono = _tonos[ficha.tono % _tonos.length];
    return Container(
      height: alto,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [tono, Color.alphaBlend(Paleta.azulNoche.withValues(alpha: 0.55), tono)],
        ),
      ),
      child: Stack(
        children: [
          Positioned(right: -10, bottom: -18, child: Icon(Icons.restaurant, size: 110, color: Colors.white.withValues(alpha: 0.12))),
          Positioned(
            left: 14,
            bottom: 12,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(color: Colors.white, borderRadius: Formas.radioAfilado),
              child: Text(ficha.zona.toUpperCase(),
                  style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w800, letterSpacing: 0.8, color: Paleta.azulNoche)),
            ),
          ),
        ],
      ),
    );
  }
}

/// Ficha de un restaurante: disponibilidad de mesas, menú público y reseñas. En el restaurante de la demo el menú es
/// el publicado por el dueño (sin costos ni recetas) y se puede probar el QR de una mesa.
class FichaRestauranteTurista extends StatelessWidget {
  const FichaRestauranteTurista({super.key, required this.ficha});

  final FichaRestaurante ficha;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final d = disponibilidadDe(context, ficha);
    final menu = ficha.esElDeLaDemo ? r.menuPublico() : menuDeEjemplo(ficha.id);
    final ancha = context.disposicion.esAncha;
    final izquierda = <Widget>[
      Text(ficha.nombre, style: tema.textTheme.headlineSmall),
      const SizedBox(height: 4),
      Text(ficha.descripcion, style: tema.textTheme.bodyLarge),
      const SizedBox(height: 12),
      ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.place_outlined), title: Text(ficha.zona)),
      ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.schedule), title: Text(ficha.horario)),
      const SizedBox(height: 8),
      Wrap(
        spacing: 6,
        runSpacing: 6,
        children: [
          for (final t in ficha.tipos) Insignia(t.etiqueta, tono: TonoInsignia.info),
          _InsigniaMesas(libres: d.libres, total: d.total, abierto: d.abierto),
          if (ficha.aceptaPasaporte) const Insignia('Acepta Pasaporte', tono: TonoInsignia.aviso, icono: Icons.local_activity_outlined),
        ],
      ),
      if (ficha.aceptaPasaporte) ...[
        const SizedBox(height: 14),
        Card(
          color: tema.colorScheme.tertiaryContainer,
          child: const ListTile(
            leading: Icon(Icons.local_activity),
            title: Text(r'$20 de descuento en tu consumo'),
            subtitle: Text('Muestra tu cupón del Pasaporte al pagar'),
          ),
        ),
      ],
      if (ficha.esElDeLaDemo) ...[
        const SizedBox(height: 14),
        FilledButton.icon(
          key: const Key('estoy-en-mesa'),
          onPressed: () => abrirPantalla<void>(context, (_) => const EscanearQr()),
          icon: const Icon(Icons.qr_code_scanner),
          label: const Text('Estoy en una mesa: escanear QR'),
        ),
        const SizedBox(height: 6),
        Text('Pide desde tu celular y sigue tu pedido, sin crear cuenta.',
            style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
      ],
    ];
    final resenas = ResenasRestaurante(resenas: ficha.resenas);
    final carta = menu == null
        ? const EstadoVacio(icono: Icons.pause_circle_outline, titulo: 'Este restaurante pausó su publicación', mensaje: 'Vuelve más tarde.')
        : CartaPublica(
            platillos: menu,
            padding: EdgeInsets.zero,
            encabezado: Padding(padding: const EdgeInsets.only(bottom: 8), child: Text('Menú', style: tema.textTheme.titleLarge)),
          );
    return Scaffold(
      appBar: AppBar(title: Text(ficha.nombre, overflow: TextOverflow.ellipsis)),
      body: Column(
        children: [
          const EtiquetaDemo(detalle: 'restaurante ficticio'),
          Expanded(
            child: ancha
                ? Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        flex: 4,
                        child: ListView(padding: const EdgeInsets.all(24), children: [_Portada(ficha: ficha, alto: 140), const SizedBox(height: 16), ...izquierda, const SizedBox(height: 20), resenas]),
                      ),
                      VerticalDivider(width: 1, color: tema.colorScheme.outlineVariant),
                      Expanded(flex: 5, child: Padding(padding: const EdgeInsets.all(24), child: carta)),
                    ],
                  )
                : ListView(
                    padding: EdgeInsets.zero,
                    children: [
                      _Portada(ficha: ficha, alto: 120),
                      Padding(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: izquierda)),
                      if (menu != null)
                        CartaPublica(
                          platillos: menu,
                          enLinea: true,
                          padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                          encabezado: Padding(padding: const EdgeInsets.only(bottom: 8), child: Text('Menú', style: tema.textTheme.titleLarge)),
                        )
                      else
                        carta,
                      Padding(padding: const EdgeInsets.fromLTRB(16, 0, 16, 32), child: resenas),
                    ],
                  ),
          ),
        ],
      ),
    );
  }
}
