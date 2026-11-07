# Pruebas de la rama `claude/integracion-todo` (vaiinilla-pruebas, tanda 1)

Referencia: `pruebas-telefonos/auditoria-estatica-182.md` (auditó `7dcfa6d6`, cabeza de #94) y el APK `vaiinilla-dev-integracion-2eaa1f18-vps2.apk`.

## Alcance real

No se corrió la skill completa (7 vectores sobre 182 recorridos). Esta tanda hizo tres cosas:

1. Revisó en el código integrado los hallazgos que la auditoría previa marcó «fallará» y los arreglos que marcó «listo», para ver si los merges los rompieron.
2. Usó la suite de pruebas JVM (1982 pruebas) como cobertura por rol.
3. Comprobó el guardia de cambio de tienda (ver el último apartado).

Nada se probó en teléfono. Ningún recorrido de este documento pasa a «arreglado» por esto.

## Por rol

| Rol | Revisado en el código integrado | Pruebas JVM (archivos de prueba) |
|---|---|---|
| Caja | G07: `Ya no se cobra` sigue en `OrderJourney.kt:508`. G09: «Ver cuentas por cobrar» (`CashCloseSheet.kt:264`) navega a `CASHIER_UNPAID`. | 12 de Caja y 12 de Cartera; pasan, salvo las intermitentes |
| Cocina | Cola de «Marcar lista» ahora en `KitchenViewModel` (#119) con `KitchenReadyDelayTest` (B04) en verde | 8 |
| Mesero | I07: «Parte de …» sigue en `WaiterSpaceComponents.kt:1629`. I09 (app): la ruta `onOpenUnpaidAccounts` ya existía en `main`. | 13 |
| Cliente | C05: `tableSummaryLine` ya distingue mesa sin pedidos de «Tú no has pedido». W04: `ReceivedNoticeLog` junta copias del mismo evento (`sameEvent`, `dedupe`); sin verificar con push real. D08: sin cambios respecto a `main` (`ReservationsScreen.kt:156` sigue pidiendo el nombre sin cuenta); #96 dice haberlo arreglado y no lo re-simulé. | 8 de alumno, 11 de carrito, 5 de mesa compartida, 6 de reservas |

## Fallas nuevas contra existentes

- **Nuevas por la integración:** ninguna detectada en esta tanda. Los fallos de la suite son intermitentes (ver `integracion-todo.md`).
- **Ya existían en `main`:** `PendingRefundsViewModelTest`, las 14 pruebas de pantalla (las arregla #116), T07 y J07 (backend: `invitaciones.repository.js`, política `usuarios_select`), L05, P08, F17 y F18 (decisión o plan). No se volvieron a probar contra VPS2.
- **Dependen del backend de VPS2:** #104 (migración 203 ausente) y todo el modo Hotel.

## NO EJECUTADOS (no cuentan como pasados)

- **Stripe:** Q01 (tarjeta), J10, F03, S01, S07.
- **Correo:** J07 (falta `ACCESS_EMAIL_FROM` en VPS2).
- **2FA/TOTP:** M01 a M05 y la última parte de Y03.
- **Cámara, QR y NFC físicos:** G04 (paso 3), G05, I03, B04, B05, E01, A05, Z01, C08, W02, F09.
- **Push:** K01 a K03, F08, F20, la mitad push de W04.
- **Modo avión y datos móviles:** O01, O02.
- **Todo lo demás de los 182 recorridos:** no se re-simuló paso a paso contra este APK ni en teléfono.

## Las 5 capturas rotas (no se tocaron)

| Captura | Cambio | Causa |
|---|---|---|
| `doop_hotel_s04_checkout` | 12.7 %: desaparece «Room service (2) $784.50» y el total baja de $6,134.50 a $5,350.00 | `checkoutBreakdown` (`HotelReceptionViewModel.kt:301`) ahora solo cuenta pedidos con `pendingPayment`; los del fixture no lo tienen. Es un cambio de comportamiento de HOTEL-A2 con la referencia vieja (`5a03d7d8`), no de la integración. Falta decidir si se regraba o se ajusta el fixture. |
| `doop_hotel_cargo_manual` | 0.37 % | No la abrí. Es del mismo grupo de pantallas; sin causa confirmada. |
| `wallet/e02_cartera_oscura` | 0.18 % | Solo cambian las horas de los movimientos («Hoy 9:06 am» contra «Hoy 12:39 am»): el fixture usa la hora real. Prueba no determinista. |
| `wallet/e14_alumno_oscura` | 0.18 % | Igual que la anterior (no abierta). |
| `video/mesero-mesa4` | sin referencia | `VideoScreenshotTest` también falla en `main`. |

## Los 11 campos «POR REVISAR» del cambio de tienda

Ninguno faltaba. `StaffOrdersViewModel.start()` (Caja, Cocina y Mesero) arranca de `OperationalUiState.freshForNewRole`, que construye un estado nuevo y solo conserva `heartbeatOnline` (y `cashChangeNotice` con el mismo negocio); `release()` también deja `OperationalUiState()`. El guardia seguía leyendo `OperationalViewModel.setRole`, que ya no es el camino del personal. Se cambió para que lea `freshForNewRole`, se vació el baseline (quedan 2 entradas con motivo) y una prueba de mutación confirma que falla si se agrega un campo al constructor. `FreshForNewRoleTest` pasa.

Riesgo menor sin cubrir: `KitchenViewModel.onBeforeRelease` manda las marcas «lista» pendientes al cambiar de negocio, y lo hace ya con el token nuevo; esas marcas podrían rechazarse. No se probó.
