import 'package:flutter/material.dart';

import '../../estado/restaurante_demo.dart';
import '../../modelos/dinero.dart';
import '../../modelos/formato.dart';
import '../../modelos/permisos_restaurante.dart';
import '../../modelos/restaurantes.dart';
import '../../tema.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/componentes.dart';
import 'alcance.dart';

/// Panel del dueño: cómo va el día, qué atender y qué platillos dejan dinero. Las cifras las calcula el código a
/// partir de las ventas y las recetas; ninguna sale de un modelo de lenguaje.
class PanelRestaurante extends StatelessWidget {
  const PanelRestaurante({super.key});

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final disponibilidad = r.disponibilidad;
    final ancho = context.disposicion.esAncha;
    final cifras = [
      Cifra(
        etiqueta: 'Vendido hoy',
        valor: pesos(r.totalVendidoHoy),
        icono: Icons.payments_outlined,
        detalle: '${r.ventas.length} ventas cobradas',
      ),
      Cifra(
        etiqueta: 'Ticket promedio',
        valor: pesos(r.ticketPromedio),
        icono: Icons.receipt_long_outlined,
        tono: TonoInsignia.info,
      ),
      Cifra(
        etiqueta: 'Cuentas en servicio',
        valor: '${r.cuentasPorAtender.length}',
        icono: Icons.restaurant_menu,
        detalle: '${r.cuentasPorAtender.where((c) => c.estado == EstadoCuenta.porCobrar).length} por cobrar',
        tono: TonoInsignia.aviso,
      ),
      Cifra(
        etiqueta: 'Mesas libres',
        valor: '${disponibilidad.libres} de ${disponibilidad.total}',
        icono: Icons.table_restaurant_outlined,
        detalle: 'Se publica en la ficha del turista',
        tono: TonoInsignia.exito,
      ),
    ];

    final izquierda = [
      _TarjetaServicio(restaurante: r),
      const SizedBox(height: 20),
      const _Avisos(),
      const SizedBox(height: 20),
      const _MasVendidos(),
    ];
    final derecha = [
      const _Alertas(),
      if (r.puede(Permiso.costoVer)) ...[const SizedBox(height: 20), const _MargenPorPlatillo()],
    ];

    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 16),
      children: [
        ContenidoCentrado(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Hoy en ${r.nombre.replaceAll(' (ejemplo)', '')}', style: Theme.of(context).textTheme.headlineSmall),
              const SizedBox(height: 14),
              LayoutBuilder(
                builder: (context, restricciones) {
                  final columnas = restricciones.maxWidth >= 760 ? 4 : 2;
                  const separacion = 12.0;
                  final anchoCifra = (restricciones.maxWidth - separacion * (columnas - 1)) / columnas;
                  return Wrap(
                    spacing: separacion,
                    runSpacing: separacion,
                    children: [for (final c in cifras) SizedBox(width: anchoCifra, child: c)],
                  );
                },
              ),
              const SizedBox(height: 24),
              if (ancho)
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: izquierda)),
                    const SizedBox(width: 24),
                    Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: derecha)),
                  ],
                )
              else
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [...izquierda, const SizedBox(height: 20), ...derecha]),
            ],
          ),
        ),
      ],
    );
  }
}

class _TarjetaServicio extends StatelessWidget {
  const _TarjetaServicio({required this.restaurante});

  final RestauranteDemo restaurante;

  Future<void> _cambiar(BuildContext context, bool abrir) async {
    final r = restaurante;
    if (abrir) {
      intentar(context, r.abrirServicio);
      return;
    }
    final conCuentas = r.cuentas.any((c) => c.estado.noPagada);
    String? motivo;
    if (conCuentas) {
      motivo = await showDialog<String>(
        context: context,
        builder: (_) => const _DialogoMotivo(
          titulo: 'Cerrar con cuentas abiertas',
          mensaje: 'Hay cuentas sin cobrar. Cerrar el servicio exige el permiso de gerente y un motivo.',
        ),
      );
      if (motivo == null || !context.mounted) {
        return;
      }
    } else if (!await confirmarDialogo(context,
        titulo: 'Cerrar el servicio', mensaje: 'Ya no se abrirán cuentas ni pedidos por QR.', confirmar: 'Cerrar')) {
      return;
    }
    if (context.mounted) {
      intentar(context, () => r.cerrarServicio(motivo: motivo));
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final r = restaurante;
    final puede = r.puede(Permiso.servicioOperar);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(r.servicioAbierto ? Icons.lock_open : Icons.lock_outline,
                color: r.servicioAbierto ? context.colores.exito : context.colores.peligro),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(r.servicioAbierto ? 'Servicio abierto' : 'Servicio cerrado', style: tema.textTheme.titleMedium),
                  Text(
                    r.servicioAbierto
                        ? 'Se pueden abrir cuentas y recibir pedidos por QR.'
                        : 'No se abren cuentas ni se reciben pedidos por QR.',
                    style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            Switch(
              key: const Key('servicio'),
              value: r.servicioAbierto,
              onChanged: puede ? (valor) => _cambiar(context, valor) : null,
            ),
          ],
        ),
      ),
    );
  }
}

/// Pide un motivo corto de texto libre (hasta 80 caracteres).
class _DialogoMotivo extends StatefulWidget {
  const _DialogoMotivo({required this.titulo, required this.mensaje});

  final String titulo;
  final String mensaje;

  @override
  State<_DialogoMotivo> createState() => _DialogoMotivoState();
}

class _DialogoMotivoState extends State<_DialogoMotivo> {
  final _texto = TextEditingController();

  @override
  void dispose() {
    _texto.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.titulo),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(widget.mensaje),
          const SizedBox(height: 12),
          TextField(
            key: const Key('motivo-servicio'),
            controller: _texto,
            maxLength: 80,
            decoration: const InputDecoration(labelText: 'Motivo'),
          ),
        ],
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Cancelar')),
        FilledButton(
          onPressed: () => Navigator.of(context).pop(_texto.text.trim()),
          child: const Text('Cerrar servicio'),
        ),
      ],
    );
  }
}

class _Avisos extends StatelessWidget {
  const _Avisos();

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final pendientes = r.avisos.where((a) => !a.atendido).toList();
    return Seccion(
      titulo: 'Avisos para el salón',
      subtitulo: 'Pedidos por QR, cuentas solicitadas y comandas listas (QR-07, QR-13)',
      child: pendientes.isEmpty
          ? const Card(child: Padding(padding: EdgeInsets.all(16), child: Text('Sin avisos pendientes.')))
          : Column(
              children: [
                for (final aviso in pendientes.take(6))
                  Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: TarjetaConBarra(
                      color: context.colores.aviso,
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(aviso.texto),
                                const SizedBox(height: 2),
                                Text(horaTexto(aviso.hora), style: Theme.of(context).textTheme.bodySmall),
                              ],
                            ),
                          ),
                          TextButton(onPressed: () => r.atenderAviso(aviso), child: const Text('Atendido')),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
    );
  }
}

class _MasVendidos extends StatelessWidget {
  const _MasVendidos();

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final lista = r.platillosMasVendidos();
    final maximo = lista.isEmpty ? 1 : lista.first.cantidad;
    final tema = Theme.of(context);
    return Seccion(
      titulo: 'Lo más vendido hoy',
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: lista.isEmpty
              ? const Text('Todavía no hay ventas.')
              : Column(
                  children: [
                    for (final v in lista)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 6),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Expanded(child: Text(v.platillo.nombre, overflow: TextOverflow.ellipsis)),
                                Text('${v.cantidad} · ${pesos(v.importe)}', style: tema.textTheme.labelLarge),
                              ],
                            ),
                            const SizedBox(height: 6),
                            ClipRRect(
                              borderRadius: Formas.radioAfilado,
                              child: LinearProgressIndicator(
                                value: v.cantidad / maximo,
                                minHeight: 6,
                                backgroundColor: context.colores.neutroSuave,
                                color: tema.colorScheme.primary,
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
        ),
      ),
    );
  }
}

class _Alertas extends StatelessWidget {
  const _Alertas();

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final c = context.colores;
    final agotados = r.platillos.where((p) => p.agotado).toList();
    final bajos = r.articulos.where((a) => a.activo && a.bajoMinimo).toList();
    final fuera = r.platillos.where((p) => p.temporada != null && !p.temporada!.contiene(r.ahora)).toList();
    return Seccion(
      titulo: 'Para revisar',
      child: Column(
        children: [
          _Alerta(
            icono: Icons.block,
            color: c.peligro,
            titulo: '${agotados.length} ${agotados.length == 1 ? 'platillo agotado' : 'platillos agotados'}',
            detalle: agotados.isEmpty ? 'Todo disponible.' : agotados.map((p) => p.nombre).join(', '),
          ),
          if (r.puede(Permiso.inventarioVer)) ...[
            const SizedBox(height: 8),
            _Alerta(
              icono: Icons.inventory_2_outlined,
              color: c.aviso,
              titulo: '${bajos.length} ${bajos.length == 1 ? 'insumo bajo el mínimo' : 'insumos bajo el mínimo'}',
              detalle: bajos.isEmpty ? 'Existencias en orden.' : bajos.map((a) => a.nombre).join(', '),
            ),
            const SizedBox(height: 8),
            _Alerta(
              icono: Icons.sync_problem_outlined,
              color: c.peligro,
              titulo: '${r.consumosFallidos.length} ${r.consumosFallidos.length == 1 ? 'consumo sin descontar' : 'consumos sin descontar'}',
              detalle: r.consumosFallidos.isEmpty
                  ? 'Las ventas descontaron sus insumos.'
                  : 'La venta se cobró, pero Inventario no pudo descontar: ${r.consumosFallidos.first.detalle}',
            ),
          ],
          const SizedBox(height: 8),
          _Alerta(
            icono: Icons.event_busy_outlined,
            color: c.neutro,
            titulo: '${fuera.length} ${fuera.length == 1 ? 'platillo fuera de temporada' : 'platillos fuera de temporada'}',
            detalle: fuera.isEmpty ? 'Todos en temporada.' : fuera.map((p) => p.nombre).join(', '),
          ),
        ],
      ),
    );
  }
}

class _Alerta extends StatelessWidget {
  const _Alerta({required this.icono, required this.color, required this.titulo, required this.detalle});

  final IconData icono;
  final Color color;
  final String titulo;
  final String detalle;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return TarjetaConBarra(
      color: color,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icono, color: color, size: 22),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(titulo, style: tema.textTheme.titleSmall),
                const SizedBox(height: 2),
                Text(detalle, style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Costo teórico y margen de cada platillo con receta. Solo para quien tiene `restaurantes.costo.ver` (RC-10).
class _MargenPorPlatillo extends StatelessWidget {
  const _MargenPorPlatillo();

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final filas = <(Platillo, CostoReceta)>[
      for (final p in r.platillos)
        if (r.recetaVigente(p.id) case final receta?) (p, r.costoDe(receta, p.precio)),
    ]..sort((a, b) => (b.$2.margenPuntosBase ?? -100000).compareTo(a.$2.margenPuntosBase ?? -100000));
    return Seccion(
      titulo: 'Qué platillos dejan dinero',
      subtitulo: 'Costo teórico con el CPP de Inventario. Si falta un costo, no se estima.',
      child: Card(
        child: Column(
          children: [
            for (var i = 0; i < filas.length; i++) ...[
              if (i > 0) const Divider(),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(filas[i].$1.nombre, overflow: TextOverflow.ellipsis),
                          Text(
                            filas[i].$2.incompleto
                                ? 'Precio ${pesos(filas[i].$1.precio)}'
                                : 'Precio ${pesos(filas[i].$1.precio)} · costo ${pesos(filas[i].$2.costo!)}',
                            style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                          ),
                        ],
                      ),
                    ),
                    if (filas[i].$2.incompleto)
                      const Insignia('Costo incompleto', tono: TonoInsignia.aviso, icono: Icons.help_outline)
                    else
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(pesos(filas[i].$2.margen!), style: tema.textTheme.titleSmall),
                          Text(porcentajeTexto(filas[i].$2.margenPuntosBase!), style: tema.textTheme.bodySmall),
                        ],
                      ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
