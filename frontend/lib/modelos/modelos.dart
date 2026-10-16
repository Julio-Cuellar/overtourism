/// Modelos de la maqueta. Son el mismo contrato que el servidor expone por GraphQL (ventas.graphqls, caja.graphqls y
/// canal.graphqls), con el dinero en centavos enteros.
library;

/// Producto que se vende. `precio` en centavos.
class Producto {
  const Producto({
    required this.id,
    required this.descripcion,
    required this.unidad,
    required this.precio,
    this.admiteFraccion = false,
  });

  final String id;
  final String descripcion;
  final String unidad;
  final int precio;

  /// Verdadero si se vende a granel (por medio kilo en la maqueta, VT-03).
  final bool admiteFraccion;
}

/// Cupon del Pasaporte de monto fijo. `descuento` en centavos.
class Cupon {
  const Cupon({required this.codigo, required this.descuento, required this.descripcion});

  final String codigo;
  final int descuento;
  final String descripcion;
}

/// Intereses del turista para filtrar (cuestionario de intereses del diseno).
enum Interes {
  gastronomia('Gastronomía'),
  culturaYArte('Cultura y arte'),
  artesanias('Artesanías'),
  puebloMagico('Pueblo Mágico');

  const Interes(this.etiqueta);

  final String etiqueta;
}

/// Negocio o atractivo que ve el turista.
class Negocio {
  const Negocio({
    required this.id,
    required this.nombre,
    required this.giro,
    required this.zona,
    required this.descripcion,
    required this.horario,
    required this.intereses,
    this.cupon,
  });

  final String id;
  final String nombre;
  final String giro;
  final String zona;
  final String descripcion;
  final String horario;
  final Set<Interes> intereses;

  /// Cupon del Pasaporte que acepta, si tiene.
  final Cupon? cupon;
}
