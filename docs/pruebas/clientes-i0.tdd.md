# Evidencia TDD: Clientes, etapa I0 (contratos y esquema GraphQL)

**Fecha:** 2026-10-13 · **Rama:** `julio` · **Autorización:** registrada en `docs/BITACORA.md` (2026-10-13).
**Plan de origen:** Mapa de Clientes v0.2, fase I0 («Contratos congelados con Ventas y esquema GraphQL»). No hay archivo `*.plan.md`: los recorridos salen del mapa.

## Recorridos de usuario que cubre esta etapa

1. Como locatario, quiero que las listas de clientes no muestren su teléfono, correo ni fecha de nacimiento completos, para proteger sus datos (P-05, SC-02).
2. Como equipo, quiero que ningún evento de Clientes lleve datos personales, para cumplir la LFPDPPP desde el principio (P-06, D-58).
3. Como equipo, quiero que Clientes lea `venta.registrada` de Ventas con un contrato tolerante y con el dinero como cadena decimal, para no depender de números de punto flotante (D-48, D-65, R5).
4. Como equipo, quiero que el esquema GraphQL no tenga `negocioId`, tenga una sola variante por comando y la superficie cerrada, para reducir los ataques propios de GraphQL (SG-02, SG-12, SG-17).
5. Como equipo, quiero que la estructura de módulos se verifique en cada corrida, para que Clientes solo dependa de `compartido` (R1 a R6).

## Ciclo RED → GREEN

| Paso | Comando | Resultado |
|---|---|---|
| Línea base | `mvn -B test` con solo el esqueleto | `BUILD SUCCESS`, «No tests to run» |
| **RED** (compilación) | `mvn -B test` con las 7 clases de prueba y sin código de producción | `BUILD FAILURE`. 16 errores de compilación, todos «package … does not exist» o «cannot find symbol» sobre `mx.negocio360.compartido.eventos`, `…compartido.graphql`, `…clientes.eventos` y `…clientes.contratos`. Ningún error de sintaxis ni de dependencias |
| **GREEN** (1.ª corrida) | `mvn -B test` con el código mínimo | 62 pruebas, 59 pasan, 3 errores en `listasPaginadas_primerosPorDefecto_esVeinte`: la prueba leía el valor por defecto como `Number` y graphql-java lo entrega como `IntValue`. Era un error de la prueba, no del código |
| **GREEN** (2.ª corrida) | `mvn -B test` tras corregir la lectura en la prueba | 62 de 62, `BUILD SUCCESS` |
| Cobertura | `mvn -B test` + JaCoCo | 81.0 % de líneas y 63.9 % de ramas: huecos en escalares de fecha y en el serializador de dinero |
| Pruebas añadidas | 9 pruebas para esos huecos | Una falló por comparar nodos de GraphQL con `equals` (no lo implementan); se corrigió la prueba |
| Fin de la primera pasada | `mvn -B test` | 71 de 71, `BUILD SUCCESS`. 97.2 % de líneas y 97.2 % de ramas |
| Revisión de código | agente `ecc:java-reviewer` (solo lectura) | Sin hallazgos CRÍTICOS, 1 ALTO, 5 MEDIOS y 8 BAJOS. Ver «Segundo ciclo» |
| **RED del segundo ciclo** | `mvn -B test` tras escribir las pruebas nuevas, con el código sin cambiar | 104 pruebas, **21 fallan** y todas corresponden a un hallazgo: 8 totales de venta inválidos que se aceptaban, 8 `datos` con flotantes o tipos no permitidos (más UUID de otra variante y listas mutables), 3 `primeros` que aceptaba `null`, 1 decimal demasiado largo y 1 literal con forma no plana |
| **GREEN final** | `mvn -B verify` | **143 de 143**, `BUILD SUCCESS` y «All coverage checks have been met» (mínimo 80 % de líneas). 96.1 % de líneas y 91.9 % de ramas |

### Segundo ciclo: hallazgos de la revisión y qué se hizo

| Hallazgo | Qué se hizo |
|---|---|
| A1 (ALTO) listas sin tope | `primeros` pasó a `Int! = 20`; nueva clase `LimitesDeConsulta` (50 por página, 5 sugerencias, 2 a 80 caracteres) con pruebas; una prueba de esquema exige que solo `consentimientosCliente` y `sugerirClientes` devuelvan listas planas |
| M1 decimales distintos y exponentes enormes | Nueva `DecimalPlano` (hasta 14 enteros y 8 decimales, sin exponente ni signo más ni espacios) usada por el escalar y por el lector JSON |
| M2 flotantes en `datos` | `SobreEvento` rechaza `Float`, `Double` y `BigDecimal`, también anidados, y copia listas y mapas de forma profunda e inmutable |
| M3 total negativo | `VentaRegistradaV1` rechaza totales negativos; el cero es válido (cupón del 100 %) |
| M4 pruebas de seguridad débiles | Se agregó la prueba de que `/graphiql` y `/graphql/schema` dan 404 y que el error de introspección lo dice; se cubre también `__type`; se quitó la prueba que solo leía su propia configuración |
| M5 falso verde en eventos | La prueba descubre los records del paquete por escaneo y exige que cada componente sea UUID o un enum del paquete (lista blanca por tipo) |
| B1, B2, B5, B7 | `valueToLiteral` con tipos reales; paridad de enums Java y GraphQL; el esquema se carga con el mismo patrón que Spring; variante del UUID y documentación del tipo `entidad.hecho` |
| B3 dependencias | Se quitaron `spring-graphql-test` y `spring-modulith-starter-test`, que no se usaban, y JaCoCo ahora exige el 80 % |
| B4 | La prueba de módulos usa `contains` y su descripción ya no promete R1 a R6 completos |
| B6, B8 | **No se cambió código.** B6 (`ComandoInput` definido en Clientes) queda registrado en la Bitácora para Inventario. B8 se documenta en el javadoc de `MapeadorEventos` |

**Limitación de este segundo ciclo, dicha con franqueza:** las pruebas de `DecimalPlano` y de `LimitesDeConsulta` se escribieron antes que su código, pero **no se ejecutaron en RED**: pasé directo a implementarlas. Las otras 21 sí se vieron fallar por el motivo correcto.

**Fuera de esta etapa, señalado por el revisor:** `/graphql` responde sin autenticación ni límites de profundidad, alias, costo, tamaño ni tasa. Hoy es inocuo porque no hay resolvedores, pero debe cerrarse antes del primero (SG-01 a SG-10). Los campos de texto del esquema (`notas`, `motivo`, `textoIntegral`) aún no tienen largo máximo.

**Verificación de que la prueba de seguridad no es vacía.** Con `-DargLine="-Dspring.graphql.schema.introspection.enabled=true"`, `ArranqueYSeguridadTest.introspeccion_consultaDelEsquema_seRechaza` falla (`BUILD FAILURE`). Con la configuración del proyecto, pasa.

## Especificación de pruebas

| N.º | Qué se garantiza | Prueba | Tipo | Resultado |
|---|---|---|---|---|
| 1 | La estructura de módulos no tiene violaciones y los módulos son exactamente `clientes` y `compartido` | `EstructuraModulosTest` (2) | estructura | PASA |
| 2 | Un sobre exige id UUIDv7, tipo `modulo.hecho`, versión ≥ 1, negocio, origen y correlación; sus datos son inmutables | `SobreEventoTest` | unitaria | PASA (15) |
| 3 | El sobre sale en JSON con los 9 nombres del contrato en snake_case, `ocurrido_en` en ISO UTC y `causa_id` nulo explícito | `SobreEventoTest` | contrato | PASA |
| 4 | El dinero se escribe como cadena decimal plana, nunca como número | `SobreEventoTest.dinero_serializar_…` | contrato | PASA |
| 5 | Decimal rechaza números y texto no decimal; Date y DateTime solo aceptan ISO-8601 y normalizan a UTC | `EscalaresGraphQlTest` | unitaria | PASA (18) |
| 6 | Los 3 eventos de Clientes son records sin componentes personales y sus claves de datos están en una lista permitida | `EventosClientesContratoTest` | contrato | PASA (11) |
| 7 | Clientes lee `venta.registrada` con y sin `cliente_id`, ignora campos nuevos, exige `total` como cadena y rechaza número, texto inválido o ausencia | `ContratoVentasConsumidoTest` | contrato | PASA (7) |
| 8 | El esquema se construye; las consultas son exactamente las 8 del inventario; las mutaciones son solo el par de la plataforma | `EsquemaGraphqlClientesTest` | contrato | PASA (14) |
| 9 | Ningún tipo, campo o argumento menciona el negocio; `ComandoInput` es `@oneOf` con 8 variantes; las listas piden 20 por defecto | `EsquemaGraphqlClientesTest` | contrato | PASA |
| 10 | Listas y sugerencias no exponen teléfono, correo, nacimiento, notas ni apellidos; no hay domicilio ni RFC; el dinero es Decimal; no hay Float | `EsquemaGraphqlClientesTest` | contrato | PASA |
| 11 | La aplicación arranca con los dos esquemas, usa el escalar Decimal propio, rechaza `__schema` y `__type` con error de introspección, y `/graphiql` y `/graphql/schema` dan 404 | `ArranqueYSeguridadTest` | integración | PASA (5) |
| 12 | Los decimales tienen una sola gramática: plana, sin exponente, signo más ni espacios, de largo acotado | `DecimalPlanoTest` (25) y `EscalaresGraphQlTest` | unitaria | PASA |
| 13 | Los datos de un evento no admiten flotantes ni `BigDecimal` (tampoco anidados) y son inmutables a fondo | `SobreEventoTest` | unitaria | PASA |
| 14 | Las consultas tienen topes (50 por página, 5 sugerencias, 2 a 80 caracteres) y `primeros` no acepta null | `LimitesDeConsultaTest` (14), `EsquemaGraphqlClientesTest` | contrato | PASA |
| 15 | Un evento nuevo de Clientes con un campo que no sea UUID ni enum del paquete hace fallar la suite | `EventosClientesContratoTest` | contrato | PASA (12) |

## Cobertura y huecos conocidos

- 96.1 % de líneas y 91.9 % de ramas (JaCoCo 0.8.12, 143 pruebas). El `pom.xml` exige 80 % de líneas en `mvn verify`.
- Sin cubrir: `Negocio360Application.main`, dos líneas posteriores a una excepción en `LectorDecimalEstricto`, y en `SobreEvento` los tipos enteros menos usados (`Short`, `Byte`, `BigInteger`) y la clave de mapa que no es cadena.
- **Sin probar todavía** (corresponde a etapas siguientes): base de datos, seguridad por fila, resolvedores, listeners de Ventas, autenticación. Esta etapa solo define contratos; ningún comportamiento de negocio está implementado.
- **Sin probar**: que `venta.registrada` de Ventas real traiga `cliente_id` y un `total` neto de cupón. Ventas no existe todavía; el contrato es el que Clientes espera y hay que congelarlo con su dueño (D-48, D-65).
- Las pruebas de esquema usan `RuntimeWiring.MOCKED_WIRING`, así que comprueban la forma del esquema y no la ejecución de las consultas.

## Evidencia de integración

No se crearon commits de punto de control: el repositorio no tiene commits de esta etapa porque el usuario no los ha pedido. Si luego se quiere un historial RED/GREEN, este reporte es el resumen. Cuando se haga el commit o el pull request, copiar aquí la tabla de «Ciclo RED → GREEN».
