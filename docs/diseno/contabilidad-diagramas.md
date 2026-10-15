# Contabilidad: diagramas, interacciones y modelo de datos

**Versión:** 0.2
**Fecha:** 2026-10-15
**Estado:** PROPUESTA. Acompaña a `docs/diseno/contabilidad.md`; las reglas (`A-01`, `E-02`...) y decisiones (`D-xx`) remiten a ese documento y a la Bitácora.
**Diseño visual:** artefacto «Mapa de Contabilidad» (https://claude.ai/artifact/CYLdtRhkzV2o1wRZsCphmh). Los diagramas de aquí son los mismos.

Todos los montos de los diagramas son datos simulados.

---

## 1. Mapa de comunicación

Contabilidad tiene cinco puertas de entrada y todas llaman a `ContabilidadApi`.

```mermaid
flowchart LR
  P["Panel del locatario"]
  CT["Persona contadora (recibe archivos)"]
  subgraph E["Entrada"]
    H["POST /graphql: consultas y comandos"]
    W["WebSocket /graphql: actividadContable (P2)"]
    R["REST: GET /contabilidad/exportaciones/..."]
    BL["Listeners del bus: 3 eventos"]
    TP["Tarea programada: abrirPeriodo"]
  end
  S["Servicios del módulo: ContabilidadApi"]
  DB[("Postgres, esquema contabilidad")]
  B(("Bus de eventos"))
  AD["Adaptador de suscripciones (P2)"]
  P --> H
  P --> W
  P --> R
  R -.-> CT
  B --> BL
  H --> S
  R --> S
  BL --> S
  TP --> S
  S --> DB
  S --> B
  B --> AD
  AD --> W
```

Qué mirar: la suscripción (P2) se alimenta del bus, no de la base. La persona contadora no entra al sistema: recibe el archivo que el locatario descarga.

---

## 2. Interacciones síncronas y asíncronas

| Id | Tipo | Operación o evento | Dirección | Función | P |
|---|---|---|---|---|---|
| G1–G12 | GraphQL (síncrono) | Consultas del panel | Panel → Contabilidad | Leer catálogo, pólizas, mayor, balanza, resultados, periodos, cortes, pendientes, reglas y panorama | P1 (G12 P2) |
| G13–G14 | GraphQL (síncrono) | `prepararComando`, `confirmarComando` | Panel → Contabilidad | Los diez comandos contables | P1 |
| W1 | WebSocket (asíncrono) | `actividadContable` | Contabilidad → Panel | Avisos de pólizas, descuadres y pendientes | P2 |
| R1–R3 | REST (síncrono) | `GET /contabilidad/exportaciones/...` | Panel → Contabilidad | Archivos para la persona contadora | P1 / P2 / P3 |
| J1 | Java (mismo proceso) | `ContabilidadApi` | Resolvedores y REST → Contabilidad | Única puerta de los adaptadores | P1 |
| J2 | Java (mismo proceso) | `contabilizarVenta`, `contabilizarCanje`, `contabilizarCorte` | Listeners → Contabilidad | Escrituras del sistema (D-22) | P1 |
| J3 | Java (mismo proceso) | `abrirPeriodo` | Tarea programada → Contabilidad | Día 1, 00:00 `America/Mexico_City` (ver K-12) | P1 |
| B1 | Bus (asíncrono) | `venta.registrada` | Ventas → Contabilidad | Póliza de ingreso | P1 |
| B2 | Bus (asíncrono) | `cupon.canjeado` | Pasaporte → Contabilidad | Reclasificación del descuento | P1 |
| B3 | Bus (asíncrono) | `caja.cerrada` | Caja → Contabilidad | Corte y conciliación | P1 |
| B4 | Bus (asíncrono) | `poliza.registrada` | Contabilidad → bus | Traza | P1 |
| B5 | Bus (asíncrono) | `periodo.cerrado` | Contabilidad → bus | Traza | P1 |
| B6 | Bus (asíncrono) | `contabilidad.descuadre_detectado` | Contabilidad → bus | Alerta | P1 |
| B7 | Bus (asíncrono) | `contabilidad.pendiente_creado` | Contabilidad → bus | Alerta | P2 |

Total: 28 vías (14 GraphQL, 1 WebSocket, 3 REST, 3 Java, 7 bus).

---

## 3. Eventos

### 3.1 Diagrama de eventos

```mermaid
flowchart LR
  subgraph VT["Ventas"]
    v1["venta.registrada v1 + medio_pago y caja_sesion_id"]
  end
  subgraph PA["Pasaporte"]
    p1["cupon.canjeado v1 + financiador"]
  end
  subgraph CA["Caja"]
    c1["caja.cerrada v1 (contrato por definir)"]
  end
  bus(("Bus de eventos, plataforma.event_publication"))
  subgraph CO["Contabilidad"]
    l1["Listener: contabilizarVenta"]
    l2["Listener: contabilizarCanje"]
    l3["Listener: contabilizarCorte"]
    pe[("pendiente_contable")]
    e1["poliza.registrada v1"]
    e2["periodo.cerrado v1"]
    e3["contabilidad.descuadre_detectado v1"]
    e4["contabilidad.pendiente_creado v1 (P2)"]
  end
  subgraph FU["Consumidores futuros"]
    f1["Panel: actividad contable (P2)"]
    f2["Panel: alertas (P2)"]
  end
  v1 --> bus
  p1 --> bus
  c1 --> bus
  bus --> l1
  bus --> l2
  bus --> l3
  l1 -. sin regla .-> pe
  l2 -. datos invalidos .-> pe
  l3 --> e3
  l1 --> e1
  l2 --> e1
  l3 --> e1
  e1 --> bus
  e2 --> bus
  e3 --> bus
  e4 -.-> bus
  bus -.-> f1
  bus -.-> f2
```

Qué mirar: entran exactamente tres flechas, una por evento permitido (D-77). Ningún productor escucha a Contabilidad.

### 3.2 Una venta entregada dos veces

```mermaid
sequenceDiagram
  autonumber
  participant V as Ventas
  participant B as Bus de eventos
  participant L as Listener de Contabilidad
  participant C as ContabilidadApi
  participant DB as Postgres (contabilidad)
  V->>B: venta.registrada (outbox de Ventas)
  B->>L: entrega al menos una vez
  L->>DB: fija negocio_id del sobre para RLS (E-03)
  L->>C: contabilizarVenta(evento)
  Note over C,DB: una sola transacción
  C->>DB: inserta evento_procesado(evento_id)
  alt el evento_id ya existe
    C-->>L: OMITIDO, sin cambios
  else venta_id ya contabilizada
    C->>DB: resultado OMITIDO, DUPLICADO_NATURAL
  else no hay regla para el medio de pago
    C->>DB: inserta pendiente_contable REGLA_FALTANTE
  else todo en orden
    C->>DB: abre el periodo si es mes nuevo (T-04)
    C->>DB: póliza de ingreso, renglones y folio
    C->>DB: suma a saldo_cuenta e inserta venta_contable
    C->>DB: si la sesión ya cerró, recalcula la conciliación (J-04)
    C->>DB: encola poliza.registrada en plataforma.event_publication
  end
  C-->>L: resultado
  L-->>B: confirma la entrega
  B->>L: segunda entrega del mismo evento
  L->>C: contabilizarVenta(evento)
  C-->>L: OMITIDO por evento_id
```

Qué mirar: `evento_procesado` se inserta primero, dentro de la misma transacción. Si algo falla, todo hace rollback y la reentrega empieza de cero.

---

## 4. Contratos de los eventos que consume

Base: `venta.registrada` en `inventario.md` §7.2; `cupon.canjeado` en el mockup de la app del turista (E-07); `caja.cerrada` no existe todavía porque Caja no tiene diseño.

```text
// Todos los campos nuevos son opcionales (R5, D-88). Datos de ejemplo inventados.

venta.registrada v1   (Ventas; base en inventario.md §7.2)
{
  "venta_id": "0192f3a0-6a00-7000-8000-00000000b002",
  "total": "232.00",                 // neto de cupón cuando aplica (D-65)
  "lineas": [ ... ],                 // Contabilidad no las usa
  "cliente_id": "...",               // de Clientes (D-48); Contabilidad lo ignora
  "medio_pago": "EFECTIVO",          // nuevo
  "caja_sesion_id": "0192f3a0-5f00-7000-8000-00000000c001",   // nuevo
  "propina": "0.00"                  // nuevo, por definir (K-6)
}

cupon.canjeado v1   (Pasaporte; base en el mockup de la app, E-07)
{
  "cupon_id": "0192f3a0-0a00-7000-8000-00000000e001",
  "visitante_id": "...",             // seudónimo; Contabilidad lo ignora
  "venta_id": "0192f3a0-6a00-7000-8000-00000000b002",   // puede faltar (U-03)
  "descuento": { "monto": "29.00", "moneda": "MXN" },
  "canjeado_en": "2026-10-15T18:40:12Z",
  "financiador": "NEGOCIO"           // nuevo: NEGOCIO o PLATAFORMA
}

caja.cerrada v1   (Caja; módulo sin diseño, todo es propuesta)
{
  "corte_id": "0192f3a0-9900-7000-8000-00000000f003",
  "caja_sesion_id": "0192f3a0-5f00-7000-8000-00000000c001",
  "efectivo_esperado": "1850.00",
  "efectivo_contado": "1834.50",
  "ventas_efectivo": "1350.00",      // nuevo, para J-03
  "retiros": []                      // nuevo, P2 (J-05)
}
```

---

## 5. Modelo de datos

### 5.1 Dominio

```mermaid
erDiagram
  configuracion ||--o{ regla_contable : "tasa por defecto"
  cuenta |o--o{ cuenta : "padre de"
  cuenta ||--o{ movimiento : recibe
  cuenta ||--o{ saldo_cuenta : acumula
  cuenta ||--o{ regla_contable : "cargo o abono"
  periodo ||--o{ poliza : contiene
  periodo ||--o{ saldo_cuenta : "corta en"
  poliza ||--|{ movimiento : "2 o mas renglones"
  poliza |o--o| poliza : "revierte a"
  poliza ||--o| venta_contable : respalda
  poliza |o--o| canje_contable : respalda
  poliza |o--o| corte_contable : respalda
  venta_en_Ventas ||..o| venta_contable : "venta_id sin FK"
  cupon_en_Pasaporte ||..o| canje_contable : "cupon_id sin FK"
  corte_en_Caja ||..o| corte_contable : "corte_id sin FK"
  configuracion {
    uuid negocio_id PK
    boolean precios_con_iva
    numeric tasa_iva_default
    smallint ejercicio_inicia_mes
    bigint version
  }
  cuenta {
    uuid id PK
    uuid negocio_id
    varchar codigo UK
    varchar nombre
    text tipo
    text naturaleza
    uuid padre_id FK
    boolean afectable
    boolean de_sistema
    varchar codigo_agrupador_sat "P2"
    text estado
  }
  regla_contable {
    uuid id PK
    uuid negocio_id
    text disparador
    text condicion
    text fuente
    uuid cuenta_cargo_id FK
    uuid cuenta_abono_id FK
    uuid cuenta_impuesto_id FK
    numeric tasa_iva "nulo si exento"
    timestamptz vigente_desde
  }
  periodo {
    uuid id PK
    uuid negocio_id
    smallint anio
    smallint mes
    text estado
    timestamptz cerrado_en
  }
  poliza {
    uuid id PK
    uuid negocio_id
    uuid periodo_id FK
    text tipo
    int folio
    date fecha
    text origen
    uuid evento_id UK "nulo si es manual"
    text referencia_tipo
    uuid referencia_id
    boolean extemporanea
    uuid revierte_poliza_id FK
    numeric total
  }
  movimiento {
    uuid id PK
    uuid poliza_id FK
    smallint renglon
    uuid cuenta_id FK
    numeric cargo
    numeric abono
  }
  saldo_cuenta {
    uuid cuenta_id PK
    uuid periodo_id PK
    numeric saldo_inicial
    numeric cargos
    numeric abonos
    numeric saldo_final
  }
  venta_contable {
    uuid id PK
    uuid venta_id UK "ref. logica a Ventas"
    uuid caja_sesion_id
    text medio_pago
    numeric total
    numeric base
    numeric iva
    uuid poliza_id FK
  }
  canje_contable {
    uuid id PK
    uuid cupon_id UK
    uuid venta_id "ref. logica, nulo"
    numeric monto_descuento
    text financiador
    text estado
    uuid poliza_id FK
  }
  corte_contable {
    uuid id PK
    uuid corte_id UK
    uuid caja_sesion_id
    numeric efectivo_esperado
    numeric efectivo_contado
    numeric diferencia
    text conciliacion
    uuid poliza_id FK
  }
```

### 5.2 Control

```mermaid
erDiagram
  evento_procesado ||--o| pendiente_contable : "si no se pudo"
  evento_procesado ||--o| poliza : "si se aplico"
  pendiente_contable |o--o| poliza : "resuelto con"
  poliza ||--o{ event_publication : "poliza.registrada"
  preparacion ||..o| poliza : "comando manual"
  exportacion }o..|| periodo : "exporta P2"
  evento_procesado {
    uuid evento_id PK "UUIDv7 del productor"
    uuid negocio_id
    text tipo
    smallint version
    text resultado "APLICADO, OMITIDO, PENDIENTE"
    text motivo
    timestamptz recibido_en
  }
  pendiente_contable {
    uuid id PK
    uuid negocio_id
    uuid evento_id UK
    text tipo_evento
    jsonb datos "solo campos del contrato"
    text motivo
    text estado
    uuid resuelto_con_poliza_id FK
  }
  preparacion {
    text token_hash PK "SHA-256 en hex"
    uuid negocio_id
    text tipo_comando
    jsonb comando "se vacia al confirmar"
    timestamptz expira_en
    timestamptz usado_en
  }
  event_publication {
    uuid id PK "esquema plataforma, Spring Modulith"
    text listener_id
    text event_type
    text serialized_event "SobreEvento con causa_id"
    timestamptz publication_date
    timestamptz completion_date
  }
  exportacion {
    uuid id PK
    uuid negocio_id
    text tipo
    text formato
    uuid actor
    int filas
  }
  poliza {
    uuid id PK
    uuid evento_id UK
  }
  periodo {
    uuid id PK
  }
```

Qué mirar: las huellas (`venta_contable`, `canje_contable`, `corte_contable`) apuntan a otros módulos sin llave foránea. El outbox es `plataforma.event_publication` (V002); Contabilidad no tiene uno propio.

### 5.3 Columnas e índices

Todas las tablas viven en el esquema `contabilidad`, llevan `negocio_id uuid NOT NULL`, tienen seguridad por fila forzada con la política `aislamiento_por_negocio` (`negocio_id = plataforma.negocio_actual()`) y dan permisos solo a `rol_contabilidad`. Dinero `numeric(14,2)`, tasas `numeric(5,4)`, fechas y horas `timestamptz` en UTC.

#### `configuracion` (P1)
| Columna | Tipo | Notas |
|---|---|---|
| `negocio_id` | uuid PK | Una fila por negocio |
| `activada_en` | timestamptz | Cuándo se copió el catálogo base (C-01) |
| `precios_con_iva` | boolean | Por defecto verdadero (F-01) |
| `tasa_iva_default` | numeric(5,4) | Se usa en canjes que llegan antes que su venta (U-01). Valor por verificar |
| `propinas_en_total` | boolean | K-6 |
| `ejercicio_inicia_mes` | smallint | 1 = enero; CHECK 1–12 |
| `version` | bigint | Bloqueo optimista |

#### `cuenta` (P1)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | UUIDv7 |
| `codigo` | varchar(20) | UNIQUE (`negocio_id`, `codigo`) (C-02) |
| `nombre` | varchar(120) | |
| `tipo` | text | CHECK: ACTIVO, PASIVO, CAPITAL, INGRESO, COSTO, GASTO |
| `naturaleza` | text | CHECK: DEUDORA, ACREEDORA |
| `padre_id` | uuid FK nulo | `cuenta(id)` |
| `afectable` | boolean | A-04 |
| `de_sistema` | boolean | C-04 |
| `codigo_agrupador_sat` | varchar(10) nulo | P2 (C-05) |
| `estado` | text | CHECK: ACTIVA, INACTIVA |
| `version` | bigint | |

Índices: `ux_cuenta_codigo (negocio_id, codigo)`, `ix_cuenta_padre (negocio_id, padre_id)`.

#### `regla_contable` (P1)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | |
| `disparador` | text | CHECK: VENTA, CUPON, CORTE_FALTANTE, CORTE_SOBRANTE, CORTE_RETIRO |
| `condicion` | text nulo | Medio de pago o financiador |
| `cuenta_cargo_id`, `cuenta_abono_id` | uuid FK | |
| `cuenta_impuesto_id` | uuid FK nulo | 208.01 si se desglosa IVA |
| `tasa_iva` | numeric(5,4) nulo | Nulo = exento |
| `fuente` | text NOT NULL | De dónde salió y fecha de verificación (`CLAUDE.md`, reglas fiscales) |
| `vigente_desde` | timestamptz | |
| `vigente_hasta` | timestamptz nulo | |
| `version` | bigint | |

Índices: `ix_regla_vigente (negocio_id, disparador, condicion, vigente_desde DESC)`. Restricción de exclusión sobre el rango de vigencia por (`negocio_id`, `disparador`, `condicion`) para `REGLA_SUPERPUESTA`.

#### `periodo` (P1)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | |
| `anio`, `mes` | smallint | UNIQUE (`negocio_id`, `anio`, `mes`) |
| `estado` | text | CHECK: ABIERTO, CERRADO |
| `abierto_en` | timestamptz | |
| `cerrado_en`, `cerrado_por` | timestamptz, uuid; nulos | CHECK: ambos nulos si está ABIERTO |
| `reaperturas` | smallint | T-06 |
| `version` | bigint | |

#### `poliza` (P1, solo inserción)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | UUIDv7; cursor de paginación |
| `periodo_id` | uuid FK | |
| `tipo` | text | CHECK: INGRESO, EGRESO, DIARIO |
| `folio` | int | UNIQUE (`negocio_id`, `periodo_id`, `tipo`, `folio`) (A-05) |
| `fecha` | date | |
| `concepto` | varchar(200) | |
| `origen` | text | CHECK: AUTOMATICA, MANUAL, REVERSA |
| `evento_id` | uuid nulo | UNIQUE (`negocio_id`, `evento_id`) |
| `referencia_tipo`, `referencia_id` | text, uuid; nulos | VENTA, CANJE, CORTE, PENDIENTE |
| `extemporanea` | boolean | T-05 |
| `revierte_poliza_id` | uuid FK nulo | UNIQUE (A-06) |
| `total` | numeric(14,2) | CHECK > 0 |
| `creado_por` | text | |
| `creado_en` | timestamptz | |

Índices: `ix_poliza_periodo (negocio_id, periodo_id, tipo, id)`, `ix_poliza_referencia (negocio_id, referencia_tipo, referencia_id)`. Disparador `plataforma.rechazar_modificacion()` en UPDATE y DELETE (A-03, SK-01).

#### `movimiento` (P1, solo inserción)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | |
| `poliza_id` | uuid FK | |
| `renglon` | smallint | UNIQUE (`poliza_id`, `renglon`); CHECK 1–50 |
| `cuenta_id` | uuid FK | |
| `cargo`, `abono` | numeric(14,2) | CHECK ≥ 0 y `(cargo > 0) <> (abono > 0)` (A-02) |
| `concepto` | varchar(120) nulo | |

Índices: `ix_movimiento_cuenta (negocio_id, cuenta_id, poliza_id)`. Mismo disparador que `poliza`. El cuadre (A-01) se valida en la aplicación y con un disparador diferido al confirmar la transacción.

#### `saldo_cuenta` (P1, derivado)
| Columna | Tipo | Notas |
|---|---|---|
| `cuenta_id`, `periodo_id` | uuid | PK compuesta con `negocio_id` |
| `saldo_inicial`, `cargos`, `abonos`, `saldo_final` | numeric(14,2) | A-08 |
| `version` | bigint | |

#### `venta_contable` (P1)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | |
| `venta_id` | uuid | UNIQUE (`negocio_id`, `venta_id`) (E-02) |
| `caja_sesion_id` | uuid nulo | D-88 |
| `medio_pago` | text nulo | D-88 |
| `total`, `base`, `iva`, `propina` | numeric(14,2) | |
| `ocurrido_en` | timestamptz | |
| `poliza_id` | uuid FK | |
| `estado` | text | CHECK: CONTABILIZADA, REVERTIDA |

Índice: `ix_venta_sesion (negocio_id, caja_sesion_id) WHERE medio_pago = 'EFECTIVO'` para la conciliación (J-03).

#### `canje_contable` (P1)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | |
| `cupon_id` | uuid | UNIQUE (`negocio_id`, `cupon_id`) (E-02) |
| `venta_id` | uuid nulo | |
| `monto_descuento`, `base_descuento` | numeric(14,2) | Viene en `descuento.monto` |
| `financiador` | text | CHECK: NEGOCIO, PLATAFORMA |
| `estado` | text | CHECK: CONTABILIZADO, SIN_EFECTO |
| `motivo` | text nulo | SIN_VENTA, MONTO_CERO |
| `poliza_id` | uuid FK nulo | CHECK: no nulo si CONTABILIZADO |

#### `corte_contable` (P1)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | |
| `corte_id` | uuid | UNIQUE (`negocio_id`, `corte_id`) |
| `caja_sesion_id` | uuid | |
| `efectivo_esperado`, `efectivo_contado`, `diferencia` | numeric(14,2) | CHECK `diferencia = efectivo_contado - efectivo_esperado` |
| `ventas_efectivo_caja` | numeric(14,2) nulo | |
| `ventas_efectivo_contables` | numeric(14,2) | |
| `conciliacion` | text | CHECK: CUADRA, DIFERENCIA_CAJA, DESCUADRE_VENTAS |
| `descuadre_publicado_en` | timestamptz nulo | J-04 |
| `poliza_id` | uuid FK nulo | |
| `cerrada_en` | timestamptz | |

Índice: `ix_corte_sesion (negocio_id, caja_sesion_id)`.

#### `evento_procesado` (P1)
| Columna | Tipo | Notas |
|---|---|---|
| `evento_id` | uuid | PK con `negocio_id` |
| `tipo`, `version` | text, smallint | |
| `recibido_en` | timestamptz | |
| `resultado` | text | CHECK: APLICADO, OMITIDO, PENDIENTE |
| `motivo` | text nulo | |

#### `pendiente_contable` (P1)
| Columna | Tipo | Notas |
|---|---|---|
| `id` | uuid PK | |
| `evento_id` | uuid | UNIQUE (`negocio_id`, `evento_id`) |
| `tipo_evento` | text | |
| `datos` | jsonb | Solo los campos del contrato; sin datos personales |
| `motivo` | text | REGLA_FALTANTE, CUENTA_INACTIVA, DATOS_INVALIDOS, VERSION_NO_SOPORTADA |
| `estado` | text | ABIERTO, CONTABILIZADO, DESCARTADO |
| `resuelto_con_poliza_id` | uuid FK nulo | |
| `motivo_resolucion` | varchar(200) nulo | Obligatorio si DESCARTADO |
| `creado_en`, `resuelto_en` | timestamptz | |

Índice: `ix_pendiente_abierto (negocio_id, creado_en) WHERE estado = 'ABIERTO'`.

#### `preparacion` (P1)
Misma forma que `clientes.preparacion` (V020): `token_hash text PK` (SHA-256 en hex), `negocio_id`, `tipo_comando`, `comando jsonb` (se vacía al confirmar), `actor`, `expira_en` (5 minutos, D-23), `usado_en`. Purga a las 24 horas (depende de A1 de la Bitácora).

#### `exportacion` (P2)
`id`, `negocio_id`, `tipo` (POLIZAS, BALANZA, SAT), `periodo char(7)`, `formato` (CSV, XLSX, XML), `actor`, `filas`, `creado_en`.

---

## 6. Estados

```mermaid
stateDiagram-v2
  [*] --> ABIERTO : abrirPeriodo, dia 1 o primer evento del mes
  ABIERTO --> CERRADO : CerrarPeriodo, sin pendientes y el anterior cerrado
  CERRADO --> ABIERTO : ReabrirPeriodo con motivo (P2)
  CERRADO --> [*]
```

```mermaid
stateDiagram-v2
  [*] --> RECIBIDO
  RECIBIDO --> OMITIDO : ya procesado o llave natural repetida
  RECIBIDO --> APLICADO : hay regla y datos validos
  RECIBIDO --> PENDIENTE : falta regla, cuenta inactiva o datos invalidos
  PENDIENTE --> CONTABILIZADO : ResolverPendiente
  PENDIENTE --> DESCARTADO : ResolverPendiente con motivo
  APLICADO --> [*]
  OMITIDO --> [*]
  CONTABILIZADO --> [*]
  DESCARTADO --> [*]
```

```mermaid
stateDiagram-v2
  [*] --> CUADRA : contado igual al esperado y ventas coinciden
  [*] --> DIFERENCIA_CAJA : contado distinto del esperado
  [*] --> DESCUADRE_VENTAS : ventas en efectivo no coinciden con Caja
  DESCUADRE_VENTAS --> CUADRA : llega una venta tardia de la sesion
  DIFERENCIA_CAJA --> [*]
  CUADRA --> [*]
```

---

## 7. Flujos

### 7.1 Venta y cupón en cualquier orden

```mermaid
sequenceDiagram
  autonumber
  participant P as Pasaporte
  participant V as Ventas
  participant B as Bus de eventos
  participant L as Listeners de Contabilidad
  participant DB as Postgres (contabilidad)
  P->>B: cupon.canjeado (29.00, NEGOCIO, venta_id)
  V->>B: venta.registrada (232.00 neto, EFECTIVO)
  Note over B: el bus no garantiza el orden
  B->>L: llega primero cupon.canjeado
  L->>DB: base 25.00 con la tasa por defecto
  L->>DB: póliza D: cargo Descuentos 25.00, abono Ventas 25.00
  B->>L: llega después venta.registrada
  L->>DB: póliza I: cargo Caja 232.00, abono Ventas 200.00 e IVA 32.00
  Note over L,DB: si llegan al revés, las mismas dos pólizas y la misma balanza (E-05)
```

### 7.2 Cierre de caja y conciliación

```mermaid
sequenceDiagram
  autonumber
  participant C as Caja
  participant B as Bus de eventos
  participant L as Listener de Contabilidad
  participant DB as Postgres (contabilidad)
  actor U as Locatario
  C->>B: caja.cerrada (esperado 1850.00, contado 1834.50, ventas_efectivo 1350.00)
  B->>L: entrega
  L->>DB: evento_procesado y corte_contable
  L->>DB: diferencia = -15.50
  L->>DB: póliza D: cargo Faltantes 15.50, abono Caja 15.50
  L->>DB: suma ventas en efectivo de la sesión = 1118.00
  alt coincide con ventas_efectivo
    L->>DB: conciliacion DIFERENCIA_CAJA
  else no coincide
    L->>DB: conciliacion DESCUADRE_VENTAS
    L->>B: contabilidad.descuadre_detectado (una sola vez)
  end
  U->>L: abre cortesContables en el panel
  Note over U,DB: si luego llega una venta de esa sesión, la conciliación se recalcula (J-04)
```

### 7.3 Cierre de mes

```mermaid
sequenceDiagram
  autonumber
  actor U as Locatario
  participant G as POST /graphql
  participant C as ContabilidadApi
  participant DB as Postgres (contabilidad)
  participant B as Bus de eventos
  U->>G: periodosContables y pendientesContables
  G-->>U: octubre ABIERTO, 1 pendiente
  U->>G: resuelve el pendiente (preparar y confirmar)
  U->>G: prepararComando con cerrarPeriodo 2026-10
  G->>C: preparar(comando)
  C->>DB: revisa septiembre cerrado y cero pendientes
  C->>DB: revisa que la balanza cuadre
  C-->>G: token y resumen con ventas netas y resultado del mes
  G-->>U: Al cerrar octubre ya no podrás registrar pólizas en ese mes
  U->>G: confirmarComando(token)
  Note over C,DB: una sola transacción
  C->>DB: periodo CERRADO, saldos finales congelados
  C->>DB: abre noviembre si no existe, con saldos iniciales
  C->>DB: encola periodo.cerrado
  C-->>U: Octubre cerrado
```

### 7.4 Corregir una venta anulada (mientras no se escuche `venta.anulada`)

```mermaid
sequenceDiagram
  autonumber
  actor U as Locatario
  participant G as POST /graphql
  participant C as ContabilidadApi
  participant DB as Postgres (contabilidad)
  U->>G: cortesContables(conciliacion DESCUADRE_VENTAS)
  G-->>U: corte con 232.00 de diferencia en ventas
  U->>G: polizas(periodo, referenciaTipo VENTA)
  U->>U: identifica la venta anulada
  U->>G: prepararComando con reversarPoliza y motivo
  C-->>U: resumen: se invertirán 3 renglones
  U->>G: confirmarComando(token)
  C->>DB: póliza REVERSA con renglones invertidos
  C->>DB: venta_contable REVERTIDA y recalcula la conciliación
  C-->>U: corte en CUADRA
```

---

## 8. Hallazgos

Los hallazgos K-1 a K-12 están en `contabilidad.md` §15. Los que afectan a otros documentos:

- **K-10:** `inventario.md` §2 e `inventario-diagramas.md` (líneas del mapa y de eventos) asignan a Contabilidad `inventario.movimiento_registrado`. Si se confirma D-77 tal cual, hay que corregirlos.
- **K-12:** la tarea `abrirPeriodo` tiene el mismo problema que las purgas (A1, Bitácora 2026-10-14): sin `app.negocio_id` no ve filas.
