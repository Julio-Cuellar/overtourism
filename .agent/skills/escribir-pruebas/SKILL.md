---
name: escribir-pruebas
description: "Guía el desarrollo con pruebas primero (TDD) en Java 21 y Spring Boot para este proyecto: pruebas unitarias del dominio, de integración con Postgres real, de módulos con Spring Modulith, de eventos y de GraphQL. Úsala solo cuando el código esté autorizado y haya que escribir o mejorar pruebas, cubrir una regla del diseño, subir la cobertura o diagnosticar una prueba que falla."
---

# Escribir pruebas (TDD)

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

## Condición previa
La Bitácora debe autorizar código. En fase de diseño no crees pruebas ni carpetas de pruebas: lo que sí puedes hacer es mejorar la **tabla de criterios de aceptación** del diseño.

## El ciclo, sin saltarse pasos
1. **ROJO.** Escribe la prueba de una regla. Ejecútala. **Debe fallar**, y por la razón correcta (no por un error de compilación).
2. **VERDE.** Escribe el mínimo código que la haga pasar. Ejecútala.
3. **REFACTOR.** Mejora sin cambiar el comportamiento. Ejecuta todo.
4. Repite con la siguiente regla. Al final, verifica cobertura de **80% o más** en lo que tocaste.

Si la prueba pasa a la primera sin código nuevo, sospecha de ella.

## Nombres y estructura
- Una prueba por regla del diseño, con el identificador en el nombre: `regla_C03_entrada_con_saldo_negativo_toma_el_costo_de_la_entrada`.
- Nombres en español que describan el comportamiento, no el método.
- Estructura Preparar–Actuar–Verificar (Arrange–Act–Assert), separada por líneas en blanco.
- Una idea por prueba.

## Qué tipo de prueba para qué

| Qué pruebas | Tipo | Herramienta (verifica versiones compatibles) |
|---|---|---|
| Cálculos y reglas puras (CPP, conversión de unidades, redondeo, FEFO) | Unitaria, sin Spring ni base | JUnit 5 |
| Consultas, restricciones, RLS, transacciones | Integración con **Postgres real** | Testcontainers (requiere Docker encendido) |
| Que un módulo publique y consuma bien sus eventos | De módulo | `@ApplicationModuleTest` y la API `Scenario` de Spring Modulith |
| Que nadie viole los límites entre módulos | Arquitectura | `ApplicationModules.verify()` |
| Consultas, mutaciones y suscripciones GraphQL | API | `GraphQlTester` |
| Un flujo completo de punta a punta | E2E, pocas y solo las críticas | La que se elija para el proyecto |

No simules la base de datos en las pruebas de integración: las restricciones y las políticas por fila son lo que se está probando.

## Casos que este proyecto siempre necesita
- **Idempotencia:** la misma llamada o el mismo evento dos veces produce un solo efecto.
- **Orden:** la anulación llega antes que la venta.
- **Atomicidad:** una línea inválida no deja nada escrito.
- **Concurrencia:** dos escrituras simultáneas sobre el mismo saldo; una reintenta o falla con `CONCURRENCIA`.
- **Redondeo:** cifras calculadas **a mano** en la prueba, incluyendo medias centésimas.
- **Umbrales:** la alerta se dispara al cruzar, no en cada movimiento.
- **Aislamiento:** un negocio no ve ni toca datos de otro.
- **Token:** vencido, reutilizado, de otro negocio, comando alterado.
- **Fechas:** caducidad hoy, mañana y ayer; cambio de día en hora de México.
- **Error reintentable vs permanente.**

## Datos de prueba
- Inventados y claramente falsos. Nada de datos reales ni de producción.
- Construidos con funciones auxiliares pequeñas (fábricas), no copiados en cada prueba.
- Los valores esperados se escriben a mano en la prueba, **no** se calculan con el mismo código que se prueba.

## Cuando una prueba falla
1. Léela completa y reproduce el fallo.
2. Decide: ¿el código está mal o la prueba está mal?
3. Arregla la causa. **Nunca** relajes ni borres una prueba para que pase, salvo que la regla de diseño esté mal; en ese caso, registra el cambio en la Bitácora.
4. Una prueba que a veces falla (intermitente) es un hallazgo, no algo que se reintenta hasta que pase. Busca esperas fijas, orden de ejecución o estado compartido.
5. Agrega una prueba de regresión para cada bug que encuentres.

## Pruebas asíncronas
No uses esperas fijas (`sleep`). Usa la espera por condición que ofrece la herramienta de eventos o una espera con tiempo máximo y sondeo.

## Entrega
Di qué ejecutaste, cuántas pasaron y cuál es la cobertura. Si algo no se pudo ejecutar (por ejemplo, Docker apagado), dilo.
