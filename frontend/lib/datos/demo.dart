/// Datos de EJEMPLO de la maqueta. Todo lo de aqui es inventado y la app lo rotula como «Datos de ejemplo».
///
/// Productos y cupones son los mismos que el servidor sirve en `productosSimulados` y `cuponesSimulados`
/// (backend `canal.demo`). Los negocios son ficticios: el plan es reemplazarlos por negocios reales del DENUE de
/// INEGI ya limpios (ver Bitacora). No representan a ningun negocio real.
library;

import '../modelos/modelos.dart';

/// Rotulo que acompana todo dato simulado.
const rotuloDemo = 'Datos de ejemplo';

const productosDemo = <Producto>[
  Producto(id: '0192f1b0-0001-7000-8000-00000000c001', descripcion: 'Mole poblano (orden)', unidad: 'pieza',
      precio: 12000),
  Producto(id: '0192f1b0-0002-7000-8000-00000000c002', descripcion: 'Chiles en nogada', unidad: 'pieza',
      precio: 26000),
  Producto(id: '0192f1b0-0003-7000-8000-00000000c003', descripcion: 'Cemita de milanesa', unidad: 'pieza',
      precio: 8500),
  Producto(id: '0192f1b0-0004-7000-8000-00000000c004', descripcion: 'Agua de jamaica', unidad: 'pieza',
      precio: 3000),
  Producto(id: '0192f1b0-0005-7000-8000-00000000c005', descripcion: 'Camote poblano a granel', unidad: 'kg',
      precio: 14000, admiteFraccion: true),
  Producto(id: '0192f1b0-0006-7000-8000-00000000c006', descripcion: 'Taza de Talavera', unidad: 'pieza',
      precio: 18000),
];

const _cupon20 = Cupon(codigo: 'PASAPORTE-20', descuento: 2000, descripcion: r'$20 de descuento en tu consumo');
const _cupon50 = Cupon(codigo: 'PASAPORTE-50', descuento: 5000, descripcion: r'$50 de descuento en artesanías');

const cuponesDemo = <Cupon>[_cupon20, _cupon50];

const negociosDemo = <Negocio>[
  Negocio(
    id: 'n1',
    nombre: 'Fonda La Cazuela (ejemplo)',
    giro: 'Fonda',
    zona: 'Barrio de Analco',
    descripcion: 'Comida corrida y mole poblano hecho en casa.',
    horario: '9:00 a 18:00',
    intereses: {Interes.gastronomia},
    cupon: _cupon20,
  ),
  Negocio(
    id: 'n2',
    nombre: 'Taller Azul Talavera (ejemplo)',
    giro: 'Taller artesanal',
    zona: 'Barrio del Artista',
    descripcion: 'Piezas de talavera pintadas a mano; recorrido por el taller.',
    horario: '10:00 a 19:00',
    intereses: {Interes.artesanias, Interes.culturaYArte},
    cupon: _cupon50,
  ),
  Negocio(
    id: 'n3',
    nombre: 'Dulces del Convento (ejemplo)',
    giro: 'Dulcería',
    zona: 'Centro Histórico',
    descripcion: 'Camotes, tortitas de Santa Clara y dulces típicos a granel.',
    horario: '10:00 a 20:00',
    intereses: {Interes.gastronomia},
  ),
  Negocio(
    id: 'n4',
    nombre: 'Cemitas Los Portales (ejemplo)',
    giro: 'Cocina económica',
    zona: 'San Pedro Cholula',
    descripcion: 'Cemitas y aguas frescas a dos calles de la zona arqueológica.',
    horario: '8:00 a 17:00',
    intereses: {Interes.gastronomia, Interes.puebloMagico},
    cupon: _cupon20,
  ),
  Negocio(
    id: 'n5',
    nombre: 'Galería del Callejón (ejemplo)',
    giro: 'Galería',
    zona: 'Atlixco',
    descripcion: 'Arte local y talleres de grabado los fines de semana.',
    horario: '11:00 a 19:00',
    intereses: {Interes.culturaYArte, Interes.puebloMagico},
  ),
];
