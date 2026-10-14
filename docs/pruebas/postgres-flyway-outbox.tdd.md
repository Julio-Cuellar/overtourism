# Evidencia TDD: PostgreSQL en Docker, migraciones con Flyway y outbox

**Fecha:** 2026-10-14 · **Rama:** `julio` · **Autorización:** entrada «Autorización: PostgreSQL con outbox y migraciones con Flyway, sin RabbitMQ» de `docs/BITACORA.md`.
**Plan de origen:** no hubo archivo `*.plan.md`; las garantías salen de `docs/diseno/clientes.md` (secciones 5, 6, 7 y 11) y del diseño de Identidad de la Bitácora (entrada «Diseño del módulo de Identidad»).

## Historias de usuario

- Como equipo, quiero una base PostgreSQL propia en mi imagen de Docker, para desarrollar sin tocar la de otros proyectos.
- Como equipo, quiero que el esquema salga de migraciones de Flyway, para que cualquier compañero reconstruya la misma base.
- Como dueño de un negocio, quiero que mis clientes nunca se mezclen con los de otro, aunque la aplicación se equivoque (seguridad por fila).
- Como persona cuyos datos se guardan, quiero que mi consentimiento y la bitácora de quién vio mis datos no se puedan editar ni borrar.
- Como plataforma, quiero que un evento se guarde en la misma transacción que el cambio que lo causó (outbox), sin RabbitMQ.

## Qué se construyó

| Pieza | Archivo |
|---|---|
| Base en Docker (`postgres:17-alpine`, `127.0.0.1:5433`, volumen propio) | `backend/docker-compose.yml`, `backend/docker/postgres/01-roles.sh` |
| Contraseñas fuera del repositorio | `backend/.env` (ignorado por git), plantilla `backend/.env.example` |
| Esquemas, roles de grupo, `negocio_actual()` | `V001__plataforma_esquemas_y_roles.sql` |
| Outbox (`event_publication` de Spring Modulith 1.4.0) | `V002__plataforma_outbox.sql` |
| Identidad: cuenta, código de acceso, sesión | `V010__identidad_cuenta_codigo_sesion.sql` |
| Clientes: 9 tablas P1 con seguridad por fila | `V020__clientes_esquema.sql` |
| Finales de línea LF para scripts | `.gitattributes` |

Dependencias nuevas (autorizadas): `spring-boot-starter-jdbc`, `postgresql`, `flyway-core`, `flyway-database-postgresql`, `spring-modulith-starter-jdbc` y, para pruebas, `spring-boot-testcontainers`, `testcontainers:postgresql` y `junit-jupiter`. **Testcontainers va fijado en 1.21.4**: la 1.21.0 que trae Spring Boot 3.5.0 no habla con Docker 29 (error «Could not find a valid Docker environment»).

## Ciclo TDD

**RED real.** Las pruebas corrieron contra un PostgreSQL de verdad en Docker. Primera ronda (43 pruebas): fallaron por esquemas, tablas y roles inexistentes. Segunda ronda, después de la revisión de `ecc:database-reviewer`: se escribieron 76 pruebas nuevas o ajustadas antes de tocar las migraciones; 18 fallaron (`Tests run: 119, Failures: 18`) por la razón buscada: anonimización sin refuerzo, preparación usada con comando, aviso editable, historial de compras borrable, índices ausentes, tiempos máximos ausentes, `negocio_actual()` no paralela y V001 que omitía permisos sin avisar. Una de las 18 (la de HTC-22 generalizada a todos los esquemas) encontró `identidad.sesion`, que tiene `negocio_id` pero no es una tabla por negocio; ahí lo que cambió fue la prueba, con la excepción documentada, no la migración. Las demás pruebas nuevas (aislamiento por tabla, CHECK, llaves compuestas, fuga entre transacciones) pasaron desde el inicio porque protegen lo que ya funcionaba; para no darlas por buenas sin más se hizo una mutación (abajo).

**GREEN.** Se corrigieron las migraciones: `Tests run: 119, Failures: 0` en el paquete `persistencia`, y la suite completa `mvn -B verify`: **824 pruebas, 0 fallas, 0 errores, «All coverage checks have been met»** (antes de este trabajo, 705).

**Mutación.** Se cambió la política de las ocho tablas a `USING (true) WITH CHECK (true)`: `AislamientoPorFilaPostgresTest` falló en **23 de 38** pruebas. Se restauró. Es la única mutación hecha en esta ronda.

**Comprobación en la base real.** Se recreó el volumen (`docker compose down -v` y `up -d --wait`), se arrancó la aplicación con el perfil `desarrollo`: Flyway aplicó las 4 migraciones (versión 020), `POST /auth/codigo` respondió 202 y ninguna contraseña apareció en el registro. En la base: 8 tablas de Clientes con seguridad por fila activada y forzada; `negocio360` es superusuario; `negocio360_app` no es superusuario ni salta la seguridad por fila y tiene `statement_timeout=30s`, `lock_timeout=10s`, `idle_in_transaction_session_timeout=60s`; extensiones: `plpgsql` y `unaccent`.

## Garantías (qué prueba cada cosa)

Todas las pruebas están en `backend/src/test/java/mx/negocio360/persistencia/` y corren contra PostgreSQL 17 real con Testcontainers. Las de permisos y aislamiento usan el rol sin privilegios de la aplicación.

| # | Garantía | Prueba | Tipo |
|---|---|---|---|
| 1 | Flyway aplicó todas las migraciones sin fallar; hay un esquema por módulo (`plataforma`, `identidad`, `clientes`) y ninguno de más | `EsquemaPostgresTest` | integración |
| 2 | Existen las 13 tablas del diseño en el esquema de su módulo | `EsquemaPostgresTest.tablas_existen` (13) | integración |
| 3 | La aplicación usa un rol que no es superusuario ni salta la seguridad por fila; no puede crear ni borrar tablas ni leer el historial de Flyway | `EsquemaPostgresTest` | integración |
| 4 | SI-11: Identidad y Clientes no ven las tablas del otro (roles de grupo) | `EsquemaPostgresTest.roles_cadaModuloSoloVeSusTablas` | integración |
| 5 | El rol de la aplicación tiene tiempos máximos de consulta, bloqueo y transacción inactiva | `EsquemaPostgresTest.rolDeLaAplicacion_tieneTiemposMaximos` | integración |
| 6 | `negocio_actual()` es `PARALLEL SAFE` | `EsquemaPostgresTest.negocioActual_esParallelSafe` | integración |
| 7 | Existen los 10 índices para las consultas del diseño y las purgas; no hay índice de trigramas ni `pg_trgm` | `EsquemaPostgresTest.indices_*` | integración |
| 8 | El script de roles de Docker crea un rol sin privilegios y no tiene finales de línea CRLF | `EsquemaPostgresTest.scriptDeRoles_creaUnRolSinPrivilegios` | estática |
| 9 | SG-03: en cada una de las 8 tablas, cada negocio ve solo sus filas y sin negocio no ve ninguna | `AislamientoPorFilaPostgresTest.lectura_*` (8) | integración |
| 10 | Desde un negocio no se modifica ni se borra una fila de otro | `AislamientoPorFilaPostgresTest.escritura_noAfectaAlOtroNegocio` (8) | integración |
| 11 | Una fila no se puede mover a otro negocio con `UPDATE` (42501) | `AislamientoPorFilaPostgresTest.escritura_noSePuedeCambiarElNegocioDeUnaFila` (6) | integración |
| 12 | SC-04: el negocio de una transacción no se filtra a la siguiente de la misma conexión (el riesgo central de `set_config(..., true)`) | `AislamientoPorFilaPostgresTest.negocio_noSeFiltraEntreTransacciones` | integración |
| 13 | Un `app.negocio_id` que no es UUID hace fallar la consulta (22P02) y no devuelve filas | `AislamientoPorFilaPostgresTest.negocio_noUuid_fallaCerrado` | integración |
| 14 | La aplicación no puede vaciar (`TRUNCATE`) ninguna de las 9 tablas de Clientes | `AislamientoPorFilaPostgresTest.truncate_*` (9) | integración |
| 15 | P-03: la bitácora de accesos y el consentimiento solo admiten inserciones; ni el dueño de la base los edita, borra ni vacía | `AislamientoPorFilaPostgresTest`, `EsquemaClientesPostgresTest` | integración |
| 16 | Un registro de un negocio no se cuelga de un cliente de otro (4 tablas, más consentimiento y cliente→aviso) | `AislamientoPorFilaPostgresTest.llavesForaneas_*` (4), `EsquemaClientesPostgresTest` | integración |
| 17 | 17 reglas de dominio con `CHECK` rechazan el valor inválido (23514) | `ChecksClientesPostgresTest` (17) | integración |
| 18 | HTC-22: toda tabla con `negocio_id` en cualquier esquema propio tiene la seguridad por fila activada y forzada (excepción documentada: `identidad.sesion`) | `EsquemaClientesPostgresTest.aislamiento_todasLasTablasConNegocioLaTienenForzada` | integración |
| 19 | B-01: un cliente anonimizado no conserva apellidos, teléfono, correo, fecha de nacimiento ni notas | `EsquemaClientesPostgresTest.cliente_anonimizadoConDatosPersonales_seRechaza` | integración |
| 20 | B-02: la anonimización es irreversible, incluso para el dueño | `EsquemaClientesPostgresTest.cliente_anonimizado_noVuelveAActivo` | integración |
| 21 | I-03: el teléfono de un cliente activo es único por negocio y se libera al anonimizar | `EsquemaClientesPostgresTest.cliente_telefonoUnicoPorNegocioSoloEntreActivos` | integración |
| 22 | SC-10: una preparación usada no conserva el comando con datos personales | `EsquemaClientesPostgresTest.preparacion_usadaSinComando` | integración |
| 23 | L-03: el contenido de un aviso publicado no se edita; solo se cierra su vigencia | `EsquemaClientesPostgresTest.aviso_soloSePuedeCerrarLaVigencia` | integración |
| 24 | H-03: la aplicación solo borra marcas `ANULADO_PREVIO`; el historial de compras aplicadas no se borra | `EsquemaClientesPostgresTest.compraAplicada_soloSeBorranLasMarcasPrevias` | integración |
| 25 | Otras reglas ya cubiertas: teléfono E.164, nombre, origen y estado (I-01, I-02), búsqueda sin acentos, venta única por negocio (H-02), un aviso vigente por negocio | `EsquemaClientesPostgresTest` | integración |
| 26 | Identidad: restricciones de cuenta, código y sesión | `EsquemaIdentidadPostgresTest` (7) | integración |
| 27 | V001 se detiene con un mensaje claro si el rol de la aplicación no existe, y no deja nada a medias | `MigracionesPostgresTest.v001_sinElRolDeLaAplicacion_falla` | integración |
| 28 | El rol de la aplicación puede llamarse distinto: V001 usa el nombre configurado (`NEGOCIO360_APP_USER`) | `MigracionesPostgresTest.v001_conOtroNombreDeRol_concedeLosPermisosAEseRol` | integración |
| 29 | Outbox: el evento publicado en una transacción llega al listener y queda completado | `OutboxPostgresTest` | integración |
| 30 | Outbox: si la transacción se revierte, el evento no existe ni se entrega | `OutboxPostgresTest` | integración |
| 31 | Outbox: si el listener falla, el evento queda pendiente para reintentarse (entrega al menos una vez) | `OutboxPostgresTest` | integración |
| 32 | Outbox: el evento se guarda con el sobre común en `snake_case` | `OutboxPostgresTest` | integración |

## Revisión de `ecc:database-reviewer` (solo lectura)

Resultado: **0 críticos, 2 altos, 9 medios y 12 bajos**. Qué se hizo con cada uno:

| Hallazgo | Estado |
|---|---|
| M1 V001 omitía permisos si el rol no existía | **Corregido** (placeholder `app_user`, falla ruidosa, garantías 27 y 28) |
| M2 `preparacion.comando` era NOT NULL | **Corregido** (anulable, con `CHECK`: usada implica sin comando) |
| M3 aviso editable | **Corregido** (`UPDATE` solo sobre `vigente_hasta`) |
| M4 anonimización sin refuerzo | **Corregido** (`CHECK` y disparador) |
| M6 índices con `negocio_id` al frente | **Corregido** (8 índices nuevos o rehechos) |
| M7 índice GIN de trigramas inútil bajo seguridad por fila | **Comprobado y retirado**: en la base, `textlike`, `texticlike`, `textregexeq` y `similarity_op` tienen `proleakproof = f`. Se quitaron el índice y `pg_trgm` |
| M8 `.sh` con CRLF en Windows | **Corregido** (`.gitattributes`) |
| M9 huecos de pruebas | **Corregido** en su mayoría (garantías 9 a 18); ver «Huecos» |
| B1 healthcheck temprano | **Corregido** (`pg_isready -h 127.0.0.1`) |
| B2 y B12 contraseñas: `.env` y cambio con volumen existente | **Documentado** en `.env.example` |
| B3 rol sin tiempos máximos | **Corregido** (garantía 5) |
| B4 `negocio_actual()` sin `PARALLEL SAFE` | **Corregido** |
| B6 borrado amplio de `compra_aplicada` | **Corregido** (política restrictiva) |
| B9 índices de caducidad en Identidad | **Corregido** |
| **A1** las purgas no ven las filas bajo seguridad por fila | **Abierto: decisión del equipo** |
| **A2** la aplicación arranca con la contraseña del superusuario (Flyway corre dentro del proceso) | **Abierto: decisión del equipo** |
| M5 `event_publication` y `evento_procesado` visibles desde cualquier negocio | **Abierto** |
| B5 coherencia de estados (`compra_aplicada`, `solicitud_arco`, correo en minúsculas) | **Abierto**, menor |
| B7 `identidad.cuenta` no se puede anonimizar sin inventar un teléfono | **Abierto**: depende del diseño de Identidad |
| B8 el outbox no tiene ciclo de vida (las filas completadas se acumulan) | **Abierto** |
| B10 contraseña del rol en una variable de entorno del contenedor | **Aceptado en desarrollo** |
| B11 los disparadores de solo inserción impiden las retenciones del diseño | **Abierto**: decidir cómo se ejecuta la retención |

## Huecos conocidos (no verificado)

- **Ningún módulo usa todavía la base.** Los repositorios de Identidad y Clientes siguen en memoria. Lo que se probó es el esquema, los permisos y el outbox, no un repositorio JDBC. Lo que la aplicación hará (fijar `app.negocio_id` por transacción) solo se simuló en las pruebas con SQL directo.
- **Un solo pool y un solo rol de aplicación**, miembro de los tres roles de grupo. Los permisos ya están separados por rol de grupo, pero las conexiones no.
- **Las purgas** (preparación a las 24 horas, `ANULADO_PREVIO` a los 30 días, `evento_procesado` a los 90) **no existen y, con la seguridad por fila actual, un trabajo programado sin negocio no vería esas filas** (A1).
- El rendimiento no se midió: los índices son los que pide el diseño, sin `EXPLAIN` sobre volumen.
- No se probó contra la imagen real el script `01-roles.sh` dentro de la suite (la suite crea el rol desde Java); la garantía 8 solo vigila que el texto del script mantenga las banderas. Sí corrió de verdad al recrear el volumen en la base real.
- Inventario no tiene migraciones: no hay autorización escrita (HTC-19).
- Las migraciones se editaron en lugar de crear V003 porque aún no hay commits ni nadie las aplicó fuera de este equipo. **Después del primer commit, un cambio al esquema debe ir en una migración nueva** (Flyway rechaza una migración ya aplicada con otro contenido).

## Cobertura

`mvn -B verify`: 824 pruebas, 0 fallas, «All coverage checks have been met» (regla de JaCoCo del `pom.xml`).
