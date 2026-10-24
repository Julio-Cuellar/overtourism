# Plan: módulos de Ventas y Caja (v1)

**Fecha:** 2026-10-14 · **Nivel:** Mayor · **Estado:** borrador, esperando confirmación
**Documentos base:** `docs/BITACORA.md` (secciones 1 a 6 y entradas del 2026-10-14), `CLAUDE.md`, `docs/diseno/inventario.md`, `docs/diseno/clientes.md`, `.agent/rules/negocio360.md`, skills `negocio360-reglas` y `planificar-funcionalidad`.
**Diseño:** `docs/diseno/ventas.md` y `docs/diseno/caja.md` (v0.1).

## 1. Resumen ejecutivo

Ventas y Caja son dos módulos horizontales separados para el comerciante (tienda de museo, misceláneo y cualquier otro giro). El P1 completo cuesta de 8 a 11 días-persona y **no cabe** antes del evento (mañana, 15 de octubre). Mayor riesgo: el calendario, y que los contratos con Catálogo y Pasaporte no tienen dueño. Hay que decidir si para la demo se muestra como mockup o se programa un corte mínimo sin base de datos.

## 2. Qué entendí

Se pidió el diseño de Ventas y Caja para el comerciante, separados, con entidades, funciones, reglas, eventos y comunicación con la API. Ventas registra la venta y su pago; Caja controla el dinero del turno y el corte. Los eventos de Ventas alimentan a Caja, Inventario, Clientes y Contabilidad, y el cupón del Pasaporte se canjea al cobrar.

**Queda fuera:** facturación y desglose de IVA, devoluciones parciales, comprobante en PDF, webhooks de proveedores de pago, bot (D-63).
**Supongo:** Catálogo expondrá precios y si un producto admite fracción; Pasaporte aceptará una consulta de cupón sin canje; Identidad dará los permisos de anular y retirar.
**No sé:** quién es dueño de Catálogo y Pasaporte; si Museos y Misceláneos serán reales en la demo (decisión abierta); disponibilidad de cada persona mañana.

## 3. Decisiones que aplican

| ID | Decisión | Estado | Efecto en este plan |
|---|---|---|---|
| D-04 a D-07 | R1 a R6 | ACEPTADA | Ventas no puede llamar a Caja ni Clientes si ellos la escuchan |
| D-20 | Redondeo HALF_UP, dinero con 2 decimales | PROPUESTA | Se usa tal cual |
| D-22, D-23 | Escrituras del sistema sin confirmar; token de 5 min | PROPUESTA | Listeners de Caja; tokens de venta y corte |
| D-25 | `venta.anulada` lleva `lineas[]` | PROPUESTA | Incluido en el contrato |
| D-34 | Ventas llama a `Pasaporte.canjearCupon` de forma síncrona | PROPUESTA | Base de VT-07 |
| D-48, D-65 | `cliente_id` opcional y total neto de cupón | ACEPTADA | Incluidos en `venta.registrada` |
| D-55 | `prepararComando` / `confirmarComando` de plataforma | PROPUESTA | Ventas y Caja agregan variantes, sin mutaciones propias |
| D-63 | Sin bot | ACEPTADA | Sin REST en P1 |
| D-135 | Ventas no conoce a Caja; el canal pasa el `turno_id`; Caja escucha a Ventas | PROPUESTA (nueva) | Evita el ciclo |
| D-136 | Contrato de `venta.registrada` v1 y `venta.anulada` v1 | PROPUESTA (nueva) | Congela lo que ya consumen Clientes e Inventario |
| D-137 | Cupón: se cotiza al preparar y se canjea al confirmar, en la misma transacción | PROPUESTA (nueva) | Necesita `cotizarCupon` en Pasaporte |
| D-138 | Folio consecutivo al confirmar | PROPUESTA (nueva) | |
| D-139 | Lo del museo y del misceláneo vive en Catálogo y en Museos, no en Ventas | PROPUESTA (nueva) | Cumple R4 |
| D-140 | Preparar el corte pone el turno `EN_CORTE` | PROPUESTA (nueva) | Excepción a «preparar no escribe» |
| D-141 | Anular usa preparar → confirmar, con motivo y permiso | PROPUESTA (nueva) | |
| D-142 | Todo el código se documenta con JavaDoc que cita reglas y códigos de error | ACEPTADA (nueva) | Dicho por el usuario el 2026-10-14 |

No hay decisión aceptada que este plan contradiga. Mi primer borrador (fuera del repositorio) hacía que Pasaporte escuchara a Ventas, lo que chocaba con D-34; se corrigió.

## 4. Requisitos y criterios de aceptación

| ID | Requisito | Criterio comprobable | Regla | Rúbrica |
|---|---|---|---|---|
| RQ-01 | Abrir un turno con fondo | Dado una caja sin turno, cuando el cajero confirma `AbrirTurno` con 500.00, entonces hay un turno `ABIERTO` y un movimiento `FONDO` de 500.00 | CJ-01, CJ-02 | Técnica 15% |
| RQ-02 | Cobrar una venta | Dado un turno abierto, cuando el cajero confirma una venta de 150.00 pagada con 200.00 en efectivo, entonces queda con folio, el cambio es 50.00 y sale `venta.registrada` | VT-01 a VT-06 | Técnica, UX |
| RQ-03 | Venta a granel | Cuando se venden 0.5 kg a 120.00, el importe es 60.00; 0.5 de un producto por pieza da `CANTIDAD_NO_FRACCIONABLE` | VT-03 | UX |
| RQ-04 | Canjear cupón del Pasaporte | Cuando se cobra con un cupón de 10 % sobre 150.00, el total es 135.00 y el cupón queda canjeado; si el canje falla, no hay venta | VT-07, VT-08 | Impacto 15%, innovación |
| RQ-05 | La caja refleja la venta | Cuando llega `venta.registrada` con pago mixto, el esperado del turno sube en cada método; el mismo evento dos veces no duplica | CJ-03, CJ-04 | Técnica |
| RQ-06 | Anular una venta | Cuando un encargado anula con motivo, la venta queda `ANULADA`, sale `venta.anulada` y la caja la compensa | VT-09, CJ-03 | Técnica |
| RQ-07 | Corte de caja | Cuando el cajero cuenta 10.00 menos de lo esperado, el corte muestra −10.00 y cierra el turno; con diferencia sobre el umbral pide autorización | CJ-06 a CJ-08, CJ-12 | UX, viabilidad |

**No funcionales:** aislamiento entre negocios (RLS) · idempotencia de listeners · sin `double` en dinero · eventos en la misma transacción · datos simulados rotulados · JavaDoc completo (D-142).

## 5. Alcance

| Prioridad | Funcionalidad | Camino dorado |
|---|---|---|
| 1 | Abrir turno, cobrar venta con efectivo y tarjeta, evento `venta.registrada`, caja que refleja la venta, corte | Sí |
| 1 | Cupón del Pasaporte al cobrar | Sí, si Pasaporte existe |
| 1 | Anular venta | No |
| 2 | Venta a granel con fracciones, conteo por denominación, suscripción `turnoEnVivo`, resumen del día | No |
| 3 | Comprobante PDF, webhooks de pago, devoluciones parciales, IVA desglosado | No |

**Camino dorado de la demo:** el cajero abre turno, cobra una venta con cupón del Pasaporte, el turista ve su cupón canjeado y el cajero hace el corte.
**Fuera del MVP:** lo de prioridad 3.
**Orden de recorte:** 1. P2 completo. 2. Anulación. 3. Cupón (se cobra sin cupón). 4. Base de datos (repositorios en memoria, como Clientes I2).

## 6. Fases y tareas

Ninguna fase de código empieza sin autorización escrita anotada en la Bitácora.

| Fase | Entregable | Criterio de salida | Estimado (O / P / Pe → E) | Responsable | Depende de |
|---|---|---|---|---|---|
| F0 | Revisión y aprobación del diseño | Equipo responde las preguntas de §13 de ambos documentos | 0.1 / 0.25 / 0.5 → 0.3 | Equipo | — |
| F1 | Contratos congelados | `venta.registrada` y `venta.anulada` acordados con Clientes e Inventario; `cotizarCupon` y `canjearCupon` con Pasaporte; `preciosParaVenta` con Catálogo | 0.25 / 0.5 / 1 → 0.5 | A | F0 |
| F2 | Ventas en memoria | `RegistrarVenta` y `AnularVenta` con preparar → confirmar, folio y evento; pruebas VT-xx en verde | 1 / 1.5 / 2.5 → 1.6 | A | F1 |
| F3 | Caja en memoria | Turno, movimientos, listeners idempotentes y corte; pruebas CJ-xx en verde | 1 / 1.5 / 2.5 → 1.6 | B | F1 (en paralelo con F2) |
| F4 | GraphQL del panel | Variantes de `ComandoInput` y consultas; pruebas con `GraphQlTester` | 0.5 / 1 / 1.5 → 1 | A | F2, F3 |
| F5 | Persistencia | Migraciones `V030` (ventas) y `V040` (caja) con RLS; repositorios JDBC | 1 / 1.5 / 2.5 → 1.6 | C | F2, F3, autorización JDBC |
| F6 | Demo | Datos simulados, camino dorado ensayado | 0.25 / 0.5 / 1 → 0.5 | C | F4 |

### Tareas de F2 (ejemplo; las demás fases se parten igual al aprobarse)

| ID | Tarea (≤ medio día) | Hecho cuando | Responsable |
|---|---|---|---|
| F2.1 | Dominio: `Venta`, `LineaVenta`, `PagoVenta`, cálculo de totales con JavaDoc | Pruebas de VT-03, VT-04, VT-08 en verde | A |
| F2.2 | Folios y preparar → confirmar | Pruebas de VT-05, VT-06 y tokens en verde | A |
| F2.3 | Puerto de Catálogo y de Pasaporte (dobles en prueba) | Pruebas de VT-02 y VT-07 en verde | A |
| F2.4 | Eventos `venta.registrada` y `venta.anulada` con `SobreEvento` | Prueba de contrato contra `VentaRegistradaV1` de Clientes en verde | A |

## 7. Dependencias

- Orden: F0 → F1 → (F2 ∥ F3) → F4 → F6; F5 en paralelo a F4 si se autoriza.
- **Contratos a congelar primero:** `venta.registrada` v1 y `venta.anulada` v1 (con quien programa Clientes e Inventario), `PasaporteApi.cotizarCupon` y `canjearCupon`, `CatalogoApi.preciosParaVenta`, `prepararComando` / `confirmarComando` (D-55).
- **Decisiones abiertas que bloquean:** dueño de Catálogo y Pasaporte (bloquea F1); si Museos y Misceláneos son reales en la demo (cambia F6); permisos de Identidad para anular y retirar.
- **Terceros y entorno:** Docker encendido para Testcontainers; Catálogo y Pasaporte no existen todavía, así que F2 usa dobles de prueba.

## 8. Reparto del equipo

| Persona | Módulo o rebanada | Fases | Revisa a |
|---|---|---|---|
| A | Ventas | F1, F2, F4 | B |
| B | Caja | F3 | A |
| C | Persistencia y demo | F5, F6 | A |

Nombres por confirmar entre liz, uriel y julio (D-31). **Punto de integración:** al terminar F2 y F3 (eventos de Ventas entrando a Caja).

## 9. Estimación

| Concepto | Días-persona |
|---|---|
| Suma de tareas (E) | 7.1 |
| Pruebas | incluidas en cada fase (TDD) |
| Documentación (JavaDoc, D-142) | incluida en cada fase |
| Integración y corrección | 0.75 |
| Preparación de la demo | incluida en F6 |
| **Subtotal** | 7.85 |
| Colchón (25 %) | 2 |
| **Total** | **≈ 9.9** |

**Supuestos:** 6 horas efectivas por persona y día; las tres personas también atienden otros módulos.
**Ruta crítica:** F0 → F1 → F2 → F4 → F6 ≈ 4 días calendario.

## 10. Calendario

| Hito | Fecha | Condición |
|---|---|---|
| Diseño aprobado | 2026-10-14 | Respuestas a §13 de ambos diseños |
| Contratos congelados | Por definir | Dueños de Catálogo y Pasaporte |
| Integración 1 | Por definir | F2 y F3 en verde |
| Evento | 2026-10-15 al 17 | |

Hoy es 2026-10-14. Falta **un día** para el 15 de octubre. Capacidad hasta el evento: como máximo 3 personas × 1 día ≈ 3 días-persona, compartidos con otros módulos. Demanda: ≈ 9.9. Veredicto: **no cabe**.

- **Opción A (recorte duro):** solo el camino dorado sin cupón, sin anulación y en memoria (F2.1, F2.2, F2.4, F3 sin conteo por denominación, F4 mínimo): ≈ 3.5 días-persona. Cabe justo solo si dos personas se dedican hoy completo.
- **Opción B (mockup):** Ventas y Caja se muestran como mockup en la demo, como se propuso para Museos y Misceláneos; el código va después del evento con este plan.
- **Opción C (mover el hito):** programar durante el evento (15 al 17), con el riesgo de llegar al pitch sin probar.

Recomiendo **B**: el tiempo de mañana rinde más en terminar Clientes e Inventario, que ya tienen diseño aprobado y código empezado.

## 11. Riesgos

| Riesgo | Prob. | Impacto | Señal de alerta | Mitigación | Dueño |
|---|---|---|---|---|---|
| No alcanza el tiempo | Alta | Alto | F2 no termina hoy | Opción B; orden de recorte de §5 | Equipo |
| Contratos de Catálogo y Pasaporte sin dueño | Alta | Alto | F1 sin respuesta | Dobles de prueba y cobrar sin cupón | A |
| Un evento de venta llega después del corte | Media | Medio | Diferencias inexplicables | CJ-06, CJ-07 y CJ-11 | B |
| Ciclo entre módulos por un atajo | Baja | Alto | `verify()` en rojo | D-135; prueba desde F2 | A |
| Docker apagado | Media | Medio | Testcontainers falla | Encender Docker antes de F5 | C |

## 12. Validación por fase

| Fase | Cómo se valida |
|---|---|
| F0, F1 | Revisión de los documentos y respuesta escrita del equipo en la Bitácora |
| F2, F3 | `mvn -B verify` en verde, cobertura ≥ 80 %, cada regla con su prueba, `ApplicationModules.verify()` en verde, `mvn javadoc:javadoc` sin advertencias |
| F4 | Pruebas con `GraphQlTester` del camino dorado |
| F5 | Pruebas de aislamiento contra PostgreSQL real |
| F6 | Ensayo del camino dorado de punta a punta |

## 13. Preguntas

1. **¿Programamos para la demo o lo mostramos como mockup?** Recomiendo B (mockup ahora, código después). Si A: hoy se dedican dos personas y se deja Inventario. Si B: no se programa nada de Ventas ni Caja antes del evento.
2. **¿Aceptan D-135 a D-141 tal como están?** Recomiendo aprobarlas todas. Si alguna cambia, cambia el contrato de eventos y hay que avisar a Clientes e Inventario.
3. **¿Quién es dueño de Catálogo y de Pasaporte?** Recomiendo que lo sea quien haga Ventas, para congelar F1 en una sola conversación.
4. **¿Quién toma A, B y C?** Recomiendo que Ventas lo tome quien no esté en Clientes ni Identidad.

**Esperando confirmación** antes de continuar. Respóndeme con «aprobado», «aprobado salvo N» o «modifica: ...».
