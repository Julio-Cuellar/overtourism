[[[[[[[[[]()]()]()]()]()]()]()]()]()# Diseño del módulo de Inventario

**Versión:** 0.1 (borrador para revisión del equipo)
**Fecha:** 2026-10-08
**Estado:** solo diseño. No existe código. Nadie debe programar contra este documento hasta que el equipo lo apruebe (ver sección 13).
**Contexto de decisiones:** `docs/BITACORA.md` (los identificadores `D-xx` remiten a ahí).

Convención de estado en las tablas: **[A]** aceptado por el equipo, **[P]** propuesto y vigente salvo objeción, **[?]** abierto.

---

## 1. Objetivo

Inventario es un módulo **horizontal**: lleva existencias, costos, lotes, caducidades y números de serie por negocio, y lo hace igual para una fonda, la tienda de un museo o un misceláneo. No sabe qué es un platillo ni una entrada de museo. Las recetas viven en el vertical **Restaurantes** y descuentan insumos llamando a la API pública de Inventario.

Stack: Java 21, Spring Boot, Spring Modulith, Spring for GraphQL, Postgres. [A]

---

## 2. Reglas de arquitectura

| # | Regla | Estado |
|---|---|---|
| R1 | Cada módulo es dueño de sus datos. Otro módulo solo los obtiene por la **API pública** del dueño o por el **bus de eventos**. "API" es una interfaz Java dentro del mismo proceso, no HTTP. Cada módulo tiene su propio esquema de Postgres. Sin joins, FKs ni repositorios entre esquemas de módulos distintos. | [A] D-04 |
| R2 | Los verticales se comunican con los horizontales así: **síncrono = llamada a la API pública; asíncrono = evento por el bus**. Un evento anuncia un hecho ya ocurrido y no devuelve respuesta. | [A] D-05 |
| R3 | Todo evento lleva un **ID único** (UUIDv7) generado por el productor. Los consumidores son **idempotentes** porque la entrega es al menos una vez. | [A] D-06 |
| R4 | Las dependencias van solo de vertical a horizontal. Un horizontal nunca importa, llama ni conoce a un vertical. | [A] D-07 |
| R5 | Los eventos son contrato público y versionado. Se pueden agregar campos opcionales; no se quitan ni se cambian. Un cambio incompatible crea la versión siguiente del evento. | [A] D-07 |
| R6 | Entre horizontales rige la misma regla que R2: síncrono = API, asíncrono = evento. | [A] D-07 |

### Dependencias permitidas (alcance de este documento)

| Módulo | Puede llamar a la API de | Escucha eventos de | Publica |
|---|---|---|---|
| Inventario (horizontal) | Catálogo | `venta.*` | `inventario.*` |
| Restaurantes (vertical) | Inventario, Catálogo | `venta.*` | `restaurantes.*` (después del MVP) |
| Contabilidad (horizontal) | por definir | `venta.*`, `inventario.movimiento_registrado` | `contabilidad.*` |
| Catálogo y Ventas (horizontales) | fuera de este documento | | |

Inventario **no** depende de Restaurantes ni de Ventas. Solo escucha eventos de Ventas. Spring Modulith verifica esto en una prueba (`ApplicationModules.verify()`), que debe estar en verde desde la fase 0.

---

## 3. Alcance

### Qué es de Inventario
Artículos (insumos o mercancía), unidades y conversiones, movimientos, saldos, costo promedio ponderado (CPP), lotes y caducidades, números de serie, alertas y políticas del negocio.

### Qué no es de Inventario
| Cosa | Dueño |
|---|---|
| Menú, productos y precios de venta | Catálogo |
| Recetas y versiones de receta | Restaurantes |
| Ventas | Ventas |
| Costo contable y asientos | Contabilidad |

### MVP y orden de recorte [A] D-08, D-12
Si falta tiempo se recorta desde abajo.

| Prioridad | Funcionalidad |
|---|---|
| 1 | Artículos, movimientos, saldo, kardex, preparar → confirmar |
| 1 | Conversión de unidades |
| 1 | CPP |
| 1 | Recetas y descuento por platillo (en Restaurantes) |
| 2 | Lotes, caducidades y salida FEFO |
| 3 | Números de serie |

### Fuera del MVP
Varios almacenes y transferencias, proveedores y órdenes de compra, subrecetas, recálculo retroactivo del CPP (y cualquier política de costeo distinta de `SIN_RECALCULO`), códigos de barras, pronóstico de reabasto, marcar platillos como agotados en la app del turista.

---

## 4. Reglas de dominio

Cada regla tiene un identificador para que las pruebas la citen.

### 4.1 Unidades (U)
| ID | Regla |
|---|---|
| U-01 | Cada artículo tiene una **unidad base** y todo saldo y movimiento se guarda en ella. La unidad base y el tipo de rastreo no se pueden cambiar cuando el artículo ya tiene movimientos. |
| U-02 | Unidades estándar (catálogo global): `g`, `kg` (masa, base g); `ml`, `l` (volumen, base ml); `pieza` (conteo). Convierten solo dentro de su misma dimensión. |
| U-03 | **Unidad propia del artículo:** nombre y factor a la unidad base, por ejemplo `caja = 24 pieza` o `manojo = 250 g`. Puede cruzar dimensiones. Al resolver una unidad se busca primero la propia y luego la estándar. |
| U-04 | Si una unidad no se puede convertir a la base del artículo, se rechaza con `UNIDAD_NO_CONVERTIBLE`. Nunca se adivina una conversión. |
| U-05 | Cada movimiento guarda lo capturado (cantidad y unidad), el factor aplicado y la cantidad en unidad base, para que la conversión sea auditable. |

### 4.2 Costeo (C) [A] D-11
| ID | Regla |
|---|---|
| C-01 | El saldo de un artículo guarda `cantidad_base` y `cpp`. El valor del inventario es `cantidad_base × cpp`. |
| C-02 | **Entrada** con saldo anterior > 0: `cpp_nuevo = (saldo × cpp + cantidad × costo) / (saldo + cantidad)`. |
| C-03 | **Entrada** con saldo anterior ≤ 0: `cpp_nuevo = costo de la entrada`. |
| C-04 | **Salida:** se valúa al CPP vigente y el CPP no cambia. El movimiento guarda el costo aplicado y nunca se modifica. |
| C-05 | **Reversión de una salida:** reingresa al costo con que salió (el del movimiento original), no al CPP actual. |
| C-06 | **Ajuste por conteo:** una diferencia positiva entra y una negativa sale, ambas al CPP vigente. |
| C-07 | **Sin recálculo retroactivo:** una entrada con fecha pasada no reescribe el historial. Los errores se corrigen con movimientos nuevos. La política es configurable (`politica_costeo`), pero en el MVP solo existe el valor `SIN_RECALCULO`. |
| C-08 | El costo de una entrada se captura **por unidad capturada** (180 por kg) y se convierte a costo por unidad base. El costo puede ser 0, no negativo. |
| C-09 | Limitación conocida: como el valor se calcula como `cantidad × cpp` y cada movimiento guarda su costo redondeado, la suma de los costos de movimientos puede diferir centavos del valor del inventario. Se documenta, no se corrige en el MVP. |

### 4.3 Redondeo [P] D-20
Una sola utilidad de redondeo en `compartido`. Modo **HALF_UP**. Cantidades base con 4 decimales, CPP y costo unitario con 4, y dinero (costo total, valor) con 2. Los factores de conversión con 8. Ningún cálculo usa `double` ni `float`.

### 4.4 Salidas y lotes (L) [A] D-10
| ID | Regla |
|---|---|
| L-01 | Rastreo por artículo: `NINGUNO`, `LOTE` o `SERIE`. Son excluyentes. |
| L-02 | Entrada de un artículo con lote: lleva `codigo` de lote y `caduca_el` opcional (fecha del calendario local de México). Recibir el mismo código otra vez suma al lote existente; si trae otra caducidad, se rechaza con `LOTE_FECHA_DISTINTA`. |
| L-03 | `politica_salida` del negocio: `FEFO` (por defecto) o `MANUAL`, con excepción opcional por artículo. Con `FEFO` el sistema asigna lotes ordenados por caducidad ascendente (sin caducidad al final) y luego por antigüedad. Con `MANUAL` la salida indica el lote. |
| L-04 | Una salida puede repartirse en varios lotes. El reparto queda en `movimiento_lote`. |
| L-05 | **Lotes caducados no se asignan automáticamente** [P] D-21. Deben darse de baja por merma. |
| L-06 | Lo que no cubre ningún lote vigente queda como **consumo sin lote** (`movimiento.cantidad_sin_lote`) y publica `inventario.consumo_sin_lote`. La salida nunca se bloquea. |
| L-07 | Una tarea diaria (hora local de México) publica `inventario.lote_por_caducar` e `inventario.lote_caducado`. Es idempotente por lote y fecha de aviso. Los días de aviso son configurables por negocio. |
| L-08 | Los caducados generan una **propuesta** de merma (preparar → confirmar). Nunca se registra sola. |
| L-09 | El costo se lleva por artículo, no por lote. El lote solo rastrea cantidad y fecha. |

### 4.5 Números de serie (S)
| ID | Regla |
|---|---|
| S-01 | Solo artículos con unidad base `pieza` y cantidades enteras. Una entrada de N piezas trae N series, únicas por negocio y artículo. |
| S-02 | Estados: `DISPONIBLE`, `VENDIDA`, `BAJA`, con historial en `movimiento`. |
| S-03 | Una serie vendida debe existir y estar `DISPONIBLE`. No hay stock negativo para artículos serializados. |
| S-04 | Una venta de un artículo serializado **sin serie** se registra igual y deja una tarea `tarea_serie` más el evento `inventario.serie_sin_asignar`. |
| S-05 | Un artículo con rastreo `SERIE` no puede ser insumo de una receta. |

### 4.6 Saldos y alertas (A) [A] D-13
| ID | Regla |
|---|---|
| A-01 | **Se permite stock negativo.** Una venta nunca se bloquea por un error de inventario. |
| A-02 | Las alertas se disparan **al cruzar el umbral**, no en cada movimiento. `bajo_minimo` al pasar de saldo > mínimo a ≤ mínimo; `repuesto` al volver a estar por encima; `saldo_negativo` al pasar de ≥ 0 a < 0. |
| A-03 | El saldo no se edita a mano. Se corrige con **ajuste por conteo**: el usuario captura lo contado y el sistema calcula la diferencia. |
| A-04 | Las bajas de artículos son lógicas y solo se permiten con saldo 0. |

### 4.7 Consumo (K) [P] D-18, D-19
| ID | Regla |
|---|---|
| K-01 | `registrarConsumo` es idempotente por `(negocio, causaId, consumidor)`. Repetir la llamada devuelve el resultado guardado y no vuelve a descontar. |
| K-02 | La llamada es atómica: o se descuentan todas las líneas o ninguna. |
| K-03 | `revertirConsumo` es idempotente por la misma llave y solo se puede aplicar una vez. Crea movimientos compensatorios al costo original y repone los mismos lotes. |
| K-04 | **Anulación antes del registro:** como no hay orden global de eventos, si llega `revertirConsumo` sin consumo previo se guarda una **marca** (`consumo.estado = ANULADO_PREVIO`). Si después llega el consumo, se omite y devuelve `OMITIDO_ANULADO_PREVIO`. |
| K-05 | Vínculo directo vs. receta: un producto tiene **modo de inventario** `NINGUNO`, `VINCULO_DIRECTO` o `RECETA`, y el dueño del dato es **Catálogo** [P] D-17. Cada módulo solo procesa las líneas de su modo. |

---

## 5. Datos

Todas las llaves primarias son UUIDv7. Toda tabla lleva `negocio_id` y una política de seguridad por fila (RLS) basada en la variable de sesión `app.negocio_id`, que fija la infraestructura al abrir la transacción. Migraciones con Flyway, una carpeta por módulo. Cantidades `NUMERIC(14,4)`, dinero `NUMERIC(14,2)`, costos unitarios `NUMERIC(14,4)`, factores `NUMERIC(18,8)`.

### 5.1 Esquema `inventario`

| Tabla | Columnas principales | Restricciones y notas |
|---|---|---|
| `unidad` (global) | `codigo` PK, `dimension` (MASA, VOLUMEN, CONTEO), `factor_a_dimension` | Datos iniciales: g=1, kg=1000, ml=1, l=1000, pieza=1 |
| `articulo` | `id`, `negocio_id`, `nombre`, `categoria`, `unidad_base`, `rastreo`, `producto_id` (nulo, sin FK), `minimo_base`, `politica_salida_override` (nulo), `activo`, `version`, `creado_en`, `actualizado_en` | Único `(negocio_id, lower(nombre))` entre activos. Único `(negocio_id, producto_id)` si no es nulo. Bloqueo optimista con `version` |
| `unidad_articulo` | `articulo_id`, `nombre`, `factor_a_base` | PK `(articulo_id, nombre)`. Factor > 0 |
| `saldo` | `articulo_id` PK, `negocio_id`, `cantidad_base`, `cpp`, `ultimo_movimiento_id`, `version` | Se actualiza en la misma transacción que el movimiento |
| `movimiento` | `id`, `negocio_id`, `articulo_id`, `tipo`, `cantidad_capturada`, `unidad_capturada`, `factor_aplicado`, `cantidad_base` (con signo), `cantidad_sin_lote`, `costo_unitario`, `costo_total`, `cpp_resultante`, `saldo_resultante`, `causa_tipo`, `causa_id`, `consumidor`, `compensa_a`, `ocurrido_en`, `registrado_en`, `registrado_por` | **Solo inserción** (sin UPDATE ni DELETE). `tipo`: ENTRADA, SALIDA, MERMA, AJUSTE_POS, AJUSTE_NEG. `causa_tipo`: VENTA, COMPRA, CONTEO, MERMA, MANUAL, REVERSION |
| `lote` | `id`, `negocio_id`, `articulo_id`, `codigo`, `caduca_el` (DATE, nulo), `cantidad_actual`, `movimiento_entrada_id`, `creado_en` | Único `(negocio_id, articulo_id, codigo)` |
| `movimiento_lote` | `movimiento_id`, `lote_id`, `cantidad_base` (con signo) | PK `(movimiento_id, lote_id)` |
| `numero_serie` | `id`, `negocio_id`, `articulo_id`, `serie`, `estado`, `mov_entrada_id`, `mov_salida_id`, `actualizado_en` | Único `(negocio_id, articulo_id, serie)` |
| `tarea_serie` | `id`, `negocio_id`, `causa_id` (venta), `linea`, `articulo_id`, `resuelta_en` | Pendientes de asignar serie |
| `consumo` | `id`, `negocio_id`, `causa_id`, `consumidor`, `estado` (REGISTRADO, REVERTIDO, ANULADO_PREVIO), `resultado` (JSON), `creado_en` | Único `(negocio_id, causa_id, consumidor)` |
| `preparacion` | `token_hash` PK, `negocio_id`, `actor`, `comando_tipo`, `comando_hash`, `resumen` (JSON), `expira_en`, `usado_en` | Token de un solo uso. Solo se guarda el hash |
| `politica_negocio` | `negocio_id` PK, `politica_salida` (FEFO por defecto), `politica_costeo` (SIN_RECALCULO), `dias_aviso_caducidad`, `version`, `actualizado_en` | Los cambios van a la auditoría de la plataforma |
| `evento_procesado` | `consumidor`, `evento_id`, `procesado_en` | PK `(consumidor, evento_id)`. Se purga pasados 90 días |

Infraestructura compartida: la tabla de publicación de eventos de Spring Modulith (outbox) es de la plataforma, no de un módulo, y es la única excepción a R1.

### 5.2 Esquema `restaurantes` (para que Inventario conozca el contrato)

| Tabla | Columnas principales | Notas |
|---|---|---|
| `receta` | `id`, `negocio_id`, `producto_id`, `activa` | Una receta por producto |
| `receta_version` | `id`, `receta_id`, `numero`, `vigente_desde`, `vigente_hasta` (nulo) | Una venta usa la versión vigente en su `ocurrido_en` |
| `receta_insumo` | `receta_version_id`, `articulo_id` (ID opaco, sin FK), `cantidad`, `unidad`, `merma_pct` (0 a 100) | La unidad se valida con `InventarioApi.aBase` al guardar |
| `consumo_fallido` | `id`, `negocio_id`, `venta_id`, `codigo`, `detalle`, `creado_en` | Para errores permanentes que no se reintentan |
| `evento_procesado` | igual que arriba | Idempotencia de sus listeners |

---

## 6. Contrato de la API pública (`InventarioApi`)

Reglas generales:
- `negocio_id` **nunca** es parámetro. Sale de `ContextoNegocio` (sesión o webhook).
- Respuesta de toda operación: `{ ok, codigo, datos, mensaje_plantilla }`, como fija el `CLAUDE.md`.
- Los tipos de salida son records inmutables (`ArticuloVista`, `SaldoVista`, `MovimientoVista`, `LoteVista`, `SerieVista`).
- Las listas son paginadas por cursor, con máximo 100 por página.

### 6.1 Consultas

| Operación | Entrada | Salida | Quién la usa |
|---|---|---|---|
| `articulo` | `articuloId` | `ArticuloVista` | Panel, Restaurantes |
| `articulos` | `filtro` (texto, categoría, solo alertas), cursor | página de `ArticuloVista` | Panel |
| `saldo` | `articuloId` | `SaldoVista` (cantidad base, unidad, cpp, valor, bajo mínimo) | Panel |
| `saldos` | `ids` (máx. 100) | mapa id → `SaldoVista` | Resolvedores GraphQL |
| `cpp` | `ids` (máx. 100) | mapa id → CPP | Restaurantes (costo teórico) |
| `aBase` | lista de `{articuloId, cantidad, unidad}` | lista de cantidades en unidad base | Restaurantes (validar y calcular recetas) |
| `kardex` | `articuloId`, `desde`, `hasta`, cursor | página de `MovimientoVista` | Panel |
| `lotes` | `articuloId`, `soloConSaldo` | lista de `LoteVista` | Panel |
| `porCaducar` | `dias` | lista de `LoteVista` | Panel |
| `series` | `articuloId`, `estado`, cursor | página de `SerieVista` | Panel |
| `politicas` | | `PoliticaVista` | Panel |
| `tareasSerie` | | lista de tareas pendientes | Panel |

### 6.2 Escrituras de usuario: preparar → confirmar [P] D-22, D-23

| Operación | Entrada | Salida |
|---|---|---|
| `preparar` | un `Comando` (ver 6.3) | `Preparacion { token, resumen, expiraEn }` |
| `confirmar` | `token` | `Resultado` del comando |

- El `token` es de un solo uso, **ligado al negocio, al actor y a un hash del comando**, y expira a los 5 minutos [P].
- `preparar` valida todo (unidades, saldos, series, lotes) y calcula el resumen **sin escribir** en las tablas de negocio.
- `confirmar` vuelve a validar, escribe y publica eventos en una sola transacción.
- Errores: `TOKEN_INVALIDO`, `TOKEN_EXPIRADO`, `TOKEN_USADO`, `COMANDO_CAMBIO` (el estado cambió y el resumen ya no es válido).

### 6.3 Comandos

| Comando | Campos | Reglas clave |
|---|---|---|
| `CrearArticulo` | `nombre`, `unidadBase`, `rastreo`, `minimo` (cantidad y unidad), `categoria?`, `productoId?`, `unidadesPropias[]?` | U-01, U-03. Si trae `productoId`, pide a Catálogo el modo `VINCULO_DIRECTO` (K-05) |
| `EditarArticulo` | `articuloId`, `nombre?`, `categoria?`, `minimo?`, `productoId?`, `politicaSalidaOverride?` | No cambia `unidadBase` ni `rastreo` si hay movimientos |
| `BajaArticulo` | `articuloId` | A-04 |
| `DefinirUnidadPropia` | `articuloId`, `nombre`, `factor` | U-03. No puede chocar con una unidad estándar |
| `RegistrarEntrada` | `articuloId`, `cantidad`, `unidad`, `costoUnitario`, `lote?{codigo, caducaEl?}`, `series[]?`, `ocurridoEn?` | C-02, C-03, C-08, L-02, S-01. `ocurridoEn` no puede ser futuro |
| `RegistrarSalida` | `articuloId`, `cantidad`, `unidad`, `motivo`, `lote?`, `series[]?` | L-03, S-03. Para consumo propio o venta no registrada |
| `RegistrarMerma` | `articuloId`, `cantidad`, `unidad`, `motivo` (CADUCIDAD, DANO, ROBO, OTRO), `lote?` | |
| `AjustePorConteo` | `articuloId`, `cantidadContada`, `unidad`, `lote?` | A-03, C-06. Con rastreo `LOTE`, el lote es obligatorio |
| `ConfigurarPolitica` | `politicaSalida?`, `diasAvisoCaducidad?` | D-10. `politicaCosteo` solo acepta `SIN_RECALCULO` |
| `AsignarSerie` | `causaId` (venta), `linea`, `serie` | S-04 |

### 6.4 Escrituras del sistema (sin confirmación humana)

Son consecuencia de una venta que ya fue confirmada. Por eso no usan preparar → confirmar. Este es un ajuste a la convención del `CLAUDE.md` que el equipo debe confirmar [P] D-22.

| Operación | Entrada | Salida |
|---|---|---|
| `registrarConsumo` | `consumidor`, `causaId`, `ocurridoEn`, `lineas[{articuloId, cantidad, unidad, serie?}]` | `ConsumoResultado { estado, movimientos[], costoTotal, alertas[] }`; `estado` ∈ `REGISTRADO`, `YA_REGISTRADO`, `OMITIDO_ANULADO_PREVIO` |
| `revertirConsumo` | `consumidor`, `causaId` | `ConsumoResultado` con `estado` ∈ `REVERTIDO`, `YA_REVERTIDO`, `MARCADO_ANULADO_PREVIO` |

Valores de `consumidor`: `vinculo` (lo usa el listener propio de Inventario) y `restaurantes`.

### 6.5 Códigos de error

Reintentables (el llamante puede volver a intentar): `CONCURRENCIA`, `ERROR_TEMPORAL`.

Permanentes: `ARTICULO_NO_ENCONTRADO`, `ARTICULO_INACTIVO`, `UNIDAD_NO_CONVERTIBLE`, `CANTIDAD_INVALIDA`, `COSTO_INVALIDO`, `RASTREO_NO_MODIFICABLE`, `UNIDAD_BASE_NO_MODIFICABLE`, `BAJA_CON_SALDO`, `LOTE_REQUERIDO`, `LOTE_NO_ENCONTRADO`, `LOTE_FECHA_DISTINTA`, `FECHA_CADUCIDAD_INVALIDA`, `SERIE_DUPLICADA`, `SERIE_NO_DISPONIBLE`, `SERIE_REQUIERE_PIEZA_ENTERA`, `MODO_INVENTARIO_CONFLICTO`, `POLITICA_NO_SOPORTADA`, `TOKEN_INVALIDO`, `TOKEN_EXPIRADO`, `TOKEN_USADO`, `COMANDO_CAMBIO`.

Cada código tiene un texto en español de México en una tabla de plantillas, y la capa que responde al usuario no improvisa texto.

---

## 7. Contrato de eventos

### 7.1 Sobre (envelope) común

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUIDv7 | Único por evento, generado por el productor al publicar |
| `tipo` | texto | `modulo.hecho_en_pasado`, por ejemplo `inventario.bajo_minimo` |
| `version` | entero ≥ 1 | Versión del contrato de ese `tipo` |
| `negocio_id` | UUID | Siempre presente |
| `origen` | texto | Módulo que publica |
| `ocurrido_en` | fecha-hora ISO-8601 en UTC | Momento del hecho, no de la entrega |
| `correlacion_id` | UUID | Hilo de negocio. Para una venta, el `venta_id` |
| `causa_id` | UUID o nulo | `id` del evento que lo provocó |
| `datos` | objeto | Carga del contrato |

Nota: el mockup de la plataforma usa `vertical` en lugar de `origen`; el mockup está desactualizado en ese campo.

Reglas de contenido:
- `datos` lleva hechos con IDs y valores, no entidades completas. Sin datos personales.
- Dinero y cantidades como **cadenas decimales** (`"180.00"`), nunca como números de punto flotante.
- Fechas y horas en ISO-8601.
- Se publican en la misma transacción que la escritura (outbox).
- Entrega **al menos una vez** y **sin orden global**. Todo consumidor deduplica por `evento_id` en su propia tabla `evento_procesado` y tolera eventos fuera de orden.

### 7.2 Eventos que Inventario consume (los publica Ventas)

Estos contratos los propone este documento y **deben congelarse con el dueño de Ventas antes de la fase 3** [P].

**`venta.registrada` v1**
| Campo | Tipo | Notas |
|---|---|---|
| `venta_id` | UUID | |
| `total` | decimal | |
| `lineas[]` | lista | Cada una: `linea` (entero), `producto_id` (UUID), `cantidad` (decimal), `unidad` (texto), `precio_unitario` (decimal), `serie` (texto, opcional) |

**`venta.anulada` v1**
| Campo | Tipo | Notas |
|---|---|---|
| `venta_id` | UUID | |
| `motivo` | texto, opcional | |

Procesamiento en Inventario: por cada línea cuyo producto tenga modo `VINCULO_DIRECTO`, llama internamente a `registrarConsumo(consumidor = "vinculo", causaId = venta_id)`. Para `venta.anulada`, `revertirConsumo` con la misma llave. Las líneas con otro modo se ignoran.

### 7.3 Eventos que Inventario publica

Todos con `origen = "inventario"`.

| Evento (v1) | Cuándo | Campos de `datos` | Consumidores previstos |
|---|---|---|---|
| `inventario.movimiento_registrado` | Por cada movimiento confirmado | `movimiento_id`, `articulo_id`, `tipo`, `cantidad_base`, `unidad_base`, `costo_unitario`, `costo_total`, `cpp_resultante`, `saldo_resultante`, `causa_tipo`, `causa_id` | Contabilidad (costo de ventas) |
| `inventario.bajo_minimo` | Al cruzar a ≤ mínimo (A-02) | `articulo_id`, `nombre`, `saldo_base`, `minimo_base`, `unidad_base` | Panel (alertas) |
| `inventario.repuesto` | Al volver por encima del mínimo | `articulo_id`, `nombre`, `saldo_base`, `minimo_base`, `unidad_base` | Panel |
| `inventario.saldo_negativo` | Al pasar de ≥ 0 a < 0 | `articulo_id`, `nombre`, `saldo_base`, `unidad_base` | Panel |
| `inventario.lote_por_caducar` | Tarea diaria, una vez por lote y fecha de aviso | `lote_id`, `articulo_id`, `codigo`, `caduca_el`, `dias_restantes`, `cantidad_base` | Panel |
| `inventario.lote_caducado` | Tarea diaria, la primera vez que un lote con saldo está caducado | `lote_id`, `articulo_id`, `codigo`, `caduca_el`, `cantidad_base`, `valor_estimado` | Panel |
| `inventario.consumo_sin_lote` | Al quedar cantidad sin lote (L-06) | `movimiento_id`, `articulo_id`, `cantidad_sin_lote`, `causa_id` | Panel |
| `inventario.serie_sin_asignar` | Venta serializada sin serie (S-04) | `causa_id` (venta), `linea`, `articulo_id` | Panel |

Ejemplo (fines ilustrativos, datos inventados):

```json
{
  "id": "0192f3a0-7c1e-7b52-9d41-5a1c3e8f2b10",
  "tipo": "inventario.movimiento_registrado",
  "version": 1,
  "negocio_id": "0192f39f-1111-7000-8000-00000000a001",
  "origen": "inventario",
  "ocurrido_en": "2026-10-08T18:22:41Z",
  "correlacion_id": "0192f3a0-6a00-7000-8000-00000000b002",
  "causa_id": "0192f3a0-6b00-7000-8000-00000000c003",
  "datos": {
    "movimiento_id": "0192f3a0-7c1e-7b52-9d41-5a1c3e8f2b11",
    "articulo_id": "0192f39f-2222-7000-8000-00000000d004",
    "tipo": "SALIDA",
    "cantidad_base": "-240.0000",
    "unidad_base": "g",
    "costo_unitario": "0.1800",
    "costo_total": "-43.20",
    "cpp_resultante": "0.1800",
    "saldo_resultante": "4760.0000",
    "causa_tipo": "VENTA",
    "causa_id": "0192f3a0-6a00-7000-8000-00000000b002"
  }
}
```

### 7.4 Flujo de una venta con receta

```
Ventas ── venta.registrada ──▶ (outbox) ──▶ Restaurantes (listener)
   1. deduplica por evento_id
   2. por cada línea con modo RECETA (pregunta a Catálogo):
        toma la versión de receta vigente en ocurrido_en
        cantidad × (1 + merma_pct/100) × unidades vendidas
   3. llama InventarioApi.registrarConsumo(
          consumidor = "restaurantes", causaId = venta_id, lineas)
                                   │
                                   ▼
                       Inventario (una transacción)
        4. ¿ya existe consumo (negocio, venta_id, "restaurantes")? → devuelve el guardado
        5. convierte unidades, asigna lotes (FEFO), valúa al CPP
        6. inserta movimientos, actualiza saldos
        7. publica movimiento_registrado y alertas por cruce de umbral
   8. error permanente → Restaurantes lo guarda en consumo_fallido (no reintenta)
      error reintentable → la publicación queda incompleta y se reintenta
```

---

## 8. Contratos que Inventario necesita de otros módulos

| De | Qué necesita | Estado |
|---|---|---|
| **Catálogo** | `existeProducto(productoId)` y `modoInventario(productoId)` / `establecerModoInventario(productoId, modo)`. Rechaza transiciones en conflicto: un producto no puede estar a la vez en `VINCULO_DIRECTO` y `RECETA` [P] D-17 | Por definir con el dueño de Catálogo |
| **Ventas** | Eventos `venta.registrada` v1 y `venta.anulada` v1 (7.2) | Por congelar |
| **Restaurantes** | Cumplir el flujo 7.4 y no llamar a otras partes de Inventario más que a su API | Este documento es el contrato |
| **Plataforma** | `ContextoNegocio` (negocio y actor), utilidad de redondeo, sobre `EventoDominio`, auditoría | `compartido`, fase 0 |

Nota sobre D-17: la versión 2 del plan hacía que Restaurantes consultara a Inventario si había un vínculo. Eso deja un hueco, porque Inventario no puede consultar a Restaurantes (R4) para saber si ya hay receta. Poner el modo en Catálogo cumple R4 y evita el doble descuento sin saltarse reglas.

---

## 9. GraphQL del panel (resumen)

El esquema `.graphqls` se redacta en la fase 7. Estas son las operaciones comprometidas. Cada resolvedor llama a `InventarioApi`; no toca repositorios.

| Tipo | Operación | Usa |
|---|---|---|
| Consulta | `inventario(soloAlertas, primeros, despues)` | `articulos`, `saldos` con DataLoader |
| Consulta | `articulo(id)` | `articulo`, `saldo` |
| Consulta | `kardex(articuloId, desde, hasta, primeros, despues)` | `kardex` |
| Consulta | `lotes(articuloId)`, `porCaducar(dias)` | `lotes`, `porCaducar` |
| Consulta | `series(articuloId, estado)`, `tareasSerie` | `series`, `tareasSerie` |
| Consulta | `receta(platilloId)`, `costoReceta(platilloId)` | `RestaurantesApi` (Restaurantes) |
| Mutación | `prepararComando(input)` → `Preparacion` | `preparar` |
| Mutación | `confirmarComando(token)` → `Resultado` | `confirmar` |
| Mutación | `guardarReceta(input)` | `RestaurantesApi`, que valida con `aBase` |
| Suscripción | `alertasNegocio` | Eventos `inventario.bajo_minimo`, `repuesto`, `saldo_negativo`, `lote_por_caducar`, `lote_caducado`, `consumo_sin_lote`, `serie_sin_asignar` |

Seguridad GraphQL: límites de profundidad y complejidad, consultas persistidas, autorización por resolvedor y `negocio_id` del contexto, nunca del argumento.

---

## 10. Consistencia y seguridad

- **Transacción única:** movimiento, saldo, lotes, series y publicación del evento van en la misma transacción.
- **Concurrencia:** bloqueo optimista con `version` en `articulo` y `saldo`, con reintento acotado. Agotado el reintento, el error es `CONCURRENCIA`.
- **Movimientos inmutables:** sin UPDATE ni DELETE. Revocado a nivel de base de datos.
- **Aislamiento:** `negocio_id` solo del contexto; RLS como segunda barrera.
- **Entrada del bot:** el LLM solo extrae datos a un esquema JSON fijo (artículo, cantidad, unidad, costo, caducidad). El código resuelve unidades y fechas relativas ("el viernes") y valida. El LLM no llama a `confirmar`.
- **Datos:** sin datos personales en eventos. Auditoría de cada confirmación (negocio, actor, comando, resultado).
- **Tokens:** se guarda solo el hash. Ligados a negocio, actor y comando.

---

## 11. Criterios de aceptación

Cada fila es una prueba obligatoria. Se escriben antes que la implementación (TDD), con Testcontainers para Postgres.

| Prueba | Reglas |
|---|---|
| `ApplicationModules.verify()` en verde: sin dependencias prohibidas | R1, R4 |
| Conversión kg↔g, l↔ml, unidad propia y unidad no convertible | U-02 a U-04 |
| Redondeo: cifras calculadas a mano, incluidos casos de media centésima | 4.3 |
| CPP: entrada con saldo positivo, con saldo cero y con saldo negativo; salida no cambia el CPP | C-02 a C-04 |
| Reversión reingresa al costo original, no al CPP actual | C-05 |
| Ajuste por conteo positivo y negativo | C-06, A-03 |
| FEFO reparte una salida en varios lotes; empate de caducidad resuelto por antigüedad | L-03, L-04 |
| Lote caducado no se asigna; faltante queda como consumo sin lote y publica el evento | L-05, L-06 |
| Tarea diaria idempotente (se ejecuta dos veces, un solo aviso) | L-07 |
| Serie: duplicada, no disponible, venta sin serie genera tarea y evento | S-01 a S-04 |
| Receta con serie como insumo es rechazada | S-05 |
| **Mismo `causaId` dos veces descuenta una sola vez** | K-01 |
| Consumo atómico: una línea inválida no descuenta ninguna | K-02 |
| Reversión dos veces revierte una sola vez | K-03 |
| `venta.anulada` antes de `venta.registrada`: no se descuenta | K-04 |
| Mismo evento entregado dos veces a un listener: un solo efecto | R3 |
| Producto no puede estar en vínculo directo y en receta a la vez | K-05 |
| Alertas solo al cruzar umbral (no en cada movimiento) | A-02 |
| Stock negativo permitido y avisado | A-01 |
| Token: expirado, reutilizado, de otro negocio, comando alterado | 6.2 |
| Aislamiento: un negocio no ve ni toca datos de otro (RLS) | 10 |

Cobertura mínima: 80%.

---

## 12. Fases y asignación

Con este documento aprobado, los contratos quedan **congelados** (cambios solo aditivos, R5) y B y C pueden empezar contra ellos sin esperar a que A termine. Las estimaciones son de la versión 2 del plan.

| Fase | Entregable | Estimado | Responsable sugerido |
|---|---|---|---|
| 0 | Esqueleto Maven, `compartido`, Testcontainers, Flyway, `verify()` | 0.5 d | A |
| 1 | Dominio base y conversión de unidades | 1.5 d | A |
| 2 | Movimientos valuados con CPP | 1 d | A |
| 3 | API pública, preparar → confirmar, eventos | 1 d | A |
| 4 | Recetas en Restaurantes, consumo idempotente, reversión, costo teórico | 2 d | B |
| 5 | Lotes, FEFO, caducidades y tarea diaria | 2 d | C |
| 6 | Números de serie | 1.5 d | C |
| 7 | GraphQL y suscripción | 1.5 d | A |

Total aproximado: 11 días-persona. Ruta crítica en paralelo: unos 5 días. Hoy es 8 de octubre y la demo es el 15, por lo que el margen es muy estrecho. Se recorta desde abajo (series, luego lotes).

Riesgos de entorno detectados el 8 de octubre: Docker Desktop instalado pero apagado (Testcontainers lo necesita); no hay Postgres ni `psql` locales; el repo está dentro de OneDrive, lo que puede bloquear archivos de compilación (conviene excluir `target/` de la sincronización).

---

## 13. Pendientes para aprobar este documento

| # | Pregunta | Propuesta |
|---|---|---|
| 1 | ¿FEFO y costeo se configuran **por negocio** (con excepción por artículo en FEFO) y no por usuario? | Sí [P] |
| 2 | ¿Maven, con Docker Desktop encendido para las pruebas? | Sí [P] D-14, D-15 |
| 3 | ¿Dónde vive la base de datos de la demo (Supabase o contenedor)? | Abierta [?] D-16 |
| 4 | ¿Aceptan que el modo de inventario de un producto lo lleve Catálogo (D-17)? | Sí [P] |
| 5 | ¿Aceptan que `registrarConsumo` y `revertirConsumo` no usen preparar → confirmar (D-22)? | Sí [P] |
| 6 | ¿Aceptan los detalles D-18 a D-21 y D-23 (llave de consumo, marca de anulación previa, redondeo, lotes caducados fuera de FEFO, token de 5 minutos)? | Sí [P] |
| 7 | ¿Quién es dueño de Catálogo y de Ventas, para congelar sus contratos con ellos? | Abierta [?] |

Quien apruebe este documento puede responder con los números, por ejemplo "aprobado salvo 3 y 7".
