import 'package:flutter/material.dart';

import '../estado/caja_demo.dart';
import '../estado/carrito.dart';
import 'negocio/modo_negocio.dart';
import 'turista/modo_turista.dart';

/// Portada: la persona elige si viene como turista o como negocio.
class Portada extends StatelessWidget {
  const Portada({super.key, required this.caja, required this.carrito});

  final CajaDemo caja;
  final Carrito carrito;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 480),
            child: ListView(
              padding: const EdgeInsets.all(24),
              shrinkWrap: true,
              children: [
                Icon(Icons.travel_explore, size: 56, color: tema.colorScheme.primary),
                const SizedBox(height: 16),
                Text('Negocio 360', style: tema.textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w700)),
                const SizedBox(height: 8),
                Text('Descubre Puebla más allá del centro y apoya a los negocios locales.',
                    style: tema.textTheme.bodyLarge),
                const SizedBox(height: 32),
                _OpcionModo(
                  icono: Icons.explore_outlined,
                  titulo: 'Soy turista',
                  descripcion: 'Negocios cerca, cupones del Pasaporte y rutas sugeridas.',
                  alTocar: () => Navigator.of(context)
                      .push(MaterialPageRoute<void>(builder: (_) => const ModoTurista())),
                ),
                const SizedBox(height: 12),
                _OpcionModo(
                  icono: Icons.storefront_outlined,
                  titulo: 'Tengo un negocio',
                  descripcion: 'Cobra, acepta cupones y haz el corte de caja.',
                  alTocar: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(builder: (_) => ModoNegocio(caja: caja, carrito: carrito))),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _OpcionModo extends StatelessWidget {
  const _OpcionModo({required this.icono, required this.titulo, required this.descripcion, required this.alTocar});

  final IconData icono;
  final String titulo;
  final String descripcion;
  final VoidCallback alTocar;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Card.outlined(
      child: InkWell(
        onTap: alTocar,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              CircleAvatar(
                radius: 26,
                backgroundColor: tema.colorScheme.primaryContainer,
                child: Icon(icono, color: tema.colorScheme.onPrimaryContainer),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(titulo, style: tema.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
                    const SizedBox(height: 4),
                    Text(descripcion, style: tema.textTheme.bodyMedium),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right),
            ],
          ),
        ),
      ),
    );
  }
}
