# Stripe: espera de confirmación en Android

Esta mejora vive en la rama `codex/stripe-android-visual-wait` y solo modifica el
repositorio Android. Backend, Supabase, Railway, Stripe Dashboard, iOS,
efectivo, saldo, wallet y cashback quedan fuera del cambio.

## Flujo

- PaymentSheet nunca convierte por sí solo el pedido en pagado.
- Al cerrarse PaymentSheet, Android consulta `GET /api/v1/pedidos/:id` de
  inmediato y vuelve a consultar cada 3 segundos durante un máximo de 90
  segundos.
- `pendiente_pago`, `processing` y `requires_action` permanecen en la pantalla
  de espera y no permiten retry ni abren otro PaymentSheet.
- El éxito solo se muestra cuando el pedido satisface
  `isStripePaymentConfirmedByBackend()`, que exige `payment_status = confirmado`
  y un estado operativo posterior al cobro.
- `fallido` y `cancelado` permiten reintentar mediante el endpoint existente
  `POST /api/v1/pedidos/:id/pago/stripe`. No se envían montos desde Android.
- El timeout es únicamente visual: muestra “Seguimos confirmando tu pago” y no
  transforma el pago en fallido.
- Al abrir un pedido Stripe pendiente desde Mis pedidos, el polling se reanuda.
  El mensaje “Pasa a Caja” continúa reservado para efectivo.

La pantalla dedicada `StripePaymentPendingScreen` usa el total devuelto por el
backend, semántica para TalkBack, objetivos táctiles y respeta la escala de
animación del sistema.

## Validación

Pasaron:

- `:app:compileDebugKotlin`
- tests focalizados de Stripe, contratos de pedido y UI (`40` tests)
- `:app:ktlintCheck`
- `:app:lintDebug`
- `:app:assembleDebug`

`testDebugUnitTest` completo y `verifyRoborazziDebug` quedan condicionados por
el entorno Windows: McAfee pone en cuarentena
`robolectric-nativeruntime.dll`, por lo que fallan los 54 tests visuales
existentes y los cuatro nuevos con `UnsatisfiedLinkError` de
`RenderNodeNatives`. No se desactivó el antivirus ni se añadieron capturas
generadas en modo Legacy, porque esas imágenes no representan la UI real. Las
capturas existentes de efectivo, saldo y wallet no fueron modificadas.

Cuando el runtime nativo pueda ejecutarse en un entorno autorizado, registrar y
verificar las cuatro referencias con:

```text
./gradlew :app:recordRoborazziDebug --no-daemon
./gradlew :app:verifyRoborazziDebug --no-daemon
```
