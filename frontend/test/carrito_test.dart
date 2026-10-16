import 'package:flutter_test/flutter_test.dart';
import 'package:negocio360/datos/demo.dart';
import 'package:negocio360/estado/carrito.dart';
import 'package:negocio360/modelos/dinero.dart';

/// El carrito calcula en centavos enteros (nunca double), como el servidor calcula con decimales exactos.
void main() {
  final mole = productosDemo.firstWhere((p) => p.descripcion.startsWith('Mole'));
  final camote = productosDemo.firstWhere((p) => p.admiteFraccion);
  final cupon20 = cuponesDemo.firstWhere((c) => c.codigo == 'PASAPORTE-20');

  test('dos ordenes de mole a 120.00 dan subtotal y total de 240.00', () {
    final carrito = Carrito()
      ..agregar(mole)
      ..agregar(mole);

    expect(carrito.subtotal, 24000);
    expect(carrito.total, 24000);
  });

  test('el cupon descuenta su monto y nunca deja el total bajo cero', () {
    final carrito = Carrito()..agregar(mole);
    carrito.aplicarCupon(cupon20);

    expect(carrito.descuento, 2000);
    expect(carrito.total, 10000);

    final barato = Carrito()..agregar(productosDemo.firstWhere((p) => p.descripcion.startsWith('Agua')));
    barato.aplicarCupon(cuponesDemo.firstWhere((c) => c.codigo == 'PASAPORTE-50'));
    expect(barato.total, 0);
  });

  test('a granel se vende por medio kilo: 0.5 kg de camote a 140.00 son 70.00', () {
    final carrito = Carrito()..agregar(camote);

    expect(carrito.cantidadDe(camote), 500);
    expect(carrito.subtotal, 7000);
  });

  test('el cambio es lo recibido menos el total, y nunca negativo', () {
    final carrito = Carrito()..agregar(mole);

    expect(carrito.cambio(20000), 8000);
    expect(carrito.cambio(5000), 0);
  });

  test('quitar resta una unidad y elimina la linea al llegar a cero', () {
    final carrito = Carrito()
      ..agregar(mole)
      ..agregar(mole)
      ..quitar(mole);

    expect(carrito.cantidadDe(mole), 1000);
    carrito.quitar(mole);
    expect(carrito.estaVacio, isTrue);
  });

  test('pesos() escribe con separador de miles y dos decimales', () {
    expect(pesos(123456), r'$1,234.56');
    expect(pesos(0), r'$0.00');
    expect(pesos(-1050), r'-$10.50');
  });
}
