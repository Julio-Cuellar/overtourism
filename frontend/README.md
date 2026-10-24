# Negocio 360 · app (Flutter)

Una sola app con varios modos, pensada primero para el teléfono (D-89) y adaptada a tableta y web:

- **Turista:** explorar negocios por intereses, ver restaurantes con mesas libres, el Pasaporte de cupones y una ruta del día.
- **Estoy en una mesa (comensal):** escanear el QR de la mesa, ver la carta, pedir directo a cocina y seguir el pedido, sin cuenta.
- **Mi restaurante (personal):** panel, menú y recetas con costo y margen, salón y cuentas, cocina y barra, insumos y ajustes. El puesto (Dueño, Mesero, Cocinero, Cajero) se elige arriba para ver cómo cambian los permisos.
- **Negocio:** abrir turno, cobrar con cupón y forma de pago, ver las ventas y hacer el corte de caja.

**Estado (2026-10-16): maqueta funcional con datos de ejemplo.** Todo funciona dentro del dispositivo y cada pantalla
rotula sus datos como «Datos de ejemplo». Todavía no habla con el servidor. La maqueta de Restaurantes sigue el diseño
`Módulo Restaurantes v0.5` y no sustituye al módulo del servidor, que no está autorizado.

## Correr

```
cd frontend
flutter pub get
flutter run            # en el emulador o teléfono conectado
flutter run -d chrome  # en el navegador
flutter test           # pruebas
```

## Estructura

| Carpeta | Qué hay |
|---|---|
| `lib/modelos` | Producto, cupón, negocio, método de pago y `dinero.dart` (todo el dinero en centavos `int`, nunca `double`) |
| `lib/datos/demo.dart` | Datos de ejemplo: los mismos productos y cupones que el servidor sirve en `productosSimulados` y `cuponesSimulados`; negocios ficticios |
| `lib/estado` | `Carrito` y `CajaDemo` (caja simulada con las reglas CJ-01 a CJ-12) |
| `lib/pantallas/turista` | Explorar, ficha del negocio, directorio y ficha de restaurantes, Pasaporte y ruta |
| `lib/pantallas/negocio` | Abrir turno, cobrar, confirmar cobro, ventas y corte |
| `lib/pantallas/restaurantes` | Panel, menú, receta, salón, cuenta, QR de mesa, cocina, insumos y ajustes |
| `lib/pantallas/comensal` | Escáner de QR simulado y la página del comensal (carta, pedido, reseñas) |
| `lib/estado/restaurante_demo.dart` | Restaurante simulado con las reglas del diseño (MN, RC, CM, MS, CT, CD y QR) |
| `lib/widgets` | Navegación adaptable (teléfono, tableta, escritorio), componentes y carta pública |
| `lib/tema.dart` | Paleta de nueve colores, formas (redondeado suave y sellos afilados) y tema claro y oscuro |

## Diseño y adaptación

Paleta: `#FFFFFF`, `#002F6C`, `#4A90E2`, `#0A192F`, `#FDFBF7`, `#3D2314`, `#E65F00`, `#1A4314` y `#8B2520`; los tonos suaves se
mezclan con blanco o crema. Verde = libre o listo, naranja = ocupado o en preparación, rojo = agotado o cancelado, azul
cielo = información. Tarjetas de 14 px, controles de 10 px y sellos de estado casi rectos (3 px).

Menos de 720 px de ancho: una columna y barra inferior. De 720 a 1120 px: riel lateral. Más de 1120 px: riel extendido y
vistas maestro-detalle (menú, salón con cuenta, cocina en tres columnas, carta con pedido al lado). La web se construye
con `flutter build web`.

## Siguiente paso: conectar con el servidor

El servidor ya expone Ventas y Caja por GraphQL en `http://<servidor>:8080/graphql` con el perfil `desarrollo`
(ver `backend/src/main/resources/graphql/*.graphqls`):

1. Iniciar sesión por REST: `POST /auth/codigo` y `POST /auth/sesion`; guardar el token.
2. Mandar cada petición GraphQL con `Authorization: Bearer <token>`.
3. Cobrar = `prepararComando(input: {registrarVenta: …})` → mostrar el `resumen` → `confirmarComando(token)`.
4. Reemplazar `CajaDemo` por un servicio que use `saldoTurno`, `turnoActivo` y los comandos de Caja.

En el emulador de Android el servidor de la laptop es `http://10.0.2.2:8080`. El dinero viaja como texto
(`"240.00"`): usar `centavosDe` y `decimalDe` de `lib/modelos/dinero.dart`.
