# Backlog Scrum en Jira (proyecto HTC): Clientes e Inventario

**Fecha:** 2026-10-13
**Estado:** PROPUESTA. Nada de esto está creado en Jira hasta que el equipo lo apruebe.
**Fuentes:** `docs/diseno/clientes.md` (v0.2, secciones 6, 7 y 16), `docs/diseno/inventario.md` (v0.1, secciones 4, 6, 11 y 12) y `docs/BITACORA.md`.
**Proyecto Jira:** HTC «Hackathon Tech Capital», tablero 2, sitio `juliocuellar.atlassian.net`.

## 1. Cómo se estimó (léelo antes de comparar con el diseño)

- **1 punto = 1 hora efectiva de una persona.** Un día-persona son unos 5 puntos (supuesto de `estimacion.md`, no un dato del equipo).
- Los puntos de cada historia son estimación mía a nivel de historia. **Suman entre 2 y 4 veces los días-persona de los diseños**: Clientes I1 a I6 suman 88 puntos (≈ 18 d-p) contra 4 d-p del diseño; el MVP de Inventario suma 108 puntos (≈ 22 d-p) contra 9 d-p. El propio diseño admite que sus cifras son heurísticas. Hay que recalibrar con la velocidad real después del Sprint 0.
- **Capacidad supuesta:** 3 personas, 5 h efectivas por día. Sprint 0: solo el 13 y 14 de octubre son de construcción (15 al 17 es el evento), unos 30 puntos. Sprints semanales: unos 40 puntos (supone media jornada de enfoque; cámbialo cuando el equipo diga su disponibilidad).
- Etapas que ya están hechas y no se vuelven a crear: Clientes I0 y el esqueleto Maven (fase 0 de Inventario).

## 2. Sprints

| Sprint | Fechas | Meta | Puntos |
|---|---|---|---|
| Sprint 0 · Demo | 13 al 17 oct | Clientes con base de datos, aviso de privacidad y alta/búsqueda de clientes; contratos y decisiones desbloqueados | 31 |
| Sprint 1 | 19 al 23 oct | Clientes completo en datos y reglas: historial de compras, consentimiento, anonimización y ARCO | 38 |
| Sprint 2 | 26 al 30 oct | API GraphQL de Clientes cerrada y segura; empieza Inventario (unidades y artículos) | 38 |
| Sprint 3 | 2 al 6 nov | Inventario: movimientos con CPP, API pública y preparar → confirmar | 33 |
| Sprint 4 | 9 al 13 nov | Inventario: recetas y consumo por venta | 26 |
| Sprint 5 | 16 al 20 nov | Inventario: lotes con FEFO y GraphQL del panel | 34 |
| Sin sprint (backlog) | | Series de Inventario (8), Clientes P2 (18) y documentos legales (9) | 35 |

**Compuerta:** la Bitácora solo autoriza programar Clientes. Antes de empezar cualquier historia de Inventario hay que anotar la autorización escrita (tarea PLT-3, en el Sprint 0).

## 3. Épicas, historias y sprint

Cada historia lleva en su descripción las reglas del diseño que cumple y su prueba de aceptación. Etiquetas: `clientes` o `inventario`, y `P1`, `P2` o `plataforma`.

### PLT · Plataforma y coordinación

| Clave | Tipo | Resumen | Pts | Sprint |
|---|---|---|---|---|
| PLT-1 | Tarea | Encender Docker Desktop y decidir dónde vive la base de la demo (D-16, D-15) | 1 | 0 |
| PLT-2 | Tarea | Congelar con el dueño de Ventas `cliente_id` opcional y total neto de cupón (D-48, D-65) y `venta.anulada` con líneas (D-25) | 2 | 0 |
| PLT-3 | Tarea | Aprobar el diseño de Inventario (sección 13), anotar la autorización escrita en la Bitácora y definir quién es dueño de Catálogo y de Ventas | 1 | 0 |
| PLT-4 | Tarea | Actualizar `inventario.md` con D-24 a D-30, D-55 y D-63 | 2 | 3 |

### Clientes P1

**CLI-I1 · Persistencia y aislamiento por negocio** (Sprint 0, 8 pts)
- Migraciones Flyway de las 9 tablas P1 del esquema `clientes` (3)
- Seguridad por fila con `app.negocio_id` en todas las tablas (2)
- Bloqueo de UPDATE y DELETE en `consentimiento` y `acceso_cliente` (P-03) (1)
- Entidades y repositorios con Testcontainers: aislamiento entre dos negocios y único parcial de teléfono (2)

**CLI-I2 · Aviso de privacidad y alta de clientes** (Sprint 0, 19 pts)
- `PublicarAvisoPrivacidad` con validación `AVISO_INCOMPLETO` y versionado (L-01, L-02) (3)
- `RegistrarCliente` con normalización E.164, unicidad de teléfono, correo y fecha de nacimiento (I-01 a I-03, I-07, I-08, L-03) (5)
- `EditarCliente` con bloqueo optimista `version` (I-06) (3)
- Búsqueda sin acentos y sugerencias; con varios candidatos decide la persona (I-04, I-05) (3)
- Patrón preparar → confirmar: tabla `preparacion`, token de un solo uso de 5 minutos, vaciado de datos personales al confirmar (D-55, C-3) (5)

**CLI-I3 · Historial de compras desde Ventas** (Sprint 1, 11 pts)
- Listener de `venta.registrada` → `aplicarCompra` idempotente; sin `cliente_id` se ignora (H-01, H-02, H-05) (3)
- Listener de `venta.anulada` → `revertirCompra` y marca `ANULADO_PREVIO` (H-03) (3)
- Resumen: total, ticket promedio, HALF_UP a 2 decimales (H-04) (2)
- Estados derivados INACTIVO y FRECUENTE con reglas versionadas (H-06, D-64) (3)

**CLI-I4 · Consentimiento, anonimización y auditoría** (Sprint 1, 14 pts)
- `RegistrarConsentimiento` solo de inserción, con forma, medio y versión del aviso (P-03, P-04, L-04) (3)
- `puedeContactar` (P-02; la sección 7.1 lo marca P2 y el plan I4 lo incluye) (2)
- `AnonimizarCliente` irreversible con preparar → confirmar, evento en la misma transacción (B-01 a B-05) (5)
- Auditoría de acceso a la ficha en `acceso_cliente` (P-05) (2)
- Purga de `preparacion` a las 24 h y de `evento_procesado` a los 90 días (SC-10) (2)

**CLI-I5 · Derechos ARCO y exportación** (Sprint 1, 13 pts)
- Calendario de días inhábiles como regla versionada y cálculo de días hábiles (L-05, C-10) (3)
- `RegistrarSolicitudArco` y `AmpliarSolicitudArco` (L-05, L-08) (3)
- `ResolverSolicitudArco` con motivo en negativas e identidad verificada (L-06, L-07) (3)
- Exportación de datos por REST con identidad verificada y auditoría (L-06) (3)
- Anonimizar ligado a una solicitud de cancelación (B-06) (1)

**CLI-I6 · API GraphQL y seguridad del panel** (Sprint 2, 23 pts)
- Autenticación y contexto de negocio en `/graphql`; cerrar antes del primer resolvedor (SG-01 a SG-03) (5)
- Límites de profundidad, alias, costo, tamaño y tasa (SG-04 a SG-10) (5)
- Resolvedores de las 8 consultas con DataLoader y teléfono enmascarado (P-05) (5)
- `prepararComando` y `confirmarComando` con las 8 variantes de `ComandoInput` (5)
- Pruebas de dos negocios y revisión de seguridad (3)

### Clientes en backlog (sin sprint)

**CLI-I7 · P2: etiquetas, vínculo con visitante e inactividad** (18 pts)
- `EtiquetarCliente` y catálogo de etiquetas (3)
- Vínculo con turista que comparte su alias, eventos de Visitantes (V-01 a V-03) (8). Depende de un contrato que aún no existe
- Alerta de inactividad `cliente.inactivo_detectado` y suscripción `alertasClientes` (7)

**CLI-DOC · Cumplimiento documental LFPDPPP, antes de un piloto con datos reales** (9 pts, no es código)
- Aviso de privacidad de cada negocio, revisado por alguien con formación jurídica (3)
- Contrato de encargo y lista de encargados, incluida la base de datos y, si se usa, la IA (3)
- Procedimiento de vulneraciones y registro de incidentes (L-09) (2)
- Designar a la persona de datos personales y redactar el compromiso de confidencialidad (1)

### Inventario (MVP: prioridades 1 y 2, sin series)

**INV-F1 · Dominio base y unidades** (Sprint 2, 15 pts)
- Catálogo de unidades estándar y conversión kg↔g, l↔ml (U-02) (2)
- Unidades propias por artículo y `UNIDAD_NO_CONVERTIBLE` (U-03, U-04) (3)
- Utilidad de redondeo HALF_UP en `compartido` (D-20) (2)
- Artículo: crear, editar y baja lógica; unidad base y rastreo inmutables con movimientos (U-01, A-04) (3)
- Migraciones del esquema `inventario` y seguridad por fila (5)

**INV-F2 · Movimientos valuados con CPP** (Sprint 3, 18 pts)
- Entrada y CPP con saldo positivo, cero o negativo (C-02, C-03, C-08) (5)
- Salida, merma y ajuste por conteo (C-04, C-06, A-03) (5)
- Reversión al costo original (C-05) (2)
- Kardex y movimientos inmutables (U-05) (3)
- Alertas por cruce de umbral y stock negativo permitido (A-01, A-02) (3)

**INV-F3 · API pública, preparar → confirmar y eventos** (Sprint 3, 13 pts)
- `InventarioApi`: consultas `saldo`, `saldos`, `cpp`, `aBase`, `kardex` (3)
- Preparar → confirmar con token ligado a negocio, actor y comando (D-29) (5)
- Eventos por outbox: `movimiento_registrado`, `bajo_minimo`, `repuesto`, `saldo_negativo` (3)
- Auditoría de confirmaciones y plantillas de error en español (2)

**INV-F4 · Recetas y consumo por venta** (Sprint 4, 26 pts)
- `registrarConsumo` atómico e idempotente por `(negocio, causaId, consumidor)` (K-01, K-02) (5)
- `revertirConsumo` y marca `ANULADO_PREVIO` (K-03, K-04) (3)
- Listener de Ventas para productos con `VINCULO_DIRECTO` (3)
- Esquema `restaurantes`: receta, versión e insumos validados con `aBase` (D-09, D-27) (5)
- Listener de Restaurantes, `consumo_fallido` y descuento por platillo (5)
- Modo de inventario en Catálogo y conflicto vínculo/receta (K-05, D-17). Depende del dueño de Catálogo (3)
- Costo teórico del platillo (2)

**INV-F5 · Lotes, FEFO y caducidades** (Sprint 5, 16 pts)
- Lotes en las entradas y fecha de caducidad (L-02) (3)
- Salida FEFO repartida en varios lotes y política por negocio (L-03, L-04) (5)
- Lotes caducados fuera de FEFO y consumo sin lote (L-05, L-06) (3)
- Tarea diaria de caducidades, idempotente por lote y fecha (L-07, D-26) (3)
- Propuesta de merma por caducidad con preparar → confirmar (L-08) (2)

**INV-F7 · GraphQL y alertas del panel** (Sprint 5, 18 pts)
- Consultas de Inventario con `extend`, sin redefinir lo de la plataforma (5)
- Las variantes de `ComandoInput` de los 10 comandos de Inventario (5)
- Suscripciones `alertasNegocio` y `saldoActualizado` (5)
- Reglas de seguridad específicas de Inventario (3)

**INV-F6 · Números de serie** (backlog, sin sprint, 8 pts). Primero en recortarse (D-12)
- Entradas con N series únicas y estados (S-01, S-02) (3)
- Venta de serie existente y disponible; venta sin serie deja tarea y evento (S-03, S-04, D-30) (3)
- Rechazar series como insumo de receta (S-05, D-27) (2)

## 4. Riesgos que este plan hace visibles

1. **La demo del 15 de octubre no alcanza para Clientes P1.** El Sprint 0 llega a base de datos, aviso y alta/búsqueda. El camino de la sección 16.2 (registrar, sugerir, ver el resumen, consentir, anonimizar) necesita también I3 y I4, que están en el Sprint 1. La pregunta abierta 7 del diseño sigue sin respuesta.
2. **Las estimaciones por historia son 2 a 4 veces las del diseño.** Revisar después del Sprint 0.
3. **Inventario depende de Ventas y de Catálogo**, cuyos dueños no están identificados (PLT-2, PLT-3, INV-F4).
4. **Inventario no está autorizado para programarse** (ver Compuerta).
