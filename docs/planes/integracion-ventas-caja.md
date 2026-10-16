# Plan de integración: Ventas y Caja (de la rama `uriel` a `julio`)

**Fecha:** 2026-10-16 · **Nivel:** Mayor · **Estado:** plan, esperando confirmación. **No hay autorización escrita para programar esto.**
**Rama de trabajo:** `julio` (local, sin push). **Base verificada:** `6366210` (mezcla de `origin/uriel`) más el trabajo de IDN-4 sin confirmar.
**Documentos base:** `docs/diseno/ventas.md`, `docs/diseno/caja.md`, `docs/planes/ventas-caja-v1.md` (los tres vienen de `uriel`), `docs/pruebas/idn4-puerta-unificada.tdd.md`, `docs/diseno/negocios.md` y `docs/BITACORA.md`.

Convención de estado: **[A]** aceptado, **[P]** propuesto y vigente salvo objeción, **[?]** abierto. Prioridades **P1** (MVP y demo), **P2** (si alcanza), **P3** (fuera del MVP). Todo dato de ejemplo es **simulado**. Las cifras de horas son estimaciones mías, sin medir.

---

## 1. Qué se pidió y qué entendí

> «Define el plan de integración, el objetivo es únicamente traer Caja y Venta de la rama `uriel`. De igual forma define cómo quedarán las queries de GraphQL para que, en lo que estamos integrando esto, trabajemos a la par en Misceláneos y en Restaurantes, para que cuando todo quede podamos consumir los módulos horizontales. Preséntalo en un `.md` y en un artefacto para mejor lectura. Sé específico y no omitas nada.»

Lo que entendí, en cinco puntos:

1. **Solo Ventas y Caja.** Lo demás que trae `uriel` (el canal de demostración, la maqueta en Flutter, los datos simulados) no es parte del objetivo.
2. **Integrar** significa que los dos módulos arranquen dentro de la plataforma real: con la sesión y los permisos de Identidad, con la puerta de `/graphql` que ya cerramos (IDN-4), con el patrón de comandos compartido, con eventos por el bus y, al final, con base de datos.
3. **Definir las queries** significa fijar el esquema GraphQL final de Ventas y Caja (nombres, tipos, argumentos, quién puede llamarlas, errores) **antes** de programar, para que otros construyan contra él.
4. **Trabajo en paralelo** en Misceláneos y Restaurantes (módulos verticales). Necesitan saber qué contratos son estables y cuándo, y qué archivos son suyos.
5. **Entregable de hoy:** este documento y su artefacto. Nada de código hasta que usted lo autorice.

**Lo que no sé** (y por eso hay preguntas en la sección 18): si el frontend de `uriel` se descarta o se conserva fuera de `julio`; quién es dueño de Misceláneos y de Restaurantes; si la persistencia en Postgres entra en esta ronda o después.

---

## 2. Estado de hoy (hechos verificados en el repositorio)

| Hecho | Evidencia |
|---|---|
| `julio` tiene un commit de mezcla local de `origin/uriel` (`6366210`), 7 commits por delante del remoto, sin push. | `git log` |
| Lo que trajo `uriel`: módulos `ventas` (36 clases de producción, 62 pruebas), `caja` (55 clases, 52 pruebas) y `canal` (13 clases, 12 pruebas); `compartido.comandos` (patrón preparar → confirmar genérico, 16 pruebas); `compartido.dinero.Redondeo`; tres esquemas GraphQL; 95 archivos de Flutter; 5 documentos. | `git diff --stat` |
| Ventas y Caja **no tienen base de datos**: repositorios en memoria (`VentasEnMemoria`, `CajaEnMemoria`), sin migraciones ni JDBC. | código |
| Sus beans **solo existen** con `negocio360.demo.habilitada=true` y fuera del perfil `produccion`. Sin esa bandera, `VentasApi` y `CajaApi` no son beans. | `VentasConfiguracion`, `CajaConfiguracion` |
| Suite completa tras la mezcla: **2249 pruebas, 3 fallas y 542 errores**. Una sola causa para los errores: la verificación de la tabla de operaciones (SG-21) no conoce `Query.cajas`, `cortes`, `turnoActivo`, `saldoTurno`, `venta`, `ventasDelTurno`, `productosSimulados`, `cuponesSimulados` ni sus mutaciones. Las 3 fallas son `EsquemaGraphqlClientesTest`. | `mvn clean verify` |
| El árbol compila. Dos choques ya resueltos en la mezcla: `compartido/dinero/Redondeo` (unión de las dos versiones) y `ContextoDeSesionTest` (doble contra la API vieja de Identidad). | Bitácora, 2026-10-16 |
| **No existen** los módulos Catálogo, Pasaporte, Misceláneos ni Restaurantes en el repositorio. Los verticales están en diseño (Restaurantes: recetas, D-09; Misceláneos: catálogo con venta a granel). | `ls backend/src/main/java/mx/negocio360` |
| El consumidor de `venta.registrada` que **sí** existe: Caja (`caja/contratos/VentaRegistradaV1`) y Clientes (`clientes/contratos/VentaRegistradaV1`, con prueba de contrato). Inventario, Misceláneos y Restaurantes aún no. | código |

---

## 3. Alcance

### 3.1 Qué entra

| Pieza | Origen en `uriel` | Destino |
|---|---|---|
| Módulo `ventas` completo (dominio, comandos, eventos, puertos, servicio) | `backend/.../ventas/**` y sus pruebas | Se queda, con cambios (secciones 9 y 12) |
| Módulo `caja` completo | `backend/.../caja/**` y sus pruebas | Se queda, con cambios |
| Patrón de comandos genérico | `compartido/comandos/**` | Se queda; gana la verificación de permiso (sección 9.3) |
| `Redondeo` unificado | `compartido/dinero/Redondeo` | Ya unificado con el de Inventario |
| Esquemas `ventas.graphqls` y `caja.graphqls` | `resources/graphql/` | Se rehacen según la sección 7 |
| `Resultado.datos` y el tipo `Dato` | `compartido.graphqls` | Se queda (lo necesita el folio y el cambio) |
| Documentos de diseño, plan, mockup y evidencia | `docs/diseno/{ventas,caja}.md`, `docs/planes/ventas-caja-v1.md`, `docs/mockups/ventas-caja.html`, `docs/pruebas/ventas-caja-f2-f3.tdd.md` | Se quedan; se renumeran sus decisiones (sección 5.1) |

### 3.2 Qué NO entra

| Pieza | Por qué | Cómo se recupera si se quiere |
|---|---|---|
| Módulo `canal` (13 clases, 12 pruebas) | Sustituido por el despachador de la plataforma (sección 6.3). Trae una **tercera forma de autenticar** y un negocio de demostración que se asigna a toda sesión sin negocio. | `git checkout origin/uriel -- backend/src/main/java/mx/negocio360/canal` |
| `canal.graphqls` (`productosSimulados`, `cuponesSimulados`) | Son datos simulados del canal | igual |
| `frontend/**` (95 archivos de Flutter) | No es Ventas ni Caja. Sigue en la rama `uriel`. | `git checkout origin/uriel -- frontend` |
| `CatalogoSimulado`, `CuponesSimulados`, `PermisosSimulados` | Los reemplazan el SPI de productos (sección 9.2), el puerto de cupones sin Pasaporte y los permisos reales de Identidad | rama `uriel` |
| El bloque `negocio360.demo.*` de `application-desarrollo.yml` | Habilita lo anterior | rama `uriel` |

### 3.3 Qué hacer con lo que ya se mezcló

La mezcla ya está confirmada en `julio` y **no se ha subido**. Tres caminos:

| Opción | Cómo | Ventaja | Costo |
|---|---|---|---|
| **A (recomendada)** | Dejar la mezcla y borrar `canal`, `frontend` y lo simulado en un commit aparte | No se reescribe historia; una futura mezcla de `uriel` sigue limpia; todo es recuperable | El historial muestra «se trajo y se quitó» |
| B | Deshacer la mezcla (`git reset` a `65b54d5`) y traer solo las rutas de Ventas y Caja con `git checkout origin/uriel -- <rutas>` | Historial más corto | Reescribe un commit local; `uriel` se vuelve a mezclar con conflictos; mi trabajo sin confirmar está encima y exige otro respaldo |
| C | Conservar todo, incluido el frontend | Cero trabajo | Contradice «únicamente Caja y Venta» y deja la tercera autenticación |

---

## 4. Hallazgos que cambian el plan

Cada fila es algo que encontré leyendo el código y que un plan hecho a ciegas no habría visto.

| # | Hallazgo | Por qué importa | Qué hace el plan |
|---|---|---|---|
| H-01 | **Los identificadores `D-76` a `D-90` están usados dos veces**: Identidad y Negocios (en `julio`) y Ventas y Caja (en `uriel`) eligieron los mismos números. Ya hay 15 repetidos en la Bitácora, por ejemplo `D-76` es «Registro de personal…» y también «Ventas y Caja son dos módulos…». | Cualquier cita a una decisión es ambigua | Se renumeran las de Ventas y Caja a `D-110`…`D-124` (sección 5.1) |
| H-02 | El canal trae **su propio interceptor de sesión** (`InterceptorDeSesion`, `ContextoGraphQl`) que autentica el Bearer por su cuenta, además del filtro de entrada (IDN-4). | Tres formas de saber quién eres | El módulo `canal` no entra; Ventas y Caja leen `EntradaGraphQl` |
| H-03 | `ContextoDeSesion` asigna un **negocio de demostración fijo** a toda sesión sin negocio. | Con ese fallback, una cuenta recién registrada vería y escribiría en el negocio de otra gente | Se elimina. Una sesión sin negocio recibe `SIN_NEGOCIO_ACTIVO` |
| H-04 | `PermisosSimulados` devuelve `true` a todo: cualquier cuenta anula ventas y autoriza diferencias de caja. | Control de dinero sin control | Se reemplaza por permisos reales (sección 8) |
| H-05 | `prepararComando` y `confirmarComando` los resuelven **dos controladores**: el del canal (Ventas/Caja) y `PublicoGraphQlController` de Negocios (reseñas). GraphQL admite un resolvedor por campo. | Con ambos presentes el arranque falla o responde el equivocado | Un **despachador único** en la plataforma (sección 6.3) |
| H-06 | `Resultado.datos` es `[Dato!]!` (no nulo) pero el resolvedor de Negocios devuelve un mapa sin `datos`. | Cualquier cliente que pida `datos` en una reseña recibe un error | El despachador siempre entrega `datos` (vacío si no hay) |
| H-07 | El gate de `/graphql` exige que cada campo de la raíz esté declarado. Ventas y Caja no están declarados. | Es la causa de los 542 errores | `OperacionesDeVentas` y `OperacionesDeCaja` (sección 7.3) |
| H-08 | Los nombres de tipo de GraphQL son globales. `caja.graphqls` define `TipoMovimiento`, `Corte`, `MetodoPago`; Inventario va a necesitar `TipoMovimiento`. | Colisión al arrancar cuando Inventario tenga esquema | Prefijo: `TipoMovimientoCaja`, `CorteDeCaja`; `MetodoPago` y `MontoPorMetodo` pasan a `compartido.graphqls` |
| H-09 | `ventasDelTurno`, `cortes`, `cajas` y `SaldoTurno.movimientos` **no están paginados**. SG-07 lo exige y el propio diseño de `ventas.md` (VF-02) pide cursor. | Un turno largo devuelve miles de filas | Conexiones con cursor (sección 7.2) |
| H-10 | Los eventos salen por `ApplicationEventPublisher` y Caja los escucha con `@EventListener`. No hay outbox ni transacción: si algo falla entre guardar la venta y publicar, se pierde el evento. | Caja puede quedar sin una venta | Se usan `PublicadorEnBus`, `FronteraTransaccional` y `@ApplicationModuleListener` (fase F5) |
| H-11 | Las migraciones que `uriel` reservó (`V030` ventas y `V040` caja) **ya están ocupadas** (`V030` es de Negocios). | Choque de Flyway | Asignación nueva (sección 13.4) |
| H-12 | El patrón preparar → confirmar existe **tres veces** (`compartido.comandos`, el de Inventario y el de Clientes). | Deuda | No se toca en esta ronda; se anota (HTC-89) |
| H-13 | `Catálogo` y `Pasaporte` no existen. Ventas depende de ambos por puertos. | Sin productos no hay venta | SPI de productos que implementan los verticales (sección 9.2); sin Pasaporte, todo cupón es inválido |
| H-14 | El código de tasa del canal y de la plataforma diferían (`DEMASIADAS_PETICIONES`/`DEMASIADAS_SOLICITUDES`). | — | Ya unificado en IDN-4 |
| H-15 | Los beans de Ventas y Caja sólo existen en modo demo; en producción no habría ni `VentasApi`. | La plataforma arrancaría sin Ventas y sin avisar | Fallar al arrancar en `produccion` si falta el almacén (sección 12, F6) |
| H-16 | La anulación y la autorización de diferencias dependen de «quién es encargado», una pregunta abierta `[?]` en ambos diseños. | Bloquea los permisos | Propuesta de matriz en la sección 8 para que usted la confirme |

---

## 5. Decisiones

### 5.1 Renumeración de las decisiones de Ventas y Caja

Las decisiones de `julio` ya están citadas en código, pruebas y documentos que otras personas usan, y ya se subieron. Las de `uriel` no están integradas. Por eso se renumeran **las de `uriel`**. Se reemplazan solo en los archivos de Ventas y Caja y en las filas de la Bitácora que vienen de `uriel`.

| Antes (uriel) | Ahora | Tema |
|---|---|---|
| D-76 | **D-110** | Ventas y Caja separados; Ventas no conoce a Caja; el turno lo pasa quien llama |
| D-77 | **D-111** | Contrato de `venta.registrada` v1 y `venta.anulada` v1 |
| D-78 | **D-112** | Cupón: cotizar al preparar, canjear al confirmar, en la misma transacción |
| D-79 | **D-113** | Folio consecutivo por negocio y serie, asignado al confirmar |
| D-80 | **D-114** | Ventas no conoce boletos ni venta a granel; viene de Catálogo |
| D-81 | **D-115** | Preparar el corte pone el turno `EN_CORTE` |
| D-82 | **D-116** | Anular usa preparar → confirmar, con motivo y permiso |
| D-83 | **D-117** | Todo el código se documenta con JavaDoc que cita reglas y códigos |
| D-84 | **D-118** | El patrón preparar → confirmar genérico vive en `compartido.comandos` |
| D-85 | **D-119** | Si el cálculo deja de cuadrar entre los pasos, `COMANDO_CAMBIO` |
| D-86 | **D-120** | Ventas verifica que el turno siga abierto (`VerificadorDeTurno`) |
| D-87 | **D-121** | `canjearCupon` recibe el descuento que vio el cajero |
| D-88 | **D-122** | El consumidor de Caja marca el evento como procesado solo si lo aplicó |
| D-89 | **D-123** | El frontend se hace en Dart y Flutter, en `frontend/` |
| D-90 | **D-124** | Negocio de demostración fijo para sesiones sin negocio |

**Notas:** `D-123` y `D-124` quedan como historia: este plan no integra el frontend y **revoca** `D-124` (ver D-129). Las decisiones antiguas compartidas (`D-20`, `D-22`, `D-23`, `D-34`, `D-45`, `D-48`, `D-55`, `D-63`, `D-65`) no cambian. La renumeración se hace con un guion que lleva una lista explícita de archivos y se verifica con `grep` (criterio en la sección 16).

### 5.2 Decisiones nuevas de este plan (todas **PROPUESTA**)

| ID | Decisión | Por qué |
|---|---|---|
| **D-125** | Un solo **despachador de comandos** en la plataforma (`compartido.graphql.comandos`) es el único dueño de `prepararComando` y `confirmarComando`. Cada módulo se registra con una interfaz `ModuloDeComandos`; el token lleva el prefijo del módulo. | H-05, H-06. Sirve también a Clientes, Inventario y los verticales |
| **D-126** | Los comandos de Ventas y Caja exigen su permiso **dos veces**: en la puerta de la plataforma (por variante) y dentro del manejador, también al **confirmar**, antes de gastar el token. | Si le quitan el rol entre los dos pasos, no confirma |
| **D-127** | **Permisos de Ventas y Caja** (11) y su matriz de roles (sección 8). Ninguno sensible por ahora. | H-04, H-16 |
| **D-128** | El **SPI de productos** (`FuenteDeProductos`) vive en `compartido.catalogo`. Misceláneos y Restaurantes lo implementan; Ventas lo consume sin conocer a ningún vertical (R4). Cuando exista Catálogo, será otra implementación. | H-13 |
| **D-129** | Se **revoca D-124** (negocio de demostración): una sesión sin negocio no opera en ninguno. Los datos de demostración salen de los verticales o de un perfil de desarrollo rotulado, nunca de un negocio fijo en el código. | H-03 |
| **D-130** | **Prefijo de tipos GraphQL** por módulo cuando el nombre es genérico; los tipos que dos horizontales comparten (`MetodoPago`, `MontoPorMetodo`) van en `compartido.graphqls`. | H-08 |
| **D-131** | **Paginación por cursor** (máximo 100, 20 por omisión) en `ventas`, `cortes` y `movimientos`; `cajas` con tope de 50 por negocio. | H-09, SG-07 |
| **D-132** | Los eventos de Ventas y Caja salen por **outbox** (`PublicadorEnBus` y `FronteraTransaccional`) y Caja los consume con `@ApplicationModuleListener`. | H-10, D-103 de Negocios |
| **D-133** | **Asignación de migraciones**: V035 (permisos de Ventas y Caja), V040 (esquema de Ventas), V041 (esquema de Caja), V042 (RLS forzada de ambos). V036 a V039 quedan para permisos de Misceláneos, Restaurantes e Inventario. | H-11 |
| **D-134** | Sin Pasaporte, **todo código de cupón responde `CUPON_INVALIDO`** (orden de recorte 3 del plan de `uriel`: se cobra sin cupón). | H-13 |

---

## 6. Arquitectura objetivo

### 6.1 Quién depende de quién

```mermaid
flowchart LR
  subgraph Plataforma["compartido (plataforma)"]
    C1[ContextoNegocio y Autorizacion]
    C2[comandos: preparar → confirmar]
    C3[graphql.comandos: despachador]
    C4[catalogo: FuenteDeProductos]
    C5[eventos: SobreEvento y PublicadorEnBus]
    C6[dinero: Redondeo]
  end
  ID[Identidad]
  VE[Ventas - horizontal]
  CA[Caja - horizontal]
  MI[Misceláneos - vertical]
  RE[Restaurantes - vertical]
  CL[Clientes]
  IN[Inventario]
  VE --> Plataforma
  CA --> Plataforma
  CA -->|VentasApi.resumenTurno| VE
  MI -->|implementa| C4
  RE -->|implementa| C4
  VE -->|consume| C4
  VE -. venta.registrada / venta.anulada .-> CA
  VE -. venta.registrada / venta.anulada .-> CL
  VE -. venta.registrada / venta.anulada .-> IN
  VE -. venta.registrada / venta.anulada .-> MI
  VE -. venta.registrada / venta.anulada .-> RE
  ID -. permisos y sesión .-> Plataforma
```

Línea sólida: llamada síncrona permitida (R2, R4, R6). Línea punteada: evento por el bus (R3, R5). **Ventas no conoce a Caja** (D-110); Caja depende de Ventas en un solo sentido. **Los verticales no llaman a Ventas para escribir ventas**: la app llama a GraphQL; los verticales ofrecen productos y reaccionan a eventos.

### 6.2 Reglas de módulos (verificadas por `ApplicationModules.verify()`)

| Módulo | `allowedDependencies` | Notas |
|---|---|---|
| `ventas` | `compartido` | Interfaces con nombre: `puertos`, `dominio`, `comandos`, `textos`, `graphql` |
| `caja` | `compartido`, `ventas` | Solo `VentasApi.resumenTurno` y el tipo `ResumenTurno` |
| `miscelaneos` (nuevo) | `compartido` | Solo eventos (por nombre) y el SPI de productos. Si necesita llamar a `ventas`, se agrega `ventas` con su interfaz con nombre |
| `restaurantes` (nuevo) | `compartido`, `inventario` | Recetas contra `InventarioApi.aBase` (D-09) |
| `canal` | **deja de existir** | — |

### 6.3 El despachador de comandos (D-125)

Hoy cada módulo resolvería `prepararComando` por su cuenta y GraphQL solo admite uno. La solución es un único controlador de plataforma que reparte por variante:

```java
// compartido.graphql.comandos
public interface ModuloDeComandos {
    String prefijo();                       // "ventas.", "caja.", "negocios."...
    Set<String> variantes();                // {"registrarVenta", "anularVenta"}
    boolean confirmaSinSesion();            // solo Negocios (la reseña del turista)
    PreparacionVista preparar(EntradaGraphQl entrada, String variante, Map<String, Object> campos);
    ResultadoVista confirmar(EntradaGraphQl entrada, String tokenSinPrefijo);
}
```

- `prepararComando(input)`: exactamente una variante (`@oneOf`); la puerta ya comprobó el permiso de **esa variante**; el despachador busca el módulo dueño y delega. Al token que devuelve el módulo se le antepone su prefijo (`ventas.…`). Igual que el canal de `uriel`, pero genérico.
- `confirmarComando(token)`: el prefijo dice a quién entregarlo. El despachador exige sesión con negocio **salvo** que el módulo declare `confirmaSinSesion()` (solo `negocios.`, por las reseñas anónimas). Esto cierra el hallazgo de seguridad pendiente de IDN-4 («`confirmarComando` pública»).
- `datos` siempre se entrega (lista vacía si no hay): arregla H-06.
- Un prefijo desconocido responde `TOKEN_INVALIDO` sin revelar qué módulos existen.
- Prueba de arquitectura: todo bean `ModuloDeComandos` declara explícitamente `confirmaSinSesion`, y la lista de los que lo dejan en `true` está fijada por una prueba.
- **Toca código de Negocios** (`PublicoGraphQlController.prepararComando` y `confirmarComando`, que pasan a un adaptador `ComandosDeNegocios`). Sus pruebas (`GraphQlEntradaTest.resenas_*`) deben seguir en verde sin cambiar una aserción. Requiere el visto bueno de quien lo mantiene.

---

## 7. Contrato GraphQL final

### 7.1 Principios

1. **El `negocioId` nunca es argumento ni campo de entrada.** Sale de la sesión (SG-02). Los ids que sí se reciben (`turnoId`, `ventaId`, `productoId`, `clienteId`) son referencias opacas: con un id de otro negocio la respuesta es vacía, igual que con uno inexistente (seguridad por fila como segunda barrera).
2. Dinero y cantidades son `Decimal` (cadena exacta, nunca punto flotante). Fechas en UTC.
3. Escrituras por el par `prepararComando` → `confirmarComando`. Ninguna mutación propia.
4. Todo campo de la raíz está declarado en la tabla de operaciones con su permiso; lo no declarado se niega.
5. Listas largas con cursor: máximo 100, 20 por omisión; el cursor es opaco.
6. Los textos de error son fijos, en español de México; el cliente se guía por `extensions.codigo`.
7. Límites de la plataforma que aplican: profundidad 7, costo 100, 10 alias, 5 campos de la raíz, 10 000 caracteres de consulta, 64 KB de cuerpo.

### 7.2 Esquema completo

**`compartido.graphqls` (cambios)**

```graphql
# Se mantienen: Decimal, Date, DateTime, Query, Mutation, Preparacion, Resultado, Dato, PageInfo.
# Nuevo: lo que comparten Ventas, Caja y los verticales que cobren.

"Forma de pago. Es el mismo contrato en Ventas, Caja y cualquier vertical que lo muestre (D-111)."
enum MetodoPago { EFECTIVO TARJETA TRANSFERENCIA }

"Un monto por método de pago, con escala 2."
type MontoPorMetodo {
  metodo: MetodoPago!
  monto: Decimal!
}

input MontoPorMetodoInput {
  metodo: MetodoPago!
  monto: Decimal!
}
```

**`ventas.graphqls`**

```graphql
# Esquema de Ventas (docs/diseno/ventas.md, v0.2). Sin negocioId: el negocio sale de la sesión (SG-02).
# Las escrituras viajan por prepararComando y confirmarComando (D-55, D-125).

extend type Query {
  "VF-01. Una venta del negocio con sus líneas y pagos; null si no existe en este negocio."
  venta(id: ID!): Venta

  "VF-02. Ventas del negocio, de la más reciente a la más antigua. Máximo 100 por página."
  ventas(filtro: FiltroVentas, primeros: Int! = 20, despues: String): VentaConexion!

  "VF-03. Conteos, totales por método y preparaciones vigentes de un turno (el corte de Caja usa lo mismo)."
  resumenTurno(turnoId: ID!): ResumenTurno!

  "G1. Productos que se pueden vender, de las fuentes del negocio (Catálogo o verticales). Máximo 50."
  productosParaVenta(texto: String, primeros: Int! = 20): [ProductoParaVenta!]!
}

enum EstadoVenta { REGISTRADA ANULADA }

type Venta {
  id: ID!
  serie: String!
  folio: Int!
  estado: EstadoVenta!
  turnoId: ID!
  subtotal: Decimal!
  descuento: Decimal!
  "Neto de cupón (D-65)."
  total: Decimal!
  lineas: [LineaVenta!]!
  pagos: [PagoVenta!]!
  "Opcional y opaco: el cliente que eligió el cajero (VT-13)."
  clienteId: ID
  "Opcional y opaco: el cupón del Pasaporte que se canjeó."
  cuponId: ID
  ocurridoEn: DateTime!
  anuladaEn: DateTime
  motivoAnulacion: String
}

type LineaVenta {
  linea: Int!
  productoId: ID!
  "Copia al vender (VT-02)."
  descripcion: String!
  cantidad: Decimal!
  unidad: String!
  precioUnitario: Decimal!
  importe: Decimal!
  serie: String
}

type PagoVenta {
  metodo: MetodoPago!
  monto: Decimal!
  "Solo efectivo."
  recibido: Decimal
  "Solo efectivo."
  cambio: Decimal
  referencia: String
}

type VentaConexion {
  ventas: [Venta!]!
  pageInfo: PageInfo!
}

input FiltroVentas {
  turnoId: ID
  estado: EstadoVenta
  "Inclusive. Con hasta debe haber a lo más 92 días de rango."
  desde: DateTime
  hasta: DateTime
  serie: String
  folio: Int
}

type ResumenTurno {
  turnoId: ID!
  ventasRegistradas: Int!
  ventasAnuladas: Int!
  "Solo los métodos con pagos."
  totalesPorMetodo: [MontoPorMetodo!]!
  preparacionesVigentes: Int!
}

type ProductoParaVenta {
  productoId: ID!
  descripcion: String!
  unidad: String!
  precio: Decimal!
  admiteFraccion: Boolean!
}

extend input ComandoInput {
  "VF-10."
  registrarVenta: RegistrarVentaInput
  "VF-11."
  anularVenta: AnularVentaInput
}

"VF-10. El precio lo pone la fuente de productos (VT-02); los pagos deben sumar el total (VT-04)."
input RegistrarVentaInput {
  turnoId: ID!
  "De 1 a 100 líneas."
  lineas: [LineaVentaInput!]!
  "De 1 a 10 pagos."
  pagos: [PagoVentaInput!]!
  "Máximo 40 caracteres. Sin Pasaporte, cualquier código es CUPON_INVALIDO (D-134)."
  codigoCupon: String
  clienteId: ID
}

input LineaVentaInput {
  productoId: ID!
  cantidad: Decimal!
  serie: String
}

input PagoVentaInput {
  metodo: MetodoPago!
  monto: Decimal!
  "Solo en efectivo: lo que entrega el cliente, para calcular el cambio."
  recibido: Decimal
  referencia: String
}

"VF-11. Motivo de hasta 200 caracteres (VT-09)."
input AnularVentaInput {
  ventaId: ID!
  motivo: String!
}
```

**`caja.graphqls`**

```graphql
# Esquema de Caja (docs/diseno/caja.md, v0.2). Sin negocioId: el negocio sale de la sesión (SG-02).

extend type Query {
  "CF-04. Cajas del negocio (máximo 50)."
  cajas: [Caja!]!

  "CF-01. Turno abierto o en corte de una caja, con su estado efectivo (D-115); null si no hay."
  turnoActivo(cajaId: ID!): Turno

  "CF-02. Esperado por método de un turno. Los movimientos van paginados."
  saldoTurno(turnoId: ID!): SaldoTurno!

  "CF-03. Cortes del negocio, del más reciente al más antiguo. Máximo 100 por página."
  cortes(filtro: FiltroCortes, primeros: Int! = 20, despues: String): CorteConexion!
}

enum EstadoTurno { ABIERTO EN_CORTE CERRADO }
enum TipoMovimientoCaja { FONDO VENTA ANULACION ENTRADA RETIRO AJUSTE }
enum TipoMovimientoManual { ENTRADA RETIRO }

type Caja {
  id: ID!
  nombre: String!
  activa: Boolean!
}

type Turno {
  id: ID!
  cajaId: ID!
  estado: EstadoTurno!
  fondoInicial: Decimal!
  abiertoEn: DateTime!
  cerradoEn: DateTime
  "Cuándo vence el corte preparado y el turno regresa a ABIERTO (D-115). Solo EN_CORTE."
  corteVenceEn: DateTime
  "Cuenta que abrió el turno (id opaco)."
  abiertoPor: ID!
}

type MovimientoCaja {
  id: ID!
  tipo: TipoMovimientoCaja!
  metodo: MetodoPago!
  "Con signo: las anulaciones y los retiros son negativos."
  monto: Decimal!
  ventaId: ID
  nota: String
  registradoEn: DateTime!
}

type MovimientoCajaConexion {
  movimientos: [MovimientoCaja!]!
  pageInfo: PageInfo!
}

type SaldoTurno {
  turnoId: ID!
  cajaId: ID!
  estado: EstadoTurno!
  esperadoPorMetodo: [MontoPorMetodo!]!
  ventasAplicadas: Int!
  "Libro del turno, en orden. Máximo 100 por página."
  movimientos(primeros: Int! = 50, despues: String): MovimientoCajaConexion!
}

type CorteDeCaja {
  id: ID!
  turnoId: ID!
  cajaId: ID!
  esperadoPorMetodo: [MontoPorMetodo!]!
  contadoPorMetodo: [MontoPorMetodo!]!
  diferenciaEfectivo: Decimal!
  abiertoEn: DateTime!
  confirmadoEn: DateTime!
  "Quién autorizó una diferencia sobre el umbral (CJ-08); null si no hizo falta."
  autorizadoPor: ID
}

type CorteConexion {
  cortes: [CorteDeCaja!]!
  pageInfo: PageInfo!
}

input FiltroCortes {
  cajaId: ID
  desde: DateTime
  hasta: DateTime
}

extend input ComandoInput {
  "CF-14."
  crearCaja: CrearCajaInput
  "CF-10."
  abrirTurno: AbrirTurnoInput
  "CF-11."
  registrarMovimientoCaja: MovimientoCajaInput
  "CF-12."
  cerrarTurno: CerrarTurnoInput
  "CF-13."
  descartarCorte: DescartarCorteInput
}

"CF-14. Nombre de 1 a 60 caracteres, único por negocio sin importar mayúsculas."
input CrearCajaInput {
  nombre: String!
}

"CF-10. Fondo de cero o más con 2 decimales (CJ-02)."
input AbrirTurnoInput {
  cajaId: ID!
  fondoInicial: Decimal!
}

"CF-11. Entrada o retiro de efectivo con nota (CJ-05, CJ-10)."
input MovimientoCajaInput {
  turnoId: ID!
  tipo: TipoMovimientoManual!
  monto: Decimal!
  nota: String!
}

"CF-12. Preparar pone el turno EN_CORTE (D-115). Un método que falta cuenta como 0.00."
input CerrarTurnoInput {
  turnoId: ID!
  contado: [MontoPorMetodoInput!]!
  "Para confirmar una diferencia sobre el umbral hace falta el permiso caja.corte.autorizar (CJ-08)."
  autorizar: Boolean! = false
}

"CF-13."
input DescartarCorteInput {
  turnoId: ID!
}
```

### 7.3 Tabla de operaciones (lo que se declara en la tabla de SG-21)

Acceso `NEGOCIO` = sesión con negocio activo y el permiso indicado. Ninguna es pública.

| Operación | Tipo | Permiso que pide la puerta | Segunda barrera (en el servicio) | Límites y notas |
|---|---|---|---|---|
| `venta(id)` | Query | `ventas.venta.ver` | `VentasApi.venta` | id ajeno → `null` |
| `ventas(filtro, primeros, despues)` | Query | `ventas.venta.ver` | `VentasApi.ventas` | ≤ 100; rango ≤ 92 días |
| `resumenTurno(turnoId)` | Query | `ventas.venta.ver` | Ventas (la ruta de Caja es interna, sin permiso de usuario) | turno ajeno → ceros |
| `productosParaVenta(texto, primeros)` | Query | `ventas.venta.registrar` | Ventas | texto ≤ 80; ≤ 50 |
| `cajas` | Query | `caja.turno.ver` | `CajaApi.cajas` | ≤ 50 |
| `turnoActivo(cajaId)` | Query | `caja.turno.ver` | `CajaApi.turnoActivo` | |
| `saldoTurno(turnoId)` | Query | `caja.turno.ver` | `CajaApi.saldoTurno` | `movimientos` ≤ 100 |
| `cortes(filtro, …)` | Query | `caja.corte.ver` | `CajaApi.cortes` | ≤ 100 |
| `prepararComando(registrarVenta)` | Mutation | `ventas.venta.registrar` | manejador (también al confirmar) | 1–100 líneas, 1–10 pagos |
| `prepararComando(anularVenta)` | Mutation | `ventas.venta.anular` | manejador (también al confirmar) | motivo ≤ 200 |
| `prepararComando(crearCaja)` | Mutation | `caja.caja.gestionar` | manejador | ≤ 50 cajas |
| `prepararComando(abrirTurno)` | Mutation | `caja.turno.abrir` | manejador | |
| `prepararComando(registrarMovimientoCaja)` | Mutation | `caja.movimiento.registrar` | manejador; si `tipo = RETIRO` exige además `caja.retiro.registrar` | |
| `prepararComando(cerrarTurno)` | Mutation | `caja.corte.cerrar` | manejador; si `autorizar = true` exige `caja.corte.autorizar` | |
| `prepararComando(descartarCorte)` | Mutation | `caja.corte.cerrar` | manejador | |
| `confirmarComando(token)` | Mutation | (el despachador exige sesión con negocio para estos prefijos) | el manejador **vuelve a comprobar** el permiso del comando antes de gastar el token | |

### 7.4 Ejemplos

**Preparar una venta con pago en efectivo**

```graphql
mutation Preparar($input: ComandoInput!) {
  prepararComando(input: $input) { token resumen expiraEn advertencias }
}
```
```json
{ "input": { "registrarVenta": {
    "turnoId": "0192a39f-0c11-7d20-8e31-abcdefabcdef",
    "lineas": [ { "productoId": "0191f1b0-0001-7000-8000-000000000001", "cantidad": "2" },
                { "productoId": "0191f1b0-0002-7000-8000-000000000002", "cantidad": "0.5" } ],
    "pagos":  [ { "metodo": "EFECTIVO", "monto": "150.00", "recibido": "200.00" } ] } } }
```

**Confirmar**

```graphql
mutation Confirmar($t: String!) {
  confirmarComando(token: $t) { ok codigo mensajePlantilla recursoId datos { clave valor } }
}
```
```json
{ "data": { "confirmarComando": { "ok": true, "codigo": "OK", "mensajePlantilla": "Venta registrada.",
  "recursoId": "0192a4c0-e8b0-7a55-9c10-7788990011aa",
  "datos": [ { "clave": "cambio", "valor": "50.00" }, { "clave": "folio", "valor": "1042" },
             { "clave": "total", "valor": "150.00" }, { "clave": "venta_id", "valor": "0192a4c0-e8b0-7a55-9c10-7788990011aa" } ] } } }
```

**Claves de `datos` por comando** (ordenadas por clave):

| Comando | Claves |
|---|---|
| `registrarVenta` | `cambio`, `folio`, `total`, `venta_id` |
| `anularVenta` | `estado`, `venta_id` |
| `crearCaja` | `caja_id` |
| `abrirTurno` | `turno_id` |
| `registrarMovimientoCaja` | `movimiento_id` |
| `cerrarTurno` | `corte_id`, `diferencia_efectivo` |
| `descartarCorte` | `turno_id` |

**Buscar ventas del turno, página de 20**

```graphql
query Ventas($f: FiltroVentas) {
  ventas(filtro: $f, primeros: 20) {
    ventas { id serie folio estado total ocurridoEn pagos { metodo monto } }
    pageInfo { hayMas cursorFinal }
  }
}
```

**Estado de la caja y sus movimientos**

```graphql
query Caja($turno: ID!) {
  saldoTurno(turnoId: $turno) {
    estado ventasAplicadas esperadoPorMetodo { metodo monto }
    movimientos(primeros: 50) { movimientos { tipo metodo monto nota registradoEn } pageInfo { hayMas cursorFinal } }
  }
}
```

**Productos para vender**

```graphql
query { productosParaVenta(texto: "camote", primeros: 10) { productoId descripcion unidad precio admiteFraccion } }
```

### 7.5 Qué cambia contra el esquema de `uriel`

| Antes | Ahora | Motivo |
|---|---|---|
| `ventasDelTurno(turnoId)` sin paginar | Se elimina; `ventas(filtro: { turnoId })` | H-09 |
| `cortes` sin paginar ni filtros | `cortes(filtro, primeros, despues)` | H-09 |
| `SaldoTurno.movimientos` lista completa | `movimientos(primeros, despues)` con conexión | H-09 |
| `MetodoPago` en `ventas.graphqls` | En `compartido.graphqls`, con `MontoPorMetodo` y su input | H-08, D-130 |
| `Corte`, `TipoMovimiento` | `CorteDeCaja`, `TipoMovimientoCaja` | H-08 |
| `PagoInput` | `PagoVentaInput` | Nombre genérico |
| `Venta` sin `clienteId`, `cuponId`, `motivoAnulacion` | Se agregan | VT-13, VT-07, VT-09 |
| `Turno` sin `abiertoPor` | Se agrega | Auditoría |
| `CorteDeCaja` sin `autorizadoPor` | Se agrega | CJ-08 |
| — | `resumenTurno`, `productosParaVenta` | VF-03 y G1 del diseño |
| `canal.graphqls` | Se elimina | Alcance |

### 7.6 Reservado para P2 (no se programa ahora, pero el nombre queda tomado)

| Reservado | Quién lo usará |
|---|---|
| `Subscription.ventaRegistrada` | Tablero en vivo del dueño. Pasará por `EventosPorNegocio`. |
| `Subscription.turnoEnVivo(turnoId)` | Idem (esperado por método cada vez que cambia). |
| `Query.resumenDia(fecha)` | Inicio del panel. |
| `Query.ventasDeCliente` | Si Clientes lo pide; hoy lo sirve el evento. |

### 7.7 Códigos de error

Todos salen en `extensions.codigo` con su texto fijo. La plataforma agrega los suyos (`NO_AUTENTICADO`, `SIN_NEGOCIO_ACTIVO`, `PERMISO_DENEGADO`, `CONSULTA_INVALIDA`, `DEMASIADOS_ALIAS`, `DEMASIADOS_CAMPOS_RAIZ`, `CONSULTA_MUY_PROFUNDA`, `CONSULTA_MUY_COSTOSA`, `ERROR_INTERNO` con `referencia`).

**Ventas (25):** `VENTA_SIN_LINEAS`, `CANTIDAD_INVALIDA`, `CANTIDAD_NO_FRACCIONABLE`, `PRODUCTO_NO_ENCONTRADO`, `PRODUCTO_INACTIVO`, `DEMASIADAS_LINEAS`, `DEMASIADOS_PAGOS`, `TEXTO_DEMASIADO_LARGO`, `PAGOS_NO_CUADRAN`, `EFECTIVO_INSUFICIENTE`, `METODO_PAGO_INVALIDO`, `CUPON_INVALIDO`, `CUPON_YA_CANJEADO`, `CUPON_NO_APLICA`, `DESCUENTO_MAYOR_AL_SUBTOTAL`, `TURNO_NO_ABIERTO`, `VENTA_NO_ENCONTRADA`, `VENTA_YA_ANULADA`, `MOTIVO_REQUERIDO`, `TOKEN_INVALIDO`, `TOKEN_EXPIRADO`, `TOKEN_USADO`, `COMANDO_CAMBIO`, `CONCURRENCIA`, `ERROR_TEMPORAL`.
**Se elimina** `SIN_PERMISO_ANULAR`: lo sustituye `PERMISO_DENEGADO` de la plataforma (D-126).

**Caja (22 y 1 nueva):** `CAJA_NO_ENCONTRADA`, `CAJA_INACTIVA`, `CAJA_DUPLICADA`, `NOMBRE_CAJA_INVALIDO`, `TURNO_NO_ENCONTRADO`, `TURNO_YA_ABIERTO`, `TURNO_NO_ABIERTO`, `TURNO_EN_CORTE`, `FONDO_INVALIDO`, `MONTO_INVALIDO`, `RETIRO_MAYOR_AL_EFECTIVO`, `NOTA_REQUERIDA`, `CORTE_VENTAS_PENDIENTES`, `CORTE_EVENTOS_PENDIENTES`, `CORTE_REQUIERE_AUTORIZACION`, `CONTEO_INVALIDO`, `TOKEN_INVALIDO`, `TOKEN_EXPIRADO`, `TOKEN_USADO`, `COMANDO_CAMBIO`, `CONCURRENCIA`, `ERROR_TEMPORAL`. **Se agrega** `DEMASIADAS_CAJAS` (más de 50 por negocio).

Clasificación hacia el cliente: `TOKEN_*`, `*_NO_ENCONTRAD*`, `*_INVALID*` y los de validación → `BAD_REQUEST`/`NOT_FOUND`; `PERMISO_DENEGADO` → `FORBIDDEN`; `CONCURRENCIA` y `ERROR_TEMPORAL` llevan `reintentable: true`.

---

## 8. Permisos e Identidad (D-127)

### 8.1 Los 11 permisos nuevos

| Permiso | Para qué | Sensible |
|---|---|---|
| `ventas.venta.ver` | Ver ventas, resumen del turno | No |
| `ventas.venta.registrar` | Cobrar (y buscar productos para vender) | No |
| `ventas.venta.anular` | Anular una venta cobrada | No (candidato cuando exista IDN-3.2) |
| `caja.caja.gestionar` | Crear cajas | No |
| `caja.turno.ver` | Ver cajas, turnos y su saldo | No |
| `caja.turno.abrir` | Abrir turno con fondo | No |
| `caja.movimiento.registrar` | Meter efectivo | No |
| `caja.retiro.registrar` | Sacar efectivo | No |
| `caja.corte.cerrar` | Preparar, descartar y confirmar el corte | No |
| `caja.corte.autorizar` | Autorizar una diferencia sobre el umbral | No (candidato) |
| `caja.corte.ver` | Auditar cortes pasados | No |

Si alguno fuera `sensible` quedaría **negado por completo** hasta que exista la confirmación reforzada (SG-19, IDN-3.2). Por eso hoy ninguno lo es y la anulación queda protegida por motivo, permiso y auditoría.

### 8.2 Matriz de roles propuesta (se pide su confirmación)

| Rol | Ventas | Caja |
|---|---|---|
| DUENO | los 3 | los 8 |
| ADMINISTRADOR | los 3 | los 8 |
| GERENTE | los 3 | todos menos `caja.caja.gestionar` |
| CAJERO | `venta.ver`, `venta.registrar` | `turno.ver`, `turno.abrir`, `movimiento.registrar`, `corte.cerrar` |
| TAQUILLERO | `venta.ver`, `venta.registrar` | `turno.ver`, `turno.abrir`, `movimiento.registrar`, `corte.cerrar` |
| MESERO | ninguno | ninguno |
| COCINERO | ninguno | ninguno |
| GUIA | ninguno | ninguno |

Resuelve la pregunta abierta de ambos diseños: anular, retirar y autorizar diferencias son de **gerente, administrador y dueño**. Un mesero no cobra: la cuenta del restaurante la cierra quien tenga caja (decisión de Restaurantes si cambia).

### 8.3 Qué se programa

- `ventas/seguridad/PermisosDeVentas.java` y `caja/seguridad/PermisosDeCaja.java` (implementan `CatalogoDePermisos`, igual que `PermisosDeClientes`).
- `RolesBase`: nuevas constantes por rol; `RolesBase.verificar` ya impide citar un permiso que nadie declara (AZ-03).
- Migración `V035__identidad_permisos_de_ventas_y_caja.sql` con `INSERT` de 11 permisos y de sus filas en `rol_permiso`.
- Pruebas que se actualizan **por derivación, no por literal** donde sea posible: `CatalogoDePermisosPlataformaTest`, `EsquemaEquipoPostgresTest` (compara migración contra `RolesBase`), las de `RolesBase`. El catálogo de permisos crece en 11.

---

## 9. Contrato Java

### 9.1 `VentasApi` y `CajaApi` (firmas finales)

Cada método de usuario recibe `ContextoNegocio` y `Autorizacion` (doble barrera, igual que Negocios). Los métodos internos entre módulos no piden permiso de usuario (como `InventarioApi.aBase`).

```java
public interface VentasApi {
    PreparacionCreada preparar(ContextoNegocio contexto, Autorizacion autorizacion, Comando comando);
    ResultadoComando confirmar(ContextoNegocio contexto, Autorizacion autorizacion, String token);
    Optional<Venta> venta(ContextoNegocio contexto, Autorizacion autorizacion, UUID ventaId);
    PaginaVentas ventas(ContextoNegocio contexto, Autorizacion autorizacion, FiltroVentas filtro, int primeros, String despues);
    ResumenTurno resumenTurno(ContextoNegocio contexto, UUID turnoId);              // interno: lo usa Caja (VF-03)
    List<ProductoParaVenta> productosParaVenta(ContextoNegocio contexto, Autorizacion autorizacion, String texto, int primeros);
}

public interface CajaApi {
    PreparacionCreada preparar(ContextoNegocio contexto, Autorizacion autorizacion, Comando comando);
    ResultadoComando confirmar(ContextoNegocio contexto, Autorizacion autorizacion, String token);
    Optional<Turno> turnoActivo(ContextoNegocio contexto, Autorizacion autorizacion, UUID cajaId);
    SaldoTurno saldoTurno(ContextoNegocio contexto, Autorizacion autorizacion, UUID turnoId);
    PaginaMovimientos movimientos(ContextoNegocio contexto, Autorizacion autorizacion, UUID turnoId, int primeros, String despues);
    PaginaCortes cortes(ContextoNegocio contexto, Autorizacion autorizacion, FiltroCortes filtro, int primeros, String despues);
    List<Caja> cajas(ContextoNegocio contexto, Autorizacion autorizacion);
}
```

Se **eliminan** `AutorizacionVentas` y `AutorizacionCaja` (puertos de permisos simulados) y `PermisosSimulados`.

### 9.2 El SPI de productos (D-128) y otros puertos

```java
// compartido.catalogo
public interface FuenteDeProductos {
    /** Precios vigentes de los productos pedidos, solo del negocio indicado. Los que no conoce no aparecen. */
    Map<UUID, ProductoParaVenta> preciosParaVenta(UUID negocioId, Set<UUID> productoIds);   // ≤ 100
    /** Búsqueda por texto para el cajero. Solo productos activos. */
    List<ProductoParaVenta> buscar(UUID negocioId, String texto, int limite);                // limite ≤ 50
}
```

- `ProductoParaVenta(productoId, descripcion, unidad, precio, admiteFraccion, activo)` pasa a `compartido.catalogo`. Lo propio del giro llega como **datos**: unidad y si admite fracción (D-114).
- Ventas inyecta `List<FuenteDeProductos>` y las compone (`CatalogoCompuesto`): para cada id, la primera fuente que lo conoce. Un id que ninguna conoce es `PRODUCTO_NO_ENCONTRADO`. Dos fuentes que reclamen el mismo id con datos distintos es un error de programación y se registra.
- El `negocioId` **siempre** viene del contexto; nunca de un argumento (misma regla que `CatalogoDeVentas`).
- **Cupones:** el puerto `CuponesDePasaporte` se queda; el adaptador por omisión es `CuponesNoDisponibles` (todo código → `CUPON_INVALIDO`, D-134).
- **Reglas de Caja:** `ReglasCaja.umbralDiferencia` se queda; el valor inicial 50.00 viene de configuración (`caja.umbral-diferencia`) hasta que existan las reglas versionadas.
- **Verificador de turno:** `VerificadorDeTurno` sigue como pregunta de Ventas que contesta Caja (D-120).

### 9.3 La verificación de permiso en `compartido.comandos`

`ManejadorComando<C>` gana un método con valor por omisión nulo:

```java
default String permiso(C comando) { return null; }   // null = ninguno; el manejador lo declara
```

`ServicioPreparacion.preparar(contexto, autorizacion, comando)` y `confirmar(contexto, autorizacion, token)` llaman a `autorizacion.exigir(permiso)` **antes** de validar y, en `confirmar`, **antes de marcar el token como usado**. Efecto: si a una persona le quitan el rol entre los dos pasos, recibe `PERMISO_DENEGADO` y el token **no se gasta**. Para `registrarMovimientoCaja` el manejador devuelve `caja.retiro.registrar` cuando `tipo = RETIRO`, y para `cerrarTurno` con `autorizar = true` devuelve `caja.corte.autorizar` (si el comando exige dos permisos, el manejador comprueba el segundo en `preparar` y `confirmar`).

---

## 10. Eventos

Sobre común: `SobreEvento` de `compartido` (`id` UUIDv7, `tipo`, `version`, `negocio_id`, `origen`, `ocurrido_en` UTC, `correlacion_id`, `causa_id`, `datos`). Dinero y cantidades como cadena decimal. **Sin datos personales.** Solo se agregan campos opcionales (R5). Todo consumidor es idempotente (R3) y lee con **lector tolerante**.

### 10.1 Los que publica Ventas (contrato congelado, D-111)

| Evento | Cuándo | `datos` |
|---|---|---|
| `venta.registrada` v1 | Al confirmar `registrarVenta`, en la misma transacción | `venta_id`, `folio` (entero), `serie`, `turno_id`, `cliente_id?`, `cupon_id?`, `subtotal`, `descuento`, `total` (neto de cupón, D-65), `lineas[]` (`linea`, `producto_id`, `cantidad`, `unidad`, `precio_unitario`, `importe`, `serie?`), `pagos[]` (`metodo`, `monto`) |
| `venta.anulada` v1 | Al confirmar `anularVenta` | `venta_id`, `turno_id`, `motivo?`, `lineas[]` (`linea`, `producto_id`), `pagos[]` (`metodo`, `monto`) |

`origen = "ventas"`, `correlacion_id = venta_id`. No llevan quién cobró, ni lo recibido, ni el cambio.

### 10.2 Los que publica Caja

| Evento | Cuándo | `datos` |
|---|---|---|
| `caja.turno_abierto` v1 | Al confirmar `abrirTurno` | `turno_id`, `caja_id`, `fondo_inicial` |
| `caja.corte_confirmado` v1 | Al confirmar `cerrarTurno` | `corte_id`, `turno_id`, `caja_id`, `esperado_por_metodo`, `contado_por_metodo`, `diferencia_efectivo`, `abierto_en`, `cerrado_en` |
| `caja.diferencia_detectada` v1 | Diferencia sobre el umbral (CJ-08) o venta tardía (CJ-11, P2) | `turno_id`, `caja_id`, `diferencia`, `umbral`, `motivo` |

`origen = "caja"`, `correlacion_id = turno_id`, sin `cajero_id`.

### 10.3 Quién consume qué

| Consumidor | Evento | Qué hace | Estado |
|---|---|---|---|
| Caja | `venta.registrada`, `venta.anulada` | Movimientos `VENTA` y `ANULACION` por método (CJ-03, CJ-04) | Existe; pasa a `@ApplicationModuleListener` en F5 |
| Clientes | `venta.registrada`, `venta.anulada` | Aplica o revierte la compra si trae `cliente_id` | Existe el lector (`clientes/contratos`) y su prueba de contrato |
| Inventario | `venta.registrada`, `venta.anulada` | Descuenta productos con vínculo directo | **Pendiente** (consumo atómico K-01 a K-04 del diseño de Inventario) |
| Misceláneos | ambos | Vínculo directo de sus productos con Inventario | **Pista B** |
| Restaurantes | ambos | Descuento por receta | **Pista C** |
| Contabilidad | `venta.registrada`, `caja.corte_confirmado` | Póliza de ingreso `[?]` | Fuera de alcance |

### 10.4 Reglas para quien consume

1. Define su **propio** lector (`contratos/VentaRegistradaV1`), sin importar tipos de `ventas`. Ignora campos que no conoce y tolera los opcionales ausentes.
2. Idempotente por `(consumidor, evento_id)` y, cuando aplica, por `venta_id`.
3. `venta.anulada` que llega **antes** de `venta.registrada` deja una marca `ANULADO_PREVIO` y la venta tardía no suma (D-19).
4. Un evento que no se pudo aplicar **no** se marca como procesado (D-122) y deja un aviso con tipo e id, nunca con sus datos.
5. Debe existir una **prueba de contrato** que construya el evento con `VentaRegistrada(...).datos()` real y compruebe que el lector lo entiende (patrón de `ContratoVentasConsumidoTest`).

---

## 11. Cómo consumen los verticales (Misceláneos y Restaurantes)

### 11.1 Los tres puntos de contacto

| # | Punto | Quién llama a quién | Qué entrega el vertical |
|---|---|---|---|
| 1 | **Productos** | Ventas → vertical (por el SPI) | Una implementación de `FuenteDeProductos` |
| 2 | **Eventos** | Ventas → bus → vertical | Un listener idempotente de `venta.registrada` y `venta.anulada` |
| 3 | **Comandos propios** (si los tiene) | App → despachador → vertical | Un `ModuloDeComandos` con su prefijo y variantes |

La app **cobra llamando a GraphQL** (`prepararComando(registrarVenta)`), no pasando por el vertical. El vertical no escribe ventas.

### 11.2 Qué trae cada uno

**Misceláneos** (`miscelaneos`, venta a granel)
- Productos con unidad (`kg`, `pieza`, `l`) y `admiteFraccion` verdadero para granel; precio al público. Subcategorías de miscelánea (D-109).
- `FuenteDeProductos` de sus productos.
- Listener de `venta.registrada`: descuenta por **vínculo directo** (D-17) contra Inventario. Depende del consumo atómico de Inventario (pendiente); mientras tanto escribe el listener contra un puerto con doble.

**Restaurantes** (`restaurantes`, recetas)
- Platillos como productos (`pieza`, sin fracción); precio del menú.
- `FuenteDeProductos` de sus platillos.
- Listener de `venta.registrada`: descuenta los insumos de la **receta** (D-09, D-18) con `InventarioApi.aBase`. Las recetas viven en Restaurantes.
- `venta.anulada`: revierte el consumo con el costo original.

### 11.3 Cómo se prueba que cumplen (el «kit de contrato»)

| Prueba | Qué comprueba | Dónde vive |
|---|---|---|
| `ContratoDeFuenteDeProductosTest` (clase abstracta) | `preciosParaVenta` no devuelve productos de otro negocio; un id desconocido no aparece; `buscar` respeta el límite y solo devuelve activos; `admiteFraccion` e `unidad` presentes | `compartido` (pruebas); cada vertical la extiende |
| `ContratoVentaRegistradaV1Test` | El lector del vertical entiende el evento real, con y sin opcionales, y rechaza uno mal formado sin lanzar | cada vertical |
| `ConsumidorIdempotenteTest` | El mismo evento dos veces deja un efecto; `anulada` antes de `registrada` | cada vertical |
| `EstructuraModulosTest` | El vertical solo depende de lo permitido | global |

### 11.4 Lo que **no** deben hacer

- No importar clases de `ventas.dominio` ni `ventas.aplicacion`.
- No leer tablas de Ventas ni de Caja (R1: cada módulo es dueño de su esquema).
- No cobrar ni escribir ventas por Java; el camino es GraphQL.
- No definir tipos GraphQL con nombre genérico (`Producto`, `Movimiento`): llevan prefijo del módulo.
- No tocar `compartido.graphql.**` ni la tabla de otro módulo (sección 13.3).

### 11.5 Qué pueden hacer **desde hoy**, antes de que termine la integración

| Pueden ya | Con qué |
|---|---|
| Diseñar y programar su dominio de productos, precios y fracciones | Este documento (sección 9.2) |
| Escribir su `FuenteDeProductos` y pasar `ContratoDeFuenteDeProductosTest` | Un doble de Ventas, o la interfaz sola |
| Escribir sus listeners y probarlos con el evento de ejemplo | Sección 10.1 y el ejemplo de `docs/diseno/ventas.md` §6.4 |
| Declarar sus permisos (`miscelaneos.*`, `restaurantes.*`) y su migración de permisos | La migración V036/V037 (sección 13.4) |
| Escribir su esquema GraphQL propio (menú, catálogo público) con prefijo | Reglas de la sección 7.1 |

### 11.6 Qué necesitan esperar

| Esperan | Hasta | Porque |
|---|---|---|
| Cobrar de punta a punta por GraphQL | Fin de F3 | Falta el despachador y el esquema final |
| Ver la venta entrar a Caja con outbox real | Fin de F5 | Hoy el bus es en proceso |
| Persistir sus productos y probar RLS junto a Ventas | Fin de F6 | Migraciones de Ventas y Caja |
| Descontar de Inventario | Cuando Inventario tenga consumo por evento | Pendiente en Inventario |

---

## 12. Plan por fases (TDD)

Cada fase: pruebas primero (RED con motivo correcto), código mínimo (GREEN), `mvn clean verify` con JDK 21, mutaciones en los puntos críticos y una entrada en la Bitácora. **Ninguna fase de código empieza sin su autorización escrita en la Bitácora.** No se hace commit ni push sin que usted lo pida.

### F0 · Higiene de la mezcla (≈ 1.5 h)

**Objetivo:** que el árbol quede verde, con Ventas y Caja presentes y probados pero **sin exponer GraphQL** todavía.

| Paso | Acción | Archivos |
|---|---|---|
| 0.1 | Quitar `canal` (código y pruebas) | `canal/**` (13 + 2) |
| 0.2 | Quitar el frontend (si usted acepta, sección 18 P-1) | `frontend/**` (95) |
| 0.3 | Quitar el bloque `negocio360.demo.*` | `application-desarrollo.yml` |
| 0.4 | Mover `ventas.graphqls` y `caja.graphqls` a `resources/graphql-pendiente/` (no se cargan) y quitar `canal.graphqls` | `resources/graphql/` |
| 0.5 | Renumerar `D-76…D-90` → `D-110…D-124` en los archivos de Ventas y Caja | sección 5.1 y anexo A |
| 0.6 | Ajustar `EstructuraModulosTest` (sin `canal`) y `EsquemaGraphqlClientesTest` | pruebas |
| 0.7 | Condicionar los beans de Ventas/Caja a `ventas.almacen=memoria` / `caja.almacen=memoria` (en vez de `demo.habilitada`) y fijar `memoria` en el perfil `desarrollo` | `VentasConfiguracion`, `CajaConfiguracion` |

**RED:** la suite actual ya está en rojo (542 errores, 3 fallas) por la causa conocida.
**GREEN / salida:** `mvn clean verify`: 0 fallas; pruebas de `ventas`, `caja` y `compartido.comandos` en verde; `grep -rn "D-7[6-9]\|D-8[0-9]\|D-90" <archivos de Ventas y Caja>` sin resultados.
**Riesgo:** olvidar una cita a un `D-xx` dentro de un JavaDoc. Mitigación: el `grep` es parte del criterio de salida.

### F1 · Permisos y roles de Ventas y Caja (≈ 2 h)

| Paso | Detalle |
|---|---|
| RED | `PermisosDeVentasTest` y `PermisosDeCajaTest` (nombres, módulo único, ninguno sensible); `RolesBaseTest` con la matriz de la sección 8.2 (una aserción por rol); `EsquemaEquipoPostgresTest` falla porque la migración no existe |
| GREEN | `PermisosDeVentas`, `PermisosDeCaja`, `RolesBase`, `V035__identidad_permisos_de_ventas_y_caja.sql` |
| Salida | Los 11 permisos aparecen en el catálogo real; `RolesBase.verificar` pasa (AZ-03); la migración coincide con `RolesBase` contra Postgres real |
| Mutación | Quitar un permiso de un rol; citar un permiso no declarado |

### F2 · Ventas y Caja con `Autorizacion` y mensajes de plataforma (≈ 5 h)

| Paso | Detalle |
|---|---|
| RED | Pruebas por permiso y por rol (cada comando y cada consulta, con y sin permiso); revocación entre preparar y confirmar (el token **no** se gasta); `RETIRO` sin `caja.retiro.registrar`; `autorizar` sin `caja.corte.autorizar`; aislamiento entre dos negocios con el mismo actor; `PERMISO_DENEGADO` sin decir qué permiso faltó |
| GREEN | `ManejadorComando.permiso(...)`; `ServicioPreparacion` con `Autorizacion`; `VentasApi` y `CajaApi` con las firmas de la sección 9.1; borrar `AutorizacionVentas`, `AutorizacionCaja`, `SIN_PERMISO_ANULAR`; `TraductorDeErroresDeVentas` y `TraductorDeErroresDeCaja` (SPI); `ventas(filtro, …)`, `cortes(filtro, …)` y `movimientos(…)` paginados con cursor en los repositorios en memoria; `DEMASIADAS_CAJAS` |
| Salida | Las ~114 pruebas existentes de Ventas y Caja siguen verdes (se les pasa una `Autorizacion` de prueba; **no se debilita ninguna aserción**); las nuevas pasan |
| Riesgo | Cambiar firmas rompe muchas pruebas: es mecánico. El `Escenario` de cada módulo concentra la creación, así que el cambio es en pocos lugares |

### F3 · Despachador de comandos y GraphQL final (≈ 6 h)

| Paso | Detalle |
|---|---|
| RED | `DespachadorDeComandosTest`: variante desconocida; dos variantes (`@oneOf`); prefijo desconocido → `TOKEN_INVALIDO` sin revelar módulos; `confirmaSinSesion` solo para Negocios; `datos` siempre presente. `OperacionesDeVentasTest` y `OperacionesDeCajaTest` (tabla exacta de la sección 7.3). `GraphQlVentasCajaTest` de punta a punta con **sesiones reales** de Identidad (patrón de `GraphQlEntradaTest`) |
| GREEN | `ModuloDeComandos`, `ComandosGraphQlController`, `ComandosDeVentas`, `ComandosDeCaja`, `ComandosDeNegocios` (migra las reseñas), `VentasGraphQlController`, `CajaGraphQlController`, `OperacionesDeVentas`, `OperacionesDeCaja`; mover los esquemas finales a `resources/graphql/`; `compartido.graphqls` con `MetodoPago` y `MontoPorMetodo` |
| Recorridos de punta a punta | (a) dueño crea caja → abre turno → cobra con efectivo → ve el saldo → anula → corta; (b) cajero cobra pero no anula; (c) cajero no autoriza diferencia; (d) dos negocios no se ven; (e) anónimo y sesión sin negocio negados; (f) token de otro módulo o ajeno; (g) las reseñas anónimas de Negocios siguen funcionando |
| Salida | La suite del equipo (`GraphQlEntradaTest`, 20) intacta; `SG-21` verifica la tabla completa; **ningún contexto de Spring falla** |
| Riesgo alto | Tocar el controlador de Negocios. Mitigación: adaptador que conserva la lógica; sus pruebas son la red de seguridad; aviso a su dueño antes |

### F4 · SPI de productos, cupones y kit de contrato (≈ 3 h)

| Paso | Detalle |
|---|---|
| RED | `ContratoDeFuenteDeProductosTest` (abstracta) con un doble; `CatalogoCompuestoTest` (varias fuentes, id repetido, id desconocido, aislamiento por negocio); `CuponesNoDisponiblesTest`; prueba de `productosParaVenta` por GraphQL |
| GREEN | `compartido.catalogo.{FuenteDeProductos,ProductoParaVenta}`, `CatalogoCompuesto`, `CuponesNoDisponibles`; retirar `CatalogoDeVentas` como puerto propio de Ventas (queda como adaptador interno) |
| Perfil de desarrollo | `FuenteDeProductosDeDesarrollo` con 6 productos rotulados como simulados, **solo** con `ventas.productos-simulados=true` y fuera de `produccion`; sirve mientras los verticales no entregan |
| Salida | Un vertical puede implementar el SPI y pasar el kit sin tocar Ventas |

### F5 · Eventos por outbox y consumidores (≈ 3 h)

| Paso | Detalle |
|---|---|
| RED | Con Postgres real: confirmar una venta deja una fila en `event_publication` y Caja la completa; el mismo evento tres veces deja un efecto; `anulada` antes de `registrada`; si la transacción falla, no sale evento |
| GREEN | `PublicadorEventosEnBus` (Ventas y Caja), `FronteraTransaccional` en `ventasApi` y `cajaApi`, `EscuchaDeVentas` con `@ApplicationModuleListener` |
| Salida | Prueba de contrato con el lector real de Clientes en verde |

### F6 · Persistencia (≈ 7 h) — decisión P-5 en la sección 18

| Paso | Detalle |
|---|---|
| RED | Pruebas contra Postgres: aislamiento por fila de cada tabla (negocio A no ve ni escribe lo de B), folio sin huecos con dos confirmaciones simultáneas, un solo turno abierto por caja (índice parcial), movimientos y líneas **sin UPDATE ni DELETE**, restricciones `CHECK` de dinero, `NUMERIC(14,2)` |
| GREEN | `V040__ventas_esquema.sql` (`venta`, `linea_venta`, `pago_venta`, `serie_folio`, `preparacion`), `V041__caja_esquema.sql` (`caja`, `turno`, `movimiento_caja`, `corte`, `conteo_denominacion`, `venta_aplicada`, `preparacion`, `evento_procesado`), `V042__ventas_caja_rls_forzada.sql`; repositorios JDBC con `EjecutorEnNegocio` (patrón de Clientes); `ventas.almacen=postgres` por omisión |
| Salida | `produccion` **no arranca** si falta el almacén de Ventas o de Caja (H-15); Ventas y Caja funcionan igual con memoria y con Postgres (misma batería de pruebas contra ambos almacenes) |

### F7 · Cierre (≈ 2.5 h)

`mvn -B clean verify` completo; 12 o más mutaciones en los puntos críticos (permiso al confirmar, aislamiento, folio, cupón, corte, despachador, prefijo del token); `ecc:java-reviewer` y `ecc:security-reviewer` de solo lectura con corrección de hallazgos con prueba primero; documento de evidencia `docs/pruebas/ventas-caja-integracion.tdd.md`; tabla de estado de SG-01 a SG-21 para estos módulos; entrada en la Bitácora; actualizar `docs/diseno/ventas.md` y `caja.md` a v0.2 con lo realmente construido.

---

## 13. Trabajo en paralelo

### 13.1 Pistas

| Pista | Quién | Qué | Depende de |
|---|---|---|---|
| **A · Integración de Ventas y Caja** | Esta sesión (julio) | F0 a F7 | Autorización de este plan |
| **B · Misceláneos** | Por asignar | Dominio, `FuenteDeProductos`, listener, permisos, esquema propio | Solo del contrato (secciones 9.2, 10, 11) |
| **C · Restaurantes** | Por asignar | Menú, recetas, `FuenteDeProductos`, listener, permisos, esquema propio | Idem, más `InventarioApi.aBase` |

### 13.2 Hitos de congelamiento

| Hito | Cuándo | Qué queda congelado para B y C |
|---|---|---|
| **H-0 Contratos v1** | Al aprobar este documento | Esquema GraphQL (7.2), eventos v1 (10), SPI `FuenteDeProductos` (9.2), permisos (8.1), reglas de dependencias (6.2). Cambiar algo de aquí exige avisar a B y C |
| **H-1 Permisos vivos** | Fin de F1 | Los roles base ya tienen permisos de Ventas y Caja; B y C agregan los suyos con V036 y V037 |
| **H-2 Cobro de punta a punta** | Fin de F3 | B y C pueden probar un cobro real por GraphQL con sesión real |
| **H-3 Kit de contrato** | Fin de F4 | `ContratoDeFuenteDeProductosTest` disponible; B y C lo extienden |
| **H-4 Outbox real** | Fin de F5 | Los listeners de B y C pueden probarse con entrega real |
| **H-5 Persistencia** | Fin de F6 | Prueba conjunta con RLS |

### 13.3 Propiedad de archivos (para no pisarse)

| Zona | Dueño | Regla |
|---|---|---|
| `ventas/**`, `caja/**`, `compartido/comandos/**`, `compartido/graphql/comandos/**`, `compartido/catalogo/**` | Pista A | B y C **no** los editan; piden el cambio |
| `miscelaneos/**` y su `graphqls` | Pista B | — |
| `restaurantes/**` y su `graphqls` | Pista C | — |
| `identidad/dominio/RolesBase.java` | **Compartido, serializado** | Cada pista agrega sus propias constantes en líneas separadas y su migración; el último en mezclar resuelve el conflicto textual |
| `docs/BITACORA.md` | Todos | Solo se **agrega al final**; los identificadores de decisión nuevos se piden por rango (A: D-110 a D-139; B: D-140 a D-159; C: D-160 a D-179) |
| `negocios/**` | Su dueño | A toca solo el adaptador de comandos y avisa |

### 13.4 Reparto de migraciones (D-133)

| Versión | Contenido | Pista |
|---|---|---|
| V035 | Permisos de Ventas y Caja | A |
| V036 | Permisos de Misceláneos | B |
| V037 | Permisos de Restaurantes | C |
| V038 | Permisos de Inventario | Inventario |
| V039 | Reservada | — |
| V040 | Esquema de Ventas | A |
| V041 | Esquema de Caja | A |
| V042 | RLS forzada de Ventas y Caja | A |
| V050 a V059 | Esquemas de Misceláneos | B |
| V060 a V069 | Esquemas de Restaurantes | C |
| V070 a V079 | Esquema de Inventario | Inventario |

### 13.5 Protocolo de integración

1. Cada pista trabaja en su rama y trae `julio` antes de mezclar (con `git merge`, no `rebase`).
2. Antes de mezclar: `mvn -B clean verify` en verde en su rama.
3. Una pista que necesita un cambio en una zona ajena lo pide por la Bitácora (una línea con el contrato que necesita), no lo edita.
4. Las pistas B y C **no** esperan a F6 para empezar; sí para el cierre conjunto.

---

## 14. Pruebas y validación

| Qué | Cómo | Cuándo |
|---|---|---|
| TDD | RED con el motivo correcto → GREEN mínimo → refactor | cada fase |
| Suite completa | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -B clean verify` | cada fase |
| Estructura | `EstructuraModulosTest` (`ApplicationModules.verify()`) | cada fase |
| Tabla de operaciones | `VerificadorDeOperaciones` al arrancar (SG-21) | cada fase |
| Aislamiento | Dos negocios, mismo actor y mismo token | F2, F3, F6 |
| Permisos | Una prueba por permiso y por rol; revocación entre pasos | F2, F3 |
| Contratos de eventos | Prueba con el evento real contra el lector de cada consumidor | F5 |
| Postgres real | Testcontainers; sin Docker las pruebas de persistencia no corren | F5, F6 |
| Mutaciones | Una por cada barrera: permiso, aislamiento, folio, cupón, corte, prefijo del token, sesión | F7 |
| Revisión | `ecc:java-reviewer` y `ecc:security-reviewer` | F7 |
| JavaDoc (D-117) | `mvn javadoc:javadoc` sin advertencias en lo nuevo | F7 |
| Cobertura | JaCoCo: se mantiene el umbral del proyecto; Ventas ≥ 80 % y Caja ≥ 80 % | F7 |

Comandos:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd backend && mvn -B clean verify
mvn -B test -Dtest='*Ventas*,*Caja*,Despachador*,EstructuraModulosTest'
```

---

## 15. Riesgos

| # | Riesgo | Prob. | Impacto | Señal | Mitigación |
|---|---|---|---|---|---|
| R-01 | Tocar el controlador de Negocios rompe las reseñas del turista | Media | Alto | `GraphQlEntradaTest.resenas_*` en rojo | Adaptador que conserva la lógica; avisar a su dueño; pruebas como red |
| R-02 | Los verticales empiezan contra un contrato que luego cambia | Media | Alto | Cambio en secciones 7, 9.2 o 10 | Hito H-0; cambios por Bitácora con aviso |
| R-03 | Choque de migraciones o de `RolesBase` entre las tres pistas | Alta | Medio | Conflicto al mezclar | Reparto de versiones (13.4); constantes separadas |
| R-04 | Renumerar decisiones deja citas viejas en el código | Media | Bajo | `grep` con resultados | `grep` como criterio de salida de F0 |
| R-05 | Dinero y concurrencia: folio con huecos o duplicado | Baja | Alto | Prueba concurrente roja | Bloqueo de fila en `serie_folio`; prueba con dos hilos; mutación |
| R-06 | Un evento llega a Caja después del corte | Media | Medio | Diferencias inexplicables | CJ-06, CJ-07; CJ-11 (P2) sigue pendiente |
| R-07 | Sin Docker no se pueden probar F5 y F6 | Media | Medio | Testcontainers falla | Encender Docker antes; las pruebas con memoria siguen corriendo |
| R-08 | Se pierde trabajo al reaplicar el stash o al borrar `frontend` | Baja | Alto | Archivos faltantes | Respaldos en archivo (ya hay dos); el frontend sigue en `origin/uriel` |
| R-09 | El calendario: el evento es del 15 al 17 y hoy es 16 | Alta | Alto | — | Etapa 1 (F0 a F4, ≈ 17.5 h) deja a los verticales integrar; F5 y F6 pueden seguir después |
| R-10 | Inventario aún no consume ventas | Alta | Medio | Los verticales no descuentan stock | Es del plan de Inventario; los listeners de B y C se prueban con doble |
| R-11 | Muchos tipos y campos nuevos tocan el límite de costo (100) | Baja | Bajo | `CONSULTA_MUY_COSTOSA` | Prueba que una consulta real de ventas con líneas y pagos cabe |

---

## 16. Criterios de aceptación

- [ ] `mvn -B clean verify` en verde con JDK 21 y la cobertura exigida; ningún dato de prueba sensible en los registros.
- [ ] `grep` de `D-76…D-90` en los archivos de Ventas y Caja: sin resultados; las 15 decisiones aparecen como `D-110…D-124` en la Bitácora, sin repetidos.
- [ ] Ventas y Caja **no** exponen GraphQL hasta que lo hagan con tabla de operaciones, permisos y resolvedores (F3).
- [ ] Los 11 permisos están en el catálogo, en `RolesBase` y en la migración V035, y coinciden contra Postgres real.
- [ ] Cada comando exige su permiso en la puerta **y** en el manejador, también al confirmar; revocar entre pasos no gasta el token.
- [ ] Una sesión sin negocio y una anónima no operan en ninguna operación de Ventas ni de Caja; no existe negocio de demostración.
- [ ] Un negocio no ve ni escribe nada de otro (memoria y Postgres).
- [ ] `prepararComando` y `confirmarComando` tienen **un solo** resolvedor; `confirmaSinSesion` es verdadero solo para Negocios; las reseñas del turista siguen pasando.
- [ ] El esquema es exactamente el de la sección 7.2; la tabla de operaciones es exactamente la de la 7.3 (probado por igualdad).
- [ ] `ventas`, `cortes` y `movimientos` paginan con cursor y topes; `cajas` tiene tope.
- [ ] `venta.registrada` v1 y `venta.anulada` v1 no cambian; Clientes y Caja los leen; hay prueba de contrato.
- [ ] Un vertical de ejemplo (doble) pasa `ContratoDeFuenteDeProductosTest` sin tocar Ventas.
- [ ] Con `produccion`, la aplicación no arranca si falta el almacén de Ventas o de Caja.
- [ ] Documento de evidencia y entrada de Bitácora escritos; `ecc:java-reviewer` y `ecc:security-reviewer` sin hallazgos altos sin atender.

---

## 17. Estimación y calendario

| Fase | Horas (mías, sin medir) | Acumulado |
|---|---|---|
| F0 Higiene | 1.5 | 1.5 |
| F1 Permisos y roles | 2 | 3.5 |
| F2 `Autorizacion` y paginación | 5 | 8.5 |
| F3 Despachador y GraphQL final | 6 | 14.5 |
| F4 SPI de productos y kit | 3 | 17.5 |
| **Etapa 1 (verticales ya integran)** | **17.5** | |
| F5 Outbox y consumidores | 3 | 20.5 |
| F6 Persistencia | 7 | 27.5 |
| F7 Cierre | 2.5 | 30 |
| **Total** | **30** | |

Con 6 horas efectivas al día son unos **5 días**; la Etapa 1 son unos **3 días**. Con colchón de 25 % para el riesgo de Negocios y de migraciones, **la Etapa 1 son 4 días**. Es una estimación de una sola persona trabajando en serie; las pistas B y C no suman a este costo porque corren a la par.

**Orden de recorte** si falta tiempo (de lo último a lo primero): 1) F6 (se queda en memoria con perfil y la advertencia de la sección 15, R-07); 2) F5; 3) `productosParaVenta` y `resumenTurno` por GraphQL; 4) paginación de `movimientos` (se deja tope fijo). **No se recorta** F1, F2 ni el despachador: sin ellos el dinero no tiene control.

---

## 18. Decisiones que necesito de usted

| # | Pregunta | Mi recomendación |
|---|---|---|
| **P-1** | ¿Quito `canal` y `frontend` de `julio` en un commit aparte (sección 3.3, opción A)? | Sí. Siguen en `uriel`. |
| **P-2** | ¿Aprueba la renumeración `D-76…D-90` → `D-110…D-124` para Ventas y Caja? | Sí. |
| **P-3** | ¿Aprueba los 11 permisos y la matriz de roles (sección 8)? En especial: ¿anular, retirar y autorizar diferencias solo para gerente, administrador y dueño? ¿El mesero no cobra? | Sí. |
| **P-4** | ¿Acepta las queries de la sección 7: sin `ventasDelTurno`, con `ventas(filtro)`, tipos con prefijo, `MetodoPago` en `compartido`? | Sí. |
| **P-5** | ¿Persistencia ahora (F6) o la dejamos para después de la Etapa 1? | Después: los verticales no la necesitan para integrarse. |
| **P-6** | ¿El SPI `FuenteDeProductos` en `compartido.catalogo` (D-128)? | Sí. |
| **P-7** | ¿Sin Pasaporte, todo cupón es inválido (D-134), y un perfil de desarrollo con 6 productos simulados? | Sí. |
| **P-8** | ¿Quién mantiene Negocios, para avisarle del adaptador de comandos (R-01)? | Avisarle antes de F3. |
| **P-9** | ¿Quién toma Misceláneos y quién Restaurantes (pistas B y C)? | Personas distintas a la que hace A. |
| **P-10** | ¿Se aprueban los rangos de decisiones y de migraciones de la sección 13? | Sí. |

**ESPERANDO CONFIRMACIÓN.** Un «sí» acepta el plan y mis recomendaciones P-1 a P-10; indique cuáles cambia. Empiezo por F0.

---

## 19. Fuera de alcance

Módulos Catálogo, Pasaporte, Contabilidad, Museos; el frontend de Flutter; el bot; facturación e IVA desglosado; devoluciones parciales; comprobante en PDF; webhooks de proveedores de pago; conteo por denominación (P2) y CJ-11 (venta tardía a un turno cerrado, P2); las suscripciones `ventaRegistrada` y `turnoEnVivo` (P2); la confirmación reforzada (IDN-3.2) y, con ella, marcar `anular` y `autorizar` como sensibles; el consumo por evento de Inventario; unificar los tres `ServicioPreparacion` (HTC-89); la conexión Identidad↔Inventario (tiene su propio plan); cualquier cambio en Jira; **cualquier commit o push sin que usted lo pida**.

---

## Anexo A · Mapa de archivos

| Ruta | Acción | Fase |
|---|---|---|
| `backend/.../canal/**` (13) y `.../test/.../canal/**` (2) | BORRAR | F0 |
| `backend/.../resources/graphql/canal.graphqls` | BORRAR | F0 |
| `frontend/**` (95) | BORRAR de `julio` (sigue en `uriel`) | F0 |
| `backend/.../resources/application-desarrollo.yml` | EDITAR (quitar `negocio360.demo.*`, agregar `ventas.almacen`, `caja.almacen`) | F0 |
| `backend/.../resources/graphql/{ventas,caja}.graphqls` | MOVER a `graphql-pendiente/` y luego rehacer | F0, F3 |
| `backend/.../ventas/infraestructura/VentasConfiguracion.java`, `caja/infraestructura/CajaConfiguracion.java` | EDITAR (condición, `FronteraTransaccional`, bus) | F0, F5, F6 |
| `docs/diseno/{ventas,caja}.md`, `docs/planes/ventas-caja-v1.md`, `docs/mockups/ventas-caja.html`, `docs/pruebas/ventas-caja-f2-f3.tdd.md` | EDITAR (renumerar decisiones) | F0 |
| `docs/BITACORA.md` | EDITAR (renumerar las filas de `uriel`, agregar entradas) | F0 en adelante |
| `ventas/seguridad/PermisosDeVentas.java`, `caja/seguridad/PermisosDeCaja.java` | CREAR | F1 |
| `identidad/dominio/RolesBase.java` | EDITAR | F1 |
| `resources/db/migration/V035__identidad_permisos_de_ventas_y_caja.sql` | CREAR | F1 |
| `compartido/comandos/{ManejadorComando,ServicioPreparacion}.java` | EDITAR (`permiso`, `Autorizacion`) | F2 |
| `ventas/VentasApi.java`, `caja/CajaApi.java` y sus servicios, manejadores y repositorios en memoria | EDITAR | F2 |
| `ventas/aplicacion/puertos/AutorizacionVentas.java`, `caja/aplicacion/puertos/AutorizacionCaja.java`, `canal/demo/PermisosSimulados.java` | BORRAR | F2 |
| `ventas/graphql/TraductorDeErroresDeVentas.java`, `caja/graphql/TraductorDeErroresDeCaja.java` | CREAR | F2 |
| `compartido/graphql/comandos/{ModuloDeComandos,ComandosGraphQlController,…}.java` | CREAR | F3 |
| `ventas/graphql/{ComandosDeVentas,VentasGraphQlController,OperacionesDeVentas}.java`, `caja/graphql/{ComandosDeCaja,CajaGraphQlController,OperacionesDeCaja}.java` | CREAR | F3 |
| `negocios/web/{ComandosDeNegocios}.java`, `PublicoGraphQlController.java` | CREAR / EDITAR (migrar `prepararComando` y `confirmarComando`) | F3 |
| `resources/graphql/{compartido,ventas,caja}.graphqls` | EDITAR / CREAR (sección 7.2) | F3 |
| `compartido/catalogo/{FuenteDeProductos,ProductoParaVenta}.java`, `ventas/infraestructura/CatalogoCompuesto.java`, `CuponesNoDisponibles.java` | CREAR | F4 |
| `ventas/infraestructura/PublicadorEventosEnBus.java`, `caja/infraestructura/PublicadorEventosEnBus.java`, `EscuchaDeVentas.java` | CREAR / EDITAR | F5 |
| `resources/db/migration/V040…V042` y repositorios JDBC | CREAR | F6 |
| `docs/pruebas/ventas-caja-integracion.tdd.md` | CREAR | F7 |

## Anexo B · Resumen de lo que cambia para quien ya usaba lo de `uriel`

| Antes | Ahora |
|---|---|
| Se encendía con `negocio360.demo.habilitada=true` | `ventas.almacen=memoria` y `caja.almacen=memoria` (o `postgres`) |
| Una sesión sin negocio operaba en un negocio de demostración | Recibe `SIN_NEGOCIO_ACTIVO` |
| Cualquier cuenta anulaba y autorizaba | Permisos reales por rol |
| `SIN_PERMISO_ANULAR` | `PERMISO_DENEGADO` |
| `ventasDelTurno(turnoId)` | `ventas(filtro: { turnoId })` |
| `Corte`, `TipoMovimiento` | `CorteDeCaja`, `TipoMovimientoCaja` |
| El token de comando llevaba `ventas.` o `caja.` por el canal | El mismo prefijo, pero lo reparte el despachador de la plataforma |
| Productos y cupones simulados en el canal | `FuenteDeProductos` de los verticales; cupones inválidos hasta que exista Pasaporte |

## Anexo C · Lista de verificación para los verticales (entregar a las pistas B y C)

- [ ] Mi módulo depende solo de lo permitido en 6.2.
- [ ] Implemento `FuenteDeProductos` y paso `ContratoDeFuenteDeProductosTest`.
- [ ] Mis productos tienen `unidad` y `admiteFraccion`; el precio es el final al público.
- [ ] Mi listener lee `venta.registrada` v1 y `venta.anulada` v1 con lector propio y tolerante.
- [ ] Mi listener es idempotente y maneja `anulada` antes de `registrada`.
- [ ] Tengo prueba de contrato con el evento real.
- [ ] Mis tipos GraphQL llevan prefijo y ningún campo de entrada recibe `negocioId`.
- [ ] Mis permisos están declarados como datos (`modulo.recurso.accion`) y en mi migración.
- [ ] No leo tablas de Ventas ni de Caja ni importo sus clases internas.
- [ ] Mis cambios en `RolesBase` y en la Bitácora son aditivos y separados.
