/// Formatos de lectura de la maqueta de Restaurantes. Todo viene de enteros: nunca se usa `double` para dinero ni
/// para cantidades de inventario.
library;

/// Cantidad de Inventario (diezmilésimas de la unidad base) como texto: `1260000` → `126 g`, `1625` → `0.1625 g`.
String cantidadTexto(int diezmilesimas, String unidad) {
  final signo = diezmilesimas < 0 ? '-' : '';
  final absoluto = diezmilesimas.abs();
  final enteros = (absoluto ~/ 10000).toString();
  final conMiles = enteros.replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (_) => ',');
  final fraccion = (absoluto % 10000).toString().padLeft(4, '0').replaceFirst(RegExp(r'0+$'), '');
  return '$signo$conMiles${fraccion.isEmpty ? '' : '.$fraccion'} $unidad';
}

/// Cantidad entera en unidad base: `120` → `120 g`.
String baseTexto(int cantidad, String unidad) => '$cantidad $unidad';

/// Puntos base como porcentaje: `4912` → `49.12 %`.
String porcentajeTexto(int puntosBase) {
  final signo = puntosBase < 0 ? '-' : '';
  final absoluto = puntosBase.abs();
  return '$signo${absoluto ~/ 100}.${(absoluto % 100).toString().padLeft(2, '0')} %';
}

String horaTexto(DateTime fecha) =>
    '${fecha.hour.toString().padLeft(2, '0')}:${fecha.minute.toString().padLeft(2, '0')}';

/// Tiempo transcurrido en minutos: `7` → `7 min`, `75` → `1 h 15 min`.
String minutosTexto(int minutos) {
  if (minutos < 60) {
    return '$minutos min';
  }
  final resto = minutos % 60;
  return resto == 0 ? '${minutos ~/ 60} h' : '${minutos ~/ 60} h $resto min';
}

/// Importe de una cantidad de pesos a partir de un texto del usuario (`"85"`, `"85.5"`, `"1,250.00"`) en centavos;
/// nulo si no es un monto válido de hasta 2 decimales.
int? centavosDeTexto(String texto) {
  final limpio = texto.trim().replaceAll(',', '');
  if (!RegExp(r'^\d{1,7}(\.\d{1,2})?$').hasMatch(limpio)) {
    return null;
  }
  final partes = limpio.split('.');
  final decimales = partes.length > 1 ? partes[1].padRight(2, '0') : '00';
  return int.parse(partes[0]) * 100 + int.parse(decimales);
}
