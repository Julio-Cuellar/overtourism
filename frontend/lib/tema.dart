import 'package:flutter/material.dart';

/// Colores de Puebla: azul talavera como base y barro como acento. Material 3, claro y oscuro.
class Tema {
  Tema._();

  static const azulTalavera = Color(0xFF1F4E9C);
  static const barro = Color(0xFFB5542D);

  static ThemeData claro() => _construir(Brightness.light);

  static ThemeData oscuro() => _construir(Brightness.dark);

  static ThemeData _construir(Brightness brillo) {
    final colores = ColorScheme.fromSeed(seedColor: azulTalavera, secondary: barro, brightness: brillo);
    return ThemeData(
      colorScheme: colores,
      useMaterial3: true,
      appBarTheme: const AppBarTheme(centerTitle: false),
      cardTheme: const CardThemeData(margin: EdgeInsets.zero, clipBehavior: Clip.antiAlias),
      inputDecorationTheme: const InputDecorationTheme(border: OutlineInputBorder()),
    );
  }
}
