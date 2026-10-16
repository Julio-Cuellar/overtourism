import 'package:flutter/foundation.dart';

import '../modelos/modelos.dart';

/// Carrito del cobro en la maqueta. Cantidades en milesimas (1000 = una pieza o un kilo) y dinero en centavos,
/// siempre enteros. Con el servidor conectado, estas cifras son solo la vista previa: el total que vale es el del
/// resumen que devuelve `prepararComando` (principio rector: el codigo del servidor calcula).
class Carrito extends ChangeNotifier {
  final Map<Producto, int> _cantidades = {};
  Cupon? _cupon;

  /// Cuanto suma cada toque: una pieza, o medio kilo a granel (VT-03).
  static int paso(Producto producto) => producto.admiteFraccion ? 500 : 1000;

  bool get estaVacio => _cantidades.isEmpty;

  Map<Producto, int> get lineas => Map.unmodifiable(_cantidades);

  Cupon? get cupon => _cupon;

  int cantidadDe(Producto producto) => _cantidades[producto] ?? 0;

  void agregar(Producto producto) {
    _cantidades.update(producto, (actual) => actual + paso(producto), ifAbsent: () => paso(producto));
    notifyListeners();
  }

  void quitar(Producto producto) {
    final restante = cantidadDe(producto) - paso(producto);
    if (restante <= 0) {
      _cantidades.remove(producto);
    } else {
      _cantidades[producto] = restante;
    }
    notifyListeners();
  }

  void aplicarCupon(Cupon? cupon) {
    _cupon = cupon;
    notifyListeners();
  }

  void vaciar() {
    _cantidades.clear();
    _cupon = null;
    notifyListeners();
  }

  /// Importe de una linea en centavos, redondeado a la mitad hacia arriba (HALF_UP, D-20).
  static int importe(Producto producto, int cantidadMilesimas) =>
      (producto.precio * cantidadMilesimas + 500) ~/ 1000;

  int get subtotal => _cantidades.entries.fold(0, (suma, e) => suma + importe(e.key, e.value));

  /// El descuento nunca pasa del subtotal.
  int get descuento {
    final monto = _cupon?.descuento ?? 0;
    return monto > subtotal ? subtotal : monto;
  }

  int get total => subtotal - descuento;

  /// Cambio del efectivo recibido; cero si no alcanza.
  int cambio(int recibido) => recibido > total ? recibido - total : 0;
}
