import 'package:flutter_test/flutter_test.dart';
import 'package:negocio360/datos/demo.dart';
import 'package:negocio360/estado/caja_demo.dart';
import 'package:negocio360/estado/carrito.dart';
import 'package:negocio360/modelos/metodo_pago.dart';

/// Caja simulada del modo negocio: turno con fondo, ventas por metodo y corte (CJ-01, CJ-03, CJ-06, CJ-12).
void main() {
  final mole = productosDemo.firstWhere((p) => p.descripcion.startsWith('Mole'));

  Carrito unMole() => Carrito()..agregar(mole);

  test('sin turno abierto no se puede vender', () {
    final caja = CajaDemo();

    expect(caja.turnoAbierto, isFalse);
    expect(() => caja.cobrar(unMole(), MetodoPago.efectivo), throwsStateError);
  });

  test('el esperado en efectivo es el fondo mas las ventas en efectivo', () {
    final caja = CajaDemo()..abrirTurno(50000);

    caja.cobrar(unMole(), MetodoPago.efectivo);
    caja.cobrar(unMole(), MetodoPago.tarjeta);

    expect(caja.esperado(MetodoPago.efectivo), 62000);
    expect(caja.esperado(MetodoPago.tarjeta), 12000);
    expect(caja.ventas, hasLength(2));
    expect(caja.ventas.first.folio, 1);
  });

  test('el corte compara lo contado con lo esperado y cierra el turno', () {
    final caja = CajaDemo()..abrirTurno(50000);
    caja.cobrar(unMole(), MetodoPago.efectivo);

    final corte = caja.cerrarTurno({MetodoPago.efectivo: 61000, MetodoPago.tarjeta: 0});

    expect(corte.diferenciaEfectivo, -1000);
    expect(caja.turnoAbierto, isFalse);
  });

  test('no se cobra un carrito vacio', () {
    final caja = CajaDemo()..abrirTurno(0);

    expect(() => caja.cobrar(Carrito(), MetodoPago.efectivo), throwsArgumentError);
  });
}
