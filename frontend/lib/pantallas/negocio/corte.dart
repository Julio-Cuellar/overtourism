import 'package:flutter/material.dart';

import '../../estado/caja_demo.dart';
import '../../modelos/dinero.dart';
import '../../modelos/metodo_pago.dart';

/// Corte de caja: se ve lo esperado por metodo, se captura lo contado y se confirma (CJ-06, CJ-12). Al confirmar,
/// el turno se cierra.
class Corte extends StatefulWidget {
  const Corte({super.key, required this.caja});

  final CajaDemo caja;

  @override
  State<Corte> createState() => _CorteState();
}

class _CorteState extends State<Corte> {
  final _contado = {for (final m in MetodoPago.values) m: TextEditingController()};
  String? _error;

  @override
  void dispose() {
    for (final controlador in _contado.values) {
      controlador.dispose();
    }
    super.dispose();
  }

  Future<void> _confirmar() async {
    final contado = <MetodoPago, int>{};
    for (final entrada in _contado.entries) {
      final texto = entrada.value.text.trim().isEmpty ? '0' : entrada.value.text.trim();
      if (!RegExp(r'^\d{1,7}(\.\d{1,2})?$').hasMatch(texto)) {
        setState(() => _error = 'Revisa el conteo: montos de cero o más, con hasta 2 decimales.');
        return;
      }
      contado[entrada.key] = centavosDe(texto);
    }
    final seguro = await showDialog<bool>(
      context: context,
      builder: (contexto) => AlertDialog(
        title: const Text('¿Confirmar el corte?'),
        content: const Text('El turno se cerrará y ya no se podrá vender en él.'),
        actions: [
          TextButton(onPressed: () => Navigator.of(contexto).pop(false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.of(contexto).pop(true), child: const Text('Confirmar corte')),
        ],
      ),
    );
    if (seguro == true) {
      widget.caja.cerrarTurno(contado);
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return ListenableBuilder(
      listenable: widget.caja,
      builder: (context, _) => ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('Cuenta lo que hay en la caja', style: tema.textTheme.titleMedium),
          const SizedBox(height: 4),
          Text('Fondo inicial: ${pesos(widget.caja.fondo)}', style: tema.textTheme.bodyMedium),
          const SizedBox(height: 16),
          for (final metodo in MetodoPago.values) ...[
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(metodo.etiqueta, style: tema.textTheme.titleSmall),
                      Text('Esperado: ${pesos(widget.caja.esperado(metodo))}', style: tema.textTheme.bodySmall),
                    ],
                  ),
                ),
                SizedBox(
                  width: 150,
                  child: TextField(
                    controller: _contado[metodo],
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: const InputDecoration(labelText: 'Contado', prefixText: r'$ ', isDense: true),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
          ],
          if (_error != null) Text(_error!, style: TextStyle(color: tema.colorScheme.error)),
          const SizedBox(height: 8),
          FilledButton.icon(onPressed: _confirmar, icon: const Icon(Icons.lock_outline), label: const Text('Hacer corte')),
        ],
      ),
    );
  }
}
