# Diseño del módulo de Contabilidad

**Versión:** 0.2
**Fecha:** 2026-10-15
**Estado:** PROPUESTA. Solo D-92 está aceptada (regla del usuario). No hay autorización escrita para programar Contabilidad.
**Contexto de decisiones:** `docs/BITACORA.md` (los identificadores `D-xx` remiten a ahí).
**Diseño visual:** artefacto «Mapa de Contabilidad» (https://claude.ai/artifact/CYLdtRhkzV2o1wRZsCphmh). Este documento es la versión escrita y vive en el repositorio.
**Diagramas y modelo de datos completo:** `docs/diseno/contabilidad-diagramas.md`.

Prioridades: **P1** = MVP y demo, **P2** = si alcanza el tiempo, **P3** = fuera del MVP.

> **Aviso.** Contabilidad no es una herramienta fiscal: no emite CFDI ni calcula declaraciones (D-104). Los ejemplos de tasas de IVA y el plazo de conservación son de memoria y están **por verificar** contra el SAT y el Código Fiscal de la Federación, con fuente y fecha, como pide `CLAUDE.md`. Todos los montos de ejemplo son simulados.

---

## 1. Qué es y para qué sirve

Contabilidad es un módulo **horizontal**: lleva el registro contable del negocio en partida doble. Cada venta, cada cupón canjeado y cada cierre de caja se convierte en una **póliza** con cargos y abonos que cuadran. Con esas pólizas arma el libro mayor, la balanza de comprobación y el estado de resultados que el locatario le entrega a su persona contadora.

Una fonda, un museo y una miscelánea usan el mismo catálogo de cuentas y las mismas reglas. Lo que cambia por giro (tasa de IVA, propinas, medios de pago) son **reglas en datos**, no código (D-95).

Qué logra el negocio con él:

1. **Ver cuánto vende de verdad**, con los descuentos de cupones a la vista.
2. **Saber si su caja cuadra**: faltantes, sobrantes y ventas que no coinciden con el corte.
3. **Entregarle orden a su contador**: pólizas, mayor y balanza por mes.

Qué **no** es: no guarda ventas (Ventas), cupones (Pasaporte), sesiones de caja (Caja), clientes (Clientes) ni costos de inventario (Inventario). No emite facturas ni declaraciones.

---

## 2. Reglas de arquitectura y dependencias

### 2.1 Reglas R1 a R6 aplicadas a Contabilidad

| Regla | Cómo se cumple |
|---|---|
| R1 | Contabilidad es dueña del esquema `contabilidad`. Los demás entran por `ContabilidadApi` o por el bus. Nadie más escribe pólizas. |
| R2 y R6 | No llama a la API de ningún otro módulo y nadie la llama salvo el panel. Escucha tres eventos; ningún productor escucha a Contabilidad, así que no hay ciclo. |
| R3 | Todo evento lleva UUIDv7. Los listeners deduplican con `evento_procesado` y con la llave natural de cada referencia (E-01, E-02). |
| R4 | Es horizontal. No depende de ningún vertical. |
| R5 | Los únicos cambios a contratos ajenos son campos nuevos y opcionales (D-103). |
| RLS | Cada transacción fija `set_config('app.negocio_id', ..., true)`: desde la sesión en el panel y desde el sobre del evento en los listeners (E-03). Política `aislamiento_por_negocio` con `plataforma.negocio_actual()`, igual que Clientes. Rol de grupo `rol_contabilidad`. |

### 2.2 Eventos que escucha (D-92, aceptada)

Solo estos tres: `venta.registrada`, `cupon.canjeado` y `caja.cerrada`. Lo que no llega por ahí entra como póliza manual del locatario.

### 2.3 Dependencias

| Módulo | Llamadas síncronas | Eventos que consume | Eventos que publica |
|---|---|---|---|
| Contabilidad (horizontal) | Ninguna | `venta.registrada`, `cupon.canjeado`, `caja.cerrada` | `poliza.registrada`, `periodo.cerrado`, `contabilidad.descuadre_detectado`, `contabilidad.pendiente_creado` (P2) |

> **Choque con Inventario (K-10).** `inventario.md` §2 y `inventario-diagramas.md` dicen que Contabilidad consume `venta.*` e `inventario.movimiento_registrado`, «base del costo de ventas». La regla D-92 lo excluye. Hay que decidir cuál manda (pregunta 9).

---

## 3. Alcance

**P1:** activar el módulo con el catálogo base; pólizas automáticas por venta, cupón y corte; pólizas manuales y reversas; periodos y cierre de mes; conciliación de cortes; bandeja de pendientes; balanza, mayor, estado de resultados (antes de costo de ventas) y panorama del día; exportación CSV de pólizas (depende de la pregunta 8).

**P2:** balance general, reabrir periodo, cupones pagados por la plataforma, retiros de caja, código agrupador del SAT, suscripción `actividadContable`, exportación de balanza en XLSX, bitácora de exportaciones, evento `contabilidad.pendiente_creado`.

**P3:** XML de contabilidad electrónica del SAT; costo de ventas (si se autoriza escuchar Inventario).

**Fuera del MVP:** CFDI, declaraciones, nómina, conciliación bancaria.

**Orden de recorte:** primero P3, luego la exportación CSV, luego la conciliación por sesión (queda solo la diferencia de conteo).

---

## 4. Dueño de cada dato

| Dato | Dueño | Qué ve Contabilidad |
|---|---|---|
| Catálogo de cuentas, pólizas, movimientos y saldos | Contabilidad | Propio |
| Periodos contables y su cierre | Contabilidad | Propio |
| Reglas de contabilización y configuración de impuestos del negocio | Contabilidad | Propio |
| Conciliación de cortes de caja | Contabilidad (derivado) | Se calcula con los eventos de Ventas y Caja. No se edita |
| La venta: líneas, total y medio de pago | Ventas | Solo `venta_id` y `total`, más `medio_pago` y `caja_sesion_id` si se agregan (D-103) |
| El canje del cupón | Pasaporte | Solo `cupon_id`, `venta_id` y `descuento.monto`, más `financiador` si se agrega (D-103) |
| Sesiones de caja, fondo, retiros y conteo | Caja | Solo los totales del corte que vienen en `caja.cerrada` |
| Clientes y sus datos personales | Clientes | Nada. Contabilidad no guarda `cliente_id` (D-102) |
| Costo de lo vendido y existencias | Inventario | Nada en el MVP (K-4, K-10) |
| CFDI, declaraciones y nómina | Fuera del MVP | No se generan (D-104) |

---

## 5. Datos

Un esquema de Postgres propio, `contabilidad`, con **14 tablas** (13 en P1). Llaves UUIDv7, `negocio_id` en toda tabla y seguridad por fila. Dinero `numeric(14,2)`; tasas `numeric(5,4)`; redondeo HALF_UP (D-20). El outbox no es de Contabilidad: es `plataforma.event_publication` (V002), la única tabla compartida.

| Tabla | Para qué sirve | Prioridad |
|---|---|---|
| `configuracion` | Una fila por negocio: precios con IVA, tasa por defecto, propinas en el total, mes de inicio del ejercicio | P1 |
| `cuenta` | Catálogo en árbol. Solo las hojas (afectables) reciben movimientos | P1 |
| `regla_contable` | Qué cuentas se cargan y abonan por evento y condición, con tasa, vigencia y **fuente** | P1 |
| `periodo` | Mes contable, abierto o cerrado | P1 |
| `poliza` | Encabezado del asiento. Inmutable | P1 |
| `movimiento` | Renglones de la póliza. Inmutable | P1 |
| `saldo_cuenta` | Saldo por cuenta y periodo, derivado | P1 |
| `venta_contable` | Huella de cada venta contabilizada (idempotencia y conciliación) | P1 |
| `canje_contable` | Huella de cada cupón canjeado; llave `cupon_id` | P1 |
| `corte_contable` | Cada cierre de caja con su diferencia y conciliación | P1 |
| `evento_procesado` | Idempotencia de los tres listeners | P1 |
| `pendiente_contable` | Eventos que esperan a una persona | P1 |
| `preparacion` | Token de preparar → confirmar (D-55), con la forma de `clientes.preparacion` | P1 |
| `exportacion` | Bitácora de exportaciones | P2 |

Las columnas, índices y el diagrama entidad-relación están en `contabilidad-diagramas.md` §5.

### 5.1 Catálogo base (igual para los tres giros)

Se copia a cada negocio al activar el módulo (C-01). Códigos ilustrativos; una persona contadora debe validarlo (pregunta 5).

| Código | Cuenta | Tipo | Naturaleza | La usa |
|---|---|---|---|---|
| 101.01 | Caja | Activo | Deudora | Ventas en efectivo, faltantes y sobrantes |
| 102.01 | Bancos | Activo | Deudora | Ventas por transferencia |
| 103.01 | Deudores por ventas con tarjeta | Activo | Deudora | Ventas con tarjeta (K-7) |
| 105.01 | Cuentas por cobrar a la plataforma | Activo | Deudora | Cupones que paga la plataforma (P2) |
| 109.01 | Efectivo en tránsito | Activo | Deudora | Retiros de caja (P2) |
| 208.01 | IVA trasladado cobrado | Pasivo | Acreedora | IVA de cada venta |
| 209.01 | Propinas por entregar | Pasivo | Acreedora | Propinas (K-6) |
| 401.01 | Ventas | Ingreso | Acreedora | Ventas y reclasificación de cupones |
| 402.01 | Descuentos sobre ventas | Ingreso (contra) | Deudora | Cupones que paga el negocio |
| 601.01 | Faltantes de caja | Gasto | Deudora | Corte con menos efectivo |
| 704.01 | Otros ingresos: sobrantes de caja | Ingreso | Acreedora | Corte con más efectivo |

---

## 6. Reglas de dominio

Prefijos: **A** asientos, **C** catálogo, **T** periodos, **E** eventos, **J** caja, **U** cupones, **F** impuestos. Las letras son del módulo; `CO-` se usa al citarlas desde otro documento para no confundirlas con las de Clientes.

### 6.1 Asientos (A)
| Id | Regla | Prueba de aceptación | P |
|---|---|---|---|
| A-01 | Toda póliza cuadra (cargos = abonos) y tiene al menos dos renglones. | 100.00 de cargo y 99.99 de abono falla con `POLIZA_DESCUADRADA`. | P1 |
| A-02 | Cada renglón tiene cargo o abono mayor que cero, nunca los dos ni ninguno. | Cargo 10 y abono 10 en el mismo renglón falla por CHECK y con `MOVIMIENTO_INVALIDO`. | P1 |
| A-03 | Pólizas y movimientos son inmutables; se corrigen con reversa. | UPDATE o DELETE con el rol de la aplicación falla por permisos y por `plataforma.rechazar_modificacion()`. | P1 |
| A-04 | Solo se mueven cuentas afectables y activas. | Cargar a 101 falla con `CUENTA_NO_AFECTABLE`; a una inactiva, con `CUENTA_INACTIVA`. | P1 |
| A-05 | Folio consecutivo por negocio, periodo y tipo, asignado al confirmar. | Dos pólizas de ingreso simultáneas reciben n y n+1. | P1 |
| A-06 | Una póliza se revierte una sola vez; la reversa invierte cada renglón y cae en el periodo abierto. | Revertir dos veces falla con `POLIZA_YA_REVERTIDA`. | P1 |
| A-07 | Centavos HALF_UP; el IVA se calcula sobre la base y el redondeo queda en el renglón de ventas. | Venta de 100.00 con IVA al 16 % incluido: ventas 86.21 e IVA 13.79. | P1 |
| A-08 | Los saldos se actualizan en la misma transacción que la póliza y son derivados. | Tras 1,000 pólizas aleatorias, la balanza coincide con la recalculada desde movimientos. | P1 |

### 6.2 Catálogo (C)
| Id | Regla | Prueba de aceptación | P |
|---|---|---|---|
| C-01 | Al activar, el negocio recibe copia del catálogo base y las reglas por defecto. El giro no cambia el catálogo. | Restaurante, museo y miscelánea quedan con los mismos códigos y reglas. | P1 |
| C-02 | Código único por negocio; tipo y naturaleza de una hija son los de su padre. | Repetir 101.01 falla con `CODIGO_CUENTA_DUPLICADO`; hija acreedora bajo padre deudor, con `NATURALEZA_INVALIDA`. | P1 |
| C-03 | Una cuenta con movimientos no cambia naturaleza, tipo ni padre, y no se borra: se desactiva. | Cambiar la naturaleza de Caja tras una venta falla con `NATURALEZA_CON_MOVIMIENTOS`. | P1 |
| C-04 | Una cuenta usada por una regla vigente no se desactiva. | Desactivar 208.01 falla con `CUENTA_EN_USO`. | P1 |
| C-05 | Cada cuenta puede llevar código agrupador del SAT. | La exportación XML (P3) se niega si faltan códigos. | P2 |

### 6.3 Periodos (T)
| Id | Regla | Prueba de aceptación | P |
|---|---|---|---|
| T-01 | Un periodo por mes; solo los abiertos reciben pólizas. | Póliza manual en mes cerrado falla con `PERIODO_CERRADO`. | P1 |
| T-02 | Se cierran en orden. | Cerrar octubre con septiembre abierto falla con `PERIODO_ANTERIOR_ABIERTO`. | P1 |
| T-03 | No se cierra con pendientes abiertos de ese mes. | Falla con `PENDIENTES_SIN_RESOLVER`. | P1 |
| T-04 | El periodo siguiente se abre solo: el día 1 por tarea programada o con el primer evento del mes. | Una venta del 1 de noviembre a las 00:05 cae en noviembre aunque la tarea no haya corrido. | P1 |
| T-05 | Un evento de un periodo cerrado entra en el periodo abierto más antiguo, con fecha del día 1, marcado como extemporáneo. | Venta del 31 de octubre con octubre cerrado: póliza del 1 de noviembre con `extemporanea` verdadero. | P1 |
| T-06 | Reabrir es operación sensible, con motivo. | Sin motivo falla con `MOTIVO_REQUERIDO`. | P2 |

### 6.4 Eventos (E)
| Id | Regla | Prueba de aceptación | P |
|---|---|---|---|
| E-01 | Deduplicación por `evento_id` en `evento_procesado`, en la misma transacción que la póliza. | El mismo evento tres veces: una póliza y dos `OMITIDO`. | P1 |
| E-02 | Llave natural por negocio: `venta_id`, `cupon_id` o `corte_id`. | Dos eventos distintos con el mismo `venta_id`: el segundo `OMITIDO` con `DUPLICADO_NATURAL`. | P1 |
| E-03 | `negocio_id` sale del sobre y se fija con `set_config` antes de tocar tablas. | Un evento del negocio A no lee ni escribe filas de B. | P1 |
| E-04 | Ningún evento se pierde ni bloquea la cola: sin regla, cuenta inactiva o datos inválidos, se crea un pendiente. | Venta con medio VALES sin regla: pendiente `REGLA_FALTANTE` y la siguiente venta se contabiliza. | P1 |
| E-05 | Sin orden global: venta y canje se contabilizan en cualquier orden con el mismo resultado. | Las dos permutaciones dan la misma balanza. | P1 |
| E-06 | Una versión desconocida crea un pendiente. | `venta.registrada` v2: pendiente `VERSION_NO_SOPORTADA`. | P1 |
| E-07 | No llama a otro módulo ni lee sus tablas para completar un evento. | Prueba de estructura de módulos: ningún paquete de Contabilidad depende de otro módulo. | P1 |
| E-08 | Los eventos propios van a `plataforma.event_publication` en la misma transacción. | Si la póliza hace rollback, no queda fila en `event_publication`. | P1 |

### 6.5 Caja y conciliación (J)
| Id | Regla | Prueba de aceptación | P |
|---|---|---|---|
| J-01 | Cada `caja.cerrada` crea un corte. Diferencia = contado − esperado. | Esperado 1,850.00 y contado 1,834.50: −15.50. | P1 |
| J-02 | Faltante: cargo 601.01, abono 101.01. Sobrante: cargo 101.01, abono 704.01. Cero: sin póliza. | −15.50 produce esa póliza por 15.50. | P1 |
| J-03 | Se comparan las ventas en efectivo de la sesión con las que reporta Caja; si difieren, `DESCUADRE_VENTAS` y aviso, sin ajuste automático. | Una anulación no escuchada produce `DESCUADRE_VENTAS`. | P1 |
| J-04 | Una venta tardía de una sesión cerrada recalcula la conciliación; el aviso sale una vez por corte. | `DESCUADRE_VENTAS` pasa a `CUADRA` sin segundo aviso. | P1 |
| J-05 | Retiros para depósito: cargo 109.01, abono 101.01. | Retiro de 1,000.00 produce esa póliza. | P2 |

### 6.6 Cupones (U)
| Id | Regla | Prueba de aceptación | P |
|---|---|---|---|
| U-01 | Canje con venta, pagado por el negocio: cargo 402.01 y abono 401.01 por la base sin IVA. El neto no cambia. | Cupón de 29.00 al 16 %: 25.00. El IVA no cambia. | P1 |
| U-02 | Pagado por la plataforma: cargo 105.01. | Mismo cupón con `PLATAFORMA`: cargo 105.01 por 25.00. | P2 |
| U-03 | Canje sin venta no genera póliza; queda `SIN_EFECTO`. | Sin `venta_id`: motivo `SIN_VENTA`. | P1 |
| U-04 | El descuento es positivo y en centavos; cero no tiene efecto; negativo o ausente crea pendiente. | Sin `descuento.monto`: pendiente `DATOS_INVALIDOS`. | P1 |

### 6.7 Impuestos y obligaciones (F)
| Id | Regla | Prueba de aceptación | P |
|---|---|---|---|
| F-01 | El IVA se desglosa según la regla vigente (con tasa o exenta). Las tasas se cargan verificadas contra el SAT, con fuente y fecha. | Museo con regla exenta: todo a 401.01 y nada a 208.01. | P1 |
| F-02 | En ventas cobradas en el momento, el IVA va a 208.01. | Tarjeta de 232.00: abono 208.01 por 32.00. | P1 |
| F-03 | No emite CFDI ni calcula declaraciones. | Documental: el panel y cada archivo lo dicen. | P1 |
| F-04 | Conservación de al menos cinco años (CFF art. 30, **por verificar**). Sin borrado físico. | No existe operación que borre pólizas. | P1 |

---

## 7. API pública (`ContabilidadApi`)

Interfaz Java en el mismo proceso. Resolvedores, controladores REST, listeners y la tarea programada solo llaman a esta interfaz. `negocio_id` nunca es parámetro.

### 7.1 Consultas
| Operación | Entrada | Salida | P |
|---|---|---|---|
| `catalogo` | tipo?, soloActivas, texto? | Árbol de cuentas con saldo del periodo abierto | P1 |
| `cuenta` | cuentaId | Cuenta con las reglas que la usan | P1 |
| `polizas` | periodo, tipo?, origen?, referenciaTipo?, cursor | Página de encabezados | P1 |
| `poliza` | polizaId | Póliza con renglones y reversa | P1 |
| `libroMayor` | cuentaId, periodo, cursor | Saldo inicial, movimientos, saldo final | P1 |
| `balanza` | periodo | Filas por cuenta y totales | P1 |
| `estadoResultados` | desde, hasta (≤ 12 periodos) | Ventas brutas, descuentos, netas, otros ingresos, gastos, resultado antes de costo | P1 |
| `periodos` | anio | Meses con estado y pendientes | P1 |
| `cortes` | periodo, conciliacion?, cursor | Cortes con diferencia y conciliación | P1 |
| `pendientes` | estado, cursor | Bandeja de pendientes | P1 |
| `reglas` | — | Reglas vigentes | P1 |
| `panorama` | fecha | Ventas netas, descuentos, IVA, faltantes, sobrantes, descuadres y pendientes del día | P1 |
| `exportarPolizas` | periodo, CSV | Archivo de pólizas (pregunta 8) | P1 |
| `balanceGeneral` | periodo | Activo, pasivo y capital | P2 |
| `exportarBalanza` | periodo, CSV o XLSX | Archivo de balanza | P2 |
| `exportarSat` | periodo, tipo | XML de contabilidad electrónica | P3 |

### 7.2 Comandos (preparar → confirmar)
| Comando | Campos | Reglas | Evento | P |
|---|---|---|---|---|
| `ActivarContabilidad` | preciosConIva, tasaIvaDefault, propinasEnTotal, ejercicioIniciaMes | C-01, T-04 | — | P1 |
| `CrearCuenta` | codigo, nombre, padreId, afectable, codigoAgrupadorSat? | C-02 | — | P1 |
| `EditarCuenta` | cuentaId, version, nombre?, codigoAgrupadorSat? | C-03 | — | P1 |
| `DesactivarCuenta` | cuentaId, version | C-03, C-04 | — | P1 |
| `RegistrarPolizaManual` | tipo, fecha, concepto, renglones[] | A-01 a A-05, T-01 | `poliza.registrada` | P1 |
| `ReversarPoliza` | polizaId, motivo | A-03, A-06, T-01 | `poliza.registrada` | P1 |
| `ConfigurarRegla` | disparador, condicion?, cuentas, tasaIva?, fuente, vigenteDesde | C-04, F-01 | — | P1 |
| `ResolverPendiente` | pendienteId, accion, motivo? | E-04, A-01 | `poliza.registrada` si contabiliza | P1 |
| `CerrarPeriodo` | periodo | T-02, T-03 | `periodo.cerrado` | P1 |
| `ReabrirPeriodo` | periodo, motivo | T-06 | — | P2 |

### 7.3 Escrituras del sistema (sin confirmación humana, D-22)
| Operación | Entrada | Resultado | Quién la llama | P |
|---|---|---|---|---|
| `contabilizarVenta` | eventoId, ventaId, total, medioPago?, ocurridoEn, cajaSesionId? | `APLICADO`, `OMITIDO` o `PENDIENTE` | Listener de `venta.registrada` | P1 |
| `contabilizarCanje` | eventoId, cuponId, ventaId?, montoDescuento, financiador? | `APLICADO`, `OMITIDO` o `PENDIENTE` | Listener de `cupon.canjeado` | P1 |
| `contabilizarCorte` | eventoId, corteId, cajaSesionId, esperado, contado, ventasEfectivo? | Corte con conciliación y póliza si hubo diferencia | Listener de `caja.cerrada` | P1 |
| `abrirPeriodo` | anio, mes | `ABIERTO` o `YA_ABIERTO` | Tarea del día 1 y listeners (T-04, K-12) | P1 |

### 7.4 Códigos de error
`CONTABILIDAD_NO_ACTIVA`, `CUENTA_NO_ENCONTRADA`, `CUENTA_NO_AFECTABLE`, `CUENTA_INACTIVA`, `CUENTA_EN_USO`, `CODIGO_CUENTA_DUPLICADO`, `NATURALEZA_INVALIDA`, `NATURALEZA_CON_MOVIMIENTOS`, `POLIZA_NO_ENCONTRADA`, `POLIZA_DESCUADRADA`, `POLIZA_SIN_RENGLONES`, `MOVIMIENTO_INVALIDO`, `MONTO_INVALIDO`, `POLIZA_YA_REVERTIDA`, `PERIODO_NO_ENCONTRADO`, `PERIODO_CERRADO`, `PERIODO_ANTERIOR_ABIERTO`, `PENDIENTES_SIN_RESOLVER`, `PENDIENTE_YA_RESUELTO`, `REGLA_SUPERPUESTA`, `TASA_INVALIDA`, `RANGO_DEMASIADO_LARGO`, `MOTIVO_REQUERIDO`, `TOKEN_INVALIDO`, `TOKEN_EXPIRADO`, `TOKEN_USADO`, `COMANDO_CAMBIO`. Reintentables: `CONCURRENCIA` y `ERROR_TEMPORAL`. Cada código tiene un texto fijo en español de México.

---

## 8. Eventos

Sobre común ya programado (`SobreEvento`): `id` UUIDv7, `tipo`, `version`, `negocio_id`, `origen`, `ocurrido_en`, `correlacion_id`, `causa_id`, `datos`. Dinero como cadena decimal; un campo opcional se omite, no va en null. Entrega al menos una vez, sin orden global.

### 8.1 Consume
| Evento | Lo publica | Datos que usa | Qué hace | P |
|---|---|---|---|---|
| `venta.registrada` v1 | Ventas | `venta_id`, `total` (`inventario.md` §7.2, neto de cupón por D-65); `ocurrido_en` del sobre. Nuevos y opcionales (D-103): `medio_pago`, `caja_sesion_id`, `propina` | `contabilizarVenta` | P1 |
| `cupon.canjeado` v1 | Pasaporte | `cupon_id`, `venta_id`, `descuento.monto`, `canjeado_en` (mockup de la app, E-07). Ignora `visitante_id`. Nuevo y opcional: `financiador` | `contabilizarCanje` | P1 |
| `caja.cerrada` v1 | Caja | Contrato completo propuesto (Caja no tiene diseño): `corte_id`, `caja_sesion_id`, `efectivo_esperado`, `efectivo_contado`, `ventas_efectivo`, `retiros` (P2), `cerrada_en` | `contabilizarCorte` | P1 |

### 8.2 Publica
| Evento | Datos | Quién lo escucha | P |
|---|---|---|---|
| `poliza.registrada` v1 | `poliza_id`, `tipo`, `folio`, `origen`, `periodo`, `total`, `referencia_tipo`, `referencia_id` | Ninguno en el MVP; actividad en vivo en P2 | P1 |
| `periodo.cerrado` v1 | `periodo`, `cerrado_en`, `ventas_netas`, `resultado` | Ninguno en el MVP | P1 |
| `contabilidad.descuadre_detectado` v1 | `corte_contable_id`, `corte_id`, `tipo`, `monto` | Panel (alerta, P2). Una vez por corte | P1 |
| `contabilidad.pendiente_creado` v1 | `pendiente_id`, `tipo_evento`, `motivo` | Panel (alerta) | P2 |

Ningún evento de Contabilidad lleva datos personales.

---

## 9. Contratos que necesita de otros módulos

| De quién | Qué | Estado |
|---|---|---|
| Ventas | `venta.registrada` v1 con `medio_pago`, `caja_sesion_id` y `propina` opcionales | Falta congelar (D-103, K-2, K-6) |
| Ventas | `venta.anulada` (con líneas, D-25), si se autoriza escucharlo | Pregunta 1 (K-1) |
| Pasaporte | `cupon.canjeado` v1 con `financiador` opcional | Falta congelar (D-103, K-3). El canje lo dispara Ventas llamando a `Pasaporte.canjearCupon` (D-34) |
| Caja | `caja.cerrada` v1 completo | Caja sin diseño; todo es propuesta |

Ejemplos de carga en `contabilidad-diagramas.md` §4.

---

## 10. GraphQL del panel y REST

El par `prepararComando` / `confirmarComando`, `Preparacion`, `Resultado`, `PageInfo` y los escalares vienen de `compartido.graphqls` (D-55). Contabilidad agrega diez variantes a `ComandoInput`. Sin `negocioId` en ningún tipo ni argumento.

| Id | Operación | Límites | P |
|---|---|---|---|
| G1 | `cuentasContables(tipo, soloActivas, texto)` | Máximo 500, texto ≤ 40 | P1 |
| G2 | `polizas(periodo, tipo, origen, primeros, despues)` | `primeros` ≤ 100 (25 por defecto); sin renglones | P1 |
| G3 | `poliza(id)` | Hasta 50 renglones; id ajeno o inexistente responde nulo | P1 |
| G4 | `libroMayor(cuentaId, periodo, primeros, despues)` | `primeros` ≤ 200; un mes y una cuenta | P1 |
| G5 | `balanzaComprobacion(periodo)` | Lee `saldo_cuenta`; costo fijo | P1 |
| G6 | `estadoResultados(desde, hasta)` | ≤ 12 periodos o `RANGO_DEMASIADO_LARGO` | P1 |
| G7 | `periodosContables(anio)` | ≤ 12 filas | P1 |
| G8 | `cortesContables(periodo, conciliacion, primeros, despues)` | `primeros` ≤ 50 | P1 |
| G9 | `pendientesContables(estado, primeros, despues)` | `primeros` ≤ 50 | P1 |
| G10 | `reglasContables` | DataLoader para cuentas | P1 |
| G11 | `panoramaContable(fecha)` | Solo agregados; hora de `America/Mexico_City` | P1 |
| G12 | `balanceGeneral(periodo)` | Costo fijo | P2 |
| G13 | `prepararComando(input)` | Una variante por solicitud | P1 |
| G14 | `confirmarComando(token)` | Token de un solo uso, 5 minutos (D-23) | P1 |
| W1 | `subscription actividadContable` | Alimentada por el bus (D-101) | P2 |
| R1 | `GET /contabilidad/exportaciones/polizas?periodo=AAAA-MM&formato=csv` | Solo el dueño, límite de tasa | P1 (pregunta 8) |
| R2 | `GET /contabilidad/exportaciones/balanza?periodo=AAAA-MM&formato=csv\|xlsx` | Igual | P2 |
| R3 | `GET /contabilidad/exportaciones/sat?periodo=AAAA-MM&tipo=catalogo\|balanza` | Exige C-05 | P3 |

El esquema propuesto completo está en el Mapa de Contabilidad (sección «Consultas»).

---

## 11. Seguridad

Hereda SG-01 a SG-21 (D-45) con los ajustes del mapa y agrega:

| Id | Regla | Control |
|---|---|---|
| SK-01 | Inmutabilidad en la base | `rol_contabilidad` sin UPDATE ni DELETE en `poliza` y `movimiento`, más el trigger `plataforma.rechazar_modificacion()` |
| SK-02 | Operaciones sensibles | Cerrar, reabrir, reversar, póliza manual y configurar reglas: preparar → confirmar con actor y motivo |
| SK-03 | Prueba de dos negocios | En cada tabla y cada listener |
| SK-04 | Exportaciones | Solo el dueño, límite de tasa, sin `negocio_id` en la URL, bitácora (P2) |
| SK-05 | Dinero exacto | `numeric`, `BigDecimal`, cadena en GraphQL y en eventos |
| SK-06 | Reportes acotados | Un mes en balanza y mayor; ≤ 12 meses en resultados |
| SK-07 | Sin datos personales | Ni eventos ni pólizas llevan nombres ni `cliente_id` |
| SK-08 | Texto y CSV seguros | Conceptos ≤ 200 caracteres de lista permitida; celdas que empiezan con `=`, `+`, `-` o `@` se escapan |
| SK-09 | Conservación | Respaldo diario y retención de cinco años (por verificar, F-04) |

---

## 12. Criterios de aceptación

Una prueba por regla, con el identificador de la regla en el nombre de la prueba (A-01 a A-08, C-01 a C-05, T-01 a T-06, E-01 a E-08, J-01 a J-05, U-01 a U-04, F-01 a F-04): **40 reglas, 40 pruebas**, las de §6. Además:

- Escenario de un día en una fonda (datos simulados): venta en efectivo de 232.00, cupón de 29.00 pagado por el negocio y faltante de 15.50 dan sumas iguales de 272.50, ventas brutas 225.00, descuentos 25.00, ventas netas 200.00.
- Prueba de propiedades: cualquier secuencia y permutación de eventos deja una balanza que cuadra (A-01, A-08, E-05).
- Prueba de aislamiento con dos negocios en las 14 tablas (SK-03).

---

## 13. Fases

### 13.1 Diseño
| Fase | Qué se hace | Estimación |
|---|---|---|
| G1 | Alcance y corte P1/P2 | 0.5 h |
| G2 | Reglas de dominio | 1.5 h |
| G3 | Modelo de datos, índices, RLS e inmutabilidad | 1 h |
| G4 | Contratos de los tres eventos con Ventas, Pasaporte y Caja | 1 h |
| G5 | API, GraphQL y REST | 1 h |
| G6 | Seguridad | 0.5 h |
| G7 | Revisión de esquema y arquitectura | 1 h |

### 13.2 Implementación (cuando se autorice por escrito)
| Fase | Qué se hace | Días-persona | Corte |
|---|---|---|---|
| I0 | Contratos congelados y esquema GraphQL | 0.25 | P1 |
| I1 | Migración Flyway (esquema, `rol_contabilidad`, RLS, trigger) y catálogo base | 0.5 | P1 |
| I2 | Pólizas, partida doble, folios y saldos | 1 | P1 |
| I3 | Tres listeners, idempotencia y pendientes | 1 | P1 |
| I4 | Periodos, cierre y conciliación | 0.5 | P1 |
| I5 | Consultas GraphQL y comandos | 0.75 | P1 |
| I6 | Exportación CSV | 0.25 | P1 |
| I7 | Pantallas del panel | 1 | P1 |
| I8 | Todo lo P2 | 1.25 | P2 |

P1 suma 5.25 días-persona y P2 1.25. Heurísticas sin medir contra el equipo. La migración irá en un archivo nuevo (por ejemplo `V030__contabilidad_esquema.sql`), no editando las existentes.

---

## 14. Decisiones

| Id | Decisión | Estado |
|---|---|---|
| D-91 | Contabilidad es horizontal con esquema `contabilidad`; dueña del catálogo, pólizas, periodos, saldos, reglas y conciliación | PROPUESTA |
| D-92 | Solo escucha `venta.registrada`, `cupon.canjeado` y `caja.cerrada` | ACEPTADA |
| D-93 | Partida doble con pólizas inmutables; corrección por reversa | PROPUESTA |
| D-94 | Una póliza por evento, no una por día | PROPUESTA |
| D-95 | Catálogo base único para los tres giros, copiado al activar; diferencias por giro en reglas | PROPUESTA |
| D-96 | Cupones en bruto: Descuentos sobre ventas contra Ventas, por la base sin IVA | PROPUESTA |
| D-97 | Evento de un mes cerrado entra como extemporáneo en el periodo abierto más antiguo | PROPUESTA |
| D-98 | Bandeja de pendientes en lugar de fallar | PROPUESTA |
| D-99 | La conciliación avisa pero no ajusta ventas | PROPUESTA |
| D-100 | Exportaciones por REST: CSV en P1, balanza en P2, XML SAT en P3 | PROPUESTA (pregunta 8) |
| D-101 | Sin WebSocket en P1; `actividadContable` en P2 | PROPUESTA |
| D-102 | Sin datos personales ni `cliente_id` | PROPUESTA |
| D-103 | Campos nuevos opcionales: `medio_pago`, `caja_sesion_id`, `propina` en `venta.registrada`; `financiador` en `cupon.canjeado`; `caja.cerrada` completo | PROPUESTA |
| D-104 | No es herramienta fiscal | PROPUESTA |

Se numeran desde D-91 porque D-66 a D-75 ya los usa el Mapa de Identidad.

---

## 15. Hallazgos

| Id | Qué se encontró | Qué se propone |
|---|---|---|
| K-1 | Sin `venta.anulada`, una venta anulada se queda como ingreso; con tarjeta no se detecta nunca | Escuchar `venta.anulada` (pregunta 1). Mientras, reversa manual |
| K-2 | `venta.registrada` v1 no trae medio de pago ni sesión de caja | Agregarlos opcionales (D-103); sin `medio_pago` la venta queda pendiente |
| K-3 | `cupon.canjeado` no dice quién paga el descuento | `financiador` opcional; mientras falte, se asume `NEGOCIO` |
| K-4 | Sin Inventario no hay costo de ventas | Resultado «antes de costo»; P3 si se autoriza |
| K-5 | El IVA puede variar por giro y producto | Tasa por regla, con fuente; validación de una persona contadora |
| K-6 | La propina no es ingreso del negocio | Campo `propina` y cuenta 209.01 (pregunta 4) |
| K-7 | El banco deposita tarjeta menos comisión, y no llega como evento | Póliza manual; conciliación bancaria fuera del MVP |
| K-8 | Una póliza por venta crece rápido | Índices y balanza desde `saldo_cuenta` |
| K-9 | Redondeo por venta y por día difieren en centavos | HALF_UP por venta (A-07, D-20), documentado |
| K-10 | Inventario asigna a Contabilidad `inventario.movimiento_registrado` | Decidir cuál manda (pregunta 9) y corregir el documento que pierda |
| K-11 | El diseño debía vivir en `docs/diseno/` | Resuelto con este documento y `contabilidad-diagramas.md` |
| K-12 | La tarea programada `abrirPeriodo` corre sin negocio, y bajo seguridad por fila no ve filas (mismo problema que A1 de la Bitácora, 2026-10-14) | Que la tarea recorra los negocios fijando `app.negocio_id` en cada uno, o lo que el equipo decida en A1. T-04 ya abre el periodo con el primer evento, así que no bloquea |

---

## 16. Preguntas abiertas

1. ¿Contabilidad escucha también `venta.anulada`? (K-1)
2. ¿Quién paga los cupones del pasaporte: el negocio, la plataforma o un patrocinador? (U-02, K-3)
3. ¿Los precios se capturan con IVA incluido en los tres giros, y qué tasa aplica a las entradas de museo? (F-01, K-5)
4. ¿La propina se cobra dentro del total de la venta? (K-6)
5. ¿Quién valida el catálogo base y las reglas fiscales? (C-01, F-01 a F-04)
6. ¿Contabilidad corre en vivo en la demo o como mockup?
7. ¿Autorizas por escrito programar Contabilidad? (D-91)
8. ¿La exportación CSV entra a P1? El alcance original de `CLAUDE.md` la dejaba fuera. (D-100)
9. ¿Contabilidad escucha solo los tres eventos aunque Inventario le asigne `inventario.movimiento_registrado`? (K-4, K-10)

Para aprobar basta responder, por ejemplo: «aprobado salvo 2 y 9».
