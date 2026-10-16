import 'package:flutter/material.dart';

import '../../estado/caja_demo.dart';
import '../../estado/carrito.dart';
import '../../modelos/dinero.dart';
import '../../widgets/etiqueta_demo.dart';
import 'cobrar.dart';
import 'corte.dart';
import 'ventas_turno.dart';

/// Modo negocio: sin turno abierto pide abrirlo (CJ-01); con turno, cobrar, ver ventas y hacer el corte.
class ModoNegocio extends StatefulWidget {
  const ModoNegocio({super.key, required this.caja, required this.carrito});

  final CajaDemo caja;
  final Carrito carrito;

  @override
  State<ModoNegocio> createState() => _ModoNegocioState();
}

class _ModoNegocioState extends State<ModoNegocio> {
  int _seccion = 0;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: widget.caja,
      builder: (context, _) {
        if (!widget.caja.turnoAbierto) {
          return _AbrirTurno(caja: widget.caja);
        }
        return Scaffold(
          appBar: AppBar(title: const Text('Caja · Mostrador')),
          body: IndexedStack(
            index: _seccion,
            children: [
              Cobrar(caja: widget.caja, carrito: widget.carrito),
              VentasTurno(caja: widget.caja),
              Corte(caja: widget.caja),
            ],
          ),
          bottomNavigationBar: NavigationBar(
            selectedIndex: _seccion,
            onDestinationSelected: (indice) => setState(() => _seccion = indice),
            destinations: const [
              NavigationDestination(icon: Icon(Icons.point_of_sale_outlined), selectedIcon: Icon(Icons.point_of_sale),
                  label: 'Cobrar'),
              NavigationDestination(icon: Icon(Icons.receipt_long_outlined), selectedIcon: Icon(Icons.receipt_long),
                  label: 'Ventas'),
              NavigationDestination(icon: Icon(Icons.calculate_outlined), selectedIcon: Icon(Icons.calculate),
                  label: 'Corte'),
            ],
          ),
        );
      },
    );
  }
}

/// Abrir turno con un fondo en efectivo (CJ-02). Si ya hubo un corte, muestra su diferencia.
class _AbrirTurno extends StatefulWidget {
  const _AbrirTurno({required this.caja});

  final CajaDemo caja;

  @override
  State<_AbrirTurno> createState() => _AbrirTurnoState();
}

class _AbrirTurnoState extends State<_AbrirTurno> {
  final _fondo = TextEditingController();
  String? _error;

  @override
  void dispose() {
    _fondo.dispose();
    super.dispose();
  }

  void _abrir() {
    final texto = _fondo.text.trim().isEmpty ? '0' : _fondo.text.trim();
    if (!RegExp(r'^\d{1,7}(\.\d{1,2})?$').hasMatch(texto)) {
      setState(() => _error = 'El fondo debe ser cero o más, con hasta 2 decimales.');
      return;
    }
    widget.caja.abrirTurno(centavosDe(texto));
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final corte = widget.caja.ultimoCorte;
    return Scaffold(
      appBar: AppBar(title: const Text('Caja')),
      body: ListView(
        children: [
          const EtiquetaDemo(detalle: 'caja simulada en el teléfono'),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (corte != null) ...[
                  Card.filled(
                    child: ListTile(
                      leading: const Icon(Icons.check_circle_outline),
                      title: const Text('Corte confirmado. El turno quedó cerrado.'),
                      subtitle: Text('Diferencia en efectivo: ${pesos(corte.diferenciaEfectivo)}'),
                    ),
                  ),
                  const SizedBox(height: 24),
                ],
                Text('Para vender, abre un turno con el efectivo que hay en la caja.',
                    style: tema.textTheme.bodyLarge),
                const SizedBox(height: 16),
                TextField(
                  key: const Key('fondo'),
                  controller: _fondo,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: InputDecoration(labelText: 'Fondo inicial', prefixText: r'$ ', errorText: _error),
                ),
                const SizedBox(height: 16),
                FilledButton.icon(onPressed: _abrir, icon: const Icon(Icons.lock_open), label: const Text('Abrir turno')),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
