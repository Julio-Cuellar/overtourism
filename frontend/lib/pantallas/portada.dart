import 'package:flutter/material.dart';

import '../estado/caja_demo.dart';
import '../estado/carrito.dart';
import '../estado/restaurante_demo.dart';
import '../tema.dart';
import '../widgets/adaptativo.dart';
import 'comensal/escaner_qr.dart';
import 'negocio/modo_negocio.dart';
import 'restaurantes/alcance.dart';
import 'restaurantes/modo_restaurante.dart';
import 'turista/modo_turista.dart';

/// Portada: la persona elige cómo entra. Una sola app, varios modos (D-89).
class Portada extends StatelessWidget {
  const Portada({super.key, required this.caja, required this.carrito, required this.restaurante});

  final CajaDemo caja;
  final Carrito carrito;
  final RestauranteDemo restaurante;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final opciones = [
      _OpcionModo(
        icono: Icons.explore_outlined,
        titulo: 'Soy turista',
        descripcion: 'Negocios cerca, restaurantes con mesas libres, cupones del Pasaporte y rutas sugeridas.',
        alTocar: () => Navigator.of(context)
            .push(MaterialPageRoute<void>(builder: (_) => ModoTurista(restaurante: restaurante))),
      ),
      _OpcionModo(
        icono: Icons.qr_code_scanner,
        titulo: 'Estoy en una mesa',
        descripcion: 'Escanea el QR de tu mesa, mira la carta y pide sin crear cuenta.',
        alTocar: () => abrirPantalla<void>(
          context,
          (_) => const EscanearQr(),
          restaurante: restaurante,
        ),
      ),
      _OpcionModo(
        icono: Icons.restaurant_menu,
        titulo: 'Mi restaurante',
        descripcion: 'Menú y recetas, salón y cuentas, cocina y barra, y qué platillos dejan dinero.',
        alTocar: () => Navigator.of(context)
            .push(MaterialPageRoute<void>(builder: (_) => ModoRestaurante(restaurante: restaurante))),
      ),
      _OpcionModo(
        icono: Icons.storefront_outlined,
        titulo: 'Tengo un negocio',
        descripcion: 'Cobra, acepta cupones y haz el corte de caja.',
        alTocar: () => Navigator.of(context)
            .push(MaterialPageRoute<void>(builder: (_) => ModoNegocio(caja: caja, carrito: carrito))),
      ),
    ];
    final ancha = context.disposicion.esAncha;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: BoxConstraints(maxWidth: ancha ? 880 : 480),
            child: ListView(
              padding: const EdgeInsets.all(24),
              shrinkWrap: true,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: tema.colorScheme.primary,
                        borderRadius: const BorderRadius.only(
                          topLeft: Radius.circular(Formas.tarjeta),
                          topRight: Radius.circular(Formas.tarjeta),
                          bottomLeft: Radius.circular(Formas.tarjeta),
                          bottomRight: Radius.circular(Formas.afilado),
                        ),
                      ),
                      child: Icon(Icons.travel_explore, size: 32, color: tema.colorScheme.onPrimary),
                    ),
                    const SizedBox(width: 14),
                    Text('Negocio 360', style: tema.textTheme.headlineMedium),
                  ],
                ),
                const SizedBox(height: 16),
                Text('Descubre Puebla más allá del centro y apoya a los negocios locales.', style: tema.textTheme.bodyLarge),
                const SizedBox(height: 28),
                if (ancha)
                  LayoutBuilder(
                    builder: (context, restricciones) => Wrap(
                      spacing: 14,
                      runSpacing: 14,
                      children: [for (final o in opciones) SizedBox(width: (restricciones.maxWidth - 14) / 2, child: o)],
                    ),
                  )
                else
                  for (final o in opciones) Padding(padding: const EdgeInsets.only(bottom: 12), child: o),
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
    return Card(
      child: InkWell(
        onTap: alTocar,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(color: tema.colorScheme.primaryContainer, borderRadius: Formas.radioControl),
                child: Icon(icono, color: tema.colorScheme.onPrimaryContainer),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(titulo, style: tema.textTheme.titleMedium),
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
