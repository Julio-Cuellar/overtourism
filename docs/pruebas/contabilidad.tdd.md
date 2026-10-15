# Evidencia TDD — Módulo de Contabilidad (P1)

**Fuente:** `docs/diseno/contabilidad.md` v0.2 (reglas de la sección 6). Autorización: Bitácora, 2026-10-15.
**Rama:** `liz`. Un commit por regla.
**Cómo se corre:** `mvn -B -o test -Dtest=<Clase> -Dsurefire.failIfNoSpecifiedTests=false` desde `backend/`, con Java 21 y Maven 3.9.9 (`~/tools/env.sh`).
**Límite:** sin Docker en esta máquina. Las pruebas contra PostgreSQL se escriben pero no se ejecutan; se marcan como «no ejecutada».

## Recorridos de usuario
1. Como locatario, quiero que cada venta, cupón y corte de caja se registre solo como póliza cuadrada, para tener mi contabilidad sin capturarla.
2. Como locatario, quiero corregir un error con una reversa y cerrar el mes, para entregarle orden a mi contador.
3. Como plataforma, quiero que un evento repetido o fuera de orden no duplique ni pierda pólizas.

## Avance por regla

| Regla | Qué se garantiza | Prueba | RED (antes del código) | GREEN |
|---|---|---|---|---|
| A-01 | Póliza con menos de dos renglones o descuadrada no se arma; el total es la suma de cargos | `contabilidad/dominio/AsientoTest` (5) | No compila: faltan `Asiento`, `Renglon`, `CodigoError`, `ContabilidadException` | 5/5 pasan; `EstructuraModulosTest` 2/2 |
