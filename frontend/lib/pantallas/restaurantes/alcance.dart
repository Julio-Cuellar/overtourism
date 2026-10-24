import 'package:flutter/widgets.dart';

import '../../estado/restaurante_demo.dart';

/// Pone el estado del restaurante al alcance de todas las pantallas de Restaurantes, la ficha del turista y la página
/// del comensal. Quien lo lea con `context.restaurante` se reconstruye cuando el estado cambia.
class AlcanceRestaurante extends InheritedNotifier<RestauranteDemo> {
  const AlcanceRestaurante({super.key, required RestauranteDemo restaurante, required super.child})
      : super(notifier: restaurante);

  static RestauranteDemo de(BuildContext context) {
    final alcance = context.dependOnInheritedWidgetOfExactType<AlcanceRestaurante>();
    assert(alcance != null, 'Falta un AlcanceRestaurante arriba en el árbol de widgets.');
    return alcance!.notifier!;
  }

  /// Lee el estado sin suscribirse a sus cambios (para manejadores de eventos).
  static RestauranteDemo leer(BuildContext context) {
    final alcance = context.getInheritedWidgetOfExactType<AlcanceRestaurante>();
    assert(alcance != null, 'Falta un AlcanceRestaurante arriba en el árbol de widgets.');
    return alcance!.notifier!;
  }
}

extension ContextoRestaurante on BuildContext {
  /// Estado del restaurante; la pantalla se reconstruye cuando cambia.
  RestauranteDemo get restaurante => AlcanceRestaurante.de(this);

  /// Estado del restaurante sin suscribirse (para botones y manejadores).
  RestauranteDemo get restauranteSinEscuchar => AlcanceRestaurante.leer(this);
}

/// Abre una pantalla nueva conservando el estado del restaurante: las rutas de `Navigator` no cuelgan del
/// [AlcanceRestaurante] del panel, así que se vuelve a poner.
///
/// Desde fuera de un [AlcanceRestaurante] (la portada) se pasa [restaurante] explícitamente.
Future<T?> abrirPantalla<T>(BuildContext context, WidgetBuilder constructor, {RestauranteDemo? restaurante}) {
  restaurante ??= context.restauranteSinEscuchar;
  return Navigator.of(context).push<T>(
    PageRouteBuilder<T>(
      pageBuilder: (contexto, _, _) =>
          AlcanceRestaurante(restaurante: restaurante!, child: Builder(builder: constructor)),
      transitionDuration: const Duration(milliseconds: 220),
      reverseTransitionDuration: const Duration(milliseconds: 180),
      transitionsBuilder: (contexto, animacion, _, hijo) => FadeTransition(
        opacity: CurvedAnimation(parent: animacion, curve: Curves.easeOut),
        child: hijo,
      ),
    ),
  );
}
