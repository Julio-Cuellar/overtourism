/// Dinero en la app: siempre centavos enteros (`int`), nunca `double` (D-20). El servidor calcula con decimales
/// exactos y manda cadenas como "207.00"; la app solo las convierte y las muestra.
library;

/// Escribe centavos como pesos mexicanos: `123456` → `$1,234.56`.
String pesos(int centavos) {
  final signo = centavos < 0 ? '-' : '';
  final absoluto = centavos.abs();
  final enteros = (absoluto ~/ 100).toString();
  final decimales = (absoluto % 100).toString().padLeft(2, '0');
  final conMiles = enteros.replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (_) => ',');
  return '$signo\$$conMiles.$decimales';
}

/// Convierte el decimal que manda el servidor (`"207.00"`, `"0.5"`) a centavos, sin pasar por `double`. El servidor
/// manda el dinero con 2 decimales; si llegaran mas, se ignoran.
int centavosDe(String decimal) {
  final limpio = decimal.trim();
  final negativo = limpio.startsWith('-');
  final partes = (negativo ? limpio.substring(1) : limpio).split('.');
  final enteros = int.parse(partes[0].isEmpty ? '0' : partes[0]);
  final fraccion = partes.length > 1 ? '${partes[1]}00'.substring(0, 2) : '00';
  final valor = enteros * 100 + int.parse(fraccion);
  return negativo ? -valor : valor;
}

/// Escribe centavos como el decimal exacto que espera el servidor: `24000` → `"240.00"`.
String decimalDe(int centavos) {
  final signo = centavos < 0 ? '-' : '';
  final absoluto = centavos.abs();
  return '$signo${absoluto ~/ 100}.${(absoluto % 100).toString().padLeft(2, '0')}';
}
