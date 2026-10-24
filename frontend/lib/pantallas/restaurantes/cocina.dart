import 'package:flutter/material.dart';

import '../../estado/restaurante_demo.dart';
import '../../modelos/formato.dart';
import '../../modelos/permisos_restaurante.dart';
import '../../modelos/restaurantes.dart';
import '../../tema.dart';
import '../../widgets/adaptativo.dart';
import '../../widgets/componentes.dart';
import 'alcance.dart';

/// Tablero de cocina y barra: una comanda por estación y por envío, de la más antigua a la más nueva (CD-01 a CD-09).
/// El tiempo de preparación lo calcula el código y se resalta pasado el umbral (CD-04).
class Cocina extends StatefulWidget {
  const Cocina({super.key});

  @override
  State<Cocina> createState() => _CocinaState();
}

class _CocinaState extends State<Cocina> {
  Estacion? _estacion;
  EstadoComanda _columna = EstadoComanda.nueva;

  static const _columnas = [EstadoComanda.nueva, EstadoComanda.enPreparacion, EstadoComanda.lista];

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final comandas = r.comandasDe(_estacion);
    List<Comanda> de(EstadoComanda e) => comandas.where((c) => c.estado == e).toList();
    final ancha = context.disposicion.esAncha;
    final tema = Theme.of(context);
    final filtro = Padding(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 8),
      child: Row(
        children: [
          Expanded(
            child: SegmentedButton<Estacion?>(
              showSelectedIcon: false,
              segments: const [
                ButtonSegment(value: null, label: Text('Todas'), icon: Icon(Icons.all_inclusive, size: 18)),
                ButtonSegment(value: Estacion.cocina, label: Text('Cocina'), icon: Icon(Icons.soup_kitchen_outlined, size: 18)),
                ButtonSegment(value: Estacion.barra, label: Text('Barra'), icon: Icon(Icons.local_bar_outlined, size: 18)),
              ],
              selected: {_estacion},
              onSelectionChanged: (s) => setState(() => _estacion = s.first),
            ),
          ),
          if (ancha) ...[
            const SizedBox(width: 16),
            Text('Resaltado desde ${RestauranteDemo.umbralPreparacionMin} min',
                style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
          ],
        ],
      ),
    );

    if (ancha) {
      return Column(
        children: [
          filtro,
          Expanded(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 16),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  for (var i = 0; i < _columnas.length; i++) ...[
                    if (i > 0) const SizedBox(width: 12),
                    Expanded(child: _Columna(estado: _columnas[i], comandas: de(_columnas[i]))),
                  ],
                ],
              ),
            ),
          ),
        ],
      );
    }
    return Column(
      children: [
        filtro,
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: SegmentedButton<EstadoComanda>(
            showSelectedIcon: false,
            segments: [
              for (final e in _columnas)
                ButtonSegment(value: e, label: Text('${e == EstadoComanda.enPreparacion ? 'Preparando' : e.etiqueta} (${de(e).length})')),
            ],
            selected: {_columna},
            onSelectionChanged: (s) => setState(() => _columna = s.first),
          ),
        ),
        const SizedBox(height: 8),
        Expanded(child: _Columna(estado: _columna, comandas: de(_columna), conTitulo: false)),
      ],
    );
  }
}

class _Columna extends StatelessWidget {
  const _Columna({required this.estado, required this.comandas, this.conTitulo = true});

  final EstadoComanda estado;
  final List<Comanda> comandas;
  final bool conTitulo;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final c = context.colores;
    final color = switch (estado) {
      EstadoComanda.nueva => c.info,
      EstadoComanda.enPreparacion => c.aviso,
      _ => c.exito,
    };
    return Container(
      decoration: conTitulo
          ? BoxDecoration(
              color: tema.colorScheme.surfaceContainer,
              borderRadius: Formas.radioTarjeta,
              border: Border.all(color: c.borde),
            )
          : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (conTitulo)
            Padding(
              padding: const EdgeInsets.fromLTRB(14, 12, 14, 8),
              child: Row(
                children: [
                  Container(width: 10, height: 10, decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(2))),
                  const SizedBox(width: 8),
                  Expanded(child: Text(estado.etiqueta, style: tema.textTheme.titleSmall, overflow: TextOverflow.ellipsis)),
                  Insignia('${comandas.length}', tono: TonoInsignia.neutro),
                ],
              ),
            ),
          Expanded(
            child: comandas.isEmpty
                ? EstadoVacio(icono: Icons.check_circle_outline, titulo: 'Nada por aquí', mensaje: 'Sin comandas en ${estado.etiqueta.toLowerCase()}.')
                : ListView(
                    padding: const EdgeInsets.fromLTRB(12, 4, 12, 16),
                    children: [
                      for (final comanda in comandas)
                        Padding(padding: const EdgeInsets.only(bottom: 10), child: _TarjetaComanda(comanda: comanda)),
                    ],
                  ),
          ),
        ],
      ),
    );
  }
}

class _TarjetaComanda extends StatelessWidget {
  const _TarjetaComanda({required this.comanda});

  final Comanda comanda;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final tema = Theme.of(context);
    final c = context.colores;
    final cuenta = r.cuenta(comanda.cuentaId);
    final mesa = cuenta?.mesaId == null ? null : r.mesa(cuenta!.mesaId!);
    final lineas = r.lineasDe(comanda);
    final lenta = r.esLenta(comanda);
    final minutos = r.minutosDe(comanda);
    final hermanas = r.comandas.where((o) => o.envioId == comanda.envioId && o.id != comanda.id).toList();
    final color = lenta
        ? c.peligro
        : switch (comanda.estado) {
            EstadoComanda.nueva => c.info,
            EstadoComanda.enPreparacion => c.aviso,
            _ => c.exito,
          };
    final tieneAlergia = lineas.any((l) => l.marcaAlergia && l.estado != EstadoLinea.anulada);
    return TarjetaConBarra(
      color: color,
      relleno: lenta ? c.peligroSuave : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(mesa == null ? 'Para llevar' : 'Mesa ${mesa.etiqueta}', style: tema.textTheme.titleMedium),
              ),
              Insignia(comanda.estacion.etiqueta, tono: comanda.estacion == Estacion.barra ? TonoInsignia.info : TonoInsignia.primario),
            ],
          ),
          const SizedBox(height: 2),
          Row(
            children: [
              Expanded(
                child: Text('Comanda ${comanda.numero} · cuenta ${cuenta?.numero ?? ''}',
                    overflow: TextOverflow.ellipsis,
                    style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
              ),
              Icon(Icons.timer_outlined, size: 16, color: lenta ? c.peligro : tema.colorScheme.onSurfaceVariant),
              const SizedBox(width: 4),
              Text(
                minutosTexto(minutos),
                key: Key('tiempo-${comanda.numero}'),
                style: tema.textTheme.labelLarge?.copyWith(color: lenta ? c.peligro : null),
              ),
            ],
          ),
          if (lenta)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Insignia('Pasó de ${RestauranteDemo.umbralPreparacionMin} min', tono: TonoInsignia.peligro, icono: Icons.schedule, solida: true),
            ),
          if (tieneAlergia)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Insignia('ALERGIA: revisar antes de preparar', tono: TonoInsignia.peligro, icono: Icons.warning_amber_rounded, solida: true),
            ),
          const Divider(height: 20),
          for (final l in lineas)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 3),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox(
                    width: 32,
                    child: Text('${l.cantidad}×',
                        style: tema.textTheme.titleSmall?.copyWith(decoration: l.estado == EstadoLinea.anulada ? TextDecoration.lineThrough : null)),
                  ),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          l.nombre,
                          style: tema.textTheme.bodyLarge?.copyWith(
                            decoration: l.estado == EstadoLinea.anulada ? TextDecoration.lineThrough : null,
                            color: l.estado == EstadoLinea.anulada ? c.peligro : null,
                          ),
                        ),
                        if (l.nota != null)
                          Text(l.nota!, style: tema.textTheme.bodySmall?.copyWith(color: l.marcaAlergia ? c.peligro : tema.colorScheme.onSurfaceVariant, fontWeight: l.marcaAlergia ? FontWeight.w700 : null)),
                        if (l.estado == EstadoLinea.anulada)
                          Text('Anulada: ${l.motivo?.etiqueta.toLowerCase() ?? ''}', style: tema.textTheme.bodySmall?.copyWith(color: c.peligro)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          if (hermanas.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(
              [for (final h in hermanas) '${h.estacion.etiqueta}: ${h.estado.etiqueta.toLowerCase()}'].join(' · '),
              style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
            ),
          ],
          const SizedBox(height: 10),
          _Acciones(comanda: comanda),
        ],
      ),
    );
  }
}

class _Acciones extends StatelessWidget {
  const _Acciones({required this.comanda});

  final Comanda comanda;

  @override
  Widget build(BuildContext context) {
    final r = context.restaurante;
    final puedePreparar = r.puede(Permiso.comandaPreparar);
    final puedeEntregar = r.puede(Permiso.comandaEntregar);
    final etiqueta = '${comanda.estacion.etiqueta} ${comanda.numero}';
    switch (comanda.estado) {
      case EstadoComanda.nueva:
        return _Boton(
          clave: 'empezar-${comanda.numero}',
          texto: 'Empezar a preparar',
          icono: Icons.play_arrow,
          alTocar: puedePreparar ? () => intentar(context, () => r.avanzarComanda(comanda.id)) : null,
          ayuda: puedePreparar ? null : 'Solo cocina y barra preparan',
        );
      case EstadoComanda.enPreparacion:
        return _Boton(
          clave: 'lista-${comanda.numero}',
          texto: 'Marcar lista',
          icono: Icons.check,
          alTocar: puedePreparar ? () => intentar(context, () => r.avanzarComanda(comanda.id)) : null,
          ayuda: puedePreparar ? null : 'Solo cocina y barra marcan lista',
        );
      case EstadoComanda.lista:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _Boton(
              clave: 'entregar-${comanda.numero}',
              texto: 'Entregar',
              icono: Icons.room_service_outlined,
              alTocar: puedeEntregar
                  ? () {
                      if (intentar(context, () => r.entregarComanda(comanda.id))) {
                        mostrarAviso(context, '$etiqueta entregada.');
                      }
                    }
                  : null,
              ayuda: puedeEntregar ? null : 'Lo entrega el mesero',
            ),
            if (puedePreparar)
              TextButton(
                key: Key('devolver-${comanda.numero}'),
                onPressed: () => intentar(context, () => r.devolverComanda(comanda.id)),
                child: const Text('Devolver a preparación'),
              ),
          ],
        );
      case EstadoComanda.entregada:
        return const SizedBox.shrink();
    }
  }
}

class _Boton extends StatelessWidget {
  const _Boton({required this.clave, required this.texto, required this.icono, required this.alTocar, this.ayuda});

  final String clave;
  final String texto;
  final IconData icono;
  final VoidCallback? alTocar;
  final String? ayuda;

  @override
  Widget build(BuildContext context) {
    final boton = FilledButton.icon(
      key: Key(clave),
      onPressed: alTocar,
      icon: Icon(icono),
      label: Text(texto, maxLines: 1, overflow: TextOverflow.ellipsis),
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        boton,
        if (ayuda != null)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(ayuda!, textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodySmall),
          ),
      ],
    );
  }
}
