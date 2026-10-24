# Guía para integrar el frontend con la API (Identidad + GraphQL)

**Fecha:** 2026-10-16 · **Estado:** vigente para el perfil `desarrollo`. Todo dato de ejemplo es **simulado**.
**Contratos de fondo:** `docs/api/identidad.openapi.yaml` (REST de Identidad) y `docs/planes/integracion-ventas-caja.md`, sección 7 (esquema GraphQL de Ventas y Caja).

## 1. Cómo levantar el backend

```bash
cd backend
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
docker compose up -d                      # PostgreSQL en el puerto 5433
mvn spring-boot:run -Dspring-boot.run.profiles=desarrollo
```

- Sale en `http://localhost:8080` (agrega `-Dspring-boot.run.arguments=--server.port=8091` si el 8080 está ocupado).
- Con `desarrollo` se encienden **Ventas, Caja e Inventario en memoria** (los datos se pierden al reiniciar) y **seis productos simulados** rotulados `[SIMULADO]`. Con el perfil `produccion` esos tres módulos **no** se encienden todavía (falta su persistencia).
- Los códigos de acceso no se envían: se leen en `GET /dev/canal/codigo?correo=...` (solo en desarrollo).

**Colección de Postman:** `backend/postman/negocio360.postman_collection.json` con su entorno `negocio360.local.postman_environment.json` prueba todos los módulos en orden (carpetas 0 a 5, 77 peticiones, cada escritura en sus dos pasos). Requiere el perfil `desarrollo`.

## 2. Una sola puerta: sesión de Identidad

Todo (REST y GraphQL) usa el mismo token: `Authorization: Bearer <token>`. El **negocio, el actor y los permisos salen de la sesión**; ninguna operación recibe `negocioId`.

| Paso | Llamada |
|---|---|
| 1. Registro | `POST /registro` con `{ nombre, apellidos, correo, telefono, aceptaAviso: true, negocio: { nombre, giro } }` → `202` |
| 2. Código | `GET /dev/canal/codigo?correo=...` (desarrollo) |
| 3. Verificar | `POST /registro/verificacion` con `{ correo, codigo }` → `201` con `token` |
| 4. Perfil | `GET /auth/yo` → cuenta, `negocioActivo`, `rol`, `permisos` |
| Volver a entrar | `POST /auth/codigo` y `POST /auth/sesion` |
| Cambiar de negocio | `PUT /auth/negocio-activo` (devuelve un token nuevo) |

Con un negocio en el registro, el dueño queda con el negocio activo en unos instantes (Negocios responde por el bus de eventos); revisa `/auth/yo` hasta que `negocioActivo` traiga valor.

**Roles y permisos** (los define Identidad, `GET /roles` y `GET /permisos`): dueño y administrador lo ven todo; el gerente casi todo; el cajero y el taquillero cobran y operan su caja; el cocinero ve existencias y registra salidas y mermas; mesero y guía no cobran. Usa `permisos` de `/auth/yo` para mostrar u ocultar botones; **el servidor los vuelve a comprobar**.

## 3. GraphQL

`POST /graphql` con `{ "query": "...", "variables": { ... } }`. Sin sesión, `NO_AUTENTICADO`; con sesión sin negocio, `SIN_NEGOCIO_ACTIVO`; sin permiso, `PERMISO_DENEGADO` (no dice cuál faltó).

### 3.1 Escrituras: preparar → confirmar

Toda escritura son dos llamadas, iguales para **todos** los módulos:

```graphql
mutation($i: ComandoInput!) { prepararComando(input: $i) { token resumen expiraEn advertencias } }
mutation($t: String!)       { confirmarComando(token: $t) { ok codigo mensajePlantilla recursoId datos { clave valor } } }
```

- `ComandoInput` acepta **exactamente una** variante (por ejemplo `{ "registrarVenta": { ... } }`).
- Muestra `resumen` a la persona; el `token` es de un solo uso y vence a los 5 minutos. Va ligado a su cuenta y su negocio.
- El token lleva el prefijo de su módulo (`ventas.`, `caja.`, `clientes.`, `inventario.`); los de reseñas del turista no llevan prefijo. Trátalo como texto opaco.
- `datos` trae lo que calculó el servidor (folio, total, cambio…); el dinero siempre llega como texto decimal exacto (`"150.00"`).

| Módulo | Variantes de `ComandoInput` |
|---|---|
| Ventas | `registrarVenta`, `anularVenta` |
| Caja | `crearCaja`, `abrirTurno`, `registrarMovimientoCaja`, `cerrarTurno`, `descartarCorte` |
| Clientes | `registrarCliente`, `editarCliente`, `publicarAvisoPrivacidad` (las demás, `COMANDO_NO_DISPONIBLE`) |
| Inventario | `crearArticulo`, `editarArticulo`, `definirUnidadPropia`, `bajaArticulo`, `registrarEntradaInventario`, `registrarSalidaInventario`, `registrarMermaInventario`, `ajustarPorConteo` |
| Negocios (turista) | `publicarResena` (sin cuenta, con el encabezado `X-Dispositivo-Id`) |

### 3.2 Consultas

| Módulo | Consulta | Permiso |
|---|---|---|
| Ventas | `venta(id)`, `ventas(filtro, primeros, despues)`, `resumenTurno(turnoId)` | `ventas.venta.ver` |
| Ventas | `productosParaVenta(texto, primeros)` | `ventas.venta.registrar` |
| Caja | `cajas`, `turnoActivo(cajaId)`, `saldoTurno(turnoId)` (con `movimientos(primeros, despues)`) | `caja.turno.ver` |
| Caja | `cortes(filtro, primeros, despues)` | `caja.corte.ver` |
| Clientes | `sugerirClientes(texto)` (2 a 80 caracteres), `avisoPrivacidadVigente` | `clientes.sugerir`, `clientes.cliente.registrar` |
| Inventario | `inventario(texto, soloAlertas, primeros, despues)`, `articulo(id)`, `kardex(articuloId, primeros, despues)` | `inventario.articulo.ver` |
| Negocios | `miNegocio`, `sucursales`, `resenas`… y lo público del turista | ver `negocios.graphqls` |

**Listas con cursor:** máximo 100 por página, 20 por omisión; el cursor (`pageInfo.cursorFinal`) es opaco. En Ventas y Caja, un tamaño fuera de rango o un cursor inventado responde `PAGINACION_INVALIDA`; en Clientes e Inventario, `CONSULTA_INVALIDA`.

### 3.3 Errores

Todo error trae `errors[0].extensions.codigo` con un código estable y `message` con un texto fijo en español de México; guíate por el código. Los errores de dominio (`PAGOS_NO_CUADRAN`, `TURNO_NO_ABIERTO`, `CUPON_INVALIDO`, `ARTICULO_NO_ENCONTRADO`, `TELEFONO_DUPLICADO`…) traen además `regla` y, si procede, `recursoId`; `CONCURRENCIA` y `ERROR_TEMPORAL` traen `reintentable: true`. Los de la plataforma: `NO_AUTENTICADO`, `SIN_NEGOCIO_ACTIVO`, `PERMISO_DENEGADO`, `TOKEN_INVALIDO`, `COMANDO_INVALIDO`, `COMANDO_NO_DISPONIBLE`, `OPERACION_NO_DISPONIBLE`, `CONSULTA_INVALIDA`, `CONSULTA_MUY_PROFUNDA`, `ERROR_INTERNO` (con `referencia`).

## 4. Ejemplo: cobrar una venta

```jsonc
// 1) abrir turno (después de crearCaja)
{ "abrirTurno": { "cajaId": "…", "fondoInicial": "300.00" } }

// 2) productos que se pueden vender
{ productosParaVenta(texto: "taco", primeros: 5) { productoId descripcion unidad precio admiteFraccion } }

// 3) cobrar: el precio lo pone el servidor; los pagos deben sumar el total
{ "registrarVenta": { "turnoId": "…",
    "lineas": [ { "productoId": "…", "cantidad": "3" }, { "productoId": "…", "cantidad": "0.5" } ],
    "pagos":  [ { "metodo": "EFECTIVO", "monto": "66.00", "recibido": "100.00" } ] } }
// → datos: cambio 34.00, folio 1, total 66.00, venta_id

// 4) anular (solo gerente, administrador y dueño)
{ "anularVenta": { "ventaId": "…", "motivo": "Cobro duplicado" } }
```

## 5. Lo que todavía no está (para no esperar de más)

| Pendiente | Efecto en el frontend |
|---|---|
| Persistencia en PostgreSQL de Ventas, Caja e Inventario (F6 del plan) | Los datos de estos módulos se pierden al reiniciar el backend |
| Productos reales (Misceláneos y Restaurantes, pistas B y C) | Mientras tanto salen seis productos `[SIMULADO]`; todo cupón responde `CUPON_INVALIDO` (no existe Pasaporte) |
| Historial de compras, consentimientos y ARCO de Clientes (CLI-I3 a I5) | `clientes`, `cliente`, `historialCliente`, `panoramaClientes`, `consentimientosCliente` y `solicitudesArco` responden `OPERACION_NO_DISPONIBLE`; para ligar un cliente a una venta usa `sugerirClientes` |
| Venta → Inventario y venta → Clientes por eventos | Cobrar no descuenta inventario ni suma al historial del cliente todavía; Caja sí recibe la venta |
| CORS para Flutter **web** | La app móvil no lo necesita; un navegador sí. Hay que decidir el origen permitido (afecta a SG-09 y SG-14) |
| Confirmación reforzada (IDN-3.2) | `publicarAvisoPrivacidad` responde `CONFIRMACION_REQUERIDA` porque es un permiso sensible |
