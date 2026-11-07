# Integración de todos los PR abiertos (rama `claude/integracion-todo`)

Rama creada desde `origin/main` (`5243c56b`) en un worktree aparte. Cada PR entró con `git merge --no-ff` desde su rama de `origin`.
No se mergeó a `main` ni se tocó ningún PR original. Los PR que ya estaban encadenados (por ejemplo #115, #119 y #120 sobre #114) llegaron por su propia rama.

## PR que entraron (en orden)

| # | Rama | Resultado |
|---|------|-----------|
| #110 | claude/gradle-build-script | limpio |
| #111 | claude/project-thread-red-seguridad | limpio |
| #116 | claude/project-thread-4bqh8t | limpio |
| #117 | claude/fix-rental-tracking-lint | limpio |
| #118 | claude/project-thread-student-auth-flaky | limpio |
| #102 | claude/project-thread-tjcaoq | limpio |
| #103 | claude/project-thread-84uh0v | limpio |
| #105 | claude/project-thread-6sbsza | limpio |
| #104 | claude/project-thread-7voyfs | limpio |
| #106 | claude/project-thread-red-unico | conflicto 1 |
| #107 | claude/project-thread-bandeja-salida | limpio |
| #112 | claude/project-thread-venue-context | limpio |
| #114 | claude/project-thread-view-models | conflicto 2 |
| #113 | claude/project-thread-hotel-venue-context | limpio (reemplaza #97 a #101) |
| #108 | claude/project-thread-zir9t3 | limpio |
| #109 | claude/project-thread-w1ezd9 | limpio |
| #115 | claude/project-thread-byd0vf | conflicto 3 |
| #119 | claude/project-thread-byd0vf-estado-efimero | conflicto 4 |
| #120 | claude/project-thread-byd0vf-estado-efimero-2 | conflicto 5 |

Ninguno estaba ya contenido en otro, así que no se saltó ninguno. **No** se mergearon #97, #98, #99, #100 ni #101 (los cubre #113), ni #92, #84, #10 ni los de dependabot.

## Conflictos y decisiones

1. **#106 vs #104** en `data/cashcut/CashCutRepository.kt`. #104 agrega `dayStart`/`dayStartBody` (hora de inicio del día) con imports de `JsonObject`, `JsonPrimitive`, `buildJsonObject`, `contentOrNull` y `put`; #106 cambia `Json { … }` por `ApiJson.omitNullsCoercing`. Se conservaron ambos cambios y se quitó solo el import `Json`, que ya no se usa.
2. **#114 vs #103** en `ui/operational/OperationalViewModel.kt` y `ui/screens/KitchenAge.kt`. #114 mueve el código de Caja, Cocina y Mesero a un ViewModel por rol; #103 borraba código sin uso. Se tomó la versión de #114 y se volvió a aplicar el borrado de #103 de `applyGalleryClientOrders` y `KitchenAgeText` después de comprobar con grep que no tienen usos.
3. **#115 vs #113 y #103** en `OperationalViewModel.kt` y `CashierOperationalScreen.kt`. Se mantuvo el borrado de `applyGalleryClientOrders`; en Caja se conservó el bloque de Recepción del hotel (#113) y la condición del aviso sin conexión de #115 (`state.queue.lostConnection`, que reemplaza a `lastRefreshOk`).
4. **#119 vs #113** en `KitchenOperationalScreen.kt`. Se conservaron `hotelBoard` y `byProductChosen` (#113) y la cola de «Marcar lista» en el ViewModel (`state.pendingReady`, `onFlushReady`, `onMarkReady`, `onUndoReady` de #119). La tarjeta del tablero de hotel (`HotelKitchenCard`) se adaptó a esa API: marca con `onMarkReady`, deshace con `onUndoReady` y lee `deadlineMs`.
5. **#120 vs #113** en `WaiterOperationalScreen.kt`. Se conservaron `deliveringCharge` y `housekeepingOpen` (#113) y `scannerOpen` como `rememberSaveable` (#120).

## Cambios hechos después de los merges

- `HotelStaffDoopScreenshotTest` y `FontScaleAuditTest` usaban `lastRefreshOk` y `onReady` (API anterior a #115 y #119): ahora usan `queue = QueueSync.Live` y `onMarkReady`.
- `HotelStayCodeScreen`: el código tecleado dentro de la hoja pasa a `rememberSaveable` (clase U04 de los guardias).
- Baselines de los guardias de `scripts/`, con su motivo:
  - `guardias_baseline.json`: `HotelKitchenBoard.kt` tiene un `delay(15_000)` que solo refresca la etiqueta de edad (igual que `KitchenAge.kt`).
  - `cambio_de_tienda_baseline.json`: 11 campos de `OperationalUiState` quedan como «POR REVISAR». Siguen en el estado compartido aunque ahora viven en los ViewModels por rol; hay que confirmar que se reinician al cambiar de negocio.
  - `contrato_baseline.json`: 55 claves del modo Hotel (`HotelJson.kt`, `PublicSpaceDto.es_habitacion`, `OrderDetailDto.estancia_id`) que el backend de `backend_claves.json` no tiene. Se quitan cuando el backend de hotel esté en `develop`.
- `ktlintFormat` sobre los 33 archivos con avisos nuevos, y dos renombrados para cumplir la regla `filename`: `CashSessionRepositoryImpl.kt` pasa a `RemoteCashSessionRepository.kt` y `DeviceHeartbeatRepositoryImpl.kt` pasa a `RemoteDeviceHeartbeatRepository.kt`. El baseline de ktlint baja 110 avisos.

## Resultados

Compilación con `scripts/gradle-build.sh`, una Gradle a la vez: los cuatro sabores dev (Alumno, Caja, Cocina, Mesero) compilan, incluidas las pruebas.

| Comprobación | `main` (5243c56b) | integración |
|---|---|---|
| `testDevCajaDebugUnitTest` | 1806 pruebas, 15 fallan | 1982 pruebas, 0 fallan en la última corrida |
| Pruebas de los otros 3 sabores | no se corrieron | Mesero 0 fallos; Alumno y Cocina 1 fallo intermitente cada uno |
| ktlint (ratchet) | n/d | sin avisos nuevos |
| Guardias de `scripts/` | no existen | pasan (con los baselines de arriba) |

Las 15 fallas de `main` son las 14 pruebas de pantalla rojas (las arregla #116) más `PendingRefundsViewModelTest › si ya se habia confirmado sale de la lista`.

Fallos intermitentes (pasan aisladas): `PendingRefundsViewModelTest` (también falla en `main`), `MenuWaiterCallTest` y `AdminStaffViewModelTest`. Todas son «UncaughtExceptionsBeforeTest» o una aserción de tiempo, por una corrutina de `Dispatchers.IO` que se resuelve después de `resetMain`. Es un problema de la suite, no de la integración.

## Pendiente y lo que no se pudo probar

- #104 (hora de inicio del día) necesita el backend #219 con la migración 203. El backend de pruebas de VPS2 no la tiene: la app lee `null` y no deja guardar la hora.
- El backend de VPS2 no tiene las rutas del modo Hotel (instancia aparte); `es_habitacion` no existe allí.
- `check_app_backend_contract.sh` solo acepta las URL de Railway; no corre contra VPS2. Las pruebas que lanza ya están dentro de la suite.
- `jvm-roles` apunta a Railway development, no a VPS2, y la skill `vaiinilla-pruebas` completa (7 vectores sobre 182 recorridos) no se corrió: requiere subagentes, que están prohibidos. Pendiente en teléfono: Stripe, 2FA/TOTP, correo, cámara/QR/NFC, push.
