---
name: contratos-eventos
description: "Define, cambia o revisa eventos del bus del proyecto: nombres, sobre común, campos, versiones, compatibilidad, idempotencia, orden, quién publica y quién consume. Úsala cuando se cree un evento nuevo, se modifique uno ya publicado, se conecte un listener, o se hable de outbox, Spring Modulith, eventos duplicados, eventos fuera de orden o de la diferencia entre evento y llamada a la API."
---

# Contratos de eventos

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

## ¿Evento o llamada a la API?

| Si necesitas... | Usa |
|---|---|
| Anunciar algo que ya pasó y que a quien publica no le importa quién reaccione | **Evento** (asíncrono) |
| Pedir un dato o una acción y usar la respuesta | **API pública** del módulo (síncrono) |
| Que una operación sea todo o nada junto con la tuya | **API pública**, en la misma transacción |
| Reaccionar a algo sin bloquear a quien lo origina | **Evento** |

Un evento nunca devuelve respuesta. Si te descubres esperando una, es una llamada a la API.

## Nombre
`modulo.hecho_en_pasado`, en minúsculas, con guion bajo. Ejemplos: `venta.registrada`, `inventario.bajo_minimo`. El nombre describe un **hecho**, no una orden (`venta.registrada`, no `registrar_venta`).

## Sobre común
| Campo | Regla |
|---|---|
| `id` | UUIDv7 generado por quien publica |
| `tipo` | Nombre del evento |
| `version` | Entero ≥ 1, de ese `tipo` |
| `negocio_id` | Siempre presente. De él sale el contexto del listener |
| `origen` | Módulo que publica |
| `ocurrido_en` | ISO-8601 en UTC. Momento del hecho, no de la entrega |
| `correlacion_id` | Hilo de negocio (en una venta, el `venta_id`) |
| `causa_id` | `id` del evento que lo provocó, o nulo |
| `datos` | Carga del contrato |

## Carga (`datos`)
- Hechos con IDs y valores, no entidades completas.
- Sin datos personales.
- Dinero y cantidades como **cadenas decimales** (`"180.00"`). Nunca `double`.
- Fechas en ISO-8601. Fechas de calendario local (como una caducidad) en `AAAA-MM-DD`.
- Si el consumidor necesita saber a qué líneas o elementos aplica el hecho, la carga los trae. No lo obligues a adivinar ni a consultar de vuelta.

## Versiones y compatibilidad (R5)

| Cambio | ¿Permitido en la misma versión? |
|---|---|
| Agregar un campo opcional | Sí |
| Agregar un valor a un enumerado que los consumidores ya ignoran | Sí, avisando a los consumidores |
| Quitar un campo | No → versión nueva |
| Renombrar un campo | No → versión nueva |
| Cambiar el tipo o el significado de un campo | No → versión nueva |
| Volver obligatorio un campo opcional | No → versión nueva |

Una versión nueva convive con la anterior hasta que todos los consumidores migren. Anótalo en la Bitácora.

## Garantías de entrega (y lo que obligan)
- **Al menos una vez** → el evento puede llegar repetido → el consumidor deduplica por `evento_id` en su propia tabla `evento_procesado`.
- **Sin orden global** → puede llegar la anulación antes que la venta → el consumidor tolera el desorden (por ejemplo con una marca).
- **Se publica en la misma transacción que la escritura** (outbox) → nunca hay un hecho sin evento ni un evento sin hecho.

## Lista para un consumidor
- [ ] Deduplica por `evento_id` antes de hacer efectos.
- [ ] Fija el contexto con el `negocio_id` **del sobre**.
- [ ] Si llama a una API, esa operación también es idempotente (una llave por causa y consumidor). Dos candados, no uno.
- [ ] Clasifica los errores: **reintentables** (concurrencia, error temporal → no marca el evento como procesado) y **permanentes** (dato inválido → registra el fallo en una tabla propia y marca el evento).
- [ ] No bloquea a quien publica.
- [ ] Ignora los eventos o líneas que no le corresponden.
- [ ] Tiene prueba de evento duplicado y de evento fuera de orden.

## Plantilla para documentar un evento

```markdown
**`modulo.hecho` v1**
| Campo | Tipo | Notas |
|---|---|---|

Publica: <módulo>. Cuándo: <condición exacta, por ejemplo al cruzar el umbral y no en cada movimiento>.
Consumen: <módulos> y qué hacen con él.
Idempotencia del consumidor: <llave>.
```

## Antes de entregar
- ¿El evento nuevo aparece en las tablas de interacciones asíncronas y en el mapa de comunicación?
- ¿Cambió el conteo de eventos publicados? Actualiza **todos** los documentos que lo mencionan.
- ¿Congelaste el contrato con el dueño del módulo que lo publica o lo consume? Si no, queda como pendiente.
