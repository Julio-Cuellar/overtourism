# Negocio 360 · app (Flutter)

Una sola app con dos modos, pensada primero para el teléfono (D-89):

- **Turista:** explorar negocios por intereses, ver la ficha, el Pasaporte de cupones y una ruta del día.
- **Negocio:** abrir turno, cobrar con cupón y forma de pago, ver las ventas y hacer el corte de caja.

**Estado (2026-10-15): maqueta funcional con datos de ejemplo.** Todo funciona dentro del teléfono y cada pantalla
rotula sus datos como «Datos de ejemplo». Todavía no habla con el servidor.

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
| `lib/pantallas/turista` | Explorar, ficha del negocio, Pasaporte y ruta |
| `lib/pantallas/negocio` | Abrir turno, cobrar, confirmar cobro, ventas y corte |

## Siguiente paso: conectar con el servidor

El servidor ya expone Ventas y Caja por GraphQL en `http://<servidor>:8080/graphql` con el perfil `desarrollo`
(ver `backend/src/main/resources/graphql/*.graphqls`):

1. Iniciar sesión por REST: `POST /auth/codigo` y `POST /auth/sesion`; guardar el token.
2. Mandar cada petición GraphQL con `Authorization: Bearer <token>`.
3. Cobrar = `prepararComando(input: {registrarVenta: …})` → mostrar el `resumen` → `confirmarComando(token)`.
4. Reemplazar `CajaDemo` por un servicio que use `saldoTurno`, `turnoActivo` y los comandos de Caja.

En el emulador de Android el servidor de la laptop es `http://10.0.2.2:8080`. El dinero viaja como texto
(`"240.00"`): usar `centavosDe` y `decimalDe` de `lib/modelos/dinero.dart`.
