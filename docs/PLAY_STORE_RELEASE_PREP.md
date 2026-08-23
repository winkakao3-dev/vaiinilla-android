# Google Play — preparación técnica de Vaiinilla Android

Fecha de corte: 2026-08-23

Este documento concentra prerrequisitos comprobables para publicar `com.vaiinilla.app` en Google Play. No sustituye los formularios de Play Console ni reabre automáticamente App Content/Data Safety; sirve para llegar a esa etapa con infraestructura, privacidad y release correctamente preparados.

## 1. Compatibilidad de plataforma

Estado: **cubierto**.

- `applicationId`: `com.vaiinilla.app`.
- `compileSdk`: 36.
- `targetSdk`: 36.
- `minSdk`: 26.

Google Play exige Android 16 / API 36 para nuevas apps y actualizaciones móviles enviadas a partir del 31 de agosto de 2026. El proyecto ya está en API 36.

Fuente oficial:
- https://support.google.com/googleplay/android-developer/answer/11926878

## 2. Política de privacidad

Estado: **borrador técnico avanzado; no publicable todavía**.

Google Play exige una política de privacidad completa y accesible desde Play Console y desde la app. La URL debe ser pública, activa, accesible globalmente y no un PDF.

Repo:
- `docs/PRIVACY_POLICY_DRAFT.md`
- `docs/DATA_MAP.md`

Faltan principalmente datos legales, retenciones, regiones/configuración de proveedores, validación jurídica del tratamiento de menores y URL pública final. La audiencia de producto ya está definida como secundaria en adelante (aprox. 12+).

Fuente oficial:
- https://support.google.com/googleplay/android-developer/answer/10144311

## 3. Eliminación de cuenta

Estado in-app: **implementado**.

Estado web externo: **pendiente — KAK-47**.

Google Play exige que una app que permite crear cuentas ofrezca:

1. una vía detectable dentro de la app para solicitar eliminación;
2. un recurso web externo desde el que también pueda iniciarse la solicitud;
3. eliminación de datos asociados, salvo retenciones legítimas que deben declararse claramente.

Fuentes oficiales:
- https://support.google.com/googleplay/android-developer/answer/13327111
- https://support.google.com/googleplay/android-developer/answer/10144311

La URL externa todavía no existe/está confirmada. `saul1217/vaiinilla-web` no es accesible con la conexión GitHub actual, así que la implementación web requiere acceso del responsable o trabajo desde su repo.

## 4. Endpoint Android de producción

Estado: **resuelto — KAK-45**.

Railway CLI confirmó que el environment `production` del proyecto `vainiilla-pruebas` ejecuta el servicio `vaiinilla_back` desde `saul1217/vaiinilla_back` / `main`, deployment `5d16aa171cfb8a489f7eb73e73f7f45fe2480fef`.

Dominio production demostrado:

```text
https://vaiinillaback-development-3f6c.up.railway.app
```

Aunque el hostname contiene `development`, Railway lo asigna inequívocamente al environment `production`. Se verificó:

```text
GET /health   -> 200
GET /api/v1/  -> 200, api: vaiinilla, version: v1
```

Repository Variable configurada y re-leída con coincidencia exacta:

```text
VAIINILLA_API_BASE_URL=https://vaiinillaback-development-3f6c.up.railway.app/api/v1/
```

`app.vaiinilla.app` sigue siendo la superficie frontend y no la API base.

## 5. Firebase

Estado repo: **auditado parcialmente — KAK-49**.

Confirmado:

- Firebase project id: `vaiinilla-b3a70`.
- Android package: `com.vaiinilla.app`.
- Gradle incluye Firebase Authentication.
- No aparecen dependencias Firebase Analytics, Crashlytics, Messaging ni Storage en el cliente actual.

Pendiente en consola:

- confirmar que ése es el proyecto definitivo de producción;
- revisar restricciones de la API key cliente;
- comprobar servicios habilitados realmente;
- confirmar retención/configuración de Firebase Authentication;
- obtener las regiones/ubicación de procesamiento que deban declararse.

## 6. Retención backend

Estado: **parcialmente resuelto — KAK-50**.

Confirmado desde Railway production:

- backend `vaiinilla_back`, 1 réplica, región `us-east4-eqdc4a`;
- sin volúmenes Railway;
- logs/métricas disponibles, sin periodo de retención expuesto por CLI;
- Railway no expuso backups/snapshots.

Confirmado desde código:

- Firebase se elimina primero;
- datos identificativos, datos visibles de pedidos, invitaciones y snapshots auditables se anonimizan;
- pedidos, pagos, movimientos, wallet y aceptaciones legales se conservan por integridad;
- `limites_tasa` mayores a 48 horas se limpian vía `pg_cron`.

Pendiente externo:

- retención real de logs Railway;
- backups/snapshots/regiones de Supabase;
- retención Firebase/Resend;
- confirmación externa de migraciones Supabase aplicadas.

## 7. Signing y Play App Signing

Estado: **completado — KAK-51**.

No se encontró una upload key oficial previa ni evidencia de una publicación anterior que obligara a conservar otra identidad. Se creó la upload key oficial de Vaiinilla fuera del repositorio:

- alias: `vaiinilla-upload`;
- RSA 4096;
- SHA256withRSA;
- SHA-256 público: `4F:93:81:4F:0B:67:68:12:22:23:08:70:9B:53:05:4E:70:CA:86:DD:0C:A1:E0:D0:DE:39:8C:86:9E:96:D8:7A`;
- custodia primaria + recovery copy con permisos restrictivos.

GitHub Actions tiene configurados los cuatro secrets esperados:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

El keystore y sus contraseñas no están versionados. `.gitignore` protege `*.jks` y `*.keystore`.

## 8. Builds y AAB

Estado: **release firmado verificado**.

Los gates locales ya estaban verdes. Después de configurar production signing se generó de nuevo el AAB y `jarsigner` verificó correctamente la firma y la coincidencia del certificado con la upload key.

El workflow `Android Release Readiness` del `main` actual (`b0d73fb2`) terminó exitosamente en el run `32505457217`. El artifact `vaiinilla-release-aab` de ese mismo SHA fue descargado y `jarsigner` confirmó una firma válida. El SHA-256 verificado del AAB es `e6d6bbfcf3bf1dd7f2436b84076e869286c607b3453c4c1fb6a20ffd7ac15f44`. El workflow incluye:

- fixtures — PASS;
- scope audit — PASS;
- unit tests — PASS;
- Android lint — PASS;
- ktlint — PASS;
- `bundleRelease` — PASS;
- artifact AAB — cargado y `jar verified`.

El commit `7bb1061af36c778eddf7cde1fc79e8b49be2397f` añadió defaults seguros de versión (`0.5.0` / `16`) para variables opcionales vacías y las protecciones de keystore en `.gitignore`.

Desde signing/CI, el AAB está listo para el proceso de Play Console. Todavía no se ha subido ni publicado en Google Play.


## 9. Stripe y Data Safety

Estado Stripe Android: **integrado en Test Mode; E2E real pendiente**.

- Stripe Android `23.13.1` está incluido en `app/build.gradle.kts`.
- Checkout presenta PaymentSheet para `PaymentMethod.STRIPE`.
- Android recibe `client_secret`, `publishable_key` y `stripe_account_id`; no
  crea PaymentIntents ni contiene secretos Stripe.
- El cliente exige `pk_test_...` y rechaza `pk_live_...` en el mapper actual.
- PaymentSheet procesa la información sensible de pago directamente con Stripe.
- Stripe documenta telemetría de interacción/características del dispositivo y
  señales antifraude, por lo que Data Safety debe incluir el SDK.

La matriz de trabajo para el formulario está en:
`docs/play-store/DATA_SAFETY_FORM.md`.

Financial features: `Mobile payments and digital wallets` debe tratarse como
aplicable. `Rewards, points ... and other incentives` requiere decisión final
porque el cliente expone cashback/recompensas.

Fuentes:
- https://support.google.com/googleplay/android-developer/answer/10787469
- https://support.google.com/googleplay/android-developer/answer/13849271
- https://support.stripe.com/questions/stripe-mobile-sdk-privacy-details


## 10. Store Listing y App Access

Estado: **preparados a nivel de documentación; carga/credenciales externas pendientes**.

- `docs/play-store/STORE_LISTING.md` contiene nombre, categoría `Comida y bebida`, descripción breve y descripción completa dentro de los límites oficiales.
- Los seis screenshots están generados externamente y pendientes de recepción/validación.
- `docs/play-store/APP_ACCESS_REVIEW.md` contiene dos juegos de instrucciones de reviewer en inglés, sin contraseñas: cliente y staff.
- Falta crear/confirmar credenciales demo reutilizables y cargarlas solo en Play Console.

Google Play exige acceso a cualquier parte restringida por login u otra autenticación. Las credenciales deben ser de prueba, estar disponibles durante la revisión y no depender de OTP, ubicación o recursos temporales.

Fuentes:
- https://support.google.com/googleplay/android-developer/answer/9859152
- https://support.google.com/googleplay/android-developer/answer/9859673
- https://support.google.com/googleplay/android-developer/answer/9859455
- https://support.google.com/googleplay/android-developer/answer/15748846


## 11. Target Audience e IARC

Estado: **preparados a nivel de producto/contenido; carga en Play Console pendiente**.

- Target Audience: secundaria en adelante. Para cubrir honestamente a estudiantes
  de 12 años, la selección de trabajo en Play es `9-12`, `13-15`, `16-17` y
  `18 and over`.
- La selección `9-12` implica Families Policy; no se intenta evitar esa
  obligación declarando artificialmente 13+.
- IARC: auditoría de contenido preparada. No se detectan violencia, sexo, drogas,
  alcohol, apuestas, lenguaje ofensivo, chat entre usuarios ni GPS.
- La etiqueta IARC final solo se conocerá después de completar el cuestionario
  oficial en Play Console.

Documentos:
- `docs/play-store/TARGET_AUDIENCE.md`
- `docs/play-store/IARC_CONTENT_RATING.md`
- `docs/play-store/FAMILIES_COMPLIANCE.md`

Fuentes:
- https://support.google.com/googleplay/android-developer/answer/9867159
- https://support.google.com/googleplay/android-developer/answer/9893335
- https://support.google.com/googleplay/android-developer/answer/9898843

## 12. Qué falta antes de entrar al envío real de Play Console

1. Resolver KAK-47 — recurso web externo de eliminación.
2. Resolver KAK-48 — crear/obtener una cuenta Firebase de producción explícitamente descartable y ejecutar el E2E.
3. Terminar KAK-49 — Firebase Console/Google Cloud.
4. Terminar KAK-50 — retenciones/backups/regiones externas.
5. Terminar KAK-44 — política publicable y URL estable.
6. Confirmar la cuenta/propietario de Google Play Console y conservar la recovery copy de signing bajo un segundo custodio seguro.
7. Completar Data Safety usando `docs/play-store/DATA_SAFETY_FORM.md` y confirmar contractualmente los campos `Shared`.
8. Ejecutar E2E Stripe Test Mode y decidir Test Mode vs Live Mode antes de promocionar tarjeta en un release público.
9. Completar Financial features y trasladar App Access, IARC y Target Audience ya documentados a Play Console; cerrar validación jurídica de menores.

