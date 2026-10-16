/// Permisos y roles de Restaurantes (sección 9 de `docs/diseno/restaurantes.md`). En la maqueta el rol se elige en la
/// pantalla para ver cómo cambia lo que cada puesto puede hacer; con el servidor, el permiso sale de la sesión de
/// Identidad y la app solo oculta lo que no se puede usar (el servidor es quien niega).
library;

enum Permiso {
  menuVer('restaurantes.menu.ver'),
  menuEditar('restaurantes.menu.editar'),
  menuAgotar('restaurantes.menu.agotar'),
  recetaVer('restaurantes.receta.ver'),
  recetaEditar('restaurantes.receta.editar'),
  costoVer('restaurantes.costo.ver'),
  salonVer('restaurantes.salon.ver'),
  servicioOperar('restaurantes.servicio.operar'),
  salonConfigurar('restaurantes.salon.configurar'),
  cuentaAbrir('restaurantes.cuenta.abrir'),
  lineaAnular('restaurantes.linea.anular'),
  lineaAnularPreparada('restaurantes.linea.anular_preparada'),
  cuentaCancelar('restaurantes.cuenta.cancelar'),
  comandaPreparar('restaurantes.comanda.preparar'),
  comandaEntregar('restaurantes.comanda.entregar'),
  cobrar('ventas.venta.registrar'),
  inventarioVer('inventario.articulo.ver');

  const Permiso(this.codigo);

  final String codigo;
}

enum RolRestaurante {
  dueno('Dueño', 'Ve y hace todo, incluido el costo y el margen'),
  mesero('Mesero', 'Abre cuentas, ordena, cancela con motivo y entrega'),
  cocinero('Cocinero', 'Ve las dos estaciones y marca platillos agotados'),
  cajero('Cajero', 'Cobra las cuentas por cobrar');

  const RolRestaurante(this.etiqueta, this.descripcion);

  final String etiqueta;
  final String descripcion;

  Set<Permiso> get permisos => switch (this) {
        RolRestaurante.dueno => Permiso.values.toSet(),
        RolRestaurante.mesero => {
            Permiso.menuVer,
            Permiso.salonVer,
            Permiso.cuentaAbrir,
            Permiso.lineaAnular,
            Permiso.cuentaCancelar,
            Permiso.comandaEntregar,
          },
        RolRestaurante.cocinero => {
            Permiso.menuVer,
            Permiso.menuAgotar,
            Permiso.recetaVer,
            Permiso.comandaPreparar,
            Permiso.comandaEntregar,
            Permiso.inventarioVer,
            Permiso.salonVer,
          },
        RolRestaurante.cajero => {
            Permiso.menuVer,
            Permiso.salonVer,
            Permiso.cobrar,
          },
      };

  bool puede(Permiso permiso) => permisos.contains(permiso);
}
