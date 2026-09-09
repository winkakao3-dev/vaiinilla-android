# Blindaje contra errores — Android, 2026-09-09

## Estado y alcance

Revisión de "a prueba de errores" del Android antes de uso público amplio. Cubre el runtime completo: reporte de crashes, capa de red, invariantes de negocio de Cocina/Caja, checkout del cliente, y limpieza de lint. Este documento existe para que la misma auditoría se pueda repetir en iOS más adelante — cada sección dice qué invariante o mecanismo se blindó, para verificar si iOS ya lo tiene o le falta.

Commits en `feature/crashlytics-error-handling` (fast-forward a `main` en `71551b56`): `1d0d0fb9`, `76027034`, `60575539`, `249e5bee`, `571b5541`, `71551b56`.

## Cambios de hardening

### Observabilidad de crashes
- Firebase Crashlytics integrado, activado **solo** en builds `prod`/release (nunca dev/debug) vía `BuildConfig.IS_PRODUCTION && !BuildConfig.DEBUG`. Ver `VaiinillaApplication.kt`.
- Handler global de excepciones no capturadas (`core/error/GlobalCrashReporter.kt`): reporta a Crashlytics y **delega** al handler previo — nunca intenta mantener viva la app en un estado corrupto, el proceso muere normalmente.
- **Pendiente en Android**: nadie ha visto un crash de prueba llegar al dashboard real de Crashlytics — falta esa verificación en consola.
- **Para revisar en iOS**: ¿existe Crashlytics/reporte de crashes equivalente? ¿Hay un handler global (`NSSetUncaughtExceptionHandler` o Crashlytics automático) que no intente recuperar el estado tras un crash?

### Capa de red
- Retry con backoff (3 intentos, 500ms/1500ms) **solo para GET** — nunca para POST/PUT/DELETE, para no duplicar pagos/checkout. Respeta el header `Retry-After` del backend. Ver `core/network/GetRequestRetryPolicy.kt` + `HttpVaiinillaApiClient.kt`.
- `NetworkConnectivityObserver.kt`: chequeo proactivo de "sin conexión" (creado, no wireado aún a la UI).
- Mensajes de error 5xx genéricos ahora muestran copy amigable en vez del código crudo (`UserFacingErrorMessage.kt`).
- **Para revisar en iOS**: ¿los reintentos de red (si existen) están igual de restringidos a operaciones idempotentes? ¿Hay copy amigable para 5xx?

### Invariantes de Cocina/Caja (backend-mirrored en el cliente)
- **Cocina no puede saltarse PREPARANDO**: la transición PAID→READY directa debe fallar; solo PAID→PREPARANDO→READY es válida.
- **Caja no puede entregar sin QR válido**: DELIVERED requiere `pickupToken` no vacío; sin él, la transición debe rechazarse aunque el pedido esté en READY.
- Estas dos reglas ya estaban **implementadas** en `FixtureOrderRepository.kt` (el fixture que respalda `OperationalViewModel`), pero **nunca se probaban** — solo existía el camino feliz. Ahora hay 5 tests que fallan si alguien rompe cualquiera de las dos reglas (`OperationalStateMachineInvariantsTest.kt`).
- **Para revisar en iOS**: ¿existe el mismo par de invariantes (secuencia de estados de cocina, QR obligatorio para entrega) probado explícitamente, o solo implícito en la UI?

### Checkout del cliente — idempotencia (evitar cobros/pedidos duplicados)
- Mecanismo: una `idempotency key` se genera una vez por contenido de carrito (fingerprint SHA-256 de método de pago + destino + espacio + notas + líneas), se persiste en `GuestSessionStore`, y se reusa si la primera petición se pierde — así un reintento nunca crea un pedido duplicado ni cobra dos veces. Simétrico para el retry de Stripe.
- Extraído a `ui/order/OrderIdempotency.kt` (funciones puras `createOrderFingerprint` y `resolveIdempotencyKey`) y probado con 7 tests — antes solo se probaba que el storage persiste un string, no la lógica de decisión real.
- **Para revisar en iOS**: ¿cómo genera y persiste iOS su idempotency key para creación de orden y retry de Stripe? ¿Sobrevive a que la app se cierre a media petición? ¿Está esa lógica de decisión (reusar vs regenerar) probada, o solo el storage?

### UX de carrito
- Al llegar al límite de 20 unidades por línea, incrementar una línea existente ahora muestra el mismo mensaje de error que agregar una línea nueva (antes el botón `+` simplemente dejaba de responder sin avisar).
- Bug real corregido en `CartScreen.kt`: el ticker animado de precio (`AnimatedContent`) ignoraba el valor que le pasaba el framework y siempre leía la cantidad actual del carrito — la animación de "precio saliendo/entrando" nunca mostraba un cambio real de valor. **Verificado en dispositivo real (OnePlus Nord 3, adb sobre Tailscale)**: $108.90 → $217.80 al subir a 2, y de vuelta a $108.90 al bajar a 1, sin crash.
- **Para revisar en iOS**: ¿el límite de cantidad por línea da el mismo feedback en todos los puntos de entrada? ¿Hay algún ticker/animación de precio con el mismo riesgo de leer el valor equivocado del closure en vez del valor animado?

### Lint / calidad de código
- `lintProdDebug`: de 1 error + 125 warnings + 2 hints → 0 errores + 90 warnings.
- Quedan sin tocar a propósito: warnings de íconos del launcher (forma/duplicados/monocromo — necesitan revisión de diseño) y ~30 sugerencias de subir versión de dependencias/AGP/Gradle (Dependabot ya abrió PRs para varias de estas; revisar una por una, no mergear a ciegas).

## Fuera de alcance de esta pasada (pendiente, ambas plataformas)
- Tests instrumentados reales en dispositivo para los 3 flujos críticos (Stripe checkout, entrega QR Caja, transición de estados Cocina) — el scaffolding de `androidTest` + workflow CI existe pero solo se verificó que compila, nunca se corrió en un emulador/dispositivo real.
- Wallet del cliente: confirmado de solo lectura por diseño (recarga es exclusiva de Caja) — no aplica blindaje ahí.
- Discovery y auth de estudiante (login, enrollment, escaneo QR de venue): no se auditó a fondo esta pasada.

## Validación local (Android)
```bash
./gradlew --no-daemon testDevDebugUnitTest testProdDebugUnitTest
./gradlew --no-daemon ktlintCheck
./gradlew --no-daemon lintProdDebug
./gradlew --no-daemon assembleDevDebug assembleProdDebug
```
Todos en verde salvo los mismos 4 tests de UI/screenshot que ya fallaban antes de esta pasada (no relacionados, confirmado comparando contra `main` limpio).
