# Diseño del módulo de Rutas (v2)

Estado: borrador v0.1, 2026-10-08. Fase de diseño: no hay código. Todo lo marcado PROPUESTA espera la confirmación del equipo.

Mockup: `docs/mockups/rutas.html` (9 pantallas, 45 acciones). Reemplaza la pantalla S-09 y las acciones A-39 a A-41 de `docs/mockups/app-turista.html`.

## 1. Qué cambia respecto a la v1

La v1 de la app tenía un botón «Generar mi ruta», preferencias fijas, una sola propuesta y «cambiar la última parada». La v2 convierte la ruta en un recorrido que se planea en pocos toques, se compara, se edita, se sigue en la calle y se corrige cuando algo falla.

Problemas que ataca (lista del mockup, pestaña «Problemas y soluciones»): preferencias fijas, una sola propuesta, edición pobre, desconfianza en la IA, plan que se rompe, ruta que se guarda y se olvida, grupos y necesidades, itinerarios apretados, costos poco claros, solo medio día, todos al mismo lugar, IA que decide el orden, cifras sin fuente, sin señal y sin forma de compartir. La lista sale de lo observado en la v1 y de problemas conocidos del rubro; no hubo investigación de mercado ni pruebas con usuarios.

## 2. Reglas de arquitectura que aplican

| Regla | Cómo se cumple |
|---|---|
| R1 | Rutas es dueño de sus tablas (esquema `rutas`). Guarda `negocio_id` sin llave foránea hacia otros esquemas. Nadie más lee sus datos |
| R2 y R6 | Rutas llama de forma síncrona a la API pública de Visitantes, Directorio y Transporte. Las novedades de mesas, afluencia y sellos las recibe por eventos |
| R3 | Todo evento lleva UUIDv7. Los consumidores de Rutas (E-05, E-12, E-13) son idempotentes |
| R4 | Rutas es horizontal y no depende de ningún vertical. El canal compone la ficha y puede llamar a Museos |
| R5 | E-11 recibe campos nuevos opcionales (`personas`, `ritmo`, `variante`): solo cambios aditivos |
| Principio rector | El modelo interpreta y redacta; el código decide (sección 6) |

## 3. Alcance (PROPUESTA)

| Nivel | Qué entra |
|---|---|
| P1, demo | Brief editable; 2 variantes (A y B); editor (ajustar tiempo, cambiar, quitar, fijar, agregar con aviso de «cabe»); «por qué» con verificador; guardar; modo viaje básico (siguiente parada, cómo llegar, sellar, voy tarde, saltar); aviso de lugar lleno por E-12 |
| P2 | Variante C (poco visitados); aviso por afluencia alta (E-13); plan B de lluvia; compartir enlace; descarga sin conexión; calificar; «Mis rutas» completo; rutas de varios días |
| P3 | Aprendizaje con duraciones reales (E-17); calendario; avisos fuera de la app; afinado de la demanda planeada |

## 4. Reglas de dominio y pruebas de aceptación

| ID | Regla | Prueba de aceptación |
|---|---|---|
| RT-01 | Una parada entra solo si está abierta desde la llegada hasta la salida, cumple las necesidades marcadas (dieta, movilidad) y cabe el grupo | Un lugar que cierra a las 17:00 con llegada 17:10 no entra; el editor avisa «cierra a las 17:00 y saldrías a las 17:xx» |
| RT-02 | El costo por persona suma los costos de las paradas y no pasa del tope. Si pasa, el solver quita paradas del final y lo dice | Con tope $150.00, la variante A trae 1 parada (Museo Talavera Viva, $60.00) y avisa qué quitó |
| RT-03 | El total va de la hora de inicio a la salida de la última parada, con traslados incluidos, y no pasa de la duración elegida | Con 2 h, la variante A trae 1 parada (90 min) |
| RT-04 | Los traslados vienen de Transporte, siempre «estimados». Sin dato se usa un valor por defecto, también rotulado | El mockup usa 15 min entre museo y fonda y 9 min entre fonda y tienda; el total de A es 90 + 15 + 75 + 9 + 30 = 219 min |
| RT-05 | El solver es determinista: mismas entradas y misma versión de datos dan la misma ruta | Repetir 100 veces la misma propuesta da el mismo resultado |
| RT-06 | Se devuelven hasta 3 variantes con pesos distintos; ninguna repite exactamente el conjunto de paradas de otra | Con los datos del ejemplo salen A, B y C con paradas distintas |
| RT-07 | Toda edición recalcula horas, caminata, costo y avisos y responde `{cabe, total_min, costo_por_persona, avisos}`. No llama al modelo | Subir 30 min la visita al museo hace que A (219 + 30 = 249 min) deje de caber en 4 h y lo avise |
| RT-08 | Una parada fija no se cambia por reemplazo automático ni al reordenar | Con la parada 1 fija, «reoptimizar» deja el museo en primer lugar |
| RT-09 | El texto del «por qué» solo usa hechos del solver. Un verificador comprueba que toda cifra del texto exista en los datos; si no, se usa la plantilla | Un texto del modelo que dice «$99.00» cuando el costo es $60.00 se descarta y sale la plantilla |
| RT-10 | Si el modelo no responde a tiempo o falla, la ruta se entrega con textos estándar | Con el modelo apagado, `proponerRutas` devuelve las mismas paradas y los textos de la plantilla |
| RT-11 | El puntaje penaliza llegar a una franja con afluencia ALTO o con demanda planeada alta (umbral por definir) y premia lugares poco visitados. Máximo un lugar poco visitado por ruta A o B, con su razón visible | Con Cocina Santa Clara en afluencia ALTO a las 12:40, la variante B la incluye con aviso de espera y la variante A no la elige |
| RT-12 | Las variantes y las ediciones son borradores y no publican. E-11 sale al guardar o empezar la primera vez (idempotente por `ruta_id`); E-15 al empezar; E-16 en cada reajuste; E-17 al terminar | Proponer y editar no genera eventos; guardar y luego empezar genera un solo E-11 |
| RT-13 | Para una ruta EN_CURSO, si E-12 trae 0 mesas libres de una parada pendiente, o E-13 sube a ALTO, Rutas empuja un aviso con opciones que caben. No cambia nada sin confirmación | Con la fonda en 0 mesas, el aviso ofrece Fonda La Esquina y Cocina Santa Clara y la opción de esperar 20 min (239 de 240 min) |
| RT-14 | Un sello (E-05) marca como hecha la parada de ese negocio. Sin sello se marca a mano. Una parada hecha no vuelve a pendiente | Tras E-05 de la fonda, la parada 2 queda HECHA y la siguiente se vuelve la actual |
| RT-15 | La ubicación no se guarda. El enlace compartido es de solo lectura, caduca a los 7 días y no incluye perfil ni ubicación. Al borrar al visitante (E-03) se borran sus rutas | Un enlace vencido responde `ENLACE_VENCIDO`; tras E-03 no queda ninguna fila del visitante |
| RT-16 | Toda duración, costo y traslado se muestra como «estimado» | Ninguna pantalla muestra un costo o una hora sin la marca |
| RT-17 | Cada visitante tiene a lo sumo una ruta EN_CURSO | `iniciarRuta` con otra en curso responde `YA_HAY_UNA_RUTA_EN_CURSO` |

## 5. Datos (esquema `rutas`)

Las rutas son del visitante, no de un negocio: la seguridad por fila usa `visitante_id` (de la sesión), no `negocio_id`.

| Tabla | Columnas | Restricciones |
|---|---|---|
| `ruta` | `id` (UUIDv7), `visitante_id`, `estado` (BORRADOR, GUARDADA, EN_CURSO, TERMINADA), `variante`, `brief` (jsonb), `personas`, `limite_min`, `presupuesto_pp` (numeric 12,2), `inicio_en`, `favorita`, `calificacion` (1 a 5, nulo), `version`, `creado_en`, `actualizado_en` | Único parcial: una EN_CURSO por `visitante_id` (RT-17). Bloqueo optimista con `version` |
| `ruta_parada` | `ruta_id`, `orden`, `negocio_id` (sin FK), `duracion_min`, `fija`, `estado` (PENDIENTE, HECHA, OMITIDA), `llegada_estimada`, `hecha_en`, `con_sello` | PK `(ruta_id, orden)`. `duracion_min` ≥ 15 |
| `ruta_reajuste` | `id`, `ruta_id`, `motivo` (RETRASO, OMITIDA, LUGAR_LLENO, ESPERA), `parada`, `reemplazo_negocio_id`, `minutos_cambio`, `causa_evento_id`, `creado_en` | Solo inserción |
| `ruta_enlace` | `id`, `ruta_id`, `token_hash`, `expira_en`, `revocado_en` | Solo se guarda el hash |
| `demanda_planeada` | `zona`, `franja`, `conteo` | Se recalcula desde `ruta` y `ruta_parada`. Mínimo de personas por celda (por definir) |

Dinero como `numeric(12,2)` y cadenas decimales en la API. Fechas ISO-8601 en UTC.

## 6. Cómo decide (canal de IA)

1. **Brief** (código): chips o texto libre. «Sorpréndeme» usa el perfil.
2. **Interpretar** (IA): solo con texto libre; salida con esquema fijo. Si falla, se muestra el formulario.
3. **Restricciones duras** (código): RT-01 a RT-03.
4. **Candidatos** (código): Directorio, Catálogo y Museos, ya filtrados.
5. **Puntaje** (código): intereses, afluencia esperada, equidad (RT-11), sello y cupón.
6. **Solver** (código): mejor recorrido bajo tiempo, presupuesto y caminata; hasta 3 variantes.
7. **Explicar** (IA): «por qué» y resumen, solo con hechos del paso 6.
8. **Verificador** (código): RT-09.
9. **App**: muestra variantes; toda edición vuelve al paso 6.

Sin IA se usan los pasos 1, 3, 4, 5 y 6 y una plantilla en lugar del 7 (RT-10). El modelo nunca elige ni ordena lugares, no calcula cifras, no toca la base de datos y no decide el `negocio_id`. Solo ve los lugares que devuelve la API. Los pesos de las variantes son cualitativos y están por calibrar.

Ficha de componente de IA, evaluación con casos fijos y plan sin IA: pendientes (skill `integrar-ia`).

## 7. Operaciones (PROPUESTA, GraphQL salvo lo indicado)

| Operación | Entrada | Salida | Errores |
|---|---|---|---|
| `proponerRutas(brief)` | `cuando`, `duracion_min`, `personas`, `con`, `presupuesto_por_persona`, `ritmo`, `necesidades[]`, `salida`; o texto libre | Hasta 3 variantes con paradas, `total_min`, `costo_por_persona`, `caminata_min`, `quitadas_para_que_quepan[]` | `RUTA_SIN_RESULTADOS`, `BRIEF_INVALIDO` |
| `ruta(rutaId)` | id | Ruta con paradas y `{cabe, avisos}` | `RUTA_NO_ENCONTRADA` |
| `editarRuta(rutaId, cambio)` | `AJUSTAR_DURACION`, `QUITAR`, `FIJAR`, `REEMPLAZAR`, `AGREGAR` | `{cabe, total_min, costo_por_persona, caminata_min, avisos[]}` | `MINIMO_UNA_PARADA`, `NEGOCIO_NO_DISPONIBLE` |
| `alternativasParada(rutaId, orden)` | id, orden | Hasta 3 alternativas con `delta_min`, `delta_costo` y `cabe` | `SIN_ALTERNATIVAS` |
| `candidatosParaAgregar(rutaId)` | id | Candidatos con `encaja`, `delta_min` y `delta_costo` | |
| `guardarRuta(rutaId, favorita?)` | id | `{estado}`; publica E-11 la primera vez | `RUTA_NO_CABE` |
| `compartirRuta(rutaId)` | id, `caduca_dias` | `{url, expira_en, solo_lectura}` | `RUTA_NO_GUARDADA` |
| `paqueteRuta(rutaId)` | id | Datos para uso sin conexión | |
| `planB(rutaId)` | id, motivo | Variante techada con traslado en autobús (estimado) | |
| `iniciarRuta(rutaId)` | id | `{estado, siguiente}`; publica E-15 | `YA_HAY_UNA_RUTA_EN_CURSO`, `RUTA_NO_CABE` |
| `marcarLlegada(rutaId, orden)` | id, orden | `{estado_parada}` | |
| `reajustarRuta(rutaId, motivo, …)` | `RETRASO`, `OMITIDA`, `LUGAR_LLENO` con reemplazo, `ESPERA` | Igual que `editarRuta`; publica E-16 | `NO_CABE` |
| `finalizarRuta(rutaId)` | id | `{estado}`; publica E-17 | |
| `calificarRuta(rutaId, calificacion)` | id, 1 a 5 | `{ok}` | |
| `misRutas(estado?)` | filtro | Lista | |
| WebSocket `rutaActiva(rutaId)` | id | Avisos `LUGAR_LLENO` y `AFLUENCIA_ALTA` con opciones | |

Las escrituras de la persona usan preparar → confirmar solo donde ya lo exige el diseño de la app (sello). Las ediciones de borrador y los reajustes no llevan token (D-35, excepción propuesta).

## 8. Eventos

Sobre común de `inventario.md` §7.1. Rutas publica eventos de plataforma sin `negocio_id` (D-37).

| Evento | Cuándo | Datos | Consumidores |
|---|---|---|---|
| `ruta.generada` v1 (E-11) | Al guardar o empezar la primera vez | `ruta_id`, `visitante_id` (seudónimo), `variante`, `personas`, `ritmo`, `paradas[{orden, negocio_id, llegada_estimada, duracion_min}]`, `estimada` | Gobierno |
| `ruta.iniciada` v1 (E-15) | Al empezar | `ruta_id`, `visitante_id`, `iniciada_en` | Gobierno |
| `ruta.reajustada` v1 (E-16) | En cada reajuste | `ruta_id`, `motivo`, `parada`, `reemplazo_negocio_id`, `minutos_cambio`; `causa_id` = evento que lo provocó | Gobierno |
| `ruta.finalizada` v1 (E-17) | Al terminar | `ruta_id`, `completadas`, `omitidas`, `duracion_real_min` | Gobierno, Rutas |

Rutas consume `directorio.disponibilidad_actualizada` (E-12), `afluencia.nivel_actualizado` (E-13) y `sello.otorgado` (E-05). Afluencia no consume eventos de Rutas: la demanda planeada la calcula Rutas con sus tablas, lo que evita un ciclo entre ambos módulos (D-41). Gobierno solo recibe agregados.

## 9. Seguridad y privacidad

- La ubicación viaja en la consulta y no se guarda ni se registra en la auditoría ni en los registros del servidor.
- Enlace compartido: solo lectura, con caducidad; se guarda el hash del token.
- `visitante_id` sale de la sesión. El modelo nunca recibe el identificador de la persona.
- La demanda planeada y todo lo que ve Gobierno son agregados con mínimo por celda.
- El texto libre del usuario es dato, nunca instrucción; el modelo solo devuelve el esquema fijo.

## 10. Decisiones y hallazgos

| ID | Decisión | Estado |
|---|---|---|
| D-39 | Rutas arma el recorrido con un solver determinista; el modelo solo interpreta el texto libre y redacta el «por qué», con verificador de cifras y plantilla si falla | PROPUESTA |
| D-40 | Ruta viva: modo viaje con reajustes, eventos E-15, E-16 y E-17, suscripción `rutaActiva`, y Rutas consume E-12, E-13 y E-05 | PROPUESTA |
| D-41 | Equidad con la demanda planeada que Rutas ya tiene y la afluencia actual. E-11 se publica al guardar o empezar. Afluencia no consume eventos de Rutas | PROPUESTA |

Hallazgos: (HR-1) el ciclo Rutas y Afluencia se evita con D-41; (HR-2) la app publicaba E-11 al proponer, hay que actualizar S-09 y A-39; (HR-3) no todos los lugares tienen sello, por eso existe `marcarLlegada`; (HR-4) «sin gluten» y «caminar poco» necesitan etiquetas en Directorio; (HR-5) los avisos necesitan datos frescos y deben mostrar su antigüedad; (HR-6) el solver necesita casos de prueba con resultado esperado antes de programar.

## 11. Datos que el solver necesita

| Dato | Dueño | De dónde sale | Estado |
|---|---|---|---|
| Duración de la visita | Directorio | La declara el negocio; respaldo por categoría; siempre «estimado» | Por diseñar |
| Costo medio por persona | Directorio o Catálogo | Del menú o catálogo publicado; respaldo por categoría | Por diseñar |
| Horario por día | Directorio | Ya está en la ficha | Existe |
| Aforo o total de mesas | Verticales | Campo `total` de C-01 | Existe |
| Dieta y accesibilidad | Directorio | Las declara el negocio | Por definir |
| Traslados | Transporte | OpenStreetMap más captura propia; sin GTFS público | Por confirmar |
| Afluencia | Afluencia | Conteos de ventas por franja, con mínimo | Existe (E-13) |

## 12. Preguntas abiertas

1. ¿Se acepta el corte P1 y los tres D-39 a D-41?
2. ¿Quién conoce los datos reales de duración de visita y aforo de los negocios del piloto?
3. ¿Qué tolerancia de espera usa el modelo para dar por fallido (RT-10)?
4. ¿Cuál es el umbral de demanda planeada para penalizar un lugar (RT-11) y el mínimo por celda?
5. ¿Los avisos fuera de la app quedan fuera del MVP, como se propone?
