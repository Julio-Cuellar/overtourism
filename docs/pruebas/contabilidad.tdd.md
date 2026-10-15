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
| A-02 | Renglón con cargo y abono, sin ninguno, negativo o sin cuenta falla con MOVIMIENTO_INVALIDO; más de 2 decimales o fuera de numeric(14,2) con MONTO_INVALIDO. El CHECK en la base queda para la prueba con Postgres | `dominio/RenglonTest` (12) | 11/12 fallan (sin validación; montoMaximo pasaba) | 12/12 y AsientoTest 5/5 |
| A-07 | Desglose de los ejemplos del diseño (100.00 → 86.21 + 13.79; 232.00 → 200.00 + 32.00); ventas + IVA = total para los 30,000 montos de 0.01 a 300.00; exento y tasa cero; TASA_INVALIDA fuera de rango | `dominio/DesgloseIvaTest` (10) | No compila: falta DesgloseIva | 10/10 |
| C-01 | Tres negocios (restaurante, museo, miscelánea) quedan con los mismos códigos y reglas; 11 cuentas afectables bajo su padre; reglas de venta con la tasa y la fuente capturadas (no de memoria); copia por negocio; TASA_INVALIDA con tasa fuera de rango o sin fuente; activar dos veces no duplica. Nota: ActivarContabilidad agrega el campo fuenteTasa, que el diseño exige en regla_contable.fuente | `aplicacion/ActivarContabilidadTest` (7) | No compila: faltan ContabilidadApi, ServicioPreparacion, ActivarContabilidadComando, Cuenta, ReglaContable, repositorios | 7/7; acumulado de Contabilidad 34/34 y EstructuraModulosTest 2/2 |
| C-02 | Repetir 101.01 falla con CODIGO_CUENTA_DUPLICADO (también si otro lo crea entre preparar y confirmar); el mismo código en otro negocio se acepta; hija con naturaleza o tipo distinto al padre falla con NATURALEZA_INVALIDA; raíz exige tipo y naturaleza; padre ajeno o inexistente con CUENTA_NO_ENCONTRADA; sin activar con CONTABILIDAD_NO_ACTIVA | `aplicacion/CrearCuentaTest` (9) | No compila: falta CrearCuentaComando | 9/9; acumulado 43/43 |
| A-04 | Póliza manual entre cuentas válidas se registra (origen MANUAL, creadoPor = actor); cargar a 101 (padre) falla con CUENTA_NO_AFECTABLE; cuenta inactiva con CUENTA_INACTIVA (también si se desactiva entre preparar y confirmar); cuenta ajena o inexistente con CUENTA_NO_ENCONTRADA; una póliza ajena no se ve | `aplicacion/CuentasAfectablesTest` (6) | No compila: faltan RegistrarPolizaManualComando, OrigenPoliza, LibroDiario, Poliza | 6/6; acumulado 49/49. Folio, periodo y saldos quedan en su mínimo a propósito para A-05, T-01 y A-08 |
| A-05 | Dos pólizas de ingreso reciben 1 y 2; series independientes por tipo, periodo y negocio; el folio se asigna al confirmar (el segundo token confirmado primero recibe el 1); 50 registros concurrentes reciben 1 a 50 sin repetir (en memoria; en Postgres depende del contador bloqueado, pendiente de prueba con Docker) | `aplicacion/FoliosTest` (4) | 4/4 fallan: el folio era 0 | 4/4; acumulado 53/53 |
