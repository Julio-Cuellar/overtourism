---
name: diagnosticar-fallo
description: "Diagnostica un error, una prueba que falla o un comportamiento extraño del sistema: saldos que no cuadran, consumos que no descuentan, eventos duplicados o que no llegan, alertas repetidas, datos de otro negocio, bloqueos y fallos intermitentes. Úsala cuando algo falle, no cuadre, 'no funciona', salga un error de Spring, Postgres, GraphQL o del bot, o haya que encontrar la causa antes de arreglar."
---

# Diagnosticar un fallo

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

## Principio
Encuentra la **causa** antes de tocar nada. Un arreglo sin causa conocida es una apuesta. Y nunca «arregles» una prueba debilitándola.

## Método

1. **Describe el síntoma** con precisión: qué esperabas, qué pasó, con qué entrada, desde cuándo. Si el usuario fue vago, haz una o dos preguntas concretas.
2. **Reproduce.** Un caso mínimo que falle siempre. Si no lo logras, dilo y busca qué cambia entre las veces que falla y las que no.
3. **Reúne evidencia** antes de teorizar: el mensaje de error completo, la traza, los registros, los datos involucrados (`movimiento`, `consumo`, `evento_procesado`...). Cita lo que viste.
4. **Formula hipótesis** (la tabla de abajo ayuda) y ordénalas por probabilidad y por lo barato de comprobarlas.
5. **Comprueba una a la vez.** Cambia una cosa, observa. Descarta con evidencia.
6. **Explica la causa raíz** en dos líneas antes de proponer el arreglo.
7. **Escribe una prueba de regresión** que falle por esa causa, y luego arregla (si el código está autorizado).
8. **Revisa si el mismo error puede estar en otro lado.**

## Sospechosos habituales de este proyecto

| Síntoma | Causas probables | Dónde mirar |
|---|---|---|
| El saldo está descuadrado | Descuento doble (llave de idempotencia sin `consumidor`); redondeo; conversión de unidad; entrada con fecha pasada; reversión al CPP actual y no al costo original | `consumo`, `movimiento` (`causa_id`, `consumidor`, `factor_aplicado`) |
| Una venta no descontó inventario | El producto no tiene modo de inventario; el evento no se entregó o quedó pendiente; el consumo quedó `FALLIDO`; error permanente en un listener | Modo en Catálogo, publicaciones pendientes del outbox, `consumo`, `consumo_fallido` |
| Descontó dos veces | Receta y vínculo directo a la vez; reentrega sin candado; llave de consumo incompleta | Modo de inventario del producto, `evento_procesado`, `consumo` |
| Llegó la anulación pero no se revirtió | La anulación llegó antes que la venta (marca `ANULADO_PREVIO`); no trae las líneas | `consumo.estado`, contenido de `venta.anulada` |
| Una alerta llega en cada movimiento | La lógica evalúa el estado y no el **cruce** del umbral | Comparar saldo anterior y nuevo |
| Se repite un aviso de caducidad | Falta marcar el lote como avisado, o dos instancias de la tarea | `lote.aviso_*` |
| Un negocio ve datos de otro | No se fijó `app.negocio_id` en un listener o tarea programada; consulta sin filtro | Contexto de negocio del hilo, política RLS |
| `CONCURRENCIA` frecuente | Muchas escrituras al mismo saldo; reintento ausente o sin límite | Bloqueo optimista, `version` |
| Un evento no llega al panel | La suscripción no filtra por negocio; la publicación quedó pendiente; se cortó el WebSocket | Despachador, adaptador de suscripciones |
| Prueba intermitente | Espera fija en una prueba asíncrona; estado compartido entre pruebas; orden de ejecución; reloj | La prueba y su aislamiento |
| La conversión de unidades da un resultado raro | Unidad propia que choca con una estándar; factor mal definido; unidad inexistente | `unidad_articulo`, `unidad` |
| Error de arranque de Spring | Configuración o secreto faltante; dependencia circular entre módulos; versión incompatible entre Spring Boot, Modulith y GraphQL | Mensaje inicial de la traza, no el final |
| Un diagrama Mermaid no se dibuja | Sintaxis (`;`, tipos con coma en ER, `<br/>` en HTML) | Skill `diagramas-mermaid` |
| El bot duplica una venta | Reintento de la plataforma sin idempotencia por `message_id` | Tabla de mensajes procesados |

## Qué NO hacer
- Cambiar varias cosas a la vez «a ver si así».
- Quitar una validación o una restricción para que el error desaparezca.
- Subir tiempos de espera para ocultar una condición de carrera.
- Atrapar la excepción y seguir (`catch` vacío).
- Culpar a la herramienta antes de descartar el código propio.
- Borrar o «limpiar» datos para que cuadre: el libro de movimientos no se edita, se corrige con movimientos nuevos.

## Formato del reporte

```markdown
## Diagnóstico: <síntoma en una línea>
**Reproducción:** pasos mínimos.
**Evidencia:** lo que vi (mensajes, filas, trazas).
**Causa raíz:** dos líneas.
**Cómo lo comprobé:** qué descarté y qué confirmó.
**Arreglo propuesto:** qué cambiar y qué prueba lo cubre.
**Riesgo / dónde más puede pasar:**
**Qué no pude verificar:**
```

Si no encontraste la causa, dilo y di qué sigue: qué dato falta o qué hipótesis queda.
