# VAI-26 — Registro, verificación y primer checkout (Android)

**Rama base:** `feature/VAI-25-descubrimiento-invitado-carrito`
**Rama trabajo:** `feature/VAI-26-registro-verificacion-checkout-david`
**Notion:** VAI-26 · Implementar registro, verificación y primer checkout
**Demo HTML:** `docs/ui-v2/Vaiinilla_Demo_Web_IA_CHAT.html` (referencia visual; **no** incluye pantallas de auth aún — seguir sistema editorial existente)

## Objetivo

Completar el wiring que VAI-25 dejó preparado (`prepareForGuestAuth`, `restoreGuestSessionAfterAuth`, tests de handoff): cuando un invitado confirma su **primer pedido en efectivo**, debe pasar por Firebase email/password, verificación de correo, alta idempotente de identidad `cliente`, creación/reuso de contexto cliente y retorno al mismo establecimiento/espacio/carrito.

## Criterios Notion (obligatorios)

1. Formulario pide solo **nombre**, **email**, **contraseña** y **aceptación auditable** de términos; identificador contextual solo si `establishment.clientIdRequired`.
2. Correo existente → ofrecer **iniciar sesión** o **recuperar acceso**.
3. **No** permitir primer pedido hasta correo verificado.
4. Tras verificar/login, volver al **mismo** establecimiento, espacio y carrito.
5. JWT Vaiinilla en `SecureSessionStore` (KeyStore); **no** guardar access token en SharedPreferences plano.
6. MOCK: flujo completo sin backend real de alta (fixtures). REMOTE: Firebase real + llamada de alta cuando esté disponible; si backend no responde, mensaje claro (dependencia VAI-19/BE).

## Referencia visual (demo HTML)

| Pantalla Android | Inspiración demo |
|----------------|------------------|
| Landing auth | `screen-01` — hero + CTA primario |
| Campos formulario | `screen-29` — `.field` label + input |
| Verificación / estados | editorial cards + `DemoEmptyState` |
| Cuenta activa (post-auth, opcional preview) | `screen-30` — avatar + filas de perfil |
| Carrito/checkout | `screen-13` — sin cambiar layout de pago efectivo |

## Arquitectura

```
domain/auth/student/
  StudentAuthRepository (interface)
  StudentAuthSession (uid, email, displayName, emailVerified)
  StudentEnrollmentRequest / Result

data/auth/student/
  FirebaseStudentAuthRepository (signUp, signIn, verify, reset)
  FixtureStudentAuthRepository (MOCK)
  RemoteStudentEnrollmentRepository (POST alta — path configurable)
  StudentAuthPreferences (solo flags UX, nunca autorización)

ui/auth/student/
  StudentAuthViewModel
  StudentAuthUiState
  StudentAuthLandingScreen
  StudentRegisterScreen
  StudentLoginScreen
  StudentVerifyEmailScreen
  StudentForgotPasswordScreen

ui/navigation/
  Routes.AUTH_* + auth subgraph en AppNavHost
```

## Flujo de navegación

```
Discovery → Catalog (guest) → Cart
  onConfirm (guest + !authenticated):
    orderFlow.prepareForGuestAuth()
    navigate AUTH_LANDING with returnRoute=CART

AUTH_LANDING → Register | Login
Register success → VerifyEmail (if !verified)
Login success → if !verified VerifyEmail else completeEnrollment()
VerifyEmail verified → completeEnrollment()
  → POST identidad/alta (MOCK/REMOTE) → sesiones/contexto-cliente → SecureSessionStore
  → orderFlow.restoreGuestSessionAfterAuth()
  → popBackStack to CART
Cart onConfirm (authenticated + verified) → submitOrder normal
```

## Reglas

- `guestSessionStore` **no** se limpia al entrar a auth.
- `clearGuestVenueForDemo()` solo en “Solo pruebas”.
- Reutilizar `VaiinillaJwtRefreshCoordinator` y patrón de `FirebaseSeedAuthRepository.exchangeContexto`.
- No tocar flujo staff/seed auth salvo extraer helper compartido si reduce duplicación mínima.
- Términos: checkbox obligatorio + timestamp en MOCK log; en REMOTE enviar en body de alta si el contrato lo exige.

## API REMOTE (contrato Entrega-02 en `VaiinillaBoveda`)

Abstraer en `StudentEnrollmentApi`:

- `POST /api/v1/identidad/alta` con Bearer Firebase ID token e `Idempotency-Key`.
  Body: `nombre`, `terminos_version` y `privacidad_version`.

- `POST /api/v1/sesiones/contexto-cliente` con Bearer Firebase ID token e `Idempotency-Key`.
  Body: `establecimiento_slug` e `identificador_cliente` opcional. La respuesta contiene el JWT y el contexto de cliente.

Si el endpoint devuelve 404/501, mostrar: “Alta de cliente no disponible en el servidor. Dependencia backend pendiente.”

## Tests

- `StudentAuthHandoffTest` — register path preserves venue+cart (extiende `DiscoveryGuestTest` pattern)
- `StudentAuthViewModelTest` — email exists → login suggestion; unverified blocks checkout
- Roborazzi opcional: `vai26_auth_register`, `vai26_auth_verify` (MOCK state)

## Archivos a tocar (lista cerrada)

- `app/src/main/java/com/vaiinilla/app/ui/navigation/Routes.kt`
- `app/src/main/java/com/vaiinilla/app/ui/navigation/AppNavHost.kt`
- `app/src/main/java/com/vaiinilla/app/ui/screens/CartScreen.kt` (CTA copy si guest)
- `app/src/main/java/com/vaiinilla/app/ui/order/OrderFlowViewModel.kt` (helper `requiresStudentAuth`)
- `app/src/main/java/com/vaiinilla/app/data/di/VaiinillaModule.kt`
- Nuevos archivos bajo `domain/auth/student`, `data/auth/student`, `ui/auth/student`, `ui/screens/StudentAuth*.kt`
- `app/src/main/java/com/vaiinilla/app/ui/components/EditorialUi.kt` — `EditorialTextField` si hace falta
- `app/src/test/java/com/vaiinilla/app/StudentAuthTest.kt`
- `app/build.gradle.kts` — bump `versionName` a `0.4.0-vai26-mock`

## Definition of Done

- [ ] Guest con carrito al confirmar → auth → vuelve a carrito con ítems
- [ ] Sin verificación de email no se crea pedido
- [ ] MOCK: `./gradlew testDebugUnitTest ktlintCheck assembleDebug` verde
- [ ] No access token en prefs inseguras
- [ ] PR abierto contra `feature/VAI-25-descubrimiento-invitado-carrito` o `main` según acuerdo equipo
