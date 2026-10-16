import 'package:flutter/material.dart';

import 'explorar.dart';
import 'pasaporte.dart';
import 'ruta.dart';

/// Modo turista: tres secciones en la barra inferior, pensado para una mano en el telefono.
class ModoTurista extends StatefulWidget {
  const ModoTurista({super.key});

  @override
  State<ModoTurista> createState() => _ModoTuristaState();
}

class _ModoTuristaState extends State<ModoTurista> {
  int _seccion = 0;

  static const _titulos = ['Explorar', 'Pasaporte', 'Ruta del día'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(_titulos[_seccion])),
      body: IndexedStack(
        index: _seccion,
        children: const [Explorar(), Pasaporte(), RutaSugerida()],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _seccion,
        onDestinationSelected: (indice) => setState(() => _seccion = indice),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.explore_outlined), selectedIcon: Icon(Icons.explore),
              label: 'Explorar'),
          NavigationDestination(icon: Icon(Icons.local_activity_outlined), selectedIcon: Icon(Icons.local_activity),
              label: 'Pasaporte'),
          NavigationDestination(icon: Icon(Icons.route_outlined), selectedIcon: Icon(Icons.route), label: 'Ruta'),
        ],
      ),
    );
  }
}
