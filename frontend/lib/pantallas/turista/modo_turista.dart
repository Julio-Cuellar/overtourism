import 'package:flutter/material.dart';

import '../../estado/restaurante_demo.dart';
import '../../widgets/adaptativo.dart';
import '../restaurantes/alcance.dart';
import 'explorar.dart';
import 'pasaporte.dart';
import 'restaurantes.dart';
import 'ruta.dart';

/// Modo turista: cuatro secciones en la barra inferior (o lateral en pantallas anchas), pensado para una mano en el
/// teléfono. El restaurante de la demo se comparte para que la ficha muestre sus mesas libres en vivo.
class ModoTurista extends StatefulWidget {
  const ModoTurista({super.key, required this.restaurante});

  final RestauranteDemo restaurante;

  @override
  State<ModoTurista> createState() => _ModoTuristaState();
}

class _ModoTuristaState extends State<ModoTurista> {
  int _seccion = 0;

  static const _titulos = ['Explorar', 'Restaurantes', 'Pasaporte', 'Ruta del día'];

  @override
  Widget build(BuildContext context) {
    return AlcanceRestaurante(
      restaurante: widget.restaurante,
      child: NavegacionAdaptativa(
        titulo: Text(_titulos[_seccion]),
        indice: _seccion,
        alCambiar: (indice) => setState(() => _seccion = indice),
        destinos: const [
          DestinoNavegacion(icono: Icons.explore_outlined, iconoActivo: Icons.explore, etiqueta: 'Explorar'),
          DestinoNavegacion(icono: Icons.restaurant_outlined, iconoActivo: Icons.restaurant, etiqueta: 'Restaurantes'),
          DestinoNavegacion(icono: Icons.local_activity_outlined, iconoActivo: Icons.local_activity, etiqueta: 'Pasaporte'),
          DestinoNavegacion(icono: Icons.route_outlined, iconoActivo: Icons.route, etiqueta: 'Ruta'),
        ],
        cuerpo: IndexedStack(
          index: _seccion,
          children: const [Explorar(), DirectorioRestaurantes(), Pasaporte(), RutaSugerida()],
        ),
      ),
    );
  }
}
