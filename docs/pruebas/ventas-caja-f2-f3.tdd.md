# Evidencia TDD: Ventas y Caja en memoria (fases F2 y F3)

**Fecha:** 2026-10-14 y 2026-10-15 · **Rama:** `uriel` (local, sin push ni commits) · **Autorización:** «Autorizo programar Ventas y Caja en la rama uriel», registrada en `docs/BITACORA.md` (2026-10-14).
**Plan:** `docs/planes/ventas-caja-v1.md`, fases F2 (Ventas en memoria) y F3 (Caja en memoria). **Diseño:** `docs/diseno/ventas.md` y `docs/diseno/caja.md`.
**Alcance:** sin base de datos, como Clientes I2: dominio, comandos con preparar → confirmar, eventos y listeners sobre puertos con dobles en memoria. Sin migraciones, sin JDBC y sin GraphQL (F4 y F5 no están autorizadas).

## Recorridos de usuario

1. Como cajero, quiero cobrar una venta con precios de Catálogo, ver total y cambio antes de confirmar y que nada se guarde hasta que confirme (VT-01 a VT-06).
2. Como cajero, quiero vender a granel (0.5 kg) pero no medio boleto (VT-03).
3. Como turista, quiero que mi cupón del Pasaporte se canjee al cobrar y que no se use dos veces (VT-07, VT-08, D-34, D-137).
4. Como encargado, quiero anular una venta con motivo y que Caja, Inventario y Clientes se enteren (VT-09, VT-11, D-141).
5. Como cajero, quiero abrir turno con fondo, meter y sacar efectivo y ver cuánto debería haber por método (CJ-01, CJ-02, CJ-05, CJ-12).
6. Como cajero, quiero hacer el corte y que no cierre si faltan ventas por llegar o si la diferencia es grande y nadie la autoriza (CJ-06 a CJ-09, D-140).
7. Como equipo, quiero que Caja reciba cada venta una sola vez aunque el evento llegue repetido o la anulación llegue primero (CJ-03, CJ-04, R3, D-19).

## Ciclo RED → GREEN

| Paso | Qué se hizo | Resultado |
|---|---|---|
| Línea base | `mvn test -Dtest=EstructuraModulosTest,PreparacionTest` sin tocar nada | En verde |
| **RED 1** (compartido y Ventas) | Pruebas de `ServicioPreparacion` genérico, `Redondeo` y Ventas, con sus dobles, sin código de producción | `mvn test-compile` no compila: 112 «cannot find symbol» y los paquetes `ventas.dominio`, `ventas.eventos` y `ventas.aplicacion.puertos` no existen. Ningún otro tipo de error |
| GREEN 1 | `compartido.comandos`, `compartido.dinero.Redondeo` y el módulo `ventas` | 522 de 523, contando Clientes y `compartido`. Falló `confirmar_precioCambio_comandoCambio`: daba `PAGOS_NO_CUADRAN` |
| Corrección | Al confirmar, el comando es el mismo que pasó al preparar, así que un fallo de pagos solo puede venir de un total que cambió: ahora responde `COMANDO_CAMBIO` (VT-05) | Ventas **54 de 54** |
| **RED 2** (Caja) | Pruebas de Caja, el camino dorado Ventas + Caja y `EstructuraModulosTest` con `ventas` y `caja` | `mvn test-compile` no compila: 94 «cannot find symbol» y los paquetes `caja.dominio`, `caja.eventos` y `caja.aplicacion.puertos` no existen |
| GREEN 2 | Módulo `caja` | 251 de 252. Falló `ventaEntrePasos_comandoCambio`: daba `CORTE_REQUIERE_AUTORIZACION` |
| Corrección | Al confirmar el corte se compara primero el resumen y después el umbral: si llegó una venta entre los dos pasos, la persona contó contra otro esperado y la respuesta es `COMANDO_CAMBIO` | Caja **41 de 41** |
| Verificación completa | `mvn -B verify` | 951 pruebas, **0 fallas y 146 errores**. Los 146 errores son de 10 clases que necesitan Docker (8 de `persistencia` más `ArranqueYSeguridadTest`, `AuthControllerTest` y `RegistrosSinSecretosTest`). Docker Desktop está apagado: `docker info` no encuentra el motor. Ningún error es de Ventas, Caja ni `compartido` |
| Verificación sin Docker | `mvn -B verify` excluyendo esas 10 clases | **805 de 805**, `BUILD SUCCESS`, «All coverage checks have been met» |
| JavaDoc (D-142) | `mvn javadoc:javadoc -Ddoclint=all` sobre `ventas`, `caja`, `compartido.comandos` y `compartido.dinero` | Primera corrida: 83 advertencias (79 del código nuevo). Tras completar el JavaDoc: **0 en el código nuevo**; quedan 4 en `DecimalPlano.java`, que ya existía y no se tocó |

**Cobertura de líneas (JaCoCo, corrida sin Docker):** Ventas 87.8 %, Caja 92.8 %, `compartido.comandos` 94.0 %, `compartido.dinero` 100 %.

**Limitaciones, dichas con franqueza:**
- No hay commits de checkpoint: la máquina no tiene identidad de Git y no la configuré sin preguntar. Esta tabla es la evidencia de RED y GREEN.
- Las pruebas de persistencia no se ejecutaron en esta sesión (Docker apagado). Ventas y Caja no tienen persistencia todavía, pero esas pruebas protegen el resto del proyecto y deben volver a correrse con Docker encendido.
- No hubo ronda de mutaciones ni revisión de agentes (`ecc:java-reviewer`, `ecc:security-reviewer`).
- `VentasYCajaTest` usa un bus en memoria síncrono; no prueba la entrega real del outbox de Spring Modulith.

## Qué garantizan las pruebas

| # | Garantía | Prueba | Regla |
|---|---|---|---|
| 1 | Preparar no escribe; un token sirve una vez, solo para su negocio y actor, y vence a los 5 minutos | `ServicioPreparacionTest` (21) | D-23, D-55 |
| 2 | Redondeo único HALF_UP: dinero a 2, cantidad y precio a 4 | `RedondeoTest` (11) | D-20 |
| 3 | Precio, descripción y unidad se copian de Catálogo; importe y subtotal los calcula el código | `CalculadoraVentaTest` (19) | VT-02, VT-08 |
| 4 | Medio boleto se rechaza; medio kilo se acepta | `CalculadoraVentaTest` | VT-03 |
| 5 | Los pagos cuadran con el total; el efectivo da cambio; se permite pago mixto | `CalculadoraVentaTest` | VT-04 |
| 6 | Si el precio cambia entre preparar y confirmar → `COMANDO_CAMBIO`, sin venta ni evento | `RegistrarVentaTest` (15) | VT-05 |
| 7 | Folios consecutivos por negocio; una preparación vencida o un canje fallido no gastan folio | `RegistrarVentaTest` | VT-06 |
| 8 | Cupón: total neto 135.00, canjeado con la venta; no se usa dos veces; si Pasaporte falla no hay venta | `RegistrarVentaTest` | VT-07, VT-08, D-65 |
| 9 | Anular con permiso y motivo, una sola vez y con bloqueo optimista | `AnularVentaTest` (12) | VT-09, VT-11 |
| 10 | Forma exacta de `venta.registrada` y `venta.anulada`, sin datos personales, legible por el lector de Clientes ya programado | `EventosVentasContratoTest` (4) | D-136, D-48, R5 |
| 11 | Resumen del turno: registradas, anuladas, totales por método y preparaciones vigentes | `ResumenTurnoTest` (4) | VF-03, CJ-07 |
| 12 | Una caja, un turno abierto; fondo válido; entradas y retiros; un retiro no deja el efectivo bajo cero | `TurnoYMovimientosTest` (14) | CJ-01, CJ-02, CJ-05, CJ-10 |
| 13 | Cada pago de una venta es un movimiento y la anulación lo compensa; un evento repetido, una venta repetida o una anulación que llega antes no suman | `ConsumidorVentasTest` (8) | CJ-03, CJ-04, R3, D-19 |
| 14 | Corte: EN_CORTE al preparar; no cierra con ventas pendientes ni eventos faltantes; umbral con autorización de encargado; inmutable, descartable y con vencimiento | `CorteTest` (14) | CJ-06 a CJ-10, CJ-12, D-140 |
| 15 | Consultas aisladas por negocio | `ConsultasCajaTest` (3) | R1 |
| 16 | Camino dorado real (turno, venta con cupón, anulación, corte sin diferencia) y un corte que no cierra si se perdió un evento | `VentasYCajaTest` (2) | D-135, D-136, CJ-07 |
| 17 | Ventas solo depende de `compartido`; Caja, de `compartido` y `ventas` | `EstructuraModulosTest` (2) | R1, R4, D-135 |

## Decisiones tomadas al programar (a confirmar por el equipo)

- **D-143** (propuesta): el patrón preparar → confirmar vive en `compartido.comandos` y lo usan Ventas y Caja; Clientes conserva el suyo hasta que alguien lo migre.
- **D-144** (propuesta): al confirmar, si el cálculo deja de cuadrar porque el mundo cambió (precio, cupón, ventas del turno), la respuesta es `COMANDO_CAMBIO` y no el error de validación.
- Códigos nuevos de Caja: `CAJA_DUPLICADA`, `NOMBRE_CAJA_INVALIDO` y `TURNO_NO_ENCONTRADO` (ya están en `caja.md`).
- Un fondo de 0.00 no crea movimiento (precisado en CJ-02).
- Una venta para un turno ya cerrado se reporta como `SIN_TURNO_ABIERTO` y no se aplica: CJ-11 (ajuste en el siguiente turno) sigue pendiente, P2.

## Pendiente

- Correr `mvn -B verify` completo con Docker encendido.
- Revisión de agentes de Java y seguridad sobre `ventas`, `caja` y `compartido.comandos`.
- JavaDoc de `DecimalPlano` (4 advertencias, código anterior a esta etapa).
- Adaptadores reales: Catálogo, Pasaporte, permisos de Identidad, reglas versionadas y repositorios JDBC (F5, sin autorización).
- GraphQL del panel (F4, sin autorización) y el canal que llama a `turnoActivo` antes de cada venta (VT-01).
- VT-10 (revertir el canje del cupón al anular) y CJ-11 (venta tardía como ajuste), ambas P2.

## Ronda 2: correcciones de la revisión (2026-10-15)

| Paso | Qué se hizo | Resultado |
|---|---|---|
| RED (compilación) | `RevisionVentasTest`, `RevisionCajaTest`, casos nuevos en `RedondeoTest`, `ConsumidorVentasTest` y `VentasYCajaTest` | `mvn test-compile` falla solo por `VerificadorDeTurno` y `VerificadorDeTurnoDesdeCaja` |
| RED (ejecución) | Firmas nuevas sin comportamiento | 37 fallas y errores por la razón esperada; uno era de armado de la prueba (actor sin permiso de anular) y se corrigió la prueba |
| GREEN | Implementación de los seis puntos (ver Bitácora) | 151/151 en las clases afectadas; `ResumenTurnoTest` necesitó abrir el turno extra en el doble |
| Verificación sin Docker | `mvn -B verify` sin las 10 clases de Docker | **848/848**, BUILD SUCCESS, cobertura: Ventas 96.6 %, Caja 97.6 % |

| # | Garantía nueva | Prueba |
|---|---|---|
| 18 | Ventas no prepara ni confirma en un turno que Caja no da por ABIERTO | `RevisionVentasTest`, `VentasYCajaTest.turnoEnCorte_ventasNoVende` |
| 19 | Límites de líneas, pagos, cupón, textos y montos con código propio, sin consultar vecinos | `RevisionVentasTest`, `RedondeoTest` |
| 20 | El cupón no se gasta si el descuento cambió al canjear | `RevisionVentasTest` |
| 21 | Dos precios que se compensan dan `COMANDO_CAMBIO` | `RevisionVentasTest` |
| 22 | Un evento no aplicado no se marca; datos inválidos u origen ajeno no tienen efecto; la anulación revierte lo registrado | `RevisionCajaTest`, `ConsumidorVentasTest` |
| 23 | `turnoActivo` y el verificador usan el estado efectivo | `RevisionCajaTest` |
| 24 | Copias defensivas, `toString` sin datos personales, textos que no invitan a reusar el token | `RevisionVentasTest`, `RevisionCajaTest` |
