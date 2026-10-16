import 'package:flutter/foundation.dart';

import '../modelos/metodo_pago.dart';
import 'carrito.dart';

/// Una venta cobrada en la maqueta.
class VentaDemo {
  const VentaDemo({required this.folio, required this.total, required this.metodo, required this.lineas});

  final int folio;
  final int total;
  final MetodoPago metodo;
  final int lineas;
}

/// Resultado del corte: diferencia en efectivo = contado - esperado (CJ-12).
class CorteDemo {
  const CorteDemo({required this.esperado, required this.contado, required this.diferenciaEfectivo});

  final Map<MetodoPago, int> esperado;
  final Map<MetodoPago, int> contado;
  final int diferenciaEfectivo;
}

/// Caja SIMULADA del modo negocio, en memoria del telefono: turno con fondo, cobros y corte. Reproduce las reglas del
/// servidor (CJ-01, CJ-02, CJ-03, CJ-06, CJ-12) para que la maqueta funcione sin red; con el servidor conectado, este
/// estado lo daran `saldoTurno` y los comandos de Caja por GraphQL.
class CajaDemo extends ChangeNotifier {
  int? _fondo;
  final List<VentaDemo> _ventas = [];
  CorteDemo? _ultimoCorte;

  bool get turnoAbierto => _fondo != null;

  int get fondo => _fondo ?? 0;

  List<VentaDemo> get ventas => List.unmodifiable(_ventas);

  CorteDemo? get ultimoCorte => _ultimoCorte;

  /// Abre turno con un fondo de cero o mas (CJ-01, CJ-02).
  void abrirTurno(int fondoCentavos) {
    if (turnoAbierto) {
      throw StateError('Ya hay un turno abierto');
    }
    if (fondoCentavos < 0) {
      throw ArgumentError.value(fondoCentavos, 'fondo', 'El fondo debe ser cero o más');
    }
    _fondo = fondoCentavos;
    _ventas.clear();
    _ultimoCorte = null;
    notifyListeners();
  }

  /// Cobra el carrito y devuelve la venta con su folio consecutivo (VT-06).
  VentaDemo cobrar(Carrito carrito, MetodoPago metodo) {
    if (!turnoAbierto) {
      throw StateError('Abre un turno de caja antes de vender');
    }
    if (carrito.estaVacio) {
      throw ArgumentError('Agrega al menos un producto a la venta');
    }
    final venta = VentaDemo(folio: _ventas.length + 1, total: carrito.total, metodo: metodo,
        lineas: carrito.lineas.length);
    _ventas.add(venta);
    notifyListeners();
    return venta;
  }

  /// Lo que deberia haber por metodo: el fondo cuenta como efectivo (CJ-03).
  int esperado(MetodoPago metodo) {
    final ventas = _ventas.where((v) => v.metodo == metodo).fold(0, (suma, v) => suma + v.total);
    return metodo == MetodoPago.efectivo ? fondo + ventas : ventas;
  }

  /// Hace el corte con lo contado y cierra el turno (CJ-06, CJ-09). Un metodo que falta cuenta como cero.
  CorteDemo cerrarTurno(Map<MetodoPago, int> contado) {
    if (!turnoAbierto) {
      throw StateError('No hay turno abierto');
    }
    final esperadoPorMetodo = {for (final m in MetodoPago.values) m: esperado(m)};
    final contadoCompleto = {for (final m in MetodoPago.values) m: contado[m] ?? 0};
    final corte = CorteDemo(
      esperado: esperadoPorMetodo,
      contado: contadoCompleto,
      diferenciaEfectivo: contadoCompleto[MetodoPago.efectivo]! - esperadoPorMetodo[MetodoPago.efectivo]!,
    );
    _ultimoCorte = corte;
    _fondo = null;
    notifyListeners();
    return corte;
  }
}
