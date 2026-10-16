---
name: disenar-modulo
description: "Diseña un módulo del proyecto Negocio 360, horizontal o vertical: qué es suyo y qué no, reglas de dominio con identificador, API pública, eventos que publica y consume, modelo de datos, criterios de aceptación y fases. Úsala cuando haya que crear o ampliar el diseño de un módulo como Ventas, Caja, Clientes, Contabilidad, Catálogo, Restaurantes, Museos o Misceláneos, o decidir si algo es horizontal o vertical."
---

# Diseñar un módulo

> Complementa `negocio360-reglas`. Si chocan, gana esa skill. Esta skill produce **documentos**, no código.

## Cuándo usarla
El usuario pide diseñar un módulo nuevo o ampliar uno existente. El modelo a imitar es `docs/diseno/inventario.md` y `docs/diseno/inventario-diagramas.md`. Léelos antes de empezar.

## Horizontal o vertical
Pregúntate: **¿lo usaría igual una fonda, un museo y una tienda?**
- Sí → **horizontal** (inventario, caja, ventas, clientes, contabilidad, catálogo). No sabe qué es un platillo ni una entrada de museo.
- No, depende del giro → **vertical** (restaurantes, museos, misceláneos). Depende de los horizontales y nunca al revés (R4).

## Pasos

1. **Dueño de los datos.** Escribe qué datos son del módulo y cuáles no, con el dueño de cada uno. Si dudas de a quién pertenece algo, pregúntate quién tiene que poder cambiarlo sin pedir permiso a nadie.
2. **Alcance.** MVP con prioridades, fuera del MVP, y orden de recorte. Calcula contra los días que quedan.
3. **Reglas de dominio con identificador.** Agrupadas por tema y con una letra fija (`U-01`, `C-02`, `K-03`...). Una regla es una frase comprobable; si no se puede escribir una prueba para ella, reescríbela.
4. **Modelo de datos.** Con la skill `modelar-base-de-datos`.
5. **API pública.** Operaciones de lectura, de escritura de usuario (preparar → confirmar) y de sistema. Con la skill `contratos-api`.
6. **Eventos.** Los que publica y los que consume. Con la skill `contratos-eventos`.
7. **Qué necesita de otros módulos.** Tabla: de quién, qué, y si ya está definido o falta congelarlo.
8. **Diagramas.** Secuencias de los flujos principales y de los casos raros. Con la skill `diagramas-mermaid`.
9. **Criterios de aceptación.** Una prueba por regla, citando su identificador.
10. **Fases y reparto**, y los pendientes que bloquean la aprobación.
11. **Revisa tu propio diseño** con la skill `revisar-diseno` antes de entregarlo.

## Estructura del documento
`docs/diseno/<modulo>.md`: 1 Objetivo · 2 Reglas de arquitectura y dependencias · 3 Alcance · 4 Reglas de dominio · 5 Datos · 6 API pública · 7 Eventos · 8 Contratos que necesita de otros · 9 GraphQL del panel · 10 Consistencia y seguridad · 11 Criterios de aceptación · 12 Fases · 13 Pendientes para aprobar.

`docs/diseno/<modulo>-diagramas.md`: mapa de comunicación · interacciones síncronas y asíncronas con su función · secuencias · diagramas de eventos · modelo de datos con todas las columnas e índices · hallazgos.

## Huecos típicos que hay que buscar
Estos aparecieron al diseñar Inventario. Revísalos en cada módulo:
- **Exclusividad entre módulos.** Si dos módulos no pueden actuar sobre lo mismo a la vez, el control lo tiene un tercero neutral, no uno de los dos.
- **Llaves de idempotencia.** Si dos consumidores reciben el mismo evento, la llave debe incluir quién consume.
- **Orden de eventos.** Qué pasa si la anulación llega antes que la venta.
- **Dónde se guarda un fallo permanente** de un listener, y quién lo ve.
- **Avisos que no deben repetirse.** Hay que guardar que ya se avisó.
- **Eventos con datos suficientes.** Un evento de anulación sin las líneas obliga a los consumidores a adivinar.
- **Trazabilidad.** Qué evento causó qué (`correlacion_id`, `causa_id`).
- **Qué pasa sin dato.** Valores por defecto de cada configuración.

## Entregable
Los dos documentos, una entrada en la Bitácora y una lista de preguntas abiertas numeradas, para que el equipo apruebe diciendo, por ejemplo, "aprobado salvo 3 y 7". No pases a código.
