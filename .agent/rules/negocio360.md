---
trigger: always_on
---

# Reglas permanentes del proyecto Negocio 360

Estas reglas se aplican en cada turno. El detalle completo está en la skill `negocio360-reglas` (`.agent/skills/negocio360-reglas/SKILL.md`).

## Antes de actuar
1. Lee `docs/BITACORA.md`: secciones 1 a 6 y las dos últimas entradas del registro. Es el estado real del proyecto.
2. Lee la skill `negocio360-reglas` y la que corresponda a la tarea. El índice está en `.agent/skills/README.md`.
3. Si no puedes leer esos archivos, dilo. No sigas como si los hubieras leído.

## No negociables
- **Fase de diseño: no se programa.** No crees proyecto Maven o Gradle, carpetas `src`, clases, migraciones, pruebas ni dependencias hasta que el usuario lo autorice de forma explícita y quede anotado en la Bitácora. Pedir un plan, un diseño, un diagrama, un documento o un mockup no es autorización de código.
- **El modelo interpreta, el código decide.** El LLM nunca toca la base de datos, no genera SQL, no inventa cifras y no decide el `negocio_id`.
- **Arquitectura R1 a R6.** Cada módulo es dueño de sus datos y otro módulo los obtiene por su API pública o por el bus. Síncrono = llamar a la API; asíncrono = evento. Los eventos llevan UUIDv7, son versionados y los consumidores son idempotentes. Las dependencias van solo de vertical a horizontal.
- **Seguridad.** Nunca pidas ni guardes credenciales del SAT. Sin secretos en código, documentos ni mensajes; usa variables de entorno.
- **Decisiones.** No presentes como aceptada una decisión que el usuario no confirmó. Sin respuesta es `PROPUESTA` o `ABIERTA`.
- **Datos.** No inventes datos reales (cifras, transporte, requisitos fiscales, negocios). Todo dato simulado va rotulado como simulado.
- **Acciones hacia afuera.** No hagas `git push`, no publiques ni subas contenido, y no ejecutes comandos destructivos, sin confirmar antes con el usuario.
- **Dinero y cantidades** con decimales exactos. Nunca `float` ni `double`.

## Al terminar
- Registra la sesión en `docs/BITACORA.md`: una entrada nueva **al final**, con decisiones (`D-xx`), archivos y lo que no verificaste. No reescribas entradas pasadas.
- Reporta con honestidad qué hiciste, qué verificaste y qué no pudiste verificar.

## Estilo
- Español de México.
- Usa los nombres del diseño tal cual, sin traducirlos ni inventar sinónimos.
- Cuando una instrucción del usuario choque con estas reglas, señálalo y pregunta; no obedezcas ni ignores en silencio.
