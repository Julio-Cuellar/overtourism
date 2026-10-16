---
name: contratos-api
description: "Diseña o revisa la API del proyecto: qué va por GraphQL, REST o WebSocket, operaciones, entradas, errores, paginación y seguridad, incluidas las herramientas que usa la IA, los webhooks del bot y la interfaz Java pública entre módulos. Úsala al definir consultas, mutaciones, suscripciones, endpoints REST, contratos JSON, códigos de error o al decidir qué protocolo usar."
---

# Contratos de API

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

## Qué protocolo para qué

| Para... | Protocolo | Ruta o forma |
|---|---|---|
| App del turista, panel del locatario, tablero de gobierno | **GraphQL** sobre HTTPS | `POST /graphql` |
| Datos que cambian y la pantalla debe reflejar | **Suscripción GraphQL** por WebSocket | mismo `/graphql`, protocolo `graphql-transport-ws` |
| Herramientas que usa el LLM | **REST**, solo lectura | `/ia/v1/...` |
| Acceso con código, subida de fotos, administración, salud | **REST** | `/auth`, `/archivos`, `/admin`, `/salud` |
| Mensajes entrantes de Telegram o WhatsApp | **REST** (webhook) | `/webhooks/...` y `/bot/...` |
| Un módulo usando a otro | **Interfaz Java pública** del módulo, en el mismo proceso | `InventarioApi`, `CatalogoApi`... |

Las tres capas (GraphQL, REST, interfaz Java) llaman a los mismos servicios del módulo. Ninguna toca repositorios.

## Reglas para todas las operaciones
- **Respuesta:** `{ ok, codigo, datos, mensaje_plantilla }`. Cada `codigo` de error tiene un texto fijo en español de México.
- **`negocio_id`** nunca es parámetro. Sale del contexto (sesión o webhook).
- **Nombres** en español, como el diseño (`registrarConsumo`, no `recordConsumption`).
- **Escrituras de usuario:** preparar → confirmar, con token de un solo uso ligado a negocio, actor y comando, y vencimiento corto.
- **Escrituras de sistema** (consecuencia de algo ya confirmado): idempotentes por una llave de causa y consumidor.
- **Paginación** por cursor, máximo 100 por página. Nada devuelve listas sin límite.
- **Validación** de toda entrada en la frontera. Falla rápido con un código claro.
- **Errores:** distingue reintentables (`CONCURRENCIA`, `ERROR_TEMPORAL`) de permanentes. No filtres detalles internos.

## GraphQL
- DataLoader para evitar consultas repetidas (N+1) en listas.
- Límites de profundidad y de complejidad. Consultas persistidas en producción.
- Autorización en cada resolvedor, no solo en la puerta de entrada.
- El resolvedor llama a la API del módulo. Para armar una ficha con datos de varios módulos, llama a la API de cada uno; no hay joins entre esquemas.
- Las suscripciones llevan un evento del bus a la pantalla. Al reconectar, el cliente **vuelve a consultar** para no depender de eventos perdidos.
- La ubicación del turista no viaja por WebSocket: se manda en la consulta de inicio cuando se mueve más de 150 m o cada minuto, sin guardar el recorrido.

## REST para la IA (`/ia/v1`)
- **Solo lectura.** Ninguna herramienta escribe.
- Resultados cortos (máximo 25) y con enumerados, no texto libre.
- El servidor valida cada identificador de negocio, los horarios de apertura y calcula duraciones y costos. El LLM no los inventa ni los suma.
- Una respuesta vacía dice que no hay resultados, nunca rellena.
- Las herramientas existentes: buscar negocios, eventos cercanos, tramos de transporte y validar una ruta.

## Webhooks y bot
- Verifica la firma del webhook y el secreto en el encabezado.
- Responde 200 de inmediato y procesa después.
- Idempotencia por `message_id` (la plataforma reintenta).
- Solo el endpoint de entrada del bot es público.
- El LLM solo extrae a un esquema JSON fijo; el código valida y llama al caso de uso.

## Plantilla para documentar una operación

```markdown
| Operación | Entrada | Salida | Escribe | Idempotencia | Errores |
|---|---|---|---|---|---|
```

Y para cada comando de escritura: campos, reglas que aplica (`U-01`...), y qué eventos publica.

## Lista de verificación
- [ ] ¿Es el protocolo correcto según la tabla?
- [ ] ¿El `negocio_id` viene del contexto?
- [ ] ¿Tiene límite de tamaño y de página?
- [ ] ¿Sus códigos de error están en el catálogo con texto?
- [ ] ¿Es idempotente si se reintenta?
- [ ] ¿Se documentó en **todos** los lugares donde aparece (diseño, diagramas, mockup si lo hay)?
