import 'package:flutter_test/flutter_test.dart';
import 'package:negocio360/estado/restaurante_demo.dart';
import 'package:negocio360/modelos/permisos_restaurante.dart';
import 'package:negocio360/modelos/restaurantes.dart';

/// Reglas de Restaurantes que la maqueta reproduce (docs/diseno/restaurantes.md). Cada prueba nombra la regla.
void main() {
  late DateTime ahora;
  late RestauranteDemo r;

  setUp(() {
    ahora = DateTime(2026, 10, 16, 14, 0);
    r = RestauranteDemo(reloj: () => ahora);
  });

  tearDown(() => r.dispose());

  ReglaRestaurante fallo(void Function() accion) {
    try {
      accion();
    } on ReglaRestaurante catch (e) {
      return e;
    }
    fail('Se esperaba una ReglaRestaurante');
  }

  String tokenDe(String etiqueta) => r.mesas.firstWhere((m) => m.etiqueta == etiqueta).qrToken;

  LineaPedidoQr linea(String id, [int cantidad = 1]) => LineaPedidoQr(platilloId: id, cantidad: cantidad);

  group('costo y margen (RC-05, RC-06)', () {
    test('la cemita poblana cuesta 43.25 y deja 41.75 (49.12 %) con las cifras del diseño', () {
      final cemita = r.platillo('p-cemita')!;
      final costo = r.costoDe(r.recetaVigente('p-cemita')!, cemita.precio);

      expect(cemita.precio, 8500);
      expect(costo.costo, 4325);
      expect(costo.margen, 4175);
      expect(costo.margenPuntosBase, 4912);
      expect(costo.incompleto, isFalse);
    });

    test('si falta el CPP de un insumo el costo queda incompleto y no se estima', () {
      final costo = r.costoDe(r.recetaVigente('p-mole')!, r.platillo('p-mole')!.precio);

      expect(costo.incompleto, isTrue);
      expect(costo.costo, isNull);
      expect(costo.margen, isNull);
      expect(costo.sinCosto, ['Chocolate de mesa']);
    });

    test('el redondeo es HALF_UP también con negativos', () {
      expect(RestauranteDemo.dividirRedondeado(5, 2), 3);
      expect(RestauranteDemo.dividirRedondeado(4, 3), 1);
      expect(RestauranteDemo.dividirRedondeado(-5, 2), -3);
    });
  });

  group('recetas: preparar y confirmar (RC-01, RC-02, RC-08)', () {
    test('confirmar crea la versión 2, cierra la 1 y conserva ambas', () {
      final vigente = r.recetaVigente('p-cemita')!;
      final borrador = r.prepararReceta('p-cemita', rendimiento: 1, insumos: [
        ...vigente.insumos,
      ]..[1] = const InsumoReceta(articuloId: 'a-milanesa', cantidad: 150, mermaPct: 5));

      expect(borrador.costo.costo, greaterThan(4325));
      final nueva = r.confirmarReceta(borrador);

      expect(nueva.numero, 2);
      expect(r.versionesDe('p-cemita').first.vigente, isFalse);
      expect(r.recetaVigente('p-cemita')!.numero, 2);
    });

    test('un insumo repetido, una cantidad cero y un artículo dado de baja se rechazan', () {
      expect(
        fallo(() => r.prepararReceta('p-cemita', rendimiento: 1, insumos: const [
              InsumoReceta(articuloId: 'a-pan', cantidad: 1),
              InsumoReceta(articuloId: 'a-pan', cantidad: 2),
            ])).codigo,
        'INSUMO_REPETIDO',
      );
      expect(
        fallo(() => r.prepararReceta('p-cemita', rendimiento: 1, insumos: const [InsumoReceta(articuloId: 'a-pan', cantidad: 0)])).codigo,
        'UNIDAD_NO_CONVERTIBLE',
      );
      expect(
        fallo(() => r.prepararReceta('p-cemita', rendimiento: 1, insumos: const [InsumoReceta(articuloId: 'a-jamaica', cantidad: 5)])).codigo,
        'ARTICULO_INACTIVO',
      );
    });

    test('si cambia el precio entre preparar y confirmar falla con COMANDO_CAMBIO', () {
      final borrador = r.prepararReceta('p-cemita', rendimiento: 1, insumos: r.recetaVigente('p-cemita')!.insumos);
      final p = r.platillo('p-cemita')!;
      r.editarPlatillo(p.id,
          nombre: p.nombre, descripcion: p.descripcion, precio: 9000, categoriaId: p.categoriaId, etiquetas: p.etiquetas, temporada: p.temporada, publicado: p.publicado);

      expect(fallo(() => r.confirmarReceta(borrador)).codigo, 'COMANDO_CAMBIO');
    });

    test('el rendimiento va de 1 a 500', () {
      expect(fallo(() => r.prepararReceta('p-cemita', rendimiento: 501, insumos: r.recetaVigente('p-cemita')!.insumos)).codigo, 'LIMITE_EXCEDIDO');
    });
  });

  group('menú (MN-01, MN-05, MN-06, MN-09, MN-10, MN-14)', () {
    test('precio cero y nombre repetido se rechazan', () {
      expect(fallo(() => r.crearPlatillo(nombre: 'Tamal', descripcion: '', precio: 0)).codigo, 'PRECIO_INVALIDO');
      expect(fallo(() => r.crearPlatillo(nombre: 'agua de HORCHATA', descripcion: '', precio: 3000)).codigo, 'NOMBRE_DUPLICADO');
      expect(fallo(() => r.crearPlatillo(nombre: 'A', descripcion: '', precio: 3000)).codigo, 'NOMBRE_INVALIDO');
    });

    test('un platillo agotado o fuera de temporada no se puede agregar a una cuenta', () {
      final cuenta = r.abrirCuenta(mesaId: 'm5', comensales: 2);

      expect(fallo(() => r.agregarLinea(cuenta.id, 'p-nogada', 1)).codigo, 'PLATILLO_AGOTADO');
      expect(fallo(() => r.agregarLinea(cuenta.id, 'p-tuna', 1)).codigo, 'FUERA_DE_TEMPORADA');
    });

    test('la temporada puede cruzar el fin de año', () {
      const temporada = Temporada(mesDesde: 12, diaDesde: 1, mesHasta: 1, diaHasta: 6);

      expect(temporada.contiene(DateTime(2026, 12, 25)), isTrue);
      expect(temporada.contiene(DateTime(2027, 1, 5)), isTrue);
      expect(temporada.contiene(DateTime(2026, 10, 16)), isFalse);
    });

    test('el menú público omite lo no publicado y lo fuera de temporada, y muestra lo agotado', () {
      final publico = r.menuPublico()!;
      final ids = publico.map((p) => p.id).toSet();

      expect(ids.contains('p-corrida'), isFalse);
      expect(ids.contains('p-tuna'), isFalse);
      expect(ids.contains('p-nogada'), isTrue);
      expect(publico.firstWhere((p) => p.id == 'p-nogada').agotado, isTrue);
    });

    test('pausar la publicación deja el menú público en nulo (PB-01)', () {
      r.cambiarVisibilidad(false);

      expect(r.menuPublico(), isNull);
      expect(r.mesaPorQr(tokenDe('5')), isNull);
    });

    test('un platillo no publicado se puede vender en el salón (MN-09)', () {
      final cuenta = r.abrirCuenta(mesaId: 'm5', comensales: 2);

      expect(r.agregarLinea(cuenta.id, 'p-corrida', 1).nombre, 'Comida corrida del día');
    });

    test('dar de baja un platillo que está en una cuenta abierta falla (MN-10)', () {
      expect(fallo(() => r.darDeBajaPlatillo('p-mole')).codigo, 'PLATILLO_EN_CUENTA_ABIERTA');
      r.darDeBajaPlatillo('p-sopa');
      expect(r.platillo('p-sopa')!.activo, isFalse);
    });
  });

  group('permisos por puesto (SR-03)', () {
    test('el cocinero marca agotado pero no edita el menú; el mesero no cobra', () {
      r.rol = RolRestaurante.cocinero;
      r.marcarAgotado('p-cemita', true);

      expect(r.platillo('p-cemita')!.agotado, isTrue);
      expect(fallo(() => r.crearPlatillo(nombre: 'Tamal', descripcion: '', precio: 3000)).codigo, 'PERMISO_DENEGADO');

      r.rol = RolRestaurante.mesero;
      expect(fallo(() => r.cobrar('cu-x', 'Efectivo')).codigo, 'PERMISO_DENEGADO');
    });

    test('solo el dueño ve el costo; el cocinero ve la receta sin costos', () {
      expect(RolRestaurante.dueno.puede(Permiso.costoVer), isTrue);
      expect(RolRestaurante.cocinero.puede(Permiso.costoVer), isFalse);
      expect(RolRestaurante.cocinero.puede(Permiso.recetaVer), isTrue);
    });
  });

  group('salón y cuentas (MS, CT)', () {
    test('las mesas libres se derivan de las cuentas: 9 de 12 al arrancar y 8 al abrir otra', () {
      expect(r.disponibilidad.libres, 9);
      expect(r.disponibilidad.total, 12);

      r.abrirCuenta(mesaId: 'm5', comensales: 2);

      expect(r.disponibilidad.libres, 8);
    });

    test('una mesa ocupada o bloqueada no abre otra cuenta', () {
      expect(fallo(() => r.abrirCuenta(mesaId: 'm3', comensales: 2)).codigo, 'MESA_OCUPADA');
      r.bloquearMesa('m4', true);
      expect(fallo(() => r.abrirCuenta(mesaId: 'm4', comensales: 2)).codigo, 'MESA_OCUPADA');
      expect(r.disponibilidad.total, 11);
    });

    test('enviar a cocina crea una comanda por estación con el mismo envío (CT-04, CD-08)', () {
      final cuenta = r.abrirCuenta(mesaId: 'm5', comensales: 2);
      r.agregarLinea(cuenta.id, 'p-cemita', 1);
      r.agregarLinea(cuenta.id, 'p-horchata', 1);

      final comandas = r.enviarACocina(cuenta.id);

      expect(comandas.map((c) => c.estacion).toSet(), {Estacion.cocina, Estacion.barra});
      expect(comandas.map((c) => c.envioId).toSet().length, 1);
      expect(fallo(() => r.enviarACocina(cuenta.id)).codigo, 'SIN_LINEAS_PENDIENTES');
    });

    test('pedir la cuenta exige haber enviado todo (CT-05)', () {
      final cuenta = r.abrirCuenta(mesaId: 'm5', comensales: 2);
      r.agregarLinea(cuenta.id, 'p-cemita', 1);

      expect(fallo(() => r.pedirCuenta(cuenta.id)).codigo, 'LINEAS_SIN_ENVIAR');
      r.enviarACocina(cuenta.id);
      r.pedirCuenta(cuenta.id);
      expect(cuenta.estado, EstadoCuenta.porCobrar);
      expect(fallo(() => r.agregarLinea(cuenta.id, 'p-cemita', 1)).codigo, 'CUENTA_NO_ABIERTA');
      r.reabrirCuenta(cuenta.id);
      expect(cuenta.estado, EstadoCuenta.abierta);
    });

    test('cerrar el servicio con cuentas abiertas pide motivo (MS-05)', () {
      expect(fallo(r.cerrarServicio).codigo, 'MOTIVO_REQUERIDO');
      r.cerrarServicio(motivo: 'Falla de gas');
      expect(r.servicioAbierto, isFalse);
      expect(fallo(() => r.abrirCuenta(mesaId: 'm5', comensales: 2)).codigo, 'SERVICIO_CERRADO');
    });

    test('anular una línea: el mesero puede antes de que esté lista, el dueño después (CT-03)', () {
      final c1042 = r.cuenta(r.cuentas.firstWhere((c) => c.numero == 1042).id)!;
      final cemitas = c1042.lineas.firstWhere((l) => l.nombre.startsWith('Cemita'));
      final horchatas = c1042.lineas.firstWhere((l) => l.nombre.startsWith('Agua de horchata'));

      r.rol = RolRestaurante.mesero;
      expect(fallo(() => r.anularLinea(c1042.id, cemitas.id, MotivoAnulacion.otro)).codigo, 'MOTIVO_REQUERIDO');
      r.anularLinea(c1042.id, cemitas.id, MotivoAnulacion.agotado);
      expect(cemitas.estado, EstadoLinea.anulada);
      // La comanda de la barra ya está lista: el mesero no puede, el dueño sí.
      expect(fallo(() => r.anularLinea(c1042.id, horchatas.id, MotivoAnulacion.errorDelPedido)).codigo, 'PERMISO_DENEGADO');
      r.rol = RolRestaurante.dueno;
      r.anularLinea(c1042.id, horchatas.id, MotivoAnulacion.errorDelPedido);
      expect(horchatas.estado, EstadoLinea.anulada);
    });
  });

  group('cobro y consumo de insumos (CM-01 a CM-06)', () {
    test('cobrar paga la cuenta, libera la mesa y descuenta los insumos por receta', () {
      final cuenta = r.abrirCuenta(mesaId: 'm5', comensales: 2);
      r.agregarLinea(cuenta.id, 'p-cemita', 2);
      r.enviarACocina(cuenta.id);
      r.pedirCuenta(cuenta.id);
      final panAntes = r.articulo('a-pan')!.existencia;
      final milanesaAntes = r.articulo('a-milanesa')!.existencia;

      final resultado = r.cobrar(cuenta.id, 'Tarjeta');

      expect(resultado.venta.total, 17000);
      expect(cuenta.estado, EstadoCuenta.pagada);
      expect(cuenta.ventaFolio, resultado.venta.folio);
      expect(r.estadoDe(r.mesa('m5')!), EstadoMesa.libre);
      expect(panAntes - r.articulo('a-pan')!.existencia, 2 * 10000);
      // 120 g con 5 % de merma por cemita: 126 g × 2.
      expect(milanesaAntes - r.articulo('a-milanesa')!.existencia, 252 * 10000);
      expect(resultado.consumoFallido, isNull);
    });

    test('un artículo dado de baja no descuenta nada y deja un consumo fallido sin bloquear la venta', () {
      final cuenta = r.abrirCuenta(mesaId: 'm5', comensales: 2);
      r.agregarLinea(cuenta.id, 'p-jamaica', 1);
      r.agregarLinea(cuenta.id, 'p-horchata', 1);
      r.enviarACocina(cuenta.id);
      r.pedirCuenta(cuenta.id);
      final azucarAntes = r.articulo('a-azucar')!.existencia;
      final fallosAntes = r.consumosFallidos.length;

      final resultado = r.cobrar(cuenta.id, 'Efectivo');

      expect(cuenta.estado, EstadoCuenta.pagada);
      expect(resultado.consumoFallido!.codigo, 'ARTICULO_INACTIVO');
      expect(r.consumosFallidos.length, fallosAntes + 1);
      expect(r.articulo('a-azucar')!.existencia, azucarAntes);
    });

    test('solo se cobra una cuenta que ya pidió la cuenta', () {
      final cuenta = r.abrirCuenta(mesaId: 'm5', comensales: 2);

      expect(fallo(() => r.cobrar(cuenta.id, 'Efectivo')).codigo, 'TRANSICION_INVALIDA');
    });

    test('una porción de un lote de 40 descuenta 1/40 de cada insumo, con merma (RC-04)', () {
      final cuenta = r.abrirCuenta(mesaId: 'm5', comensales: 1);
      r.agregarLinea(cuenta.id, 'p-mole', 1);
      r.enviarACocina(cuenta.id);
      r.pedirCuenta(cuenta.id);
      final polloAntes = r.articulo('a-pollo')!.existencia;

      r.cobrar(cuenta.id, 'Efectivo');

      // 6000 g × 1.08 ÷ 40 = 162 g.
      expect(polloAntes - r.articulo('a-pollo')!.existencia, 162 * 10000);
    });
  });

  group('cocina (CD-01 a CD-04)', () {
    test('la comanda avanza nueva → en preparación → lista y solo se devuelve desde lista', () {
      final nueva = r.comandas.firstWhere((c) => c.estado == EstadoComanda.nueva);
      r.rol = RolRestaurante.cocinero;

      r.avanzarComanda(nueva.id);
      r.avanzarComanda(nueva.id);
      expect(nueva.estado, EstadoComanda.lista);
      expect(fallo(() => r.avanzarComanda(nueva.id)).codigo, 'TRANSICION_INVALIDA');
      r.devolverComanda(nueva.id);
      expect(nueva.estado, EstadoComanda.enPreparacion);
      expect(fallo(() => r.devolverComanda(nueva.id)).codigo, 'TRANSICION_INVALIDA');
    });

    test('el cocinero no entrega ni el mesero marca lista', () {
      final lista = r.comandas.firstWhere((c) => c.estado == EstadoComanda.lista);
      final enPrep = r.comandas.firstWhere((c) => c.estado == EstadoComanda.enPreparacion);

      r.rol = RolRestaurante.mesero;
      expect(fallo(() => r.avanzarComanda(enPrep.id)).codigo, 'PERMISO_DENEGADO');
      r.entregarComanda(lista.id);
      expect(lista.estado, EstadoComanda.entregada);
    });

    test('una comanda con 15 minutos o más se resalta como lenta y el tiempo lo calcula el código', () {
      final enPrep = r.comandas.firstWhere((c) => c.estado == EstadoComanda.enPreparacion);

      expect(r.minutosDe(enPrep), 18);
      expect(r.esLenta(enPrep), isTrue);

      ahora = ahora.add(const Duration(minutes: 30));
      expect(r.minutosDe(enPrep), 48);
    });

    test('las comandas de una estación van de la más antigua a la más nueva', () {
      final cocina = r.comandasDe(Estacion.cocina);

      expect(cocina.every((c) => c.estacion == Estacion.cocina), isTrue);
      for (var i = 1; i < cocina.length; i++) {
        expect(cocina[i].enviadaEn.isBefore(cocina[i - 1].enviadaEn), isFalse);
      }
    });
  });

  group('mesa por QR (QR-01 a QR-21)', () {
    test('un token inexistente o rotado responde nulo y no revela nada', () {
      expect(r.mesaPorQr('no-existe'), isNull);
      final viejo = tokenDe('5');
      r.regenerarQr('m5');

      expect(r.mesaPorQr(viejo), isNull);
      expect(r.mesaPorQr(tokenDe('5')), isNotNull);
      expect(fallo(() => r.pedirPorQr(viejo, 'd1', [linea('p-cemita')], llave: 'k0')).codigo, 'QR_INVALIDO');
    });

    test('el primer pedido abre la cuenta y va directo a cocina y barra sin aceptación', () {
      final token = tokenDe('5');
      final resultado = r.pedirPorQr(token, 'd1', [linea('p-cemita', 2), linea('p-horchata')], llave: 'k1');

      expect(resultado.abrioCuenta, isTrue);
      final cuenta = r.cuentaActivaDe('m5')!;
      expect(cuenta.origen, OrigenPedido.qr);
      expect(cuenta.lineas.every((l) => l.estado == EstadoLinea.enviada), isTrue);
      expect(r.comandas.where((c) => c.cuentaId == cuenta.id).map((c) => c.estacion).toSet(), {Estacion.cocina, Estacion.barra});
      expect(r.avisos.first.texto, contains('mesa 5'));
      expect(r.disponibilidad.libres, 8);
    });

    test('repetir la misma llave no duplica el pedido (Idempotency-Key)', () {
      final token = tokenDe('5');
      r.pedirPorQr(token, 'd1', [linea('p-cemita')], llave: 'k1');
      r.pedirPorQr(token, 'd1', [linea('p-cemita')], llave: 'k1');

      expect(r.cuentaActivaDe('m5')!.lineas.length, 1);
    });

    test('un platillo agotado o fuera de temporada rechaza todo el pedido', () {
      final token = tokenDe('5');

      expect(fallo(() => r.pedirPorQr(token, 'd1', [linea('p-cemita'), linea('p-nogada')], llave: 'k1')).codigo, 'PLATILLO_AGOTADO');
      expect(r.cuentaActivaDe('m5'), isNull);
      expect(fallo(() => r.pedirPorQr(token, 'd1', [linea('p-tuna')], llave: 'k2')).codigo, 'FUERA_DE_TEMPORADA');
    });

    test('los límites del pedido se respetan (QR-15)', () {
      final token = tokenDe('5');

      expect(fallo(() => r.pedirPorQr(token, 'd1', [linea('p-cemita', 11)], llave: 'k1')).codigo, 'LIMITE_QR_EXCEDIDO');
      expect(
        fallo(() => r.pedirPorQr(token, 'd1', [for (var i = 0; i < 21; i++) linea('p-cemita')], llave: 'k2')).codigo,
        'LIMITE_QR_EXCEDIDO',
      );
      // 10 × 26,000 en una cuenta supera los 3,000.00: dos nogadas no, pero 12 moles sí.
      expect(fallo(() => r.pedirPorQr(token, 'd1', [linea('p-mole', 10), linea('p-pipian', 10), linea('p-cemita', 10)], llave: 'k3')).codigo, 'LIMITE_QR_EXCEDIDO');
    });

    test('seis pedidos en un minuto con el mismo token: el sexto se rechaza (QR-16)', () {
      final token = tokenDe('5');
      for (var i = 0; i < 5; i++) {
        r.pedirPorQr(token, 'd1', [linea('p-cafe')], llave: 'k$i');
      }

      expect(fallo(() => r.pedirPorQr(token, 'd1', [linea('p-cafe')], llave: 'k5')).codigo, 'TASA_EXCEDIDA');
      ahora = ahora.add(const Duration(minutes: 2));
      r.pedirPorQr(token, 'd1', [linea('p-cafe')], llave: 'k6');
    });

    test('con el servicio cerrado, el QR apagado o la mesa en pausa no se acepta el pedido', () {
      final token = tokenDe('5');

      r.habilitarQr(false);
      expect(r.mesaPorQr(token)!.aceptaPedidos, isFalse);
      expect(fallo(() => r.pedirPorQr(token, 'd1', [linea('p-cafe')], llave: 'k1')).codigo, 'QR_DESHABILITADO');
      r.habilitarQr(true);
      r.pausarQr('m5', true);
      expect(fallo(() => r.pedirPorQr(token, 'd1', [linea('p-cafe')], llave: 'k2')).codigo, 'QR_PAUSADO');
      r.pausarQr('m5', false);
      r.cerrarServicio(motivo: 'Cierre');
      expect(fallo(() => r.pedirPorQr(token, 'd1', [linea('p-cafe')], llave: 'k3')).codigo, 'SERVICIO_CERRADO');
    });

    test('cada dispositivo ve solo sus líneas y la cancelación llega con su motivo (QR-10, QR-11)', () {
      final token = tokenDe('5');
      r.pedirPorQr(token, 'd1', [linea('p-mole')], llave: 'k1');
      r.pedirPorQr(token, 'd2', [linea('p-cafe')], llave: 'k2');
      final cuenta = r.cuentaActivaDe('m5')!;

      expect(r.estadoDePedidoQr(token, 'd1')!.lineas.map((l) => l.nombre), ['Mole poblano con pollo']);
      expect(r.estadoDePedidoQr(token, 'd1')!.lineas.single.estado, EstadoVistaLinea.enCocina);

      r.rol = RolRestaurante.mesero;
      r.anularLinea(cuenta.id, cuenta.lineas.first.id, MotivoAnulacion.agotado);

      final vista = r.estadoDePedidoQr(token, 'd1')!.lineas.single;
      expect(vista.estado, EstadoVistaLinea.cancelado);
      expect(vista.mensaje, 'Cancelado: agotado');
      expect(r.estadoDePedidoQr(token, 'd3')!.lineas, isEmpty);
    });

    test('el comensal sigue el avance de la comanda (en cocina → preparando → listo)', () {
      final token = tokenDe('5');
      r.pedirPorQr(token, 'd1', [linea('p-cemita')], llave: 'k1');
      final comanda = r.comandas.last;

      r.rol = RolRestaurante.cocinero;
      r.avanzarComanda(comanda.id);
      expect(r.estadoDePedidoQr(token, 'd1')!.lineas.single.estado, EstadoVistaLinea.preparando);
      r.avanzarComanda(comanda.id);
      expect(r.estadoDePedidoQr(token, 'd1')!.lineas.single.estado, EstadoVistaLinea.listo);
    });

    test('pedir la cuenta por QR avisa al personal y bloquea nuevos pedidos hasta que pague (QR-13, QR-14)', () {
      final token = tokenDe('5');
      r.pedirPorQr(token, 'd1', [linea('p-cemita')], llave: 'k1');

      r.pedirCuentaPorQr(token);

      expect(r.mesaPorQr(token)!.estadoCuenta, EstadoCuentaQr.solicitada);
      expect(r.avisos.first.texto, contains('pidió la cuenta'));
      expect(fallo(() => r.pedirPorQr(token, 'd1', [linea('p-cafe')], llave: 'k2')).codigo, 'CUENTA_SOLICITADA');

      final cuenta = r.cuentaActivaDe('m5')!;
      r.cobrar(cuenta.id, 'Efectivo');

      final nueva = r.pedirPorQr(token, 'd1', [linea('p-cafe')], llave: 'k3');
      expect(nueva.abrioCuenta, isTrue);
      expect(r.cuentaActivaDe('m5')!.numero, isNot(cuenta.numero));
    });

    test('tres cuentas sin comensales en una hora pausan el QR de esa mesa (QR-17)', () {
      final token = tokenDe('5');
      for (var i = 0; i < 3; i++) {
        r.pedirPorQr(token, 'd1', [linea('p-cafe')], llave: 'k$i');
        r.cancelarCuenta(r.cuentaActivaDe('m5')!.id, MotivoAnulacion.sinComensales);
      }

      expect(r.mesa('m5')!.qrPausado, isTrue);
      expect(r.mesaPorQr(token)!.aceptaPedidos, isFalse);
      expect(r.disponibilidad.libres, 9);
    });

    test('la vista pública no trae datos de la cuenta (QR-03)', () {
      final token = tokenDe('3');
      final vista = r.mesaPorQr(token)!;

      expect(vista.estadoCuenta, EstadoCuentaQr.abierta);
      expect(vista.mesa, '3');
      expect(vista.restaurante, r.nombre);
    });
  });
}
