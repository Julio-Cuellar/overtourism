# Plan v2: lo que tenemos de verdad y lo que falta en los horizontales para Restaurantes

**Fecha:** 2026-10-24 · **Versión 2.** Reemplaza a la v1 del mismo día, que partía de un `origin/julio` anterior (`97b969f`).
**Estado:** PLAN, esperando confirmación. **No hay autorización escrita para programar** lo que este plan propone (CLAUDE.md, «Convenciones»); cada fase se autoriza en la Bitácora antes de empezar.
**Base medida:** `origin/julio` en `f97c0ad` (compilado y probado en una copia limpia) **más** lo que solo existe en tu disco sin commitear.
**Documentos hermanos:** [`docs/api/guia-front-consultas.md`](../api/guia-front-consultas.md) (cómo consulta el front) y el artefacto «Mapa de Restaurantes» (misma URL, republicado con este estado).
**Complejidad:** Media-Alta. ≈ 16 días-persona de servidor, sin contar Clientes opcional.

---

## 0. Estado de ejecución (2026-10-24, tras la autorización escrita)

Autorizado: Fase 0, Fase 1 y, de la Fase 2, solo HZ-22, HZ-23 y HZ-28 (ver la Bitácora). **Nada se ha subido al remoto.**

| Tarea | Estado | Evidencia |
|---|---|---|
| HZ-00 traer `origin/julio` | **Hecho** | `julio` local avanzó a `f97c0ad` con respaldo previo (parche y tar) y `stash`; 4 archivos chocaron y se resolvieron; el tronco compila |
| HZ-01 consumo de Inventario en Postgres | **Hecho, sin ejecutar contra Postgres** | `V072` (rastreo y tabla `consumo`, seguridad por fila forzada), `InventarioJdbc` implementa `RepositorioConsumos` con `ON CONFLICT DO NOTHING`, `InventarioPostgresConfiguracion` cablea el consumo; 8 pruebas nuevas del contrato (corren en memoria) y `ConsumoInventarioPostgresTest` (compila, sin ejecutar: no hay Docker) |
| HZ-02 `mvn -B verify` con Docker | **Pendiente (tuya)** | Aquí: 0 fallas; los errores son de contextos que necesitan Docker |
| HZ-03 commit local de F5/F6 | **Hecho, sin push** | 5 commits: compartido, ventas, caja, inventario (con HZ-01) y docs/pruebas transversales |
| HZ-04 un solo `restaurantes.md` | **Hecho, sin commitear** | `restaurantes-original.md` pasó a `restaurantes.md`; la copia del artefacto sigue en el historial (`cf2ac99`) |
| HZ-05 orden de Flyway | **Pendiente de tu base** | No hay `out-of-order` configurado; ver la Bitácora |
| HZ-10, HZ-11, HZ-12 | **Hecho** | Guía v2 actualizada; D-208 a D-214 aceptadas; contrato aditivo congelado |
| HZ-28 kit de contrato | **Hecho, sin commitear** | `ContratoDeFuenteDeProductosTest` + `ModoInventario` + `modoInventario` por omisión; la fuente simulada lo pasa (2 pruebas opcionales omitidas) |
| HZ-23 Restaurantes implementa `FuenteDeProductos` | **Hecho, sin commitear** | `PlatillosParaVenta` implementa la interfaz de la plataforma y declara el modo de inventario; `ProductoDePlatillo` se eliminó; 10 pruebas de contrato en verde |
| HZ-22 `ComandosDeRestaurantes` | **Hecho, sin commitear** | Variantes `guardarReceta` y `darDeBajaPlatillo` en `ComandoInput`, declaradas en la tabla de operaciones (SG-21); 7 pruebas nuevas |

**Hallazgos de la ejecución:**
1. **Los manejadores de Restaurantes no declaraban `permisos()`.** Con eso la plataforma no exigía el permiso antes de gastar el token y D-126 no se cumplía en los comandos de Restaurantes. Corregido en `ManejadorGuardarReceta` y `ManejadorDarDeBajaPlatillo`.
2. **Tres pruebas del remoto quedaban rotas por mi persistencia** (`PreparacionesEnMemoriaTest`, `InventarioEnMemoriaTest`, `InventarioConfiguracionTest`) y se ajustaron; `AislamientoVentasCajaInventarioPostgresTest` compara la lista de tablas con la base y se actualizó con `inventario.consumo`.
3. **Mutaciones:** 4 de 4 detectadas en HZ-22, HZ-23 y HZ-28.

---

## 1. Qué se pidió

«Con lo que tengo actualmente vuelve a generar el plan y el artefacto, para saber en realidad qué tenemos actualmente.» Este plan parte de lo **medido en el código y en las pruebas**, no de lo que dicen los diseños.

## 2. Lo que tenemos de verdad

### 2.1 Tres capas, para no confundirlas

| Capa | Dónde vive | Qué contiene |
|---|---|---|
| **A. Subido** | `origin/julio` (`f97c0ad`) | Identidad, Negocios, Clientes, Ventas, Caja e Inventario **en memoria** (Ventas, Caja e Inventario), el despachador de comandos, **Restaurantes P1a (R0 a R3)**, el consumo por venta de Inventario, la maqueta de Flutter y los diseños |
| **B. Solo en tu disco** | Árbol de trabajo de `julio`, sin commitear, **82 rutas** | Persistencia en Postgres y outbox de Ventas, Caja e Inventario (F5/F6: `V040`, `V041`, `V042`, `V070`, `V071`, repositorios JDBC, `AlmacenesObligatoriosEnProduccion`) y los documentos de esta sesión |
| **C. Solo diseño** | `docs/diseno/` y artefactos | Restaurantes P1b (salón, cuentas, cocina, QR), persistencia de Restaurantes (R7), Misceláneos, Museos, Pasaporte, Catálogo, Contabilidad |

Tu `julio` local está **3 commits atrás** de la capa A: no tiene Restaurantes ni el consumo de Inventario.

### 2.2 Pruebas (copia limpia de `origin/julio`, JDK 21, **sin Docker**)

`mvn -B test`: **compila sin errores; 2,593 pruebas, 0 fallas y 568 errores**, que coinciden exactamente con lo que anotaste en la Bitácora. Todo apunta a que los errores son contextos de Spring y pruebas de Postgres que necesitan Testcontainers; **no revisé los 568 uno por uno**.

| Módulo | Pruebas | Errores (sin Docker) | Archivos main / líneas |
|---|---|---|---|
| Identidad | 681 | 100 | 83 / 4,416 |
| Clientes | 338 | 6 | 65 / 2,872 |
| Seguridad (puerta de `/graphql`) | 327 | 120 | (en `compartido`) |
| Negocios | 262 | 130 | 113 / 5,690 |
| Compartido | 241 | 0 | 96 / 5,017 |
| **Restaurantes** | **193** | **0** | **85 / 3,992** |
| Persistencia (Postgres) | 187 | 187 | — |
| Inventario | 164 | 6 | 73 / 3,430 |
| Ventas | 106 | 9 | 43 / 2,349 |
| Caja | 79 | 0 | 60 / 3,096 |
| Raíz (arranque, catálogo de permisos) | 15 | 10 | — |

Lectura: las 193 pruebas de Restaurantes **sí corren aquí** (son unitarias, no necesitan base). Lo que no se ha visto nunca es el arranque completo de la aplicación con Restaurantes: lo dice tu Bitácora y yo no pude comprobarlo sin Docker.

### 2.3 Módulo por módulo (verificado en el código)

| Módulo | Almacén en `origin/julio` | Superficie | Lo que NO tiene |
|---|---|---|---|
| Identidad | Postgres | REST `/auth/*`, `/registro/*`; 63 permisos para el dueño (30 base + 18 de Restaurantes + 15 de Ventas, Caja e Inventario) | — |
| Negocios | Postgres | 8 consultas, 10 mutaciones, 1 suscripción; 12 negocios simulados | Mesas libres/total; evento `negocio.estado_cambiado`; `negocio.disponibilidad_cambiada` (C-01) |
| Clientes | Postgres | `sugerirClientes`, aviso de privacidad, 3 comandos | 6 consultas responden `OPERACION_NO_DISPONIBLE` |
| Ventas | **Memoria** | `registrarVenta`, `anularVenta`, 4 consultas; publica `venta.registrada` y `venta.anulada` | **`referencia`**; solo vende los 6 productos `[SIMULADO]` (ningún vertical conectado) |
| Caja | **Memoria** | Cajas, turnos, movimientos, cortes; consume `venta.*` | Carrera cerrar turno/aplicar venta; CJ-11 |
| Inventario | **Memoria** | Artículos, movimientos, kardex; **`registrarConsumo`, `revertirConsumo`, `articulosPorId`** (nuevos) | Almacén Postgres del consumo; transacción de K-02; lotes y series |
| Restaurantes | **Memoria** (`restaurantes.almacen=memoria`) | **8 consultas y 8 mutaciones** (ver 2.4) | Todo P1b; comandos; conexión real con Ventas |
| Plataforma | — | Puerta única, despachador de comandos, suscripciones por negocio | `Idempotency-Key`; CORS; escrituras públicas |

En el remoto, Ventas, Caja e Inventario **no tienen tablas**: las migraciones `V040` a `V071` existen solo en tu disco. El remoto tiene 15 migraciones (`V001` a `V038`, sin `V036`).

### 2.4 Restaurantes: diseño contra código

**Subido y probado (R0 a R3):**

| Pieza | Evidencia |
|---|---|
| Módulo `restaurantes`, dependencias `compartido`, `inventario`, `inventario::api` | `EstructuraModulosTest` |
| 18 permisos, rol `MESERO_CAJA`, `V037` | `PermisosDeRestaurantes`, `restaurantes-r0.tdd.md` |
| Menú: platillos, categorías, etiquetas, temporada, agotado, publicar, baja | 8 consultas y 8 mutaciones en `restaurantes.graphqls` |
| Recetas versionadas, costo teórico y margen exactos | `RecetasServicio`, `CalculadoraDeCosto` |
| Consumo por `venta.registrada`, reversa por `venta.anulada`, `consumo_fallido` | `ConsumidorDeVentas`, `InventarioApiComoPuerto` |
| Seguimiento de `negocio.registrado`, `negocio.actualizado`, `negocio.estado_cambiado` | `SeguimientoDeNegocios` |
| Menú público sin sesión | `menuPublico`, `MenuPublicoServicio` |

**Cobertura de las reglas del diseño** (IDs que aparecen citados en el código o en las pruebas; es un indicador, no una prueba de cumplimiento): **38 de 109 reglas**.

| Grupo | En el diseño | Citadas | Sin cita |
|---|---|---|---|
| MN (menú) | 14 | 13 | MN-11 |
| RC (recetas) | 10 | 10 | — |
| CM (consumo) | 9 | 8 | CM-07 |
| PB (menú público) | 6 | 6 | — |
| DM (demo) | 4 | 1 | DM-02, DM-03, DM-04 |
| SR (seguridad) | 7 | 0 | todas |
| MS, CT, CD, QR (salón, cuentas, cocina, QR) | 54 | 0 | todas (P1b) |
| IA (P2) | 5 | 0 | todas |

**Lo que el diseño da por hecho y el código no tiene** (hallazgos de esta revisión):

1. **`guardarReceta` y `darDeBajaPlatillo` no se pueden llamar.** La lógica existe (`ManejadorGuardarReceta`, `ManejadorDarDeBajaPlatillo`), pero no hay `ComandosDeRestaurantes` que implemente `ModuloDeComandos`, y el esquema no tiene las variantes en `ComandoInput`. El despachador ya existe desde `97b969f`, así que esto ya se puede hacer.
2. **Ventas no puede vender platillos.** `PlatillosParaVenta` usa «una interfaz propia mientras esa no exista» (su Javadoc); la `FuenteDeProductos` de `compartido.catalogo` ya existe, pero Restaurantes no la implementa. Hoy lo único que implementa `FuenteDeProductos` es `FuenteDeProductosSimulada`. La Bitácora dice «fuente de productos para Ventas»; el cableado real falta.
3. **Restaurantes publica un solo evento** (`restaurantes.consumo_fallido`). Los otros siete del diseño son de P1b/P2.
4. **`negocio.estado_cambiado` se consume pero nadie lo emite** (Negocios no lo publica). Restaurantes solo se entera de la publicación por `negocio.actualizado`, no de pausas ni suspensiones.
5. **Paginación inconsistente:** Restaurantes devuelve `{ nodos, siguiente }`; Ventas, Caja e Inventario devuelven `pageInfo { hayMas, cursorFinal }`. El front tendría dos formas de paginar.
6. **Dos documentos de diseño distintos con el mismo nombre de versión:** `restaurantes.md` (copia del artefacto, dice que R2/R3 esperan a Inventario) y `restaurantes-original.md` (el tuyo, con P1a programada). Difieren en 312 líneas. Tu Bitácora deja la decisión a tu otra sesión.
7. **La colección de Postman no tiene nada de Restaurantes** (0 coincidencias con sus operaciones).
8. **`docs/api/graphql-integracion-front.md` está desactualizado** (dice que la persistencia está pendiente y no menciona Restaurantes).

### 2.5 Lo que sigue sin existir (verificado)

| Pendiente | Búsqueda |
|---|---|
| `referencia` de venta | `Venta.java` y `RegistrarVentaComando.java` solo tienen `turnoId`, `clienteId`, `cuponId` |
| `Idempotency-Key` y cabeceras CORS | cero coincidencias en `main/` |
| Disponibilidad con mesas libres y `negocio.disponibilidad_cambiada` | cero coincidencias en `negocios/` |
| `ContratoDeFuenteDeProductosTest` y `modoInventario` | no están en `compartido/catalogo` |
| Postgres para Restaurantes, Inventario-consumo, Ventas, Caja | solo hay `*Jdbc` de Clientes, Identidad y Negocios en el remoto |
| Suscripciones de Restaurantes (`comandasDeCocina`, `salonActualizado`, `alertasRestaurante`) | solo existe `altaDeNegocio` |

### 2.6 Lo que ya resolviste y mi v1 pedía (se retira del plan)

| Tarea de la v1 | Qué pasó |
|---|---|
| HZ-03 Renumerar decisiones | **Hecho** por ti: Inventario-consumo `D-200` a `D-203`, integración `D-204` a `D-207`. Mi propuesta «D-200» se retira |
| HZ-22 `@NamedInterface` y `aBase` con `activo`/`rastreo` | **Hecho de otra forma:** `@NamedInterface("api")` (D-203) y `articulosPorId` (tu D-200). Mi D-209 se retira |
| HZ-25 `registrarConsumo` y `revertirConsumo` | **Hecho en memoria** (D-22 aceptada). Falta Postgres |
| HZ-23 permisos y `MESERO_CAJA` | **Hecho** (`V037`). Falta darle a `MESERO_CAJA` los permisos de Ventas y Caja |
| Pregunta 1 (¿existe R0–R3?) | **Respondida:** sí, está en `origin/julio` |

## 3. Patrones del código que el plan reutiliza

| Categoría | Ejemplo a imitar | Cómo se usa |
|---|---|---|
| Módulo y comandos | `compartido/graphql/comandos/ModuloDeComandos.java:16`; `caja/graphql/ComandosDeCaja.java:35`; `inventario/graphql/ComandosDeInventario.java:37` | `ComandosDeRestaurantes` copia esta forma: prefijo `restaurantes.` y variantes |
| Errores | `ventas/dominio/CodigoError.java:7` + `ventas/graphql/TraductorDeErroresDeVentas.java:19`; `restaurantes/web/TraductorDeErroresDeRestaurantes.java` | Enum estable + plantilla en español de México |
| Datos | `ventas/infraestructura/RepositorioVentasJdbc.java:42` (solo en tu disco) con `EjecutorEnNegocio` | JDBC con seguridad por fila; filtra por `negocio_id` |
| Fuente de productos | `compartido/catalogo/FuenteDeProductos.java:15`; `restaurantes/aplicacion/PlatillosParaVenta.java:56` | Se adapta lo existente: `ProductoDePlatillo` → `ProductoParaVenta` |
| Eventos | `ventas/eventos/VentaRegistrada.java:22`; `negocios/aplicacion/FabricaEventos.java:39` | Sobre común, campos opcionales omitidos (R5) |
| Consumo | `caja/infraestructura/EscuchaDeVentasEnOutbox.java:43`; `restaurantes/infraestructura/EscuchaDeVentas.java` | `@ApplicationModuleListener`, idempotente |
| Pruebas | `ContratoRepositorioVentasTest.java:25` (abstracta) + subclases memoria y Postgres | La misma batería contra los dos almacenes |
| Evidencia | `docs/pruebas/restaurantes-p1a.tdd.md`, `inventario-consumo.tdd.md` | RED con motivo correcto, GREEN, mutaciones, cobertura |

Reglas del repo en cada tarea: TDD, JavaDoc (D-117), español de México, dinero en texto decimal, `negocio_id` de la sesión, R1 a R6, **sin commit ni push salvo que lo pidas**.

## 4. Decisiones propuestas (PROPUESTA; la numeración sigue a la tuya: D-208 en adelante)

| ID | Decisión | Por qué |
|---|---|---|
| D-208 | `Idempotency-Key` es cabecera HTTP opcional en las operaciones que declaren `idempotente = true`. Misma llave y misma petición devuelven la respuesta guardada; misma llave con otra petición, `IDEMPOTENCIA_CONFLICTO`. Vigencia 24 h; alcance `(negocio o token público, operación, llave)` | CT-14 y QR-15 del diseño la exigen y nada la provee |
| D-209 | **Restaurantes expone `guardarReceta` y `darDeBajaPlatillo` con un `ModuloDeComandos` propio** (`ComandosDeRestaurantes`, prefijo `restaurantes.`) | Cierra el pendiente de R2 que la Bitácora atribuye a la Pista A |
| D-210 | Una operación pública puede escribir solo si se declara (`Acceso.PUBLICA` + `escritura = true`) y lleva límite por token de recurso, dispositivo e IP | `pedirPorQr` y `pedirCuentaPorQr` son escrituras anónimas |
| D-211 | CORS reusa `seguridad.http.origenes-permitidos` (sin comodines): preflight y `Access-Control-Allow-*` solo al origen exacto | Flutter web hoy no puede llamar a `/graphql` |
| D-212 | Disponibilidad con `tipo/libres/total/abierto/actualizadoEn`; evento más viejo se ignora; ficha pública por sondeo en v1 | La infraestructura de WebSocket exige token |
| D-213 | **Armonizar la paginación de Restaurantes** al contrato de la plataforma: `{ nodos, pageInfo { hayMas cursorFinal } }` | Barato ahora (el front aún no lo consume); caro después |
| D-214 | `Restaurantes` implementa `FuenteDeProductos` (se retira la interfaz propia `PlatillosParaVenta`) | Es lo que pidió D-128; sin esto Ventas no vende platillos |

## 5. Fases y tareas

Cada tarea: pruebas primero (RED con el motivo correcto) → código mínimo (GREEN) → `mvn -B verify` → evidencia en `docs/pruebas/` → entrada en la Bitácora. Estimaciones en días-persona (d-p).

### Fase 0 · Poner el repo local al día y estable (≈ 1.5 d-p)

| # | Tarea | Valida | d-p |
|---|---|---|---|
| HZ-00 | Traer `origin/julio` a tu disco (guardar con `stash -u`, avance rápido, recuperar). **Choques esperados:** `InventarioEnMemoria.java`, `application-desarrollo.yml`, `fakes/RepositorioPreparacionesEnMemoria.java`, `BITACORA.md`, más renombre contra modificación en `InventarioConfiguracion` | `git status` sin marcadores; todo compila | 0.5 |
| HZ-01 | Completar la persistencia de Inventario tras el merge: tu `InventarioJdbc` no implementa el puerto nuevo `RepositorioConsumos`; hay que agregar la tabla `consumo` (migración nueva `V072`) y envolver `registrarConsumo`/`revertirConsumo` en una transacción (K-02) | `ContratoAlmacenDeInventarioTest` + pruebas de consumo contra Postgres, dos hilos sobre la misma `causaId` | 1.0 |
| HZ-02 | `mvn -B verify` **con Docker** (tú) y anotar el total | 0 fallas; sin Docker aquí no se puede | — |
| HZ-03 | Commit local de F5/F6 en commits separados. **Sin push** | `git status` limpio | — |
| HZ-04 | Dejar un solo `restaurantes.md`: ¿manda `restaurantes-original.md`? | Un archivo y la copia retirada o renombrada | — |
| HZ-05 | Revisar el orden de Flyway en tu base de desarrollo (`V037` llegó después de `V038`) | `out-of-order` en desarrollo o base nueva | — |

### Fase 1 · Contrato congelado (hito H-C, ≈ 0.5 d-p, documentos)

| # | Tarea | Resultado |
|---|---|---|
| HZ-10 | Guía de consultas del front (actualizada con el esquema real de Restaurantes) | `docs/api/guia-front-consultas.md` |
| HZ-11 | Aceptar o corregir D-208 a D-214 | Bitácora: ACEPTADA |
| HZ-12 | Congelar los cambios aditivos de la guía, sección 7 | Hito **H-C**: desde aquí no se renombra nada |

### Fase 2 · Desbloqueos (≈ 10.25 d-p)

| # | Dónde | Tarea | Valida | d-p | Depende de |
|---|---|---|---|---|---|
| HZ-20 | **Ventas** | Campo opcional `referencia` (hasta 80 caracteres) en el comando, `Venta`, `venta.registrada` v1 y GraphQL. `V040` aún no está commiteada: se edita ahí | `ContratoRepositorioVentasTest`, `GraphQlVentasCajaContratoTest`, lector de Clientes con y sin el campo | 1 | HZ-12 |
| HZ-21 | **Plataforma** | CORS con preflight sobre la lista existente (D-211) | Pruebas SG-09/SG-14 en verde; `OPTIONS /graphql` desde origen permitido, 204 con cabeceras; origen ajeno, 403 | 0.5 | HZ-12 |
| HZ-28 | **Plataforma** | `ContratoDeFuenteDeProductosTest` (abstracta) y `modoInventario` opcional | La fuente simulada lo extiende y pasa | 0.5 | — |
| HZ-22 | **Restaurantes** | `ComandosDeRestaurantes` (D-209): variantes `guardarReceta` y `darDeBajaPlatillo` en `ComandoInput`, permiso al preparar y al confirmar sin gastar el token | Prueba de punta a punta como `GraphQlVentasCajaContratoTest`: preparar, resumen con costo, confirmar, versión 2 | 0.75 | HZ-00 |
| HZ-23 | **Restaurantes** | Implementar `FuenteDeProductos` (D-214) y retirar `PlatillosParaVenta` propio; `registrarVenta` con un platillo | `ContratoDeFuenteDeProductosTest` (de HZ-28) y venta de un platillo seguida de descuento de insumos | 0.75 | HZ-28 |
| HZ-24 | **Identidad** | `V039`: permisos de Ventas y Caja para `MESERO_CAJA` | `RolesYPermisosTest` contra Postgres | 0.25 | HZ-00 |
| HZ-25 | **Plataforma** | `Idempotency-Key` (D-208): tabla `plataforma.idempotencia` (`V003`), interceptor, bandera en `OperacionDeclarada` | Misma llave dos veces = un efecto; otro cuerpo = conflicto; dos negocios no se ven | 1.5 | HZ-12 |
| HZ-26 | **Negocios** | Disponibilidad por vertical (D-212): leer `negocio.disponibilidad_cambiada`, guardarla, exponerla en `NegocioPublico`; **emitir `negocio.estado_cambiado`** | Evento repetido = un efecto; evento viejo se ignora; Restaurantes lo consume sin cambios | 2 | HZ-12 |
| HZ-27 | **Plataforma** | Operaciones públicas con escritura y límite por token, dispositivo e IP (D-210) | Sexto intento por minuto = `TASA_EXCEDIDA`; revisión del `security-reviewer` | 1.5 | HZ-25 |
| HZ-29 | **Restaurantes** | Armonizar la paginación (D-213) | Pruebas de la conexión con `pageInfo` | 0.5 | HZ-11 |
| HZ-29b | **Restaurantes** | Postman: carpeta de Restaurantes (8 consultas y 8 mutaciones, y los comandos de HZ-22) | La colección corre completa contra `desarrollo` | 0.5 | HZ-22 |

### Fase 3 · Cierre de la integración (F7) (≈ 4 d-p)

| # | Tarea | Valida | d-p |
|---|---|---|---|
| HZ-30 | Carrera cerrar turno contra aplicar venta (A2) | Dos hilos sobre Postgres: la venta del medio entra al corte | 1 |
| HZ-31 | CJ-11, venta con el turno ya cerrado: decidir y programar | Prueba de aceptación; el outbox deja de reentregar en cada arranque | 1 |
| HZ-32 | Purga de preparaciones vencidas y de `evento_procesado` | Patrón de `LimpiezaProgramadaTest` | 0.5 |
| HZ-33 | Medir las dos conexiones por confirmación con pool chico | Prueba de carga corta | 0.5 |
| HZ-34 | `ventas.md` y `caja.md` a v0.2, ≥ 12 mutaciones, `java-reviewer` y `security-reviewer` | Evidencia en `docs/pruebas/` | 1 |

### Fase 4 · Verificación final

`mvn -B clean verify` con Docker, Postman completo contra `desarrollo` y prueba de humo del front. **Primer arranque real** de la aplicación con Restaurantes, Inventario y Ventas juntos: hasta hoy ninguna de las dos ramas lo ha visto correr.

## 6. Lo que sigue en Restaurantes (pista C, no es de los horizontales)

| Fase del diseño | Estado real | Qué la desbloquea |
|---|---|---|
| R0 a R3 | Hechas en memoria, sin comandos ni fuente de productos | HZ-22 y HZ-23 |
| R4 salón y cuentas | **No empezada, no autorizada** | HZ-20 (`referencia`, bloqueante), HZ-25 |
| R5 cocina | **No empezada, no autorizada** | R4 y las suscripciones por negocio |
| R6 QR | **No empezada, no autorizada** | R4, HZ-25, HZ-27, HZ-21 |
| R7 persistencia (`V060` a `V065`) | **No empezada, no autorizada** | HZ-00 a HZ-03 |
| R8 demo y cierre | Parcial (DM-01) | R7 |

## 7. Orden y paralelismo

```
HZ-00 → HZ-01 → HZ-02 → HZ-03         HZ-04  HZ-05
                  ↓
          HZ-10/11/12  (H-C)
                  ↓
  ┌───────────┬───────────┬───────────┬───────────┐
 HZ-20      HZ-21      HZ-28 → HZ-23   HZ-26     HZ-25 → HZ-27
(Ventas)   (CORS)    (kit → Restaurantes)  (Negocios) (idempotencia → públicas)
  HZ-22 (comandos de Restaurantes)   HZ-24 (V039)   HZ-29 / HZ-29b
                  ↓
        Fase 3 (F7)  →  Fase 4
```

Propiedad de archivos: `RolesBase` y la Bitácora son serializados; migraciones por rango (`V039`, `V043`–`V059` libres; `V060`–`V069` de Restaurantes; `V070`–`V079` de Inventario).

**Hitos que ve el front** (detalle en su guía):

| Hito | Cuándo | Qué puede conectar el front |
|---|---|---|
| H-0 | Hoy | Auth, Ventas, Caja, Inventario, Negocios, Clientes **y las 16 operaciones de Restaurantes P1a** (en memoria) |
| H-C | Fin de Fase 1 | Nombres congelados, incluida la paginación armonizada |
| H-1 | HZ-20, HZ-21, HZ-24 | Cobro con `referencia`, Flutter web, permisos de cobro del mesero |
| H-2 | HZ-22, HZ-23 | Guardar receta y vender un platillo reales |
| H-3 | HZ-26 | Mesas libres reales en la ficha |
| H-4 | HZ-25, HZ-27 | Pedido por QR |

## 8. Riesgos

| Riesgo | Prob. | Efecto | Mitigación |
|---|---|---|---|
| Nunca se ha arrancado la aplicación con todo junto | Alta | Sorpresas al integrar | Primer arranque en HZ-02 y en la Fase 4 |
| El merge local choca con tu persistencia | Alta | Tiempo y riesgo de pérdida | Respaldo (parche y tar) y verificación archivo por archivo, como la vez anterior |
| Sin Docker no puedo ejecutar las pruebas de Postgres | Alta | Verificación incompleta | Las corres tú; cada entrega dice qué se corrió |
| Flyway fuera de orden (`V037` tras `V038`) | Alta en tu base actual | Arranque rechazado | HZ-05 |
| Ventas no vende platillos hasta HZ-23 | Cierta | Demo sin cobro de Restaurantes | HZ-28 y HZ-23 primero |
| `Idempotency-Key` toca la puerta única de `/graphql` | Media | Romper SG-01 a SG-21 | Pruebas SG existentes como red |
| Dos diseños de Restaurantes en el repo | Media | Decisiones contradictorias | HZ-04 |
| Paginación distinta en Restaurantes | Media | Doble código en el front | D-213 antes del front |
| CJ-11 sin definición | Media | Bloqueo en F7 | Se decide antes |

## 9. Criterios de aceptación

- [ ] `mvn -B verify` termina en 0 fallas con Docker, sobre el árbol unido (remoto más persistencia).
- [ ] La aplicación arranca con el perfil `desarrollo` y `produccion` exige Postgres.
- [ ] Un platillo se vende con Ventas y descuenta insumos de punta a punta.
- [ ] `guardarReceta` y `darDeBajaPlatillo` se llaman por GraphQL.
- [ ] El front consulta H-0 sin cambios y usa H-C sin renombres.
- [ ] Nada se sube al remoto hasta que lo pidas.

## 10. Preguntas (recomendación entre paréntesis)

1. ¿Cuál diseño manda: `restaurantes-original.md` o la copia `restaurantes.md`? (El original, que sabe del código.)
2. ¿Traigo `origin/julio` a tu disco ya (HZ-00) con el mismo procedimiento de respaldo? (Sí.)
3. ¿Corres tú `mvn -B verify` con Docker tras HZ-01? (Sí.)
4. ¿Aceptas D-208 a D-214? (Sí; D-209 y D-214 son lo que más desbloquea.)
5. CJ-11, venta con el turno ya cerrado: ¿al siguiente turno abierto o se rechaza? (Al siguiente turno abierto.)
6. ¿Commit local de F5/F6 en commits separados, sin push? (Sí.)
7. ¿Qué fases autorizas ahora por escrito? (Fase 0, Fase 1 y HZ-22, HZ-23, HZ-28 de la Fase 2.)
8. ¿Incluimos Clientes (historial) ahora? (No.)

**ESPERANDO CONFIRMACIÓN.**
