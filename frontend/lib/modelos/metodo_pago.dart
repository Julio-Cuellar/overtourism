/// Formas de pago; los nombres coinciden con el enum `MetodoPago` del servidor (ventas.graphqls).
enum MetodoPago {
  efectivo('EFECTIVO', 'Efectivo'),
  tarjeta('TARJETA', 'Tarjeta'),
  transferencia('TRANSFERENCIA', 'Transferencia');

  const MetodoPago(this.codigo, this.etiqueta);

  /// Nombre en el contrato GraphQL.
  final String codigo;

  /// Texto para la persona.
  final String etiqueta;
}
