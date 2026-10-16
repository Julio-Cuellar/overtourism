import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:negocio360/app.dart';

/// Recorridos de la maqueta: la app arranca, ofrece los dos modos y rotula los datos simulados.
void main() {
  testWidgets('la portada ofrece el modo turista y el modo negocio', (tester) async {
    await tester.pumpWidget(const Negocio360App());

    expect(find.text('Soy turista'), findsOneWidget);
    expect(find.text('Tengo un negocio'), findsOneWidget);
  });

  testWidgets('el modo turista muestra negocios rotulados como datos de ejemplo', (tester) async {
    await tester.pumpWidget(const Negocio360App());
    await tester.tap(find.text('Soy turista'));
    await tester.pumpAndSettle();

    expect(find.text('Explorar'), findsWidgets);
    expect(find.textContaining('Datos de ejemplo'), findsWidgets);
  });

  testWidgets('el modo negocio pide abrir turno antes de cobrar', (tester) async {
    await tester.pumpWidget(const Negocio360App());
    await tester.tap(find.text('Tengo un negocio'));
    await tester.pumpAndSettle();

    expect(find.text('Abrir turno'), findsOneWidget);
  });

  testWidgets('abrir turno, agregar un producto y cobrar deja la venta con folio 1', (tester) async {
    await tester.pumpWidget(const Negocio360App());
    await tester.tap(find.text('Tengo un negocio'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('fondo')), '500');
    await tester.tap(find.text('Abrir turno'));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('agregar-Mole poblano (orden)')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('ir-a-cobrar')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('confirmar-cobro')));
    await tester.pumpAndSettle();

    expect(find.textContaining('Folio 1'), findsOneWidget);
  });
}
