import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:negocio360/app.dart';
import 'package:negocio360/estado/restaurante_demo.dart';
import 'package:negocio360/modelos/permisos_restaurante.dart';
import 'package:negocio360/modelos/restaurantes.dart';
import 'package:negocio360/pantallas/comensal/comensal_qr.dart';
import 'package:negocio360/pantallas/restaurantes/alcance.dart';
import 'package:negocio360/pantallas/restaurantes/modo_restaurante.dart';
import 'package:negocio360/pantallas/turista/modo_turista.dart';
import 'package:negocio360/tema.dart';

/// Recorridos de la maqueta de Restaurantes: personal, comensal por QR y turista, en teléfono y en pantalla ancha.
void main() {
  late RestauranteDemo r;

  setUp(() => r = RestauranteDemo(reloj: () => DateTime(2026, 10, 16, 14, 0)));
  tearDown(() => r.dispose());

  void tamano(WidgetTester tester, double ancho, double alto) {
    tester.view.physicalSize = Size(ancho, alto);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
  }

  Widget app(Widget home) => MaterialApp(theme: Tema.claro(), home: home);

  Future<void> tocar(WidgetTester tester, Finder finder) async {
    await tester.ensureVisible(finder);
    await tester.pumpAndSettle();
    await tester.tap(finder);
    await tester.pumpAndSettle();
  }

  testWidgets('la portada ofrece los cuatro modos', (tester) async {
    await tester.pumpWidget(const Negocio360App());

    expect(find.text('Soy turista'), findsOneWidget);
    expect(find.text('Estoy en una mesa'), findsOneWidget);
    expect(find.text('Mi restaurante'), findsOneWidget);
    expect(find.text('Tengo un negocio'), findsOneWidget);
  });

  group('disposición adaptativa', () {
    testWidgets('en teléfono hay barra inferior y en escritorio un riel lateral', (tester) async {
      tamano(tester, 400, 860);
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();
      expect(find.byType(NavigationBar), findsOneWidget);
      expect(find.byType(NavigationRail), findsNothing);

      tamano(tester, 1440, 900);
      await tester.pumpAndSettle();
      expect(find.byType(NavigationRail), findsOneWidget);
      expect(find.byType(NavigationBar), findsNothing);
    });

    testWidgets('el panel del dueño muestra las cifras del día calculadas por el código', (tester) async {
      tamano(tester, 400, 860);
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();

      expect(find.text('Vendido hoy'), findsOneWidget);
      expect(find.text(r'$1,710.00'), findsOneWidget);
      expect(find.text('9 de 12'), findsOneWidget);
      expect(find.textContaining('Datos de ejemplo'), findsOneWidget);
    });
  });

  group('puestos', () {
    testWidgets('cambiar a cocinero oculta el panel y deja la cocina', (tester) async {
      tamano(tester, 400, 860);
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('selector-puesto')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('puesto-cocinero')));
      await tester.pumpAndSettle();

      expect(r.rol, RolRestaurante.cocinero);
      expect(find.text('Panel'), findsNothing);
      expect(find.text('Empezar a preparar'), findsWidgets);
    });

    testWidgets('el dueño ve el costo y el margen de la cemita; el cocinero no', (tester) async {
      tamano(tester, 1440, 2600);
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Menú'));
      await tester.pumpAndSettle();

      await tocar(tester, find.byKey(const Key('platillo-Cemita poblana de milanesa')));
      expect(find.text(r'$43.25'), findsOneWidget);
      expect(find.text(r'$41.75'), findsOneWidget);
      expect(find.text('49.12 %'), findsWidgets);

      r.rol = RolRestaurante.cocinero;
      await tester.pumpAndSettle();
      await tester.tap(find.text('Menú'));
      await tester.pumpAndSettle();
      await tocar(tester, find.byKey(const Key('platillo-Cemita poblana de milanesa')));

      expect(find.text(r'$43.25'), findsNothing);
      expect(find.text('Pan de cemita'), findsOneWidget);
      expect(find.textContaining('solo los ve quien tiene el permiso'), findsOneWidget);
    });
  });

  group('menú', () {
    testWidgets('un nombre repetido se rechaza con su mensaje en español', (tester) async {
      tamano(tester, 1440, 900);
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Menú'));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('nuevo-platillo')));
      await tester.pumpAndSettle();
      await tester.enterText(find.byKey(const Key('campo-nombre')), 'agua de horchata');
      await tester.enterText(find.byKey(const Key('campo-precio')), '30');
      await tocar(tester, find.byKey(const Key('guardar-platillo')));

      expect(find.text('Ya existe un platillo activo con ese nombre.'), findsOneWidget);
    });

    testWidgets('crear un platillo lo agrega al menú', (tester) async {
      tamano(tester, 1440, 900);
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Menú'));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('nuevo-platillo')));
      await tester.pumpAndSettle();
      await tester.enterText(find.byKey(const Key('campo-nombre')), 'Tlayuda poblana');
      await tester.enterText(find.byKey(const Key('campo-precio')), '95.50');
      await tocar(tester, find.byKey(const Key('guardar-platillo')));

      expect(r.platillos.any((p) => p.nombre == 'Tlayuda poblana' && p.precio == 9550), isTrue);
    });

    testWidgets('marcar agotado impide agregarlo a una cuenta', (tester) async {
      tamano(tester, 1440, 2600);
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Menú'));
      await tester.pumpAndSettle();
      await tocar(tester, find.byKey(const Key('platillo-Cemita poblana de milanesa')));

      await tocar(tester, find.byKey(const Key('agotado')));

      expect(r.platillo('p-cemita')!.agotado, isTrue);
      final cuenta = r.abrirCuenta(mesaId: 'm5', comensales: 2);
      expect(() => r.agregarLinea(cuenta.id, 'p-cemita', 1), throwsA(isA<ReglaRestaurante>()));
    });
  });

  group('salón y cuenta', () {
    testWidgets('abrir la mesa 5, ordenar, enviar a cocina, pedir la cuenta y cobrar descuenta los insumos', (tester) async {
      tamano(tester, 412, 900);
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Salón'));
      await tester.pumpAndSettle();
      final panAntes = r.articulo('a-pan')!.existencia;

      await tocar(tester, find.byKey(const Key('mesa-5')));
      await tocar(tester, find.byKey(const Key('abrir-cuenta')));
      expect(find.text('Cuenta 1044 · 4 comensales · abierta 14:00'), findsOneWidget);

      await tocar(tester, find.byKey(const Key('agregar-platillo')));
      await tocar(tester, find.byKey(const Key('elegir-Cemita poblana de milanesa')));
      await tocar(tester, find.byKey(const Key('mas-cantidad')));
      await tocar(tester, find.byKey(const Key('confirmar-linea')));
      expect(find.text(r'$170.00'), findsWidgets);

      await tocar(tester, find.byKey(const Key('enviar-cocina')));
      expect(find.text('EN SERVICIO'), findsOneWidget);
      await tocar(tester, find.byKey(const Key('pedir-cuenta')));
      expect(find.text('Por cobrar'), findsWidgets);

      await tocar(tester, find.byKey(const Key('cobrar')));
      await tocar(tester, find.byKey(const Key('pago-Tarjeta')));
      await tocar(tester, find.byKey(const Key('confirmar-cobro-cuenta')));

      expect(find.text('Venta 2300 registrada'), findsOneWidget);
      expect(find.text('Insumos descontados'), findsOneWidget);
      expect(find.text('Pan de cemita'), findsOneWidget);
      expect(panAntes - r.articulo('a-pan')!.existencia, 2 * 10000);
      await tocar(tester, find.byKey(const Key('cerrar-cobro')));
      expect(r.estadoDe(r.mesa('m5')!), EstadoMesa.libre);
    });

    testWidgets('un platillo agotado sale apagado con su motivo en el selector', (tester) async {
      tamano(tester, 412, 900);
      final cuenta = r.abrirCuenta(mesaId: 'm5', comensales: 2);
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Salón'));
      await tester.pumpAndSettle();
      await tocar(tester, find.byKey(const Key('mesa-5')));
      await tocar(tester, find.byKey(const Key('agregar-platillo')));

      expect(cuenta.numero, 1044);
      await tester.ensureVisible(find.byKey(const Key('elegir-Chiles en nogada')));
      expect(find.text('Agotado'), findsWidgets);
      final nogada = tester.widget<ListTile>(find.byKey(const Key('elegir-Chiles en nogada')));
      expect(nogada.enabled, isFalse);
    });

    testWidgets('el cajero no abre cuentas desde una mesa libre', (tester) async {
      tamano(tester, 412, 900);
      r.rol = RolRestaurante.cajero;
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();

      await tocar(tester, find.byKey(const Key('mesa-5')));

      expect(find.text('Tu puesto no abre cuentas. Pide al mesero.'), findsOneWidget);
      expect(r.cuentaActivaDe('m5'), isNull);
    });

    testWidgets('el QR de la mesa muestra su enlace y se puede regenerar', (tester) async {
      tamano(tester, 412, 900);
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Salón'));
      await tester.pumpAndSettle();
      final viejo = r.mesa('m5')!.qrToken;

      await tocar(tester, find.byKey(const Key('opciones-5')));
      expect(find.textContaining('/m/$viejo'), findsOneWidget);
      await tocar(tester, find.byKey(const Key('regenerar-qr')));
      await tester.tap(find.text('Regenerar'));
      await tester.pumpAndSettle();

      expect(r.mesa('m5')!.qrToken, isNot(viejo));
      expect(r.mesaPorQr(viejo), isNull);
    });
  });

  group('cocina', () {
    testWidgets('el cocinero empieza a preparar una comanda nueva', (tester) async {
      tamano(tester, 412, 900);
      r.rol = RolRestaurante.cocinero;
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();

      await tocar(tester, find.byKey(const Key('empezar-5')));

      expect(r.comanda(r.comandas.firstWhere((c) => c.numero == 5).id)!.estado, EstadoComanda.enPreparacion);
    });

    testWidgets('en escritorio las tres columnas se ven juntas y la comanda lenta se resalta', (tester) async {
      tamano(tester, 1440, 900);
      r.rol = RolRestaurante.cocinero;
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();

      expect(find.text('Nueva'), findsOneWidget);
      expect(find.text('En preparación'), findsOneWidget);
      expect(find.text('Lista'), findsOneWidget);
      expect(find.text('Pasó de 15 min'), findsOneWidget);
      expect(find.text('ALERGIA: revisar antes de preparar'), findsNothing);
    });

    testWidgets('el mesero no puede marcar lista y el botón avisa a quién le toca', (tester) async {
      tamano(tester, 412, 900);
      r.rol = RolRestaurante.mesero;
      await tester.pumpWidget(app(ModoRestaurante(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Cocina').last);
      await tester.pumpAndSettle();

      expect(find.text('Solo cocina y barra preparan'), findsWidgets);
    });
  });

  group('comensal por QR', () {
    testWidgets('pedir desde la mesa 5 lo manda a cocina y se ve en Mi pedido', (tester) async {
      tamano(tester, 412, 900);
      final token = r.mesas.firstWhere((m) => m.etiqueta == '5').qrToken;
      await tester.pumpWidget(app(AlcanceRestaurante(restaurante: r, child: PaginaComensal(token: token))));
      await tester.pumpAndSettle();

      expect(find.text('Pedidos abiertos'), findsOneWidget);
      await tocar(tester, find.byKey(const Key('agregar-Guacamole con totopos')));
      await tocar(tester, find.byKey(const Key('ver-pedido')));
      await tocar(tester, find.byKey(const Key('enviar-pedido')));

      expect(find.text('1× Guacamole con totopos'), findsOneWidget);
      expect(find.text('En cocina'), findsOneWidget);
      expect(r.cuentaActivaDe('m5')!.origen, OrigenPedido.qr);
      expect(r.avisos.first.texto, contains('mesa 5'));
    });

    testWidgets('si el mesero cancela, el comensal ve el motivo', (tester) async {
      tamano(tester, 412, 900);
      final token = r.mesas.firstWhere((m) => m.etiqueta == '5').qrToken;
      r.pedirPorQr(token, RestauranteDemo.dispositivoDemo, const [LineaPedidoQr(platilloId: 'p-pipian', cantidad: 1)], llave: 'k');
      await tester.pumpWidget(app(AlcanceRestaurante(restaurante: r, child: PaginaComensal(token: token))));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Mi pedido'));
      await tester.pumpAndSettle();
      expect(find.text('En cocina'), findsOneWidget);

      final cuenta = r.cuentaActivaDe('m5')!;
      r.anularLinea(cuenta.id, cuenta.lineas.first.id, MotivoAnulacion.agotado);
      await tester.pumpAndSettle();

      expect(find.text('Cancelado: agotado'), findsOneWidget);
    });

    testWidgets('un QR que ya no existe muestra un mensaje claro', (tester) async {
      tamano(tester, 412, 900);
      await tester.pumpWidget(app(AlcanceRestaurante(restaurante: r, child: const PaginaComensal(token: 'viejo'))));
      await tester.pumpAndSettle();

      expect(find.text('Este código QR no es válido'), findsOneWidget);
    });

    testWidgets('con el servicio cerrado la carta se ve pero no se puede pedir', (tester) async {
      tamano(tester, 412, 900);
      r.cerrarServicio(motivo: 'Cierre');
      final token = r.mesas.firstWhere((m) => m.etiqueta == '5').qrToken;
      await tester.pumpWidget(app(AlcanceRestaurante(restaurante: r, child: PaginaComensal(token: token))));
      await tester.pumpAndSettle();

      expect(find.text('Solo carta'), findsOneWidget);
      expect(find.text('El servicio está cerrado.'), findsOneWidget);
      expect(find.byKey(const Key('agregar-Guacamole con totopos')), findsNothing);
    });

    testWidgets('en pantalla ancha la carta y el pedido se ven lado a lado', (tester) async {
      tamano(tester, 1440, 900);
      final token = r.mesas.firstWhere((m) => m.etiqueta == '5').qrToken;
      await tester.pumpWidget(app(AlcanceRestaurante(restaurante: r, child: PaginaComensal(token: token))));
      await tester.pumpAndSettle();

      expect(find.text('Tu pedido'), findsOneWidget);
      expect(find.text('Guacamole con totopos'), findsOneWidget);
      expect(find.byKey(const Key('ver-pedido')), findsNothing);
    });
  });

  group('turista', () {
    testWidgets('el directorio muestra las mesas libres en vivo del restaurante de la demo', (tester) async {
      tamano(tester, 412, 900);
      await tester.pumpWidget(app(ModoTurista(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Restaurantes').last);
      await tester.pumpAndSettle();

      expect(find.text('Mesas libres 9 de 12'), findsOneWidget);
      r.abrirCuenta(mesaId: 'm5', comensales: 2);
      await tester.pumpAndSettle();
      expect(find.text('Mesas libres 8 de 12'), findsOneWidget);
    });

    testWidgets('filtrar por cocina extranjera deja los restaurantes que la ofrecen', (tester) async {
      tamano(tester, 1440, 900);
      await tester.pumpWidget(app(ModoTurista(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Restaurantes').last);
      await tester.pumpAndSettle();

      await tocar(tester, find.byKey(const Key('tipo-extranjera')));

      expect(find.text('Trattoria del Zócalo (ejemplo)'), findsOneWidget);
      expect(find.text('Fonda La Cazuela (ejemplo)'), findsNothing);
    });

    testWidgets('la ficha muestra el menú público con etiquetas declaradas y sin costos', (tester) async {
      tamano(tester, 412, 3200);
      await tester.pumpWidget(app(ModoTurista(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Restaurantes').last);
      await tester.pumpAndSettle();
      await tocar(tester, find.byKey(const Key('restaurante-r1')));

      expect(find.text('Menú'), findsOneWidget);
      expect(find.text('Guacamole con totopos'), findsOneWidget);
      expect(find.text('Comida corrida del día'), findsNothing);
      expect(find.textContaining('Costo'), findsNothing);
      expect(find.text('Estoy en una mesa: escanear QR'), findsOneWidget);
    });

    testWidgets('desde la ficha se escanea una mesa y se abre la página del comensal', (tester) async {
      tamano(tester, 412, 900);
      await tester.pumpWidget(app(ModoTurista(restaurante: r)));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Restaurantes').last);
      await tester.pumpAndSettle();
      await tocar(tester, find.byKey(const Key('restaurante-r1')));
      await tocar(tester, find.byKey(const Key('estoy-en-mesa')));
      await tocar(tester, find.byKey(const Key('escanear-5')));

      expect(find.text('Mesa 5'), findsWidgets);
      expect(find.text('Pedidos abiertos'), findsOneWidget);
    });
  });
}
