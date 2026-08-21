# Vaiinilla Android ↔ Backend — flujo E2E y guardas de regresión

Última baseline conocida buena: `bf656596` (2026-08-19).

Este documento es fuente de verdad para cualquier cambio Android que toque registro, verificación, enrolamiento, contexto cliente, checkout, pedidos, seguimiento o entrega.

## 1. Límite de propiedad

Android puede inspeccionar contratos, logs y respuestas del backend para diagnosticar.

**No modificar código, Railway, Firebase/Auth config ni datos de producción del backend sin autorización explícita del responsable backend (Saúl).** Si una regresión nace en Android, se corrige en Android.

El servidor sigue siendo autoridad para identidad registrada, membresía, establecimiento, rol, precios, total, folio y estado del pedido.

## 2. Precondición de build

Una APK destinada a teléfonos reales debe usar una API base HTTPS válida. Nunca publicar/compartir una APK que haya caído al fallback `https://localhost.invalid/api/v1/`.

Configuración esperada por Gradle: `VAIINILLA_API_BASE_URL`, `-PvaiinillaApiBaseUrl` o `vaiinillaApiBaseUrl` en `local.properties`.
## 3. Flujo correcto del alumno

1. Discovery guarda `GuestVenueContext` con id, slug y reglas públicas del establecimiento.
2. Antes de mostrar registro/login, refrescar esos datos públicos. Si `identificador_cliente_obligatorio=true`, pedir el campo desde el primer enrolamiento (por ejemplo Matrícula).
3. Firebase crea/autentica la identidad. Correo verificado **no** significa checkout listo.
4. Verificación de correo: intentar primero el transporte Vaiinilla. Solo ante red/5xx/error interno puede usarse el fallback seguro de Firebase SDK. No usar fallback para 4xx, auth/validación ni `429 RATE_LIMITED`.
5. Tras verificar, completar `identidad/alta` si corresponde y pedir `POST /api/v1/sesiones/contexto-cliente` con Firebase ID token + establecimiento + identificador requerido.
6. Guardar el JWT de contexto retornado y arrancar `VaiinillaJwtRefreshCoordinator` con una callback capaz de regenerar **ese mismo contexto cliente**.
7. Marcar enrolamiento local únicamente después de obtener contexto válido.

### Invariante de checkout

`isReadyForCheckout(establishmentId)` debe equivaler a:

`Firebase session presente + emailVerified + enrolado para ESE establecimiento + JWT de contexto presente`.

Nunca reducirlo otra vez a `emailVerified` solamente.
## 4. Reinicio/actualización de la app

El JWT puede persistir en disco, pero el refresh coordinator vive en memoria. Por eso un JWT persistido **no puede considerarse listo por sí solo** tras reiniciar el proceso.

Al restaurar sesión:

1. conservar Firebase Auth y el establecimiento seleccionado;
2. invalidar el JWT cliente persistido antes de confiar en él;
3. usar Firebase para consultar `GET /api/v1/sesiones/accesos`;
4. localizar la membresía `cliente` del establecimiento actual y recuperar `identificador_cliente` si existe;
5. emitir un nuevo contexto mediante `POST /api/v1/sesiones/contexto-cliente`;
6. guardar el JWT nuevo y volver a iniciar el refresh coordinator.

Si el establecimiento exige identificador y el backend no devuelve uno, llevar al usuario a **Completa tu acceso**. No dejarlo avanzar con una sesión parcial.

Esto evita el fallo: `401 → refrescar → No hay sesión activa para refrescar`.
## 5. Pedido y estado operativo

Antes de crear pedido, el alumno debe tener contexto cliente válido. `GET /api/v1/estado-operativo` usa ese JWT; un error de auth no debe convertirse en “establecimiento offline”.

Para efectivo y para llevar, la secuencia operativa conocida buena es:

`por_cobrar → cobrado → preparando → listo → entregado`

### 5.1 Stripe Test Mode + Connect Direct Charges

Contrato final publicado en `/api/docs.json` y consumido por Android:

- crear: `POST /api/v1/pedidos` con `metodo_pago = "stripe"` e `Idempotency-Key`;
- reanudar/reintentar: `POST /api/v1/pedidos/{id}/pago/stripe` con `Idempotency-Key` y **sin request body**;
- reconciliar autoridad: `GET /api/v1/pedidos/{id}` después de cerrar PaymentSheet.

Android nunca envía total/subtotal/precios/comisiones, nunca crea PaymentIntents y nunca contiene claves secretas Stripe. La respuesta efímera de creación/reanudación entrega `client_secret`, `publishable_key` y `stripe_account_id`; no se persisten ni se registran en logs. Para la demo solo se acepta `publishable_key` de Test Mode (`pk_test_...`). Cada PaymentSheet se configura con la cuenta conectada recibida en esa misma respuesta.

`PaymentSheetResult.Completed` no equivale a cobro confirmado. Android solo muestra Stripe como confirmado cuando **ambas** condiciones vienen del backend: `pago.payment_status == confirmado` y `estado` está en `cobrado`, `preparando`, `listo` o `entregado`. Mientras el webhook no haya producido esa combinación se conserva estado pendiente y se vuelve a consultar con polling acotado. Un fallo o cancelación local de PaymentSheet tampoco inventa `fallido`/`cancelado`; esos estados solo se muestran si backend los devuelve.

El retry actúa sobre el mismo pedido. Una operación nueva genera nueva UUID; si se pierde la respuesta de esa misma operación, Android conserva su idempotency key para poder repetirla sin duplicar efectos.

- Cliente crea pedido.
- Caja confirma cobro.
- Cocina pasa a preparando y luego listo.
- Caja entrega pedidos `para_llevar`.
- La transición `listo → entregado` exige el token de recogida correcto.

El campo `usuario.matricula` en respuestas de pedidos puede ser `null`; el DTO Android debe seguir aceptándolo como nullable.

El backend puede responder estados válidos aunque datos de perfil opcionales sean nulos. Nunca hacer fallar el parser completo por un dato opcional.
## 6. QR/token de recogida

El backend devuelve `qr_token` **solo en la respuesta de creación del pedido**. Las consultas posteriores no lo exponen.

Android debe:

1. guardarlo inmediatamente en `PickupTokenStore` al crear el pedido;
2. volver a adjuntarlo a `OrderDetail` cuando carga seguimiento/listados locales;
3. mostrarlo al alumno cuando el pedido esté `listo` como QR y texto de respaldo;
4. conservarlo hasta completar entrega/cerrar sesión según la política de limpieza.

Caja escanea ese valor y lo envía como `qr_token` al hacer `listo → entregado`.

No inventar un token desde folio/id y no reemplazar el token real por el QR visual del recibo.

## 7. UX que también forma parte del contrato

- Una sesión Firebase ya verificada no debe volver a mostrar “Registrarme / Ya tengo cuenta”; debe continuar el enrolamiento pendiente.
- Si falta Matrícula, pedir solo Matrícula en “Completa tu acceso”.
- En seguimiento usar **Ver recibo**, reutilizando el recibo original; no reintroducir “Ver sticker”.
- El card amarillo `listo` debe usar texto oscuro (`accentInk`) también en Dark/Amoled.
## 8. Errores que ya cometimos y no debemos repetir

| Incidente | Causa | Guarda actual |
|---|---|---|
| Login nuevo no conectaba | APK Ubuntu compilada con `localhost.invalid` | preflight de API base + CI explícito |
| Caja parpadeaba error JSON | `usuario.matricula=null` contra `String` no nullable | `OrderUserDtoNullabilityTest` |
| Correo “enviado” aunque falló | UI asumía éxito del backend | estado de entrega real + fallback seguro |
| Verificación funcionaba antes y dejó de hacerlo | se eliminó fallback Firebase indiscriminadamente | fallback solo en fallos recuperables |
| Checkout dejaba avanzar sin contexto | readiness reducida a `emailVerified` | tests de enrolamiento/contexto |
| Tras reinicio aparecía “No hay sesión activa para refrescar” | JWT persistido + refresher en memoria vacío | rehidratación vía `/sesiones/accesos` |
| Caja no podía cerrar entrega | token existía pero alumno no lo veía | QR/token visible en `LISTO` |
| Pedido listo ilegible en oscuro | amarillo + texto heredado blanco | `accentInk` para tarjeta `LISTO` |

## 9. Baselines útiles

- `5c86b43d`: tolerancia a `matricula: null`.
- `61ba398d`: fallback seguro de verificación.
- `5d0b9237`: contexto cliente requerido antes de checkout.
- `e5fa24c3`: matrícula/enrolamiento, recibo, QR de recogida y contraste.
- `bf656596`: restauración segura del contexto cliente tras reinicio.
## 10. Gate antes de compartir una APK

Ejecutar siempre:

```bash
./scripts/check_app_backend_contract.sh
./gradlew --no-daemon testDebugUnitTest lintDebug ktlintCheck assembleDebug
```

No publicar APK si cualquiera falla.

## 11. Smoke E2E físico mínimo

Con una cuenta alumno válida y saulP1:

1. entrar a la tienda y confirmar que Matrícula aparece si hace falta;
2. registro/login → verificación → volver al carrito sin repetir identidad;
3. confirmar pedido en efectivo;
4. Caja ve `por_cobrar` y cobra;
5. Cocina recibe, inicia y marca `listo`;
6. alumno ve `LISTO` + QR/token + recibo;
7. Caja escanea el QR y el pedido pasa a `entregado`;
8. cerrar/reabrir app alumno y confirmar que la sesión se rehidrata sin error de refresh.

Si una modificación toca cualquiera de estas etapas, repetir este smoke antes de Release.
