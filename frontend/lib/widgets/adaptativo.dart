import 'package:flutter/material.dart';

/// Tres disposiciones para el mismo contenido: teléfono (una columna y barra inferior), tableta (barra lateral y
/// paneles) y escritorio o web (barra lateral extendida y vistas maestro-detalle).
enum Disposicion {
  movil,
  tableta,
  escritorio;

  bool get esMovil => this == Disposicion.movil;

  bool get esAncha => this != Disposicion.movil;
}

class Puntos {
  Puntos._();

  static const double tableta = 720;
  static const double escritorio = 1120;

  /// Ancho máximo del contenido para que el texto no se estire en pantallas grandes.
  static const double contenido = 1280;
}

Disposicion disposicionDeAncho(double ancho) {
  if (ancho >= Puntos.escritorio) {
    return Disposicion.escritorio;
  }
  if (ancho >= Puntos.tableta) {
    return Disposicion.tableta;
  }
  return Disposicion.movil;
}

extension DisposicionContexto on BuildContext {
  /// Disposición según el ancho disponible de la ventana.
  Disposicion get disposicion => disposicionDeAncho(MediaQuery.sizeOf(this).width);
}

/// Centra el contenido con un ancho máximo y un margen lateral cómodo.
class ContenidoCentrado extends StatelessWidget {
  const ContenidoCentrado({
    super.key,
    required this.child,
    this.anchoMaximo = Puntos.contenido,
    this.padding = const EdgeInsets.symmetric(horizontal: 16),
  });

  final Widget child;
  final double anchoMaximo;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: anchoMaximo),
        child: Padding(padding: padding, child: child),
      ),
    );
  }
}

/// Una sección de la navegación principal. `cuenta` pinta una insignia numérica cuando es mayor que cero.
class DestinoNavegacion {
  const DestinoNavegacion({required this.icono, required this.iconoActivo, required this.etiqueta, this.cuenta = 0});

  final IconData icono;
  final IconData iconoActivo;
  final String etiqueta;
  final int cuenta;
}

/// Navegación que se adapta: barra inferior en teléfono (con «Más» si hay más de cinco secciones), riel lateral en
/// tableta y riel extendido en escritorio. El contenido de cada sección lo arma `constructor`.
class NavegacionAdaptativa extends StatelessWidget {
  const NavegacionAdaptativa({
    super.key,
    required this.destinos,
    required this.indice,
    required this.alCambiar,
    required this.cuerpo,
    this.titulo,
    this.acciones = const [],
    this.encabezadoRiel,
    this.franja,
  });

  final List<DestinoNavegacion> destinos;
  final int indice;
  final ValueChanged<int> alCambiar;
  final Widget cuerpo;
  final Widget? titulo;
  final List<Widget> acciones;

  /// Widget sobre el riel lateral (por ejemplo el nombre del restaurante).
  final Widget? encabezadoRiel;

  /// Franja bajo la barra superior (por ejemplo, el rótulo de datos de ejemplo).
  final Widget? franja;

  static const _maxInferior = 5;

  Widget _icono(DestinoNavegacion d, {bool activo = false}) {
    final icono = Icon(activo ? d.iconoActivo : d.icono);
    return d.cuenta > 0 ? Badge.count(count: d.cuenta, child: icono) : icono;
  }

  @override
  Widget build(BuildContext context) {
    final disposicion = context.disposicion;
    final barra = AppBar(title: titulo, actions: acciones);
    final contenido = Column(
      children: [
        ?franja,
        Expanded(child: cuerpo),
      ],
    );
    if (disposicion.esMovil) {
      return Scaffold(
        appBar: barra,
        body: contenido,
        bottomNavigationBar: _barraInferior(context),
      );
    }
    final extendido = disposicion == Disposicion.escritorio;
    return Scaffold(
      appBar: barra,
      body: Row(
        children: [
          DecoratedBox(
            decoration: BoxDecoration(
              border: Border(right: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
            ),
            child: LayoutBuilder(
              builder: (context, restricciones) => SingleChildScrollView(
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: restricciones.maxHeight),
                  child: IntrinsicHeight(
                    child: NavigationRail(
                      extended: extendido,
                      minExtendedWidth: 220,
                      selectedIndex: indice,
                      onDestinationSelected: alCambiar,
                      labelType: extendido ? NavigationRailLabelType.none : NavigationRailLabelType.all,
                      leading: encabezadoRiel == null || !extendido
                          ? null
                          : Padding(padding: const EdgeInsets.fromLTRB(12, 8, 12, 12), child: encabezadoRiel),
                      destinations: [
                        for (final d in destinos)
                          NavigationRailDestination(
                            icon: _icono(d),
                            selectedIcon: _icono(d, activo: true),
                            label: Text(d.etiqueta),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
          Expanded(child: contenido),
        ],
      ),
    );
  }

  Widget _barraInferior(BuildContext context) {
    final conMas = destinos.length > _maxInferior;
    final visibles = conMas ? destinos.take(_maxInferior - 1).toList() : destinos;
    final restantes = conMas ? destinos.skip(_maxInferior - 1).toList() : const <DestinoNavegacion>[];
    final enMas = conMas && indice >= _maxInferior - 1;
    return NavigationBar(
      selectedIndex: enMas ? visibles.length : indice,
      onDestinationSelected: (i) async {
        if (conMas && i == visibles.length) {
          final elegido = await showModalBottomSheet<int>(
            context: context,
            builder: (contexto) => SafeArea(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  for (var j = 0; j < restantes.length; j++)
                    ListTile(
                      leading: _icono(restantes[j]),
                      title: Text(restantes[j].etiqueta),
                      onTap: () => Navigator.of(contexto).pop(visibles.length + j),
                    ),
                ],
              ),
            ),
          );
          if (elegido != null) {
            alCambiar(elegido);
          }
          return;
        }
        alCambiar(i);
      },
      destinations: [
        for (var i = 0; i < visibles.length; i++)
          NavigationDestination(
            icon: _icono(visibles[i]),
            selectedIcon: _icono(visibles[i], activo: true),
            label: visibles[i].etiqueta,
          ),
        if (conMas) const NavigationDestination(icon: Icon(Icons.more_horiz), label: 'Más'),
      ],
    );
  }
}
