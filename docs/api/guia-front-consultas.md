# Guía de consultas para el front (Flutter) mientras el back avanza

**Fecha:** 2026-10-24 · **Versión 2** (medida sobre `origin/julio` en `f97c0ad`, que ya trae Restaurantes P1a). **Estado:** PROPUESTA hasta el hito H-C (contrato congelado). Lo marcado **VIGENTE** se verificó en el código; lo marcado **PROPUESTO** todavía no existe en el servidor. Restaurantes y Ventas/Caja/Inventario corren **en memoria** en el perfil `desarrollo` (los datos se pierden al reiniciar).
**Complementa:** [`graphql-integracion-front.md`](graphql-integracion-front.md) (cómo levantar el backend y probar lo existente) y [`../planes/horizontales-para-restaurantes.md`](../planes/horizontales-para-restaurantes.md) (el plan del back, con las tareas `HZ-xx` que se mencionan aquí).
**Todo dato de ejemplo es simulado.**

---

## 1. Resumen en diez líneas

1. **Un solo endpoint** `POST /graphql` para consultas y escrituras, más `ws://…/graphql` para suscripciones, más unos pocos **REST** de Identidad (`/auth/*`, `/registro/*`).
2. **Una sola credencial:** `Authorization: Bearer <token>`. El negocio, el actor y los permisos salen de la sesión. **Ninguna operación recibe `negocioId`** (salvo las públicas del turista, donde el negocio es el objeto consultado).
3. **Toda escritura son dos llamadas:** `prepararComando` (devuelve un resumen y un token) y `confirmarComando` (lo gasta). Excepción: unas cuantas mutaciones directas de Negocios y las futuras de Restaurantes.
4. **El dinero es texto decimal** (`"150.00"`), nunca número. En Dart se convierte a centavos enteros con `lib/modelos/dinero.dart`.
5. **Los errores traen un código estable** en `errors[0].extensions.codigo`; el texto es fijo en español de México. Guíate por el código.
6. **La UI se arma con `permisos`** de `GET /auth/yo`; el servidor vuelve a comprobarlos.
7. **Las listas usan cursor opaco** (`primeros`, `despues`, `pageInfo.cursorFinal`).
8. **Las suscripciones solo empujan cambios;** siempre hay una consulta de respaldo.
9. **Para trabajar en paralelo:** pon una capa de servicios entre las pantallas y la red, con una implementación `Demo` (la que ya tienes) y una `Remoto`, y elige por módulo con `--dart-define`.
10. **Los cambios que vienen son aditivos** (campos opcionales y operaciones nuevas): lo que programes hoy contra lo VIGENTE no se rompe.

## 2. Qué puedes conectar hoy y qué no

| Función del Flutter | Estado en el servidor | Qué hacer ahora |
|---|---|---|
| Registro, código, sesión, perfil, cambio de negocio | **VIGENTE** (REST) | Conectar ya |
| Caja: crear caja, abrir turno, movimientos, corte | **VIGENTE** (GraphQL) | Conectar ya |
| Ventas: cobrar, anular, listar, resumen de turno | **VIGENTE** | Conectar ya |
| Inventario: artículos, entradas, salidas, mermas, conteo, kardex | **VIGENTE** | Conectar ya |
| Negocios: panel del locatario, ficha pública, reseñas | **VIGENTE** | Conectar ya |
| Clientes: `sugerirClientes`, aviso de privacidad, registrar y editar | **VIGENTE** | Conectar ya |
| Clientes: historial, panorama, consentimientos, ARCO | Responden `OPERACION_NO_DISPONIBLE` | Mantener demo |
| Cobro de una cuenta de restaurante con `referencia` | **PROPUESTO** (HZ-20) | Escribir contra el contrato; probar con demo |
| Mesas libres reales en la ficha del turista | **PROPUESTO** (HZ-26) | Campos opcionales; hoy solo `nivel` |
| Front web (Flutter web) | **Bloqueado** hasta HZ-21 (CORS) | Usar móvil o emulador; ver sección 3.2 |
| Restaurantes P1a: menú, categorías, platillos, recetas con costo y margen, consumos fallidos, `menuPublico` | **VIGENTE** en memoria: 8 consultas y 8 mutaciones en `restaurantes.graphqls` | Conectar ya (sección 8); la paginación cambiará de forma (D-213) |
| Restaurantes: `guardarReceta` y `darDeBajaPlatillo` | **En el esquema** como variantes de `ComandoInput` (HZ-22, **solo en el disco local, sin subir**); el contrato está en la sección 5.3 | Escribir contra el contrato; con el servidor de `julio` actual todavía no existen |
| Vender un platillo con Ventas | Restaurantes ya implementa `FuenteDeProductos` (HZ-23, **local, sin subir**), así que Ventas los verá en `productosParaVenta` de un restaurante. Nunca se arrancó la aplicación completa con los tres módulos | Seguir con demo hasta probarlo |
| Restaurantes P1b: salón, cuentas, comandas, QR | **Sin código ni autorización** | Seguir con `RestauranteDemo` |
| Pedir por QR con `Idempotency-Key` | **PROPUESTO** (HZ-24, HZ-27) | Preparar el cliente; probar con demo |
| Panel: «lo más vendido», margen real | No existe en P1 (`analisisDePlatillos` es P2) | Mantener datos de ejemplo rotulados |

## 3. Transporte

### 3.1 URLs

| Dónde corre la app | URL base del servidor (`desarrollo`, puerto 8080) |
|---|---|
| Emulador de Android | `http://10.0.2.2:8080` |
| Simulador de iOS | `http://localhost:8080` |
| Teléfono físico | `http://<IP de tu laptop>:8080` (misma red) |
| Flutter web | **No funciona todavía:** ver 3.2 |

GraphQL: `POST {base}/graphql`. WebSocket: `ws://{host}:8080/graphql` (mismo camino). En producción serán `https` y `wss`.

### 3.2 Flutter web y CORS (hallazgo)

`PeticionGraphqlEstrictaFilter` ya compara la cabecera `Origin` con `seguridad.http.origenes-permitidos` (lista exacta, **sin comodines**). Pero **no existe ninguna cabecera `Access-Control-Allow-*` ni respuesta al preflight `OPTIONS`**, así que un navegador falla antes de llegar a GraphQL aunque el origen esté en la lista. La tarea `HZ-21` lo resuelve (D-211). Mientras tanto:

- Prueba en emulador o teléfono (los clientes nativos no mandan `Origin` y no necesitan CORS).
- Cuando exista HZ-21, corre la web con puerto fijo: `flutter run -d chrome --web-port=5000` y pide agregar `http://localhost:5000` a `seguridad.http.origenes-permitidos`.

### 3.3 Encabezados

| Encabezado | Cuándo | Valor |
|---|---|---|
| `Content-Type` | Siempre en `/graphql` | `application/json; charset=utf-8` (solo JSON UTF-8, un solo objeto, **sin lotes**) |
| `Authorization` | Toda operación con sesión | `Bearer <token>` |
| `X-Dispositivo-Id` | Operaciones públicas de escritura del turista (hoy `publicarResena`) | Un UUID que la app genera una vez y guarda en el dispositivo |
| `Idempotency-Key` | **PROPUESTO** (D-208): en las operaciones que lo declaren | Un UUID nuevo por intento de la persona; se reusa solo al reintentar la misma acción |

Límites vigentes: cuerpo de hasta 64 KB; tasa por cuenta y por IP (respuesta `429` con `Retry-After`); profundidad máxima de consulta (`CONSULTA_MUY_PROFUNDA`).

## 4. Autenticación (REST)

Contratos completos: [`identidad.openapi.yaml`](identidad.openapi.yaml).

| Paso | Llamada | Notas |
|---|---|---|
| Registro | `POST /registro` con `{ nombre, apellidos, correo, telefono, aceptaAviso: true, negocio: { nombre, giro } }` → `202` | `giro` ∈ `RESTAURANTE`, `MISCELANEA`, `MUSEO` |
| Código (desarrollo) | `GET /dev/canal/codigo?correo=…` | Solo con el perfil `desarrollo`; en producción llega por SMS o mensajería |
| Verificar | `POST /registro/verificacion` con `{ correo, codigo }` → `201` con `token` | |
| Volver a entrar | `POST /auth/codigo` con `{ identificador }` y luego `POST /auth/sesion` con `{ identificador, codigo }` | Devuelve `{ token, expiraEn, cuenta, negocioActivo, … }` |
| Perfil | `GET /auth/yo` | `cuenta`, `negocioActivo`, `rol`, `permisos[]` |
| Cambiar de negocio | `PUT /auth/negocio-activo` con `{ negocioId }` | Devuelve un token nuevo; reemplaza el anterior |
| Cerrar sesión | `DELETE /auth/sesion` (esta) o `DELETE /auth/sesiones` (todas) | |
| Catálogos | `GET /roles`, `GET /permisos` | Para pintar nombres de roles |

Reglas para el cliente:

- Guarda el token en **almacenamiento seguro** (`flutter_secure_storage`), no en preferencias normales. Guarda también `expiraEn`.
- Tras registrarse con negocio, el dueño queda con el negocio activo **unos instantes después** (Negocios responde por el bus): consulta `/auth/yo` cada 1 o 2 segundos hasta que `negocioActivo` traiga valor, o usa la suscripción `altaDeNegocio` (sección 6).
- Un `401` o un `NO_AUTENTICADO` significa «vuelve a iniciar sesión»: borra el token y manda al acceso.
- La app del turista y del comensal **no tienen sesión**: llaman solo a las operaciones públicas (sección 5.5).

## 5. GraphQL

### 5.1 Una petición

```http
POST /graphql
Authorization: Bearer <token>
Content-Type: application/json; charset=utf-8

{ "query": "query($t:String){ productosParaVenta(texto:$t, primeros:5){ productoId descripcion precio } }",
  "variables": { "t": "taco" } }
```

### 5.2 Cómo leer una respuesta

Hay **dos niveles de error** y el cliente debe manejar ambos:

| Nivel | Cómo se ve | Ejemplos |
|---|---|---|
| **HTTP, antes de GraphQL** (la puerta de entrada) | Estado distinto de 200; cuerpo `{"codigo":"…"}` cuando lo trae | `401` sin sesión, `403` con `ORIGEN_NO_PERMITIDO`, `405 METODO_NO_PERMITIDO`, `413 CUERPO_MUY_GRANDE`, `415 TIPO_NO_SOPORTADO`, `400 PETICION_INVALIDA`, `429` con `Retry-After` |
| **GraphQL** | Estado `200` con `errors[0].extensions.codigo` | Todos los de abajo |

Códigos de plataforma (**VIGENTE**): `NO_AUTENTICADO`, `SIN_NEGOCIO_ACTIVO`, `PERMISO_DENEGADO` (no dice cuál faltó), `TOKEN_INVALIDO`, `TOKEN_USADO`, `TOKEN_EXPIRADO`, `COMANDO_INVALIDO`, `COMANDO_NO_DISPONIBLE`, `OPERACION_NO_DISPONIBLE`, `CONSULTA_INVALIDA`, `CONSULTA_MUY_PROFUNDA`, `PAGINACION_INVALIDA`, `ERROR_INTERNO` (con `referencia` para soporte). De dominio: `PAGOS_NO_CUADRAN`, `TURNO_NO_ABIERTO`, `CUPON_INVALIDO`, `ARTICULO_NO_ENCONTRADO`, `TELEFONO_DUPLICADO`, etc., con `regla` y, si procede, `recursoId`. `CONCURRENCIA` y `ERROR_TEMPORAL` traen `reintentable: true`.

**PROPUESTO** (se agregan con HZ-24 y HZ-27): `IDEMPOTENCIA_CONFLICTO`, `TASA_EXCEDIDA`; y los de Restaurantes del diseño §7 (`PLATILLO_AGOTADO`, `MESA_OCUPADA`, `CUENTA_SOLICITADA`, `QR_INVALIDO`, …).

Regla práctica: **reintenta solo si `reintentable` es `true`**, con espera creciente. Nunca reintentes un error de dominio.

### 5.3 Escrituras: preparar → confirmar (VIGENTE)

```graphql
mutation($i: ComandoInput!) { prepararComando(input: $i) { token resumen expiraEn advertencias } }
mutation($t: String!)       { confirmarComando(token: $t) { ok codigo mensajePlantilla recursoId datos { clave valor } } }
```

Flujo de la pantalla:

1. Arma la variante: `{ "registrarVenta": { … } }` (exactamente **una** por llamada).
2. `prepararComando` → muestra `resumen` y `advertencias` a la persona.
3. La persona confirma → `confirmarComando(token)`.
4. Lee `datos` (folio, total, cambio…); el servidor calculó todo, **no recalcules dinero en el cliente**.

Reglas del token: de un solo uso, vence a los 5 minutos, ligado a su cuenta y negocio, con prefijo de módulo (`ventas.`, `caja.`, `clientes.`, `inventario.`): trátalo como texto opaco. Si se revoca un permiso entre los dos pasos, `confirmarComando` responde `PERMISO_DENEGADO` **sin gastar el token**.

**Si se corta la red al confirmar:** no vuelvas a preparar. Consulta el estado (por ejemplo `ventas(filtro: { turnoId })`) para saber si se registró; si el token ya se usó, repetirlo responde `TOKEN_USADO`, `TOKEN_EXPIRADO` o `TOKEN_INVALIDO` según el módulo.

Variantes de `ComandoInput` **VIGENTES**:

| Módulo | Variantes |
|---|---|
| Ventas | `registrarVenta`, `anularVenta` |
| Caja | `crearCaja`, `abrirTurno`, `registrarMovimientoCaja`, `cerrarTurno`, `descartarCorte` |
| Clientes | `registrarCliente`, `editarCliente`, `publicarAvisoPrivacidad` |
| Inventario | `crearArticulo`, `editarArticulo`, `definirUnidadPropia`, `bajaArticulo`, `registrarEntradaInventario`, `registrarSalidaInventario`, `registrarMermaInventario`, `ajustarPorConteo` |
| Negocios (turista, sin cuenta) | `publicarResena` |

**Restaurantes (HZ-22, local, sin subir):** `guardarReceta` y `darDeBajaPlatillo`, con prefijo de token `restaurantes.`. Piden `restaurantes.receta.editar` y `restaurantes.menu.editar` al preparar y otra vez al confirmar (si se quita el permiso entre los dos pasos, `PERMISO_DENEGADO` y el token no se gasta). Después, en P1b, `anularLineaPreparada` y `cancelarCuentaPreparada`.

```graphql
input RestGuardarRecetaInput {
  platilloId: ID!
  rendimiento: Int! = 1           # porciones, de 1 a 500
  insumos: [RestRecetaInsumoInput!]!   # de 1 a 30, sin repetir artículo
}
input RestRecetaInsumoInput {
  articuloId: ID!
  cantidad: Decimal!
  unidad: String!                 # se convierte a la unidad base del artículo
  mermaPct: Decimal!              # 0 a 100; mándalo siempre ("0" si no hay)
}
input RestBajaPlatilloInput { platilloId: ID! }
```

```json
{ "guardarReceta": { "platilloId": "…", "rendimiento": 4,
    "insumos": [ { "articuloId": "…", "cantidad": "500", "unidad": "g", "mermaPct": "10" } ] } }
```

Los nombres llevan el prefijo `Rest` (D-130) y difieren de los de la sección 8.3 del diseño (`GuardarRecetaInput`, `RecetaInsumoInput`, `BajaPlatilloInput`).

### 5.4 Consultas VIGENTES

| Módulo | Consulta | Permiso |
|---|---|---|
| Ventas | `venta(id)`, `ventas(filtro, primeros, despues)`, `resumenTurno(turnoId)` | `ventas.venta.ver` |
| Ventas | `productosParaVenta(texto, primeros)` | `ventas.venta.registrar` |
| Caja | `cajas`, `turnoActivo(cajaId)`, `saldoTurno(turnoId)` | `caja.turno.ver` |
| Caja | `cortes(filtro, primeros, despues)` | `caja.corte.ver` |
| Inventario | `inventario(texto, soloAlertas, primeros, despues)`, `articulo(id)`, `kardex(articuloId, primeros, despues)` | `inventario.articulo.ver` |
| Clientes | `sugerirClientes(texto)`, `avisoPrivacidadVigente` | `clientes.sugerir`, `clientes.cliente.registrar` |
| Restaurantes (**en memoria**) | `categoriasDeMenu`, `platillos(filtro, primeros, despues)`, `platillo(id)`, `resumenDeMenu` | `restaurantes.menu.ver` |
| Restaurantes | `receta(platilloId)` (sin cifras de costo) / `costoDeReceta(platilloId)` | `restaurantes.receta.ver` / `restaurantes.costo.ver` |
| Restaurantes | `consumosFallidos(primeros, despues)` | `restaurantes.consumo.ver` |
| Negocios (panel) | `miNegocio`, `sucursales`, `horarioDeSucursal(sucursalId)`, `misAltasDeNegocio`, `resenas(filtro, primeros, despues)` | permisos de Negocios |
| Negocios (público, **sin sesión**) | `negociosPublicos(filtro, primeros, despues)`, `negocioPublico(id)`, `resenasDeNegocio(negocioId, primeros, despues)` | ninguno |

Mutaciones directas de Negocios (no pasan por comandos): `actualizarNegocio`, `definirSubcategorias`, `definirAtributos`, `definirDisponibilidad`, `publicarNegocio`, `actualizarSucursal`, `definirHorario`, `agregarExcepcionHorario`, `quitarExcepcionHorario`, `responderResena`. Llevan `version` para evitar pisar cambios ajenos (`CONCURRENCIA`).

**Listas:** máximo 100 por página (20 por omisión; 20 en lo público de Negocios); el cursor es opaco; un tamaño fuera de rango o un cursor inventado responde `PAGINACION_INVALIDA` (Ventas y Caja) o `CONSULTA_INVALIDA` (Clientes e Inventario).

### 5.5 Operaciones públicas (turista y comensal, sin sesión)

- No mandes `Authorization`. Manda `X-Dispositivo-Id` cuando escribas.
- **VIGENTE:** `negociosPublicos` (filtros: `texto`, `giro`, `subcategorias`, `dieta`, `accesibilidad`, `abiertoAhora`, `cerca`, `orden`), `negocioPublico`, `resenasDeNegocio`, y `publicarResena` por comando.
- **VIGENTE** (Restaurantes, en memoria): `menuPublico(negocioId, idioma = "es-MX")`. Responde `null` si el negocio no es un restaurante visible (publicado y activo). Solo trae los campos permitidos: sin costos, recetas ni notas.
- **PROPUESTO** (Restaurantes, P1b sin autorización): `mesaPorQr(token)`, `estadoDePedidoQr(token, dispositivoId)`, `pedirPorQr`, `pedirCuentaPorQr`. Las dos últimas **escriben sin sesión**, llevan `Idempotency-Key` y están limitadas por token, dispositivo e IP.
- El `dispositivoId` es un UUID aleatorio que la app genera la primera vez y guarda localmente; el servidor guarda solo un hash.

## 6. WebSocket (suscripciones)

**Protocolo (VIGENTE):** `graphql-transport-ws`, en `ws://{host}:8080/graphql`. Cada suscripción es una operación sobre la misma conexión.

```text
cliente → {"type":"connection_init","payload":{"Authorization":"Bearer <token>"}}
servidor → {"type":"connection_ack"}
cliente → {"id":"1","type":"subscribe","payload":{"query":"subscription { altaDeNegocio { negocioId estado motivo } }"}}
servidor → {"id":"1","type":"next","payload":{"data":{"altaDeNegocio":{ … }}}}
cliente → {"id":"1","type":"complete"}          // para cancelar
servidor/cliente → {"type":"ping"} / {"type":"pong"}
```

- El token va **en el mensaje `connection_init`**, no en una cabecera. Una sesión inválida cierra la conexión con el código **4401**.
- Máximo **5 suscripciones abiertas por cuenta** (sumando todas sus conexiones), 30 conexiones nuevas por minuto por IP y 64 KB por mensaje.
- Cada operación nueva vuelve a validar el token; una suscripción ya abierta sigue hasta que termine (límite conocido).
- **Una suscripción solo avisa.** Guarda siempre una consulta de respaldo (por ejemplo `misAltasDeNegocio`): al abrir la pantalla consulta, luego suscríbete, y si el socket se cae vuelve a consultar y reconecta con espera creciente.
- Para navegadores el `Origin` de la subida debe estar en la lista de orígenes permitidos.

**Suscripciones hoy:** solo `altaDeNegocio` (sin negocio activo permitido).

**PROPUESTAS (Restaurantes, HZ posteriores):** `comandasDeCocina(estacion)`, `salonActualizado`, `alertasRestaurante`. Todas por negocio de la sesión; ninguna cambia estado.

**No hay WebSocket público** en v1 (D-212): la ficha del turista y la página del comensal **sondean** (`negocioPublico` cada ~15 s, `estadoDePedidoQr` cada ~10 s).

> **Prueba de humo recomendada (antes de apoyarte en WebSocket):** abre una conexión con `web_socket_channel`, fija el subprotocolo `graphql-transport-ws`, manda `connection_init` y suscríbete a `altaDeNegocio`. Algunos clientes de GraphQL para Dart hablan por omisión el protocolo antiguo (`graphql-ws`/`subscriptions-transport-ws`) y no conectan con este servidor. El protocolo es corto: un cliente propio de ~80 líneas es una opción segura. No lo probé contra este servidor desde Flutter.

## 7. Cambios de contrato que vienen (todos aditivos, PROPUESTOS)

Tras el hito H-C **no se renombra nada**; solo se agregan campos opcionales y operaciones.

### 7.1 Ventas: `referencia` (HZ-20)

```graphql
input RegistrarVentaInput {
  # … campos actuales …
  "Opcional, hasta 80 caracteres. Para cobrar una cuenta de restaurante: \"restaurantes:cuenta:<id>\"."
  referencia: String
}
type Venta {
  # … campos actuales …
  referencia: String
}
```

El evento `venta.registrada` v1 la lleva como campo opcional. No cambia nada para quien no la manda.

### 7.2 Negocios: disponibilidad con mesas (HZ-26)

Hoy: `type DisponibilidadPublica { nivel nota declaradaEn }`. Se agregan campos **opcionales**:

```graphql
enum TipoDisponibilidad { MANUAL MESAS AFORO ATENCION }
type DisponibilidadPublica {
  # … nivel, nota, declaradaEn …
  tipo: TipoDisponibilidad      # nulo = MANUAL (comportamiento actual)
  libres: Int
  total: Int
  abierto: Boolean
  actualizadoEn: DateTime
}
```

Qué hacer en la UI: si `libres` y `total` vienen, muestra «4 de 12 mesas libres»; si no, muestra el `nivel` como hoy. Muestra `actualizadoEn` como «hace N minutos» y nunca pintes mesas libres sin esa marca.

### 7.3 Permisos y roles (HZ-23)

**VIGENTE desde `V037`:** `GET /auth/yo` trae `restaurantes.*` en `permisos[]` para los roles del giro `RESTAURANTE`, y existe el rol `MESERO_CAJA`. Hoy ese rol **solo tiene los permisos del mesero**: los de cobrar (`ventas.venta.registrar`, `caja.turno.ver`) llegan con una migración `V039` (HZ-24). Tu selector de puesto de la maqueta sigue siendo solo de la maqueta: con el servidor el puesto sale de la sesión. Los 18 permisos del diseño v0.5: `restaurantes.menu.ver|editar|agotar`, `.receta.ver|editar`, `.costo.ver`, `.salon.ver|configurar`, `.servicio.operar`, `.cuenta.ver|abrir|cancelar`, `.linea.anular`, `.linea.anular_preparada`, `.comanda.ver|preparar|entregar`, `.consumo.ver`.

### 7.4 `Idempotency-Key` (HZ-24)

Se manda como cabecera. El servidor la exige o la acepta solo en las operaciones que la declaren (en Restaurantes: `abrirCuenta`, `agregarLinea`, `pedirPorQr`). Misma llave y misma petición devuelven la respuesta guardada; misma llave con otro cuerpo responde `IDEMPOTENCIA_CONFLICTO`. Genera un UUID **cuando la persona toca el botón** y reúsalo solo si reintentas esa misma acción.

### 7.5 Inventario: sin cambios para el front

`registrarConsumo` y `revertirConsumo` (HZ-25) son internos; el front los ve indirectamente como movimientos `SALIDA` con causa `VENTA` en `kardex` y como cambios en `cantidadBase`.

## 8. Restaurantes: qué operación usa cada pantalla de la maqueta

Mapeo hecho a partir de los archivos de `frontend/lib/pantallas/` y de la entrada de la Bitácora del 2026-10-16; confírmalo pantalla por pantalla. Operaciones del diseño v0.5, sección 8. **Los tipos de menú y recetas ya existen** en `backend/src/main/resources/graphql/restaurantes.graphqls` (`RestPlatillo`, `RestCategoria`, `RestReceta`, `RestCostoReceta`, `RestMenuPublico`, `RestConsumoFallido`…). Los de salón, cuentas, comandas y QR (`RestCuenta`, …) **no existen** y siguen con `RestauranteDemo`.

**Dos cosas del esquema real que conviene saber antes de modelar:**

1. **Paginación:** `platillos` y `consumosFallidos` devuelven hoy `{ nodos, siguiente }`, no `pageInfo { hayMas cursorFinal }` como Ventas, Caja e Inventario. La propuesta D-213 las alinea al contrato de la plataforma; escribe el parser de listas **una sola vez** pensando en `pageInfo`.
2. **Dinero y fechas:** `precio`, `costoPorPorcion`, `margen` y `mermaPct` son `Decimal` (texto); `vigenteDesde`, `ocurridoEn` y `creadoEn` son `DateTime` UTC; `temporadaDesde`/`temporadaHasta` son texto `MM-dd`.

| Pantalla (`lib/pantallas/…`) | Consultas | Escrituras | Tiempo real |
|---|---|---|---|
| `restaurantes/modo_restaurante` | `GET /auth/yo` (rol y permisos) | — | — |
| `restaurantes/panel` | `resumenDeMenu`, `salon`, `inventario(soloAlertas: true)`, `costoDeReceta`, Ventas `ventas`/`resumenTurno` | — | `alertasRestaurante` |
| `restaurantes/menu_platillos`, `detalle_platillo`, `editor_platillo` | `categoriasDeMenu`, `platillos`, `platillo` | `crearCategoria`, `editarCategoria`, `crearPlatillo`, `editarPlatillo`, `publicarPlatillo`, `marcarAgotado`; comando `darDeBajaPlatillo` | — |
| `restaurantes/receta` | `receta`, `costoDeReceta`, `inventario(texto)` para elegir insumos | comando `guardarReceta` | — |
| `restaurantes/salon`, `mesa_qr` | `salon`, `qrDeMesa` | `crearZona`, `crearMesa`, `editarMesa`, `bloquearMesa`, `regenerarQrDeMesa`, `pausarQrDeMesa` | `salonActualizado` |
| `restaurantes/cuenta` | `cuenta`, `cuentasAbiertas`, `cuentaParaCobro`, `kardex` (insumos descontados) | `abrirCuenta`, `agregarLinea`, `quitarLineaPendiente`, `enviarACocina`, `anularLinea`, `pedirCuenta`, `reabrirCuenta`, `cancelarCuenta`; cobro: comando de Ventas `registrarVenta` con `referencia` | `salonActualizado` |
| `restaurantes/cocina` | `comandas(estacion, estados)` | `avanzarComanda`, `devolverComanda`, `marcarAgotado` | `comandasDeCocina` |
| `restaurantes/insumos` | `inventario`, `articulo`, `kardex`, `consumosFallidos` | comandos de Inventario (entrada, salida, merma, conteo) | `alertasRestaurante` |
| `restaurantes/ajustes` | `miNegocio` | `abrirServicio`, `cerrarServicio`, `habilitarQr`, `publicarNegocio` | — |
| `comensal/escaner_qr`, `comensal_qr` | `mesaPorQr`, `menuPublico`, `negocioPublico`, `resenasDeNegocio`, `estadoDePedidoQr` (sondeo ~10 s) | `pedirPorQr`, `pedirCuentaPorQr` (públicas, con `Idempotency-Key`) | — |
| `turista/restaurantes`, `resenas_restaurante` | `negociosPublicos(filtro: { giro: "RESTAURANTE" })`, `negocioPublico` (con mesas libres, sondeo ~15 s), `menuPublico`, `resenasDeNegocio` | comando `publicarResena` (con `X-Dispositivo-Id`) | — |

Lo que **no** tiene operación en P1 (déjalo con datos de ejemplo rotulados): «lo más vendido» y «margen real contra teórico» del panel (`analisisDePlatillos` es P2), y las traducciones del menú (P2).

## 9. Cómo organizar el Flutter para avanzar en paralelo

Hoy las pantallas usan directamente `CajaDemo` y `RestauranteDemo` (`ChangeNotifier`) y el proyecto no tiene dependencias de red. El objetivo es **cambiar de simulado a real módulo por módulo, sin tocar las pantallas**.

### 9.1 Capas

```text
lib/
  api/                      ← NUEVO: red y sesión (no sabe de pantallas)
    cliente_api.dart        POST /graphql, REST, encabezados, errores
    errores_api.dart        ApiError(codigo, mensaje, reintentable, regla, recursoId)
    sesion.dart             token, expiraEn, permisos (flutter_secure_storage)
    ws_cliente.dart         graphql-transport-ws
  servicios/                ← NUEVO: interfaces que ven las pantallas
    servicio_caja.dart      abstract class ServicioCaja extends Listenable { … }
    servicio_ventas.dart
    servicio_inventario.dart
    servicio_negocios.dart
    servicio_restaurante.dart
    remoto/                 una implementación por interfaz, sobre ClienteApi
  estado/                   ← lo que ya tienes: CajaDemo y RestauranteDemo pasan a implementar las interfaces
```

Reglas:

- Las pantallas dependen de las **interfaces**, no de `CajaDemo` ni de `ClienteApi`. Se inyectan con constructor o con un `InheritedWidget`.
- Lo que hoy es público en `CajaDemo` y `RestauranteDemo` es la lista de métodos de cada interfaz (ya hay cientos de líneas probadas en `restaurante_demo_test.dart`). **Extrae la interfaz de lo que las pantallas ya llaman**, no de lo que imagines.
- El dinero sigue en **centavos enteros** hacia las pantallas; la conversión de `Decimal` en texto vive solo en `servicios/remoto/` con `centavosDe` y `decimalDe`.
- Todo dato simulado conserva el rótulo «Datos de ejemplo»; las implementaciones remotas no lo ponen.

### 9.2 Elegir demo o remoto por módulo

```bash
# Todo simulado (como hoy)
flutter run

# Ventas y Caja reales; Restaurantes y turista simulados
flutter run --dart-define=API_URL=http://10.0.2.2:8080 \
            --dart-define=API_REMOTO=ventas,caja,inventario,negocios
```

`API_REMOTO` es la lista de módulos que usan la implementación `Remoto`; el resto usa `Demo`. Así pasas un módulo a real el día que su contrato esté listo, sin una rama aparte.

### 9.3 Fixtures compartidos (la pieza que mantiene al front y al back alineados)

Guarda en `frontend/test/contratos/<modulo>/<operacion>.json` una respuesta de ejemplo **con la forma exacta del servidor** (por ejemplo `registrarVenta.confirmar.json`). Úsalas para:

1. probar que tu parser entiende la respuesta (sin red),
2. alimentar la implementación `Demo` cuando quieras que se parezca al servidor,
3. y, cuando el back las reciba, compararlas contra respuestas reales en una prueba de contrato.

Pídelas a partir del esquema (`backend/src/main/resources/graphql/*.graphqls`, que es la fuente de verdad) o de la colección de Postman (`backend/postman/`, 77 peticiones).

### 9.4 Dependencias sugeridas

| Paquete | Para qué |
|---|---|
| `http` | `POST /graphql` y REST |
| `flutter_secure_storage` | Token y `expiraEn` |
| `web_socket_channel` | WebSocket (ver la advertencia de la sección 6) |
| `uuid` | `Idempotency-Key` y `dispositivoId` |

No hace falta un generador de código al principio: los documentos GraphQL son cadenas en `servicios/remoto/` y los modelos se parsean a mano. Si la superficie crece, evalúa `graphql_codegen` leyendo los `.graphqls` del repo.

### 9.5 Qué no hacer

- No mandes `negocioId`. No lo aceptan.
- No calcules totales, cambio, costos ni márgenes en el cliente; el servidor los calcula y los devuelve.
- No uses `double` para dinero.
- No guardes el token en preferencias sin cifrar.
- No reintentes `confirmarComando` a ciegas; consulta el estado primero.
- No muestres el código de error al usuario: muestra `message` (ya viene en español) y usa el código para decidir qué hacer.
- No dependas de una suscripción para tener el dato: consulta primero.

## 10. Ejemplos en Dart (ilustrativos, no probados contra el servidor)

### 10.1 Cliente y errores

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiError implements Exception {
  ApiError(this.codigo, this.mensaje,
      {this.reintentable = false, this.regla, this.recursoId, this.estadoHttp});
  final String codigo;
  final String mensaje;
  final bool reintentable;
  final String? regla;
  final String? recursoId;
  final int? estadoHttp;
  @override
  String toString() => 'ApiError($codigo)';
}

class ClienteApi {
  ClienteApi({required this.baseUrl, http.Client? cliente}) : _http = cliente ?? http.Client();
  final String baseUrl;
  final http.Client _http;
  String? token;

  Future<Map<String, dynamic>> graphql(String consulta,
      {Map<String, dynamic>? variables, String? idempotencyKey, String? dispositivoId}) async {
    final r = await _http.post(
      Uri.parse('$baseUrl/graphql'),
      headers: {
        'Content-Type': 'application/json; charset=utf-8',
        if (token != null) 'Authorization': 'Bearer $token',
        if (idempotencyKey != null) 'Idempotency-Key': idempotencyKey,
        if (dispositivoId != null) 'X-Dispositivo-Id': dispositivoId,
      },
      body: jsonEncode({'query': consulta, if (variables != null) 'variables': variables}),
    );
    Map<String, dynamic> cuerpo = {};
    if (r.body.isNotEmpty) {
      try {
        cuerpo = jsonDecode(utf8.decode(r.bodyBytes)) as Map<String, dynamic>;
      } catch (_) {/* cuerpo no JSON */}
    }
    if (r.statusCode != 200) {
      throw ApiError(
        (cuerpo['codigo'] as String?) ?? (r.statusCode == 401 ? 'NO_AUTENTICADO' : 'ERROR_HTTP_${r.statusCode}'),
        'No se pudo completar la operación.',
        reintentable: r.statusCode == 429 || r.statusCode >= 500,
        estadoHttp: r.statusCode,
      );
    }
    final errores = cuerpo['errors'] as List?;
    if (errores != null && errores.isNotEmpty) {
      final e = errores.first as Map<String, dynamic>;
      final ext = (e['extensions'] as Map?)?.cast<String, dynamic>() ?? {};
      throw ApiError(
        (ext['codigo'] as String?) ?? 'ERROR_INTERNO',
        (e['message'] as String?) ?? 'Ocurrió un error.',
        reintentable: ext['reintentable'] == true,
        regla: ext['regla'] as String?,
        recursoId: ext['recursoId'] as String?,
      );
    }
    return cuerpo['data'] as Map<String, dynamic>;
  }
}
```

### 10.2 Escritura en dos pasos

```dart
const _preparar = r'mutation($i: ComandoInput!){ prepararComando(input:$i){ token resumen expiraEn advertencias } }';
const _confirmar = r'mutation($t: String!){ confirmarComando(token:$t){ ok codigo mensajePlantilla recursoId datos{ clave valor } } }';

class Preparado {
  Preparado(this.token, this.resumen, this.advertencias);
  final String token;
  final String resumen;
  final List<String> advertencias;
}

Future<Preparado> preparar(ClienteApi api, Map<String, dynamic> variante) async {
  final d = await api.graphql(_preparar, variables: {'i': variante});
  final p = d['prepararComando'] as Map<String, dynamic>;
  return Preparado(p['token'] as String, p['resumen'] as String, (p['advertencias'] as List).cast<String>());
}

Future<Map<String, String>> confirmar(ClienteApi api, String token) async {
  final d = await api.graphql(_confirmar, variables: {'t': token});
  final r = d['confirmarComando'] as Map<String, dynamic>;
  return {for (final x in (r['datos'] as List)) x['clave'] as String: x['valor'] as String};
}

// Cobrar: preparar → mostrar resumen → confirmar. "monto" y "cantidad" van como texto decimal.
// final p = await preparar(api, {'registrarVenta': {'turnoId': turnoId,
//   'lineas': [{'productoId': id, 'cantidad': '3'}],
//   'pagos': [{'metodo': 'EFECTIVO', 'monto': '66.00', 'recibido': '100.00'}]}});
```

### 10.3 Pruebas rápidas con curl

```bash
BASE=http://localhost:8080
# 1) código y sesión (desarrollo)
curl -s -X POST $BASE/auth/codigo -H 'Content-Type: application/json' -d '{"identificador":"yo@example.com"}'
CODIGO=$(curl -s "$BASE/dev/canal/codigo?correo=yo@example.com")   # solo desarrollo
TOKEN=$(curl -s -X POST $BASE/auth/sesion -H 'Content-Type: application/json' \
  -d "{\"identificador\":\"yo@example.com\",\"codigo\":\"$CODIGO\"}" | jq -r .token)
# 2) una consulta
curl -s -X POST $BASE/graphql -H 'Content-Type: application/json' -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"{ cajas { id nombre activa } }"}'
```

## 11. Hitos: qué conectar y cuándo

| Hito | Contenido | Qué puede hacer el front |
|---|---|---|
| **H-0 (hoy)** | Lo VIGENTE de las secciones 4 y 5, **más las 16 operaciones de menú y recetas de Restaurantes** (en memoria) | Capa `api/`, sesión, Caja, Ventas, Inventario, Negocios y el menú/recetas de Restaurantes. Extraer las interfaces de `servicios/` |
| **H-C** (fin de la Fase 1 del plan) | Contrato congelado: secciones 7 y 8 con tipos de Restaurantes | Escribir modelos y fixtures de todo lo propuesto; implementaciones `Remoto` listas sin servidor |
| **H-1** (HZ-20, HZ-21, HZ-23) | `referencia`, CORS, permisos y rol de restaurante | Cobrar una cuenta con `referencia`; correr Flutter web; leer permisos nuevos |
| **H-2** (HZ-26) | Mesas libres en la ficha pública | Pantalla del turista con «N de M mesas libres» real |
| **H-3** (HZ-24, HZ-27) | `Idempotency-Key` y escrituras públicas con límites | Pedido por QR real |
| **H-2b** (HZ-22, HZ-23) | **Hecho en el disco local, sin subir:** `guardarReceta` y `darDeBajaPlatillo` expuestos; Ventas ve los platillos | Guardar una receta y cobrar un platillo reales, cuando se suba y se arranque la aplicación completa |
| **H-4** (Restaurantes P1b, sin autorización) | Salón, cuentas, cocina, QR | Pantallas de salón, cocina y comensal reales |

El orden que recomiendo para el front, de menor a mayor riesgo: `api/` y sesión → Caja → Ventas → Inventario → Negocios público → (al llegar H-C) Restaurantes.

## 12. Cosas que hay que saber para no perder tiempo

- **Un negocio recién registrado tarda un instante en quedar activo** (sección 4).
- **Sin sesión con negocio activo** casi todo responde `SIN_NEGOCIO_ACTIVO`.
- **El perfil `produccion` aún no es para el front:** exige Postgres para Ventas, Caja e Inventario y no arranca sin él. Para desarrollo usa `desarrollo` (memoria, los datos se pierden al reiniciar) o Postgres con `docker compose up -d` y `ventas.almacen=postgres`, `caja.almacen=postgres`, `inventario.almacen=postgres`.
- **Seis productos `[SIMULADO]`** salen de `productosParaVenta` hasta que existan Misceláneos o Restaurantes; **todo cupón responde `CUPON_INVALIDO`** (no hay Pasaporte).
- **`publicarAvisoPrivacidad`** responde `CONFIRMACION_REQUERIDA` (permiso sensible, IDN-3.2).
- **Esta guía puede quedar desactualizada en lo PROPUESTO.** El día que algo pase a VIGENTE se actualiza aquí y se anota en la Bitácora.
