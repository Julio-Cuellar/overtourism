import 'package:flutter/material.dart';

import 'estado/caja_demo.dart';
import 'estado/carrito.dart';
import 'estado/restaurante_demo.dart';
import 'pantallas/portada.dart';
import 'tema.dart';

/// App de Negocio 360: un solo binario con varios modos (turista, comensal por QR, restaurante y caja), pensada primero
/// para el teléfono y adaptada a tableta y web (D-89).
/// En esta etapa es una maqueta que funciona con datos de ejemplo; el cliente GraphQL se conecta despues.
class Negocio360App extends StatefulWidget {
  const Negocio360App({super.key});

  @override
  State<Negocio360App> createState() => _Negocio360AppState();
}

class _Negocio360AppState extends State<Negocio360App> {
  final _caja = CajaDemo();
  final _carrito = Carrito();
  final _restaurante = RestauranteDemo();

  @override
  void dispose() {
    _caja.dispose();
    _carrito.dispose();
    _restaurante.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Negocio 360',
      debugShowCheckedModeBanner: false,
      theme: Tema.claro(),
      darkTheme: Tema.oscuro(),
      home: Portada(caja: _caja, carrito: _carrito, restaurante: _restaurante),
    );
  }
}
