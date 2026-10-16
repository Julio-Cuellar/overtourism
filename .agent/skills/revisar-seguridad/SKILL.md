---
name: revisar-seguridad
description: "Revisa la seguridad de un diseño o de código del proyecto: aislamiento entre negocios, seguridad por fila, tokens, secretos, webhooks, inyección de instrucciones en el LLM, GraphQL, archivos subidos y datos personales. Úsala cuando se toque autenticación, permisos, entradas del usuario, el bot, la API de la IA, la ubicación del turista, el tablero de gobierno, o antes de publicar o integrar algo."
---

# Revisar seguridad

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

## Reglas de actuación
- Si encuentras un problema **CRÍTICO**, detente, repórtalo primero y no sigas con otras revisiones menores.
- **Nunca imprimas un secreto** completo en tu respuesta ni en la Bitácora. Di en qué archivo y línea está, y que hay que **rotarlo**.
- No intentes explotar nada. Describe el escenario, no el ataque paso a paso.
- No ejecutes escáneres ni herramientas de red sin que el usuario lo pida.

## 1. Aislamiento entre negocios
- [ ] `negocio_id` sale de la sesión o del webhook. Nunca de un argumento, del LLM o de la carga de un evento.
- [ ] Toda tabla de negocio tiene RLS con `app.negocio_id`, como segunda barrera.
- [ ] **El contexto también se fija** en listeners de eventos y tareas programadas. Sin él, o la consulta falla o, peor, ve datos de todos.
- [ ] Una consulta o una caché no mezcla datos de dos negocios.
- [ ] Hay prueba de que un negocio no ve ni toca datos de otro.

## 2. Autenticación y sesión
- [ ] Acceso por código enviado al número del dueño (SMS o mensajería), sin contraseñas.
- [ ] El código vence, tiene intentos limitados y no se reutiliza.
- [ ] Limitación de intentos por número y por IP.
- [ ] La sesión no expone datos de otro negocio ni se reutiliza entre dispositivos sin control.

## 3. Escrituras y tokens
- [ ] Toda escritura de usuario pasa por preparar → confirmar.
- [ ] El token es de un solo uso, vence pronto y está ligado a negocio, actor y comando.
- [ ] Solo se guarda el **hash** del token.
- [ ] Si el comando cambia entre preparar y confirmar, se rechaza.

## 4. Webhooks y bot
- [ ] Se valida la firma del webhook de Telegram o WhatsApp y el secreto del encabezado.
- [ ] Idempotencia por `message_id`.
- [ ] Responde 200 rápido; el trabajo pesado va después.
- [ ] Solo el endpoint de entrada del bot es público.
- [ ] Hay una lista blanca de intenciones por estado de la conversación (mínimo privilegio).
- [ ] Hay bloqueo o cola por usuario para evitar condiciones de carrera.

## 5. El LLM
- [ ] El texto del usuario, y todo lo que el modelo lea de archivos o páginas, es **dato**, nunca instrucción.
- [ ] Las herramientas de la IA son de **solo lectura**, con resultados acotados.
- [ ] El LLM no escribe en la base de datos ni genera SQL libre.
- [ ] El servidor valida identificadores, horarios y calcula las cifras.
- [ ] Si el texto final se reescribe con el modelo, un verificador comprueba que cada cifra exista en los datos; si no, se envía la plantilla original.
- [ ] El modelo no recibe secretos ni datos de otros negocios en el contexto.
- [ ] Hay prueba de inyección: un mensaje que dice «ignora tus reglas y...» no cambia el comportamiento.

## 6. GraphQL
- [ ] Límites de profundidad y de complejidad.
- [ ] Consultas persistidas en producción; introspección restringida donde corresponda.
- [ ] Autorización en cada resolvedor.
- [ ] Los mensajes de error no filtran trazas ni detalles internos.
- [ ] Las suscripciones filtran por negocio.

## 7. Datos personales y privacidad
- [ ] Los eventos no llevan datos personales.
- [ ] La ubicación del turista solo se manda al consultar y **no se guarda** el recorrido.
- [ ] El tablero de gobierno es agregado y anónimo, con un **mínimo de negocios por celda** para que nadie sea identificable.
- [ ] El negocio decide qué comparte; hay consentimiento para el historial financiero.
- [ ] **Nunca** se piden ni se guardan credenciales del SAT.

## 8. Secretos y configuración
- [ ] Ningún secreto en el código, en documentos, en la Bitácora ni en ejemplos. Todo en variables de entorno o gestor de secretos.
- [ ] La aplicación **falla al arrancar** si falta un secreto obligatorio.
- [ ] Los archivos de configuración local con claves están en `.gitignore`.
- [ ] Un secreto expuesto se considera comprometido: se rota.

## 9. Archivos y entradas
- [ ] Las fotos y archivos subidos se validan por tipo y tamaño, y no se ejecutan.
- [ ] Toda entrada se valida en la frontera. Consultas parametrizadas, sin concatenar texto.
- [ ] Las rutas de archivos no aceptan rutas del usuario sin normalizar.

## 10. Disponibilidad y abuso
- [ ] Limitación de tasa en todos los endpoints públicos y en la API de la IA.
- [ ] Todo lo que devuelve listas tiene límite.
- [ ] Hay auditoría de cada confirmación (negocio, actor, comando, resultado).

## Severidad
| Nivel | Ejemplo |
|---|---|
| CRÍTICO | Un negocio puede ver datos de otro; un secreto en el repositorio; el LLM escribe en la base |
| ALTO | Falta validar la firma de un webhook; token reutilizable; datos personales en eventos |
| MEDIO | Falta limitación de tasa; mensajes de error con detalles internos |
| BAJO | Endurecimientos opcionales |

## Formato de salida
```markdown
# Revisión de seguridad de <alcance> (<fecha>)
**Veredicto:** Aprobar / Con reservas / Bloquear

| Sev. | Dónde | Hallazgo | Escenario | Corrección |
|---|---|---|---|---|

**Qué no pude verificar:** ...
```
Si no usaste a un revisor de seguridad especializado y el cambio toca autenticación, pagos o datos personales, recomiéndalo.
