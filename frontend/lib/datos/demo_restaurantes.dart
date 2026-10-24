/// Datos de EJEMPLO de la maqueta de Restaurantes. Todo es inventado y la app lo rotula como «Datos de ejemplo»
/// (regla del proyecto). Las cifras de la cemita poblana son las del diseño (`restaurantes.md`, sección 4.2):
/// precio 85.00, costo teórico 43.25, margen 41.75 (49.12 %). Los negocios no representan a ninguno real.
library;

import '../modelos/restaurantes.dart';

const nombreRestauranteDemo = 'Fonda La Cazuela (ejemplo)';
const idRestauranteDemo = 'r1';

/// Texto de la ficha pública (PB-05).
const leyendaPrecios = 'Precios y disponibilidad sujetos a cambio.';

const categoriasDemo = <CategoriaMenu>[
  CategoriaMenu(id: 'c-entradas', nombre: 'Entradas', orden: 1),
  CategoriaMenu(id: 'c-fuertes', nombre: 'Platos fuertes', orden: 2),
  CategoriaMenu(id: 'c-cemitas', nombre: 'Cemitas y antojitos', orden: 3),
  CategoriaMenu(id: 'c-postres', nombre: 'Postres', orden: 4),
  CategoriaMenu(id: 'c-bebidas', nombre: 'Bebidas', orden: 5, estacion: Estacion.barra),
];

List<Platillo> platillosDemo() => const [
      Platillo(
        id: 'p-guacamole',
        nombre: 'Guacamole con totopos',
        descripcion: 'Aguacate machacado al momento con cebolla y papalo.',
        precio: 7500,
        categoriaId: 'c-entradas',
        etiquetas: {EtiquetaDieta.vegano, EtiquetaDieta.sinLactosa},
        modoInventario: ModoInventario.receta,
        foto: 1,
      ),
      Platillo(
        id: 'p-huitlacoche',
        nombre: 'Quesadillas de huitlacoche',
        descripcion: 'Tortilla de maíz azul, huitlacoche y quesillo.',
        precio: 7000,
        categoriaId: 'c-entradas',
        etiquetas: {EtiquetaDieta.vegetariano, EtiquetaDieta.tipicoPoblano},
        foto: 2,
      ),
      Platillo(
        id: 'p-sopa',
        nombre: 'Sopa de tortilla',
        descripcion: 'Caldo de jitomate con chile pasilla, aguacate y crema.',
        precio: 6500,
        categoriaId: 'c-entradas',
        etiquetas: {EtiquetaDieta.picante},
        foto: 3,
      ),
      Platillo(
        id: 'p-mole',
        nombre: 'Mole poblano con pollo',
        descripcion: 'Mole de la casa con pollo de rancho, arroz y frijoles.',
        precio: 12000,
        categoriaId: 'c-fuertes',
        etiquetas: {EtiquetaDieta.tipicoPoblano, EtiquetaDieta.picante},
        modoInventario: ModoInventario.receta,
        foto: 4,
      ),
      Platillo(
        id: 'p-nogada',
        nombre: 'Chiles en nogada',
        descripcion: 'Chile poblano relleno de picadillo, nogada y granada.',
        precio: 26000,
        categoriaId: 'c-fuertes',
        etiquetas: {EtiquetaDieta.tipicoPoblano, EtiquetaDieta.deTemporada},
        agotado: true,
        temporada: Temporada(mesDesde: 8, diaDesde: 1, mesHasta: 10, diaHasta: 31),
        foto: 5,
      ),
      Platillo(
        id: 'p-pipian',
        nombre: 'Pipián verde con pollo',
        descripcion: 'Salsa de pepita de calabaza con pollo y arroz.',
        precio: 11500,
        categoriaId: 'c-fuertes',
        etiquetas: {EtiquetaDieta.tipicoPoblano, EtiquetaDieta.sinGluten},
        foto: 6,
      ),
      Platillo(
        id: 'p-corrida',
        nombre: 'Comida corrida del día',
        descripcion: 'Sopa, guisado del día, arroz, frijoles y agua.',
        precio: 11000,
        categoriaId: 'c-fuertes',
        publicado: false,
        foto: 7,
      ),
      Platillo(
        id: 'p-cemita',
        nombre: 'Cemita poblana de milanesa',
        descripcion: 'Pan de cemita, milanesa, quesillo, aguacate, chipotle y papalo.',
        precio: 8500,
        categoriaId: 'c-cemitas',
        etiquetas: {EtiquetaDieta.tipicoPoblano, EtiquetaDieta.picante},
        modoInventario: ModoInventario.receta,
        foto: 8,
      ),
      Platillo(
        id: 'p-arabes',
        nombre: 'Tacos árabes (3 piezas)',
        descripcion: 'Cerdo al pastor en pan árabe con salsa de chipotle.',
        precio: 9500,
        categoriaId: 'c-cemitas',
        etiquetas: {EtiquetaDieta.tipicoPoblano, EtiquetaDieta.picante},
        foto: 9,
      ),
      Platillo(
        id: 'p-camote',
        nombre: 'Camote poblano con leche',
        descripcion: 'Camote en dulce con leche condensada.',
        precio: 5500,
        categoriaId: 'c-postres',
        etiquetas: {EtiquetaDieta.vegetariano, EtiquetaDieta.tipicoPoblano},
        foto: 10,
      ),
      Platillo(
        id: 'p-panmuerto',
        nombre: 'Pan de muerto con chocolate',
        descripcion: 'Pan de temporada con chocolate de mesa caliente.',
        precio: 4500,
        categoriaId: 'c-postres',
        etiquetas: {EtiquetaDieta.vegetariano, EtiquetaDieta.deTemporada},
        temporada: Temporada(mesDesde: 10, diaDesde: 1, mesHasta: 11, diaHasta: 2),
        foto: 11,
      ),
      Platillo(
        id: 'p-horchata',
        nombre: 'Agua de horchata',
        descripcion: 'Arroz y canela, vaso grande.',
        precio: 3000,
        categoriaId: 'c-bebidas',
        etiquetas: {EtiquetaDieta.vegano, EtiquetaDieta.sinGluten},
        modoInventario: ModoInventario.receta,
        foto: 12,
      ),
      Platillo(
        id: 'p-jamaica',
        nombre: 'Agua de jamaica',
        descripcion: 'Flor de jamaica, vaso grande.',
        precio: 3000,
        categoriaId: 'c-bebidas',
        etiquetas: {EtiquetaDieta.vegano, EtiquetaDieta.sinGluten},
        modoInventario: ModoInventario.receta,
        foto: 13,
      ),
      Platillo(
        id: 'p-tuna',
        nombre: 'Agua de tuna',
        descripcion: 'Tuna roja de temporada.',
        precio: 3200,
        categoriaId: 'c-bebidas',
        etiquetas: {EtiquetaDieta.vegano, EtiquetaDieta.deTemporada},
        temporada: Temporada(mesDesde: 6, diaDesde: 1, mesHasta: 9, diaHasta: 30),
        foto: 14,
      ),
      Platillo(
        id: 'p-cafe',
        nombre: 'Café de olla',
        descripcion: 'Con canela y piloncillo.',
        precio: 3500,
        categoriaId: 'c-bebidas',
        etiquetas: {EtiquetaDieta.vegano, EtiquetaDieta.sinGluten},
        foto: 15,
      ),
      Platillo(
        id: 'p-cerveza',
        nombre: 'Cerveza artesanal poblana',
        descripcion: 'Botella de 355 ml, ámbar.',
        precio: 6500,
        categoriaId: 'c-bebidas',
        etiquetas: {EtiquetaDieta.contieneAlcohol},
        foto: 16,
      ),
    ];

/// Artículos de Inventario que usan las recetas. Se crean nuevos en cada llamada porque la existencia cambia con las
/// ventas. Existencias y mínimos en diezmilésimas de la unidad base; CPP en milésimas de peso por unidad base.
List<Articulo> articulosDemo() => [
      Articulo(id: 'a-pan', nombre: 'Pan de cemita', unidad: 'pza', existencia: 60 * 10000, minimo: 20 * 10000, cpp: 6000),
      Articulo(id: 'a-milanesa', nombre: 'Milanesa de res', unidad: 'g', existencia: 3000 * 10000, minimo: 1500 * 10000, cpp: 180),
      Articulo(id: 'a-quesillo', nombre: 'Quesillo de Oaxaca', unidad: 'g', existencia: 2500 * 10000, minimo: 800 * 10000, cpp: 160),
      Articulo(id: 'a-aguacate', nombre: 'Aguacate', unidad: 'g', existencia: 1800 * 10000, minimo: 1000 * 10000, cpp: 70),
      Articulo(id: 'a-chipotle', nombre: 'Chile chipotle', unidad: 'g', existencia: 600 * 10000, minimo: 200 * 10000, cpp: 120),
      Articulo(id: 'a-papalo', nombre: 'Papalo', unidad: 'g', existencia: 104 * 10000, minimo: 100 * 10000, cpp: 50),
      Articulo(id: 'a-cebolla', nombre: 'Cebolla blanca', unidad: 'g', existencia: 1200 * 10000, minimo: 500 * 10000, cpp: 30),
      Articulo(id: 'a-pollo', nombre: 'Pollo de rancho', unidad: 'g', existencia: 8000 * 10000, minimo: 3000 * 10000, cpp: 85),
      Articulo(id: 'a-mulato', nombre: 'Chile mulato', unidad: 'g', existencia: 900 * 10000, minimo: 400 * 10000, cpp: 210),
      Articulo(id: 'a-ancho', nombre: 'Chile ancho', unidad: 'g', existencia: 800 * 10000, minimo: 400 * 10000, cpp: 190),
      Articulo(id: 'a-chocolate', nombre: 'Chocolate de mesa', unidad: 'g', existencia: 700 * 10000, minimo: 300 * 10000),
      Articulo(id: 'a-ajonjoli', nombre: 'Ajonjolí', unidad: 'g', existencia: 500 * 10000, minimo: 150 * 10000, cpp: 260),
      Articulo(id: 'a-arroz', nombre: 'Arroz', unidad: 'g', existencia: 5000 * 10000, minimo: 1500 * 10000, cpp: 28),
      Articulo(id: 'a-canela', nombre: 'Canela en raja', unidad: 'g', existencia: 100 * 10000, minimo: 50 * 10000, cpp: 400),
      Articulo(id: 'a-azucar', nombre: 'Azúcar', unidad: 'g', existencia: 6000 * 10000, minimo: 2000 * 10000, cpp: 32),
      Articulo(
          id: 'a-jamaica', nombre: 'Flor de jamaica', unidad: 'g', existencia: 0, minimo: 100 * 10000, cpp: 150, activo: false),
    ];

/// Recetas vigentes de la siembra (versión 1 de cada una).
Map<String, List<InsumoReceta>> insumosDeRecetasDemo() => const {
      'p-cemita': [
        InsumoReceta(articuloId: 'a-pan', cantidad: 1),
        InsumoReceta(articuloId: 'a-milanesa', cantidad: 120, mermaPct: 5),
        InsumoReceta(articuloId: 'a-quesillo', cantidad: 50),
        InsumoReceta(articuloId: 'a-aguacate', cantidad: 60, mermaPct: 10),
        InsumoReceta(articuloId: 'a-chipotle', cantidad: 10),
        InsumoReceta(articuloId: 'a-papalo', cantidad: 3),
        InsumoReceta(articuloId: 'a-cebolla', cantidad: 20),
      ],
      'p-mole': [
        InsumoReceta(articuloId: 'a-pollo', cantidad: 6000, mermaPct: 8),
        InsumoReceta(articuloId: 'a-mulato', cantidad: 400, mermaPct: 5),
        InsumoReceta(articuloId: 'a-ancho', cantidad: 300, mermaPct: 5),
        InsumoReceta(articuloId: 'a-chocolate', cantidad: 200),
        InsumoReceta(articuloId: 'a-ajonjoli', cantidad: 100),
      ],
      'p-horchata': [
        InsumoReceta(articuloId: 'a-arroz', cantidad: 500),
        InsumoReceta(articuloId: 'a-canela', cantidad: 20),
        InsumoReceta(articuloId: 'a-azucar', cantidad: 300),
      ],
      'p-jamaica': [
        InsumoReceta(articuloId: 'a-jamaica', cantidad: 200),
        InsumoReceta(articuloId: 'a-azucar', cantidad: 250),
      ],
      'p-guacamole': [
        InsumoReceta(articuloId: 'a-aguacate', cantidad: 500, mermaPct: 12),
        InsumoReceta(articuloId: 'a-cebolla', cantidad: 60),
        InsumoReceta(articuloId: 'a-papalo', cantidad: 6),
      ],
    };

/// Rendimiento (porciones) de la versión 1 de cada receta (RC-04).
const rendimientosDemo = <String, int>{
  'p-cemita': 1,
  'p-mole': 40,
  'p-horchata': 8,
  'p-jamaica': 10,
  'p-guacamole': 4,
};

const zonasDemo = <Zona>[Zona(id: 'z-salon', nombre: 'Salón'), Zona(id: 'z-terraza', nombre: 'Terraza')];

/// Tokens de QR de ejemplo: 128 bits en base64url, sin relación con el id de la mesa (QR-01). Son ficticios.
const tokensQrDemo = <String>[
  'qk3N7aTz0w1xYb9LmPvAeQ',
  'Zr8cV2dHj5sUo1KfXnW4gA',
  'Hn6bT0yLq9EaMi3RpCdS7w',
  'Uf1xG4kJv8NzB2oYtQhL5A',
  'Wm9sD3rPc7XeA0lKiVjZ6g',
  'Cb5aY2tNh8QuR1fOwEoM4Q',
  'Jd0kS6vLx3GzP9nHbTyF2w',
  'Ae7oM1qWc4UiN5rDjXlK8A',
  'Tg3pB9hZs6VfY0uCkRaE1Q',
  'Lx4eK8nJd2OwS7mGtPiH5g',
  'Rv6yA1cQb9ZoF3xNlUjD0w',
  'Ph2uT5iMz8WsE4kBvYqC7A',
];

List<Mesa> mesasDemo() => [
      Mesa(id: 'm1', etiqueta: '1', zonaId: 'z-salon', capacidad: 2, qrToken: tokensQrDemo[0]),
      Mesa(id: 'm2', etiqueta: '2', zonaId: 'z-salon', capacidad: 4, qrToken: tokensQrDemo[1]),
      Mesa(id: 'm3', etiqueta: '3', zonaId: 'z-salon', capacidad: 4, qrToken: tokensQrDemo[2]),
      Mesa(id: 'm4', etiqueta: '4', zonaId: 'z-salon', capacidad: 4, qrToken: tokensQrDemo[3]),
      Mesa(id: 'm5', etiqueta: '5', zonaId: 'z-salon', capacidad: 6, qrToken: tokensQrDemo[4]),
      Mesa(id: 'm6', etiqueta: '6', zonaId: 'z-salon', capacidad: 4, qrToken: tokensQrDemo[5]),
      Mesa(id: 'm7', etiqueta: '7', zonaId: 'z-salon', capacidad: 2, qrToken: tokensQrDemo[6]),
      Mesa(id: 'm8', etiqueta: '8', zonaId: 'z-salon', capacidad: 8, qrToken: tokensQrDemo[7]),
      Mesa(id: 'm9', etiqueta: 'T1', zonaId: 'z-terraza', capacidad: 4, qrToken: tokensQrDemo[8]),
      Mesa(id: 'm10', etiqueta: 'T2', zonaId: 'z-terraza', capacidad: 4, qrToken: tokensQrDemo[9]),
      Mesa(id: 'm11', etiqueta: 'T3', zonaId: 'z-terraza', capacidad: 2, qrToken: tokensQrDemo[10]),
      Mesa(id: 'm12', etiqueta: 'T4', zonaId: 'z-terraza', capacidad: 6, qrToken: tokensQrDemo[11]),
    ];

const resenasDemo = <ResenaRestaurante>[
  ResenaRestaurante(
    autor: 'Mariana (visitante de CDMX)',
    estrellas: 5,
    texto: 'El mole es de los mejores que probé en Puebla. La cemita llega rápido y el servicio es amable.',
    fecha: 'hace 3 días',
    respuesta: '¡Gracias por visitarnos! Te esperamos en la temporada de chiles en nogada.',
  ),
  ResenaRestaurante(
    autor: 'Jorge R.',
    estrellas: 4,
    texto: 'Muy buena comida corrida. Un poco lleno a la hora de comer, pero vale la espera.',
    fecha: 'hace 1 semana',
  ),
  ResenaRestaurante(
    autor: 'Ana y familia',
    estrellas: 5,
    texto: 'Escaneamos el QR de la mesa y pedimos desde el celular, muy práctico con niños.',
    fecha: 'hace 2 semanas',
  ),
  ResenaRestaurante(
    autor: 'Visitante de Monterrey',
    estrellas: 4,
    texto: 'La horchata es enorme y el pan de muerto estaba delicioso.',
    fecha: 'hace 3 semanas',
  ),
];

const _resenasCortas = <ResenaRestaurante>[
  ResenaRestaurante(autor: 'Luis M.', estrellas: 4, texto: 'Buen sabor y precios justos.', fecha: 'hace 4 días'),
  ResenaRestaurante(autor: 'Carla (turista)', estrellas: 5, texto: 'Lo recomiendo para una comida tranquila.', fecha: 'hace 2 semanas'),
];

/// Directorio del turista. El primero es el restaurante de la demo: su disponibilidad se lee del estado en vivo.
const directorioDemo = <FichaRestaurante>[
  FichaRestaurante(
    id: idRestauranteDemo,
    nombre: nombreRestauranteDemo,
    zona: 'Barrio de Analco',
    descripcion: 'Comida corrida y mole poblano hecho en casa. Pide desde tu mesa escaneando el QR.',
    horario: '9:00 a 18:00',
    tipos: {TipoCocina.tipica, TipoCocina.comidaCorrida},
    precioMedio: 18000,
    resenas: resenasDemo,
    mesasLibres: 12,
    mesasTotal: 12,
    aceptaPasaporte: true,
    esElDeLaDemo: true,
    tono: 0,
  ),
  FichaRestaurante(
    id: 'r2',
    nombre: 'Cemitas Los Portales (ejemplo)',
    zona: 'San Pedro Cholula',
    descripcion: 'Cemitas y aguas frescas a dos calles de la zona arqueológica.',
    horario: '8:00 a 17:00',
    tipos: {TipoCocina.tipica},
    precioMedio: 12000,
    resenas: _resenasCortas,
    mesasLibres: 4,
    mesasTotal: 6,
    aceptaPasaporte: true,
    tono: 1,
  ),
  FichaRestaurante(
    id: 'r3',
    nombre: 'Casa Verde Vegetal (ejemplo)',
    zona: 'Centro Histórico',
    descripcion: 'Cocina vegetal con opciones sin gluten y productos de la Sierra Norte.',
    horario: '9:00 a 21:00',
    tipos: {TipoCocina.sinGluten, TipoCocina.mixto},
    precioMedio: 21000,
    resenas: _resenasCortas,
    mesasLibres: 3,
    mesasTotal: 10,
    tono: 2,
  ),
  FichaRestaurante(
    id: 'r4',
    nombre: 'Trattoria del Zócalo (ejemplo)',
    zona: 'Centro Histórico',
    descripcion: 'Pastas frescas y horno de leña, a una cuadra de la Catedral.',
    horario: '13:00 a 23:00',
    tipos: {TipoCocina.extranjera},
    precioMedio: 32000,
    resenas: _resenasCortas,
    mesasLibres: 0,
    mesasTotal: 14,
    tono: 3,
  ),
  FichaRestaurante(
    id: 'r5',
    nombre: 'Terraza Atlixco de Verano (ejemplo)',
    zona: 'Atlixco',
    descripcion: 'Carta ligera de temporada con vista a los volcanes. Abre en vacaciones.',
    horario: 'Cerrado por temporada',
    tipos: {TipoCocina.verano, TipoCocina.mixto},
    precioMedio: 25000,
    resenas: _resenasCortas,
    abierto: false,
    tono: 4,
  ),
  FichaRestaurante(
    id: 'r6',
    nombre: 'Comedor Doña Chelo (ejemplo)',
    zona: 'Barrio del Artista',
    descripcion: 'Comida corrida de tres tiempos todos los días.',
    horario: '12:00 a 17:00',
    tipos: {TipoCocina.comidaCorrida, TipoCocina.tipica},
    precioMedio: 9000,
    resenas: _resenasCortas,
    aceptaPasaporte: true,
    tono: 5,
  ),
];

/// Menú breve de los restaurantes del directorio que no son el de la demo (solo muestra; no hay pedido).
List<Platillo> menuDeEjemplo(String restauranteId) {
  const base = <String, List<(String, String, int)>>{
    'r2': [('Cemita de milanesa', 'Con quesillo y aguacate.', 8500), ('Agua de jamaica', 'Vaso grande.', 3000)],
    'r3': [('Bowl de nopales', 'Nopales asados, frijol y salsa verde.', 12000), ('Tostada de hongos', 'Hongos de la Sierra Norte.', 9500)],
    'r4': [('Tagliatelle al ragú', 'Pasta fresca de la casa.', 22000), ('Pizza de leña margherita', 'Mozzarella y albahaca.', 19500)],
    'r5': [('Ensalada de temporada', 'Fruta, jícama y limón.', 11000)],
    'r6': [('Comida corrida', 'Sopa, guisado, arroz y postre.', 9000)],
  };
  final platos = base[restauranteId] ?? const [];
  return [
    for (var i = 0; i < platos.length; i++)
      Platillo(
        id: '$restauranteId-$i',
        nombre: platos[i].$1,
        descripcion: platos[i].$2,
        precio: platos[i].$3,
        categoriaId: 'c-fuertes',
        foto: 20 + i,
      ),
  ];
}
