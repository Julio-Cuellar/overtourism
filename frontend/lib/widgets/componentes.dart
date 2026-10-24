import 'dart:math' as matematicas;

import 'package:flutter/material.dart';

import '../estado/restaurante_demo.dart';
import '../modelos/restaurantes.dart';
import '../tema.dart';

/// Significado del color de una insignia.
enum TonoInsignia { exito, aviso, peligro, info, neutro, primario }

/// Sello de estado: esquinas casi rectas (`Formas.afilado`) para distinguirlo de las tarjetas redondeadas.
class Insignia extends StatelessWidget {
  const Insignia(this.texto, {super.key, this.tono = TonoInsignia.neutro, this.icono, this.solida = false});

  final String texto;
  final TonoInsignia tono;
  final IconData? icono;

  /// Relleno pleno en vez de suave, para lo que exige atención inmediata.
  final bool solida;

  (Color, Color) _colores(BuildContext context) {
    final c = context.colores;
    final esquema = Theme.of(context).colorScheme;
    return switch (tono) {
      TonoInsignia.exito => (c.exito, c.exitoSuave),
      TonoInsignia.aviso => (c.aviso, c.avisoSuave),
      TonoInsignia.peligro => (c.peligro, c.peligroSuave),
      TonoInsignia.info => (c.info, c.infoSuave),
      TonoInsignia.neutro => (c.neutro, c.neutroSuave),
      TonoInsignia.primario => (esquema.primary, esquema.primaryContainer),
    };
  }

  @override
  Widget build(BuildContext context) {
    final (fuerte, suave) = _colores(context);
    final texto = solida ? Colors.white : fuerte;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: solida ? fuerte : suave,
        borderRadius: Formas.radioAfilado,
        border: solida ? null : Border.all(color: fuerte.withValues(alpha: 0.28)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icono != null) ...[Icon(icono, size: 13, color: texto), const SizedBox(width: 4)],
          Flexible(
            child: Text(
              this.texto,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context)
                  .textTheme
                  .labelSmall
                  ?.copyWith(color: texto, fontWeight: FontWeight.w700, letterSpacing: 0.2),
            ),
          ),
        ],
      ),
    );
  }
}

/// Tarjeta con una barra lateral recta del color del estado (esquinas redondeadas por fuera, filo por dentro).
class TarjetaConBarra extends StatelessWidget {
  const TarjetaConBarra({super.key, required this.color, required this.child, this.alTocar, this.relleno});

  final Color color;
  final Widget child;
  final VoidCallback? alTocar;
  final Color? relleno;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: relleno,
      child: InkWell(
        onTap: alTocar,
        child: IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(width: 4, color: color),
              Expanded(child: Padding(padding: const EdgeInsets.all(14), child: child)),
            ],
          ),
        ),
      ),
    );
  }
}

/// Título de sección con un filete fino y una acción opcional.
class Seccion extends StatelessWidget {
  const Seccion({super.key, required this.titulo, required this.child, this.accion, this.subtitulo});

  final String titulo;
  final String? subtitulo;
  final Widget child;
  final Widget? accion;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(titulo, style: tema.textTheme.titleMedium),
                  if (subtitulo != null)
                    Text(subtitulo!, style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
                ],
              ),
            ),
            ?accion,
          ],
        ),
        const SizedBox(height: 10),
        child,
      ],
    );
  }
}

/// Cifra grande con etiqueta, para el panel.
class Cifra extends StatelessWidget {
  const Cifra({super.key, required this.etiqueta, required this.valor, this.icono, this.detalle, this.tono = TonoInsignia.primario});

  final String etiqueta;
  final String valor;
  final String? detalle;
  final IconData? icono;
  final TonoInsignia tono;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final c = context.colores;
    final color = switch (tono) {
      TonoInsignia.exito => c.exito,
      TonoInsignia.aviso => c.aviso,
      TonoInsignia.peligro => c.peligro,
      TonoInsignia.info => c.info,
      TonoInsignia.neutro => c.neutro,
      TonoInsignia.primario => tema.colorScheme.primary,
    };
    return Card(
      child: Container(
        constraints: const BoxConstraints(minHeight: 104),
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                if (icono != null) ...[Icon(icono, size: 18, color: color), const SizedBox(width: 6)],
                Expanded(
                  child: Text(etiqueta,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: tema.textTheme.labelMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
                ),
              ],
            ),
            const SizedBox(height: 6),
            FittedBox(
              fit: BoxFit.scaleDown,
              alignment: Alignment.centerLeft,
              child: Text(valor, style: tema.textTheme.headlineSmall?.copyWith(color: color)),
            ),
            if (detalle != null) ...[
              const SizedBox(height: 2),
              Text(detalle!, style: tema.textTheme.bodySmall?.copyWith(color: tema.colorScheme.onSurfaceVariant)),
            ],
          ],
        ),
      ),
    );
  }
}

class EstadoVacio extends StatelessWidget {
  const EstadoVacio({super.key, required this.icono, required this.titulo, this.mensaje});

  final IconData icono;
  final String titulo;
  final String? mensaje;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icono, size: 44, color: tema.colorScheme.outline),
            const SizedBox(height: 12),
            Text(titulo, style: tema.textTheme.titleMedium, textAlign: TextAlign.center),
            if (mensaje != null) ...[
              const SizedBox(height: 4),
              Text(mensaje!,
                  style: tema.textTheme.bodyMedium?.copyWith(color: tema.colorScheme.onSurfaceVariant),
                  textAlign: TextAlign.center),
            ],
          ],
        ),
      ),
    );
  }
}

/// Etiquetas de dieta, rotuladas como declaradas por el negocio (MN-08).
class EtiquetasDieta extends StatelessWidget {
  const EtiquetasDieta(this.etiquetas, {super.key});

  final Set<EtiquetaDieta> etiquetas;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 6,
      runSpacing: 4,
      children: [
        for (final e in etiquetas)
          Insignia(
            e.etiqueta,
            tono: switch (e) {
              EtiquetaDieta.picante || EtiquetaDieta.contieneAlcohol => TonoInsignia.aviso,
              EtiquetaDieta.tipicoPoblano || EtiquetaDieta.deTemporada => TonoInsignia.info,
              _ => TonoInsignia.exito,
            },
          ),
      ],
    );
  }
}

/// Ilustración de un platillo: la maqueta no usa fotos reales, solo un tono de la paleta y un icono.
class FotoPlatillo extends StatelessWidget {
  const FotoPlatillo({super.key, required this.platillo, this.estacion = Estacion.cocina, this.lado = 64});

  final Platillo platillo;
  final Estacion estacion;
  final double lado;

  static const _tonos = [Paleta.azulTalavera, Paleta.naranja, Paleta.verde, Paleta.cafe, Paleta.rojo, Paleta.azulCielo];

  @override
  Widget build(BuildContext context) {
    final tono = _tonos[(platillo.foto ?? 0) % _tonos.length];
    final oscuro = Theme.of(context).brightness == Brightness.dark;
    final icono = estacion == Estacion.barra ? Icons.local_cafe_outlined : Icons.restaurant_outlined;
    return Container(
      width: lado,
      height: lado,
      decoration: BoxDecoration(
        borderRadius: Formas.radioControl,
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Color.alphaBlend(tono.withValues(alpha: oscuro ? 0.45 : 0.18), oscuro ? Paleta.azulNoche : Paleta.crema),
            Color.alphaBlend(tono.withValues(alpha: oscuro ? 0.75 : 0.34), oscuro ? Paleta.azulNoche : Paleta.crema),
          ],
        ),
      ),
      child: Icon(icono, color: oscuro ? Paleta.crema : tono, size: lado * 0.42),
    );
  }
}

/// Dibujo de un QR de ejemplo, derivado del token. No es un QR escaneable: el cliente real lo dibuja con una
/// biblioteca a partir de la URL que entrega el servidor (`qrDeMesa`, QR-21).
class QrDibujo extends StatelessWidget {
  const QrDibujo({super.key, required this.token, this.lado = 168});

  final String token;
  final double lado;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Código QR de ejemplo de la mesa',
      child: Container(
        width: lado,
        height: lado,
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: Formas.radioControl,
          border: Border.all(color: context.colores.borde),
        ),
        child: CustomPaint(painter: _PintorQr(token)),
      ),
    );
  }
}

class _PintorQr extends CustomPainter {
  _PintorQr(this.token);

  final String token;
  static const _celdas = 25;

  List<List<bool>> _matriz() {
    var semilla = 17;
    for (final unidad in token.codeUnits) {
      semilla = (semilla * 31 + unidad) & 0x7fffffff;
    }
    final azar = matematicas.Random(semilla);
    final m = List.generate(_celdas, (_) => List.generate(_celdas, (_) => azar.nextBool()));
    void buscador(int fila, int columna) {
      for (var f = -1; f <= 7; f++) {
        for (var c = -1; c <= 7; c++) {
          final ff = fila + f;
          final cc = columna + c;
          if (ff < 0 || cc < 0 || ff >= _celdas || cc >= _celdas) {
            continue;
          }
          final borde = f == 0 || f == 6 || c == 0 || c == 6;
          final centro = f >= 2 && f <= 4 && c >= 2 && c <= 4;
          m[ff][cc] = f >= 0 && f <= 6 && c >= 0 && c <= 6 && (borde || centro);
        }
      }
    }

    buscador(0, 0);
    buscador(0, _celdas - 7);
    buscador(_celdas - 7, 0);
    return m;
  }

  @override
  void paint(Canvas lienzo, Size tamano) {
    final lado = tamano.shortestSide / _celdas;
    final pincel = Paint()..color = Paleta.azulNoche;
    final m = _matriz();
    for (var f = 0; f < _celdas; f++) {
      for (var c = 0; c < _celdas; c++) {
        if (m[f][c]) {
          lienzo.drawRect(Rect.fromLTWH(c * lado, f * lado, lado + 0.4, lado + 0.4), pincel);
        }
      }
    }
  }

  @override
  bool shouldRepaint(_PintorQr anterior) => anterior.token != token;
}

/// Muestra el error de una regla con su texto en español; cualquier otro error se muestra genérico.
void mostrarError(BuildContext context, Object error) {
  final mensaje = error is ReglaRestaurante ? error.mensaje : 'No se pudo completar la operación.';
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(mensaje)));
}

void mostrarAviso(BuildContext context, String mensaje) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(mensaje)));
}

/// Ejecuta [accion] y muestra el error de la regla si falla. Devuelve verdadero si salió bien.
bool intentar(BuildContext context, void Function() accion) {
  try {
    accion();
    return true;
  } on ReglaRestaurante catch (error) {
    mostrarError(context, error);
    return false;
  }
}

Future<bool> confirmarDialogo(
  BuildContext context, {
  required String titulo,
  required String mensaje,
  String confirmar = 'Confirmar',
  String cancelar = 'Cancelar',
  bool destructivo = false,
}) async {
  final resultado = await showDialog<bool>(
    context: context,
    builder: (contexto) => AlertDialog(
      title: Text(titulo),
      content: Text(mensaje),
      actions: [
        TextButton(onPressed: () => Navigator.of(contexto).pop(false), child: Text(cancelar)),
        FilledButton(
          style: destructivo
              ? FilledButton.styleFrom(backgroundColor: Theme.of(contexto).colorScheme.error)
              : null,
          onPressed: () => Navigator.of(contexto).pop(true),
          child: Text(confirmar),
        ),
      ],
    ),
  );
  return resultado ?? false;
}
