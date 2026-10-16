import 'package:flutter/material.dart';

/// Paleta de Negocio 360 (maqueta de Restaurantes, 2026-10-16). Nueve colores base; los tonos suaves de fondos y
/// bordes se obtienen mezclando esos mismos colores con blanco o crema, no se agregan colores nuevos.
class Paleta {
  Paleta._();

  static const blanco = Color(0xFFFFFFFF);
  static const azulTalavera = Color(0xFF002F6C);
  static const azulCielo = Color(0xFF4A90E2);
  static const azulNoche = Color(0xFF0A192F);
  static const crema = Color(0xFFFDFBF7);
  static const cafe = Color(0xFF3D2314);
  static const naranja = Color(0xFFE65F00);
  static const verde = Color(0xFF1A4314);
  static const rojo = Color(0xFF8B2520);

  /// Mezcla [color] sobre [base] con [opacidad]; da los tonos suaves sin salirse de la paleta.
  static Color suave(Color color, {Color base = crema, double opacidad = 0.1}) =>
      Color.alphaBlend(color.withValues(alpha: opacidad), base);
}

/// Radios de las esquinas: redondeado suave para lo que se toca y contiene, afilado para lo que etiqueta.
class Formas {
  Formas._();

  /// Tarjetas y paneles.
  static const double tarjeta = 14;

  /// Botones, campos y hojas.
  static const double control = 10;

  /// Etiquetas, insignias y marcas de estado: casi rectas, para que se lean como sellos.
  static const double afilado = 3;

  static BorderRadius get radioTarjeta => BorderRadius.circular(tarjeta);

  static BorderRadius get radioControl => BorderRadius.circular(control);

  static BorderRadius get radioAfilado => BorderRadius.circular(afilado);
}

/// Colores con significado fijo en toda la app, además de los del `ColorScheme`.
@immutable
class ColoresSemanticos extends ThemeExtension<ColoresSemanticos> {
  const ColoresSemanticos({
    required this.exito,
    required this.exitoSuave,
    required this.aviso,
    required this.avisoSuave,
    required this.peligro,
    required this.peligroSuave,
    required this.info,
    required this.infoSuave,
    required this.neutro,
    required this.neutroSuave,
    required this.borde,
  });

  /// Libre, listo, disponible (verde).
  final Color exito;
  final Color exitoSuave;

  /// Ocupado, en preparación, atención (naranja).
  final Color aviso;
  final Color avisoSuave;

  /// Agotado, cancelado, error (rojo).
  final Color peligro;
  final Color peligroSuave;

  /// Información y acciones secundarias (azul cielo).
  final Color info;
  final Color infoSuave;

  /// Bloqueado, inactivo, sin dato (café apagado).
  final Color neutro;
  final Color neutroSuave;

  /// Línea fina de tarjetas y separadores.
  final Color borde;

  static const claro = ColoresSemanticos(
    exito: Paleta.verde,
    exitoSuave: Color(0xFFE9EEE6),
    aviso: Paleta.naranja,
    avisoSuave: Color(0xFFFCEBDD),
    peligro: Paleta.rojo,
    peligroSuave: Color(0xFFF3E3E2),
    info: Paleta.azulCielo,
    infoSuave: Color(0xFFE6EFFB),
    neutro: Color(0xFF7A6558),
    neutroSuave: Color(0xFFF1ECE4),
    borde: Color(0xFFE6E0D5),
  );

  static const oscuro = ColoresSemanticos(
    exito: Color(0xFF7BB36F),
    exitoSuave: Color(0xFF173018),
    aviso: Color(0xFFFF8A3D),
    avisoSuave: Color(0xFF3A2312),
    peligro: Color(0xFFE5837D),
    peligroSuave: Color(0xFF3B1C1A),
    info: Paleta.azulCielo,
    infoSuave: Color(0xFF12294A),
    neutro: Color(0xFFB8A89B),
    neutroSuave: Color(0xFF1B2A42),
    borde: Color(0xFF243752),
  );

  @override
  ColoresSemanticos copyWith({
    Color? exito,
    Color? exitoSuave,
    Color? aviso,
    Color? avisoSuave,
    Color? peligro,
    Color? peligroSuave,
    Color? info,
    Color? infoSuave,
    Color? neutro,
    Color? neutroSuave,
    Color? borde,
  }) =>
      ColoresSemanticos(
        exito: exito ?? this.exito,
        exitoSuave: exitoSuave ?? this.exitoSuave,
        aviso: aviso ?? this.aviso,
        avisoSuave: avisoSuave ?? this.avisoSuave,
        peligro: peligro ?? this.peligro,
        peligroSuave: peligroSuave ?? this.peligroSuave,
        info: info ?? this.info,
        infoSuave: infoSuave ?? this.infoSuave,
        neutro: neutro ?? this.neutro,
        neutroSuave: neutroSuave ?? this.neutroSuave,
        borde: borde ?? this.borde,
      );

  @override
  ColoresSemanticos lerp(ThemeExtension<ColoresSemanticos>? other, double t) {
    if (other is! ColoresSemanticos) {
      return this;
    }
    return ColoresSemanticos(
      exito: Color.lerp(exito, other.exito, t)!,
      exitoSuave: Color.lerp(exitoSuave, other.exitoSuave, t)!,
      aviso: Color.lerp(aviso, other.aviso, t)!,
      avisoSuave: Color.lerp(avisoSuave, other.avisoSuave, t)!,
      peligro: Color.lerp(peligro, other.peligro, t)!,
      peligroSuave: Color.lerp(peligroSuave, other.peligroSuave, t)!,
      info: Color.lerp(info, other.info, t)!,
      infoSuave: Color.lerp(infoSuave, other.infoSuave, t)!,
      neutro: Color.lerp(neutro, other.neutro, t)!,
      neutroSuave: Color.lerp(neutroSuave, other.neutroSuave, t)!,
      borde: Color.lerp(borde, other.borde, t)!,
    );
  }
}

extension TemaContexto on BuildContext {
  /// Colores semánticos del tema vigente.
  ColoresSemanticos get colores => Theme.of(this).extension<ColoresSemanticos>()!;
}

/// Tema de Negocio 360: Material 3, claro (crema y azul talavera) y oscuro (azul noche).
class Tema {
  Tema._();

  static const azulTalavera = Paleta.azulTalavera;
  static const barro = Paleta.naranja;

  static ThemeData claro() => _construir(Brightness.light);

  static ThemeData oscuro() => _construir(Brightness.dark);

  static ThemeData _construir(Brightness brillo) {
    final claro = brillo == Brightness.light;
    final semanticos = claro ? ColoresSemanticos.claro : ColoresSemanticos.oscuro;
    final fondo = claro ? Paleta.crema : Paleta.azulNoche;
    final superficie = claro ? Paleta.blanco : const Color(0xFF10233D);
    final texto = claro ? Paleta.azulNoche : Paleta.crema;
    final primario = claro ? Paleta.azulTalavera : Paleta.azulCielo;

    final colores = ColorScheme(
      brightness: brillo,
      primary: primario,
      onPrimary: claro ? Paleta.blanco : Paleta.azulNoche,
      primaryContainer: claro ? const Color(0xFFDDE7F5) : const Color(0xFF12294A),
      onPrimaryContainer: claro ? Paleta.azulTalavera : Paleta.crema,
      secondary: claro ? Paleta.cafe : const Color(0xFFD9C3B4),
      onSecondary: claro ? Paleta.blanco : Paleta.azulNoche,
      secondaryContainer: claro ? const Color(0xFFF1E7DD) : const Color(0xFF3A2A20),
      onSecondaryContainer: claro ? Paleta.cafe : Paleta.crema,
      tertiary: semanticos.aviso,
      onTertiary: Paleta.blanco,
      tertiaryContainer: semanticos.avisoSuave,
      onTertiaryContainer: claro ? const Color(0xFF7A3200) : const Color(0xFFFFD2B0),
      error: semanticos.peligro,
      onError: claro ? Paleta.blanco : Paleta.azulNoche,
      errorContainer: semanticos.peligroSuave,
      onErrorContainer: claro ? Paleta.rojo : const Color(0xFFFFD9D6),
      surface: superficie,
      onSurface: texto,
      onSurfaceVariant: claro ? const Color(0xFF5B4A3F) : const Color(0xFFB9C4D6),
      surfaceContainerLowest: superficie,
      surfaceContainerLow: fondo,
      surfaceContainer: claro ? const Color(0xFFF8F4EC) : const Color(0xFF0F2038),
      surfaceContainerHigh: claro ? const Color(0xFFF3EEE4) : const Color(0xFF16294A),
      surfaceContainerHighest: claro ? const Color(0xFFEDE7DB) : const Color(0xFF1B3256),
      outline: claro ? const Color(0xFF9A8B7F) : const Color(0xFF5E7390),
      outlineVariant: semanticos.borde,
      shadow: Paleta.azulNoche,
      scrim: Paleta.azulNoche,
      inverseSurface: claro ? Paleta.azulNoche : Paleta.crema,
      onInverseSurface: claro ? Paleta.crema : Paleta.azulNoche,
      inversePrimary: claro ? Paleta.azulCielo : Paleta.azulTalavera,
    );

    final base = ThemeData(brightness: brillo, useMaterial3: true, colorScheme: colores);
    final textos = base.textTheme.apply(bodyColor: texto, displayColor: texto);
    final bordeFino = BorderSide(color: semanticos.borde);

    return base.copyWith(
      scaffoldBackgroundColor: fondo,
      extensions: [semanticos],
      textTheme: textos.copyWith(
        headlineMedium: textos.headlineMedium?.copyWith(fontWeight: FontWeight.w700, letterSpacing: -0.4),
        headlineSmall: textos.headlineSmall?.copyWith(fontWeight: FontWeight.w700, letterSpacing: -0.2),
        titleLarge: textos.titleLarge?.copyWith(fontWeight: FontWeight.w700),
        titleMedium: textos.titleMedium?.copyWith(fontWeight: FontWeight.w600),
        labelLarge: textos.labelLarge?.copyWith(fontWeight: FontWeight.w600),
      ),
      appBarTheme: AppBarTheme(
        centerTitle: false,
        elevation: 0,
        scrolledUnderElevation: 0,
        backgroundColor: fondo,
        foregroundColor: texto,
        surfaceTintColor: Colors.transparent,
        shape: Border(bottom: bordeFino),
        titleTextStyle: textos.titleLarge?.copyWith(fontWeight: FontWeight.w700, color: texto),
      ),
      cardTheme: CardThemeData(
        margin: EdgeInsets.zero,
        clipBehavior: Clip.antiAlias,
        elevation: 0,
        color: superficie,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: Formas.radioTarjeta, side: bordeFino),
      ),
      dividerTheme: DividerThemeData(color: semanticos.borde, space: 1, thickness: 1),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: superficie,
        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        border: OutlineInputBorder(borderRadius: Formas.radioControl, borderSide: bordeFino),
        enabledBorder: OutlineInputBorder(borderRadius: Formas.radioControl, borderSide: bordeFino),
        focusedBorder:
            OutlineInputBorder(borderRadius: Formas.radioControl, borderSide: BorderSide(color: primario, width: 2)),
        errorBorder:
            OutlineInputBorder(borderRadius: Formas.radioControl, borderSide: BorderSide(color: semanticos.peligro)),
        focusedErrorBorder: OutlineInputBorder(
            borderRadius: Formas.radioControl, borderSide: BorderSide(color: semanticos.peligro, width: 2)),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size(48, 48),
          shape: RoundedRectangleBorder(borderRadius: Formas.radioControl),
          textStyle: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(48, 48),
          side: BorderSide(color: semanticos.borde),
          shape: RoundedRectangleBorder(borderRadius: Formas.radioControl),
          textStyle: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          minimumSize: const Size(48, 44),
          shape: RoundedRectangleBorder(borderRadius: Formas.radioControl),
          textStyle: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ),
      iconButtonTheme: IconButtonThemeData(
        style: IconButton.styleFrom(shape: RoundedRectangleBorder(borderRadius: Formas.radioControl)),
      ),
      chipTheme: ChipThemeData(
        shape: RoundedRectangleBorder(borderRadius: Formas.radioControl, side: bordeFino),
        backgroundColor: superficie,
        selectedColor: colores.primaryContainer,
        side: bordeFino,
        labelStyle: textos.labelLarge,
        showCheckmark: false,
      ),
      segmentedButtonTheme: SegmentedButtonThemeData(
        style: SegmentedButton.styleFrom(
          shape: RoundedRectangleBorder(borderRadius: Formas.radioControl),
          side: bordeFino,
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: superficie,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        height: 68,
        indicatorColor: colores.primaryContainer,
        indicatorShape: RoundedRectangleBorder(borderRadius: Formas.radioControl),
        labelTextStyle: WidgetStatePropertyAll(textos.labelMedium?.copyWith(fontWeight: FontWeight.w600)),
      ),
      navigationRailTheme: NavigationRailThemeData(
        backgroundColor: superficie,
        indicatorColor: colores.primaryContainer,
        indicatorShape: RoundedRectangleBorder(borderRadius: Formas.radioControl),
        selectedLabelTextStyle: textos.labelLarge?.copyWith(color: primario, fontWeight: FontWeight.w700),
        unselectedLabelTextStyle: textos.labelLarge?.copyWith(color: colores.onSurfaceVariant),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: superficie,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: Formas.radioTarjeta),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: superficie,
        surfaceTintColor: Colors.transparent,
        showDragHandle: true,
        shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(Formas.tarjeta))),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: claro ? Paleta.azulNoche : Paleta.crema,
        contentTextStyle: TextStyle(color: claro ? Paleta.crema : Paleta.azulNoche),
        shape: RoundedRectangleBorder(borderRadius: Formas.radioControl),
      ),
      listTileTheme: ListTileThemeData(shape: RoundedRectangleBorder(borderRadius: Formas.radioControl)),
      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.resolveWith(
            (e) => e.contains(WidgetState.selected) ? colores.onPrimary : colores.outline),
        trackColor: WidgetStateProperty.resolveWith(
            (e) => e.contains(WidgetState.selected) ? primario : colores.surfaceContainerHighest),
      ),
      tabBarTheme: TabBarThemeData(
        indicatorSize: TabBarIndicatorSize.label,
        dividerColor: semanticos.borde,
        labelStyle: textos.labelLarge,
        unselectedLabelStyle: textos.labelLarge,
      ),
    );
  }
}
