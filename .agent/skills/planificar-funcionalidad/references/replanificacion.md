# Replanificar y recortar

Úsalo cuando un plan ya existe y algo cambió. **Nunca sobrescribas el plan anterior en silencio:** quien lo aprobó tiene que ver qué cambió.

## Cuándo se replanifica
- El usuario amplía o cambia el alcance.
- Una decisión abierta se resolvió y cambia el camino.
- Un contrato congelado cambió.
- Una fase tardó mucho más (o menos) de lo estimado.
- Faltan días y ya no cabe.
- Apareció un riesgo que se volvió realidad.

## Procedimiento

1. **Congela la versión anterior.** Llámala «v1» (luego «v2»...). No la edites.
2. **Escribe el cambio en una frase.** «El usuario pidió sumar X», «la fase 2 tardó 2 días y no 1».
3. **Calcula el delta** contra la versión anterior (plantilla abajo).
4. **Revisa qué se afecta:**
   - Decisiones: ¿alguna aceptada se contradice? Márcala `REEMPLAZADA por D-xx` con motivo.
   - Dependencias y contratos congelados.
   - Reparto: ¿alguien queda sin trabajo o sobrecargado?
   - Riesgos nuevos y los que dejan de aplicar.
5. **Reestima** solo lo que cambió y recalcula el total, el colchón y la ruta crítica.
6. **Vuelve a comparar demanda contra capacidad** con la fecha de hoy, no con la de la primera versión.
7. **Presenta el plan v2** con el delta primero. Pide confirmación otra vez.
8. **Registra** en la Bitácora qué cambió y por qué.

## Plantilla de delta

```markdown
## Cambios de v1 a v2
**Motivo:** <una frase>

| Elemento | v1 | v2 | Efecto en estimado |
|---|---|---|---|
| Alcance | | | |
| Fase F3 | 1 día | 2 días | +1 |
| Nueva fase F7 | — | 1.5 días | +1.5 |

**Total:** v1 <n> → v2 <n> (diferencia <±n>, <±%>).
**Ruta crítica:** v1 <n> días → v2 <n> días.
**Calendario:** v1 «cabe» → v2 «<cabe / cabe justo / no cabe>».
**Decisiones afectadas:** <ID y qué pasa>
**Riesgos nuevos:** <lista>
**Opciones si no cabe:** A <...> · B <...> · C <...>
```

## Recorte de emergencia (falta tiempo)

1. **Mide dónde estás.** Qué fases están hechas de verdad (con su criterio de salida cumplido), cuáles a medias. «Casi listo» cuenta como pendiente.
2. **Calcula lo que falta** con el factor real (real ÷ estimado) de lo ya hecho, no con la estimación original.
3. **Compara con los días que quedan.**
4. **Recorta según el orden de recorte** del plan, **de abajo hacia arriba y nunca a la mitad de algo**. Lo que se recorta pasa a hoja de ruta y así se presenta; no se hace pasar por terminado.
5. **Protege siempre:** el camino dorado, los datos, la documentación del modelo de IA y el ensayo de la demo.
6. **Congela funciones.** Después de cierta fecha solo se corrigen errores.
7. **Comunica** al equipo qué se recorta, quién absorbe qué y qué se dice en el pitch sobre lo recortado.
8. **Registra** el recorte en la Bitácora como decisión.

## Señales de que hay que replanificar ya
- Una tarea lleva el doble de lo estimado y sigue sin terminar.
- Dos personas dependen del mismo contrato y aún no está congelado.
- Se está agregando algo «pequeño» por tercera vez.
- La integración se pospuso otra vez.
- Docker, la base de datos o las claves de IA siguen sin funcionar a pocos días del evento.

## Errores comunes
- Reescribir el plan sin mostrar el delta.
- Replanificar con la capacidad del primer día y no la de hoy.
- Recortar lo que ya está a medias en vez de lo que no se empezó.
- Recortar las pruebas o la documentación «para ganar tiempo»: solo cambia dónde se paga el costo.
- Olvidar avisar al equipo y que cada quien siga el plan viejo.
