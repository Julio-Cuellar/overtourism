import 'package:flutter/material.dart';

import '../../estado/restaurante_demo.dart';
import '../../modelos/permisos_restaurante.dart';
import '../../modelos/restaurantes.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/componentes.dart';
import '../../widgets/etiqueta_demo.dart';
import 'ajustes.dart';
import 'alcance.dart';
import 'cocina.dart';
import 'insumos.dart';
import 'menu_platillos.dart';
import 'panel.dart';
import 'salon.dart';

/// Modo restaurante: el panel del personal. Las secciones cambian según el puesto; la maqueta deja elegir el puesto
/// para ver cómo se comporta cada uno (con el servidor, el puesto sale de la sesión de Identidad).
class ModoRestaurante extends StatelessWidget {
  const ModoRestaurante({super.key, required this.restaurante});

  final RestauranteDemo restaurante;

  @override
  Widget build(BuildContext context) {
    return AlcanceRestaurante(restaurante: restaurante, child: const _Estructura());
  }
}

class _Seccion {
  const _Seccion(this.destino, this.constructor, {this.cuenta});

  final DestinoNavegacion destino;
  final WidgetBuilder constructor;
  final int Function(RestauranteDemo)? cuenta;
}

class _Estructura extends StatefulWidget {
  const _Estructura();

  @override
  State<_Estructura> createState() => _EstructuraState();
}

class _EstructuraState extends State<_Estructura> {
  int _indice = 0;
  RolRestaurante? _rolAnterior;

  List<_Seccion> _seccionesDe(RolRestaurante rol) {
    const panel = _Seccion(
      DestinoNavegacion(icono: Icons.dashboard_outlined, iconoActivo: Icons.dashboard, etiqueta: 'Panel'),
      _construirPanel,
    );
    const menu = _Seccion(
      DestinoNavegacion(icono: Icons.menu_book_outlined, iconoActivo: Icons.menu_book, etiqueta: 'Menú'),
      _construirMenu,
    );
    final salon = _Seccion(
      const DestinoNavegacion(icono: Icons.table_restaurant_outlined, iconoActivo: Icons.table_restaurant, etiqueta: 'Salón'),
      _construirSalon,
      cuenta: (r) => r.avisosPendientes,
    );
    final cocina = _Seccion(
      const DestinoNavegacion(icono: Icons.soup_kitchen_outlined, iconoActivo: Icons.soup_kitchen, etiqueta: 'Cocina'),
      _construirCocina,
      cuenta: (r) => r.comandas.where((c) => c.estado == EstadoComanda.nueva).length,
    );
    const insumos = _Seccion(
      DestinoNavegacion(icono: Icons.inventory_2_outlined, iconoActivo: Icons.inventory_2, etiqueta: 'Insumos'),
      _construirInsumos,
    );
    const ajustes = _Seccion(
      DestinoNavegacion(icono: Icons.tune_outlined, iconoActivo: Icons.tune, etiqueta: 'Ajustes'),
      _construirAjustes,
    );
    return switch (rol) {
      RolRestaurante.dueno => [panel, menu, salon, cocina, insumos, ajustes],
      RolRestaurante.mesero => [salon, cocina, menu],
      RolRestaurante.cocinero => [cocina, menu, insumos],
      RolRestaurante.cajero => [salon, menu],
    };
  }

  @override
  Widget build(BuildContext context) {
    final restaurante = context.restaurante;
    final rol = restaurante.rol;
    if (_rolAnterior != rol) {
      _indice = 0;
      _rolAnterior = rol;
    }
    final secciones = _seccionesDe(rol);
    final indice = _indice.clamp(0, secciones.length - 1);
    final disposicion = context.disposicion;
    return NavegacionAdaptativa(
      titulo: Row(
        children: [
          Flexible(child: Text(disposicion.esMovil ? 'La Cazuela' : restaurante.nombre, overflow: TextOverflow.ellipsis)),
          const SizedBox(width: 10),
          Flexible(child: _InsigniaServicio(abierto: restaurante.servicioAbierto, corto: disposicion.esMovil)),
        ],
      ),
      acciones: [
        _SelectorDePuesto(rol: rol, alElegir: (r) => restaurante.rol = r),
        const SizedBox(width: 8),
      ],
      franja: const EtiquetaDemo(detalle: 'restaurante ficticio; el puesto se elige aquí solo en la maqueta'),
      encabezadoRiel: _EncabezadoRiel(rol: rol),
      destinos: [
        for (final s in secciones)
          DestinoNavegacion(
            icono: s.destino.icono,
            iconoActivo: s.destino.iconoActivo,
            etiqueta: s.destino.etiqueta,
            cuenta: s.cuenta?.call(restaurante) ?? 0,
          ),
      ],
      indice: indice,
      alCambiar: (i) => setState(() => _indice = i),
      cuerpo: KeyedSubtree(key: ValueKey('${rol.name}-$indice'), child: secciones[indice].constructor(context)),
    );
  }
}

Widget _construirPanel(BuildContext context) => const PanelRestaurante();

Widget _construirMenu(BuildContext context) => const MenuPlatillos();

Widget _construirSalon(BuildContext context) => const Salon();

Widget _construirCocina(BuildContext context) => const Cocina();

Widget _construirInsumos(BuildContext context) => const Insumos();

Widget _construirAjustes(BuildContext context) => const AjustesRestaurante();

class _InsigniaServicio extends StatelessWidget {
  const _InsigniaServicio({required this.abierto, required this.corto});

  final bool abierto;

  /// En teléfono el título es angosto y la marca dice solo «Abierto» o «Cerrado».
  final bool corto;

  @override
  Widget build(BuildContext context) => Insignia(
        abierto ? (corto ? 'Abierto' : 'Servicio abierto') : (corto ? 'Cerrado' : 'Servicio cerrado'),
        tono: abierto ? TonoInsignia.exito : TonoInsignia.peligro,
        solida: true,
      );
}

class _EncabezadoRiel extends StatelessWidget {
  const _EncabezadoRiel({required this.rol});

  final RolRestaurante rol;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Puesto', style: tema.textTheme.labelSmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
        const SizedBox(height: 2),
        Text(rol.etiqueta, style: tema.textTheme.titleSmall),
        const SizedBox(height: 2),
        Text(rol.descripcion, style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
      ],
    );
  }
}

class _SelectorDePuesto extends StatelessWidget {
  const _SelectorDePuesto({required this.rol, required this.alElegir});

  final RolRestaurante rol;
  final ValueChanged<RolRestaurante> alElegir;

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<RolRestaurante>(
      key: const Key('selector-puesto'),
      tooltip: 'Ver la app como…',
      onSelected: alElegir,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      itemBuilder: (contexto) => [
        for (final r in RolRestaurante.values)
          PopupMenuItem(
            key: Key('puesto-${r.name}'),
            value: r,
            child: ListTile(
              dense: true,
              contentPadding: EdgeInsets.zero,
              leading: Icon(r == rol ? Icons.radio_button_checked : Icons.radio_button_off, size: 20),
              title: Text(r.etiqueta),
              subtitle: Text(r.descripcion),
            ),
          ),
      ],
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.badge_outlined, size: 18),
            const SizedBox(width: 6),
            Text(rol.etiqueta, style: Theme.of(context).textTheme.labelLarge),
            const Icon(Icons.arrow_drop_down),
          ],
        ),
      ),
    );
  }
}
