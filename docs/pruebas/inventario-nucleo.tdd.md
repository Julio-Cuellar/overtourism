# Evidencia TDD — Núcleo de Inventario (2026-10-15)

**Plan de origen:** `/ecc:plan` de la sesión (núcleo de Inventario a partir del análisis de Clinerya). **Alcance:** U-01 a U-05, C-01 a C-08, A-01 a A-04 de `docs/diseno/inventario.md`, con repositorios en memoria.
**Entorno:** `mvn -B verify` en `backend/`, JDK 21 (`/usr/lib/jvm/java-21-openjdk-amd64`), Maven 3.9.12.

## Ciclos

| Ciclo | RED (evidencia) | GREEN (evidencia) |
|---|---|---|
| 1. Núcleo: dominio, preparar → confirmar, comandos, consultas y eventos | `mvn test-compile` con 11 clases de prueba y sin código de producción: **146 errores de compilación**, todos por clases inexistentes | 108 de 109 pruebas del módulo; la única falla fue un error de mi doble de pruebas (`unica()` con 2 preparaciones), corregido en la prueba. Luego `mvn -B verify`: **931 pruebas, 0 fallas** (824 previas + 107) |
| 2. Correcciones de la revisión de código (A1, A2, A3, M2, M3) | 3 clases de prueba nuevas (10 pruebas): **9 fallan** por la razón esperada (total de $465.00 en lugar de $463.75, tiempo agotado con `1E+3000000`, saldo y costo que no caben, ids fuera de orden, escritura con saldo distinto al comparado). La décima ya pasaba | `mvn -B verify`: **941 pruebas, 0 fallas**, `All coverage checks have been met` |

## Qué garantizan las pruebas

| Regla | Qué se garantiza | Prueba |
|---|---|---|
| U-02 a U-04 | kg↔g, l↔ml, unidad propia con factor, unidad de otra dimensión o desconocida → `UNIDAD_NO_CONVERTIBLE`; nunca se adivina | `UnidadesTest` |
| U-05 | el movimiento guarda lo capturado, la unidad, el factor y la cantidad base | `MovimientosTest` |
| C-02, C-03 | entrada con saldo > 0 promedia (2000 g a 0.018 + 1000 g a 0.021 = 0.0190); con saldo ≤ 0 toma el costo de la entrada | `SaldoTest`, `MovimientosTest` |
| C-04, A-01 | la salida se valúa al CPP sin cambiarlo y puede dejar saldo negativo | `SaldoTest`, `MovimientosTest` |
| C-06, A-03 | el ajuste por conteo calcula la diferencia y la registra al CPP; contar lo mismo → `CONTEO_SIN_DIFERENCIA` | `MovimientosTest` |
| C-07 | una entrada con fecha pasada no reescribe el historial | `MovimientosTest` |
| C-08 | el costo se captura por unidad capturada | `MovimientosTest`, `PrecisionYLimitesTest` |
| A-02 | las alertas salen solo al cruzar el umbral | `CruceDeUmbralTest`, `MovimientosTest` |
| A-04 | baja lógica solo con saldo 0; libera el nombre | `ArticulosTest` |
| 4.3 | HALF_UP: 1.00005 → 1.0001, 0.375 → 0.38, −0.375 → −0.38 | `RedondeoTest`, `SaldoTest` |
| 6.2 | token de un solo uso: expirado, usado, de otro negocio o actor, comando alterado, saldo cambiado → `COMANDO_CAMBIO` | `PreparacionTest` |
| 10 | un negocio no ve ni toca artículos de otro | `ConsultasTest`, `MovimientosTest` |
| 7.3, R5 | contrato v1 de `movimiento_registrado`, `bajo_minimo`, `repuesto`, `saldo_negativo` con cifras en cadena | `EventosInventarioContratoTest` |
| R1, R4 | `ApplicationModules.verify()` en verde con el módulo `inventario` | `EstructuraModulosTest` |
| Revisión A1 | 25 kg a $18.55/kg = $463.75 exactos; 1000 cajas de 24 a $100 = $100,000.00 | `PrecisionYLimitesTest` |
| Revisión A2 | un decimal con exponente enorme se rechaza en menos de 500 ms | `PrecisionYLimitesTest` |
| Revisión A3 | el saldo, el costo total y lo capturado deben caber en sus columnas | `PrecisionYLimitesTest` |
| Revisión M2 | el saldo que se compara con el resumen es el mismo que respalda la escritura | `CarreraEnConfirmarTest` |
| Revisión M3 | los ids de movimientos crecen aunque el reloj tenga microsegundos | `SecuenciaDeIdsTest` |

## Cobertura (JaCoCo, antes del ciclo 2)
`inventario.aplicacion` 95.7 % instrucciones / 80.4 % ramas; `inventario.dominio` 96.8 % / 89.3 %; `inventario.eventos` y `compartido.dinero` 100 %. Total del proyecto: 97.7 % / 91.6 %. El `pom.xml` exige 80 %.

## Límites conocidos
- **El CPP y el costo unitario siguen en 4 decimales** (D-20). El total pagado de cada entrada ya es exacto, pero el CPP de 25 kg a $18.55/kg es 0.0186 (real 0.01855), y `valor` = cantidad × CPP da $465.00. Resolverlo del todo exige más decimales: es una decisión de diseño (D-80), no se cambió sola.
- Solo repositorios en memoria: nada se probó contra PostgreSQL.
- El test de A2 usa un tope de 500 ms; en una máquina muy rápida el código anterior podría haberlo cumplido por poco. En la corrida RED observada falló por tiempo agotado.
- Nadie volvió a revisar las correcciones del ciclo 2.
- No hay commits de checkpoint: la convención del repo es no commitear hasta que el usuario lo pida.
